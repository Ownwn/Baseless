package com.ownwn.server.sockets;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import com.ownwn.server.java.lang.replacement.*;
import com.ownwn.server.java.lang.replacement.stream.InputStream;
import com.ownwn.server.java.lang.replacement.stream.OutputStream;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

public abstract class Client {
    public abstract InputStream getInputStream(Arena arena);
    public abstract OutputStream getOutputStream(Arena arena);
    public abstract SimpleAddress getInetAddress();

    public static Client fromFileSocketDescriptor(int c) {
        return new Client() {
            @Override
            public InputStream getInputStream(Arena arena) {
                return new InputStream() {
                    @Override
                    public int read() {
                        // todo chunk buf to optimize allocs
                        MemorySegment buf = arena.allocate(1, 1L);

                        try {
                            int numReaded = (int) ((long) FFIHelper.of().callFunction("read", JAVA_LONG, List.of(JAVA_INT, ADDRESS, JAVA_LONG), List.of(c, buf, buf.byteSize())));
                            if (numReaded <= 0) return -1;

                            return buf.get(JAVA_BYTE, 0) & 0xFF;
                        } catch (Throwable e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public int readNBytes(byte[] buf, int offset, int length) throws IOException {
                        try {
                            // struct timeval
                            MemorySegment timeval = arena.allocate(16);
                            timeval.set(JAVA_LONG, 0, 5);
                            timeval.set(JAVA_LONG, 8, 0);
                            FFIHelper.of().callFunction("setsockopt", JAVA_INT,
                                    List.of(JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT),
                                    List.of(c, 1, 20, timeval, 16));

                            int totalRead = 0;
                            while (totalRead < length) {
                                int toReadChunk = Math.min(length - totalRead, 4096);
                                MemorySegment tempBuf = arena.allocate(toReadChunk);

                                long numReaded = (long) FFIHelper.of().callFunction("read", JAVA_LONG, List.of(JAVA_INT, ADDRESS, JAVA_LONG), List.of(c, tempBuf, toReadChunk));

                                if (numReaded < 0) {
                                    return totalRead == 0 ? -1 : totalRead;
                                } else if (numReaded == 0) {
                                    break;
                                } else {
                                    for (int i = 0; i < numReaded; i++) {
                                        buf[offset + totalRead + i] = tempBuf.get(JAVA_BYTE, i);
                                    }
                                    totalRead += numReaded;
                                }
                            }
                            return totalRead;
                        } catch (Throwable e) {
                            throw new IOException(e);
                        }
                    }

                    @Override
                    public void close() {
                        try {
                            FFIHelper.of().callIntFunction("close", JAVA_INT, List.of(c));
                        } catch (Throwable e) {
                            throw new RuntimeException("Cannot close inputstream" + e);
                        }
                    }
                };
            }

            @Override
            public OutputStream getOutputStream(Arena arena) {
                return new OutputStream() {
                    @Override
                    public void write(int b) {
                        MemorySegment resByte = arena.allocateFrom(JAVA_BYTE, (byte) b); // todo optimize chunk size
                        try {
                            FFIHelper.of().callFunction("write", JAVA_LONG, List.of(JAVA_INT, ADDRESS, JAVA_LONG), List.of(c, resByte, 1));
                        } catch (Throwable e) {
                            throw new RuntimeException(e);
                        }

                    }

                    @Override
                    public void close() {
                        try {
                            FFIHelper.of().callIntFunction("close", JAVA_INT, List.of(c));
                        } catch (Throwable e) {
                            throw new RuntimeException("Cannot close outputstream" + e);
                        }
                    }
                };
            }

            @Override
            public SimpleAddress getInetAddress() {
                return new SimpleAddress("123.123.123.123", 8083); // todo
            }
        };
    }
}
