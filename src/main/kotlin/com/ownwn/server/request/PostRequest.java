package com.ownwn.server.request;


import com.ownwn.server.FormAttachment;
import com.ownwn.server.FormByteParser;
import com.ownwn.server.Headers;
import com.ownwn.server.HttpMethod;

import com.ownwn.server.java.lang.replacement.*;
import com.ownwn.server.java.lang.replacement.stream.InputStream;
import com.ownwn.server.java.lang.replacement.stream.OutputStream;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PostRequest extends Request {
    private static final Pattern formBoundaryPattern = Pattern.compile("^\\s*multipart/form-data;\\s*boundary=(.+)\\s*$");

    private String multiPartFormBoundary = null;
    private Iterator<FormAttachment> formAttachments = null;


    public PostRequest(SimpleAddress remoteAddress, InputStream requestBody, Headers requestHeaders,
                       OutputStream responseBody, String path, Map<String, String> cookies, Map<String, String> queryParameters) {

        super(remoteAddress, requestBody, requestHeaders, responseBody, path, cookies, queryParameters);

        multiPartFormBoundary = tryParseMultiPartFormBoundary();

    }

    @Override
    public HttpMethod method() {
        return HttpMethod.POST;
    }

    public java.util.Iterator<FormAttachment> formDataIterator() {
        if (multiPartFormBoundary == null) return null;
        if (formAttachments != null) return formAttachments.java();
        int contentLength;
        try {
            contentLength = Integer.parseUnsignedInt(requestHeaders().get("Content-Length").trim());
        } catch (Exception e) {
            return Iterator.<FormAttachment>empty().java();
        }


        FormByteParser parser = new FormByteParser(requestBody, multiPartFormBoundary, contentLength);
        return new java.util.Iterator<FormAttachment>() {
            @Override
            public boolean hasNext() {
                parser.
            }

            @Override
            public FormAttachment next() {
                return null;
            }
        }
        formAttachments = parser.getAttachments();
        return formAttachments.java();
    }

    private String tryParseMultiPartFormBoundary() {
        String contentType = requestHeaders.get("Content-Type");
        if (contentType == null) return null;

        Matcher formBoundaryMatcher = formBoundaryPattern.matcher(contentType);
        if (!formBoundaryMatcher.find()) {
            return null;
        }

        return "--" + formBoundaryMatcher.group(1);
    }

}
