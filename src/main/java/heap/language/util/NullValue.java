package heap.language.util;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

/** A simple object representing null (since we can't safely return actual null from interop methods). */
@ExportLibrary(InteropLibrary.class)
public class NullValue implements TruffleObject {

    public static final NullValue INSTANCE = new NullValue();

    private NullValue() { }

    @ExportMessage
    public static boolean isNull(@SuppressWarnings("unused") NullValue receiver) {
        return true;
    }

}
