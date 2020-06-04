package com.oracle.truffle.heap;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleRuntime;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.*;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.heap.interop.*;
import com.oracle.truffle.heap.interop.nodes.*;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A native object provided by {@link HeapLanguage} to communicate with a memory-mapped {@link Heap}.
 * The API of the object is given by the OQL language specification.
 */
@ExportLibrary(InteropLibrary.class)
final class ObjectHeap implements TruffleObject {

    @NonNull
    private final Heap heap;

    public ObjectHeap(@NonNull Heap heap) {
        this.heap = heap;
    }

    @NonNull
    public Heap getHeap() {
        return heap;
    }

    /// The object is structured so that the actual implementation is first and all the interop code is at the end.

    /* Calls a callback function for each Java Class. The callback can return a boolean, and iteration is stopped
     * if this value is true. */
    private Object invoke_forEachClass(Object[] arguments)
            throws  ArityException,
            UnsupportedTypeException,
            UnsupportedMessageException
    {
        Args.checkArity(arguments, 1);
        TruffleObject callback = Args.unwrapExecutable(arguments, 0);
        InteropLibrary interop = InteropLibrary.getFactory().getUncached();
        //noinspection unchecked
        for (JavaClass javaClass : (Iterable<JavaClass>) this.heap.getAllClasses()) {
            Boolean stop = Types.tryAsBoolean(interop.execute(callback, ObjectJavaClass.create(javaClass)));
            if (stop != null && stop) break;
        }
        return this;
    }

    /* Finds Java Class of given name. */
    private Object invoke_findClass(Object[] arguments) throws ArityException, UnsupportedTypeException {
        Args.checkArity(arguments, 1);
        return ObjectJavaClass.create(HeapUtils.findClass(heap, Args.unwrapString(arguments, 0)));
    }

    /* Finds object from given object id. */
    private Object invoke_findObject(Object[] arguments) throws ArityException, UnsupportedTypeException {
        Args.checkArity(arguments, 1);
        long id = HeapLanguage.unwrapObjectIdArgument(arguments, 0);
        return ObjectInstance.create(heap.getInstanceByID(id));
    }

