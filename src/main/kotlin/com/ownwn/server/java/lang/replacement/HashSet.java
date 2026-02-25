package com.ownwn.server.java.lang.replacement;

import org.jetbrains.annotations.NotNull; // todo remove this shit

import java.util.Collection;
import java.util.Iterator;

public class HashSet<T> implements Set<T> {
    private HashMap<T, Object> map;
    private static final Object value = new Object();

    public HashSet() {
        map = new HashMap<>();
    }

    public HashSet(Collection<T> col) {
        map = new HashMap<>();
        for (T t : col) {
            add(t);
        }
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    @Override
    public Iterator<T> iterator() {
        List<T> iterList = new ArrayList<>(size());
        for (var bucket : map.buckets) {
            for (var entry : bucket) {
                iterList.add(entry.getKey());
            }
        }
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return !iterList.isEmpty();
            }

            @Override
            public T next() {
                return iterList.removeLast();
            }
        };
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return new Object[0];
    }

    @NotNull
    @Override
    public <T1> T1[] toArray(@NotNull T1[] a) {
        return null;
    }

    @Override
    public boolean add(T t) {
        return map.put(t, value) == null;
    }

    @Override
    public boolean remove(Object o) {
        return map.remove(o) != null;
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        for (var item : c) {
            if (!map.containsKey(item)) return false;
        }
        return true;
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends T> c) {
        for (var item : c) {
            map.put(item, value);
        }
        return true;
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        return false;
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        return false;
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder("[");
        var keySet = map.keySet();
        for (var key : keySet) {
            res.append(key.toString());
            res.append(", ");
        }
        if (size() > 0)res.delete(res.length()-2, res.length());
        res.append("]");
        return res.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof java.util.HashSet<?> set)) return false;
        if (size() != set.size()) return false;
        return set.containsAll(this) && this.containsAll(set);
    }
}
