import com.ownwn.server.FormAttachmentHandler;
import com.ownwn.server.java.lang.replacement.stream.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Run after make compile: javac -cp out -d out tests/FormAttachmentHandlerTest.java && java -cp out FormAttachmentHandlerTest */
public class FormAttachmentHandlerTest {
    static final String B = "------geckoformboundary97875ff997775de94d269bd10a567594";
    static byte[] bytes(String s) { return s.getBytes(StandardCharsets.UTF_8); }

    // Deliberately returns short reads; a single-byte underlying read is a test failure.
    static class Source implements InputStream {
        final byte[] data;
        final int fragment;
        int position;
        Source(byte[] data, int fragment) { this.data = data; this.fragment = fragment; }
        public int read() { throw new AssertionError("Parser must use bulk input"); }
        public void close() { throw new AssertionError("Parser must not close the connection"); }
        public int readNBytes(byte[] buffer, int offset, int length) {
            int n = Math.min(Math.min(length, fragment), data.length - position);
            System.arraycopy(data, position, buffer, offset, n);
            position += n;
            return n;
        }
    }

    static void check(byte[] body, List<byte[]> expected, int fragment) throws Exception {
        byte[] withNextRequest = Arrays.copyOf(body, body.length + 10);
        Source source = new Source(withNextRequest, fragment);
        var handler = new FormAttachmentHandler(source, bytes(B), body.length);
        var parts = new ArrayList<byte[]>();
        var current = new ByteArrayOutputStream();
        handler.consumeAttachments(b -> current.write(b & 255), () -> {
            parts.add(current.toByteArray());
            current.reset();
        });
        if (source.position != body.length) throw new AssertionError("Read beyond Content-Length");
        if (parts.size() != expected.size()) throw new AssertionError("Wrong completion count: " + parts.size());
        for (int i = 0; i < parts.size(); i++) {
            if (!Arrays.equals(parts.get(i), expected.get(i))) throw new AssertionError("Content mismatch in part " + i);
        }
        if (current.size() != 0) throw new AssertionError("Content delivered after completion");
    }

    static void rejects(byte[] body, int contentLength) throws Exception {
        try {
            new FormAttachmentHandler(new Source(body, 7), bytes(B), contentLength)
                    .consumeAttachments(b -> {}, () -> {});
            throw new AssertionError("Expected IOException");
        } catch (IOException expected) { }
    }

    static void checkDestinations(byte[] body, byte[] expected) throws Exception {
        var textPath = java.nio.file.Files.createTempFile("multipart-text-", ".txt");
        var emptyPath = java.nio.file.Files.createTempFile("multipart-file-", ".bin");
        var opened = new ArrayList<java.io.OutputStream>();
        int[] closed = {0};
        try {
            var handler = new FormAttachmentHandler(new Source(body, 7), bytes(B), body.length);
            handler.consumeAttachments(headers -> {
                String disposition = headers.get("content-disposition");
                if (disposition == null) throw new AssertionError("Missing header");
                boolean file = disposition.contains("filename=");
                if (file && !"application/octet-stream".equals(headers.get("Content-Type"))) {
                    throw new AssertionError("Missing content type");
                }
                var output = new java.io.FilterOutputStream(java.nio.file.Files.newOutputStream(file ? emptyPath : textPath)) {
                    public void close() throws IOException { super.close(); closed[0]++; }
                };
                opened.add(output);
                return output;
            });
            if (opened.size() != 2 || closed[0] != 2) throw new AssertionError("Destinations not closed per part");
            if (!Arrays.equals(java.nio.file.Files.readAllBytes(textPath), expected)) throw new AssertionError("File bytes differ");
            if (java.nio.file.Files.size(emptyPath) != 0) throw new AssertionError("Empty file has content");
        } finally {
            java.nio.file.Files.deleteIfExists(textPath);
            java.nio.file.Files.deleteIfExists(emptyPath);
        }
        int[] closedOnError = {0};
        byte[] truncated = Arrays.copyOf(body, body.length - B.length() - 6);
        try {
            new FormAttachmentHandler(new Source(truncated, 7), bytes(B), truncated.length)
                    .consumeAttachments(headers -> new java.io.ByteArrayOutputStream() {
                        public void close() { closedOnError[0]++; }
                    });
            throw new AssertionError("Expected truncated input failure");
        } catch (IOException expectedError) {
            if (closedOnError[0] == 0) throw new AssertionError("Destination leaked on error");
        }
        new FormAttachmentHandler(new Source(body, 7), bytes(B), body.length)
                .consumeAttachments(headers -> java.io.OutputStream.nullOutputStream());
    }

    public static void main(String[] args) throws Exception {
        String text = "10ZLOIav/aRHXSd6Y62zBL2YnTom2dgvCKtVAD5AJk8=";
        String first = B + "\r\nContent-Disposition: form-data; name=\"text\"\r\n\r\n";
        String fileHeaders = "Content-Disposition: form-data; name=\"file\"; filename=\"%s\"\r\nContent-Type: application/octet-stream\r\n\r\n";
        byte[] curl = bytes(first + text + "\r\n" + B + "\r\n" + fileHeaders.formatted("padding.txt")
                + B + "\r\n" + fileHeaders.formatted("makoGraduate.txt") + B + "--\r\n");
        for (int size = 1; size <= B.length() + 8; size++) {
            check(curl, List.of(bytes(text), new byte[0], new byte[0]), size);
        }
        // Binary data, false delimiter prefixes, overlapping CRLFs, and multiple input buffers.
        var payload = new ByteArrayOutputStream();
        for (int i = 0; i < 100000; i++) payload.write(i & 255);
        payload.write(bytes("\r\r\n" + B + "X\r\n" + B + "-X\r\n" + B.substring(0, 20)));
        var large = new ByteArrayOutputStream();
        large.write(bytes(first));
        large.write(payload.toByteArray());
        large.write(bytes("\r\n" + B + "\r\n" + fileHeaders.formatted("empty") + "\r\n" + B + "--\r\n"));
        for (int size : new int[]{1, 7, 59, 32768}) {
            check(large.toByteArray(), List.of(payload.toByteArray(), new byte[0]), size);
        }
        checkDestinations(large.toByteArray(), payload.toByteArray());
        check(bytes(B + "--\r\n"), List.of(), 1);
        rejects(bytes("wrong"), 5);
        rejects(bytes(first + text), first.length() + text.length());
        rejects(curl, curl.length + 1);
        rejects(bytes(B + "\r\n" + "x".repeat(16385)), B.length() + 2 + 16385);
        System.out.println("Multipart tests passed (curl, split boundaries, binary data, empty parts, framing, malformed input).");
    }
}
