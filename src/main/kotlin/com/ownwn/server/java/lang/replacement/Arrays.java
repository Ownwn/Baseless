package com.ownwn.server.java.lang.replacement;

import com.ownwn.server.java.lang.replacement.stream.Stream;

public class Arrays {

    public static Object[] copyOf(Object[] o, int newSize) {
        Object[] res = new Object[newSize];
        for (int i = 0; i < o.length; i++) {
            res[i] = o[i];
        }
        return res;
    }

    public static byte[] copyOfRange(byte[] original, int from, int to) {
        int range = to-from;
        byte[] res = new byte[range];
        for (int i = 0; i < range; i++) {
            res[i] = original[from + i];
        }
        return res;
    }

    public static <T> Stream<T> stream(T[] arr) { // todo crap
        List<T> l = new ArrayList<>(arr.length);
        l.addAll(java.util.Arrays.asList(arr));
        return l.stream();
    }

    public static String toString(Object[] arr) {
        StringBuilder res = new StringBuilder();
        res.append('[');
        for (var o : arr) {
            res.append(o.toString()).append(", ");
        }
        if (arr.length != 0) res.delete(res.length()-2, res.length());
        return res.append(']').toString();
    }
}
