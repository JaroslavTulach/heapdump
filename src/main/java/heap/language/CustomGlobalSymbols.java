package heap.language;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.interop.*;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import heap.language.util.NullValue;

import java.util.Map;

/**
 * Global symbols of heap language which are provided by us.
 */
interface CustomGlobalSymbols {

    /**
     * Bind all global symbols into the context bindings of a specific language. Context can be either provided
     * by the language (e.g. {@code this} in JavaScript), or by the interop directly
     * (e.g. {@code context.getBindings("python")}).
     */
    @ExportLibrary(InteropLibrary.class)
    final class BindGlobalSymbols implements TruffleObject {

        static final BindGlobalSymbols INSTANCE = new BindGlobalSymbols();

        private BindGlobalSymbols() {}

        @ExportMessage
        static boolean isExecutable(@SuppressWarnings("unused") BindGlobalSymbols receiver) {
            return true;
        }

        @ExportMessage
        static Object execute(@SuppressWarnings("unused") BindGlobalSymbols receiver, Object[] arguments) throws ArityException {
            Interop.checkArity(arguments, 1);
            TruffleObject bindings = (TruffleObject) arguments[0];
            BindGlobalSymbols.registerSymbols(bindings, InteropLibrary.getFactory().getUncached());
            return bindings;
        }

        @CompilerDirectives.TruffleBoundary
        private static void registerSymbols(TruffleObject bindings, InteropLibrary interop) {
            for (Map.Entry<String, TruffleObject> symbol : HeapLanguage.Globals.INSTANCES.entrySet()) {
                try {
                    interop.writeMember(bindings, symbol.getKey(), symbol.getValue());
                } catch (UnsupportedTypeException | UnsupportedMessageException | UnknownIdentifierException e) {
                    throw new IllegalStateException("Cannot register global symbol `"+symbol.getKey()+"`.", e);
                }
            }
        }

    }

    /** Set a script language that should be used for expression arguments. */
    @ExportLibrary(InteropLibrary.class)
    final class SetScriptLanguage implements TruffleObject {

        static final SetScriptLanguage INSTANCE = new SetScriptLanguage();

        private SetScriptLanguage() {}

        @ExportMessage
        static boolean isExecutable(@SuppressWarnings("unused") SetScriptLanguage receiver) {
            return true;
        }

        @ExportMessage
        static Object execute(@SuppressWarnings("unused") SetScriptLanguage receiver, Object[] arguments) throws ArityException {
            Interop.checkArity(arguments, 1);
            String language = Interop.isNull(arguments[0]) ? null : Interop.asString(arguments[0]);
            HeapLanguage.setScriptLanguage(language);
            return NullValue.INSTANCE;
        }

    }

}
