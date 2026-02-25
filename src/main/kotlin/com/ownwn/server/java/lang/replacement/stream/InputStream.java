package com.ownwn.server.java.lang.replacement.stream;

import java.io.IOException;

public interface InputStream {

    int read() throws IOException;

    void close() throws IOException;

    default int readNBytes(byte[] buf, int offset, int length) throws IOException {
        int read;
        int i = 0;
        while ((read = read()) != -1 && i < length) {
            buf[offset + i++] = (byte) (read & 0xff);
        }
        return i;
    }

    /** Does not close! */
    default long transferTo(OutputStream outputStream) throws IOException {
        long transferred = 0;

        int read;

        while ((read = read()) != -1) {
            outputStream.write(read);
            transferred++;
        }

        return transferred;
    }
}
