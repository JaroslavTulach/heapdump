package com.oracle.truffle.heap.interop.nodes;

import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.heap.interop.Args;
import com.oracle.truffle.heap.interop.Errors;

/**
 * Unwraps an optional boolean argument with the option to provide a default value.
 */
@GenerateUncached
@ImportStatic(Args.class)
public abstract class ArgBooleanOptional extends Node {

    public abstract Boolean execute(Object[] arguments, int index, Boolean defaultTo) throws UnsupportedTypeException;

    static boolean isBoolean(Object[] arguments, int index) {
        return Args.isMissing(arguments, index) || arguments[index] instanceof Boolean;
    }

    @Specialization(guards = "isMissing(arguments, index)") @SuppressWarnings("unused")
    static Boolean doMissing(Object[] arguments, int index, Boolean defaultTo) {
        return defaultTo;
    }

    @Specialization(guards = "isBoolean(arguments, index)", replaces = "doMissing")
    static Boolean doBoolean(Object[] arguments, int index, Boolean defaultTo) {
        return Args.isMissing(arguments, index) ? defaultTo : (boolean) arguments[index];
    }

    @Specialization(replaces = "doBoolean")
    static Boolean doGeneric(Object[] arguments, int index, Boolean defaultTo, @CachedLibrary(limit = "3") InteropLibrary interop) throws UnsupportedTypeException {
        Object arg = (index >= arguments.length) ? null : arguments[index];
        if (arg == null) {
            return defaultTo;
        } else if (arg instanceof Boolean) {
            return (boolean) arg;
        } else if (interop.isNull(arg)) {
            return defaultTo;
        } else if (interop.isBoolean(arg)) {
            try {
                return interop.asBoolean(arg);
            } catch (UnsupportedMessageException e) {
                return Errors.rethrow(RuntimeException.class, e);
            }
        } else {
            return Errors.expectedArgumentType(arguments, Boolean.class, index);
        }
    }

}
