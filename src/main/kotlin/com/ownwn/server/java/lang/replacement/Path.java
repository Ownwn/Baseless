package com.ownwn.server.java.lang.replacement;

import org.jetbrains.annotations.NotNull;

public record Path(File file) {
    public Path(String file) {
        this(new File(file));
    }

    public static Path of(String s) {
        return new Path(s);
    }

    public File toFile() {
        return file;
    }

    @NotNull
    @Override
    public String toString() {
        return file.toString();
    }
}
