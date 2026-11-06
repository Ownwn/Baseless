package com.ownwn.server.request;


import com.ownwn.server.FormAttachmentHandler;
import com.ownwn.server.Headers;
import com.ownwn.server.HttpMethod;

import com.ownwn.server.java.lang.replacement.*;
import com.ownwn.server.java.lang.replacement.stream.InputStream;
import com.ownwn.server.java.lang.replacement.stream.OutputStream;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PostRequest extends Request {
    private static final Pattern formBoundaryPattern = Pattern.compile("^\\s*multipart/form-data;\\s*boundary=(.+)\\s*$");

    private String multiPartFormBoundary = null;
    private FormAttachmentHandler handler = null;


    public PostRequest(SimpleAddress remoteAddress, InputStream requestBody, Headers requestHeaders,
                       OutputStream responseBody, String path, Map<String, String> cookies, Map<String, String> queryParameters) {

        super(remoteAddress, requestBody, requestHeaders, responseBody, path, cookies, queryParameters);

        multiPartFormBoundary = tryParseMultiPartFormBoundary();

    }

    @Override
    public HttpMethod method() {
        return HttpMethod.POST;
    }

    public FormAttachmentHandler formAttachmentHandler() {
        if (multiPartFormBoundary == null) {
            throw new RuntimeException("Malformed request!");
        };
        if (handler != null) return handler;

        int contentLength = Integer.parseUnsignedInt(requestHeaders().get("Content-Length").trim());

        try {
            return (handler = new FormAttachmentHandler(requestBody, multiPartFormBoundary.getBytes(StandardCharsets.UTF_8), contentLength));
        } catch (IOException e) {
            throw new RuntimeException(e); // todo all the exceptions
        }
    }

    private String tryParseMultiPartFormBoundary() {
        String contentType = requestHeaders.get("Content-Type");
        if (contentType == null) throw new RuntimeException("Missing content type!");

        Matcher formBoundaryMatcher = formBoundaryPattern.matcher(contentType);
        if (!formBoundaryMatcher.find()) {
            throw new RuntimeException("cant find form boundary in content type!");
        }

        return "--" + formBoundaryMatcher.group(1);
    }

}
