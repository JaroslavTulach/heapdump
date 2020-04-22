package heap.language.heap;

import com.oracle.truffle.api.interop.*;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import heap.language.HeapLanguage;
import heap.language.util.Descriptors;
import heap.language.util.HeapLanguageUtils;
import heap.language.util.NullValue;
import heap.language.util.ReadOnlyArray;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.lib.profiler.heap.Field;
import org.netbeans.lib.profiler.heap.JavaClass;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A truffle object wrapper around {@link JavaClass}, implementing the API given by the OQL specification.
 */
@ExportLibrary(InteropLibrary.class)
public class JavaClassObject implements TruffleObject {

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

    private static final Descriptors MEMBERS = Descriptors.build(new String[]{
            NAME, SUPERCLASS, STATICS, FIELDS, LOADER, SIGNERS, PROTECTION_DOMAIN
    }, new String[] {
            IS_SUBCLASS_OF, IS_SUPERCLASS_OF, SUBCLASSES, SUPERCLASSES, AS_JAVA_CLASS
    });

    @NonNull
    private final JavaClass clazz;

    public JavaClassObject(@NonNull JavaClass clazz) {
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
    private Object read_superclass() {
        JavaClass superclass = this.clazz.getSuperClass();
        return superclass == null ? NullValue.INSTANCE : new JavaClassObject(superclass);
    }

    /* Name, value pairs for static fields of the Class. */
    private Object read_statics() {
        return new StaticsObject(this.clazz);
    }

    /* Array of field heap. Field heap have name, signature properties. */
    private Object read_fields() {
        //noinspection unchecked
        List<Field> fields = clazz.getFields();
        Object[] items = new Object[fields.size()];
        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);
            items[i] = new FieldDescriptorObject(field.getName(), field.getType().getName());
        }
        return new ReadOnlyArray(items);
    }

    /* ClassLoader object that loaded this class. */
    private Object read_loader() {
        return HeapLanguageUtils.heapToTruffle(this.clazz.getClassLoader());
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
        HeapLanguageUtils.arityCheck(1, arguments);
        if (!(arguments[0] instanceof JavaClassObject)) {
            throw UnsupportedTypeException.create(arguments, "Expected Java Class instance.");
        }

        return isSubclassOf(this.clazz, ((JavaClassObject) arguments[0]).clazz);
    }

    /* Tests whether given Class is direct or indirect superclass of this class or not. */
    private Object invoke_isSuperclassOf(Object[] arguments) throws ArityException, UnsupportedTypeException {
        HeapLanguageUtils.arityCheck(1, arguments);
        if (!(arguments[0] instanceof JavaClassObject)) {
            throw UnsupportedTypeException.create(arguments, "Expected Java Class instance.");
        }

        return isSubclassOf(((JavaClassObject) arguments[0]).clazz, this.clazz);
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
        HeapLanguageUtils.arityCheck(0, arguments);
        //noinspection unchecked
        Collection<JavaClass> subClasses = this.clazz.getSubClasses();
        JavaClassObject[] items = new JavaClassObject[subClasses.size()];
        int i = 0;
        for (JavaClass subClass : subClasses) {
            items[i] = new JavaClassObject(subClass);
            i += 1;
        }
        return new ReadOnlyArray(items);
    }

    /* Returns array of direct and indirect superclasses. */
    private Object invoke_superclasses(Object[] arguments) throws ArityException {
        HeapLanguageUtils.arityCheck(0, arguments);
        ArrayList<JavaClassObject> superClasses = new ArrayList<>();
        JavaClass superClass = this.clazz.getSuperClass();
        while (superClass != null) {
            superClasses.add(new JavaClassObject(superClass));
            superClass = superClass.getSuperClass();
        }
        return new ReadOnlyArray(superClasses.toArray(new JavaClassObject[0]));
    }

    @ExportMessage
    static boolean hasMembers(@SuppressWarnings("unused") JavaClassObject receiver) {
        return true;
    }

    @ExportMessage
    static Object getMembers(
            @SuppressWarnings("unused") JavaClassObject receiver,
            @SuppressWarnings("unused") boolean includeInternal
    ) {
        return MEMBERS;
    }

    @ExportMessage
    static boolean isMemberInvocable(@SuppressWarnings("unused") JavaClassObject receiver, String member) {
        return MEMBERS.hasFunction(member);
    }

    @ExportMessage
    static boolean isMemberReadable(@SuppressWarnings("unused") JavaClassObject receiver, String member) {
        return MEMBERS.hasProperty(member);
    }

    @ExportMessage
    static Object readMember(JavaClassObject receiver, String member) throws UnknownIdentifierException {
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
    static Object invokeMember(JavaClassObject receiver, String member, Object[] arguments) throws UnknownIdentifierException, ArityException, UnsupportedTypeException {
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
