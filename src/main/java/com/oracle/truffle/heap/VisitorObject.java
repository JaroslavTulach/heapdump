package com.oracle.truffle.heap;

import com.oracle.truffle.api.interop.*;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import org.graalvm.polyglot.Value;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.modules.profiler.oql.engine.api.OQLEngine;

import java.util.Map;

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
                Object value = arguments[0];
                // This is a heap language object, just unwrap it, keep primitive values and try to convert everything else to map...
                if (Types.isNull(value)) value = null;
                else if (value instanceof ObjectInstance) value = ((ObjectInstance) value).getInstance();
                else if (value instanceof ObjectJavaClass) value = ((ObjectJavaClass) value).getJavaClass();
                else if (value instanceof ObjectHeap) value = ((ObjectHeap) value).getHeap();
                else if (!Types.isPrimitiveValue(value)) value = Value.asValue(value).as(Map.class);
                finished = receiver.visitor.visit(value);
            }
            return finished;
        } else {
            throw UnknownIdentifierException.create(member);
        }
    }


}
