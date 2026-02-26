package com.ownwn.server.java.lang.replacement;

import com.ownwn.server.sockets.FFIHelper;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

public final class File {
    private final String path;

    public File(String path) {
        this.path = path;
    }

    public boolean isDirectory() {
        return listFiles().length != 0; // cant cache this, since it might change between calls!
    }

    public boolean exists() {
        try (Arena a = Arena.ofConfined()) {
            FFIHelper ffiHelper = FFIHelper.of();
            try {
                MemorySegment fileNameMemory = a.allocateFrom(path);
                int res = (int) ffiHelper.callFunction("access", ValueLayout.JAVA_INT, List.of(ValueLayout.ADDRESS, ValueLayout.JAVA_INT), List.of(fileNameMemory, 0));
                return res == 0;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

    public byte[] getAllBytes() {
        try (Arena a = Arena.ofConfined()) {
            FFIHelper ffiHelper = FFIHelper.of();
            try {
                MemorySegment fileNameMemory = a.allocateFrom(path);
                int fd = (int) ffiHelper.callFunction("open", ValueLayout.JAVA_INT, List.of(ValueLayout.ADDRESS, ValueLayout.JAVA_INT), List.of(fileNameMemory, 0));
                if (fd < 0) {
                    throw new RuntimeException("error opening file " + path);
                }

                long size = (long) ffiHelper.callFunction("lseek", ValueLayout.JAVA_LONG, List.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT), List.of(fd, 0L, 2));
                ffiHelper.callFunction("lseek", ValueLayout.JAVA_LONG, List.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT), List.of(fd, 0L, 0));

                MemorySegment buf = a.allocate(size);
                ffiHelper.callFunction("read", ValueLayout.JAVA_LONG, List.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG), List.of(fd, buf, size));

                ffiHelper.callFunction("close", ValueLayout.JAVA_INT, List.of(ValueLayout.JAVA_INT), List.of(fd));

                return buf.toArray(ValueLayout.JAVA_BYTE);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String getName() {
        String[] parts = path.split("/"); // todo this is jank
        return parts[parts.length - 1];
    }

    public File[] listFiles() { // todo proper arena shit
        List<File> files = new ArrayList<>();
        try (Arena a = Arena.ofConfined()) {
            FFIHelper ffiHelper = FFIHelper.of();
            MemorySegment DIR_p = a.allocate(8);
            MemorySegment dirName = a.allocateFrom(path);

            try {
                DIR_p = (MemorySegment) ffiHelper.callFunction("opendir", ValueLayout.ADDRESS, List.of(ValueLayout.ADDRESS), List.of(dirName));
                if (DIR_p.equals(MemorySegment.NULL)) {
                    return new File[0];
                }

                MemorySegment dirent_p;

                while (!(dirent_p = (MemorySegment) ffiHelper.callFunction("readdir", ValueLayout.ADDRESS, List.of(ValueLayout.ADDRESS), List.of(DIR_p))).equals(MemorySegment.NULL)) {
                    MemorySegment dirent = dirent_p.reinterpret(280);
                    String fileName = dirent.getString(19);
                    if (fileName.equals(".") || fileName.equals("..")) continue;

                    String newFileName = path.endsWith("/") ? path + fileName : path + "/" + fileName;
                    files.add(new File(newFileName));
                }
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }

            return files.toArray(new File[files.size()]);
        }
    }

    public Path toPath() {
        return Path.of(this.path);
    }

    @Override
    public String toString() {
        return "File[" + path + "]";
    }

    public String path() {
        return path;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (File) obj;
        return Objects.equals(this.path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }
}
