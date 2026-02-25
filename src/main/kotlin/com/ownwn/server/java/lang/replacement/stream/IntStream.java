package com.ownwn.server.java.lang.replacement.stream;

import com.ownwn.server.java.lang.replacement.ArrayList;
import com.ownwn.server.java.lang.replacement.List;
import com.ownwn.server.java.lang.replacement.function.Function;

public class IntStream {
    int[] internal;
    public static IntStream range(int startInclusive, int endExclusive) {
        int len = endExclusive - startInclusive;

        return new IntStream() {{
            internal = new int[len];
            for (int i = startInclusive; i < endExclusive; i++) {
                internal[i] = i;
            }
        }};
    }

    public <T> Stream<T> mapToObj(Function<Integer, T> mapper) {
        List<T> res = new ArrayList<>();
        for (int i : internal) {
            res.add(mapper.apply(i));
        }
        return res.stream();
    }
}
