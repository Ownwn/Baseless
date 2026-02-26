package com.ownwn.server.java.lang.replacement.stream;

import com.ownwn.server.java.lang.replacement.*;
import com.ownwn.server.java.lang.replacement.function.*;

import java.util.stream.Collector;

public class Stream<T> {
    protected List<T> underlying;

    static <T> Stream<T> empty() {
        return new Stream<>();
    }


    protected Stream(Collection<T> col) {
        underlying = new ArrayList<>(col);
    }

    protected Stream() {
        underlying = new ArrayList<>();
    }

    
    public Stream<T> filter(Predicate<? super T> predicate) {
        return new Stream<>() {
            {
                for (var t : Stream.this.underlying) {
                    if (predicate.test(t)) {
                        underlying.add(t);
                    }
                }
            }
        };
    }

    public <R> Stream<R> map(Function<? super T, ? extends R> mapper) {
        return new Stream<R>() {
            {
                for (var t : Stream.this.underlying) {
                    underlying.add(mapper.apply(t));
                }
            }
        };
    }

    
    public <R> java.util.stream.Stream<R> flatMap(Function<? super T, ? extends java.util.stream.Stream<? extends R>> mapper) {
        return java.util.stream.Stream.empty();
    }

    
    public Stream<T> distinct() {
        Set<T> set = new HashSet<>(underlying);
        underlying = new ArrayList<>(set);
        return this;
    }

    
    public Stream<T> sorted() {
        return null;
    }

    
    public Stream<T> sorted(Comparator<? super T> comparator) {
        underlying.sort(comparator);
        return this;
    }

    
    public Stream<T> peek(Consumer<? super T> action) {
        for (T t : underlying) action.accept(t);
        return this;
    }

    
    public Stream<T> limit(long maxSize) {
        underlying = underlying.subList(0, (int) maxSize);
        return this;
    }

    
    public Stream<T> skip(long n) {
        underlying = underlying.subList(1, underlying.size());
        return this;
    }

    
    public void forEach(Consumer<? super T> action) {
        for (T t : underlying) action.accept(t);
    }

    
    public void forEachOrdered(Consumer<? super T> action) {

    }

    
    
    public Object[] toArray() {
        return new Object[0];
    }


    public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
        R res = supplier.get();
        for (T t : underlying) {
            accumulator.accept(res, t);
        }
        return res;
    }

    
    public <R, A> R collect(Collector<? super T, A, R> collector) {
        A res = collector.supplier().get();
        for (T t : underlying) {
            collector.accumulator().accept(res, t);
        }
        return collector.finisher().apply(res);
    }

    public List<T> toList() {
        return underlying; // todo
    }

    
    
    public Optional<T> min(Comparator<? super T> comparator) {
        return max(comparator.reversed());
    }

    
    
    public Optional<T> max(Comparator<? super T> comparator) {
        if (underlying.isEmpty()) return Optional.empty();
        T best = underlying.getFirst();
        for (int i = 1; i < underlying.size(); i++) {
            best = comparator.compare(best, underlying.get(i)) >= 0 ? best : underlying.get(i);
        }
        return Optional.of(best);
    }

    
    public long count() {
        return underlying.size();
    }

    
    public boolean anyMatch(Predicate<? super T> predicate) {
        for (T t : underlying) {
            if (predicate.test(t)) return true;
        }
        return false;
    }

    
    public boolean allMatch(Predicate<? super T> predicate) {
        for (T t : underlying) {
            if (!predicate.test(t)) return false;
        }
        return true;
    }

    
    public boolean noneMatch(Predicate<? super T> predicate) {
        for (T t : underlying) {
            if (predicate.test(t)) return false;
        }
        return true;
    }

    
    
    public Optional<T> findFirst() {
        if (underlying.isEmpty()) return Optional.empty();
        return Optional.of(underlying.getFirst());
    }

    
    
    public Optional<T> findAny() {
        if (underlying.isEmpty()) return Optional.empty();
        return Optional.of(underlying.getFirst());
    }

    public String collect(Object o) {
        throw new Error("NYI");
    }

    
    public java.util.stream.Stream<T> sequential() {
        return java.util.stream.Stream.empty();
    }

    
    
    public java.util.stream.Stream<T> parallel() {
        return java.util.stream.Stream.empty();
    }

    
    
    public java.util.stream.Stream<T> unordered() {
        return java.util.stream.Stream.empty();
    }

    
    
    public java.util.stream.Stream<T> onClose( Runnable closeHandler) {
        return java.util.stream.Stream.empty();
    }

    
    public void close() {

    }
}
