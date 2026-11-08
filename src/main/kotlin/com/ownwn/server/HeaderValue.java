package com.ownwn.server;

import com.ownwn.server.java.lang.replacement.HashMap;
import com.ownwn.server.java.lang.replacement.Map;
import java.util.Locale;

/// Ai generated, pretty unreadable but should do the job
public record HeaderValue(String value, Map<String, String> parameters) {
    public static HeaderValue parse(String raw) {
        if (raw == null) throw new HttpRequestException(400, "Missing header value");
        int end = raw.indexOf(';');
        if (end < 0) end = raw.length();
        String value = raw.substring(0, end).trim();
        if (value.isEmpty()) throw malformed();
        Map<String, String> parameters = new HashMap<>();
        int pos = end;
        while (pos < raw.length()) {
            pos++; // semicolon
            while (pos < raw.length() && whitespace(raw.charAt(pos))) pos++;
            int start = pos;
            while (pos < raw.length() && token(raw.charAt(pos))) pos++;
            if (start == pos) throw malformed();
            String name = raw.substring(start, pos).toLowerCase(Locale.ROOT);
            while (pos < raw.length() && whitespace(raw.charAt(pos))) pos++;
            if (pos == raw.length() || raw.charAt(pos++) != '=') throw malformed();
            while (pos < raw.length() && whitespace(raw.charAt(pos))) pos++;
            String parameter;
            if (pos < raw.length() && raw.charAt(pos) == '"') {
                pos++;
                StringBuilder quoted = new StringBuilder();
                boolean closed = false;
                while (pos < raw.length()) {
                    char c = raw.charAt(pos++);
                    if (c == '"') { closed = true; break; }
                    if (c == '\\') {
                        if (pos == raw.length()) throw malformed();
                        c = raw.charAt(pos++);
                    }
                    if ((c < 32 && c != '\t') || c == 127) throw malformed();
                    quoted.append(c);
                }
                if (!closed) throw malformed();
                parameter = quoted.toString();
            } else {
                start = pos;
                while (pos < raw.length() && token(raw.charAt(pos))) pos++;
                if (start == pos) throw malformed();
                parameter = raw.substring(start, pos);
            }
            if (parameters.get(name) != null) throw malformed();
            parameters.put(name, parameter);
            while (pos < raw.length() && whitespace(raw.charAt(pos))) pos++;
            if (pos < raw.length() && raw.charAt(pos) != ';') throw malformed();
        }
        return new HeaderValue(value, parameters);
    }

    static boolean token(char c) {
        return c >= '0' && c <= '9' || c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z'
                || "!#$%&'*+-.^_`|~".indexOf(c) >= 0;
    }

    private static boolean whitespace(char c) { return c == ' ' || c == '\t'; }
    private static HttpRequestException malformed() {
        return new HttpRequestException(400, "Malformed header parameters");
    }
}
