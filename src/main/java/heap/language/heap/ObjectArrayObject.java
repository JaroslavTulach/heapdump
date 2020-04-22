package heap.language.heap;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import heap.language.util.HeapLanguageUtils;
import heap.language.util.InstanceWrapper;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.heap.ObjectArrayInstance;

import java.util.List;

@ExportLibrary(InteropLibrary.class)
public class ObjectArrayObject extends InstanceWrapper<ObjectArrayInstance> implements TruffleObject {

    @NonNull
    private final List<Instance> instanceArray;  // underlying implementation is (supposedly) lazy

    public ObjectArrayObject(@NonNull ObjectArrayInstance instance) {
        super(instance);
        //noinspection unchecked
        this.instanceArray = (List<Instance>) instance.getValues();
    }

    public int getLength() {
        return this.instance.getLength();
    }

    public ObjectArrayInstance getInstance() {
        return instance;
    }

    @ExportMessage
    static boolean hasArrayElements(@SuppressWarnings("unused") ObjectArrayObject receiver) {
        return true;
    }

    @ExportMessage
    static boolean isArrayElementReadable(ObjectArrayObject receiver, long index) {
        return index >= 0 && index < receiver.instance.getLength();
    }

    @ExportMessage
    static int getArraySize(ObjectArrayObject receiver) {
        return receiver.instance.getLength();
    }

    @ExportMessage
    static Object readArrayElement(ObjectArrayObject receiver, long at) throws InvalidArrayIndexException {
        if (!isArrayElementReadable(receiver, at)) {
            throw InvalidArrayIndexException.create(at);
        }
        return HeapLanguageUtils.heapToTruffle(receiver.instanceArray.get((int) at));
    }


}
