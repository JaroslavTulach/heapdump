package com.oracle.truffle.heap;

import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * <p>A very simple iterator which implements filter (we have it as explicit class because it is actually used
 * in multiple places).</p>
 */
abstract class IteratorFilter<T> implements Iterator<Object> {

    @NonNull
    private final Iterator<T> iterator;

    @NullAllowed
    private Object nextValid = null;

    public IteratorFilter(@NonNull Iterator<T> iterator) {
        this.iterator = iterator;
        findNext(true); // returns null
    }

    @Override
    public boolean hasNext() {
        return nextValid != null;
    }

    @Override
    public Object next() {
        return findNext(false);  // move iterator and return previous value
    }

    // Discard current nextValid value and try to find next one in the underlying iterator.
    // Returns the previous valid value.
    private Object findNext(boolean first) {
        Object original = nextValid;
        while (iterator.hasNext()) {
            Object item = check(iterator.next());
            if (item != null) {
                nextValid = item;
                return original;
            }
        }
        nextValid = null;
        if (original == null && !first) throw new NoSuchElementException();
        return original;
    }

    // check if item is valid - return null if not (this allows object transformation if needed
    public abstract Object check(T item);

}
