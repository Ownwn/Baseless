package com.ownwn.server.java.lang.replacement;

public class Objects {

    public static boolean equals(Object a, Object b) {
        return (a == b) || (a != null && a.equals(b));
    }

    public static <T> T requireNonNull(T obj) {
        if (obj == null) throw new NullPointerException();
        return obj;
    }

    public static int hash(Object o) {
        if (o == null) return 0;
        return o.hashCode();
    }
}
