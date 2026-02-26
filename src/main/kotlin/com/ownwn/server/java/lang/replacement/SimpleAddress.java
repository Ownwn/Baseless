package com.ownwn.server.java.lang.replacement;

public record SimpleAddress(String ip, Integer port) {
    public static SimpleAddress ofLiteral(String ip) {
        return new SimpleAddress(ip, null);
    }
}