    /* Returns an enumeration of all Java classes. */
    private Object invoke_classes(Object[] arguments) throws ArityException {
        Args.checkArity(arguments, 0);
        Iterator<JavaClass> it = get_all_classes(heap);
        return Iterators.exportIterator(new Iterator<Object>() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public Object next() {
                return ObjectJavaClass.create(it.next());
            }
        });
    }

    @CompilerDirectives.TruffleBoundary
    private static Iterator<JavaClass> get_all_classes(Heap heap) {
        //noinspection unchecked
        return (Iterator<JavaClass>) heap.getAllClasses().iterator();
    }

    /* Returns an enumeration of Java objects. Three arguments: clazz, includeSubtypes and filter.
     *  - clazz is the class whose instances are selected. If not specified, defaults to java.lang.Object.
     *  - includeSubtypes is a boolean flag that specifies whether to include subtype instances or not.
     *  Default value of this flag is true.
     *  - An optional filter expression to filter the result set of objects.
     */
    private Object invoke_objects(Object[] arguments)
            throws ArityException,
            UnsupportedTypeException {
        Args.checkArityBetween(arguments, 0, 3);

        JavaClass javaClass;
        if (arguments.length == 0) {
            javaClass = HeapUtils.findClass(heap, "java.lang.Object");
        } else {
            javaClass = HeapLanguage.unwrapJavaClassArgument(arguments, 0, heap);
        }

        boolean includeSubtypes = true;
        if (arguments.length >= 2) {
            includeSubtypes = Args.unwrapBoolean(arguments, 1);
        }

        TruffleObject filter = null;
        if (arguments.length >= 3) {
            filter = HeapLanguage.unwrapCallbackArgument(arguments, 2, "it");
        }

        final Iterator<Instance> instances = HeapUtils.getInstances(this.heap, javaClass, includeSubtypes);
        if (filter == null) {
            return Iterators.exportIterator(new IteratorObjectInstance(instances));
        } else {
            final TruffleObject finalFilter = filter;
            InteropLibrary interop = InteropLibrary.getFactory().getUncached();
            return Iterators.exportIterator(new IteratorFilter<Instance>(instances) {
                @Override
                public Object check(Instance item) {
                    try {
                        TruffleObject value = ObjectInstance.create(item);
                        Object isValid = interop.execute(finalFilter, value);
                        return Types.asBoolean(isValid) ? value : null;
                    } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException e) {
                        throw new IllegalStateException("Cannot execute filter callback.", e);
                    }
                }
            });
        }
    }

    private Object invoke_finalizables(Object[] arguments) throws ArityException {
        Args.checkArity(arguments, 0);
        Iterator<Instance> instances = HeapUtils.getFinalizerObjects(heap);
        return Iterators.exportIterator(new IteratorObjectInstance(instances));
    }

    private Object invoke_livepaths(Object[] arguments) throws ArityException, UnsupportedTypeException {
        Args.checkArityBetween(arguments, 1, 2);
        boolean includeWeak = false;
        if (arguments.length == 2) {
            includeWeak = Args.unwrapBoolean(arguments, 1);
        }
        ObjectInstance object = Args.unwrapInstance(arguments, 0, ObjectInstance.class);

        // Reference chain is a linked list of items which are either JavaClass or Instance objects.
        ReferenceChain[] chain = HeapUtils.rootsetReferencesTo(heap, object.instance, includeWeak);
        TruffleObject[] transformed = new TruffleObject[chain.length];
        for (int i=0; i < chain.length; i++) {
            ReferenceChain item = chain[i];
            transformed[i] = Iterators.exportIterator(new Iterator<Object>() {

                private ReferenceChain next = item;

                @Override
                public boolean hasNext() {
                    return next != null;
                }

                @Override
                public Object next() {
                    ReferenceChain chain = next;
                    if (chain == null) throw new NoSuchElementException();
                    next = chain.getNext();
                    Object element = chain.getObj();
                    if (element instanceof Instance) {
                        return ObjectInstance.create((Instance) element);
                    } else if (element instanceof JavaClass) {
                        return ObjectJavaClass.create((JavaClass) element);
                    } else {
                        throw new IllegalStateException("unreachable");
                    }
                }
            });
        }

        return Interop.wrapArray(transformed);
    }

    private Object invoke_roots(Object[] arguments) throws ArityException {
        Args.checkArity(arguments, 0);
        Iterator<Object> it = HeapUtils.getRoots(this.heap);
        return Iterators.exportIterator(new Iterator<Object>() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public Object next() {
                Object next = it.next();
                if (next instanceof Instance) {
                    return ObjectInstance.create((Instance) next);
                } else if (next instanceof JavaClass) {
                    return ObjectJavaClass.create((JavaClass) next);
                } else {
                    throw new IllegalStateException("unreachable");
                }
            }

        });
    }

    @ExportMessage
    static boolean hasMembers(@SuppressWarnings("unused") ObjectHeap receiver) {
        return true;
    }

    @ExportMessage
    static boolean isMemberInvocable(@SuppressWarnings("unused") ObjectHeap receiver, String member) {
        return InvokeMember.MEMBERS.hasFunction(member);
    }

    @ExportMessage
    static Object getMembers(
            @SuppressWarnings("unused") ObjectHeap receiver,
            @SuppressWarnings("unused") boolean includeInternal
    ) {
        return InvokeMember.MEMBERS;
    }

    /* Calls a callback function for each Java object. Three arguments: callback, clazz and includeSubtypes.
     *  - Callback can be either an executable object or string expression. If it returns `true`, iteration is stopped.
     *  - Clazz is the class whose instances are selected. If not specified, defaults to java.lang.Object.
     *  - IncludeSubtypes is a boolean flag that specifies whether to include subtype instances or not.
     *  Default value of this flag is true.
     */
    // Alias: Callback = TruffleObject[Executable: (Instance) -> boolean?]]
    // Arguments: [Callback, Heap, JavaClass?, Boolean]
    static final class ForEachObject extends RootNode {

        @Child @SuppressWarnings("FieldMayBeFinal")
        private LoopNode loop;

        private final FrameSlot iteratorSlot;

        protected ForEachObject() {
            super(HeapLanguage.getInstance());
            iteratorSlot = getFrameDescriptor().addFrameSlot("iterator", null, FrameSlotKind.Object);
            loop = Truffle.getRuntime().createLoopNode(new Loop(iteratorSlot));
        }

        @Override
        public Object execute(VirtualFrame frame) {
            Object[] args = frame.getArguments();
            assert args.length == 4;
            assert args[1] instanceof Heap;
            assert args[2] == null || args[2] instanceof JavaClass;
            assert args[3] instanceof Boolean;
            Iterator<Instance> iterator = HeapUtils.getInstances((Heap) args[1], (JavaClass) args[2], (boolean) args[3]);   // Truffle Boundary!
            frame.setObject(iteratorSlot, iterator);
            return loop.execute(frame);
        }

        // Arguments: [TruffleObject[Executable]]
        private static final class Loop extends Node implements RepeatingNode {

            @Child @SuppressWarnings("FieldMayBeFinal")
            private InteropLibrary interop = InteropLibrary.getFactory().createDispatched(3);

            @Child @SuppressWarnings("FieldMayBeFinal")
            private UnwrapBoolean bool = UnwrapBoolean.create();

            private final FrameSlot iteratorSlot;

            public Loop(FrameSlot iteratorSlot) {
                this.iteratorSlot = iteratorSlot;
            }

            @Override
            public boolean executeRepeating(VirtualFrame frame) {
                try {
                    Object callback = frame.getArguments()[0];
                    assert callback instanceof TruffleObject;
                    //noinspection unchecked
                    Instance item = next((Iterator<Instance>) frame.getObject(iteratorSlot));   // Truffle Boundary!
                    Boolean finish = item == null ? null : bool.execute(interop.execute(callback, ObjectInstance.create(item)));
                    return item != null && (finish == null || !finish); // End loop if there was no item or callback returned `true`.
                } catch (FrameSlotTypeException | UnsupportedTypeException | UnsupportedMessageException | ArityException e) {
                    return Errors.rethrow(RuntimeException.class, e);
                }
            }

            @CompilerDirectives.TruffleBoundary
            static Instance next(Iterator<Instance> iterator) {
                return iterator.hasNext() ? iterator.next() : null;
            }

        }

    }

    @ExportMessage
    static class InvokeMember {

        // member constants are defined here so that we can use them in specializations
        public static final String FOR_EACH_CLASS = "forEachClass";
        public static final String FOR_EACH_OBJECT = "forEachObject";
        public static final String FIND_CLASS = "findClass";
        public static final String FIND_OBJECT = "findObject";
        public static final String CLASSES = "classes";
        public static final String OBJECTS = "objects";
        public static final String FINALIZABLES = "finalizables";
        public static final String LIVEPATHS = "livepaths";
        public static final String ROOTS = "roots";

        private static final MemberDescriptor MEMBERS = MemberDescriptor.functions(
                FOR_EACH_CLASS, FOR_EACH_OBJECT, FIND_CLASS, FIND_OBJECT, CLASSES, OBJECTS, FINALIZABLES, LIVEPATHS, ROOTS
        );

        static DirectCallNode makeForEach() {
            TruffleRuntime runtime = Truffle.getRuntime();
            return runtime.createDirectCallNode(runtime.createCallTarget(new ForEachObject()));
        }

        @Specialization(guards = "FOR_EACH_CLASS.equals(member)")
        static Object doForEachClass(ObjectHeap receiver, @SuppressWarnings("unused") String member, Object[] arguments) throws UnsupportedMessageException, ArityException, UnsupportedTypeException {
            return receiver.invoke_forEachClass(arguments);
        }

        @Specialization(guards = "FOR_EACH_OBJECT.equals(member)")
        static Object doForEachObject(ObjectHeap receiver, @SuppressWarnings("unused") String member, Object[] arguments,
                                      @Cached(value = "makeForEach()", allowUncached = true) DirectCallNode node,
                                      @Cached ArgCallback callbackArg,
                                      @Cached ArgJavaClassOptional classArg,
                                      @Cached ArgBooleanOptional subclassArg
        ) throws UnsupportedTypeException, ArityException {
            Args.checkArityBetween(arguments, 1, 3);
            TruffleObject callback = callbackArg.execute(arguments, 0, new String[]{ "it" });
            JavaClass clazz = classArg.execute(arguments, 1, receiver.heap);
            Boolean includeSubclasses = subclassArg.execute(arguments, 2, true);
            node.call(callback, receiver.heap, clazz, includeSubclasses);
            return receiver;
        }

        @Specialization(guards = "FIND_CLASS.equals(member)")
        static Object doFindClass(ObjectHeap receiver, @SuppressWarnings("unused") String member, Object[] arguments) throws ArityException, UnsupportedTypeException {
            return receiver.invoke_findClass(arguments);
        }

        @Specialization(guards = "FIND_OBJECT.equals(member)")
        static Object doFindObject(ObjectHeap receiver, @SuppressWarnings("unused") String member, Object[] arguments) throws ArityException, UnsupportedTypeException {
            return receiver.invoke_findObject(arguments);
        }

        @Specialization(guards = "CLASSES.equals(member)")
        static Object doClasses(ObjectHeap receiver, @SuppressWarnings("unused") String member, Object[] arguments) throws ArityException {
            return receiver.invoke_classes(arguments);
        }

        @Specialization(guards = "OBJECTS.equals(member)")
        static Object doObjects(ObjectHeap receiver, @SuppressWarnings("unused") String member, Object[] arguments) throws ArityException, UnsupportedTypeException {
            return receiver.invoke_objects(arguments);
        }

        @Specialization(guards = "FINALIZABLES.equals(member)")
        static Object doFinalizables(ObjectHeap receiver, @SuppressWarnings("unused") String member, Object[] arguments) throws ArityException {
            return receiver.invoke_finalizables(arguments);
        }

        @Specialization(guards = "LIVEPATHS.equals(member)")
        static Object doLivepaths(ObjectHeap receiver, @SuppressWarnings("unused") String member, Object[] arguments) throws ArityException, UnsupportedTypeException {
            return receiver.invoke_livepaths(arguments);
        }

        @Specialization(guards = "ROOTS.equals(member)")
        static Object doRoots(ObjectHeap receiver, @SuppressWarnings("unused") String member, Object[] arguments) throws ArityException {
            return receiver.invoke_roots(arguments);
        }

        @Specialization @SuppressWarnings("unused")
        public static Object doUnknown(ObjectHeap receiver, String member, Object[] arguments) throws UnknownIdentifierException {
            throw UnknownIdentifierException.create(member);
        }

    }

}

