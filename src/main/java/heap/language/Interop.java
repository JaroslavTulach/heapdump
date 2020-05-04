package heap.language;

import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;

/**
 * Provides utility methods for manipulating values transferred between languages.
 */
interface Interop {

    /**
     * Ensure that arguments have the expected length.
     */
    static void checkArity(Object[] arguments, int expected) throws ArityException {
        if (arguments.length != expected) {
            throw ArityException.create(expected, arguments.length);
        }
    }

    /**
     * Check if the provided interop object represents a null value.
     */
    static boolean isNull(@NullAllowed Object obj) {
        if (obj == null) {
            return true;
        } else if (obj instanceof TruffleObject) {
            InteropLibrary interop = InteropLibrary.getFactory().getUncached();
            return interop.isNull(obj);
        }
        return false;
    }

    /**
     * Takes an interop value and treats it as a string if possible.
     */
    static String asString(@NonNull Object strObject) {
        if (strObject instanceof String) {
            return (String) strObject;
        } else if (strObject instanceof TruffleObject) {
            InteropLibrary interop = InteropLibrary.getFactory().getUncached();
            if (interop.isString(strObject)) {
                try {
                    return interop.asString(strObject);
                } catch (UnsupportedMessageException e) {
                    throw new IllegalStateException("Argument is string but does not implement `asString`.", e);
                }
            }
        }
        throw new ClassCastException("Expected string-like argument, but got: "+strObject);
    }

}
