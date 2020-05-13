package com.oracle.truffle.api.interop;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;

import java.util.Objects;

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
     * Rethrow the given exception, possibly as runtime exception.
     */
    static <E extends Exception> E rethrow(Class<E> type, Exception ex) throws E {
        //noinspection unchecked - this is intentional
        throw (E) ex;
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
                /* TODO
                if (original == null) { // should not happen, but just in case...
                    wrapped = Primitives.Null.INSTANCE;
                } else if (original instanceof TruffleObject) { // truffle objects should be fine
                    wrapped = original;
                } else if (original instanceof Boolean) {
                    wrapped = new Primitives.Bool((Boolean) original);
                } else if (original instanceof Character) {
                    wrapped = new Primitives.Str(Character.toString((Character) original));
                } else if (original instanceof String) {
                    wrapped = new Primitives.Str((String) original);
                } else if (original instanceof Byte) {
                    wrapped = new Primitives.Integral((Byte) original);
                } else if (original instanceof Short) {
                    wrapped = new Primitives.Integral((Short) original);
                } else if (original instanceof Integer) {
                    wrapped = new Primitives.Integral((Integer) original);
                } else if (original instanceof Long) {
                    wrapped = new Primitives.Integral((Long) original);
                } else if (original instanceof Float) {
                    wrapped = new Primitives.FloatingPoint((Float) original);
                } else if (original instanceof Double) {
                    wrapped = new Primitives.FloatingPoint((Double) original);
                } else {
                    wrapped = receiver.language.asGuestValue(original);
                }*/
                if (original == null) { // should not happen, but just in case...
                    wrapped = Primitives.Null.INSTANCE;
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

    /**
     * Implementations of interop wrappers around primitive values. This is mostly used to allow wrapping
     * {@link com.oracle.truffle.api.CallTarget} into executable truffle objects, since different languages
     * can handle primitive types differently (e.g. JavaScript only allows {@link Double}, {@link Long} and
     * {@link String}). Using these wrappers, we ensure that anything that we pass to the call target is
     * a valid interop value in any well-behaved language.
     *
     * Note that {@link com.oracle.truffle.api.TruffleLanguage.Env#asGuestValue(Object)} is not sufficient to
     * ensure this, because it assumes all primitive values are correctly handled by the callee.
     */
    interface Primitives {

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

        @ExportLibrary(InteropLibrary.class)
        final class Bool implements TruffleObject {

            private final boolean value;

            public Bool(boolean value) {
                this.value = value;
            }

            @ExportMessage
            static boolean isBoolean(@SuppressWarnings("unused") Bool receiver) {
                return true;
            }

            @ExportMessage
            static boolean asBoolean(Bool receiver) {
                return receiver.value;
            }

            @Override
            public String toString() {
                return Boolean.toString(value);
            }

        }

        @ExportLibrary(InteropLibrary.class)
        final class Str implements TruffleObject {

            private final String value;

            public Str(String value) {
                this.value = value;
            }

            @Override
            public String toString() {
                return value;
            }

            @ExportMessage
            static boolean isString(@SuppressWarnings("unused") Str receiver) {
                return true;
            }

            @ExportMessage
            static String asString(Str receiver) {
                return receiver.value;
            }

        }

        @ExportLibrary(InteropLibrary.class)
        final class FloatingPoint implements TruffleObject {

            private final double value;

            public FloatingPoint(double value) {
                this.value = value;
            }

            @Override
            public String toString() {
                return Double.toString(value);
            }

            @ExportMessage
            static boolean isNumber(@SuppressWarnings("unused") FloatingPoint receiver) {
                return true;
            }

            @ExportMessage
            static boolean fitsInByte(@SuppressWarnings("unused") FloatingPoint receiver) {
                return false;
            }

            @ExportMessage
            static boolean fitsInShort(@SuppressWarnings("unused") FloatingPoint receiver) {
                return false;
            }

            @ExportMessage
            static boolean fitsInInt(@SuppressWarnings("unused") FloatingPoint receiver) {
                return false;
            }

            @ExportMessage
            static boolean fitsInLong(@SuppressWarnings("unused") FloatingPoint receiver) {
                return false;
            }

            @ExportMessage
            static boolean fitsInFloat(FloatingPoint receiver) {
                return ((double)(float) receiver.value) == receiver.value;
            }

            @ExportMessage
            static boolean fitsInDouble(@SuppressWarnings("unused") FloatingPoint receiver) {
                return true;
            }

            @ExportMessage
            static byte asByte(@SuppressWarnings("unused") FloatingPoint receiver) throws UnsupportedMessageException {
                throw UnsupportedMessageException.create();
            }

            @ExportMessage
            static short asShort(@SuppressWarnings("unused") FloatingPoint receiver) throws UnsupportedMessageException {
                throw UnsupportedMessageException.create();
            }

            @ExportMessage
            static int asInt(@SuppressWarnings("unused") FloatingPoint receiver) throws UnsupportedMessageException {
                throw UnsupportedMessageException.create();
            }

            @ExportMessage
            static long asLong(@SuppressWarnings("unused") FloatingPoint receiver) throws UnsupportedMessageException {
                throw UnsupportedMessageException.create();
            }

            @ExportMessage
            static float asFloat(FloatingPoint receiver) {
                return (float) receiver.value;
            }

            @ExportMessage
            static double asDouble(FloatingPoint receiver) {
                return receiver.value;
            }

        }

        @ExportLibrary(InteropLibrary.class)
        final class Integral implements TruffleObject {

            private final long value;

            public Integral(long value) {
                this.value = value;
            }

            @Override
            public String toString() {
                return Long.toString(value);
            }

            @ExportMessage
            static boolean isNumber(@SuppressWarnings("unused") Integral receiver) {
                return true;
            }

            @ExportMessage
            static boolean fitsInByte(Integral receiver) {
                return Byte.MIN_VALUE <= receiver.value && receiver.value <= Byte.MAX_VALUE;
            }

            @ExportMessage
            static boolean fitsInShort(Integral receiver) {
                return Short.MIN_VALUE <= receiver.value && receiver.value <= Short.MAX_VALUE;
            }

            @ExportMessage
            static boolean fitsInInt(Integral receiver) {
                return Integer.MIN_VALUE <= receiver.value && receiver.value <= Integer.MAX_VALUE;
            }

            @ExportMessage
            static boolean fitsInLong(@SuppressWarnings("unused") Integral receiver) {
                return true;
            }

            @ExportMessage
            static boolean fitsInFloat(@SuppressWarnings("unused") Integral receiver) {
                return false;
            }

            @ExportMessage
            static boolean fitsInDouble(@SuppressWarnings("unused") Integral receiver) {
                return true;
            }

            @ExportMessage
            static byte asByte(Integral receiver) {
                return (byte) receiver.value;
            }

            @ExportMessage
            static short asShort(Integral receiver) {
                return (short) receiver.value;
            }

            @ExportMessage
            static int asInt(Integral receiver) {
                return (int) receiver.value;
            }

            @ExportMessage
            static long asLong(Integral receiver) {
                return receiver.value;
            }

            @ExportMessage
            static float asFloat(@SuppressWarnings("unused") Integral receiver) throws UnsupportedMessageException {
                throw UnsupportedMessageException.create();
            }

            @ExportMessage
            static double asDouble(Integral receiver) {
                return (double) receiver.value;
            }

        }

    }

}
