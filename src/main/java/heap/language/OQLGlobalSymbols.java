package heap.language;

import com.oracle.truffle.api.interop.*;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import heap.language.heap.*;
import heap.language.util.HeapLanguageUtils;
import heap.language.util.InstanceWrapper;

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
    public class Length implements TruffleObject {

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
        ) throws ArityException, UnsupportedTypeException, UnsupportedMessageException {
            HeapLanguageUtils.arityCheck(1, arguments);
            Object instance = arguments[0];
            // TODO: Implement this for normal objects, iterators and enumerations
            if (instance instanceof TruffleObject && call.hasArrayElements(instance)) {
                // instance is a truffle array
                return call.getArraySize(instance);
            } else {
                throw UnsupportedTypeException.create(arguments, "Expected array/enumeration as argument.");
            }
        }

    }

    /*
    @ExportLibrary(InteropLibrary.class)
    class Map implements TruffleObject {

        public static final Map INSTANCE = new Map();

        private Map() {}

        @ExportMessage
        static boolean isExecutable(@SuppressWarnings("unused") Map receiver) {
            return true;
        }

        @ExportMessage
        static Object execute(@SuppressWarnings("unused") Map receiver, Object[] arguments) throws ArityException, UnsupportedTypeException {
            HeapLanguageUtils.arityCheck(1, arguments);
            InstanceWrapper<?> instance = HeapLanguageUtils.unwrapArgument(arguments, 0, InstanceWrapper.class);
            return new JavaClassObject(instance.getInstance().getJavaClass());
        }

    }
    */

}
