package heap.language.functions;

import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import heap.language.heap.IteratorWrapperObject;
import heap.language.heap.ObjectArrayObject;
import heap.language.heap.PrimitiveArrayObject;
import heap.language.util.HeapLanguageUtils;

@ExportLibrary(InteropLibrary.class)
public class Length implements TruffleObject {

    public static final Length INSTANCE = new Length();

    private Length() {}

    @ExportMessage
    static boolean isExecutable(@SuppressWarnings("unused") Length receiver) {
        return true;
    }

    @ExportMessage
    static Object execute(@SuppressWarnings("unused") Length receiver, Object[] arguments) throws ArityException, UnsupportedTypeException {
        HeapLanguageUtils.arityCheck(1, arguments);
        Object instance = arguments[0];
        if (instance instanceof PrimitiveArrayObject) {
            return ((PrimitiveArrayObject) instance).getLength();
        } else if (instance instanceof ObjectArrayObject) {
            return ((ObjectArrayObject) instance).getLength();
        } else if (instance instanceof IteratorWrapperObject) {
            // TODO: This might not be what we really want since this consumes the iterator, but there is no other way :/
            IteratorWrapperObject iterator = (IteratorWrapperObject) instance;
            int count = 0;
            while (iterator.hasNext()) {
                count += 1; iterator.next();
            }
            return count;
        } else {
            // TODO: Also accept JS arrays/enumerations?
            throw UnsupportedTypeException.create(arguments, "Expected array/enumeration as argument.");
        }
    }

}
