package com.ownwn.server.java.lang.replacement;

import com.ownwn.server.java.lang.replacement.function.Comparator;
import com.ownwn.server.java.lang.replacement.stream.Stream;

public interface List<T> extends Collection<T> {

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

    List<T> subList(int fromIndex, int toIndex);

    T removeFirst();
    T removeLast();

    default T getFirst() {
        return get(0);
    }

    default T getLast() {
        return get(size() - 1);
    }

    void sort(Comparator<? super T> comparator);

    void clear();

    default java.util.List<T> java() {
        var res = new java.util.ArrayList<T>();
        for (T t : this) {
            res.add(t);
        }
        return res;
    }

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

    T remove(int index);
    int indexOf(Object o);
    int lastIndexOf(Object o);

    default boolean addAll(Collection<? extends T> l) {
        for (T t : l) {
            add(t);
        }
        return true;
    }

    default boolean removeAll(List<?> c) {
        for (var item : c) {
            remove(item);
        }
        return true;
    }

    boolean containsAll(List<?> l);

    default Stream<T> stream() {
        var self = this;
        return new Stream<T>() {{underlying = self;}};
    }
}
