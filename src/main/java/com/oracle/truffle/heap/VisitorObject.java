package com.oracle.truffle.heap;

import com.oracle.truffle.api.interop.*;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.heap.interop.Args;
import com.oracle.truffle.heap.interop.Interop;
import com.oracle.truffle.heap.interop.MemberDescriptor;
import com.oracle.truffle.heap.interop.Types;
import org.graalvm.polyglot.Value;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

@ExportLibrary(InteropLibrary.class)
public class VisitorObject implements TruffleObject {

    private static final String VISIT = "visit";
    private static final MemberDescriptor MEMBERS = MemberDescriptor.functions(VISIT);

    //@NonNull
    //private final Object visitor;

    private final Object hostVisitor;
    private final Method hostVisitorMethod;

    public VisitorObject(@NonNull Object visitor) {
        //this.visitor = visitor;
        this.hostVisitor = HeapLanguage.tryAsHostObject(visitor);
        if (this.hostVisitor == null) {
            throw new IllegalArgumentException("Expected the visitor to be a host object.");
        } else {
            try {
                this.hostVisitorMethod = this.hostVisitor.getClass().getMethod("visit", Object.class);
                this.hostVisitorMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException("Expected the visitor to be a host object with visit(Object) method.");
            }
        }
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
            throws UnknownIdentifierException, ArityException, UnsupportedTypeException, UnsupportedMessageException {
        if (VISIT.equals(member)) {
            Args.checkArity(arguments, 1);
            return receiver.dispatchValue(arguments[0]);
        } else {
            throw UnknownIdentifierException.create(member);
        }
    }

    private boolean dispatchValue(Object value) throws UnsupportedMessageException {
        if (value == null) return false;
        InteropLibrary interop = InteropLibrary.getFactory().getUncached();
        Object directDispatch = null;
        if (Types.isPrimitiveValue(value)) {
            directDispatch = value;
        } else if (value instanceof ObjectInstance) {
            // First, try to dispatch heap language objects as their original interfaces using reflection...
            Instance instance = ((ObjectInstance) value).getInstance();
            directDispatch = HeapLanguage.asHostInterface(instance, Instance.class.getName());
        } else if (value instanceof ObjectJavaClass) {
            JavaClass javaClass = ((ObjectJavaClass) value).getJavaClass();
            directDispatch = HeapLanguage.asHostInterface(javaClass, JavaClass.class.getName());
        } else if (value instanceof ObjectHeap) {
            Heap heap = ((ObjectHeap) value).getHeap();
            directDispatch = HeapLanguage.asHostInterface(heap, Heap.class.getName());
        } else if (interop.hasArrayElements(value)) {
            // Second, if the object is an array, we recursively dispatch every element...
            int i = 0;
            while (i < interop.getArraySize(value)) {
                if (interop.isArrayElementReadable(value, i)) {
                    try {
                        Object item = interop.readArrayElement(value, i);
                        if (dispatchValue(item)) return true;
                    } catch (InvalidArrayIndexException e) {
                        Interop.rethrow(RuntimeException.class, e);
                    }
                }
                i += 1;
            }
        } else if (interop.hasMembers(value)) {
            // Third, if the value has members, we can cast it to Map...
            directDispatch = Value.asValue(value).as(Map.class);
        } else {
            // When there is nothing else to do, well, turn it into a value and hope for the best.
            directDispatch = Value.asValue(value);
        }

        try {
            return directDispatch != null && (boolean) this.hostVisitorMethod.invoke(this.hostVisitor, directDispatch);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw Interop.rethrow(RuntimeException.class, e);
        }
    }

}
