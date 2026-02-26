package com.ownwn.server.java.lang.replacement.stream;


import com.ownwn.server.java.lang.replacement.function.*;

public interface Collector<T, A, R> {
    Supplier<A> supplier();

    BiConsumer<A, T> accumulator();

    BinaryOperator<A> combiner();

    Function<A, R> finisher();
}
