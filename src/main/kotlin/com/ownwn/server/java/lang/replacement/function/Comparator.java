package com.ownwn.server.java.lang.replacement.function;

public interface Comparator<T> {
    int compare(T first, T second);

    default Comparator<T> reversed() {
        return (a, b) -> -compare(a, b);
    }
}
