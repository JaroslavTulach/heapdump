package com.oracle.truffle.heap.interop;

import org.netbeans.api.annotations.common.NonNull;

import java.util.Objects;

public final class IndexPair<I, V> {

    @NonNull
    private final I index;

    @NonNull
    private final V value;

    public IndexPair(@NonNull I index, @NonNull V value) {
        this.index = index;
        this.value = value;
    }

    public I getIndex() {
        return index;
    }

    public V getValue() {
        return value;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        IndexPair<?, ?> indexPair = (IndexPair<?, ?>) object;
        return index.equals(indexPair.index) &&
                value.equals(indexPair.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, value);
    }

    @Override
    public String toString() {
        return "(" + index.toString() + ": " + value.toString() + ")";
    }

}
