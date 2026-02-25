package com.ownwn.server.java.lang.replacement;

import com.ownwn.server.java.lang.replacement.stream.Stream;

public interface List<T> extends Iterable<T>, java.util.List<T> {

    int size();

    boolean isEmpty();

    boolean contains(Object o);

    boolean add(T t);

    boolean remove(Object o);

    int hashCode();

    T get(int index);

    T set(int index, T elem);

    void add(int index, T elem);

    T[] toArray();

    T[] toArray(Object[] arr);

    @Override
    List<T> subList(int fromIndex, int toIndex);

    @SafeVarargs // todo bad?
    static <T> List<T> of(T... values) {
        List<T> res = new ArrayList<>() { // todo need to make add() immutable, but then cant add ~20 lines below!
            @Override
            public T set(int index, T elem) {
                throw new UnsupportedOperationException("Cannot modify immutable list");
            }

            @Override
            public boolean remove(Object o) {
                throw new UnsupportedOperationException("Cannot modify immutable list");
            }

            @Override
            public void add(int index, Object elem) {
                throw new UnsupportedOperationException("Cannot modify immutable list");
            }
        };
        for (var value : values) {
            res.add(value);
        }
        return res;
    }

    @Override
    default Stream<T> stream() {
        var self = this;
        return new Stream<T>() {{underlying = self;}};
    }
}
