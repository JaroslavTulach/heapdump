package com.oracle.truffle.heap;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.MemberDescriptor;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.Types;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;

import java.util.List;

/**
 * <p>A truffle object that holds references to values of static fields of a particular {@link JavaClass}.</p>
 */
@ExportLibrary(InteropLibrary.class)
final class ObjectStatics implements TruffleObject {

    public static TruffleObject create(@NullAllowed JavaClass clazz) {
        if (clazz == null) {
            return HeapLanguage.NULL;
        } else {
            return new ObjectStatics(clazz);
        }
    }

    @NonNull
    private final JavaClass clazz;

    @NonNull
    private final MemberDescriptor members;

    private ObjectStatics(@NonNull JavaClass clazz) {
        this.clazz = clazz;
        //noinspection unchecked
        List<FieldValue> staticFieldValues = (List<FieldValue>) clazz.getStaticFieldValues();
        String[] members = new String[staticFieldValues.size()];
        for (int i = 0; i < staticFieldValues.size(); i++) {
            FieldValue value = staticFieldValues.get(i);
            members[i] = value.getField().getName();
        }
        this.members = MemberDescriptor.build(members, null);
    }

    @ExportMessage
    static boolean hasMembers(@SuppressWarnings("unused") ObjectStatics receiver) {
        return true;
    }

    @ExportMessage
    static Object getMembers(ObjectStatics receiver, @SuppressWarnings("unused") boolean includeInternal) {
        return receiver.members;
    }

    @ExportMessage
    static boolean isMemberReadable(ObjectStatics receiver, String member) {
        return receiver.members.contains(member);
    }

    @ExportMessage
    static Object readMember(ObjectStatics receiver, String member) {
        Object value = receiver.clazz.getValueOfStaticField(member);
        if (value == null || value instanceof Instance) {
            return ObjectInstance.create((Instance) value);
        } else if (Types.isPrimitiveValue(value)) {
            return value;
        } else {
            throw new IllegalStateException("unreachable");
        }
    }

}
