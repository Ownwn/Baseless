package com.ownwn.server.request;

import com.ownwn.server.FormAttachmentHandler;
import com.ownwn.server.Headers;
import com.ownwn.server.HeaderValue;
import com.ownwn.server.HttpMethod;
import com.ownwn.server.HttpRequestException;
import com.ownwn.server.java.lang.replacement.*;
import com.ownwn.server.java.lang.replacement.stream.InputStream;
import com.ownwn.server.java.lang.replacement.stream.OutputStream;
import java.nio.charset.StandardCharsets;

public class PostRequest extends Request {
    private FormAttachmentHandler handler;

    public PostRequest(SimpleAddress remoteAddress, InputStream requestBody, Headers requestHeaders,
                       OutputStream responseBody, String path, Map<String, String> cookies, Map<String, String> queryParameters) {
        super(remoteAddress, requestBody, requestHeaders, responseBody, path, cookies, queryParameters);
    }

    @Override
    public HttpMethod method() { return HttpMethod.POST; }

    public FormAttachmentHandler formAttachmentHandler() {
        if (handler != null) return handler;
        String contentType = requestHeaders.get("Content-Type");
        if (contentType == null) throw new HttpRequestException(415, "Expected multipart/form-data");
        HeaderValue parsed = HeaderValue.parse(contentType);
        if (!parsed.value().equalsIgnoreCase("multipart/form-data")) {
            throw new HttpRequestException(415, "Expected multipart/form-data");
        }
        String boundary = parsed.parameters().get("boundary");
        if (boundary == null || !boundary.matches("[0-9A-Za-z'()+_,./:=? -]{1,70}") || boundary.endsWith(" ")) {
            throw new HttpRequestException(400, "Missing or invalid multipart boundary");
        }
        handler = new FormAttachmentHandler(requestBody,
                ("--" + boundary).getBytes(StandardCharsets.US_ASCII), contentLength());
        return handler;
    }
}
