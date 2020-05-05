package com.oracle.truffle.heap;

import com.oracle.truffle.api.interop.*;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.interop.MemberDescriptor;
import com.oracle.truffle.heap.util.HeapLanguageUtils;
import com.oracle.truffle.heap.util.InstanceWrapper;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.lib.profiler.heap.PrimitiveArrayInstance;

import java.util.List;

/**
 * Primitive array instances are effectively string arrays from interop perspective.
 */
@ExportLibrary(InteropLibrary.class)
public class PrimitiveArrayObject extends InstanceWrapper<PrimitiveArrayInstance> implements TruffleObject {

    private static final MemberDescriptor MEMBERS = MemberDescriptor.build(new String[]{
            InstanceObject.CLAZZ, InstanceObject.STATICS, InstanceObject.ID, InstanceObject.WRAPPED_OBJECT
    }, new String[]{ InstanceObject.TO_STRING });

    @NonNull
    private final List<String> primitiveArray;  // underlying implementation is (supposedly) lazy

    public PrimitiveArrayObject(@NonNull PrimitiveArrayInstance instance) {
        super(instance);
        //noinspection unchecked
        this.primitiveArray = (List<String>) instance.getValues();
    }

    public int getLength() {
        return this.instance.getLength();
    }

    public PrimitiveArrayInstance getInstance() {
        return this.instance;
    }

    @ExportMessage
    static boolean hasArrayElements(@SuppressWarnings("unused") PrimitiveArrayObject receiver) {
        return true;
    }

    @ExportMessage
    static boolean isArrayElementReadable(PrimitiveArrayObject receiver, long index) {
        return index >= 0 && index < receiver.instance.getLength();
    }

    @ExportMessage
    static int getArraySize(PrimitiveArrayObject receiver) {
        return receiver.instance.getLength();
    }

    @ExportMessage
    static String readArrayElement(PrimitiveArrayObject receiver, long at) throws InvalidArrayIndexException {
        if (!isArrayElementReadable(receiver, at)) {
            throw InvalidArrayIndexException.create(at);
        }
        return receiver.primitiveArray.get((int) at);
    }

    @ExportMessage
    static boolean hasMembers(@SuppressWarnings("unused") PrimitiveArrayObject receiver) {
        return true;
    }

    @ExportMessage
    static Object getMembers(@SuppressWarnings("unused") PrimitiveArrayObject receiver, @SuppressWarnings("unused") boolean includeInternal) {
        return MEMBERS;
    }

    @ExportMessage
    static boolean isMemberReadable(@SuppressWarnings("unused") PrimitiveArrayObject receiver, String member) {
        return MEMBERS.hasProperty(member);
    }

    @ExportMessage
    static boolean isMemberInvocable(@SuppressWarnings("unused") PrimitiveArrayObject receiver, String member) {
        return MEMBERS.hasFunction(member);
    }

    @ExportMessage
    static Object invokeMember(PrimitiveArrayObject receiver, String member, Object[] arguments) throws ArityException, UnknownIdentifierException {
        if (InstanceObject.TO_STRING.equals(member)) {
            HeapLanguageUtils.arityCheck(0, arguments);
            return InstanceWrapper.instanceString(receiver);
        } else {
            throw UnknownIdentifierException.create(member);
        }
    }

    @ExportMessage
    static Object readMember(PrimitiveArrayObject receiver, String member) {
        switch (member) {
            case InstanceObject.CLAZZ:
                return new JavaClassObject(receiver.instance.getJavaClass());
            case InstanceObject.STATICS:
                return new StaticsObject(receiver.instance.getJavaClass());
            case InstanceObject.ID:
                return receiver.instance.getInstanceId();
            case InstanceObject.WRAPPED_OBJECT:
                return HeapLanguage.asGuestValue(receiver.instance);
            default:
                Object value = receiver.instance.getValueOfField(member);
                return HeapLanguageUtils.heapToTruffle(value);
        }
    }

}
