package com.ownwn.server.java.lang.replacement.stream;


import com.ownwn.server.java.lang.replacement.IOException;

public interface OutputStream {

    void write(int b) throws IOException;

    void close() throws IOException;

    default void write(byte[] bytes) throws IOException {
        write(bytes, bytes.length);
    }

    default void write(byte[] bytes, int len) throws IOException {
        for (int i = 0; i < len; i++) {
            write(bytes[i]);
        }
    }

    static OutputStream fromJava(java.io.OutputStream os) {
        return new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                try {
                    os.write(b);
                } catch (java.io.IOException e) {
                    throw new IOException(e);
                }
            }

            @Override
            public void close() throws IOException {
                try {
                    os.close();
                } catch (java.io.IOException e) {
                    throw new IOException(e);
                }
            }
        };
    }
}
