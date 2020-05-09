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
                return receiver.dispatchValue(value);
            }
            return finished;
        } else {
            throw UnknownIdentifierException.create(member);
        }
    }

    private boolean dispatchValue(Object value) {
        // TODO: WTF this control flow?
        if (visitor == null) return false;
        if (value == null) return false;
        InteropLibrary interop = InteropLibrary.getFactory().getUncached();
        // If this object is "iterable", deliver each item separately...
        Boolean heapDispatch = dispatchHeap(value); // first, unwrap all ArrayInstances, etc. because we don't want to iterate those...
        if (heapDispatch != null) {
            return heapDispatch;
        }
        if (value instanceof TruffleObject && interop.hasArrayElements(value)) {
            try {
                int i = 0;
                while (i < interop.getArraySize(value)) {
                    if (interop.isArrayElementReadable(value, i)) {
                        Object item = interop.readArrayElement(value, i);
                        if (dispatchValue(item)) return true;
                    }
                    i += 1;
                }
                return false;
            } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
                throw new IllegalStateException("Value is array, but cannot access elemnts.", e);
            }
        } else {
            value = transformValue(value);
            return visitor.visit(value);
        }
    }

    private Boolean dispatchHeap(Object value) {
        if (visitor == null) return null;
        if (value instanceof ObjectInstance) return visitor.visit(((ObjectInstance) value).getInstance());
        else if (value instanceof ObjectJavaClass) return visitor.visit(((ObjectJavaClass) value).getJavaClass());
        else if (value instanceof ObjectHeap) return visitor.visit(((ObjectHeap) value).getHeap());
        return null;
    }

    private static Object transformValue(Object value) {
        // TODO: Cover other objects as well...
        // This is a heap language object, just unwrap it, keep primitive values and try to convert everything else to map...
        if (Types.isNull(value)) return null;
        //else if (value instanceof ObjectInstance) return ((ObjectInstance) value).getInstance();
        //else if (value instanceof ObjectJavaClass) return ((ObjectJavaClass) value).getJavaClass();
        //else if (value instanceof ObjectHeap) return ((ObjectHeap) value).getHeap();
        else if (!Types.isPrimitiveValue(value)) {
            return Value.asValue(value).as(Map.class);
        }
        return value;
    }

}
