package heap.language;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.interop.*;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import heap.language.util.Descriptors;
import heap.language.util.ReadOnlyArray;
import org.graalvm.collections.Pair;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;

/**
 * Provides utility methods for manipulating values transferred between languages.
 */
interface Interop {

    /**
     * Ensure that arguments have the expected length.
     */
    static void checkArity(Object[] arguments, int expected) throws ArityException {
        if (arguments.length != expected) {
            throw ArityException.create(expected, arguments.length);
        }
    }

    /**
     * Ensure that arguments have the expected length, allowing number of optional arguments.
     */
    static void checkArityOptional(Object[] arguments, int minimum, int maximum) throws ArityException {
        if (arguments.length < minimum || arguments.length > maximum) {
            throw ArityException.create(maximum, arguments.length);
        }
    }

    /**
     * Check if the provided interop object represents a null value.
     */
    static boolean isNull(@NullAllowed Object obj) {
        if (obj == null) {
            return true;
        } else if (obj instanceof TruffleObject) {
            InteropLibrary interop = InteropLibrary.getFactory().getUncached();
            return interop.isNull(obj);
        }
        return false;
    }

    /**
     * Takes an interop value and tries to interpret it as string. If the value is not string, returns null.
     */
    static String tryAsString(@NonNull Object strObject) {
        if (strObject instanceof String) {
            return (String) strObject;
        } else if (strObject instanceof TruffleObject) {
            InteropLibrary interop = InteropLibrary.getFactory().getUncached();
            if (interop.isString(strObject)) {
                try {
                    return interop.asString(strObject);
                } catch (UnsupportedMessageException e) {
                    throw new IllegalStateException("Argument is string but does not implement `asString`.", e);
                }
            }
        }
        return null;
    }

    /**
     * Takes an interop value and treats it as a string if possible.
     */
    static String asString(@NonNull Object strObject) {
        String value = tryAsString(strObject);
        return Objects.requireNonNull(value, "Expected string-like argument.");
    }

    /**
     * Takes an interop value and treats it as a bool.
     */
    static boolean asBoolean(Object boolObject) {
        if (boolObject instanceof Boolean) {
            return (Boolean) boolObject;
        } else if (boolObject instanceof TruffleObject) {
            InteropLibrary interop = InteropLibrary.getFactory().getUncached();
            if (interop.isBoolean(boolObject)) {
                try {
                    return interop.asBoolean(boolObject);
                } catch (UnsupportedMessageException e) {
                    throw new IllegalStateException("Argument is boolean but does not implement `asBoolean`.", e);
                }
            }
        }
        throw new IllegalArgumentException("Expected boolean, but found "+boolObject);
    }

    /**
     * Interpret the given interop values as numbers and compare them (assuming standard Java compareTo semantics).
     */
    static int compareNumeric(Object left, Object right) {
        // First, try to compare as long (more precise)
        Long leftLong = tryAsIntegralNumber(left);
        Long rightLong = tryAsIntegralNumber(right);
        if (leftLong != null && rightLong != null) {
            return Long.compare(leftLong, rightLong);
        }
        Double leftDouble = leftLong == null ? tryAsFloatingPointNumber(left) : Double.valueOf(leftLong);
        Double rightDouble = rightLong == null ? tryAsFloatingPointNumber(right) : Double.valueOf(rightLong);
        if (leftDouble != null && rightDouble != null) {
            return Double.compare(leftDouble, rightDouble);
        }
        throw new IllegalArgumentException(left+" "+right+" are not numerically comparable.");
    }

