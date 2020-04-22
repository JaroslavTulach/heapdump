package heap.language;

import com.oracle.truffle.api.interop.*;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import heap.language.util.HeapLanguageUtils;
import heap.language.util.NullValue;

import java.util.Map;

/**
 * Global symbols of heap language which are provided by us
 */
interface CustomGlobalSymbols {

    /** Bind all global symbols into the context of a specific language. */
    @ExportLibrary(InteropLibrary.class)
    final class BindGlobalSymbols implements TruffleObject {

        public static final BindGlobalSymbols INSTANCE = new BindGlobalSymbols();

        public BindGlobalSymbols() {}

        @ExportMessage
        static boolean isExecutable(@SuppressWarnings("unused") BindGlobalSymbols receiver) {
            return true;
        }

        @ExportMessage
        static Object execute(
                @SuppressWarnings("unused") BindGlobalSymbols receiver,
                Object[] arguments,
                @CachedLibrary(limit = "3") InteropLibrary callFn
        ) throws ArityException {
            HeapLanguageUtils.arityCheck(1, arguments);
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

    /** Set a script language that should be used for expression arguments. */
    @ExportLibrary(InteropLibrary.class)
    final class SetScriptLanguage implements TruffleObject {

        public static final SetScriptLanguage INSTANCE = new SetScriptLanguage();

        public SetScriptLanguage() {}

        @ExportMessage
        static boolean isExecutable(@SuppressWarnings("unused") SetScriptLanguage receiver) {
            return true;
        }

        @ExportMessage
        static Object execute(
                @SuppressWarnings("unused") SetScriptLanguage receiver,
                Object[] arguments,
                @CachedLibrary(limit = "3") InteropLibrary interop
        ) throws ArityException, UnsupportedTypeException {
            HeapLanguageUtils.arityCheck(1, arguments);
            Object arg = arguments[0];
            if (arg instanceof String) {
                HeapLanguage.setScriptLanguage((String) arg);
            } else if (arg instanceof TruffleObject && interop.isNull(arg)) {
                HeapLanguage.setScriptLanguage(null);
            } else {
                throw UnsupportedTypeException.create(arguments, "Expected String or null as argument.");
            }
            return NullValue.INSTANCE;
        }

    }




}
