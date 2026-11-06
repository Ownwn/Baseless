package com.ownwn.server.java.lang.replacement;

public interface Iterator<T> {
    boolean hasNext();
    T next();

    default java.util.Iterator<T> java() {
        return new java.util.Iterator<>() {
            @Override
            public boolean hasNext() {
                return Iterator.this.hasNext();
            }

            @Override
            public T next() {
                return Iterator.this.next();
            }
        };
    }

    static <T> Iterator<T> empty() {
        return new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public T next() {
                return null;
            }
        };
    }
}
