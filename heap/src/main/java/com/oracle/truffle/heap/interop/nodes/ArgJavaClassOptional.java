package com.oracle.truffle.heap.interop.nodes;

import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.heap.HeapUtils;
import com.oracle.truffle.heap.ObjectJavaClass;
import com.oracle.truffle.heap.interop.Args;
import com.oracle.truffle.heap.interop.Errors;
import com.oracle.truffle.heap.interop.Types;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.JavaClass;

/**
 * Unwrap an optional JavaClass argument, defaulting to null.
 */
@GenerateUncached
@ImportStatic(Args.class)
public abstract class ArgJavaClassOptional extends Node {
    public abstract JavaClass execute(Object[] arguments, int index, Heap heap) throws UnsupportedTypeException;

    static boolean isJavaClass(Object[] arguments, int index) {
        return Args.isMissing(arguments, index) || arguments[index] instanceof ObjectJavaClass;
    }

    @Specialization(guards = "isMissing(arguments, index)") @SuppressWarnings("unused")
    static JavaClass doMissing(Object[] arguments, int index, Heap heap) {
        return null;
    }

    @Specialization(guards = "isJavaClass(arguments, index)", replaces = "doMissing")
    static JavaClass doJavaClass(Object[] arguments, int index, @SuppressWarnings("unused") Heap heap){
        return Args.isMissing(arguments, index) ? null : ((ObjectJavaClass) arguments[index]).getJavaClass();
    }

    @Specialization(replaces = "doJavaClass")
    static JavaClass doGeneric(Object[] arguments, int index, Heap heap, @CachedLibrary(limit = "3") InteropLibrary interop) throws UnsupportedTypeException {
        Object arg = (index >= arguments.length) ? null : arguments[index];
        if (arg == null) {
            return null;
        } else if (arg instanceof ObjectJavaClass) {
            return ((ObjectJavaClass) arg).getJavaClass();
        } else if (arg instanceof String || interop.isString(arg)) {
            String className = Types.asString(arg, interop);
            JavaClass clazz = HeapUtils.findClass(heap, className); // Truffle Boundary!
            return clazz != null ? clazz : Errors.illegalArgument("Class "+className+" not found in heap dump.");
        } else if (interop.isNull(arg)) {
            return null;
        } else {
            return Errors.unsupportedType(arguments, "Expected JavaClass or String as argument %d but found %s.", index+1, arg);
        }
    }

}
