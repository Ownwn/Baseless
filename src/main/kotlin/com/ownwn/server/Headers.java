package com.ownwn.server;


import com.ownwn.server.java.lang.replacement.*;

import java.util.Locale;

public class Headers {
    private final Map<String, String> internalHeaders;

    public static Headers fromRawList(List<String> rawHeaders) {
        Headers headers = new Headers();
        for (String header : rawHeaders) {
            int colon = header.indexOf(':');
            if (colon <= 0) throw new HttpRequestException(400, "Malformed HTTP header");
            String name = header.substring(0, colon);
            for (int i = 0; i < name.length(); i++) {
                if (!HeaderValue.token(name.charAt(i))) throw new HttpRequestException(400, "Invalid header name");
            }
            // Do not silently overwrite duplicate framing headers.
            if ((name.equalsIgnoreCase("Content-Length") || name.equalsIgnoreCase("Transfer-Encoding"))
                    && headers.get(name) != null) {
                throw new HttpRequestException(400, "Duplicate request body framing header");
            }
            String value = header.substring(colon + 1).trim();
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                if ((c < 32 && c != '\t') || c == 127) throw new HttpRequestException(400, "Invalid header value");
            }
            headers.put(name, value);
        }
        return headers;
    }

    public static Headers of(String... headersPair) {
        if (headersPair.length % 2 != 0) {
            throw new IllegalArgumentException("bad headers len");
        }
        Headers h = new Headers();
        for (int i = 0; i < headersPair.length; i+= 2) {
            h.put(headersPair[i], headersPair[i+1]);
        }
        return h;
    }

    public Map<String, String> getAndRemoveCookies() {
        String cookieValue = internalHeaders.remove("Cookie");
        if (cookieValue == null || cookieValue.isBlank()) return Map.of();
        Map<String, String> res = new HashMap<>();
        String[] cookies = cookieValue.split(";"); // todo breaks on ; inside cookie content?
        for (String cookie : cookies) {
            String[] parts = cookie.split("=");
            if (parts.length != 2) continue;
            res.put(parts[0], parts[1]);
        }
        return res;
    }

    public Set<Map.Entry<String, String>> entrySet() {
        return internalHeaders.entrySet();
    }

    public Headers() {
        internalHeaders = new HashMap<>();
    }

    public String put(String headerName, String headerValue) {
        return internalHeaders.put(capitalise(headerName), headerValue);
    }

    public String get(String headerName) {
        return internalHeaders.get(capitalise(headerName));
    }

    @Override
    public String toString() {
        return "Headers{" +
                "internalHeaders=" + internalHeaders +
                '}';
    }

    private String capitalise(String s) {
        return Arrays.stream(s.split("-", -1))
                .map(str -> str.isEmpty() ? str : (str.charAt(0) + "").toUpperCase(Locale.ROOT) + str.substring(1).toLowerCase(Locale.ROOT))
                .collect(Collectors.joining("-"));
    }


}
