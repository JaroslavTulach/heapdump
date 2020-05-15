package benchmark.runner;

import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.Instance;
import org.netbeans.modules.profiler.oql.engine.api.OQLException;

import java.io.IOException;
import java.util.Iterator;

public class Bench01 extends Bench {

    public static void main(String[] args) throws IOException, OQLException {
        new Bench01().runBenchmark(args, "01_forEachObject.js");
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
