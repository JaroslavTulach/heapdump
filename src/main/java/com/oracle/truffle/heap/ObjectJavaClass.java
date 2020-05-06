package com.oracle.truffle.heap;

import com.oracle.truffle.api.interop.*;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.interop.MemberDescriptor;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.lib.profiler.heap.Field;
import org.netbeans.lib.profiler.heap.JavaClass;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A truffle object wrapper around {@link JavaClass}, implementing the API given by the OQL specification.
 */
@ExportLibrary(InteropLibrary.class)
public final class ObjectJavaClass implements TruffleObject {

    public static TruffleObject create(@NullAllowed JavaClass clazz) {
        if (clazz == null) return HeapLanguage.NULL; else {
            return new ObjectJavaClass(clazz);
        }
    }

    // Constants for member names
    private static final String NAME = "name";
    private static final String SUPERCLASS = "superclass";
    private static final String STATICS = "statics";
    private static final String FIELDS = "fields";
    private static final String LOADER = "loader";
    private static final String SIGNERS = "signers";
    private static final String PROTECTION_DOMAIN = "protectionDomain";
    private static final String IS_SUBCLASS_OF = "isSubclassOf";
    private static final String IS_SUPERCLASS_OF = "isSuperclassOf";
    private static final String SUBCLASSES = "subclasses";
    private static final String SUPERCLASSES = "superclasses";
    private static final String AS_JAVA_CLASS = "asJavaClass";

    private static final MemberDescriptor MEMBERS = MemberDescriptor.build(new String[]{
            NAME, SUPERCLASS, STATICS, FIELDS, LOADER, SIGNERS, PROTECTION_DOMAIN
    }, new String[] {
            IS_SUBCLASS_OF, IS_SUPERCLASS_OF, SUBCLASSES, SUPERCLASSES, AS_JAVA_CLASS
    });

    @NonNull
    private final JavaClass clazz;

    private ObjectJavaClass(@NonNull JavaClass clazz) {
        this.clazz = clazz;
    }

    public JavaClass getJavaClass() {
        return this.clazz;
    }

    /* Name of the class. */
    private String read_name() {
        return this.clazz.getName();
    }

    /* Class object for super class (or null if java.lang.Object). */
    private TruffleObject read_superclass() {
        JavaClass superclass = this.clazz.getSuperClass();
        return superclass == null ? HeapLanguage.NULL : new ObjectJavaClass(superclass);
    }

    /* Name, value pairs for static fields of the Class. */
    private TruffleObject read_statics() {
        return ObjectStatics.create(this.clazz);
    }

    /* Array of field objects. Field objects have name and signature properties. */
    private TruffleObject read_fields() {
        //noinspection unchecked
        List<Field> fields = clazz.getFields();
        Object[] items = new Object[fields.size()];
        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);
            items[i] = new ObjectFieldDescriptor(field.getName(), field.getType().getName());
        }
        return Interop.wrapArray(items);
    }

    /* ClassLoader object that loaded this class. */
    private TruffleObject read_loader() {
        return ObjectInstance.create(this.clazz.getClassLoader());
    }

    /* Signers that signed this class. */
    private Object read_signers() {
        return "";  // TODO: Not implemented.
    }

    /* Protection domain to which this class belongs. */
    private Object read_protectionDomain() {
        return ""; // TODO: Not implemented.
    }

    /* Tests whether given class is direct or indirect subclass of this class or not. */
    private Object invoke_isSubclassOf(Object[] arguments) throws ArityException, UnsupportedTypeException {
        Args.checkArity(arguments, 1);
        ObjectJavaClass argument = Args.unwrapInstance(arguments, 0, ObjectJavaClass.class);
        return isSubclassOf(this.clazz, argument.clazz);
    }

    /* Tests whether given Class is direct or indirect superclass of this class or not. */
    private Object invoke_isSuperclassOf(Object[] arguments) throws ArityException, UnsupportedTypeException {
        Args.checkArity(arguments, 1);
        ObjectJavaClass argument = Args.unwrapInstance(arguments, 0, ObjectJavaClass.class);
        return isSubclassOf(argument.clazz, this.clazz);
    }

    private static boolean isSubclassOf(JavaClass child, JavaClass parent) {
        while (child != null) {
            if (child.equals(parent)) {
                return true;
            }
            child = child.getSuperClass();
        }
        return false;
    }

    /* Returns array of direct and indirect subclasses. */
    private Object invoke_subclasses(Object[] arguments) throws ArityException {
        Args.checkArity(arguments, 0);
        //noinspection unchecked
        Collection<JavaClass> subClasses = this.clazz.getSubClasses();
        ObjectJavaClass[] items = new ObjectJavaClass[subClasses.size()];
        int i = 0;
        for (JavaClass subClass : subClasses) {
            items[i] = new ObjectJavaClass(subClass);
            i += 1;
        }
        return Interop.wrapArray(items);
    }

    /* Returns array of direct and indirect superclasses. */
    private Object invoke_superclasses(Object[] arguments) throws ArityException {
        Args.checkArity(arguments, 0);
        ArrayList<ObjectJavaClass> superClasses = new ArrayList<>();
        JavaClass superClass = this.clazz.getSuperClass();
        while (superClass != null) {
            superClasses.add(new ObjectJavaClass(superClass));
            superClass = superClass.getSuperClass();
        }
        return Interop.wrapArray(superClasses.toArray());
    }

    @ExportMessage
    static boolean hasMembers(@SuppressWarnings("unused") ObjectJavaClass receiver) {
        return true;
    }

    @ExportMessage
    static Object getMembers(
            @SuppressWarnings("unused") ObjectJavaClass receiver,
            @SuppressWarnings("unused") boolean includeInternal
    ) {
        return MEMBERS;
    }

    @ExportMessage
    static boolean isMemberInvocable(@SuppressWarnings("unused") ObjectJavaClass receiver, String member) {
        return MEMBERS.hasFunction(member);
    }

    @ExportMessage
    static boolean isMemberReadable(@SuppressWarnings("unused") ObjectJavaClass receiver, String member) {
        return MEMBERS.hasProperty(member);
    }

    @ExportMessage
    static Object readMember(ObjectJavaClass receiver, String member) throws UnknownIdentifierException {
        switch (member) {
            case NAME:
                return receiver.read_name();
            case SUPERCLASS:
                return receiver.read_superclass();
            case STATICS:
                return receiver.read_statics();
            case FIELDS:
                return receiver.read_fields();
            case LOADER:
                return receiver.read_loader();
            case SIGNERS:
                return receiver.read_signers();
            case PROTECTION_DOMAIN:
                return receiver.read_protectionDomain();
            default:
                throw UnknownIdentifierException.create(member);
        }
    }

    @ExportMessage
    static Object invokeMember(ObjectJavaClass receiver, String member, Object[] arguments) throws UnknownIdentifierException, ArityException, UnsupportedTypeException {
        switch (member) {
            case IS_SUBCLASS_OF:
                return receiver.invoke_isSubclassOf(arguments);
            case IS_SUPERCLASS_OF:
                return receiver.invoke_isSuperclassOf(arguments);
            case SUBCLASSES:
                return receiver.invoke_subclasses(arguments);
            case SUPERCLASSES:
                return receiver.invoke_superclasses(arguments);
            case AS_JAVA_CLASS:
                return HeapLanguage.asGuestValue(receiver.getJavaClass());
            default:
                throw UnknownIdentifierException.create(member);
        }
    }

}
