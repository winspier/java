package info.kgeorgiy.ja.koloskov.arrayset;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

public class ConstantValueMap<K, V> extends AbstractMap<K, V> {

    private final Set<K> keys;
    private final V value;

    public ConstantValueMap(final Set<K> keys, final V value) {
        this.keys = keys;
        this.value = value;
    }

    @Override
    @SuppressWarnings("SuspiciousMethodCalls")
    public boolean containsKey(final Object key) {
        return keys.contains(key);
    }

    @Override
    public Set<K> keySet() {
        return keys;
    }

    @Override
    public Collection<V> values() {
        return Collections.nCopies(size(), value);
    }

    @Override
    public boolean containsValue(final Object value) {
        return size() > 0 && Objects.equals(value, this.value);
    }

    @Override
    public V get(final Object key) {
        return containsKey(key) ? value : null;
    }

    @Override
    public int size() {
        return keys.size();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return new AbstractSet<>() {
            @Override
            public Iterator<Entry<K, V>> iterator() {
                final var it = keys.iterator();
                return new Iterator<>() {

                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public Entry<K, V> next() {
                        return new AbstractMap.SimpleImmutableEntry<>(it.next(), value);
                    }
                };
            }

            @Override
            public int size() {
                return ConstantValueMap.this.size();
            }
        };
    }

}
