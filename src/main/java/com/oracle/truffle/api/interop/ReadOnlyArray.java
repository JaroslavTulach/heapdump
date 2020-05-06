package com.oracle.truffle.api.interop;

import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

/**
 * <p>Wraps an array instance in a {@link TruffleObject} so that it can be shared between different
 * Truffle languages.</p>
 *
 * <p>Warning: Individual objects are not sanitized in any way - it is your responsibility to ensure
 * all objects in the array are valid interop instances.</p>
 */
@ExportLibrary(InteropLibrary.class)
final class ReadOnlyArray implements TruffleObject {

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

