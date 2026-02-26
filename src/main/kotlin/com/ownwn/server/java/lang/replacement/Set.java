package com.ownwn.server.java.lang.replacement;

import com.ownwn.server.java.lang.replacement.stream.Stream;

import java.util.Iterator;

public interface Set<T> extends Collection<T> {

    int size();
    boolean isEmpty();
    boolean contains(Object o);
    Iterator<T> iterator();
    Object[] toArray();
    <T1> T1[] toArray(T1[] a);
    boolean add(T t);
    boolean remove(Object o);
    boolean containsAll(Collection<?> c);
    boolean addAll(Collection<? extends T> c);
    void clear();

    Stream<T> stream();
}
