package com.oracle.truffle.heap;

import com.oracle.truffle.api.interop.*;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.heap.interop.Args;
import com.oracle.truffle.heap.interop.Iterators;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.modules.profiler.oql.engine.api.impl.ReachableObjects;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

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
    class ForEachReferrer implements TruffleObject {

        public static final ForEachReferrer INSTANCE = new ForEachReferrer();

        private ForEachReferrer() {}

        @ExportMessage
        static boolean isExecutable(@SuppressWarnings("unused") ForEachReferrer receiver) {
            return true;
        }

        @ExportMessage
        static Object execute(ForEachReferrer receiver, Object[] arguments) throws ArityException, UnsupportedTypeException, UnsupportedMessageException {
            Args.checkArityBetween(arguments, 2, 3);
            // TODO: argument might be a JavaClass, not an instance...
            boolean includeWeak = false;
            if (arguments.length == 3) includeWeak = Args.unwrapBoolean(arguments, 1);
            ObjectInstance instance = Args.unwrapInstance(arguments, 1, ObjectInstance.class);
            TruffleObject callback = HeapLanguage.unwrapCallbackArgument(arguments, 0, "it");
            Iterator<Instance> referrers = HeapUtils.getReferrers(instance.getInstance(), includeWeak);
            InteropLibrary interop = InteropLibrary.getFactory().getUncached();
            while (referrers.hasNext()) {
                interop.execute(callback, ObjectInstance.create(referrers.next()));
            }
            return instance;
        }

    }

    @ExportLibrary(InteropLibrary.class)
    class ForEachReferee implements TruffleObject {

        public static final ForEachReferee INSTANCE = new ForEachReferee();

        private ForEachReferee() {}

        @ExportMessage
        static boolean isExecutable(@SuppressWarnings("unused") ForEachReferee receiver) {
            return true;
        }

        @ExportMessage
        static Object execute(@SuppressWarnings("unused") ForEachReferee receiver, Object[] arguments) throws UnsupportedTypeException, ArityException, UnsupportedMessageException {
            Args.checkArityBetween(arguments, 2, 3);
            // TODO: argument might be a JavaClass, not an instance...
            boolean includeWeak = false;
            if (arguments.length == 3) includeWeak = Args.unwrapBoolean(arguments, 1);
            ObjectInstance instance = Args.unwrapInstance(arguments, 1, ObjectInstance.class);
            TruffleObject callback = HeapLanguage.unwrapCallbackArgument(arguments, 0, "it");
            Iterator<Instance> referrers = HeapUtils.getReferees(instance.getInstance(), includeWeak);
            InteropLibrary interop = InteropLibrary.getFactory().getUncached();
            while (referrers.hasNext()) {
                interop.execute(callback, ObjectInstance.create(referrers.next()));
            }
            return instance;
        }

    }

    @ExportLibrary(InteropLibrary.class)
    class Identical implements TruffleObject {

        public static final Identical INSTANCE = new Identical();

        private Identical() {}

        @ExportMessage
        static boolean isExecutable(Identical receiver) {
            return true;
        }

        @ExportMessage
        static Object execute(Identical receiver, Object[] arguments) throws ArityException, UnsupportedTypeException {
            Args.checkArity(arguments, 2);
            return identical(arguments[0], arguments[1]);
        }

        // Version of identity that defaults to object equality when not heap object.
        static boolean identical(Object lhs, Object rhs) {
            if (lhs instanceof ObjectInstance || lhs instanceof ObjectJavaClass) {
                try {
                    lhs = ObjectId.execute(null, new Object[]{ lhs });
                } catch (ArityException | UnsupportedTypeException e) {
                    // do nothing...
                }
            }
            if (rhs instanceof ObjectInstance || rhs instanceof ObjectJavaClass) {
                try {
                    rhs = ObjectId.execute(null, new Object[] { rhs });
                } catch (ArityException | UnsupportedTypeException e) {
                    // do nothing...
                }
            }
            if (lhs == null || rhs == null) return lhs == rhs;
            return lhs.equals(rhs);
        }

    }

    @ExportLibrary(InteropLibrary.class)
    class ObjectId implements TruffleObject {

        public static final ObjectId INSTANCE = new ObjectId();

        private ObjectId() {}

        @ExportMessage
        static boolean isExecutable(@SuppressWarnings("unused") ObjectId receiver) {
            return true;
        }

        @ExportMessage
        static Object execute(@SuppressWarnings("unused") ObjectId receiver, Object[] arguments) throws ArityException, UnsupportedTypeException {
            Args.checkArity(arguments, 1);
            Object arg = arguments[0];
            if (arg instanceof ObjectInstance) {
                return ((ObjectInstance) arg).getInstance().getInstanceId();
            } else if (arg instanceof ObjectJavaClass) {
                return ((ObjectJavaClass) arg).getJavaClass().getJavaClassId();
            } else {
                throw UnsupportedTypeException.create(arguments, "Expected class or instance object as argument, but found "+arg);
            }
        }

    }

    @ExportLibrary(InteropLibrary.class)
    class Reachables implements TruffleObject {

        public static final Reachables INSTANCE = new Reachables();

        private Reachables() {}

        @ExportMessage
        static boolean isExecutable(@SuppressWarnings("unused") Reachables receiver) {
            return true;
        }

        @ExportMessage
        static Object execute(@SuppressWarnings("unused") Reachables receiver, Object[] arguments) throws ArityException, UnsupportedTypeException {
            Args.checkArityBetween(arguments, 1, 2);
            Instance arg = Args.unwrapInstance(arguments, 0, ObjectInstance.class).getInstance();
            ReachableObjects ro;
            if (arguments.length == 1) {
                ro = new ReachableObjects(arg, null);
            } else {
                String fields = Args.unwrapString(arguments, 1);
                StringTokenizer tokens = new StringTokenizer(fields, ",");
                ArrayList<String> excluded = new ArrayList<>();
                while (tokens.hasMoreTokens()) {
                    excluded.add(tokens.nextToken().trim());
                }
                ro = new ReachableObjects(arg, excluded::contains);
            }

            return Iterators.exportIterator(new IteratorObjectInstance(ro.getReachables()));
        }

    }

    @ExportLibrary(InteropLibrary.class)
    class Referrers implements TruffleObject {

        public static final Referrers INSTANCE = new Referrers();

        private Referrers() {}

        @ExportMessage
        static boolean isExecutable(@SuppressWarnings("unused") Referrers receiver) {
            return true;
        }

        @ExportMessage
        static Object execute(@SuppressWarnings("unused") Referrers receiver, Object[] arguments) throws ArityException, UnsupportedTypeException {
            Args.checkArityBetween(arguments, 1, 2);
            // TODO: first argument might be a JavaClass, not an instance...
            boolean includeWeak = false;
            if (arguments.length == 2) includeWeak = Args.unwrapBoolean(arguments, 1);
            ObjectInstance instance = Args.unwrapInstance(arguments, 0, ObjectInstance.class);
            Iterator<Instance> referrers = HeapUtils.getReferrers(instance.getInstance(), includeWeak);
            return Iterators.exportIterator(new IteratorObjectInstance(referrers));
        }

    }

    @ExportLibrary(InteropLibrary.class)
    class Referees implements TruffleObject {

        public static final Referees INSTANCE = new Referees();

        private Referees() {}

        @ExportMessage
        static boolean isExecutable(@SuppressWarnings("unused") Referees receiver) {
            return true;
        }

        @ExportMessage
        static Object execute(@SuppressWarnings("unused") Referees receiver, Object[] arguments) throws ArityException, UnsupportedTypeException {
            Args.checkArityBetween(arguments, 1, 2);
            boolean includeWeak = false;
            if (arguments.length == 2) includeWeak = Args.unwrapBoolean(arguments, 1);
            Object arg = arguments[0];
            if (arg instanceof ObjectJavaClass) {
                Iterator<Instance> referrers = HeapUtils.getReferees(((ObjectJavaClass) arg).getJavaClass(), includeWeak);
                return Iterators.exportIterator(new IteratorObjectInstance(referrers));
            } else if (arg instanceof ObjectInstance) {
                Iterator<Instance> referrers = HeapUtils.getReferees(((ObjectInstance) arg).getInstance(), includeWeak);
                return Iterators.exportIterator(new IteratorObjectInstance(referrers));
            } else {
                throw UnsupportedTypeException.create(arguments, "Expected class or object instance as first argument, but got "+arg);
            }
        }
    }

    @ExportLibrary(InteropLibrary.class)
    class Refers implements TruffleObject {

        public static final Refers INSTANCE = new Refers();

        private Refers() {}

        @ExportMessage
        static boolean isExecutable(@SuppressWarnings("unused") Refers receiver) {
            return true;
        }

        @ExportMessage
        static Object execute(@SuppressWarnings("unused") Refers receiver, Object[] arguments) throws ArityException, UnsupportedTypeException {
            Args.checkArity(arguments, 2);
            Object from = arguments[0];
            if (from instanceof ObjectInstance.PrimitiveArray) {
                return false;
            } else if (from instanceof ObjectInstance.ObjectArray) {
                for (Instance item : ((ObjectInstance.ObjectArray) from).getItems()) {
                    if (Identical.identical(item, arguments[1])) {
                        return true;
                    }
                }
                return false;
            } else if (from instanceof ObjectInstance) {
                ObjectInstance instance = (ObjectInstance) from;
                for (String member : instance.members.getProperties()) {
                    if (instance.isMemberReadable(member)) {
                        Object item = instance.readMember(member);
                        if (Identical.identical(item, arguments[1])) {
                            return true;
                        }
                    }
                }
            } else if (from instanceof ObjectJavaClass) {
                JavaClass clazz = ((ObjectJavaClass) from).getJavaClass();
                //noinspection unchecked
                List<FieldValue> staticFieldValues = (List<FieldValue>) clazz.getStaticFieldValues();
                for (FieldValue field : staticFieldValues) {
                    Object value = clazz.getValueOfStaticField(field.getField().getName());
                    if (Identical.identical(value, arguments[1])) {
                        return true;
                    }
                }
            }
            return false;
        }

    }

    @ExportLibrary(InteropLibrary.class)
    class Root implements TruffleObject {

        public static final Root INSTANCE = new Root();

        private Root() {}

        @ExportMessage
        static boolean isExecutable(@SuppressWarnings("unused") Root receiver) {
            return true;
        }

        @ExportMessage
        static Object execute(@SuppressWarnings("unused") Root receiver, Object[] arguments) throws ArityException, UnsupportedTypeException {
            Args.checkArity(arguments, 1);
            ObjectInstance instance = Args.unwrapInstance(arguments, 0, ObjectInstance.class);
            Instance root = instance.getInstance().getNearestGCRootPointer();
            while (!root.isGCRoot()) {
                root = root.getNearestGCRootPointer();
            }
            // TODO: Return the actual root object, not the instance...
            return ObjectInstance.create(root);
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
