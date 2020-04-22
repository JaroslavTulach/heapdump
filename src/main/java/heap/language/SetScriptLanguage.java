package heap.language;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import heap.language.util.NullValue;

@ExportLibrary(InteropLibrary.class)
final class SetScriptLanguage implements TruffleObject {

    public static final SetScriptLanguage INSTANCE = new SetScriptLanguage();

    public SetScriptLanguage() {}

    @ExportMessage
    static boolean isExecutable(@SuppressWarnings("unused") SetScriptLanguage receiver) {
        return true;
    }

    @ExportMessage
    static Object execute(@SuppressWarnings("unused") SetScriptLanguage receiver, Object[] arguments) {
        // TODO: Check arity and type
        HeapLanguage.setScriptLanguage((String) arguments[0]);
        return NullValue.INSTANCE;
    }

}
