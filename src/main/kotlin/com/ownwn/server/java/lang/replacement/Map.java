package com.ownwn.server.java.lang.replacement;


public interface Map<K, V> extends java.util.Map<K, V> {

    static <K, V> Map<K, V> of() {
        return new HashMap<>(); // todo make immutable
    }

    static <K, V> Map<K, V> of(K k, V v) {
        Map<K, V> map = new HashMap<>();
        map.put(k, v);
        return map;
    }

    @Override
    Set<Entry<K, V>> entrySet();
}
