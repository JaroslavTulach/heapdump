package heap.language.heap;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import heap.language.util.Descriptors;
import heap.language.util.NullObject;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;

import java.util.List;

/**
 * A truffle object that holds references to values of static fields of a particular {@link JavaClassObject}.
 */
@ExportLibrary(InteropLibrary.class)
public class StaticsObject implements TruffleObject {

    @NonNull
    private final JavaClass clazz;

    private final Descriptors members;

    public StaticsObject(@NonNull JavaClass clazz) {
        this.clazz = clazz;
        //noinspection unchecked
        List<FieldValue> staticFieldValues = (List<FieldValue>) clazz.getStaticFieldValues();
        String[] members = new String[staticFieldValues.size()];
        for (int i = 0; i < staticFieldValues.size(); i++) {
            FieldValue value = staticFieldValues.get(i);
            members[i] = value.getField().getName();
        }
        this.members = Descriptors.build(members, null);
    }

    @ExportMessage
    static boolean hasMembers(@SuppressWarnings("unused") StaticsObject receiver) {
        return true;
    }

    @ExportMessage
    static Object getMembers(StaticsObject receiver, @SuppressWarnings("unused") boolean includeInternal) {
        return receiver.members;
    }

    @ExportMessage
    static boolean isMemberReadable(StaticsObject receiver, String member) {
        return receiver.members.contains(member);
    }

    @ExportMessage
    static Object readMember(StaticsObject receiver, String member) {
        Object value = receiver.clazz.getValueOfStaticField(member);
        if (value == null) {
            return NullObject.INSTANCE;
        } else if (value instanceof Instance) {
            return new InstanceObject((Instance) value);
        } else {
            // Otherwise, value is a primitive type (boolean, integer, etc.)
            return value;
        }
    }

}
