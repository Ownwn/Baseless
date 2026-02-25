package com.ownwn.server.response;

import com.ownwn.server.java.lang.replacement.*;
import com.ownwn.server.java.lang.replacement.stream.ByteArrayInputStream;
import com.ownwn.server.java.lang.replacement.stream.InputStream;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class TemplateResponse extends Response {
    public static final String templatesDirectory = "templates/";
    public static final String notFoundUrl = "notfound";

    private final String url;

    TemplateResponse(int status, String url) {
        this.url = url;
        this.status = status;
    }

    @Override
    public int bodyLength() {
        return 0;
    }

    @Override
    public InputStream body() throws IOException {
        Optional<File> file = getTemplateFile(url);
        if (file.isPresent()) {
            return Files.newInputStream(file.get().toPath());
        }
        status = 404;


        Optional<File> errorNotFoundTemplate = getTemplateFile(notFoundUrl);
        if (errorNotFoundTemplate.isPresent()) {
            return Files.newInputStream(errorNotFoundTemplate.get().toPath());
        }

        return new ByteArrayInputStream("<h1>404 Not found</h1>".getBytes(StandardCharsets.UTF_8));

    }

    Optional<File> getTemplateFile(String url) {
        File[] templates = getTemplatesFolder().listFiles();
        //noinspection DataFlowIssue
        for (File template : templates) {
            if (template.isDirectory() || !template.getName().endsWith(".html")) {
                continue;
            }
            String name = template.getName().replaceFirst("\\.html$", "");
            if (url.equals(name)) {
                return Optional.of(template);
            }
        }
        return Optional.empty();
    }

    private File getTemplatesFolder() {
        File templatesFolder = Path.of(templatesDirectory).toFile();
        if (!templatesFolder.exists() || !templatesFolder.isDirectory()) {
            throw new RuntimeException("Templates folder missing when loading " + url);
        }
        return templatesFolder;

    }

    public static TemplateResponse of(String url) {
        return TemplateResponse.of(200, url);
    }

    public static TemplateResponse of(int status, String url) {
        return new TemplateResponse(status, url);
    }
}
