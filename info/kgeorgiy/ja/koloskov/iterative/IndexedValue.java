package info.kgeorgiy.ja.koloskov.iterative;

/**
 * A helper data structure used in {@link IndexedListWorker} to store an element
 * and its associated index.
 *
 * @param <T> the type of the value stored in the indexed value
 */
public record IndexedValue<T>(int index, T value) {}
