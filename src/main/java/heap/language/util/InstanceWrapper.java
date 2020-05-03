package heap.language.util;

import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.PrimitiveArrayInstance;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    public static String instanceString(InstanceWrapper<?> receiver) {
        try {
            // Taken from org.netbeans.modules.profiler.oql.engine.api.impl.Snapshot
            if (receiver.instance.getJavaClass().getName().equals(String.class.getName())) {
                Class proxy = Class.forName("org.netbeans.lib.profiler.heap.HprofProxy"); // NOI18N
                Method method = proxy.getDeclaredMethod("getString", Instance.class); // NOI18N
                method.setAccessible(true);
                return (String) method.invoke(proxy, receiver.instance);
            } else if (receiver.instance.getJavaClass().getName().equals("char[]")) { // NOI18N
                Method method = receiver.instance.getClass().getDeclaredMethod("getChars", int.class, int.class);
                method.setAccessible(true);
                char[] chars = (char[])method.invoke(receiver.instance, 0, ((PrimitiveArrayInstance)receiver.instance).getLength());
                if (chars != null) {
                    return new String(chars);
                } else {
                    return "*null*"; // NOI18N
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error getting toString() value of an instance dump", ex);
        }
        return receiver.instance.toString();
    }

}
