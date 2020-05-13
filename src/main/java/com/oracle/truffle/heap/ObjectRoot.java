package com.oracle.truffle.heap;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.heap.interop.MemberDescriptor;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import org.netbeans.lib.profiler.heap.GCRoot;

@ExportLibrary(InteropLibrary.class)
public class ObjectRoot implements TruffleObject {

    private static final String ID = "id";
    private static final String DESCRIPTION = "description";
    private static final String REFERRER = "referrer";
    private static final String TYPE = "type";

    private static final MemberDescriptor MEMBERS = MemberDescriptor.properties(ID, DESCRIPTION, REFERRER, TYPE);

    private final GCRoot root;

    public ObjectRoot(GCRoot root) {
        this.root = root;
    }

    @ExportMessage
    static boolean hasMembers(@SuppressWarnings("unused") ObjectRoot receiver) {
        return true;
    }

    @ExportMessage
    static Object getMembers(@SuppressWarnings("unused") ObjectRoot receiver, @SuppressWarnings("unused") boolean includeInternal) {
        return MEMBERS;
    }

    @ExportMessage
    static boolean isMemberReadable(@SuppressWarnings("unused") ObjectRoot receiver, String member) {
        return MEMBERS.hasProperty(member);
    }

    @ExportMessage
    static Object readMember(ObjectRoot receiver, String member) throws UnknownIdentifierException {
        switch (member) {
            case ID:
            case TYPE:
                return receiver.root.getKind();
            case DESCRIPTION:
                return "Reference " + receiver.root.getKind();
            case REFERRER:
                return ObjectInstance.create(receiver.root.getInstance());
            default:
                throw UnknownIdentifierException.create(member);
        }
    }

}
