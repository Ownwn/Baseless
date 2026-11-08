package com.ownwn.server.java.lang.replacement.stream;


import com.ownwn.server.java.lang.replacement.IOException;

public interface OutputStream {

    void write(int b) throws IOException;

    void close() throws IOException;

    default void write(byte[] bytes) throws IOException {
        write(bytes, bytes.length);
    }

    void write(byte[] bytes, int len) throws IOException;

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
            public void write(byte[] bytes, int len) throws IOException {
                try {
                    os.write(bytes, 0, len);
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