    /**
     * Interpret the given interop value as integral. If not possible, return null.
     *
     * All smaller data types are automatically lifted to long.
     */
    static Long tryAsIntegralNumber(Object number) {
        if (number instanceof Byte) {
            return (long) (Byte) number;
        } else if (number instanceof Short) {
            return (long) (Short) number;
        } else if (number instanceof Integer) {
            return (long) (Integer) number;
        } else if (number instanceof Long) {
            return (Long) number;
        } else if (number instanceof TruffleObject) {
            InteropLibrary interop = InteropLibrary.getFactory().getUncached();
            if (interop.isNumber(number) && interop.fitsInLong(number)) {
                try {
                    return interop.asLong(number);
                } catch (UnsupportedMessageException e) {
                    throw new IllegalStateException("Fits into long, but conversion failed.", e);
                }
            }
        }
        return null;
    }

    /**
     * Same as {@link Interop#tryAsIntegralNumber(Object)}, but throws exception if the conversion is
     * not successful.
     */
    static long asIntegralNumber(Object number) {
        Long value = tryAsIntegralNumber(number);
        return Objects.requireNonNull(value, "Value cannot be converted to long."); // TODO: This is not semantically correct exception here
    }

    /**
     * Interpret the given interop value as floating point number. If not possible, return null.
     */
    static Double tryAsFloatingPointNumber(Object number) {
        if (number instanceof Float) {
            return (double) (Float) number;
        } else if (number instanceof Double) {
            return (Double) number;
        } else if (number instanceof TruffleObject) {
            InteropLibrary interop = InteropLibrary.getFactory().getUncached();
            if (interop.isNumber(number) && interop.fitsInDouble(number)) {
                try {
                    return interop.asDouble(number);
                } catch (UnsupportedMessageException e) {
                    throw new IllegalStateException("Fits into double, but conversion failed.", e);
                }
            }
        }
        Long integral = tryAsIntegralNumber(number);
        return integral != null ? (double) integral : null;
    }

    /**
     * Create an executable truffle object wrapping the given call target. The language environment
     * is needed to wrap unknown Java objects into guest values.
     */
    static TruffleObject wrapCallTarget(CallTarget call, TruffleLanguage.Env language) {
        return new WrappedCall(call, language);
    }

    /**
     * Create a lazy array-like truffle object which accesses elements in the given iterator.
     */
    static TruffleObject wrapIterator(Iterator<?> iterator) {
        if (iterator instanceof WrappedIterator) {
            return (WrappedIterator<?>) iterator;   // no need to wrap if already wrapped
        }
        return new WrappedIterator<>(iterator);
    }

    /**
     * Create a read-only array-like truffle object from the given list of items. Items are not converted,
     * so make sure list contents are already valid interop values.
     */
    static TruffleObject wrapArray(Object[] items) {
        return new ReadOnlyArray(items);
    }

