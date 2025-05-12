package info.kgeorgiy.ja.koloskov.arrayset;

import info.kgeorgiy.java.advanced.arrayset.AdvancedSet;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

public class ArraySet<T> extends AbstractSet<T> implements AdvancedSet<T> {

    private final List<T> elements;
    private final Comparator<? super T> comparator;

    public ArraySet() {
        this(List.of(), null);
    }

    private ArraySet(final List<T> elements, final Comparator<? super T> comparator) {
        this.elements = elements;
        this.comparator = comparator;
    }

    public ArraySet(final Collection<? extends T> collection) {
        this(collection, null);
    }

    public ArraySet(
            final Collection<? extends T> collection,
            final Comparator<? super T> comparator
    ) {
        this.elements = Collections.unmodifiableList(new ArrayList<>(getSortedSet(
                collection,
                comparator
        )));
        this.comparator = comparator;
    }

    private SortedSet<? extends T> getSortedSet(
            final Collection<? extends T> collection,
            final Comparator<? super T> comparator
    ) {
        if (collection instanceof final SortedSet<? extends T> set && Objects.equals(
                set.comparator(), comparator)
        ) {
            return set;
        }
        final var set = new TreeSet<T>(comparator);
        set.addAll(collection);
        return set;
    }

    public ArraySet(final Comparator<? super T> comparator) {
        this(List.of(), comparator);
    }

    @Override
    public T lower(final T t) {
        return get(findBoundaryIndex(t, false, true));
    }

    @Override
    public T floor(final T t) {
        return get(findBoundaryIndex(t, true, true));
    }

    @Override
    public T ceiling(final T t) {
        return get(findBoundaryIndex(t, true, false));
    }

    @Override
    public T higher(final T t) {
        return get(findBoundaryIndex(t, false, false));
    }

    @Override
    public T pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T pollLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ArraySet<T> descendingSet() {
        return new ArraySet<>(elements.reversed(), getComparator().reversed());
    }

    @Override
    public Iterator<T> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    public Iterator<T> iterator() {
        return elements.iterator();
    }

    @Override
    public int size() {
        return elements.size();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(final Object o) {
        return Collections.binarySearch(elements, (T) o, getComparator()) >= 0;
    }

    private Comparator<? super T> getComparator() {
        return Collections.reverseOrder(comparator).reversed();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    private T get(final int index) {
        return 0 <= index && index < size() ? elements.get(index) : null;
    }

    private int findBoundaryIndex(final T val, final boolean includeEqual, final boolean moveLeft) {
        final int index = Collections.binarySearch(elements, val, getComparator());
        if (index >= 0) {
            return includeEqual ? index : index + (moveLeft ? -1 : 1);
        }
        return (-index - 1) + (moveLeft ? -1 : 0);
    }

    @Override
    public ArraySet<T> subSet(
            final T fromElement,
            final boolean fromInclusive,
            final T toElement,
            final boolean toInclusive
    ) {
        if (getComparator().compare(fromElement, toElement) > 0) {
            throw new IllegalArgumentException("fromKey > toKey");
        }
        final int from = getFromIndex(fromElement, fromInclusive);
        final int to = getToIndex(toElement, toInclusive);
        return from <= to ? subSet(from, to) : new ArraySet<>(comparator);
    }

    @Override
    public ArraySet<T> headSet(final T toElement, final boolean inclusive) {
        return subSet(0, getToIndex(toElement, inclusive));
    }

    @Override
    public ArraySet<T> tailSet(final T fromElement, final boolean inclusive) {
        return subSet(getFromIndex(fromElement, inclusive), size());
    }

    @Override
    public ArraySet<T> subSet(final T fromElement, final T toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public ArraySet<T> headSet(final T toElement) {
        return headSet(toElement, false);
    }

    private ArraySet<T> subSet(final int fromIndex, final int toIndex) {
        return new ArraySet<>(elements.subList(fromIndex, toIndex), comparator);
    }

    private int getToIndex(final T toElement, final boolean toInclusive) {
        return findBoundaryIndex(toElement, !toInclusive, false);
    }

    @Override
    public ArraySet<T> tailSet(final T fromElement) {
        return tailSet(fromElement, true);
    }

    private int getFromIndex(final T fromElement, final boolean fromInclusive) {
        return findBoundaryIndex(fromElement, fromInclusive, false);
    }

    @Override
    public <V> Map<T, V> asMap(final V v) {
        return new ConstantValueMap<>(this, v);
    }

    @Override
    public Comparator<? super T> comparator() {
        return comparator;
    }

    @Override
    public T first() {
        return elements.getFirst();
    }

    @Override
    public T last() {
        return elements.getLast();
    }
}
