package com.ownwn.server;

import com.ownwn.server.java.lang.replacement.*;
import com.ownwn.server.java.lang.replacement.function.*;
import com.ownwn.server.java.lang.replacement.stream.BufferedOutputStream;
import com.ownwn.server.java.lang.replacement.stream.InputStream;
import com.ownwn.server.java.lang.replacement.stream.OutputStream;

import java.nio.charset.StandardCharsets;

public class FormAttachmentHandler {
    private static final int chunkSize = 32768;
    private final InputStream body;
    private final int contentLength;
    private final byte[] boundary;
    private boolean consumed;


    public void consumeAttachments(Function<FormAttachment, java.io.OutputStream> attachmentHandler) throws IOException {
        if (consumed) throw new IllegalStateException("Multipart body has already been consumed");
        consumed = true; // Also disallow retries after a failed or partial read.
        class Writer implements PartConsumer, AutoCloseable {
            private OutputStream output;

            public void start(Headers headers) {
                HeaderValue disposition = HeaderValue.parse(headers.get("Content-Disposition"));
                String name = disposition.parameters().get("name");
                if (!disposition.value().equalsIgnoreCase("form-data") || name == null || name.isEmpty()) {
                    throw new HttpRequestException(400, "Expected form-data disposition with a name");
                }
                FormAttachment attachment = new FormAttachment(name, disposition.parameters().get("filename"));
                var outputStream = OutputStream.fromJava(attachmentHandler.apply(attachment));
                output = new BufferedOutputStream(Objects.requireNonNull(outputStream), chunkSize);
            }

            public void write(byte value) throws IOException {
                output.write(value & 0xff);
            }

            public void done() throws IOException {
                close();
            }

            public void close() throws IOException {
                if (output != null) {
                    var current = output;
                    output = null;
                    current.close();
                }
            }
        }
        try (var writer = new Writer()) {
            consumeParts(writer);
        }
    }

    private interface PartConsumer {
        void start(Headers headers) throws IOException;
        void write(byte value) throws IOException;
        void done() throws IOException;
    }

    private void consumeParts(PartConsumer consumer) throws IOException {
        BufferedBody input = new BufferedBody();
        for (byte expected : boundary) {
            if (input.read() != (expected & 0xff)) {
                throw new HttpRequestException(400, "Expected starting multipart boundary");
            }
        }
        int first = input.read();
        int second = input.read();
        if (first == '-' && second == '-') {
            int suffix = input.read();
            if (suffix != -1 && (suffix != '\r' || input.read() != '\n')) {
                throw new HttpRequestException(400, "Malformed closing multipart boundary");
            }
            input.drain();
            return;
        }
        if (first != '\r' || second != '\n') {
            throw new HttpRequestException(400, "Expected CRLF after opening boundary");
        }

        // Include the suffix in the match: boundary-like file bytes followed by
        // something other than CRLF or -- are ordinary content.
        byte[] nextPart = new byte[boundary.length + 4];
        nextPart[0] = '\r';
        nextPart[1] = '\n';
        System.arraycopy(boundary, 0, nextPart, 2, boundary.length);
        nextPart[nextPart.length - 2] = '\r';
        nextPart[nextPart.length - 1] = '\n';
        byte[] lastPart = new byte[nextPart.length + 2];
        System.arraycopy(nextPart, 0, lastPart, 0, nextPart.length - 2);
        lastPart[lastPart.length - 4] = '-';
        lastPart[lastPart.length - 3] = '-';
        lastPart[lastPart.length - 2] = '\r';
        lastPart[lastPart.length - 1] = '\n';

        boolean finished = false;
        while (!finished) {
            consumer.start(grabHeaders(input));
            byte[] pending = new byte[lastPart.length];
            // Only actual CRLF bytes delimit parts; file bytes may start with --boundary.
            int pendingSize = 0;
            while (true) {
                int value = input.read();
                if (value == -1) {
                    // A closing boundary may end at EOF without a final CRLF.
                    if (pendingSize == lastPart.length - 2 && isPrefix(pending, pendingSize, lastPart)) {
                        consumer.done();
                        finished = true;
                        break;
                    }
                    throw new HttpRequestException(400, "Multipart content ended before its closing boundary");
                }
                pending[pendingSize++] = (byte) value;

                while (!isPrefix(pending, pendingSize, nextPart)
                        && !isPrefix(pending, pendingSize, lastPart)) {
                    consumer.write(pending[0]);
                    System.arraycopy(pending, 1, pending, 0, --pendingSize);
                }
                if (pendingSize == lastPart.length
                        || pendingSize == nextPart.length && isPrefix(pending, pendingSize, nextPart)) {
                    finished = pendingSize == lastPart.length;
                    consumer.done();
                    break;
                }
            }
        }
        // Consume the final CRLF/epilogue, but never read into the next request.
        input.drain();
    }

    private static boolean isPrefix(byte[] pending, int size, byte[] delimiter) {
        if (size > delimiter.length) return false;
        for (int i = 0; i < size; i++) {
            if (pending[i] != delimiter[i]) return false;
        }
        return true;
    }

    private static Headers grabHeaders(BufferedBody input) throws IOException {
        int total = 0;
        int lineLength = 0;
        var headers = new Headers();
        byte[] buf = new byte[16384];
        while (true) {
            int value = input.read();
            if (value == -1) throw new HttpRequestException(400, "Unexpected end of multipart headers");
            if (++total > 16384) throw new HttpRequestException(400, "Multipart headers exceed 16 KB");
            if (value == '\r') {
                if (input.read() != '\n') throw new HttpRequestException(400, "Expected CRLF in multipart headers");
                if (++total > 16384) throw new HttpRequestException(400, "Multipart headers exceed 16 KB");
                if (lineLength == 0) return headers;
                String line = new String(buf, 0, lineLength, StandardCharsets.ISO_8859_1);
                int colon = line.indexOf(':');
                if (colon <= 0) throw new HttpRequestException(400, "Malformed multipart header");
                String name = line.substring(0, colon);
                for (int i = 0; i < name.length(); i++) {
                    if (!HeaderValue.token(name.charAt(i))) throw new HttpRequestException(400, "Invalid multipart header name");
                }
                if (headers.get(name) != null) throw new HttpRequestException(400, "Duplicate multipart header");
                headers.put(name, line.substring(colon + 1).trim());
                lineLength = 0;
            } else {
                if ((value < 32 && value != '\t') || value == 127) throw new HttpRequestException(400, "Invalid multipart header value");
                buf[lineLength++] = (byte) value;
            }
        }
    }

    /** read() here indexes an in-memory buffer; refills stay inside Content-Length.
     * method written with assistance of LLM
     * */
    private final class BufferedBody {
        private final byte[] buffer = new byte[chunkSize];
        private int position;
        private int available;
        private int remaining = contentLength;

        int read() throws IOException {
            if (position == available) {
                if (remaining == 0) return -1;
                available = body.readNBytes(buffer, 0, Math.min(buffer.length, remaining));
                if (available <= 0) throw new HttpRequestException(400, "Request body shorter than Content-Length");
                remaining -= available;
                position = 0;
            }
            return buffer[position++] & 0xff;
        }

        void drain() throws IOException {
            while (read() != -1) { }
        }
    }



    public FormAttachmentHandler(InputStream body, byte[] boundary, int contentLength) {
        if (boundary.length == 0 || contentLength < 0) {
            throw new IllegalArgumentException("Expected a boundary and non-negative Content-Length");
        }
        this.boundary = boundary.clone();
        this.body = body;
        this.contentLength = contentLength;
    }
}
