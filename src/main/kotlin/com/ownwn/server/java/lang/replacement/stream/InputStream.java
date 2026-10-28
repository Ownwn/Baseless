package com.ownwn.server.java.lang.replacement.stream;

import java.io.IOException;

public interface InputStream {

    int read() throws IOException;

    void close() throws IOException;

    default int readNBytes(byte[] buf, int offset, int length) throws IOException {
        int read;
        int i = 0;
        while (i < length && (read = read()) != -1) { // need to keep the length on the left short circuit eval
            buf[offset + i++] = (byte) (read & 0xff);
        }
        return i;
    }

    /** Does not close! */
    default long transferTo(OutputStream outputStream) throws IOException {
        long transferred = 0;

        byte[] buf = new byte[1024];

        int read;
        while ((read = readNBytes(buf, 0, 1024)) > 0) { // todo could fail transfer if it reads 0 but is not done?
            outputStream.write(buf, read);
            transferred+= read;
        }

        return transferred;
    }

    default java.io.InputStream java() {
        return new java.io.InputStream() {
            @Override
            public int read() throws IOException {
                return InputStream.this.read();
            }

            @Override
            public long transferTo(java.io.OutputStream out) throws IOException {
                return super.transferTo(out);
            }
        };
    }
}
