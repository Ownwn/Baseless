package com.ownwn.server;

import com.ownwn.server.java.lang.replacement.Arrays;

public enum HttpMethod {
    GET, POST, PUT, DELETE, PATCH, HEAD, CONNECT, OPTIONS, TRACE; // todo most are unsupported

    static HttpMethod fromString(String name) {
        return Arrays.stream(values()).filter(m -> m.name().intern() == name.intern()).findAny().orElse(null);
    }
}
