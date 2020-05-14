package com.oracle.truffle.heap.interop;

import com.oracle.truffle.api.interop.*;

import java.util.Iterator;

/**
 * Utility methods for treating truffle interop objects as iterators and vice versa.
 */
public final class Iterators {
    private Iterators() {}

    public static <T> TruffleObject exportIterator(Iterator<T> iterator) {
        if (iterator instanceof InteropIterator<?>) {
            return (InteropIterator<?>) iterator;
        } else {
            return new InteropIterator<>(iterator);
        }
    }

    public static Iterator<?> tryAsIterator(Object value, InteropLibrary interop) {
        if (value instanceof Iterator<?>) return (Iterator<?>) value;
        if (value instanceof Iterable<?>) return ((Iterable<?>) value).iterator();
        Iterator<?> result;
        result = tryAsStringIterator(value, interop);
        if (result != null) return result;
        result = tryAsArrayIterator(value, interop);
        if (result != null) return result;
        result = tryAsMemberIterator(value, interop);
        return result;
    }

    public static Iterator<Character> tryAsStringIterator(Object value, InteropLibrary interop) {
        String string = Types.tryAsString(value, interop);
        if (string == null) return null;
        return new Iterator<Character>() {

            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < string.length();
            }

            @Override
            public Character next() {
                index += 1;
                return string.charAt(index - 1);
            }
        };
    }

    public static Iterator<Object> tryAsArrayIterator(Object value, InteropLibrary interop) {
        TruffleObject array = Types.tryAsArray(value, interop);
        if (array == null) return null;
        return new Iterator<Object>() {

            private int index = 0;

            @Override
            public boolean hasNext() {
                // Note: We do not use getArraySize, because array-like iterators would have to pre-cache the whole array.
                return interop.isArrayElementReadable(value, index);
            }

            @Override
            public Object next() {
                index += 1;
                try {
                    return interop.readArrayElement(value, index - 1);
                } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
                    return Errors.noSuchElement(e, "Cannot access array element at index %d.", index - 1);
                }
            }
        };
    }

    public static Iterator<Object> tryAsMemberIterator(Object value, InteropLibrary interop) {
        TruffleObject members = Types.tryReadMemberDescriptor(value, interop);
        if (members == null) return null;
        return new Iterator<Object>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                while (interop.isArrayElementReadable(members, index)) {
                    try {
                        String key = Types.asString(interop.readArrayElement(members, index), interop);
                        if (interop.isMemberReadable(value, key)) return true;
                        index += 1;
                    } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
                        Errors.rethrow(RuntimeException.class, e);  // if element is readable, the methods mut not fail...
                    }
                }
                return false;
            }

            @Override
            public Object next() {
                index += 1;
                String key = null;
                try {
                    key = Types.asString(interop.readArrayElement(members, index - 1), interop);
                    return interop.readMember(value, key);
                } catch (UnsupportedMessageException | InvalidArrayIndexException | UnknownIdentifierException e) {
                    return Errors.noSuchElement(e, "Cannot access object member %s at index %d.", key, index - 1);
                }
            }
        };
    }

    public static Iterator<? extends IndexPair<?, ?>> tryAsIndexedIterator(Object value, InteropLibrary interop) {
        Iterator<? extends IndexPair<?, ?>> result;
        result = tryAsIndexedStringIterator(value, interop);
        if (result == null) result = tryAsIndexedArrayIterator(value, interop);
        if (result == null) result = tryAsIndexedMemberIterator(value, interop);
        return result;
    }

    public static Iterator<IndexPair<Integer, Character>> tryAsIndexedStringIterator(Object value, InteropLibrary interop) {
        String string = Types.tryAsString(value, interop);
        if (string == null) return null;
        return new Iterator<IndexPair<Integer, Character>>() {

            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < string.length();
            }

            @Override
            public IndexPair<Integer, Character> next() {
                index += 1;
                return new IndexPair<>(index - 1, string.charAt(index - 1));
            }
        };
    }

    public static Iterator<IndexPair<Integer, Object>> tryAsIndexedArrayIterator(Object value, InteropLibrary interop) {
        TruffleObject array = Types.tryAsArray(value, interop);
        if (array == null) return null;
        return new Iterator<IndexPair<Integer, Object>>() {

            private int index = 0;

            @Override
            public boolean hasNext() {
                // Note: We do not use getArraySize, because array-like iterators would have to pre-cache the whole array.
                return interop.isArrayElementReadable(value, index);
            }

            @Override
            public IndexPair<Integer, Object> next() {
                index += 1;
                try {
                    return new IndexPair<>(index - 1, interop.readArrayElement(value, index - 1));
                } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
                    return Errors.noSuchElement(e, "Cannot access array element at %d.", index - 1);
                }
            }
        };
    }

    public static Iterator<IndexPair<String, Object>> tryAsIndexedMemberIterator(Object value, InteropLibrary interop) {
        TruffleObject members = Types.tryReadMemberDescriptor(value, interop);
        if (members == null) return null;
        return new Iterator<IndexPair<String, Object>>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                while (interop.isArrayElementReadable(members, index)) {
                    try {
                        String key = Types.asString(interop.readArrayElement(members, index), interop);
                        if (interop.isMemberReadable(value, key)) return true;
                        index += 1;
                    } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
                        Errors.rethrow(RuntimeException.class, e);
                    }
                }
                return false;
            }

            @Override
            public IndexPair<String, Object> next() {
                index += 1;
                String key = null;
                try {
                    key = Types.asString(interop.readArrayElement(members, index - 1), interop);
                    return new IndexPair<>(key, interop.readMember(value, key));
                } catch (UnsupportedMessageException | InvalidArrayIndexException | UnknownIdentifierException e) {
                    return Errors.noSuchElement(e, "Cannot access object member %s at index %d.", key, index - 1);
                }
            }
        };
    }

}
