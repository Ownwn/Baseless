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

    public static byte[] copyOfLength(byte[] original, int from, int length) {
        byte[] res = new byte[length];
        for (int i = 0; i < length; i++) {
            res[i] = original[from + i];
        }
        return res;
    }

    public static <T> Stream<T> stream(T[] arr) { // todo crap
        List<T> l = new ArrayList<>(arr.length);
        l.addAll(Arrays.asList(arr));
        return l.stream();
    }

    public static <T> List<T> asList(T[] arr) {
        List<T> res = new ArrayList<>(arr.length);
        for (T t : arr) {
            res.add(t);
        }
        return res;
    }

    public static <A, B> boolean equals(A[] arr1, int arr1Offset, B[] arr2, int arr2Offset, int length) {
        for (int i = 0; i < length; ++i) {
            if (!Objects.equals(arr1[arr1Offset + i], arr2[arr2Offset + i])) {
                return false;
            }
        }
        return true;
    }

    public static <A, B> boolean equals(A[] arr1, B[] arr2) {
        if (arr1.length != arr2.length) return false;
        return equals(arr1, 0, arr2, 0, arr1.length);
    }

    public static <A> boolean equals(A[] arr1, byte[] arr2) {
        if (arr1.length != arr2.length) return false;
        if (!(arr1 instanceof Byte[] arr)) return false;
        byte[] arr1Copy = new byte[arr1.length];
        for (int i = 0; i < arr1.length; i++) {
            arr1Copy[i] = arr[i];
        }

        return equals(arr1Copy, arr2);
    }

    public static boolean equals(byte[] arr1, byte[] arr2) {
        if (arr1.length != arr2.length) return false;
        for (int i = 0; i < arr1.length; i++) {
            if (arr1[i] != arr2[i]) return false;
        }
        return true;
    }

    public static boolean equals(byte[] arr1, int arr1Offset, byte[] arr2, int arr2Offset, int length) {
        for (int i = 0; i < length; i++) {
            if (arr1[arr1Offset + i] != arr2[arr2Offset + i]) return false;
        }
        return true;
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
