package benchmark.runner;

import org.netbeans.lib.profiler.heap.*;
import org.netbeans.modules.profiler.oql.engine.api.OQLEngine;
import org.netbeans.modules.profiler.oql.engine.api.OQLException;
import org.netbeans.modules.profiler.oql.engine.api.impl.truffle.OQLEngineImpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;

public abstract class Bench {

    abstract void computeNative(Heap heap);

    public void runBenchmark(String[] args, String fileName) throws IOException, OQLException {
        File heapFile = new File(args[0]);
        File scriptFile = new File("../scripts/"+fileName);
        String script = Files.readAllLines(scriptFile.toPath()).stream().reduce("", (s, s2) -> s + "\n" + s2);
        System.out.println("Execute script: " + fileName);
        System.out.println("Running with: " + System.getProperty("engine"));
        long start;
        Heap heap;
        switch (System.getProperty("engine")) {
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

    public static final OQLEngine.ObjectVisitor VISITOR = o -> {
        if (o instanceof JavaClass) {
            System.out.println("Found java class: "+((JavaClass) o).getName());
        } else if (o instanceof Instance) {
            System.out.println("Found instance: "+((Instance) o).getInstanceNumber()+" of "+((Instance) o).getJavaClass().getName());
        } else {
            System.out.println("Found: "+o);
        }
        return false;
    };

    public static class Bench_01_Count_objects_via_callback extends Bench {

        public static void main(String[] args) throws IOException, OQLException {
            new Bench_01_Count_objects_via_callback().runBenchmark(args, "01_count_objects_via_callback.js");
        }

        @Override
        void computeNative(Heap heap) {
            int count = 0;
            Iterator<?> it = heap.getAllInstancesIterator();
            while (it.hasNext()) {
                it.next();
                count += 1;
            }
            System.out.println("Total objects: "+count);
        }
    }

    public static class Bench_02_Count_objects_with_package_via_callback extends Bench {

        public static void main(String[] args) throws IOException, OQLException {
            new Bench_02_Count_objects_with_package_via_callback().runBenchmark(args, "02_count_objects_with_package_via_callback.js");
        }

        @Override
        void computeNative(Heap heap) {
            int count = 0;
            //noinspection unchecked
            Iterator<Instance> it = heap.getAllInstancesIterator();
            while (it.hasNext()) {
                if (it.next().getJavaClass().getName().startsWith("benchmark.problem")) {
                    count += 1;
                }
            }
            System.out.println("Counted instances: "+count);
        }

    }

    public static class Bench_03_Count_objects_with_package_via_method_callback extends Bench {

        public static void main(String[] args) throws IOException, OQLException {
            new Bench_03_Count_objects_with_package_via_method_callback().runBenchmark(args, "03_count_objects_with_package_via_method_callback.js");
        }

        @Override
        void computeNative(Heap heap) {
            throw new IllegalStateException("unimplemented");
        }
    }

    public static class Bench_04_Count_objects_with_package_via_method_string extends Bench {

        public static void main(String[] args) throws IOException, OQLException {
            new Bench_04_Count_objects_with_package_via_method_string().runBenchmark(args, "04_count_objects_with_package_via_method_string.js");
        }

        @Override
        void computeNative(Heap heap) {
            throw new IllegalStateException("unimplemented");
        }
    }

    public static class Bench_05_Complex_where_clause_with_method_callback extends Bench {

        public static void main(String[] args) throws IOException, OQLException {
            new Bench_05_Complex_where_clause_with_method_callback().runBenchmark(args, "05_complex_where_clause_with_method_callback.js");
        }

        @Override
        void computeNative(Heap heap) {
            //noinspection unchecked
            Iterable<Instance> instances = heap.getJavaClassByName("benchmark.problem.BooleanNetwork$State").getInstances();
            for (Instance item : instances) {
                //noinspection unchecked
                List<String> values = ((PrimitiveArrayInstance) item.getValueOfField("values")).getValues();
                if (!values.contains("false")) {
                    System.out.println("Found: " + item.getInstanceId());
                }
            }
        }

    }

}
