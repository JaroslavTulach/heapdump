package com.oracle.truffle.heap;

import com.oracle.truffle.api.interop.*;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.lib.profiler.heap.*;

import java.util.List;

/**
 * <p>A wrapper around {@link org.netbeans.lib.profiler.heap.Instance} objects. It actually provides three
 * possible implementations depending on whether the instance is object, array or primitive array. However,
 * most of the functionality is shared.</p>
 *
 * <p>By default, it allows accessing all fields of the given instance object plus several special ones
 * (clazz, statics, id, wrapped-object) and a toString method. Array instances then also enable array-like
 * access.</p>
 */
@ExportLibrary(InteropLibrary.class)
class ObjectInstance implements TruffleObject {

    public static TruffleObject create(@NullAllowed Instance instance) {
        if (instance == null) {
            return HeapLanguage.NULL;
        } else if (instance instanceof PrimitiveArrayInstance) {
            return new ObjectInstance.PrimitiveArray((PrimitiveArrayInstance) instance);
        } else if (instance instanceof ObjectArrayInstance) {
            return new ObjectInstance.ObjectArray((ObjectArrayInstance) instance);
        } else {
            return new ObjectInstance(instance);
        }
    }

    static final String CLAZZ = "clazz";
    static final String STATICS = "statics";
    static final String ID = "id";
    static final String WRAPPED_OBJECT = "wrapped-object";
    static final String TO_STRING = "toString";

    protected final MemberDescriptor members;
    protected final Instance instance;

    protected ObjectInstance(@NonNull Instance instance) {
        this.instance = instance;
        //noinspection unchecked
        List<FieldValue> fieldValues = (List<FieldValue>) instance.getFieldValues();
        // Add extra members to the class fields
        String[] members = new String[fieldValues.size() + 4];
        members[0] = "clazz";
        members[1] = "statics";
        members[2] = "id";
        members[3] = "wrapped-object";
        for (int i = 0; i < fieldValues.size(); i++) {
            FieldValue value = fieldValues.get(i);
            members[i + 4] = value.getField().getName();
        }
        this.members = MemberDescriptor.build(members, new String[]{ TO_STRING });
    }

    public Instance getInstance() {
        return instance;
    }

    @ExportMessage
    boolean hasMembers() {
        return true;
    }

    @ExportMessage
    Object getMembers(@SuppressWarnings("unused") boolean includeInternal) {
        return this.members;
    }

    @ExportMessage
    boolean isMemberReadable(String member) {
        return this.members.hasProperty(member);
    }

    @ExportMessage
    boolean isMemberInvocable(String member) {
        return this.members.hasFunction(member);
    }

    @ExportMessage
    Object invokeMember(String member, Object[] arguments) throws ArityException, UnknownIdentifierException {
        if (TO_STRING.equals(member)) {
            Args.checkArity(arguments, 0);
            return HeapUtils.instanceString(this.instance);
        } else {
            throw UnknownIdentifierException.create(member);
        }
    }

    @ExportMessage
    Object readMember(String member) {
        switch (member) {
            case CLAZZ:
                return ObjectJavaClass.create(this.instance.getJavaClass());
            case STATICS:
                return ObjectStatics.create(this.instance.getJavaClass());
            case ID:
                return this.instance.getInstanceId();
            case WRAPPED_OBJECT:
                return HeapLanguage.asGuestValue(this.instance);
            default:
                Object value = this.instance.getValueOfField(member);
                if (value == null || value instanceof Instance) {
                    return ObjectInstance.create((Instance) value);
                } else if (Types.isPrimitiveValue(value)) {
                    return value;
                } else {
                    throw new IllegalStateException("unreachable");
                }
        }
    }

    @ExportLibrary(InteropLibrary.class)
    static final class ObjectArray extends ObjectInstance {

        private final List<Instance> items;

        private ObjectArray(@NonNull ObjectArrayInstance instance) {
            super(instance);
            //noinspection unchecked
            this.items = (List<Instance>) instance.getValues();
        }

        public List<Instance> getItems() {
            return items;
        }

        @ExportMessage
        public boolean hasArrayElements() {
            return true;
        }

        @ExportMessage
        public long getArraySize() {
            return this.items.size();
        }

        @ExportMessage
        public boolean isArrayElementReadable(long index) {
            return 0 <= index && index < this.items.size();
        }

        @ExportMessage
        public Object readArrayElement(long index) throws InvalidArrayIndexException {
            if (!isArrayElementReadable(index)) {
                throw InvalidArrayIndexException.create(index);
            }
            return ObjectInstance.create(this.items.get((int) index));
        }

    }

    @ExportLibrary(InteropLibrary.class)
    static final class PrimitiveArray extends ObjectInstance {

        private final List<String> items;

        private PrimitiveArray(@NonNull PrimitiveArrayInstance instance) {
            super(instance);
            //noinspection unchecked
            this.items = (List<String>) instance.getValues();
        }

        @ExportMessage
        public boolean hasArrayElements() {
            return true;
        }

        @ExportMessage
        public long getArraySize() {
            return items.size();
        }

        @ExportMessage
        public boolean isArrayElementReadable(long index) {
            return 0 <= index && index <= items.size();
        }

        @ExportMessage
        public Object readArrayElement(long index) throws InvalidArrayIndexException {
            if (!isArrayElementReadable(index)) {
                throw InvalidArrayIndexException.create(index);
            }
            return this.items.get((int) index);
        }

    }

}