    /**
     * Create an iterator of (index, value) pairs out of (almost) any truffle object.
     *  - If the object is an array, we iterate the array.
     *  - If the object has readable members, we iterate these members.
     */
    static Iterator<Pair<?, ?>> intoIndexedIterator(final Object object, InteropLibrary interop) {
        // TODO: Maybe we can bypass this for objects that actually implement Iterator/Iterable?
        if (interop.hasArrayElements(object)) {
            return new Iterator<Pair<?, ?>>() {
                private int index = 0;
                @Override
                public boolean hasNext() {
                    return interop.isArrayElementReadable(object, index);
                }

                @Override
                public Pair<Integer, Object> next() {
                    try {
                        int elementIndex = index;
                        Object element = interop.readArrayElement(object, index);
                        this.index += 1;
                        return Pair.create(elementIndex, element);
                    } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
                        throw new IllegalStateException("Illegal array access", e);  // should be unreachable
                    }
                }
            };
        } else if (interop.hasMembers(object)) {
            try {
                Object members = interop.getMembers(object);
                return new Iterator<Pair<?,?>>() {

                    private int index = 0;

                    @Override
                    public boolean hasNext() {
                        while (interop.isArrayElementReadable(members, index)) {
                            try {
                                String elementKey = asString(interop.readArrayElement(members, index));
                                if (interop.isMemberReadable(object, elementKey)) {
                                    return true;
                                } else {
                                    index += 1;
                                }
                            } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
                                throw new IllegalStateException("Error accessing object member", e);  // should be unreachable
                            }
                        }
                        return false;
                    }

                    @Override
                    public Pair<String, Object> next() {
                        try {
                            String elementKey = asString(interop.readArrayElement(members, index));
                            Object element = interop.readMember(object, elementKey);
                            index += 1;
                            return Pair.create(elementKey, element);
                        } catch (UnsupportedMessageException | InvalidArrayIndexException | UnknownIdentifierException e) {
                            e.printStackTrace();
                            throw new IllegalStateException("Error accessing object member", e);  // should be unreachable
                        }
                    }

                };
            } catch (UnsupportedMessageException e) {
                throw new IllegalStateException("Object does not have members", e);  // should be unreachable
            }
        } else {
            throw new IllegalArgumentException("Cannot iterate "+object+".");
        }
    }

    /**
     * Exposes Java iterator as lazy truffle array. Currently, truffle does not have an iterator interface
     * so this is basically the best we can do unless we want to emulate each language individually.
     *
     * If you want to access the values in a truly lazy way, we also export the {@code hasNext()/next()} methods
     * as defined by standard Java iterators.
     */
    @ExportLibrary(InteropLibrary.class)
    final class WrappedIterator<T> implements TruffleObject, Iterator<T> {

        private static final String HAS_NEXT = "hasNext";
        private static final String NEXT = "next";

        private static final Descriptors MEMBERS = Descriptors.functions(HAS_NEXT, NEXT);

        private final Iterator<T> iterator;
        private final ArrayList<Object> valueCache;

        public WrappedIterator(Iterator<T> iterator) {
            this.iterator = iterator;
            this.valueCache = new ArrayList<>();
        }

        @Override
        public boolean hasNext() {
            return this.iterator.hasNext();
        }

        @Override
        public T next() {
            T val = this.iterator.next();
            valueCache.add(val);
            return val;
        }

        @ExportMessage
        public static boolean hasArrayElements(@SuppressWarnings("unused") WrappedIterator<?> receiver) {
            return true;
        }

        @ExportMessage
        public static long getArraySize(WrappedIterator<?> receiver) {
            // Here, we sadly have to iterate the whole array at the moment...
            while (receiver.hasNext()) { receiver.next(); }
            return receiver.valueCache.size();
        }

        @ExportMessage
        public static boolean isArrayElementReadable(WrappedIterator<?> receiver, long index) {
            while (receiver.hasNext() && index >= receiver.valueCache.size()) {
                receiver.next();
            }
            return index < receiver.valueCache.size();
        }

        @ExportMessage
        public static Object readArrayElement(WrappedIterator<?> receiver, long index) {
            if (!isArrayElementReadable(receiver, index)) {
                throw new ArrayIndexOutOfBoundsException((int) index);
            }
            return receiver.valueCache.get((int) index);
        }

        @ExportMessage
        static boolean hasMembers(@SuppressWarnings("unused") WrappedIterator<?> receiver) {
            return true;
        }

        @ExportMessage
        static Object getMembers(@SuppressWarnings("unused") WrappedIterator<?> receiver,
                                 @SuppressWarnings("unused") boolean includeInternal) {
            return WrappedIterator.MEMBERS;
        }

        @ExportMessage
        static boolean isMemberInvocable(@SuppressWarnings("unused") WrappedIterator<?> receiver, String member) {
            return MEMBERS.hasFunction(member);
        }

        @ExportMessage
        static Object invokeMember(WrappedIterator<?> receiver, String member, Object[] arguments)
                throws ArityException, UnknownIdentifierException
        {
            Interop.checkArity(arguments, 0);
            switch (member) {
                case NEXT:
                    return receiver.next();
                case HAS_NEXT:
                    return receiver.hasNext();
                default:
                    throw UnknownIdentifierException.create(member);
            }
        }

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
