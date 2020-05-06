package com.oracle.truffle.heap;

import com.oracle.truffle.api.interop.TruffleObject;
import org.netbeans.lib.profiler.heap.Instance;

import java.util.Iterator;

/**
 * Just a simple wrapper around normal {@link Iterator} of {@link org.netbeans.lib.profiler.heap.Instance} objects
 * which automatically converts them to valid truffle objects.
 */
public class IteratorObjectInstance implements Iterator<TruffleObject> {

    private final Iterator<Instance> iterator;

    public IteratorObjectInstance(Iterator<Instance> iterator) {
        this.iterator = iterator;
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public TruffleObject next() {
        return ObjectInstance.create(iterator.next());
    }

}
