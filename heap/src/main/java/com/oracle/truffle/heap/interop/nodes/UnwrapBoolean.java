package com.oracle.truffle.heap.interop.nodes;

import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.heap.interop.Errors;

/**
 * <p>An interop node that will efficiently unwrap interop boolean values. If the value is not boolean,
 * return {@code null}.</p>
 *
 * <p>We assume that incoming values will be usually of the same type — either always boolean, always
 * boolean-like or always not boolean — and specialize for these cases.</p>
 */
@GenerateUncached
public abstract class UnwrapBoolean extends Node {

    public static UnwrapBoolean create() {
        return UnwrapBooleanNodeGen.create();
    }

    public abstract Boolean execute(Object value);

    public static boolean isBoolean(Object value) {
        return value instanceof Boolean;
    }

    public static boolean isLikeBoolean(Object value, InteropLibrary interop) {
        return interop.isBoolean(value);
    }

    @Specialization(guards = "isBoolean(value)")
    public static Boolean getBoolean(Object value) {
        return (boolean) value;
    }

    @Specialization(guards = "isLikeBoolean(value, interop)")
    public static Boolean getBooleanLike(Object value, @CachedLibrary(limit = "3") InteropLibrary interop) {
        try {
            return interop.asBoolean(value);
        } catch (UnsupportedMessageException e) {
            return Errors.rethrow(RuntimeException.class, e);
        }
    }

    @Specialization(guards = { "!isBoolean(value)", "!isLikeBoolean(value, interop)" }) @SuppressWarnings("unused")
    public static Boolean getNothing(Object value, @CachedLibrary(limit = "3") InteropLibrary interop) {
        return null;
    }

    @Specialization
    public static Boolean doGeneric(Object value, @CachedLibrary(limit = "3") InteropLibrary interop) {
        if (value instanceof Boolean) {
            return (boolean) value;
        } else if (interop.isBoolean(value)) {
            try {
                return interop.asBoolean(value);
            } catch (UnsupportedMessageException e) {
                return Errors.rethrow(RuntimeException.class, e);
            }
        } else {
            return null;
        }
    }

}
