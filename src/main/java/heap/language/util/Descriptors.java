package heap.language.util;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;

/**
 * A truffle object that stores property and function descriptor keys of a complex truffle object.
 */
@ExportLibrary(InteropLibrary.class)
public final class Descriptors implements TruffleObject {

    @NullAllowed
    private final String[] properties;

    @NullAllowed
    private final String[] functions;

    // Total length of the array as seen from interop library.
    private final int totalLength;

    // Offset of functions array compared to global array index.
    private final int functionsIndexOffset;

    /** Create a new instance with property keys only. */
    public static Descriptors properties(String... properties) {
        return new Descriptors(properties, null);
    }

    /** Create a new instance with function keys only. */
    public static Descriptors functions(String... functions) {
        return new Descriptors(null, functions);
    }

    /** Create a new instance with property and function keys. */
    public static Descriptors build(@NullAllowed String[] properties, @NullAllowed String[] functions) {
        return new Descriptors(properties, functions);
    }

    private Descriptors(@NullAllowed String[] properties, @NullAllowed String[] functions) {
        this.properties = properties;
        this.functions = functions;
        this.functionsIndexOffset = properties == null ? 0 : properties.length;
        this.totalLength = (properties == null ? 0 : properties.length) + (functions == null ? 0 : functions.length);
    }

    /** True if there is any descriptor with the given name. */
    public boolean contains(@NonNull String key) {
        return hasProperty(key) || hasFunction(key);
    }

    /** True if there is a <i>property</i> descriptor with the given name. */
    public boolean hasProperty(@NonNull String key) {
        return arrayContains(properties, key);
    }

    /** True if there is a <i>function</i> descriptor with the given name. */
    public boolean hasFunction(@NonNull String key) {
        return arrayContains(functions, key);
    }

    private static boolean arrayContains(String[] data, String key) {
        if (data != null) {
            for (String value : data) {
                if (key.equals(value)) return true;
            }
        }
        return false;
    }

    @ExportMessage
    static boolean hasArrayElements(@SuppressWarnings("unused") Descriptors receiver) {
        return true;
    }

    @ExportMessage
    static boolean isArrayElementReadable(Descriptors receiver, long index) {
        return index >= 0 && index < receiver.totalLength;
    }

    @ExportMessage
    static int getArraySize(Descriptors receiver) {
        return receiver.totalLength;
    }

    @ExportMessage
    static String readArrayElement(Descriptors receiver, long at) throws InvalidArrayIndexException {
        if (!isArrayElementReadable(receiver, at)) {
            throw InvalidArrayIndexException.create(at);
        }
        int functionsIndex = ((int) at) - receiver.functionsIndexOffset;
        if (functionsIndex >= 0 && receiver.functions != null) {
            return receiver.functions[functionsIndex];
        } else if (receiver.properties != null) {
            return receiver.properties[(int) at];
        } else {    // There are no elements - technically unreachable.
            throw InvalidArrayIndexException.create(at);
        }
    }

}
