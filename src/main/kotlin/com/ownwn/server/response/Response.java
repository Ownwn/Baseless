package com.ownwn.server.response;

import com.ownwn.server.Headers;
import com.ownwn.server.java.lang.replacement.*;
import com.ownwn.server.java.lang.replacement.stream.ByteArrayInputStream;
import com.ownwn.server.java.lang.replacement.stream.InputStream;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public abstract class Response {
    protected Headers headers = new Headers() {{
        put("Content-Type", "text/html");
    }};
    protected int status;

    public abstract int bodyLength();
    public abstract InputStream body() throws IOException;


    public Headers headers() {
        return headers;
    }

    public int status() {
        return status;
    }

    public static TemplateResponse notFound = new TemplateResponse(404, TemplateResponse.notFoundUrl) {
        @Override
        public InputStream body() throws IOException {
            Optional<File> errorNotFoundTemplate = getTemplateFile(notFoundUrl);
            if (errorNotFoundTemplate.isPresent()) {
                return Files.newInputStream(errorNotFoundTemplate.get().toPath());
            }

            return new ByteArrayInputStream("<h1>404 Not found</h1>".getBytes(StandardCharsets.UTF_8));

        }
    };
}