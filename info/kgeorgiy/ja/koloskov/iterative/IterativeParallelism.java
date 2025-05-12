package info.kgeorgiy.ja.koloskov.iterative;

import info.kgeorgiy.java.advanced.iterative.AdvancedIP;
import info.kgeorgiy.java.advanced.iterative.ListIP;
import info.kgeorgiy.java.advanced.iterative.ScalarIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Implementation of the {@link AdvancedIP}, {@link ListIP}, and {@link ScalarIP}
 * interfaces for parallel list processing.
 * This class provides various operations that divide a list into multiple tasks,
 * each executed in a separate thread.
 */
public class IterativeParallelism implements AdvancedIP, AutoCloseable {

    private final ParallelMapper worker;
    private static final int DEFAULT_MAX_THREADS = 32;

    /**
     * Constructs an {@code IterativeParallelism} instance with a default number of threads.
     * A new instance of {@link ParallelMapperImpl} will be created using default settings.
     */
    public IterativeParallelism() {
        this.worker = new ParallelMapperImpl(DEFAULT_MAX_THREADS);
    }


    /**
     * Constructs an {@code IterativeParallelism} instance with a given {@link ParallelMapper}.
     *
     * @param worker the mapper to be used for executing tasks in parallel.
     */
    public IterativeParallelism(ParallelMapper worker) {
        this.worker = worker;
    }

    private <T, R> R work(int parts, Function<Stream<? extends T>, R> mapper, Function<Stream<R>, R> combiner, List<? extends T> values) throws InterruptedException {
        int threadsToUse = Math.min(parts, values.size());
        int baseSize = values.size() / threadsToUse;
        int remainder = values.size() % threadsToUse;

        List<Stream<? extends T>> splittedList = new ArrayList<>();
        for (int i = 0, start = 0; i < threadsToUse; i++) {
            int end = start + baseSize + (i < remainder ? 1 : 0);
            splittedList.add(values.subList(start, end).stream());
            start = end;
        }

        List<R> results = worker.map(mapper, splittedList);

        return combiner.apply(results.stream());
    }

    @Override
    public <T> T reduce(int threads, List<T> values, T identity, BinaryOperator<T> operator)
            throws InterruptedException {
        return mapReduce(threads, values, Function.identity(), identity, operator);
    }

    @Override
    public <T, R> R mapReduce(
            int threads,
            List<T> values,
            Function<T, R> lift,
            R identity,
            BinaryOperator<R> operator
    ) throws InterruptedException {
        return work(
                threads,
                sublist -> sublist.map(lift).reduce(identity, operator),
                results -> results.reduce(identity, operator),
                values
        );
    }

    @Override
    public <T> int[] indices(int threads, List<? extends T> values, Predicate<? super T> predicate)
            throws InterruptedException {
        List<Map.Entry<Integer, ? extends T>> pairs = IntStream.range(0, values.size())
                .mapToObj(i -> Map.entry(i, values.get(i)))
                .collect(Collectors.toList());

        List<Integer> resultList = work(
                threads,
                sublist -> sublist
                        .filter(entry -> predicate.test(entry.getValue()))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toList()),
                results -> results
                        .flatMap(List::stream)
                        .sorted()
                        .collect(Collectors.toList()),
                pairs
        );

        return resultList.stream().mapToInt(Integer::intValue).toArray();
    }

    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate)
            throws InterruptedException {
        return work(
                threads,
                list -> list.filter(predicate).collect(Collectors.toList()),
                list -> list
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList()),
                values
        );
    }

    @Override
    public <T, U> List<U> map(
            int threads,
            List<? extends T> values,
            Function<? super T, ? extends U> f
    ) throws InterruptedException {
        if (f == null) {
            throw new NullPointerException("Function cannot be null");
        }
        return work(
                threads,
                list -> list.map(f).collect(Collectors.toList()),
                lists -> lists.flatMap(List::stream).collect(Collectors.toList()),
                values
        );
    }

    @Override
    public <T> int argMax(int threads, List<T> values, Comparator<? super T> comparator)
            throws InterruptedException {
        List<Map.Entry<Integer, T>> entries = IntStream.range(0, values.size())
                .mapToObj(i -> Map.entry(i, values.get(i)))
                .collect(Collectors.toList());

        Comparator<Map.Entry<Integer, T>> entryComparator =
                Entry.comparingByValue(comparator);

        return work(
                threads,
                sublist -> sublist.max(entryComparator).orElse(null),
                partialMaxes -> partialMaxes.filter(Objects::nonNull)
                        .max(entryComparator)
                        .orElseThrow(),
                entries
        ).getKey();

    }

    @Override
    public <T> int argMin(int threads, List<T> values, Comparator<? super T> comparator)
            throws InterruptedException {
        return argMax(threads, values, Collections.reverseOrder(comparator));
    }

    private <T> int indexOfImpl(int threads, List<T> values, Predicate<? super T> predicate, boolean reversed) throws InterruptedException {
        Comparator<Integer> comparator = reversed ? Comparator.reverseOrder() : Comparator.naturalOrder();
        return work(
                threads,
                subIndices -> new IndexedValue<>(0, subIndices
                        .filter(entry -> predicate.test(values.get(entry.index())))
                        .findFirst()
                        .map(IndexedValue::index)
                        .orElse(-1)),
                results -> results
                        .map(IndexedValue::value)
                        .filter(a -> a >= 0)
                        .min(comparator)
                        .map(result -> new IndexedValue<>(0, result))
                        .orElse(new IndexedValue<>(0, -1)),

        reversed?
            reverseIndexedList(values) :
            indexedList(values)
        ).value();
    }

    @Override
    public <T> int indexOf(int threads, List<T> values, Predicate<? super T> predicate)
            throws InterruptedException {
        return indexOfImpl(threads, values, predicate, false);
    }

    @Override
    public <T> int lastIndexOf(int threads, List<T> values, Predicate<? super T> predicate)
            throws InterruptedException {
        return indexOfImpl(threads, values, predicate, true);
    }

    @Override
    public <T> long sumIndices(
            int threads,
            List<? extends T> values,
            Predicate<? super T> predicate
    ) throws InterruptedException {
        return work(
                threads,
                sublist -> new IndexedValue<>(
                        0, sublist
                        .filter(entry -> predicate.test(entry.value()))
                        .mapToLong(IndexedValue::index)
                        .sum()
                ),
                results -> new IndexedValue<>(
                        0, results
                        .mapToLong(IndexedValue::value)
                        .sum()
                ),
            indexedList(values)
        ).value();
    }

    private <T> List<IndexedValue<? extends T>> indexedList(List<? extends T> values) {
        return IntStream.range(0, values.size())
                .mapToObj(i -> new IndexedValue<>(i, values.get(i)))
                .collect(Collectors.toList());
    }

    private <T> List<IndexedValue<? extends T>> reverseIndexedList(List<? extends T> values) {
        return IntStream.range(0, values.size())
                .mapToObj(i -> new IndexedValue<>(values.size() - 1 - i, values.get(i)))
                .collect(Collectors.toList());

    }

    /**
     * Closes the internal {@link ParallelMapper} instance used for parallel execution.
     * Further use of this object will result in {@link IllegalStateException}.
     */
    @Override
    public void close() {
        worker.close();
    }
}
