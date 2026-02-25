package com.ownwn.server.java.lang.replacement;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class Collectors {

    public static Collector<CharSequence, StringBuilder, String> joining(String del, String prefix, String suffix) {
        return new Collector<>() {
            @Override
            public Supplier<StringBuilder> supplier() {
                return StringBuilder::new;
            }

            @Override
            public BiConsumer<StringBuilder, CharSequence> accumulator() {
                return (sb, cs) -> {
                    if (!sb.isEmpty()) sb.append(del);
                    sb.append(cs);
                };
            }

            @Override
            public BinaryOperator<StringBuilder> combiner() {
                return (o1, o2) -> {
                    if (o1.isEmpty()) return o2;
                    if (o2.isEmpty()) return o1;
                    return o1.append(del).append(o2);
                };
            }

            @Override
            public Function<StringBuilder, String> finisher() {
                return sb -> prefix + sb.toString() + suffix;
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Set.of();
            }
        };
    }
    public static Collector<CharSequence, StringBuilder, String> joining(String del) {
        return joining(del, "", "");
    }

}
