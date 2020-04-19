package heap.language.util;

import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;

import java.util.Iterator;

public abstract class FilterIterator<T> implements Iterator<Object> {

    @NonNull
    private final Iterator<T> iterator;

    @NullAllowed
    private Object nextValid = null;

    public FilterIterator(@NonNull Iterator<T> iterator) {
        this.iterator = iterator;
        findNext(); // returns null
    }

    @Override
    public boolean hasNext() {
        return nextValid != null;
    }

    @Override
    public Object next() {
        return findNext();  // move iterator and return previous value
    }

    // Discard current nextValid value and try to find next one in the underlying iterator.
    // Returns the previous valid value.
    private Object findNext() {
        Object original = nextValid;
        while (iterator.hasNext()) {
            Object item = check(iterator.next());
            if (item != null) {
                nextValid = item;
                return original;
            }
        }
        nextValid = null;
        return original;
    }

    // check if item is valid - return null if not (this allows object transformation if needed
    public abstract Object check(T item);

}
