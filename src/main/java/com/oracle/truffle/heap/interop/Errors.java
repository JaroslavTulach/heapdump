package com.oracle.truffle.heap.interop;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.UnsupportedTypeException;

import java.util.NoSuchElementException;

/**
 * Errors contains utility methods for creating useful exceptions. Each variant provides option to easily
 * create a specific type of exception, with a formatted message, a cause exception, etc.
 *
 * Important aspect is also that each method transfers to interpreter, thus avoiding compilation of exception
 * handling code.
 */
public interface Errors {

    /**
     * Rethrow the given exception, possibly as runtime exception.
     */
    static <E extends Exception, R> R rethrow(@SuppressWarnings("unused") Class<E> type, Exception ex) throws E {
        CompilerDirectives.transferToInterpreter();
        //noinspection unchecked - this is intentional
        throw (E) ex;
    }

    /*
     * Utility methods to throw common exceptions and transfer to interpreter (useful e.g. in ternary operators).
     */

    static <T> T illegalArgument(String message) {
        CompilerDirectives.transferToInterpreter();
        throw new IllegalArgumentException(message);
    }

    static <T> T illegalState(String message) {
        CompilerDirectives.transferToInterpreter();
        throw new IllegalStateException(message);
    }

    static <T> T expectedArgumentType(Object[] arguments, Object argType, int argIndex) throws  UnsupportedTypeException {
        CompilerDirectives.transferToInterpreter();
        throw UnsupportedTypeException.create(arguments, String.format("Expected %s as argument %d, but found %s.", argType, argIndex + 1, arguments[argIndex]));
    }

    static <T> T unsupportedType(Object[] arguments, String message, Object... args) throws UnsupportedTypeException {
        CompilerDirectives.transferToInterpreter();
        throw UnsupportedTypeException.create(arguments, String.format(message, args));
    }

    static <T> T classCast(String message) {
        CompilerDirectives.transferToInterpreter();
        throw new ClassCastException(message);
    }

    static <T> T classCast(Object value, Class<?> expected) {
        return classCast("Cannot cast "+value+" to "+expected);
    }

    static <T> T noSuchElement(Throwable cause, String message, Object... args) {
        CompilerDirectives.transferToInterpreter();
        throw new NoSuchElementException(String.format(message, args) + " " + cause.getMessage());
    }

    static <T> T noSuchElement(String message, Object... args) {
        CompilerDirectives.transferToInterpreter();
        throw new NoSuchElementException(String.format(message, args));
    }

}
