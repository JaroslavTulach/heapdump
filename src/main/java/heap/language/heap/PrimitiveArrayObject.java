package heap.language.heap;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import heap.language.util.InstanceWrapper;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.heap.PrimitiveArrayInstance;

import java.util.List;

/**
 * Primitive array instances are effectively string arrays from interop perspective.
 */
@ExportLibrary(InteropLibrary.class)
public class PrimitiveArrayObject extends InstanceWrapper<PrimitiveArrayInstance> implements TruffleObject {

    @NonNull
    private final List<String> primitiveArray;  // underlying implementation is (supposedly) lazy

    public PrimitiveArrayObject(@NonNull PrimitiveArrayInstance instance) {
        super(instance);
        //noinspection unchecked
        this.primitiveArray = (List<String>) instance.getValues();
    }

    public int getLength() {
        return this.instance.getLength();
    }

    public PrimitiveArrayInstance getInstance() {
        return this.instance;
    }

    @ExportMessage
    static boolean hasArrayElements(@SuppressWarnings("unused") PrimitiveArrayObject receiver) {
        return true;
    }

    @ExportMessage
    static boolean isArrayElementReadable(PrimitiveArrayObject receiver, long index) {
        return index >= 0 && index < receiver.instance.getLength();
    }

    @ExportMessage
    static int getArraySize(PrimitiveArrayObject receiver) {
        return receiver.instance.getLength();
    }

    @ExportMessage
    static String readArrayElement(PrimitiveArrayObject receiver, long at) throws InvalidArrayIndexException {
        if (!isArrayElementReadable(receiver, at)) {
            throw InvalidArrayIndexException.create(at);
        }
        return receiver.primitiveArray.get((int) at);
    }

}
