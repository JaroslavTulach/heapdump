package heap.language.util;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

/**
 * A read-only wrapper around array of interop heap.
 */
@ExportLibrary(InteropLibrary.class)
public final class ReadOnlyArray implements TruffleObject {

    private final Object[] values;

    public ReadOnlyArray(Object[] keys) {
        this.values = keys;
    }

    @ExportMessage
    static boolean hasArrayElements(@SuppressWarnings("unused") ReadOnlyArray receiver) {
        return true;
    }

    @ExportMessage
    static boolean isArrayElementReadable(ReadOnlyArray receiver, long index) {
        return index >= 0 && index < receiver.values.length;
    }

    @ExportMessage
    static int getArraySize(ReadOnlyArray receiver) {
        return receiver.values.length;
    }

    @ExportMessage
    static Object readArrayElement(ReadOnlyArray receiver, long at) throws InvalidArrayIndexException {
        if (!isArrayElementReadable(receiver, at)) {
            throw InvalidArrayIndexException.create(at);
        }
        return receiver.values[(int) at];
    }
}

