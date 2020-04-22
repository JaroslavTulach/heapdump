package heap.language.util;

import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import heap.language.HeapLanguage;
import heap.language.heap.*;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.ByteSequence;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.heap.ObjectArrayInstance;
import org.netbeans.lib.profiler.heap.PrimitiveArrayInstance;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class HeapLanguageUtils {

    public static ByteSequence bytesOf(File heapFile) throws IOException {
        long length = heapFile.length();
        try (RandomAccessFile file = new RandomAccessFile(heapFile, "r")) {
            MappedByteBuffer out = file.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, length);
            return new ByteSequence() {
                @Override
                public int length() {
                    return (int) heapFile.length();
                }

                @Override
                public byte byteAt(int index) {
                    return out.get(index);
                }
            };
        }
    }

    /**
     * Utility method for checking arity of function arguments.
     */
    public static void arityCheck(int expected, Object[] arguments) throws ArityException {
        if (arguments.length != expected) {
            throw ArityException.create(expected, arguments.length);
        }
    }

    public static <T> T unwrapArgument(Object[] arguments, int argIndex, Class<T> clazz) throws UnsupportedTypeException {
        if (argIndex < 0) {
            throw new IllegalArgumentException("Functions can't have negative arguments. Argument index: "+argIndex);
        }
        if (argIndex < arguments.length) {
            Object arg = arguments[argIndex];
            if (clazz.isInstance(arg)) {
                return clazz.cast(arg);
            }
        }
        throw UnsupportedTypeException.create(arguments, String.format("Expected %s as argument %d.", clazz.getSimpleName(), argIndex+1));
    }

    /**
     * Safely convert an object returned by the heap API so that it can be manipulated by truffle.
     */
    public static Object heapToTruffle(Object heapObject) {
        if (heapObject == null) {
            return NullValue.INSTANCE;
        } else if (isInteropValue(heapObject)) {
            return heapObject;
        } else if (heapObject instanceof PrimitiveArrayInstance) {
            return new PrimitiveArrayObject((PrimitiveArrayInstance) heapObject);
        } else if (heapObject instanceof ObjectArrayInstance) {
            return new ObjectArrayObject((ObjectArrayInstance) heapObject);
        } else if (heapObject instanceof Instance) {
            return new InstanceObject((Instance) heapObject);
        } else if (heapObject instanceof JavaClass) {
            return new JavaClassObject((JavaClass) heapObject);
        } else {
            // If everything else fails, create a guest value object which will use reflection to handle the calls.
            return HeapLanguage.asGuestValue(heapObject);
        }
    }

    public static Object truffleToHeap(Object truffleObject) {
        if (isPrimitiveValue(truffleObject)) {
            return truffleObject;   // no conversion
        } else if (truffleObject instanceof Value) {
            Value value = (Value) truffleObject;
            // TODO: Is there a better way to convert Value heap?
            if (value.canInvokeMember("asJavaClass")) {
                return value.invokeMember("asJavaClass").as(JavaClass.class);
            } else {
                // TODO: Other heap...
                return value;
            }
        } else if (truffleObject instanceof JavaClassObject) {
            return ((JavaClassObject) truffleObject).getJavaClass();
        } else if (truffleObject instanceof PrimitiveArrayObject) {
            return ((PrimitiveArrayObject) truffleObject).getInstance();
        } else if (truffleObject instanceof ObjectArrayObject) {
            return ((ObjectArrayObject) truffleObject).getInstance();
        } else if (truffleObject instanceof InstanceObject) {
            return ((InstanceObject) truffleObject).getInstance();
        } else if (truffleObject instanceof NullValue) {
            return null;
        } else {
            return truffleObject;
            // If everything else fails, try to convert automatically.
            //return HeapLanguage.asHostObject(truffleObject);
        }
    }

    private static boolean isInteropValue(Object o) {
        return o instanceof TruffleObject || isPrimitiveValue(o);
    }

    private static boolean isPrimitiveValue(Object o) {
        return o instanceof Boolean || o instanceof Byte || o instanceof Short ||
                o instanceof Integer || o instanceof Long || o instanceof Float ||
                o instanceof Double || o instanceof Character || o instanceof String;
    }

}
