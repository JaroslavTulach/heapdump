package com.oracle.truffle.heap;

import com.oracle.truffle.api.interop.*;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.interop.MemberDescriptor;
import com.oracle.truffle.heap.util.HeapLanguageUtils;
import com.oracle.truffle.heap.util.InstanceWrapper;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.ObjectArrayInstance;

import java.util.List;

@ExportLibrary(InteropLibrary.class)
public class ObjectArrayObject extends InstanceWrapper<ObjectArrayInstance> implements TruffleObject {

    private static final MemberDescriptor MEMBERS = MemberDescriptor.build(new String[]{
        InstanceObject.CLAZZ, InstanceObject.STATICS, InstanceObject.ID, InstanceObject.WRAPPED_OBJECT
    }, new String[]{ InstanceObject.TO_STRING });

    @NonNull
    private final List<Instance> instanceArray;  // underlying implementation is (supposedly) lazy

    public ObjectArrayObject(@NonNull ObjectArrayInstance instance) {
        super(instance);
        //noinspection unchecked
        this.instanceArray = (List<Instance>) instance.getValues();
    }

    public int getLength() {
        return this.instance.getLength();
    }

    public ObjectArrayInstance getInstance() {
        return instance;
    }

    @ExportMessage
    static boolean hasArrayElements(@SuppressWarnings("unused") ObjectArrayObject receiver) {
        return true;
    }

    @ExportMessage
    static boolean isArrayElementReadable(ObjectArrayObject receiver, long index) {
        return index >= 0 && index < receiver.instance.getLength();
    }

    @ExportMessage
    static int getArraySize(ObjectArrayObject receiver) {
        return receiver.instance.getLength();
    }

    @ExportMessage
    static Object readArrayElement(ObjectArrayObject receiver, long at) throws InvalidArrayIndexException {
        if (!isArrayElementReadable(receiver, at)) {
            throw InvalidArrayIndexException.create(at);
        }
        return HeapLanguageUtils.heapToTruffle(receiver.instanceArray.get((int) at));
    }

    @ExportMessage
    static boolean hasMembers(@SuppressWarnings("unused") ObjectArrayObject receiver) {
        return true;
    }

    @ExportMessage
    static Object getMembers(ObjectArrayObject receiver, @SuppressWarnings("unused") boolean includeInternal) {
        return MEMBERS;
    }

    @ExportMessage
    static boolean isMemberReadable(ObjectArrayObject receiver, String member) {
        return MEMBERS.hasProperty(member);
    }

    @ExportMessage
    static boolean isMemberInvocable(ObjectArrayObject receiver, String member) {
        return MEMBERS.hasFunction(member);
    }

    @ExportMessage
    static Object invokeMember(ObjectArrayObject receiver, String member, Object[] arguments) throws ArityException, UnknownIdentifierException {
        if (InstanceObject.TO_STRING.equals(member)) {
            Args.checkArity(arguments, 0);
            return InstanceWrapper.instanceString(receiver);
        } else {
            throw UnknownIdentifierException.create(member);
        }
    }

    @ExportMessage
    static Object readMember(ObjectArrayObject receiver, String member) {
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
