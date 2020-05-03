package heap.language.heap;

import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import heap.language.HeapLanguage;
import heap.language.util.Descriptors;
import heap.language.util.HeapLanguageUtils;
import heap.language.util.InstanceWrapper;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Instance;

import java.util.List;

/**
 * Truffle object holding reference to a particular heap object {@link Instance}. It allows to inspect the fields
 * of the instance object as if it were its own fields.
 */
@ExportLibrary(InteropLibrary.class)
public class InstanceObject extends InstanceWrapper<Instance> implements TruffleObject {

    static final String CLAZZ = "clazz";
    static final String STATICS = "statics";
    static final String ID = "id";
    static final String WRAPPED_OBJECT = "wrapped-object";
    static final String TO_STRING = "toString";

    @NonNull
    private final Descriptors members;

    public InstanceObject(@NonNull Instance instance) {
        super(instance);
        //noinspection unchecked
        List<FieldValue> fieldValues = (List<FieldValue>) instance.getFieldValues();
        String[] members = new String[fieldValues.size() + 4];
        members[0] = "clazz";
        members[1] = "statics";
        members[2] = "id";
        members[3] = "wrapped-object";
        // TODO: Add this to array wrappers as well.
        for (int i = 0; i < fieldValues.size(); i++) {
            FieldValue value = fieldValues.get(i);
            members[i + 4] = value.getField().getName();
        }
        this.members = Descriptors.build(members, new String[]{ TO_STRING });
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
        return receiver.members.hasProperty(member);
    }

    @ExportMessage
    static boolean isMemberInvocable(InstanceObject receiver, String member) {
        return receiver.members.hasFunction(member);
    }

    @ExportMessage
    static Object invokeMember(InstanceObject receiver, String member, Object[] arguments) throws ArityException, UnknownIdentifierException {
        if (TO_STRING.equals(member)) {
            HeapLanguageUtils.arityCheck(0, arguments);
            return InstanceWrapper.instanceString(receiver);
        } else {
            throw UnknownIdentifierException.create(member);
        }
    }

    @ExportMessage
    static Object readMember(InstanceObject receiver, String member) {
        switch (member) {
            case CLAZZ:
                return new JavaClassObject(receiver.instance.getJavaClass());
            case STATICS:
                return new StaticsObject(receiver.instance.getJavaClass());
            case ID:
                return receiver.instance.getInstanceId();
            case WRAPPED_OBJECT:
                return HeapLanguage.asGuestValue(receiver.instance);
            default:
                Object value = receiver.instance.getValueOfField(member);
                return HeapLanguageUtils.heapToTruffle(value);
        }
    }

}
