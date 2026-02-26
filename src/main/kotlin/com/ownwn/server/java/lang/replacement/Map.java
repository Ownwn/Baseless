package com.ownwn.server.java.lang.replacement;


import com.ownwn.server.java.lang.replacement.function.Function;

public interface Map<K, V>{
    static <K, V> Map<K, V> of() {
        return new HashMap<>(); // todo make immutable
    }

    static <K, V> Map<K, V> of(K k, V v) {
        Map<K, V> map = new HashMap<>();
        map.put(k, v);
        return map;
    }

    Set<Entry<K, V>> entrySet();

    V put(K k, V value);

    int size();

    default boolean isEmpty() {
        return size() == 0;
    }

    V computeIfAbsent(K k, Function<K, V> f);

    boolean containsValue(Object value);

    boolean containsKey(Object key);
    V get(Object key);

    V remove(Object key);

    void clear();

    Set<K> keySet();

    Collection<V> values();

    default java.util.Map<K, V> java() {
        var res = new java.util.HashMap<K, V>();
        for (var e : this.entrySet()) {
            res.put(e.getKey(), e.getValue());
        }
        return res;
    }




    record Entry<K, V>(K key, V value) {
        public K getKey() {
            return key;
        }
        public V getValue() {
            return value;
        }
    }
}
