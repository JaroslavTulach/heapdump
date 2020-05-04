package heap.language;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.interop.*;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import heap.language.heap.IteratorWrapperObject;
import heap.language.heap.JavaClassObject;
import heap.language.util.Descriptors;
import heap.language.util.FilterIterator;
import heap.language.util.HeapLanguageUtils;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.modules.profiler.oql.engine.api.impl.Snapshot;

import java.util.Iterator;

/**
 * A native object provided by {@link heap.language.HeapLanguage} to communicate with a memory-mapped {@link Heap}.
 * The API of the object is given by the OQL language specification.
 */
@ExportLibrary(InteropLibrary.class)
public class HeapObject implements TruffleObject {

    // Constants for member names
    private static final String FOR_EACH_CLASS = "forEachClass";
    private static final String FOR_EACH_OBJECT = "forEachObject";
    private static final String FIND_CLASS = "findClass";
    private static final String FIND_OBJECT = "findObject";
    private static final String CLASSES = "classes";
    private static final String OBJECTS = "objects";
    private static final String FINALIZABLES = "finalizables";
    private static final String LIVEPATHS = "livepaths";
    private static final String ROOTS = "roots";

    private static final Descriptors MEMBERS = Descriptors.functions(
        FOR_EACH_CLASS, FOR_EACH_OBJECT, FIND_CLASS, FIND_OBJECT, CLASSES, OBJECTS, FINALIZABLES, LIVEPATHS, ROOTS
    );

    @NonNull
    final Snapshot heap;

    public HeapObject(@NonNull Heap heap) {
        this.heap = new Snapshot(heap, null);
    }

    /* calls a callback function for each Java Class */
    private Object invoke_forEachClass(Object[] arguments, InteropLibrary callFn)
            throws  ArityException,
            UnsupportedTypeException,
            UnsupportedMessageException
    {
        HeapLanguageUtils.arityCheck(1, arguments);
        TruffleObject callback = (TruffleObject) arguments[0];

        //noinspection unchecked
        Iterator<JavaClass> it = this.heap.getClasses();
        while (it.hasNext()) {
            JavaClass clazz = it.next();
            callFn.execute(callback, new JavaClassObject(clazz));
        }
        return this;
    }

    private Object invoke_forEachObject(Object[] arguments, InteropLibrary callFn)
            throws ArityException,
            UnsupportedMessageException,
            UnsupportedTypeException
    {
        if (arguments.length == 0) {
            throw UnsupportedTypeException.create(arguments, "Expected callback as first argument.");
        }
        TruffleObject callback = (TruffleObject) arguments[0];

        JavaClass javaClass = null;
        if (arguments.length >= 2) {
            Object arg = arguments[1];
            if (arg instanceof JavaClassObject) {
                javaClass = ((JavaClassObject) arg).getJavaClass();
            } else if (arg instanceof String) {
                javaClass = heap.findClass((String) arg);
            } else {
                throw UnsupportedTypeException.create(arguments, "Expected Java Class as second argument.");
            }
        }
        if (javaClass == null) {
            javaClass = this.heap.findClass("java.lang.Object");
        }

        boolean includeSubtypes = true;
        if (arguments.length >= 3) {
            Object arg = arguments[2];
            if (!(arg instanceof Boolean)) {
                throw UnsupportedTypeException.create(arguments, "Expected boolean as third argument.");
            }
            includeSubtypes = (Boolean) arg;
        }

        if (arguments.length > 3) {
            throw ArityException.create(3, arguments.length);
        }

        //noinspection unchecked
        Iterator<Instance> items = (Iterator<Instance>) this.heap.getInstances(javaClass, includeSubtypes);
        while (items.hasNext()) {
            Instance instance = items.next();
            callFn.execute(callback, HeapLanguageUtils.heapToTruffle(instance));
        }
        return this;
    }

    /* finds Java Class of given name */
    private JavaClassObject invoke_findClass(Object[] arguments) throws ArityException {
        HeapLanguageUtils.arityCheck(1, arguments);
        return new JavaClassObject(heap.findClass((String) arguments[0]));
    }

    private Object invoke_findObject(Object[] arguments) throws ArityException, UnsupportedTypeException {
        HeapLanguageUtils.arityCheck(1, arguments);
        Object arg = arguments[0];
        long id;
        if (arg instanceof Long) {
            id = (Long) arg;
        } else if (arg instanceof Integer) {
            id = (Integer) arg;
        } else if (arg instanceof String) {
            try {
                id = Long.parseLong((String) arg);
            } catch (NumberFormatException e) {
                throw UnsupportedTypeException.create(arguments, "Expected object id, got "+arg);
            }
        } else {
            throw UnsupportedTypeException.create(arguments, "Expected object id, got "+arg);
        }

        return HeapLanguageUtils.heapToTruffle(this.heap.findThing(id));
    }

    /* returns an enumeration of all Java classes */
    private Object invoke_classes(Object[] arguments) throws ArityException {
        HeapLanguageUtils.arityCheck(0, arguments);
        //noinspection unchecked
        return new IteratorWrapperObject(this.heap.getClasses());
    }

