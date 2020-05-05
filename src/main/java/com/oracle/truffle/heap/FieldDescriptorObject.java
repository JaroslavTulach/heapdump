package com.oracle.truffle.heap;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.MemberDescriptor;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import org.netbeans.api.annotations.common.NonNull;

/**
 * Holds the name and signature of a class field.
 */
@ExportLibrary(InteropLibrary.class)
public class FieldDescriptorObject implements TruffleObject {

    private static final String NAME = "name";
    private static final String SIGNATURE = "signature";

    private static final MemberDescriptor MEMBERS = MemberDescriptor.properties(NAME, SIGNATURE);

    @NonNull
    private final String name;

    @NonNull
    private final String signature;

    public FieldDescriptorObject(@NonNull String name, @NonNull String signature) {
        this.name = name;
        this.signature = signature;
    }

    @ExportMessage
    static boolean hasMembers(@SuppressWarnings("unused") FieldDescriptorObject receiver) {
        return true;
    }

    @ExportMessage
    static Object getMembers(@SuppressWarnings("unused") FieldDescriptorObject receiver, @SuppressWarnings("unused") boolean includeInternal) {
        return MEMBERS;
    }

    @ExportMessage
    static boolean isMemberReadable(@SuppressWarnings("unused") FieldDescriptorObject receiver, String member) {
        return MEMBERS.contains(member);
    }

    @ExportMessage
    static Object readMember(FieldDescriptorObject receiver, String member) throws UnknownIdentifierException {
        switch (member) {
            case NAME:
                return receiver.name;
            case SIGNATURE:
                return receiver.signature;
            default:
                throw UnknownIdentifierException.create(member);
        }
    }


}
