package benchmark.problem;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Supplier;

public class MakeHeapDump {

    @SuppressWarnings("ResultOfMethodCallIgnored")
    static void dump(String name, Supplier<? extends BooleanNetwork> constructor) throws IOException {
        System.out.print("Creating network `"+name+"`...");
        BooleanNetwork network = constructor.get();
        System.out.print("dumping ... ");
        File folder = new File("../dumps");
        folder.mkdirs();
        File dump = new File(folder, name+".hprof");
        dump.delete();
        System.gc();
        BooleanNetwork.dumpHeap(dump.getAbsolutePath(), false);
        System.out.println("dumped: "+network.stateSpace.size()+" states.");
    }

    public static void main(String[] args) throws IOException {
        dump("tumor_cell", TumorCellPathway::new);
    }


}
