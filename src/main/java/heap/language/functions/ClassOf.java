package heap.language.functions;

import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import heap.language.heap.InstanceObject;
import heap.language.heap.JavaClassObject;
import heap.language.heap.ObjectArrayObject;
import heap.language.heap.PrimitiveArrayObject;
import heap.language.util.HeapLanguageUtils;

/**
 * Returns Class object of a given Java Object.
 */
@ExportLibrary(InteropLibrary.class)
public class ClassOf implements TruffleObject {

    public static final ClassOf INSTANCE = new ClassOf();

    private ClassOf() {}

    @ExportMessage
    static boolean isExecutable(@SuppressWarnings("unused") ClassOf receiver) {
        return true;
    }

    @ExportMessage
    static Object execute(@SuppressWarnings("unused") ClassOf receiver, Object[] arguments) throws ArityException, UnsupportedTypeException {
        HeapLanguageUtils.arityCheck(1, arguments);
        Object instance = arguments[0];
        if (instance instanceof InstanceObject) {
            return new JavaClassObject(((InstanceObject) instance).getJavaClass());
        } else if (instance instanceof PrimitiveArrayObject) {
            return new JavaClassObject(((PrimitiveArrayObject) instance).getJavaClass());
        } else if (instance instanceof ObjectArrayObject) {
            return new JavaClassObject(((ObjectArrayObject) instance).getJavaClass());
        } else {
            throw UnsupportedTypeException.create(arguments, "Expected object instance as argument.");
        }
    }

}
