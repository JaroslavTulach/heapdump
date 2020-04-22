package heap.language.util;

import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.lib.profiler.heap.Instance;

/**
 * {@code InstanceWrapper} is just a supertype of objects which hold a reference to some type of {@link Instance}
 * and provides access to the underlying value.
 *
 * @param <T> A specific type of {@link Instance} held in this wrapper.
 */
public abstract class InstanceWrapper<T extends Instance> {

    @NonNull
    protected final T instance;

    protected InstanceWrapper(@NonNull T instance) {
        this.instance = instance;
    }

    @NonNull
    public T getInstance() {
        return instance;
    }

}
