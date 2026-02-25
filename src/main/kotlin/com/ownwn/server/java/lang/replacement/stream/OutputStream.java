package com.ownwn.server.java.lang.replacement.stream;

public interface OutputStream {

    void write(int b);

    void close();

    default void write(byte[] bytes) {
        for (byte b : bytes) {
            write(b);
        }
    }

}
