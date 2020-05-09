package com.oracle.truffle.heap;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.*;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

import java.util.*;

/**
 * <p>These functions accept an array/iterator/enumeration and an expression string [or a callback function] as input.
 * These functions iterate the array/iterator/enumeration and apply the expression (or function) on each element.
 * Note that JavaScript objects are associative arrays. So, these functions may also be used with
 * arbitrary JavaScript objects.</p>
 */
interface OQLSequenceGlobals {

    /**
     * Concatenates two arrays or enumerations (i.e., returns composite enumeration).
     */
    @ExportLibrary(InteropLibrary.class)
    class Concat implements TruffleObject {

        public static final Concat INSTANCE = new Concat();

        private Concat() {}

        @ExportMessage
        static boolean isExecutable(@SuppressWarnings("unused") Concat receiver) {
            return true;
        }

        @ExportMessage
        static Object execute(@SuppressWarnings("unused") Concat receiver, Object[] arguments)
                throws ArityException, UnsupportedTypeException {
            Args.checkArity(arguments, 2);
            Iterator<?> i1 = Args.unwrapIterator(arguments, 0);
            Iterator<?> i2 = Args.unwrapIterator(arguments, 1);

            Iterator<?> concatIterator = new Iterator<Object>() {

                @Override
                public boolean hasNext() {
                    return i1.hasNext() || i2.hasNext();
                }

                @Override
                public Object next() {
                    if (i1.hasNext()) {
                        return i1.next();
                    } else {
                        return i2.next();
                    }
                }
            };

            return Iterators.exportIterator(concatIterator);
        }

    }

    /**
     * Returns whether the given array/enumeration contains an element the given boolean expression specified in code.
     */
    @ExportLibrary(InteropLibrary.class)
    class Contains implements TruffleObject {

        public static final Contains INSTANCE = new Contains();

        private Contains() {}

        @ExportMessage
        static boolean isExecutable(@SuppressWarnings("unused") Contains receiver) {
            return true;
        }

        @ExportMessage
        static Object execute(@SuppressWarnings("unused") Contains receiver, Object[] arguments)
                throws ArityException, UnsupportedTypeException, UnsupportedMessageException
        {
            Args.checkArity(arguments, 2);
            InteropLibrary interop = InteropLibrary.getFactory().getUncached();
            Iterator<? extends IndexPair<?, ?>> it = Args.unwrapIndexedIterator(arguments, 0);
            TruffleObject callback = HeapLanguage.unwrapCallbackArgument(arguments, 1, "it", "index", "array");
            while (it.hasNext()) {
                IndexPair<?, ?> element = it.next();
                if (Types.asBoolean(interop.execute(callback, element.getValue(), element.getIndex(), arguments[0]))) {
                    return Boolean.TRUE;
                }
            }
            return Boolean.FALSE;
        }

    }

    /**
     * Count function returns the count of elements of the input array/enumeration that satisfy the given boolean expression.
     */
    @ExportLibrary(InteropLibrary.class)
    class Count implements TruffleObject {

        public static final Count INSTANCE = new Count();

        private Count() {}

        @ExportMessage
        static boolean isExecutable(@SuppressWarnings("unused") Count receiver) {
            return true;
        }

        @ExportMessage
        static Object execute(@SuppressWarnings("unused") Count receiver, Object[] arguments)
                throws ArityException, UnsupportedTypeException, UnsupportedMessageException
        {
            Args.checkArityBetween(arguments, 1, 2);
            if (arguments.length == 1) return Length.execute(null, arguments);
            Iterator<? extends IndexPair<?, ?>> it = Args.unwrapIndexedIterator(arguments, 0);
            TruffleObject callback = HeapLanguage.unwrapCallbackArgument(arguments, 1, "it", "index", "array");
            long count = 0;
            InteropLibrary interop = InteropLibrary.getFactory().getUncached();
            while (it.hasNext()) {
                IndexPair<?, ?> element = it.next();
                if (Types.asBoolean(interop.execute(callback, element.getValue(), element.getIndex(), arguments[0]))) {
                    count += 1;
                }
            }
            return count;
        }

    }

    /**
     * Filter function returns an array/enumeration that contains elements of the input array/enumeration that satisfy the given boolean expression.
     */
    @ExportLibrary(InteropLibrary.class)
    class Filter implements TruffleObject {

        public static final Filter INSTANCE = new Filter();

        private Filter() {}

