package com.oracle.truffle.heap.util;

import com.oracle.truffle.api.interop.*;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.modules.profiler.oql.engine.api.OQLEngine;

@ExportLibrary(InteropLibrary.class)
public class VisitorObject implements TruffleObject {

    private static final String VISIT = "visit";
    private static final MemberDescriptor MEMBERS = MemberDescriptor.functions(VISIT);

    @NullAllowed
    private final OQLEngine.ObjectVisitor visitor;

    public VisitorObject(@NullAllowed OQLEngine.ObjectVisitor visitor) {
        this.visitor = visitor;
    }

    @ExportMessage
    static boolean hasMembers(@SuppressWarnings("unused") VisitorObject receiver) {
        return true;
    }

    @ExportMessage
    static Object getMembers(@SuppressWarnings("unused") VisitorObject receiver, @SuppressWarnings("unused") boolean includeInternal) {
        return MEMBERS;
    }

    @ExportMessage
    static boolean isMemberInvocable(@SuppressWarnings("unused") VisitorObject receiver, String member) {
        return MEMBERS.hasFunction(member);
    }

    @ExportMessage
    static Object invokeMember(VisitorObject receiver, String member, Object[] arguments)
            throws UnknownIdentifierException, ArityException {
        if (VISIT.equals(member)) {
            Args.checkArity(arguments, 1);
            boolean finished = false;
            if (receiver.visitor != null) {
                finished = receiver.visitor.visit(HeapLanguageUtils.truffleToHeap(arguments[0]));
            }
            return finished;
        } else {
            throw UnknownIdentifierException.create(member);
        }
    }


}
