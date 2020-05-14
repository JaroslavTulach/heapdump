package com.oracle.truffle.heap.interop;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;

import java.util.Collections;
import java.util.Iterator;

/**
 * <p>A {@link TruffleObject} that stores a list of {@link String} member names. Each member is either a
 * property or a function. Both sets can be queried in a set-like manner (however, the access is currently
 * linear so avoid enormous lists).</p>
 *
 * <p>This is generally useful when implementing custom objects with fixed set of methods/properties that
 * need to be stored somewhere.</p>
 */
@ExportLibrary(InteropLibrary.class)
public final class MemberDescriptor implements TruffleObject {

    @NullAllowed
    private final String[] properties;

    @NullAllowed
    private final String[] functions;

    // Total length of the array as seen from interop library.
    private final int totalLength;

    // Offset of functions array compared to global array index.
    private final int functionsIndexOffset;

    /**
     * <p>Create a new instance with property keys only.</p>
     */
    public static MemberDescriptor properties(String... properties) {
        return new MemberDescriptor(properties, null);
    }

    /**
     * <p>Create a new instance with function keys only.</p>
     */
    public static MemberDescriptor functions(String... functions) {
        return new MemberDescriptor(null, functions);
    }

    /**
     * <p>Create a new instance with property and function keys.</p>
     */
    public static MemberDescriptor build(@NullAllowed String[] properties, @NullAllowed String[] functions) {
        return new MemberDescriptor(properties, functions);
    }

    private MemberDescriptor(@NullAllowed String[] properties, @NullAllowed String[] functions) {
        this.properties = properties;
        this.functions = functions;
        this.functionsIndexOffset = properties == null ? 0 : properties.length;
        this.totalLength = (properties == null ? 0 : properties.length) + (functions == null ? 0 : functions.length);
    }

    public Iterable<String> getProperties() {
        if (properties == null) return Collections.emptyList();
        return () -> new Iterator<>() {
            int i = 0;

            @Override
            public boolean hasNext() {
                return i < properties.length;
            }

            @Override
            public String next() {
                i += 1;
                return properties[i - 1];
            }
        };
    }

    /**
     * <p>True if there is any descriptor with the given name.</p>
     */
    public boolean contains(@NonNull String key) {
        return hasProperty(key) || hasFunction(key);
    }

    /**
     * <p>True if there is a <i>property</i> descriptor with the given name.</p>
     */
    public boolean hasProperty(@NonNull String key) {
        return arrayContains(properties, key);
    }

    /**
     * <p>True if there is a <i>function</i> descriptor with the given name.</p>
     */
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
    static boolean hasArrayElements(@SuppressWarnings("unused") MemberDescriptor receiver) {
        return true;
    }

    @ExportMessage
    static boolean isArrayElementReadable(MemberDescriptor receiver, long index) {
        return index >= 0 && index < receiver.totalLength;
    }

    @ExportMessage
    static int getArraySize(MemberDescriptor receiver) {
        return receiver.totalLength;
    }

    @ExportMessage
    static String readArrayElement(MemberDescriptor receiver, long at) throws InvalidArrayIndexException {
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