        @ExportMessage
        static boolean isExecutable(@SuppressWarnings("unused") Filter receiver) {
            return true;
        }

        @ExportMessage
        static Object execute(@SuppressWarnings("unused") Filter receiver, Object[] arguments) throws ArityException, UnsupportedTypeException {
            Args.checkArity(arguments, 2);
            //noinspection unchecked
            Iterator<IndexPair<?, ?>> it = (Iterator<IndexPair<?, ?>>) Args.unwrapIndexedIterator(arguments, 0);
            TruffleObject callback = HeapLanguage.unwrapCallbackArgument(arguments, 1, "it", "index", "array", "result");

            InteropLibrary interop = InteropLibrary.getFactory().getUncached();
            Iterator<Object> filterIterator = new IteratorFilter<IndexPair<?, ?>>(it) {

                @Override
                public Object check(IndexPair<?, ?> item) {
                    try {
                        Object isValid = interop.execute(callback, item.getValue(), item.getIndex(), arguments[0], HeapLanguage.asGuestValue(this));
                        return Types.asBoolean(isValid) ? item.getValue() : null;
                    } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException e) {
                        throw new IllegalStateException("Cannot execute filter callback.", e);
                    }
                }

            };

            return Iterators.exportIterator(filterIterator);
        }

    }

    /**
     * Length function returns number of elements of an array/enumeration.
     */
    @ExportLibrary(InteropLibrary.class)
    class Length implements TruffleObject {

        public static final Length INSTANCE = new Length();

        private Length() {}

        @ExportMessage
        static boolean isExecutable(@SuppressWarnings("unused") Length receiver) {
            return true;
        }

        @ExportMessage
        static Object execute(@SuppressWarnings("unused") Length receiver, Object[] arguments) throws ArityException, UnsupportedTypeException, UnsupportedMessageException {
            Args.checkArity(arguments, 1);
            // First, try to check array length
            TruffleObject array = Types.tryAsArray(arguments[0]);
            InteropLibrary interop = InteropLibrary.getFactory().getUncached();
            if (array != null) return interop.getArraySize(array);
            // If that fails, iterate the object
            Iterator<?> it = Args.unwrapIterator(arguments, 0);
            long count = 0;
            while (it.hasNext()) {
                count += 1; it.next();
            }
            return count;
        }

    }

    /**
     * Transforms the given array/enumeration by evaluating given code on each element.
     */
    @ExportLibrary(InteropLibrary.class)
    class Map implements TruffleObject {

        public static final Map INSTANCE = new Map();

        private Map() {}

        @ExportMessage
        static boolean isExecutable(@SuppressWarnings("unused") Map receiver) {
            return true;
        }

        @ExportMessage
        static Object execute(@SuppressWarnings("unused") Map receiver, Object[] arguments) throws ArityException, UnsupportedTypeException {
            Args.checkArity(arguments, 2);
            Iterator<? extends IndexPair<?, ?>> items = Args.unwrapIndexedIterator(arguments, 0);
            TruffleObject callback = HeapLanguage.unwrapCallbackArgument(arguments, 1, "it", "index", "array", "result");

            InteropLibrary interop = InteropLibrary.getFactory().getUncached();
            Iterator<Object> mappedIterator = new Iterator<Object>() {
                @Override
                public boolean hasNext() {
                    return items.hasNext();
                }

                @Override
                public Object next() {
                    try {
                        IndexPair<?, ?> item = items.next();
                        return interop.execute(callback, item.getValue(), item.getIndex(), arguments[0], HeapLanguage.asGuestValue(this));
                    } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException e) {
                        throw new IllegalStateException("Cannot execute map callback.", e);
                    }
                }
            };

            return Iterators.exportIterator(mappedIterator);
        }

    }

    /**
     * Returns the maximum element of the given array/enumeration. Optionally accepts code expression to compare
     * elements of the array. By default numerical comparison is used.
     */
    @ExportLibrary(InteropLibrary.class)
    class Max implements TruffleObject {

        public static final Max INSTANCE = new Max();

        private Max() {}

        @ExportMessage
        static boolean isExecutable(@SuppressWarnings("unused") Max receiver) {
            return true;
        }

        @ExportMessage
        static Object execute(@SuppressWarnings("unused") Max receiver, Object[] arguments)
                throws ArityException, UnsupportedTypeException, UnsupportedMessageException
        {
            Args.checkArityBetween(arguments, 1, 2);
            Iterator<?> it = Args.unwrapIterator(arguments, 0);
            Object max = null;
            if (arguments.length == 1) {
                if (it.hasNext()) max = it.next();
                while (it.hasNext()) {
                    Object element = it.next();
                    if (Types.compareValues(max, element) < 0) {
                        max = element;
                    }
                }
            } else {
                InteropLibrary interop = InteropLibrary.getFactory().getUncached();
                TruffleObject callback = HeapLanguage.unwrapCallbackArgument(arguments, 1, "lhs", "rhs");
                if (it.hasNext()) max = it.next();
                while (it.hasNext()) {
                    Object element = it.next();
                    if (Types.asBoolean(interop.execute(callback, element, max))) { // true if lhs > rhs
                        max = element;
                    }
                }
            }
            return max == null ? HeapLanguage.NULL : max;
        }

    }

    /**
     * Returns the minimum element of the given array/enumeration. Optionally accepts code expression to compare
     * elements of the array. By default numerical comparison is used.
     */
    @ExportLibrary(InteropLibrary.class)
    class Min implements TruffleObject {

        public static final Min INSTANCE = new Min();

        private Min() {}

        @ExportMessage
        static boolean isExecutable(@SuppressWarnings("unused") Min receiver) {
            return true;
        }

        @ExportMessage
        static Object execute(@SuppressWarnings("unused") Min receiver, Object[] arguments)
                throws ArityException, UnsupportedTypeException, UnsupportedMessageException {
            Args.checkArityBetween(arguments, 1, 2);
            Iterator<?> it = Args.unwrapIterator(arguments, 0);
            Object min = null;
            if (arguments.length == 1) {
                if (it.hasNext()) min = it.next();
                while (it.hasNext()) {
                    Object element = it.next();
                    if (Types.compareValues(min, element) > 0) {    // min > element
                        min = element;
                    }
                }
            } else {
                InteropLibrary interop = InteropLibrary.getFactory().getUncached();
                TruffleObject callback = HeapLanguage.unwrapCallbackArgument(arguments, 1, "lhs", "rhs");
                if (it.hasNext()) min = it.next();
                while (it.hasNext()) {
                    Object element = it.next();
                    if (Types.asBoolean(interop.execute(callback, element, min))) { // true if lhs < rhs
                        min = element;
                    }
                }
            }
            return min == null ? HeapLanguage.NULL : min;
        }
    }

    /**
     * Sorts given array/enumeration. Optionally accepts code expression to compare elements of the array. By default
     * numerical comparison is used.
     */
    @ExportLibrary(InteropLibrary.class)
    class Sort implements TruffleObject {

        public static final Sort INSTANCE = new Sort();

        private Sort() {}

        @ExportMessage
        static boolean isExecutable(@SuppressWarnings("unused") Sort receiver) {
            return true;
        }

        @ExportMessage
        static Object execute(@SuppressWarnings("unused") Sort receiver, Object[] arguments)
                throws ArityException, UnsupportedTypeException {
            Args.checkArityBetween(arguments, 1, 2);
            List<?> items = Args.unwrapList(arguments, 0);
            Comparator<Object> cmp;
            if (arguments.length == 1) {    // use default comparison
                cmp = Types::compareValues;
            } else {
                InteropLibrary interop = InteropLibrary.getFactory().getUncached();
                TruffleObject callback = HeapLanguage.unwrapCallbackArgument(arguments, 1, "lhs", "rhs");
                cmp = (o1, o2) -> {
                    try {
                        return (int) Types.asIntegralNumber(interop.execute(callback, o1, o2));
                    } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException e) {
                        throw new IllegalStateException("Cannot execute compare callback.", e);
                    }
                };
            }

            items.sort(cmp);
            return Interop.wrapArray(items.toArray());
        }

    }

    /**
     * This function returns the sum of all the elements of the given input array or enumeration. Optionally,
     * accepts an expression as second param. This is used to map the input elements before summing those.
     */
    @ExportLibrary(InteropLibrary.class)
    class Sum implements TruffleObject {

        public static final Sum INSTANCE = new Sum();

        private Sum() {}

        @ExportMessage
        static boolean isExecutable(@SuppressWarnings("unused") Sum receiver) {
            return true;
        }

        @ExportMessage
        static Object execute(@SuppressWarnings("unused") Sum receiver, Object[] arguments)
                throws ArityException, UnsupportedTypeException, UnsupportedMessageException
        {
            Args.checkArityBetween(arguments, 1, 2);
            InteropLibrary interop = InteropLibrary.getFactory().getUncached();
            long longSum = 0; boolean longValid = true;
            double doubleSum = 0.0; // we keep two sums, because if long works, it will be more precise
            Iterator<? extends IndexPair<?, ?>> it = Args.unwrapIndexedIterator(arguments, 0);
            TruffleObject callback = arguments.length == 1 ? null : HeapLanguage.unwrapCallbackArgument(arguments, 1, "it", "index", "array");
            while (it.hasNext()) {
                Object number;
                if (callback == null) {
                    number = it.next().getValue();
                } else {
                    IndexPair<?, ?> element = it.next();
                    number = interop.execute(callback, element.getValue(), element.getIndex(), arguments[0]);
                }
                if (longValid) {
                    Long lValue = Types.tryAsIntegralNumber(number);
                    if (lValue != null) {
                        longSum += lValue;
                        doubleSum += lValue;
                        continue;
                    }
                }
                // if conversion to long failed, continue with double only
                longValid = false;
                doubleSum += Types.asFloatingPointNumber(number);
            }

            return longValid ? longSum : doubleSum;
        }

    }

    /**
     * This function returns an array that contains elements of the input array/enumeration.
     */
    @ExportLibrary(InteropLibrary.class)
    class ToArray implements TruffleObject {

        public static final ToArray INSTANCE = new ToArray();

        private ToArray() {}

        @ExportMessage
        static boolean isExecutable(@SuppressWarnings("unused") ToArray receiver) {
            return true;
        }

        @ExportMessage
        static Object execute(@SuppressWarnings("unused") ToArray receiver, Object[] arguments) throws ArityException, UnsupportedTypeException {
            Args.checkArity(arguments, 1);
            List<?> items = Args.unwrapList(arguments, 0);
            return Interop.wrapArray(items.toArray());
        }

    }

    @ExportLibrary(InteropLibrary.class)
    class Top implements TruffleObject {

        public static final Top INSTANCE = new Top();

        private Top() {}

        @ExportMessage
        static boolean isExecutable(@SuppressWarnings("unused") Top receiver) {
            return true;
        }

        @ExportMessage
        static Object execute(@SuppressWarnings("unused") Top receiver, Object[] arguments) throws ArityException, UnsupportedTypeException {
            Args.checkArityBetween(arguments, 1, 3);

            Comparator<Object> cmp;
            if (arguments.length == 1) {    // use default comparison
                cmp = (o1, o2) -> 0;        // should preserve initial sorting
            } else {
                InteropLibrary interop = InteropLibrary.getFactory().getUncached();
                TruffleObject callback = HeapLanguage.unwrapCallbackArgument(arguments, 1, "lhs", "rhs");
                cmp = (o1, o2) -> {
                    try {
                        return (int) Types.asIntegralNumber(interop.execute(callback, o1, o2));
                    } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException e) {
                        throw new IllegalStateException("Cannot execute compare callback.", e);
                    }
                };
            }

            long take = 10;
            if (arguments.length == 3) {
                take = Args.unwrapIntegral(arguments, 2);
            }

            List<?> items = Args.unwrapList(arguments, 0);
            items.sort(cmp);
            Object[] result = new Object[(int) take];   // TODO: Safe cast? Also other places...
            for (int i=0; i<take && i< items.size(); i++) {
                result[i] = items.get(i);
            }

            return Interop.wrapArray(result);
        }

    }

    /**
     * This function returns an array/enumeration containing unique elements of the given input array/enumeration.
     */
    @ExportLibrary(InteropLibrary.class)
    class Unique implements TruffleObject {

        public static final Unique INSTANCE = new Unique();

        private Unique() {}

        @ExportMessage
        static boolean isExecutable(@SuppressWarnings("unused") Unique receiver) {
            return true;
        }

        @ExportMessage
        static Object execute(@SuppressWarnings("unused") Unique receiver, Object[] arguments) throws ArityException, UnsupportedTypeException {
            Args.checkArity(arguments, 1);
            return Interop.wrapArray(unique(Args.unwrapIterator(arguments, 0)));
        }

        @CompilerDirectives.TruffleBoundary
        private static Object[] unique(Iterator<?> it) {
            LinkedHashSet<Object> set = new LinkedHashSet<>();
            while (it.hasNext()) {
                set.add(it.next());
            }
            return set.toArray();
        }

    }

}
