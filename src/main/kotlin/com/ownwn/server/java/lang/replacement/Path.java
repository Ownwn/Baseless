package com.ownwn.server.java.lang.replacement;

public class Path {
    private final File file;

    private Path(String file) {
        this.file = new File(file);
    }

    public static Path of(String s) {
        return new Path(s);

    }
}
