package heap.language.util;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

/** A simple object representing null (since we can't safely return actual null from interop methods). */
@ExportLibrary(InteropLibrary.class)
public class NullObject implements TruffleObject {

    public static final NullObject INSTANCE = new NullObject();

    private NullObject() { }

    @ExportMessage
    public static boolean isNull(@SuppressWarnings("unused") NullObject receiver) {
        return true;
    }

}
