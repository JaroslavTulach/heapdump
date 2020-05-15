package benchmark.runner;

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

public abstract class Bench {

    abstract void computeNative(Heap heap);

    public void runBenchmark(String[] args, String fileName) throws IOException, OQLException {
        File heapFile = new File(args[0]);
        File scriptFile = new File("../scripts/"+fileName);
        String script = Files.readAllLines(scriptFile.toPath()).stream().reduce("", (s, s2) -> s + "\n" + s2);
        System.out.println("Execute script: " + fileName);
        long start;
        Heap heap;
        switch (System.getProperty("bench")) {
            case "native":
                start = System.currentTimeMillis();
                heap = HeapFactory.createHeap(heapFile);
                System.out.println("Heap dump loaded in "+(System.currentTimeMillis() - start));
                start = System.currentTimeMillis();
                computeNative(heap);
                System.out.println("Native implementation finished in "+(System.currentTimeMillis() - start)+"ms.");
                return;
            case "original":
                start = System.currentTimeMillis();
                OQLEngine engine = new OQLEngine(HeapFactory.createHeap(heapFile));
                System.out.println("Heap dump loaded in "+(System.currentTimeMillis() - start));
                start = System.currentTimeMillis();
                engine.executeQuery(script, VISITOR);
                System.out.println("Query finished in "+(System.currentTimeMillis() - start)+"ms.");
                return;
            case "truffle":
                start = System.currentTimeMillis();
                OQLEngineImpl truffle = new OQLEngineImpl(heapFile);
                System.out.println("Heap dump loaded in "+(System.currentTimeMillis() - start));
                start = System.currentTimeMillis();
                truffle.executeQuery(script, VISITOR);
                System.out.println("Query finished in "+(System.currentTimeMillis() - start)+"ms.");
                return;
            default:
                throw new IllegalStateException("Unknown benchmark type "+System.getProperty("bench"));
        }
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
