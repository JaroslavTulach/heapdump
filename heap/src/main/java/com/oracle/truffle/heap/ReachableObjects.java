package com.oracle.truffle.heap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.netbeans.lib.profiler.heap.Field;
import org.netbeans.lib.profiler.heap.FieldValue;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.ObjectArrayInstance;
import org.netbeans.lib.profiler.heap.ObjectFieldValue;

/**
 *
 * @author Jaroslav Bachorik
 */
public class ReachableObjects {
    private ReachableExcludes excludes;
    private Instance root;
    private Set<Instance> alreadyReached;

    public ReachableObjects(Instance root, final ReachableExcludes excludes) {
        this.root = root;
        this.excludes = excludes;
        alreadyReached = new HashSet<>();
    }

    public Instance getRoot() {
        return root;
    }

    public Iterator<Instance> getReachables() {
        return new TreeIterator<Instance, Instance>(root) {

            @Override
            protected Iterator<Instance> getSameLevelIterator(Instance popped) {
                Collection<Instance> instances = new ArrayList<Instance>();
                for(Object fv : popped.getFieldValues()) {
                    if (fv instanceof ObjectFieldValue) {
                        if (excludes == null || !excludes.isExcluded(getFQFieldName(((FieldValue)fv).getField()))) {
                            Instance i = ((ObjectFieldValue)fv).getInstance();
                            if (i != null && !alreadyReached.contains(i)) {
                                instances.add(i);
                                alreadyReached.add(i);
                            }
                        }
                    }
                }
                if (popped instanceof ObjectArrayInstance) {
                    for(Object el : ((ObjectArrayInstance)popped).getValues()) {
                        Instance i = (Instance) el;
                        if (i != null && !alreadyReached.contains(i)) {
                            instances.add(i);
                            alreadyReached.add(i);
                        }
                    }
                }
                return instances.iterator();
            }

            @Override
            protected Iterator<Instance> getTraversingIterator(Instance popped) {
                Collection<Instance> instances = new ArrayList<Instance>();
                for(Object fv : popped.getFieldValues()) {
                    if (fv instanceof ObjectFieldValue) {
                        if (excludes == null || !excludes.isExcluded(getFQFieldName(((FieldValue)fv).getField()))) {
                            Instance i = ((ObjectFieldValue)fv).getInstance();
                            if (i != null) {
                                instances.add(i);
                            }
                        }
                    }
                }
                if (popped instanceof ObjectArrayInstance) {
                    for(Object el : ((ObjectArrayInstance)popped).getValues()) {
                        if (el instanceof Instance) {
                            instances.add((Instance)el);
                        }
                    }
                }
                return instances.iterator();
            }
        };
    }

    public long getTotalSize() {
        return -1;
    }

    private String getFQFieldName(Field fld) {
        return fld.getDeclaringClass().getName() + "." + fld.getName();
    }
}
