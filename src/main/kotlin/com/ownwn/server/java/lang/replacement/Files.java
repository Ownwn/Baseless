package com.ownwn.server.java.lang.replacement;

import com.ownwn.server.java.lang.replacement.stream.InputStream;
import com.ownwn.server.sockets.FFIHelper;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;

public class Files {
    public static InputStream newInputStream(Path path) throws IOException {
        Arena arena = Arena.ofConfined();
        try {
            FFIHelper ffiHelper = FFIHelper.of();
            MemorySegment fileNameMemory = arena.allocateFrom(path.toFile().path());
            int fd = (int) ffiHelper.callFunction("open", ValueLayout.JAVA_INT, List.of(ValueLayout.ADDRESS, ValueLayout.JAVA_INT), List.of(fileNameMemory, 0));
            if (fd < 0) {
                arena.close();
                throw new IOException("error opening file " + path);
            }

            return new InputStream() {
                @Override
                public int read() throws IOException {
                    try {
                        MemorySegment buf = arena.allocate(1, 1L);
                        long numRead = (long) FFIHelper.of().callFunction("read", ValueLayout.JAVA_LONG, List.of(ValueLayout.JAVA_INT, ADDRESS, ValueLayout.JAVA_LONG), List.of(fd, buf, 1L));
                        if (numRead <= 0) return -1;
                        return buf.get(JAVA_BYTE, 0) & 0xFF;
                    } catch (Throwable e) {
                        throw new IOException(e);
                    }
                }

                @Override
                public void close() throws IOException {
                    try {
                        FFIHelper.of().callFunction("close", ValueLayout.JAVA_INT, List.of(ValueLayout.JAVA_INT), List.of(fd));
                    } catch (Throwable e) {
                        throw new IOException(e);
                    } finally {
                        arena.close();
                    }
                }
            };
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
