package com.ownwn.server.java.lang.replacement;

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

    @Override
    public String toString() {
        return file.toString();
    }
}
