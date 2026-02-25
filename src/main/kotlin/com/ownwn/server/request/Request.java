package com.ownwn.server.request;


import com.ownwn.server.Headers;
import com.ownwn.server.HttpMethod;
import com.ownwn.server.java.lang.replacement.*;
import com.ownwn.server.java.lang.replacement.stream.InputStream;
import com.ownwn.server.java.lang.replacement.stream.OutputStream;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public abstract class Request {
    protected final SimpleAddress remoteAddress;
    protected final InputStream requestBody;
    protected final Headers requestHeaders;
    protected final OutputStream responseBody;
    protected String path;
    protected final Map<String, String> cookies;
    protected final Map<String, String> queryParameters;

    public Request(SimpleAddress remoteAddress, InputStream requestBody, Headers requestHeaders,
                   OutputStream responseBody, String path, Map<String, String> cookies, Map<String, String> queryParameters) {
        this.remoteAddress = remoteAddress;
        this.requestBody = requestBody;
        this.requestHeaders = requestHeaders;
        this.responseBody = responseBody;
        this.path = path;
        this.cookies = cookies.entrySet().stream().collect(HashMap::new, (map, val) -> map.put(val.getKey().trim(), val.getValue()), (a1, a2) -> {throw new Error("no parrelel");});
        this.queryParameters = queryParameters;
    }

    private Map<String, String> parseQueries(String query) {
        if (query == null || query.isBlank()) return Map.of();

        Map<String, String> queries = new HashMap<>();
        String[] parts = query.split("&");
        for (String pair : parts) {
            String[] keyValue = pair.split("=");
            if (keyValue.length != 2) continue;
            if (keyValue[1].isBlank() || keyValue[0].isBlank()) continue;
            queries.put(keyValue[0], keyValue[1]);
        }
        return queries;
    }

    private Map<String, String> parseCookies(Headers headers) {
        String cookieString = headers.get("Cookie");
        if (cookieString == null) {
            return Map.of();
        }

        String[] cookiePairs = cookieString.split(";", 50);
        if (cookiePairs.length >= 50) {
            System.err.println("Max number of cookies reached! " + cookiePairs[0] + ", " + cookiePairs[1] + "...");
            return Map.of();
        }

        Map<String, String> cookies = new HashMap<>();

        for (String cookie : cookiePairs) {
            String[] parts = cookie.split("=", 2);
            if (parts.length != 2) {
                System.err.println("Malformed cookie recieved " + cookie.substring(0, Math.min(20, cookie.length())));
                return cookies;
            }
            cookies.put(parts[0].trim(), parts[1]);
        }
        return cookies;
    }

    public SimpleAddress remoteAddress() {
        return remoteAddress;
    }

    public void sendResponseOk(InputStream body) {
        Headers h = new Headers();

        sendResponse(200, h, body);
    }

    public void sendResponse404(InputStream body) {
        sendResponse(404, new Headers(), body);
    }

    public void sendResponse(int status, Headers headers, InputStream body) {
        try {
            String httpMsg = "HTTP/1.1 " + status + " " + switch (status) {
                case 200 -> "OK";
                case 404 -> "Not Found";
                case 401 -> "Unauthorised";
                default -> "NYI";
            };


            responseBody.write(httpMsg.getBytes(StandardCharsets.UTF_8));


            for (var header : headers.entrySet()) {
                responseBody.write(new byte[]{13, 10});
                responseBody.write(header.getKey().getBytes(StandardCharsets.UTF_8));
                responseBody.write(": ".getBytes(StandardCharsets.UTF_8));
                responseBody.write(header.getValue().getBytes(StandardCharsets.UTF_8));
            }
            responseBody.write(new byte[]{13, 10, 13, 10});

            body.transferTo(responseBody);

            responseBody.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public abstract HttpMethod method();

    public InputStream requestBody() {
        return requestBody;
    }

    public Headers requestHeaders() {
        return requestHeaders;
    }

    public OutputStream responseBody() {
        return responseBody;
    }

    public String path() {
        return path;
    }

    public Map<String, String> cookies() {
        return cookies;
    }

    public Map<String, String> queryParameters() {
        return queryParameters;
    }

    public void setPath(String newPath) {
        this.path = newPath;
    }

//    @Override
//    public boolean equals(Object obj) {
//        if (obj == this) return true;
//        if (obj == null || obj.getClass() != this.getClass()) return false;
//        var that = (Request) obj;
//        return Objects.equals(this.remoteAddress, that.remoteAddress) &&
//                Objects.equals(this.requestBody, that.requestBody) &&
//                Objects.equals(this.requestHeaders, that.requestHeaders) &&
//                Objects.equals(this.responseBody, that.responseBody) &&
//                Objects.equals(this.path, that.path) &&
//                Objects.equals(this.cookies, that.cookies) &&
//                Objects.equals(this.queryParameters, that.queryParameters);
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(remoteAddress, requestBody, requestHeaders, responseBody, path, cookies, queryParameters);
//    }
//
//    @Override
//    public String toString() {
//        return "Request[" +
//                "remoteAddress=" + remoteAddress + ", " +
//                "requestBody=" + requestBody + ", " +
//                "requestHeaders=" + requestHeaders + ", " +
//                "responseBody=" + responseBody + ", " +
//                "path=" + path + ", " +
//                "cookies=" + cookies + ", " +
//                "queryParameters=" + queryParameters + ']';
//    }


}
