package heap.language;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.nodes.RootNode;
import heap.language.heap.HeapObject;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.HeapFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * A Truffle Language object for natively dealing with heap dumps.
 *
 * Heap Language treats data from heap dumps as "code" of a new language called
 * "heap". As such, a heap dump can be "executed", result of which is a native
 * heap object that can be (via truffle) shared with other languages.
 *
 * The supported primitives are described in
 * <a href="http://cr.openjdk.java.net/~sundar/8022483/webrev.01/raw_files/new/src/share/classes/com/sun/tools/hat/resources/oqlhelp.html">OQL specification</a>.
 */
@TruffleLanguage.Registration(
        byteMimeTypes = "application/x-netbeans-profiler-hprof",
        name = "heap",
        id = "heap"
)
public class HeapLanguage extends TruffleLanguage<Void> {

    // Used to perform utility operations, like converting Java heap to polyglot values.
    private Env env;

    @Override
    protected Void createContext(Env env) {
        this.env = env;
        return null;
    }

    @Override
    protected boolean isObjectOfLanguage(Object object) {
        // TODO
        return false;
    }

    @Override
    protected CallTarget parse(ParsingRequest request) {
        final URI location = request.getSource().getURI();
        final File file = new File(location);
        return Truffle.getRuntime().createCallTarget(new HeapLanguage.HeapNode(this, file));
    }

    /**
     * Utility method for checking arity of function arguments
     */
    public static void arityCheck(int expected, Object[] arguments) throws ArityException {
        if (arguments.length != expected) {
            throw ArityException.create(expected, arguments.length);
        }
    }

    /**
     * Convert given Java object into a HeapLanguage guest value.
     */
    public static Object asGuestValue(Object value) {
        return HeapLanguage.getInstance().env.asGuestValue(value);
    }

    /**
     * Obtain instance of HeapLanguage used by current Thread. If HeapLanguage is not available,
     * throws IllegalStateException.
     *
     * @return current instance of HeapLanguage
     */
    public static HeapLanguage getInstance() {
        return HeapLanguage.getCurrentLanguage(HeapLanguage.class);
    }

    /**
     * An executable root node of the "AST" of our Heap Language.
     *
     * Technically, there is no AST to execute, just a reference to the heap dump that needs to be
     * loaded and processed.
     */
    private static final class HeapNode extends RootNode {

        @NonNull
        private final File file;

        HeapNode(HeapLanguage language, @NonNull File file) {
            super(language);
            this.file = file;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            try {
                Heap heap = HeapFactory.createHeap(file);
                return new HeapObject(heap);
            } catch (IOException e) {
                throw new RuntimeException("Error while reading heap dump.", e);
            }
        }
    }
}