    /* returns an enumeration of Java heap */
    private Object invoke_objects(Object[] arguments, InteropLibrary callFn)
            throws ArityException,
            UnsupportedTypeException {
        JavaClass javaClass = null;
        if (arguments.length >= 1) {
            Object arg = arguments[0];
            if (arg instanceof JavaClassObject) {
                javaClass = ((JavaClassObject) arg).getJavaClass();
            } else if (arg instanceof String) {
                javaClass = heap.findClass((String) arg);
                if (javaClass == null) {
                    throw UnsupportedTypeException.create(arguments, "Java Class "+arg+" not valid in this heap.");
                }
            } else {
                throw UnsupportedTypeException.create(arguments, "Expected Java Class as first argument.");
            }
        }
        if (javaClass == null) {
            javaClass = heap.findClass("java.lang.Object");
        }

        boolean includeSubtypes = true;
        if (arguments.length >= 2) {
            Object arg = arguments[1];
            if (!(arg instanceof Boolean)) {
                throw UnsupportedTypeException.create(arguments, "Expected boolean as second argument.");
            }
            includeSubtypes = (Boolean) arg;
        }

        TruffleObject filter = null;
        if (arguments.length >= 3) {
            Object arg = arguments[2];
            if (arg instanceof TruffleObject) { // Filter is a callback function
                if (!callFn.isExecutable(arg)) {
                    throw UnsupportedTypeException.create(arguments, "Expected executable filter function as third argument.");
                }
                filter = (TruffleObject) arg;
            } else if (arg instanceof String) { // Filter is a string expression
                TruffleObject filterCall = HeapLanguage.parseArgumentExpression((String) arg, "it");

                // TODO: Unite with callback method once we have a better option to call JS
                //noinspection unchecked
                final Iterator<Instance> instances = (Iterator<Instance>) this.heap.getInstances(javaClass, includeSubtypes);
                return new IteratorWrapperObject(new FilterIterator<Instance>(instances) {
                    @Override
                    public Object check(Instance item) {
                        Object value = HeapLanguageUtils.heapToTruffle(item);
                        boolean isValid;
                        try {
                            // TODO: This can return a bool-like object!
                            isValid = (Boolean) callFn.execute(filterCall, value);
                        } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException e) {
                            throw new RuntimeException(e);
                        }
                        return isValid ? value : null;
                    }
                });
            } else {
                throw UnsupportedTypeException.create(arguments, "Expected filter as third argument.");
            }
        }

        if (arguments.length > 3) {
            throw ArityException.create(3, arguments.length);
        }

        //noinspection unchecked
        final Iterator<Instance> instances = (Iterator<Instance>) this.heap.getInstances(javaClass, includeSubtypes);
        if (filter == null) {
            return new IteratorWrapperObject(instances);
        } else {
            TruffleObject finalFilter = filter;
            return new IteratorWrapperObject(new FilterIterator<Instance>(instances) {
                @Override
                public Object check(Instance item) {
                    try {
                        Object value = HeapLanguageUtils.heapToTruffle(item);
                        boolean isValid = (Boolean) callFn.execute(finalFilter, value);
                        return isValid ? value : null;
                    } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
    }

    private Object invoke_finalizables(Object[] arguments) throws ArityException {
        HeapLanguageUtils.arityCheck(0, arguments);
        //noinspection unchecked
        Iterator<Instance> instances = (Iterator<Instance>) heap.getFinalizerObjects();
        return new IteratorWrapperObject(instances);
    }

    private static Object invoke_livepaths(HeapObject receiver, Object[] arguments) {
        throw new IllegalStateException("Unimplemented: "+receiver+" "+arguments.length);   // TODO

    }

    private static Object invoke_roots(HeapObject receiver, Object[] arguments) {
        throw new IllegalStateException("Unimplemented: "+receiver+" "+arguments.length);   // TODO
    }

    @ExportMessage
    static boolean hasMembers(@SuppressWarnings("unused") HeapObject receiver) {
        return true;    // Every valid heap object has member functions.
    }

    @ExportMessage
    static boolean isMemberInvocable(@SuppressWarnings("unused") HeapObject receiver, String member) {
        return MEMBERS.hasFunction(member);
    }

    @ExportMessage
    static Object getMembers(
            @SuppressWarnings("unused") HeapObject receiver,
            @SuppressWarnings("unused") boolean includeInternal
    ) {
        return MEMBERS; // there are no internal members
    }

    @ExportMessage
    static boolean isMemberReadable(@SuppressWarnings("unused") HeapObject receiver, String member) {
        // Invokable members with no arguments can be also seen as properties...
        switch (member) {
            case FINALIZABLES:
            case CLASSES:
                return true;
            default:
                return false;
        }
    }

    @ExportMessage
    static Object readMember(HeapObject receiver, String member) throws UnknownIdentifierException {
        try {
            switch (member) {
                case FINALIZABLES:
                        return receiver.invoke_finalizables(new Object[0]);
                case CLASSES:
                    return receiver.invoke_classes(new Object[0]);
                default:
                    throw UnknownIdentifierException.create(member);
            }
        } catch (ArityException e) {
            throw new RuntimeException(e);  // unreachable
        }
    }

    @ExportMessage
    static Object invokeMember(
            HeapObject receiver, String member, Object[] arguments,
            @CachedLibrary(limit = "3") InteropLibrary callFn
    ) throws ArityException,
            UnsupportedTypeException,
            UnknownIdentifierException,
            UnsupportedMessageException
    {
        switch (member) {
            case FOR_EACH_CLASS:
                return receiver.invoke_forEachClass(arguments, callFn);
            case FOR_EACH_OBJECT:
                return receiver.invoke_forEachObject(arguments, callFn);
            case FIND_CLASS:
                return receiver.invoke_findClass(arguments);
            case FIND_OBJECT:
                return receiver.invoke_findObject(arguments);
            case CLASSES:
                return receiver.invoke_classes(arguments);
            case OBJECTS:
                return receiver.invoke_objects(arguments, callFn);
            case FINALIZABLES:
                return receiver.invoke_finalizables(arguments);
            case LIVEPATHS:
                return invoke_livepaths(receiver, arguments);
            case ROOTS:
                return invoke_roots(receiver, arguments);
            default:
                throw UnknownIdentifierException.create(member);
        }
    }

}

