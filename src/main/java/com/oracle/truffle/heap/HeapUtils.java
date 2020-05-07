package com.oracle.truffle.heap;

import org.netbeans.lib.profiler.heap.*;
import org.netbeans.modules.profiler.oql.engine.api.impl.TreeIterator;

import java.lang.reflect.Method;
import java.util.*;

import static org.netbeans.lib.profiler.utils.VMUtils.*;

public abstract class HeapUtils {
    private HeapUtils() {}

    public static String instanceString(Instance instance) {
        try {
            // Taken from org.netbeans.modules.profiler.oql.engine.api.impl.Snapshot
            if (instance.getJavaClass().getName().equals(String.class.getName())) {
                Class proxy = Class.forName("org.netbeans.lib.profiler.heap.HprofProxy"); // NOI18N
                Method method = proxy.getDeclaredMethod("getString", Instance.class); // NOI18N
                method.setAccessible(true);
                return (String) method.invoke(proxy, instance);
            } else if (instance.getJavaClass().getName().equals("char[]")) { // NOI18N
                Method method = instance.getClass().getDeclaredMethod("getChars", int.class, int.class);
                method.setAccessible(true);
                char[] chars = (char[])method.invoke(instance, 0, ((PrimitiveArrayInstance) instance).getLength());
                if (chars != null) {
                    return new String(chars);
                } else {
                    return "*null*"; // NOI18N
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("Error getting toString() value of an instance dump", ex);
        }
        return instance.toString();
    }

    public static Iterator<Instance> getFinalizerObjects(Heap heap) {
        // TODO: This needs to be tested...
        JavaClass clazz = findClass(heap, "java.lang.ref.Finalizer");
        Instance queue = (Instance) clazz.getValueOfStaticField("queue");
        Instance head = (Instance) queue.getValueOfField("head");

        List<Instance> finalizables = new ArrayList<>();
        if (head != null) {
            while (true) {
                Instance referentFld = (Instance) head.getValueOfField("referent");
                Instance nextFld = (Instance) head.getValueOfField("next");

                if (nextFld == null || nextFld.equals(head)) {
                    break;
                }
                head = nextFld;
                finalizables.add(referentFld);
            }
        }
        return finalizables.iterator();
    }

    public static Iterator<Instance> getInstances(Heap heap, final JavaClass clazz, final boolean includeSubclasses) {
        // special case for all subclasses of java.lang.Object
        if (includeSubclasses && clazz.getSuperClass() == null) {
            //noinspection unchecked
            return (Iterator<Instance>) heap.getAllInstancesIterator();
        }
        return new TreeIterator<Instance, JavaClass>(clazz) {

            @Override
            protected Iterator<Instance> getSameLevelIterator(JavaClass popped) {
                return popped.getInstances().iterator();
            }

            @Override
            protected Iterator<JavaClass> getTraversingIterator(JavaClass popped) {
                return includeSubclasses ? popped.getSubClasses().iterator() : Collections.<JavaClass>emptyList().iterator();
            }
        };
    }

    public static JavaClass findClass(Heap heap, String name) {
        try {
            long classId;
            if (name.startsWith("0x")) {
                classId = Long.parseLong(name.substring(2), 16);
            } else {
                classId = Long.parseLong(name);
            }
            return heap.getJavaClassByID(classId);
        } catch (NumberFormatException e) {}
        return heap.getJavaClassByName(preprocessClassName(name));
    }

    private static String preprocessClassName(String className) {
        int arrDim = 0;
        if (className.startsWith("[")) { // NOI18N
            arrDim = className.lastIndexOf('[') + 1; // NOI18N

            className = className.substring(arrDim);
        }
        if (className.length() == 1) {
            switch (className) {
                case INT_CODE:
                    className = INT_STRING;
                    break;
                case LONG_CODE:
                    className = LONG_STRING;
                    break;
                case DOUBLE_CODE:
                    className = DOUBLE_STRING;
                    break;
                case FLOAT_CODE:
                    className = FLOAT_STRING;
                    break;
                case BYTE_CODE:
                    className = BYTE_STRING;
                    break;
                case SHORT_CODE:
                    className = SHORT_STRING;
                    break;
                case CHAR_CODE:
                    className = CHAR_STRING;
                    break;
                case BOOLEAN_CODE:
                    className = BOOLEAN_STRING;
                    break;
            }
        }
        if (arrDim > 0 && className.charAt(0) == REFERENCE) {   // class name
            className = className.substring(1);
        }
        StringBuilder sb = new StringBuilder(className);
        for (int i = 0; i < arrDim; i++) {
            sb.append("[]"); // NOI18N
        }

        return sb.toString();
    }

    // Returns a list of objects which are either JavaClass or Instance objects.
    public static Iterator<Object> getRoots(Heap heap) {
        // TODO: This is completely not according to specs
        return getRootsList(heap).iterator();
    }

    public static List<Object> getRootsList(Heap heap) {
        List<Object> roots = new ArrayList<>();
        //noinspection unchecked
        for(GCRoot root : (Collection<GCRoot>) heap.getGCRoots()) {
            Instance inst = root.getInstance();
            if (inst.getJavaClass().getName().equals("java.lang.Class")) {
                JavaClass jc = heap.getJavaClassByID(inst.getInstanceId());
                if (jc != null) {
                    roots.add(jc);
                } else {
                    roots.add(inst);
                }
            } else {
                roots.add(inst);
            }
        }
        return roots;
    }

    public static Iterator<Instance> getReferrers(Object obj, boolean includeWeak) {
        List<Instance> instances  = new ArrayList<>();
        List<Object> references = new ArrayList<>();

        if (obj instanceof Instance) {
            references.addAll(((Instance)obj).getReferences());
        } else if (obj instanceof JavaClass) {
            references.addAll(((JavaClass)obj).getInstances());
            references.add(((JavaClass)obj).getClassLoader());
        }
        if (!references.isEmpty()) {
            for (Object o : references) {
                if (o instanceof Value) {
                    Value val = (Value) o;
                    Instance inst = val.getDefiningInstance();
                    if (includeWeak || !isWeakRef(inst.getJavaClass())) {
                        instances.add(inst);
                    }
                } else if (o instanceof Instance) {
                    if (includeWeak || !isWeakRef(((Instance)o).getJavaClass())) {
                        instances.add((Instance) o);
                    }
                }
            }
        }
        return instances.iterator();
    }

    private static boolean isWeakRef(JavaClass clazz) {
        if (clazz == null) return false;
        boolean isWeak = clazz.getName().equals("java.lang.ref.Reference");
        isWeak = isWeak || clazz.getName().equals("sun.misc.Ref");  // some very old JVMs
        if (isWeak) {
            return true;
        } else {
            return isWeakRef(clazz.getSuperClass());
        }
    }

    private static boolean isAssignable(JavaClass from, JavaClass to) {
        if (from == to) {
            return true;
        } else if (from == null) {
            return false;
        } else {
            return isAssignable(from.getSuperClass(), to);
            // Trivial tail recursion:  I have faith in javac.
        }
    }

}
