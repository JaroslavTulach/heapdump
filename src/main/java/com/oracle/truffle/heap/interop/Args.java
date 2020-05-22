package com.oracle.truffle.heap.interop;

import com.oracle.truffle.api.interop.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Utility methods for dealing with function arguments.
 */
public final class Args {
    private Args() {}

    public static boolean isMissing(Object[] arguments, int index) {
        return index >= arguments.length || arguments[index] == null;
    }

    public static <T extends TruffleObject> T unwrapInstance(Object[] arguments, int argIndex, Class<T> clazz) throws UnsupportedTypeException {
        Object argument = arguments[argIndex];
        if (clazz.isInstance(argument)) {
            return clazz.cast(argument);
        }
        return Errors.expectedArgumentType(arguments, clazz, argIndex);
    }

    public static List<?> unwrapList(Object[] arguments, int argIndex) throws UnsupportedTypeException {
        return unwrapList(arguments, argIndex, InteropLibrary.getFactory().getUncached());
    }

    public static boolean unwrapBoolean(Object[] arguments, int argIndex) throws UnsupportedTypeException {
        Boolean value = Types.tryAsBoolean(arguments[argIndex]);
        return value != null ? value : Errors.expectedArgumentType(arguments, "boolean", argIndex);
    }

    public static long unwrapIntegral(Object[] arguments, int argIndex) throws UnsupportedTypeException {
        Long value = Types.tryAsIntegralNumber(arguments[argIndex]);
        return value != null ? value : Errors.expectedArgumentType(arguments, "integral number", argIndex);
    }

    public static String tryUnwrapString(Object[] arguments, int argIndex) {
        return Types.tryAsString(arguments[argIndex]);
    }

    public static String unwrapString(Object[] arguments, int argIndex) throws UnsupportedTypeException {
        String value = Types.tryAsString(arguments[argIndex]);
        return value != null ? value : Errors.expectedArgumentType(arguments, "String", argIndex);
    }

    public static List<?> unwrapList(Object[] arguments, int argIndex, InteropLibrary interop) throws UnsupportedTypeException {
        ArrayList<Object> result = new ArrayList<>();
        Object argument = arguments[argIndex];
        TruffleObject array = Types.tryAsArray(argument, interop);
        if (array != null) {    // read as array
            int index = 0;
            while (interop.isArrayElementReadable(array, index)) {
                try {
                    result.add(interop.readArrayElement(array, index));
                } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
                    Errors.rethrow(RuntimeException.class, e);
                }
                index += 1;
            }
            return result;
        }
        Iterator<?> iterator = Iterators.tryAsIterator(argument, interop);
        if (iterator != null) {
            while (iterator.hasNext()) {
                result.add(iterator.next());
            }
            return result;
        }

        return Errors.expectedArgumentType(arguments, "array or iterable", argIndex);
    }

    public static TruffleObject unwrapExecutable(Object[] arguments, int argIndex) throws UnsupportedTypeException {
        return unwrapExecutable(arguments, argIndex, InteropLibrary.getFactory().getUncached());
    }

    public static TruffleObject unwrapExecutable(Object[] arguments, int argIndex, InteropLibrary interop) throws UnsupportedTypeException {
        TruffleObject executable = Types.tryAsExecutable(arguments[argIndex], interop);
        return executable != null ? executable : Errors.expectedArgumentType(arguments, "executable", argIndex);
    }

    public static Iterator<?> unwrapIterator(Object[] arguments, int argIndex) throws UnsupportedTypeException {
        return unwrapIterator(arguments, argIndex, InteropLibrary.getFactory().getUncached());
    }

    public static Iterator<?> unwrapIterator(Object[] arguments, int argIndex, InteropLibrary interop) throws UnsupportedTypeException {
        Iterator<?> iterator = Iterators.tryAsIterator(arguments[argIndex], interop);
        return iterator != null ? iterator : Errors.expectedArgumentType(arguments, "array/iterator", argIndex);
    }

    public static Iterator<? extends IndexPair<?, ?>> unwrapIndexedIterator(Object[] arguments, int argIndex) throws UnsupportedTypeException {
        return unwrapIndexedIterator(arguments, argIndex, InteropLibrary.getFactory().getUncached());
    }

    public static Iterator<? extends IndexPair<?, ?>> unwrapIndexedIterator(Object[] arguments, int argIndex, InteropLibrary interop) throws UnsupportedTypeException {
        Iterator<? extends IndexPair<?, ?>> iterator = Iterators.tryAsIndexedIterator(arguments[argIndex], interop);
        return iterator != null ? iterator : Errors.expectedArgumentType(arguments, "array/iterator", argIndex);
    }

    /**
     * <p>Ensure that arguments have the expected length.</p>
     */
    public static void checkArity(Object[] arguments, int expected) throws ArityException {
        if (arguments.length != expected) {
            throw ArityException.create(expected, arguments.length);
        }
    }

    /**
     * <p>Ensure that arguments have the expected length, allowing number of optional arguments.</p>
     */
    public static void checkArityBetween(Object[] arguments, int minimum, int maximum) throws ArityException {
        if (arguments.length < minimum || arguments.length > maximum) {
            throw ArityException.create(maximum, arguments.length);
        }
    }

}
