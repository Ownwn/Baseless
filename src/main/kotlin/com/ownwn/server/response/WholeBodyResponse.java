package com.ownwn.server.response;

import com.ownwn.server.Headers;
import com.ownwn.server.JsonConvertible;

import java.nio.charset.StandardCharsets;

import com.ownwn.server.java.lang.replacement.*;
import com.ownwn.server.java.lang.replacement.stream.ByteArrayInputStream;
import com.ownwn.server.java.lang.replacement.stream.InputStream;

public class WholeBodyResponse extends Response {
    private final byte[] body;


    private WholeBodyResponse(int status, byte[] body, Headers headers) {
        this.status = status;
        this.body = body;
        this.headers = headers;
    }

    @Override
    public int bodyLength() {
        return body.length;
    }

    @Override
    public InputStream body() {
        return new ByteArrayInputStream(body);
    }

    public static WholeBodyResponse of(int status, String body) {
        return WholeBodyResponse.of(status, body.getBytes(StandardCharsets.UTF_8));
    }

    public static WholeBodyResponse template(int status, String templateName, Map<String, String> headers) {
        Headers resHeaders = new Headers();
        for (var header : headers.entrySet()) {
            resHeaders.put(header.getKey(), header.getValue());
        }

        return new WholeBodyResponse(status, new byte[]{}, resHeaders);
    }

    public static WholeBodyResponse of(int status, byte[] body, Map<String, String> headers) {
        Headers resHeaders = new Headers();
        for (var header : headers.entrySet()) {
            resHeaders.put(header.getKey(), header.getValue());
        }
        return new WholeBodyResponse(status, body, resHeaders);
    }

    public static WholeBodyResponse ok(String body) {
        return WholeBodyResponse.of(200, body);
    }

    public static <T extends JsonConvertible> WholeBodyResponse json(java.util.List<T> list) {
        String json = new ArrayList<>(list).stream().map(JsonConvertible::toJson).collect(Collectors.joining(", ", "[", "]"));
        return WholeBodyResponse.of(200, json.getBytes(StandardCharsets.UTF_8), Map.of("Content-Type", "application/json"));
    }

    public static WholeBodyResponse ok() {
        return WholeBodyResponse.of(200, "");
    }

    public static WholeBodyResponse of(int status, byte[] body) {
        return WholeBodyResponse.of(status, body, Map.of());
    }

    public static WholeBodyResponse ok(byte[] body) {
        return WholeBodyResponse.of(200, body);
    }

    public static WholeBodyResponse softRedirect(String path) {
        return WholeBodyResponse.of(302, new byte[]{}, Map.of("Location", path));
    }

    public static WholeBodyResponse unauthorised() {
        return WholeBodyResponse.of(401, "401 Unauthorised");
    }

    public static WholeBodyResponse badRequest() {
        return WholeBodyResponse.of(400, "");
    }

    public static WholeBodyResponse badRequest(String body) {
        return WholeBodyResponse.of(400, body);
    }
}
