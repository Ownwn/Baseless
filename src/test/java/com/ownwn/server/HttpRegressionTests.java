package com.ownwn.server;

import com.ownwn.server.java.lang.replacement.Map;
import com.ownwn.server.java.lang.replacement.SimpleAddress;
import com.ownwn.server.java.lang.replacement.IOException;
import com.ownwn.server.java.lang.replacement.stream.*;
import com.ownwn.server.request.*;
import com.ownwn.server.response.*;
import com.ownwn.server.sockets.Client;
import java.lang.foreign.Arena;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/// Ai generated, pretty unreadable but should do the job
public class HttpRegressionTests {
    private static int checks;

    public static void main(String[] args) throws Exception {
        multipartBytes();
        malformedForms();
        // Expected failures should retain diagnostics, without flooding test output.
        var originalError = System.err;
        var diagnostics = new java.io.ByteArrayOutputStream();
        try (var capturedError = new java.io.PrintStream(diagnostics)) {
            System.setErr(capturedError);
            connectionFailures();
        } finally {
            System.setErr(originalError);
        }
        check(diagnostics.toString().contains("private details"), "log unexpected server failures");
        check(diagnostics.toString().contains("HTTP 413: limit"), "log concise client rejections");
        storageFailures();
        System.out.println("Passed " + checks + " HTTP regression checks");
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static byte[] bytes(String value) { return value.getBytes(StandardCharsets.ISO_8859_1); }

    private static PostRequest post(InputStream body, Headers headers) {
        return new PostRequest(null, body, headers, new Capture(), "/upload", Map.of(), Map.of());
    }

    private static byte[] form(byte[] data) {
        var buffer = new java.io.ByteArrayOutputStream();
        buffer.writeBytes(bytes("--test\r\nContent-Disposition: form-data; filename=\"a;\\\"b.bin\"; name=\"custom\"\r\n\r\n"));
        buffer.writeBytes(data);
        buffer.writeBytes(bytes("\r\n--test--\r\n"));
        return buffer.toByteArray();
    }

    private static Headers formHeaders(int length) {
        return Headers.of("Content-Type", "Multipart/Form-Data; charset=utf-8; BOUNDARY=\"test\"", "Content-Length", "" + length);
    }

    private static void multipartBytes() throws Exception {
        byte[][] cases = {new byte[0], bytes("--test\r\npayload"), bytes("\r\n--testX\r\n--test--X\r\n--tes\r\n"), new byte[]{0, -1, 13, 10, 0}};
        for (byte[] data : cases) {
            byte[] form = form(data);
            for (int fragment : new int[]{1, 2, 7, 32768}) {
                var input = new Fragmented(form, fragment);
                var handler = post(input, formHeaders(form.length)).formAttachmentHandler();
                var out = new java.io.ByteArrayOutputStream();
                handler.consumeAttachments(part -> {
                    check(part.type().equals("custom"), "preserve actual field name");
                    check(part.fileName().equals("a;\"b.bin"), "quoted disposition parameters");
                    return out;
                });
                check(Arrays.equals(data, out.toByteArray()), "exact file bytes, fragment " + fragment);
                try { handler.consumeAttachments(part -> out); throw new AssertionError("second consumption accepted"); }
                catch (IllegalStateException expected) { checks++; }
            }
        }
        consume("--test--", 8); // closing delimiter can end at EOF
        checks++;
        Headers unusualName = Headers.of("X--Test-", "value");
        check("value".equals(unusualName.get("x--test-")), "preserve valid header names with empty hyphen segments");
        // Exercise the real 32 KB refill boundary as well as tiny fragmented reads.
        byte[] large = new byte[70000];
        new java.util.Random(1234).nextBytes(large);
        byte[] payload = form(large);
        var out = new java.io.ByteArrayOutputStream();
        post(new Fragmented(payload, 32768), formHeaders(payload.length)).formAttachmentHandler().consumeAttachments(part -> out);
        check(Arrays.equals(large, out.toByteArray()), "binary content across refills");
        byte[] multi = bytes("--test\r\nContent-Disposition: form-data; name=text\r\n\r\none\r\n--test\r\nContent-Disposition: form-data; name=file; filename=x\r\n\r\n\r\n--test--\r\n");
        var parts = new java.util.ArrayList<java.io.ByteArrayOutputStream>();
        post(new Fragmented(multi, 1), formHeaders(multi.length)).formAttachmentHandler().consumeAttachments(part -> {
            var stream = new java.io.ByteArrayOutputStream(); parts.add(stream); return stream;
        });
        check(parts.size() == 2 && parts.get(0).toString().equals("one") && parts.get(1).size() == 0, "multiple and empty parts");
        byte[] sentinel = bytes("--test--\r\nNEXT REQUEST");
        var input = new Fragmented(sentinel, 1);
        post(input, formHeaders(10)).formAttachmentHandler().consumeAttachments(part -> new java.io.ByteArrayOutputStream());
        check(input.read() == 'N', "do not read beyond Content-Length");
    }

    private static void expectStatus(int status, Runnable action) {
        try { action.run(); throw new AssertionError("Expected HTTP " + status); }
        catch (HttpRequestException expected) { check(expected.status() == status, "HTTP status " + status); }
    }

    private static void malformedForms() {
        // Construction is lazy: a non-multipart POST can reach routing/authentication.
        post(InputStream.emptyInputStream(), new Headers()); checks++;
        expectStatus(415, () -> post(InputStream.emptyInputStream(), new Headers()).formAttachmentHandler());
        expectStatus(415, () -> post(InputStream.emptyInputStream(), Headers.of("Content-Type", "application/json")).formAttachmentHandler());
        for (String type : new String[]{"multipart/form-data", "multipart/form-data; boundary=\"\"", "multipart/form-data; boundary=\"test", "multipart/form-data; boundary=test; boundary=test"}) {
            expectStatus(400, () -> post(InputStream.emptyInputStream(), Headers.of("Content-Type", type)).formAttachmentHandler());
        }
        expectStatus(411, () -> post(InputStream.emptyInputStream(), Headers.of("Content-Type", "multipart/form-data; boundary=test")).formAttachmentHandler());
        for (String length : new String[]{"-1", "+1", "", "abc", "1, 1"}) {
            expectStatus(400, () -> post(InputStream.emptyInputStream(), Headers.of("Content-Length", length)));
        }
        expectStatus(413, () -> post(InputStream.emptyInputStream(), Headers.of("Content-Length", "9999999999999999999")));
        for (String value : new String[]{"bad", "--test\rX", "--test\r\n\r\n", "--test\r\nContent-Disposition: form-data\r\n\r\na\r\n--test--", "--test\r\nContent-Disposition: form-data; name=x\r\n\r\ntruncated"}) {
            expectStatus(400, () -> consume(value, value.length()));
        }
        expectStatus(400, () -> consume("--test--\r\n", 100));
    }

    private static void consume(String value, int length) {
        try {
            post(new Fragmented(bytes(value), 2), formHeaders(length)).formAttachmentHandler()
                    .consumeAttachments(part -> new java.io.ByteArrayOutputStream());
        } catch (IOException e) { throw new RuntimeException(e); }
    }

    private static void storageFailures() throws Exception {
        byte[] data = form(bytes("payload"));
        boolean[] closed = {false};
        try {
            post(new Fragmented(data, 1), formHeaders(data.length)).formAttachmentHandler().consumeAttachments(part -> new java.io.OutputStream() {
                public void write(int b) throws java.io.IOException { throw new java.io.IOException("disk failed"); }
                public void close() { closed[0] = true; }
            });
            throw new AssertionError("storage failure swallowed");
        } catch (IOException expected) {
            check(expected.getCause() instanceof java.io.IOException, "preserve storage failure cause");
        }
        check(closed[0], "close attachment output even when flush fails");
    }

    private static void connectionFailures() throws Exception {
        runConnection("GET / HTTP/1.1\r\n\r\n", new Capture(), request -> WholeBodyResponse.ok("ok"), "200");
        runConnection("bad\r\n\r\n", new Capture(), request -> { throw new AssertionError("must not route"); }, "400");
        runConnection("POST / HTTP/1.1\r\nContent-Length: 0\r\ncontent-length: 1\r\n\r\n", new Capture(), request -> WholeBodyResponse.ok(), "400");
        runConnection("POST / HTTP/1.1\r\nTransfer-Encoding: chunked\r\n\r\n", new Capture(), request -> WholeBodyResponse.ok(), "501");
        runConnection("POST / HTTP/1.1\r\nTransfer-Encoding: chunked\r\nContent-Length: 0\r\n\r\n", new Capture(), request -> WholeBodyResponse.ok(), "400");
        runConnection("GET / HTTP/1.1\r\n\r\n", new Capture(), request -> { throw new HttpRequestException(413, "limit"); }, "413");
        runConnection("GET / HTTP/1.1\r\n\r\n", new Capture(), request -> { throw new IllegalStateException("private details"); }, "500");
        Capture failedWrite = new Capture(); failedWrite.failAt = 12;
        runConnection("GET / HTTP/1.1\r\n\r\n", failedWrite, request -> WholeBodyResponse.ok("ok"), null);
        check(failedWrite.attempts == 13, "no fallback writes after a partial write");
        final boolean[] closed = {false};
        Response brokenBody = new Response(new Headers(), 200) {
            public int bodyLength() { return 0; }
            public InputStream body() { return new InputStream() {
                public int read() throws IOException { throw new IOException("body failure"); }
                public void close() { closed[0] = true; }
            }; }
        };
        runConnection("GET / HTTP/1.1\r\n\r\n", new Capture(), request -> brokenBody, "200");
        check(closed[0], "response body closes on transfer failure");
        Response cannotOpen = new Response(new Headers(), 200) {
            public int bodyLength() { return 0; }
            public InputStream body() throws IOException { throw new IOException("open failure"); }
        };
        runConnection("GET / HTTP/1.1\r\n\r\n", new Capture(), request -> cannotOpen, "500");
        Capture output = new Capture();
        runConnection("GET / HTTP/1.1\r\n\r\n", output, request -> {
            request.sendResponseOk(InputStream.emptyInputStream());
            throw new HttpRequestException(400, "too late");
        }, "200");
    }

    private static void runConnection(String raw, Capture output,
            com.ownwn.server.java.lang.replacement.function.Function<Request, Response> handler, String status) {
        Client client = new Client() {
            public InputStream getInputStream(Arena arena) { return new Fragmented(bytes(raw), 3); }
            public OutputStream getOutputStream(Arena arena) { return output; }
            public SimpleAddress getInetAddress() { return null; }
        };
        try (Arena arena = Arena.ofConfined()) { BaseHttpServer.processClient(client, arena, handler); }
        String response = output.data.toString(StandardCharsets.ISO_8859_1);
        check(output.closes == 1, "socket closes exactly once");
        check(response.indexOf("HTTP/1.1") == response.lastIndexOf("HTTP/1.1"), "one status line");
        if (status != null) check(response.startsWith("HTTP/1.1 " + status), "response status " + status);
        if ("500".equals(status)) check(!response.contains("private details"), "hide internal errors");
    }

    private static class Capture implements OutputStream {
        final java.io.ByteArrayOutputStream data = new java.io.ByteArrayOutputStream();
        int closes, attempts, failAt = Integer.MAX_VALUE;
        public void write(int b) throws IOException {
            if (attempts++ >= failAt) throw new IOException("simulated disconnect");
            data.write(b);
        }
        public void close() { closes++; }
    }

    private static class Fragmented implements InputStream {
        private final byte[] data;
        private final int fragment;
        private int position;
        Fragmented(byte[] data, int fragment) { this.data = data; this.fragment = fragment; }
        public int read() { return position < data.length ? data[position++] & 255 : -1; }
        public int readNBytes(byte[] target, int offset, int length) {
            int count = Math.min(Math.min(length, fragment), data.length - position);
            System.arraycopy(data, position, target, offset, count); position += count; return count;
        }
        public void close() {}
    }
}
