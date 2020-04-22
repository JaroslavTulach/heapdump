package heap.language;

import com.oracle.truffle.api.interop.*;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import heap.language.util.NullValue;

import java.util.Map;

@ExportLibrary(InteropLibrary.class)
final class BindGlobalSymbols implements TruffleObject {

    public static final BindGlobalSymbols INSTANCE = new BindGlobalSymbols();

    public BindGlobalSymbols() {}

    @ExportMessage
    static boolean isExecutable(@SuppressWarnings("unused") BindGlobalSymbols receiver) {
        return true;
    }

    @ExportMessage
    static Object execute(@SuppressWarnings("unused") BindGlobalSymbols receiver, Object[] arguments,
                          @CachedLibrary(limit = "3") InteropLibrary callFn) {
        // TODO: Check arity and type
        Object bindings = arguments[0];
        for (Map.Entry<String, TruffleObject> symbol : HeapLanguage.Globals.INSTANCES.entrySet()) {
            try {
                callFn.writeMember(bindings, symbol.getKey(), symbol.getValue());
            } catch (UnsupportedMessageException | UnknownIdentifierException | UnsupportedTypeException e) {
                throw new RuntimeException("Error registering global symbol.", e);
            }
        }
        return NullValue.INSTANCE;
    }

}
