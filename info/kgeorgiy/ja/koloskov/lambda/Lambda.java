package info.kgeorgiy.ja.koloskov.lambda;

import info.kgeorgiy.java.advanced.lambda.AdvancedLambda;
import info.kgeorgiy.java.advanced.lambda.Trees.Binary;
import info.kgeorgiy.java.advanced.lambda.Trees.Nary;
import info.kgeorgiy.java.advanced.lambda.Trees.SizedBinary;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;


public class Lambda implements AdvancedLambda {

    @Override
    public <T> Spliterator<T> nestedBinaryTreeSpliterator(final Binary<List<T>> tree) {
        return new NewNestedBinary<>(tree);
    }

    @Override
    public <T> Spliterator<T> nestedSizedBinaryTreeSpliterator(final SizedBinary<List<T>> tree) {
        return new NewNestedSizedBinary<>(tree);
    }

    @Override
    public <T> Spliterator<T> nestedNaryTreeSpliterator(final Nary<List<T>> tree) {
        return new NewNestedNary<>(tree);
    }

    @Override
    public <T> Collector<T, ?, List<T>> head(final int k) {
        return collectLimit(k, true);
    }

    @Override
    public <T> Collector<T, ?, List<T>> tail(final int k) {
        return collectLimit(k, false);
    }

    @Override
    public <T> Collector<T, ?, Optional<T>> kth(final int k) {
        return Collector.of(
                () -> new Object() {
                    T element = null;
                    int index = -1;
                },
                (state, element) -> {
                    if (state.index < k) {
                        state.index++;
                        state.element = element;
                    }
                },
                (left, right) -> {
                    throw new UnsupportedOperationException("Parallel streams not supported");
                },
                state -> state.index == k
                        ? Optional.ofNullable(state.element)
                        : Optional.empty()
        );
    }

    private <T> Collector<T, ?, List<T>> collectLimit(final int k, final boolean isHead) {
        return Collector.of(
                LinkedList::new,
                (state, t) -> {
                    if (isHead) {
                        if (state.size() < k) {
                            state.add(t);
                        }
                    } else {
                        state.add(t);
                        if (state.size() > k) {
                            state.removeFirst();
                        }
                    }
                },
                (s1, s2) -> {
                    if (isHead) {
                        s1.addAll(s2.subList(0, Math.min(k - s1.size(), s2.size())));
                        return s1;
                    } else {
                        s2.addAll(0, s1.subList(Math.max(0, s1.size() - k), s1.size()));
                        return s2;
                    }
                },
                Function.identity()
        );
    }

    @Override
    public <T> Spliterator<T> binaryTreeSpliterator(final Binary<T> tree) {
        return new NewBinary<>(tree);
    }

    @Override
    public <T> Spliterator<T> sizedBinaryTreeSpliterator(final SizedBinary<T> tree) {
        return new NewSized<>(tree);
    }

    @Override
    public <T> Spliterator<T> naryTreeSpliterator(final Nary<T> tree) {
        return new NewNary<>(tree);
    }

    @Override
    public <T> Collector<T, ?, Optional<T>> first() {
        return Collectors.reducing((a, b) -> a);
    }

    @Override
    public <T> Collector<T, ?, Optional<T>> last() {
        return Collectors.reducing((a, b) -> b);
    }

    @Override
    public <T> Collector<T, ?, Optional<T>> middle() {
        return Collector.of(
                () -> new Object() {
                    final Deque<T> list = new ArrayDeque<>();
                    int cnt = 0;
                },
                (state, element) -> {
                    state.list.add(element);
                    state.cnt++;
                    if (state.list.size() > (state.cnt - 1) / 2 + 1) {
                        state.list.removeFirst();
                    }
                },
                (left, right) -> {
                    throw new UnsupportedOperationException("Parallel streams not supported");
                },
                state -> state.list.isEmpty()
                        ? Optional.empty()
                        : Optional.of(state.list.getFirst())
        );
    }

    @Override
    public Collector<CharSequence, ?, String> commonPrefix() {
        return common(true);
    }

    @Override
    public Collector<CharSequence, ?, String> commonSuffix() {
        return common(false);
    }

    private Collector<CharSequence, ?, String> common(final boolean isPrefix) {
        return Collector.of(
                StringBuilder::new,
                (sb, cs) -> {
                    if (sb.isEmpty()) {
                        if (isPrefix) {
                            sb.append("$").append(cs);
                        } else {
                            sb.append(cs).append("$");
                        }
                    } else {
                        compareAndRemoveExtra(sb, cs, isPrefix);
                    }
                },
                (sb1, sb2) -> {
                    compareAndRemoveExtra(sb1, sb2, isPrefix);
                    return sb1;
                },
                sb -> isPrefix ? sb.substring(Math.min(sb.length(), 1))
                        : sb.substring(0, Math.max(sb.length() - 1, 0))
        );
    }

    private static void compareAndRemoveExtra(
            final StringBuilder sb,
            final CharSequence cs,
            final boolean isPrefix
    ) {
        int i1 = isPrefix ? 1 : sb.length() - 2;
        int i2 = isPrefix ? 0 : cs.length() - 1;
        final int step = isPrefix ? 1 : -1;

        while (i1 >= 0 && i1 < sb.length() && i2 >= 0 && i2 < cs.length()
                && sb.charAt(i1) == cs.charAt(i2)) {
            i1 += step;
            i2 += step;
        }

        if (isPrefix) {
            sb.setLength(i1);
        } else {
            sb.delete(0, i1 + 1);
        }
    }


    @Override
    public <T> Collector<T, ?, List<T>> distinctBy(final Function<? super T, ?> mapper) {
        return Collector.of(
                LinkedHashMap<Object, T>::new,
                (state, el) -> state.putIfAbsent(mapper.apply(el), el),
                (left, right) -> {
                    left.putAll(right);
                    return left;
                },
                state -> new ArrayList<>(state.values())
        );
    }

    @Override
    public <T> Collector<T, ?, OptionalLong> minIndex(final Comparator<? super T> comparator) {
        return indexCollector(comparator, true);
    }

    @Override
    public <T> Collector<T, ?, OptionalLong> maxIndex(final Comparator<? super T> comparator) {
        return indexCollector(comparator, false);
    }

    private <T> Collector<T, ?, OptionalLong> indexCollector(
            final Comparator<? super T> comparator,
            final boolean findMin
    ) {
        return Collector.of(
                () -> new Object() {
                    T value = null;
                    long index = -1;
                    long size = 0;
                },
                (state, element) -> {
                    if (state.value == null ||
                            comparator.compare(element, state.value) * (findMin ? 1 : -1) < 0) {
                        state.value = element;
                        state.index = state.size;
                    }
                    state.size++;
                },
                (state1, state2) -> {
                    state2.index += state1.size;
                    state2.size += state1.size;

                    if (findMin) {
                        final var temp = state1;
                        state1 = state2;
                        state2 = temp;
                    }

                    if (state1.value != null &&
                            (state2.value == null ||
                                    comparator.compare(state1.value, state2.value) * (findMin ? 1 : -1) < 0)) {
                        return state1;
                    }
                    return state2;
                },
                state -> state.value != null
                        ? OptionalLong.of(state.index)
                        : OptionalLong.empty()
        );
    }
}
