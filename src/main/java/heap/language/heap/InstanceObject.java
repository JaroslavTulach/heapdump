package heap.language.heap;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import heap.language.util.Descriptors;
import heap.language.util.HeapLanguageUtils;
import heap.language.util.InstanceWrapper;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;

import java.util.List;

/**
 * Truffle object holding reference to a particular heap object {@link Instance}. It allows to inspect the fields
 * of the instance object as if it were its own fields.
 */
@ExportLibrary(InteropLibrary.class)
public class InstanceObject extends InstanceWrapper<Instance> implements TruffleObject {

    @NonNull
    private final Descriptors members;

    public InstanceObject(@NonNull Instance instance) {
        super(instance);
        //noinspection unchecked
        List<FieldValue> fieldValues = (List<FieldValue>) instance.getFieldValues();
        String[] members = new String[fieldValues.size()];
        for (int i = 0; i < fieldValues.size(); i++) {
            FieldValue value = fieldValues.get(i);
            members[i] = value.getField().getName();
        }
        this.members = Descriptors.build(members, null);
    }

    @NonNull
    public Instance getInstance() {
        return instance;
    }

    @ExportMessage
    static boolean hasMembers(@SuppressWarnings("unused") InstanceObject receiver) {
        return true;
    }

    @ExportMessage
    static Object getMembers(InstanceObject receiver, @SuppressWarnings("unused") boolean includeInternal) {
        return receiver.members;
    }

    @ExportMessage
    static boolean isMemberReadable(InstanceObject receiver, String member) {
        return receiver.members.contains(member);
    }

    @ExportMessage
    static Object readMember(InstanceObject receiver, String member) {
        Object value = receiver.instance.getValueOfField(member);
        return HeapLanguageUtils.heapToTruffle(value);
    }

}
