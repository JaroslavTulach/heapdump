package com.oracle.truffle.heap;

import com.oracle.truffle.api.interop.*;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;

/**
 * Implementations of built in global functions from OQL for individual objects.
 */
interface OQLGlobalSymbols {

    @ExportLibrary(InteropLibrary.class)
    class AllocTrace implements TruffleObject {

        public static final AllocTrace INSTANCE = new AllocTrace();

        private AllocTrace() {}

        @ExportMessage
        static boolean isExecutable(@SuppressWarnings("unused") AllocTrace receiver) {
            return true;
        }

        @ExportMessage
        static Object execute(@SuppressWarnings("unused") AllocTrace receiver, @SuppressWarnings("unused") Object[] arguments) {
            // TODO: In the original JS implementation, this uses an undefined method, fails, and then defaults to null...
            return HeapLanguage.NULL;
        }

    }

    /**
     * Returns Class object of a given Java Object.
     */
    @ExportLibrary(InteropLibrary.class)
    class ClassOf implements TruffleObject {

        public static final ClassOf INSTANCE = new ClassOf();

        private ClassOf() {}

        @ExportMessage
        static boolean isExecutable(@SuppressWarnings("unused") ClassOf receiver) {
            return true;
        }

        @ExportMessage
        static Object execute(@SuppressWarnings("unused") ClassOf receiver, Object[] arguments) throws ArityException, UnsupportedTypeException {
            Args.checkArity(arguments, 1);
            ObjectInstance argument = Args.unwrapInstance(arguments, 0, ObjectInstance.class);
            return ObjectJavaClass.create(argument.getInstance().getJavaClass());
        }

    }

    @ExportLibrary(InteropLibrary.class)
    class SizeOf implements TruffleObject {

        public static final SizeOf INSTANCE = new SizeOf();

        private SizeOf() {}

        @ExportMessage
        static boolean isExecutable(@SuppressWarnings("unused") SizeOf receiver) {
            return true;
        }

        @ExportMessage
        static Object execute(@SuppressWarnings("unused") SizeOf receiver, Object[] arguments) throws ArityException, UnsupportedTypeException {
            Args.checkArity(arguments, 1);
            ObjectInstance argument = Args.unwrapInstance(arguments, 0, ObjectInstance.class);
            return argument.getInstance().getSize();
        }

    }

    @ExportLibrary(InteropLibrary.class)
    class ToHtml implements TruffleObject {

        public static final ToHtml INSTANCE = new ToHtml();

        private ToHtml() {}

        @ExportMessage
        static boolean isExecutable(@SuppressWarnings("unused") ToHtml receiver) {
            return true;
        }

        @ExportMessage
        static Object execute(
                @SuppressWarnings("unused") ToHtml receiver,
                Object[] arguments,
                @CachedLibrary(limit = "3") InteropLibrary call
        ) throws ArityException, UnsupportedMessageException {
            Args.checkArity(arguments, 1);
            Object arg = arguments[0];
            // TODO: Is there an implicit "toString" conversion in truffle?
            return toHtml(arg, call);
        }

        private static String toHtml(Object arg, InteropLibrary call) throws UnsupportedMessageException {
            if (arg instanceof ObjectJavaClass) {
                JavaClass clazz = ((ObjectJavaClass) arg).getJavaClass();
                long id = clazz.getJavaClassId();
                return "<a href='file://class/" + id + "' name='" + id + "'>class " + clazz.getName() + "</a>";
            } else if (arg instanceof ObjectInstance) {
                Instance instance = ((ObjectInstance) arg).getInstance();
                long id = instance.getInstanceId();
                return "<a href='file://instance/" + id + "' name='" + id + "'>" + instance.getJavaClass().getName() + "#" + instance.getInstanceNumber() + "</a>";
            } else if (arg instanceof TruffleObject) {
                if (call.isNull(arg)) {
                    return "null";
                } else if (call.hasArrayElements(arg)) {
                    StringBuilder result = new StringBuilder("[");
                    int size = (int) call.getArraySize(arg);
                    for (int i=0; i < size; i++) {
                        try {
                            if (i != 0) result.append(", ");
                            result.append(toHtml(call.readArrayElement(arg, i), call));
                        } catch (InvalidArrayIndexException e) {
                            throw new RuntimeException("Cannot read array element.", e);    // should be unreachable
                        }
                    }
                    result.append("]");
                    return result.toString();
                } else if (call.hasMembers(arg)) {
                    StringBuilder result = new StringBuilder("{");
                    Object members = call.getMembers(arg);
                    int size = (int) call.getArraySize(members);
                    for (int i=0; i < size; i++) {
                        try {
                            if (i != 0) result.append(", ");
                            String key = (String) call.readArrayElement(members, i);
                            result.append(key);
                            result.append(": ");
                            result.append(toHtml(call.readMember(arg, key), call));
                        } catch (InvalidArrayIndexException | UnknownIdentifierException e) {
                            throw new RuntimeException("Cannot read object member.", e);    // should be unreachable
                        }
                    }
                    result.append("}");
                    return result.toString();
                } else {
                    // no members, not an array, well, we have to try something...
                    return toStringEscaped(arg);
                }
            } else {
                // arg is either a primitive value or something much more sinister - either case, just toString it...
                return toStringEscaped(arg);
            }
        }

        private static String toStringEscaped(Object arg) {
            return arg.toString().replace("<", "&lt;").replace(">", "&gt;");    // escape < >
        }
    }

}
