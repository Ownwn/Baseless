package com.ownwn.server.java.lang.replacement;

import com.ownwn.server.sockets.FFIHelper;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Objects;

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

    public void getAllBytes() {

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

                    files.add(new File(fileName));
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
