package com.oracle.truffle.api.interop;

import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * <p>Exposes Java iterator as a lazy truffle array. Currently, truffle does not have an iterator interface
 * so this is basically the best we can do unless we want to emulate each language individually.</p>
 *
 * <p>If you want to access the values in a truly lazy way, we also export the {@code hasNext()/next()} methods
 * as defined by Java {@link Iterator}.</p>
 */
@ExportLibrary(InteropLibrary.class)
final class InteropIterator<T> implements TruffleObject, Iterator<T> {

    private static final String HAS_NEXT = "hasNext";
    private static final String NEXT = "next";

    private static final MemberDescriptor MEMBERS = MemberDescriptor.functions(HAS_NEXT, NEXT);

    private final Iterator<T> iterator;
    private final ArrayList<Object> valueCache;

    public InteropIterator(Iterator<T> iterator) {
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
    public static boolean hasArrayElements(@SuppressWarnings("unused") InteropIterator<?> receiver) {
        return true;
    }

    @ExportMessage
    public static long getArraySize(InteropIterator<?> receiver) {
        // Here, we sadly have to iterate the whole array at the moment...
        // TODO: With latest JS and python, we should be able to just extend the array by one.
        while (receiver.hasNext()) { receiver.next(); }
        return receiver.valueCache.size();
    }

    @ExportMessage
    public static boolean isArrayElementReadable(InteropIterator<?> receiver, long index) {
        while (receiver.hasNext() && index >= receiver.valueCache.size()) {
            receiver.next();
        }
        return index < receiver.valueCache.size();
    }

    @ExportMessage
    public static Object readArrayElement(InteropIterator<?> receiver, long index) {
        if (!isArrayElementReadable(receiver, index)) {
            throw new ArrayIndexOutOfBoundsException((int) index);
        }
        return receiver.valueCache.get((int) index);
    }

    @ExportMessage
    static boolean hasMembers(@SuppressWarnings("unused") InteropIterator<?> receiver) {
        return true;
    }

    @ExportMessage
    static Object getMembers(@SuppressWarnings("unused") InteropIterator<?> receiver,
                             @SuppressWarnings("unused") boolean includeInternal) {
        return InteropIterator.MEMBERS;
    }

    @ExportMessage
    static boolean isMemberInvocable(@SuppressWarnings("unused") InteropIterator<?> receiver, String member) {
        return MEMBERS.hasFunction(member);
    }

    @ExportMessage
    static Object invokeMember(InteropIterator<?> receiver, String member, Object[] arguments)
            throws ArityException, UnknownIdentifierException
    {
        Args.checkArity(arguments, 0);
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
