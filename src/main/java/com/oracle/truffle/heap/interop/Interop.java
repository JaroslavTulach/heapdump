package com.oracle.truffle.heap.interop;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

/**
 * Provides utility methods for manipulating values transferred between languages.
 */
public interface Interop {

    /**
     * Create an executable truffle object wrapping the given call target. The language environment
     * is needed to wrap unknown Java objects into guest values.
     */
    static TruffleObject wrapCallTarget(CallTarget call, TruffleLanguage.Env language) {
        return new WrappedCall(call, language);
    }

    /**
     * Create a read-only array-like truffle object from the given list of items. Items are not converted,
     * so make sure list contents are already valid interop values.
     */
    static TruffleObject wrapArray(Object[] items) {
        return new ReadOnlyArray(items);
    }

    /**
     * Used by {@link Interop#wrapCallTarget(CallTarget, TruffleLanguage.Env)}
     * to create interop friendly call targets.
     */
    @ExportLibrary(InteropLibrary.class)
    final class WrappedCall implements TruffleObject {

        private final CallTarget call;
        private final TruffleLanguage.Env language;

        public WrappedCall(CallTarget call, TruffleLanguage.Env language) {
            this.call = call;
            this.language = language;
        }

        @ExportMessage
        static boolean isExecutable(@SuppressWarnings("unused") WrappedCall receiver) {
            return true;
        }

        @ExportMessage
        static Object execute(WrappedCall receiver, Object[] arguments) {
            for (int i=0; i<arguments.length; i++) {    // replace arguments with interop-friendly wrappers
                Object original = arguments[i];
                Object wrapped;
                if (original == null) { // should not happen, but just in case...
                    wrapped = Null.INSTANCE;
                } else if (original instanceof TruffleObject) { // truffle objects should be fine
                    wrapped = original;
                } else if (original instanceof Boolean) {       // booleans as well
                    wrapped = original;
                } else if (original instanceof Character) {
                    wrapped = Character.toString((Character) original);
                } else if (original instanceof String) {        // strings are fine too
                    wrapped = original;
                } else if (original instanceof Byte) {          // numbers we convert to longs/doubles
                    wrapped = Long.valueOf((Byte) original);
                } else if (original instanceof Short) {
                    wrapped = Long.valueOf((Short) original);
                } else if (original instanceof Integer) {
                    wrapped = Long.valueOf((Integer) original);
                } else if (original instanceof Long) {
                    wrapped = original;
                } else if (original instanceof Float) {
                    wrapped = Double.valueOf((Float) original);
                } else if (original instanceof Double) {
                    wrapped = original;
                } else {
                    wrapped = receiver.language.asGuestValue(original);
                }
                arguments[i] = wrapped;
            }
            return receiver.call.call(arguments);
        }

    }

    @ExportLibrary(InteropLibrary.class)
    final class Null implements TruffleObject {
        public static final Null INSTANCE = new Null();
        private Null() { }

        @ExportMessage
        public static boolean isNull(@SuppressWarnings("unused") Null receiver) {
            return true;
        }

        @Override
        public String toString() {
            return "null";
        }
    }

}
