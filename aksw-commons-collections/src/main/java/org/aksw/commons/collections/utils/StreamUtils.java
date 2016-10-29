package org.aksw.commons.collections.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StreamUtils {

    /**
     * Creates a new stream which upon reaching its end performs and action.
     * It concatenates the original stream with one having a single item
     * that is filtered out again. The action is run as- part of the filter.
     *
     * @param stream
     * @param runnable
     * @return
     */
    public static <T> Stream<T> appendAction(Stream<? extends T> stream, Runnable runnable) {
        Stream<T> result = Stream.concat(
                stream,
                Stream
                    .of((T)null)
                    .filter(x -> {
                        runnable.run();
                        return false;
                    })
                );
        return result;
    }

    // TODO Add to StreamUtils
    public static <S, X> Stream<X> stream(BiConsumer<S, Consumer<X>> fn, S baseSolution) {
        List<X> result = new ArrayList<>();

        fn.accept(baseSolution, (item) -> result.add(item));

        return result.stream();
    }

    public static <T> Stream<T> stream(Iterator<T> iterator) {
    	Iterable<T> i = () -> iterator;
    	Stream<T> result = StreamSupport.stream(i.spliterator(), false);
    	return result;
    }
}
