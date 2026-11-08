package com.ownwn.server.request;


import com.ownwn.server.Headers;
import com.ownwn.server.HttpMethod;
import com.ownwn.server.HttpRequestException;
import com.ownwn.server.response.ResponseWriter;
import com.ownwn.server.java.lang.replacement.*;
import com.ownwn.server.java.lang.replacement.stream.InputStream;
import com.ownwn.server.java.lang.replacement.stream.OutputStream;


public abstract class Request {
    protected final SimpleAddress remoteAddress;
    protected final InputStream requestBody;
    protected final Headers requestHeaders;
    protected final OutputStream responseBody;
    private final ResponseWriter responseWriter;
    private final Integer contentLength;
    protected String path;
    protected final Map<String, String> cookies;
    protected final Map<String, String> queryParameters;

    public Request(SimpleAddress remoteAddress, InputStream requestBody, Headers requestHeaders,
                   OutputStream responseBody, String path, Map<String, String> cookies, Map<String, String> queryParameters) {
        this.remoteAddress = remoteAddress;
        this.requestBody = requestBody;
        this.requestHeaders = requestHeaders;
        this.contentLength = validateBodyFraming(requestHeaders);
        this.responseBody = responseBody;
        this.responseWriter = new ResponseWriter(responseBody);
        this.path = path;
        this.cookies = cookies.entrySet().stream().collect(HashMap::new, (map, val) -> map.put(val.getKey().trim(), val.getValue()), (a1, a2) -> {throw new Error("no parrelel");});
        this.queryParameters = queryParameters;
    }

    private static Integer validateBodyFraming(Headers headers) {
        if (headers.get("Transfer-Encoding") != null) {
            if (headers.get("Content-Length") != null) {
                throw new HttpRequestException(400, "Conflicting request body framing");
            }
            throw new HttpRequestException(501, "Transfer-Encoding is not supported");
        }
        String length = headers.get("Content-Length");
        if (length == null) return null;
        length = length.trim();
        if (!length.matches("[0-9]+")) throw new HttpRequestException(400, "Invalid Content-Length");
        try {
            return Integer.parseInt(length);
        } catch (NumberFormatException e) {
            throw new HttpRequestException(413, "Content-Length exceeds supported size");
        }
    }

    public int contentLength() {
        if (contentLength == null) throw new HttpRequestException(411, "Content-Length is required");
        return contentLength;
    }

    public static GetRequest getRequest(SimpleAddress remoteAddress, InputStream requestBody, Headers requestHeaders, OutputStream responseBody, String path, Map<String, String> cookies, Map<String, String> queryParameters) {
        return new GetRequest(remoteAddress, requestBody, requestHeaders, responseBody, path, cookies, queryParameters);
    }
    public static PostRequest postRequest(SimpleAddress remoteAddress, InputStream requestBody, Headers requestHeaders, OutputStream responseBody, String path, Map<String, String> cookies, Map<String, String> queryParameters) {
        return new PostRequest(remoteAddress, requestBody, requestHeaders, responseBody, path, cookies, queryParameters);
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

    public static String getHttpMessageFromCode(int status) {
        return "HTTP/1.1 " + status + " " + switch (status) {
            case 200 -> "OK";
            case 302 -> "Found";
            case 411 -> "Length Required";
            case 413 -> "Content Too Large";
            case 415 -> "Unsupported Media Type";
            case 431 -> "Request Header Fields Too Large";
            case 500 -> "Internal Server Error";
            case 501 -> "Not Implemented";
            case 404 -> "Not Found";
            case 401 -> "Unauthorised";
            case 405 -> "Method Not Allowed";
            case 400 -> "Bad Request";
            case 505 -> "HTTP Version Not Supported";
            default -> "NYI";
        };
    }

    /** Prefer returning a Response from handlers; kept for existing callers. */
    public void sendResponse(int status, Headers headers, InputStream body) {
        try {
            responseWriter.send(status, headers, body);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ResponseWriter responseWriter() {
        return responseWriter;
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

}
