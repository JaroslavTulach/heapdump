package heap.language.util;

import com.oracle.truffle.api.interop.*;
import heap.language.HeapLanguage;
import heap.language.heap.*;
import org.graalvm.collections.Pair;
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
import java.util.Enumeration;
import java.util.Iterator;

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

    /**
     * Read an argument of the expected type from the given arguments array. Throws when argument does not have
     * the expected type.
     */
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
     * Construct a Java Enumeration from an arbitrary truffle object. The iterator yields a pair: index and the actual value.
     * Both values must be valid interop values.
     */
    public static Enumeration<Pair<Object, Object>> makeIterator(Object object, InteropLibrary call) {
        if (object instanceof TruffleObject) {
            if (call.hasArrayElements(object)) {    // object is array-like - we can access it using indices
                return new Enumeration<Pair<Object, Object>>() {

                    private int index = 0;

                    @Override
                    public boolean hasMoreElements() {
                        return call.isArrayElementReadable(object, index);
                    }

                    @Override
                    public Pair<Object, Object> nextElement() {
                        try {
                            int elementIndex = index;
                            Object element = call.readArrayElement(object, index);
                            this.index += 1;
                            return Pair.create(elementIndex, element);
                        } catch (UnsupportedMessageException | InvalidArrayIndexException e) {
                            throw new RuntimeException("Illegal array access", e);  // should be unreachable
                        }
                    }

                };
            } else if (call.hasMembers(object)) {   // object is not an array, but it has iterable members
                try {
                    Object members = call.getMembers(object);
                    return new Enumeration<Pair<Object, Object>>() {

                        private int index = 0;

                        @Override
                        public boolean hasMoreElements() {
                            return call.isArrayElementReadable(members, index);
                        }

                        @Override
                        public Pair<Object, Object> nextElement() {
                            try {
                                String elementKey = (String) call.readArrayElement(members, index);
                                Object element = call.readMember(object, elementKey);
                                index += 1;
                                return Pair.create(elementKey, element);
                            } catch (UnsupportedMessageException | InvalidArrayIndexException | UnknownIdentifierException e) {
                                throw new RuntimeException("Cannot access object member", e);
                            }
                        }

                    };
                } catch (UnsupportedMessageException e) {
                    throw new RuntimeException("Object does not have members", e);  // should be unreachable
                }
            }
        }
        // either not a truffle object or it is not an array or object...
        throw new IllegalArgumentException("Expected array/enumeration, but found "+object);
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

    public static boolean isPrimitiveValue(Object o) {
        return o instanceof Boolean || o instanceof Byte || o instanceof Short ||
                o instanceof Integer || o instanceof Long || o instanceof Float ||
                o instanceof Double || o instanceof Character || o instanceof String;
    }

}
