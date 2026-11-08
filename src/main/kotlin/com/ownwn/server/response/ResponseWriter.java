package com.ownwn.server.response;

import com.ownwn.server.Headers;
import com.ownwn.server.java.lang.replacement.IOException;
import com.ownwn.server.java.lang.replacement.stream.InputStream;
import com.ownwn.server.java.lang.replacement.stream.OutputStream;
import com.ownwn.server.request.Request;
import java.nio.charset.StandardCharsets;


public final class ResponseWriter {
    private final OutputStream output;
    private boolean started;

    public ResponseWriter(OutputStream output) {
        this.output = output;
    }

    public boolean hasStarted() {
        return started;
    }

    public void send(Response response) throws IOException {
        // Open the body before committing headers: opening a file can fail.
        send(response.status(), response.headers(), response.body());
    }

    public void send(int status, Headers headers, InputStream body) throws IOException {
        try {
            if (started) throw new IllegalStateException("Response already started");
            started = true; // A failed write may already have sent some bytes.
            output.write(Request.getHttpMessageFromCode(status).getBytes(StandardCharsets.US_ASCII));
            for (var header : headers.entrySet()) {
                if (header.getKey().equalsIgnoreCase("Connection")) continue;
                output.write(("\r\n" + header.getKey() + ": " + header.getValue()).getBytes(StandardCharsets.ISO_8859_1));
            }
            output.write("\r\nConnection: close\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
            body.transferTo(output);
        } catch (IOException | RuntimeException | Error e) {
            try { body.close(); } catch (Exception closeError) { e.addSuppressed(closeError); }
            throw e;
        }
        body.close();
    }
}
