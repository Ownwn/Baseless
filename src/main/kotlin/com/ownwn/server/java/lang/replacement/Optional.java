package com.ownwn.server.java.lang.replacement;

import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Supplier;

public class Optional<T>  {
    private static final Optional<Object> EMPTY = new Optional<>();
    public static <T> Optional<T> empty() {
        //noinspection unchecked
        return (Optional<T>) EMPTY;
    }

    private final T t;

    private Optional(T t) {
        this.t = t;
    }
    private Optional() {
        this(null);
    }

    public static <T> Optional<T> of(T t) {
        if (t == null) throw new IllegalArgumentException("t cannot be null");
        return new Optional<>(t);
    }

    public static <T> Optional<T> ofNullable(T t) {
        if (t == null) return Optional.empty();
        return new Optional<>(t);
    }

    public T get() {
        if (t == null) throw new NoSuchElementException();
        return t;
    }

    public <R> Optional<R> map(Function<T, R> mapper) {
        return new Optional<>(mapper.apply(t));
    }

    public T orElseThrow(Supplier<Throwable> supplier) throws Throwable {
        if (t == null) throw supplier.get();
        return t;
    }

    public T orElse(T alt) {
        if (t == null) return alt;
        return t;
    }

    public boolean isPresent() {
        return t != null;
    }

    @Override
    public String toString() {
        return "Optional[" + t + "]";
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof java.util.Optional<?> optional && optional.isPresent() && isPresent() && optional.get().equals(get());
    }

    @Override
    public int hashCode() {
        if (t == null) return 0;
        return t.hashCode();
    }
}
