package com.oracle.truffle.api.interop;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Utility methods for dealing with function arguments.
 */
public final class Args {
    private Args() {}

    public static List<?> unwrapList(Object[] arguments, int argIndex) throws UnsupportedTypeException {
        return unwrapList(arguments, argIndex, InteropLibrary.getFactory().getUncached());
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
                    throw new IllegalStateException("Array element readable byt does not implement `readArrayElement`.", e);
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

        throw UnsupportedTypeException.create(arguments, "Expected array/iterable as argument "+(argIndex+1)+", but found "+argument+".");
    }

    public static TruffleObject unwrapExecutable(Object[] arguments, int argIndex, InteropLibrary interop) throws UnsupportedTypeException {
        TruffleObject executable = Types.tryAsExecutable(arguments[argIndex], interop);
        if (executable != null) return executable; else {
            throw UnsupportedTypeException.create(arguments, "Expected executable as argument "+(argIndex+1)+", but found "+arguments[argIndex]+".");
        }
    }

    public static Iterator<?> unwrapIterator(Object[] arguments, int argIndex) throws UnsupportedTypeException {
        return unwrapIterator(arguments, argIndex, InteropLibrary.getFactory().getUncached());
    }

    public static Iterator<?> unwrapIterator(Object[] arguments, int argIndex, InteropLibrary interop) throws UnsupportedTypeException {
        Iterator<?> iterator = Iterators.tryAsIterator(arguments[argIndex], interop);
        if (iterator != null) return iterator; else {
            throw UnsupportedTypeException.create(arguments, "Expected array/iterable as argument "+(argIndex+1)+", but found "+arguments[argIndex]+".");
        }
    }

    public static Iterator<? extends IndexPair<?, ?>> unwrapIndexedIterator(Object[] arguments, int argIndex) throws UnsupportedTypeException {
        return unwrapIndexedIterator(arguments, argIndex, InteropLibrary.getFactory().getUncached());
    }

    public static Iterator<? extends IndexPair<?, ?>> unwrapIndexedIterator(Object[] arguments, int argIndex, InteropLibrary interop) throws UnsupportedTypeException {
        Iterator<? extends IndexPair<?, ?>> iterator = Iterators.tryAsIndexedIterator(arguments[argIndex], interop);
        if (iterator != null) return iterator; else {
            throw UnsupportedTypeException.create(arguments, "Expected array/iterable as argument "+(argIndex+1)+", but found "+arguments[argIndex]+".");
        }
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
