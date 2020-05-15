package com.oracle.truffle.heap;

import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.HeapFactory;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.modules.profiler.oql.engine.api.OQLEngine;
import org.netbeans.modules.profiler.oql.engine.api.OQLException;
import org.netbeans.modules.profiler.oql.engine.api.impl.truffle.OQLEngineImpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;

public class ScriptRunner {

    public static void main(String[] args) throws IOException, OQLException {
        if (args.length != 2) {
            System.err.println("Expected two arguments: path to a heap file and path to an OQL script file.");
        }
        File heapFile = new File(args[0]);
        File scriptFile = new File(args[1]);
        String script = Files.readAllLines(scriptFile.toPath()).stream().reduce("", (s, s2) -> s + "\n" + s2);
        System.out.println("Running on: "+System.getProperty("java.home"));
        if ("off".equals(System.getProperty("truffle", "on"))) {
            System.out.println("Running with original OQL Engine:");
            runWithOriginal(heapFile, script);
        } else {
            System.out.println("Running with Heap Language:");
            runWithHeapLanguage(heapFile, script);
        }
        /*int count = 0;
        Heap heap = HeapFactory.createHeap(heapFile);
        long start = System.currentTimeMillis();
        //noinspection unchecked
        Iterator<Instance> it = heap.getAllInstancesIterator();
        while (it.hasNext()) {
            if (it.next().getJavaClass().getName().startsWith("java")) {
                count += 1;
            }
        }
        System.out.println("Count: "+count+" in "+(System.currentTimeMillis() - start)+"ms");*/
    }

    private static void runWithHeapLanguage(File heapFile, String script) throws IOException, OQLException {
        long start = System.currentTimeMillis();
        OQLEngineImpl engine = new OQLEngineImpl(heapFile);
        System.out.println("Heap dump loaded in "+(System.currentTimeMillis() - start));
        start = System.currentTimeMillis();
        engine.executeQuery(script, VISITOR);
        System.out.println("Query finished in "+(System.currentTimeMillis() - start)+"ms.");
    }

    private static void runWithOriginal(File heapFile, String script) throws IOException, OQLException {
        long start = System.currentTimeMillis();
        OQLEngine engine = new OQLEngine(HeapFactory.createHeap(heapFile));
        System.out.println("Heap dump loaded in "+(System.currentTimeMillis() - start));
        start = System.currentTimeMillis();
        engine.executeQuery(script, VISITOR);
        System.out.println("Query finished in "+(System.currentTimeMillis() - start)+"ms.");
    }

    private static final OQLEngine.ObjectVisitor VISITOR = o -> {
        if (o instanceof JavaClass) {
            System.out.println("Found java class: "+((JavaClass) o).getName());
        } else if (o instanceof Instance) {
            System.out.println("Found instance: "+((Instance) o).getInstanceNumber()+" of "+((Instance) o).getJavaClass().getName());
        } else {
            System.out.println("Found: "+o);
        }
        return false;
    };

}
