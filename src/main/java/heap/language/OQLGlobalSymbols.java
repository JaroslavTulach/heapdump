package heap.language;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.interop.*;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import heap.language.heap.JavaClassObject;
import heap.language.util.HeapLanguageUtils;
import heap.language.util.InstanceWrapper;
import heap.language.util.ReadOnlyArray;
import org.graalvm.collections.Pair;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;

import java.util.ArrayList;
import java.util.Enumeration;

/**
 * Implementations of built in global functions from OQL.
 */
interface OQLGlobalSymbols {

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
            HeapLanguageUtils.arityCheck(1, arguments);
            InstanceWrapper<?> instance = HeapLanguageUtils.unwrapArgument(arguments, 0, InstanceWrapper.class);
            return new JavaClassObject(instance.getInstance().getJavaClass());
        }

    }

    /**
     * Length function returns number of elements of an array/enumeration.
     */
    @ExportLibrary(InteropLibrary.class)
    class Length implements TruffleObject {

        public static final Length INSTANCE = new Length();

        private Length() {}

        @ExportMessage
        static boolean isExecutable(@SuppressWarnings("unused") Length receiver) {
            return true;
        }

        @ExportMessage
        static Object execute(
                @SuppressWarnings("unused") Length receiver,
                Object[] arguments,
                @CachedLibrary(limit = "3") InteropLibrary call
        ) throws ArityException {
            HeapLanguageUtils.arityCheck(1, arguments);
            // TODO: Fast lane for array-like objects...
            Enumeration<Pair<Object, Object>> iterator = HeapLanguageUtils.makeIterator(arguments[0], call);
            int count = 0;
            while (iterator.hasMoreElements()) {
                count += 1;
                iterator.nextElement();
            }
            return count;
        }

    }


    @ExportLibrary(InteropLibrary.class)
    class Map implements TruffleObject {

        public static final Map INSTANCE = new Map();

        private Map() {}

        @ExportMessage
        static boolean isExecutable(@SuppressWarnings("unused") Map receiver) {
            return true;
        }

        @ExportMessage
        static Object execute(
                @SuppressWarnings("unused") Map receiver,
                Object[] arguments,
                @CachedLibrary(limit = "3") InteropLibrary call
        ) throws ArityException, UnsupportedTypeException, UnsupportedMessageException {
            HeapLanguageUtils.arityCheck(2, arguments);
            Enumeration<Pair<Object, Object>> it = HeapLanguageUtils.makeIterator(arguments[0], call);
            Object action = arguments[1];
            ArrayList<Object> result = new ArrayList<>();
            if (action instanceof String) {
                CallTarget target = HeapLanguage.parseArgumentExpression((String) action, "it", "index", "array", "result");
                while (it.hasMoreElements()) {
                    Pair<Object, Object> el = it.nextElement();
                    Object element = el.getRight();
                    if (element instanceof Character) { // WHAT THE ACTUAL FUCK?!
                        element = element.toString();
                    }
                    result.add(target.call(element, el.getLeft(), arguments[0], HeapLanguage.asGuestValue(result)));
                }
            } else if (call.isExecutable(action)) {
                while (it.hasMoreElements()) {
                    Pair<Object, Object> el = it.nextElement();
                    Object element = el.getRight();
                    if (element instanceof Character) {
                        element = element.toString();
                    }
                    result.add(call.execute(action, element, el.getLeft(), arguments[0], HeapLanguage.asGuestValue(result)));
                }
            } else {
                throw UnsupportedTypeException.create(arguments, "Expected callback or string expression as second argument.");
            }

            return new ReadOnlyArray(result.toArray(new Object[0]));
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
            HeapLanguageUtils.arityCheck(1, arguments);
            Object arg = arguments[0];
            // TODO: Is there an implicit "toString" conversion in truffle?
            return toHtml(arg, call);
        }

        private static String toHtml(Object arg, InteropLibrary call) throws UnsupportedMessageException {
            if (arg instanceof JavaClassObject) {
                JavaClass clazz = ((JavaClassObject) arg).getJavaClass();
                long id = clazz.getJavaClassId();
                return "<a href='file://class/" + id + "' name='" + id + "'>class " + clazz.getName() + "</a>";
            } else if (arg instanceof InstanceWrapper<?>) {
                Instance instance = ((InstanceWrapper<?>) arg).getInstance();
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

    @ExportLibrary(InteropLibrary.class)
    class SizeOf implements TruffleObject {

        public static final SizeOf INSTANCE = new SizeOf();

        private SizeOf() {}

        @ExportMessage
        static boolean isExecutable(SizeOf receiver) {
            return true;
        }

        @ExportMessage
        static Object execute(@SuppressWarnings("unused") SizeOf receiver, Object[] arguments) throws ArityException, UnsupportedTypeException {
            HeapLanguageUtils.arityCheck(1, arguments);
            InstanceWrapper<?> instance = HeapLanguageUtils.unwrapArgument(arguments, 0, InstanceWrapper.class);
            return instance.getInstance().getSize();
        }

    }

}
