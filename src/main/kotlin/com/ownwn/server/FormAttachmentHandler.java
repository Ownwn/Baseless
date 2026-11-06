package com.ownwn.server;

import com.ownwn.server.java.lang.replacement.*;
import com.ownwn.server.java.lang.replacement.function.*;
import com.ownwn.server.java.lang.replacement.stream.InputStream;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class FormAttachmentHandler {
    private static final Pattern dispositionPattern = Pattern.compile("form-data; name=\"([^\"]+)\"(?:; filename=\"([^\"]+)\")?");
    private static final int chunkSize = 32768;
    private final InputStream body;
    private final int contentLength;
    private final byte[] boundary;


    public void consumeAttachments(Function<FormAttachment, java.io.OutputStream> attachmentHandler) throws IOException {
        class Writer implements PartConsumer, AutoCloseable {
            private java.io.OutputStream output;

            public void start(Headers headers) {
                var dispositionMatcher = dispositionPattern.matcher(headers.get("Content-Disposition"));
                FormAttachment attachment;
                if (!dispositionMatcher.find()) {
                    throw new RuntimeException("cant find match content disposition");
                }
                if (dispositionMatcher.group(1).equals("text")) {
                    attachment = new FormAttachment("text", null);
                } else {
                    attachment = new FormAttachment("file", dispositionMatcher.group(2));
                }
                output = new BufferedOutputStream(Objects.requireNonNull(attachmentHandler.apply(attachment)), chunkSize);
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
                throw new IOException("Expected starting multipart boundary");
            }
        }
        int first = input.read();
        int second = input.read();
        if (first == '-' && second == '-') {
            input.drain();
            return;
        }
        if (first != '\r' || second != '\n') {
            throw new IOException("Expected CRLF after opening boundary");
        }

        // Include the suffix in the match: boundary-like file bytes followed by
        // something other than CRLF or -- are ordinary content.
        byte[] nextPart = new byte[boundary.length + 4];
        nextPart[0] = '\r';
        nextPart[1] = '\n';
        System.arraycopy(boundary, 0, nextPart, 2, boundary.length);
        nextPart[nextPart.length - 2] = '\r';
        nextPart[nextPart.length - 1] = '\n';
        byte[] lastPart = nextPart.clone();
        lastPart[lastPart.length - 2] = '-';
        lastPart[lastPart.length - 1] = '-';

        boolean finished = false;
        while (!finished) {
            consumer.start(grabHeaders(input));
            byte[] pending = new byte[nextPart.length];
            // accept an empty part whose boundary immediately follows the header blank line
            pending[0] = '\r';
            pending[1] = '\n';
            int pendingSize = 2;
            int syntheticBytes = 2;
            while (true) {
                int value = input.read();
                if (value == -1) throw new IOException("Multipart content ended before its closing boundary");
                pending[pendingSize++] = (byte) value;

                while (!isPrefix(pending, pendingSize, nextPart)
                        && !isPrefix(pending, pendingSize, lastPart)) {
                    if (syntheticBytes > 0) syntheticBytes--;
                    else consumer.write(pending[0]);
                    System.arraycopy(pending, 1, pending, 0, --pendingSize);
                }
                if (pendingSize == nextPart.length) {
                    finished = pending[pendingSize - 1] == '-';
                    consumer.done();
                    break;
                }
            }
        }
        // Consume the final CRLF/epilogue, but never read into the next request.
        input.drain();
    }

    private static boolean isPrefix(byte[] pending, int size, byte[] delimiter) {
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
            if (value == -1) throw new IOException("Unexpected end of multipart headers");
            if (++total > 16384) throw new IOException("Multipart headers exceed 16 KB");
            if (value == '\r') {
                if (input.read() != '\n') throw new IOException("Expected CRLF in multipart headers");
                if (++total > 16384) throw new IOException("Multipart headers exceed 16 KB");
                if (lineLength == 0) return headers;
                String line = new String(buf, 0, lineLength, StandardCharsets.ISO_8859_1);
                int colon = line.indexOf(':');
                if (colon <= 0) throw new IOException("Malformed multipart header");
                headers.put(line.substring(0, colon).trim(), line.substring(colon + 1).trim());
                lineLength = 0;
            } else {
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
                if (available <= 0) throw new IOException("Request body shorter than Content-Length");
                remaining -= available;
                position = 0;
            }
            return buffer[position++] & 0xff;
        }

        void drain() throws IOException {
            while (read() != -1) { }
        }
    }



    public FormAttachmentHandler(InputStream body, byte[] boundary, int contentLength) throws IOException {
        if (boundary.length == 0 || contentLength < 0) {
            throw new IllegalArgumentException("Expected a boundary and non-negative Content-Length");
        }
        this.boundary = boundary.clone();
        this.body = body;
        this.contentLength = contentLength;
    }
}
