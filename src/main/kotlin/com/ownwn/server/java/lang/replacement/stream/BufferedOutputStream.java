package com.ownwn.server.java.lang.replacement.stream;


import com.ownwn.server.java.lang.replacement.Arrays;
import com.ownwn.server.java.lang.replacement.IOException;


public final class BufferedOutputStream implements OutputStream {
    private final OutputStream outputStream;
    private final byte[] buf;
    private int bufPos = 0;

    public BufferedOutputStream(OutputStream outputStream, int bufSize) {
        this.outputStream = outputStream;
        this.buf = new byte[bufSize];
    }


    @Override
    public void write(int b) throws IOException {
        buf[bufPos++] = (byte) b;
        if (bufPos >= buf.length) {
            flush();
        }
    }

    private void flush() throws IOException {
        if (bufPos != 0) {
            outputStream.write(buf, bufPos);
            bufPos = 0;
        }
    }

    @Override
    public void close() throws IOException {
        try {
            flush();
        } catch (IOException | RuntimeException | Error e) {
            try { outputStream.close(); } catch (Exception closeError) { e.addSuppressed(closeError); }
            throw e;
        }
        outputStream.close();
    }

    @Override
    public void write(byte[] bytes, int len) throws IOException {
        int offset = 0;
        while (offset < len) {
            int count = Math.min(len - offset, buf.length - bufPos);
            Arrays.copyFrom(bytes, offset, buf, bufPos, count);
            bufPos += count;
            offset += count;

            if (bufPos == buf.length) {
                flush();
            }
        }
    }
}
