package com.ownwn.server.java.lang.replacement;

public class IOException extends Exception {

    public IOException(Throwable t) {
        super(t.getMessage(), t.getCause());
    }
    public IOException(String message) {
        super(message);
    }
}
