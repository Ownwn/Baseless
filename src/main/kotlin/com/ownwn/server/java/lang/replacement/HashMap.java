package com.ownwn.server.java.lang.replacement;

import com.ownwn.server.java.lang.replacement.function.Function;

public class HashMap<K, V> implements Map<K, V> { // todo store size field for faster computation
    // todo handle mutable keys, will be slow but need to deal with them
    private static final double loadFactor = 0.75;
    private static final int nullHash = 29283873;
    List<List<Map.Entry<K, V>>> buckets;
    private int numItems = 0;

    public HashMap() {
        this(32);
    }

    public HashMap(int initialCapacity) {
        buckets = new ArrayList<>(initialCapacity);
        for (int i = 0; i < initialCapacity; i++) {
            buckets.add(new ArrayList<>());
        }
    }

    private int hash(Map.Entry<K, V> e) {
        return hash(e.key());
    }

    private int hash(Object o) {
        return o == null ? nullHash : Math.abs(o.hashCode());
    }

    @Override
    public int size() {
        return numItems;
    }

    @Override
    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    @Override
    public boolean containsValue(Object value) {
        for (var bucket : buckets) {
            for (var entry : bucket) {
                if (Objects.equals(entry.getValue(), value)) return true;
            }
        }
        return false;
    }

    @Override
    public V get(Object key) {
        int hash = hash(key);
        var bucket = buckets.get(hash % buckets.size());
        for (var entry : bucket) {
            if (Objects.equals(entry.getKey(), key)) return entry.getValue();
        }
        return null;
    }

    @Override
    public V put(K key, V value) {
        V old = get(key); // might be null, meaning no previous key/entry
        int hash = hash(key);
        var bucket = buckets.get(hash % buckets.size());
        if (old != null) {
            bucket.remove(new Map.Entry<>(key, old));
            numItems--;
        }
        bucket.add(new Map.Entry<>(key, value));
        numItems++;

        checkLoad();

        return old;
    }

    @Override
    public V computeIfAbsent(K k, Function<K, V> f) {
        V v = get(k);
        if (v != null) return v;
        V newValue = f.apply(k);
        put(k, newValue);
        return newValue;
    }

    private void checkLoad() {
        double currentLoad = ((double) numItems) / buckets.size();
        if (currentLoad >= loadFactor) {
            resize();
        }
    }

    private void resize() {
        numItems = 0;
        var oldBuckets = buckets;
        buckets = new ArrayList<>();

        for (int i = 0; i < oldBuckets.size() * 2; i++) {
            buckets.add(new ArrayList<>());
        }

        for (var bucket : oldBuckets) {
            for (var entry : bucket) {
                put(entry.getKey(), entry.getValue());
            }
        }
    }

    @Override
    public V remove(Object key) {
        int hash = hash(key);
        var bucket = buckets.get(hash % buckets.size());
        for (int i = 0; i < bucket.size(); i++) {
            var oldEntry = bucket.get(i);
            if (oldEntry.getKey().equals(key)) {
                bucket.remove(i);
                numItems--;
                return oldEntry.getValue();
            }
        }
        return null;
    }

    @Override
    public void clear() {
        for (var bucket : buckets) {
            bucket.clear();
        }
        numItems = 0;
    }

    @Override
        public Set<K> keySet() {
        var entries = entrySet();
        Set<K> keySet = new HashSet<>();
        for (var entry : entries) {
            keySet.add(entry.getKey());
        }
        return keySet;
    }

    @Override
    public Collection<V> values() {
        var entries = entrySet();
        Set<V> valueSet = new HashSet<>();
        for (var entry : entries) {
            valueSet.add(entry.getValue());
        }
        return valueSet;
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> set = new HashSet<>();

        for (var bucket : buckets) {
            for (var item : bucket) set.add(item);
        }
        return set;
    }
}
