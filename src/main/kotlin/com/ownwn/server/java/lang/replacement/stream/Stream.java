package com.ownwn.server.java.lang.replacement.stream;

import com.ownwn.server.java.lang.replacement.ArrayList;
import com.ownwn.server.java.lang.replacement.HashSet;
import com.ownwn.server.java.lang.replacement.List;
import com.ownwn.server.java.lang.replacement.Set;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import java.util.function.Consumer;

public class Stream<T> implements java.util.stream.Stream<T> {
    protected List<T> underlying;

    static <T> Stream<T> empty() {
        return new Stream<>();
    }


    protected Stream(List<T> col) {
        underlying = new ArrayList<>(col);
    }

    protected Stream() {
        underlying = new ArrayList<>();
    }

    @Override
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

    @Override
    public IntStream mapToInt(ToIntFunction<? super T> mapper) {
        return IntStream.empty();
    }

    @Override
    public LongStream mapToLong(ToLongFunction<? super T> mapper) {
        return LongStream.empty();
    }

    @Override
    public DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper) {
        return DoubleStream.empty();
    }

    @Override
    public <R> java.util.stream.Stream<R> flatMap(Function<? super T, ? extends java.util.stream.Stream<? extends R>> mapper) {
        return java.util.stream.Stream.empty();
    }

    @Override
    public IntStream flatMapToInt(Function<? super T, ? extends IntStream> mapper) {
        return IntStream.empty();
    }

    @Override
    public LongStream flatMapToLong(Function<? super T, ? extends LongStream> mapper) {
        return LongStream.empty();
    }

    @Override
    public DoubleStream flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper) {
        return DoubleStream.empty();
    }

    @Override
    public Stream<T> distinct() {
        Set<T> set = new HashSet<>(underlying);
        underlying = new ArrayList<>(set);
        return this;
    }

    @Override
    public Stream<T> sorted() {
        return null;
    }

    @Override
    public Stream<T> sorted(Comparator<? super T> comparator) {
        underlying.sort(comparator);
        return this;
    }

    @Override
    public Stream<T> peek(Consumer<? super T> action) {
        for (T t : underlying) action.accept(t);
        return this;
    }

    @Override
    public Stream<T> limit(long maxSize) {
        underlying = underlying.subList(0, (int) maxSize);
        return this;
    }

    @Override
    public Stream<T> skip(long n) {
        underlying = underlying.subList(1, underlying.size());
        return this;
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        for (T t : underlying) action.accept(t);
    }

    @Override
    public void forEachOrdered(Consumer<? super T> action) {

    }

    @NotNull
    @Override
    public Object[] toArray() {
        return new Object[0];
    }

    @NotNull
    @Override
    public <A> A[] toArray(IntFunction<A[]> generator) {
        return null;
    }

    @Override
    public T reduce(T identity, BinaryOperator<T> accumulator) {
        T res = underlying.getFirst();
        for (int i = 1; i < underlying.size(); i++) {
            res = accumulator.apply(res, underlying.get(i));
        }
        return res;
    }

    @NotNull
    @Override
    public Optional<T> reduce(BinaryOperator<T> accumulator) {
        if (underlying.isEmpty()) return Optional.empty();
        T res = underlying.getFirst();
        for (int i = 1; i < underlying.size(); i++) {
            res = accumulator.apply(res, underlying.get(i));
        }
        return Optional.of(res);
    }

    @Override
    public <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner) {
        U current = identity;
        for (T t : underlying) {
            current = accumulator.apply(current, t);
        }
        return current;
    }

    @Override
    public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
        R res = supplier.get();
        for (T t : underlying) {
            accumulator.accept(res, t);
        }
        return res;
    }

    @Override
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

    @NotNull
    @Override
    public Optional<T> min(Comparator<? super T> comparator) {
        return max(comparator.reversed());
    }

    @NotNull
    @Override
    public Optional<T> max(Comparator<? super T> comparator) {
        if (underlying.isEmpty()) return Optional.empty();
        T best = underlying.getFirst();
        for (int i = 1; i < underlying.size(); i++) {
            best = comparator.compare(best, underlying.get(i)) >= 0 ? best : underlying.get(i);
        }
        return Optional.of(best);
    }

    @Override
    public long count() {
        return underlying.size();
    }

    @Override
    public boolean anyMatch(Predicate<? super T> predicate) {
        for (T t : underlying) {
            if (predicate.test(t)) return true;
        }
        return false;
    }

    @Override
    public boolean allMatch(Predicate<? super T> predicate) {
        for (T t : underlying) {
            if (!predicate.test(t)) return false;
        }
        return true;
    }

    @Override
    public boolean noneMatch(Predicate<? super T> predicate) {
        for (T t : underlying) {
            if (predicate.test(t)) return false;
        }
        return true;
    }

    @NotNull
    @Override
    public Optional<T> findFirst() {
        if (underlying.isEmpty()) return Optional.empty();
        return Optional.of(underlying.getFirst());
    }

    @NotNull
    @Override
    public Optional<T> findAny() {
        if (underlying.isEmpty()) return Optional.empty();
        return Optional.of(underlying.getFirst());
    }

    public String collect(Object o) {
        throw new Error("NYI");
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return underlying.iterator();
    }

    @NotNull
    @Override
    public Spliterator<T> spliterator() {
        return null;
    }

    @Override
    public boolean isParallel() {
        return false;
    }

    @NotNull
    @Override
    public java.util.stream.Stream<T> sequential() {
        return java.util.stream.Stream.empty();
    }

    @NotNull
    @Override
    public java.util.stream.Stream<T> parallel() {
        return java.util.stream.Stream.empty();
    }

    @NotNull
    @Override
    public java.util.stream.Stream<T> unordered() {
        return java.util.stream.Stream.empty();
    }

    @NotNull
    @Override
    public java.util.stream.Stream<T> onClose(@NotNull Runnable closeHandler) {
        return java.util.stream.Stream.empty();
    }

    @Override
    public void close() {

    }
}
