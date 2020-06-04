package benchmark;

import org.netbeans.lib.profiler.heap.HeapFactory;
import org.netbeans.modules.profiler.oql.engine.api.OQLEngine;
import org.netbeans.modules.profiler.oql.engine.api.OQLException;
import org.netbeans.modules.profiler.oql.engine.api.impl.truffle.OQLEngineImpl;
import org.openjdk.jmh.annotations.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class Bench {

    @State(Scope.Thread)
    public static class Engine {

        @Param({ "01_count_objects_via_callback.js" })
        public String scriptFile;

        @Param({ "small_heap.bin" })
        public String heapFile;

        public String scriptFileContents;

        public OQLEngineImpl engine;
        public OQLEngine oldEngine;

        @Setup(Level.Trial)
        public void setUp() throws IOException, URISyntaxException {
            File heapFile = copyResourceAsTempFile("/"+this.heapFile);
            this.engine = new OQLEngineImpl(heapFile);
            this.oldEngine = new OQLEngine(HeapFactory.createHeap(heapFile));
            File scriptFile = copyResourceAsTempFile("/"+this.scriptFile);
            this.scriptFileContents = Files.readAllLines(scriptFile.toPath()).stream().reduce("", (s, s2) -> s + "\n" + s2);
        }

        private File copyResourceAsTempFile(String path) throws IOException {
            Path temp = Files.createTempFile("heap_", ".hprof");
            Files.copy(getClass().getResourceAsStream(path), temp, StandardCopyOption.REPLACE_EXISTING);
            return temp.toFile();
        }

    }

    @Fork(value = 1, warmups = 1)
    @Benchmark
    public void truffleEngine(Engine engine) throws OQLException {
        engine.engine.executeQuery(engine.scriptFileContents, OQLEngine.ObjectVisitor.DEFAULT);
    }

    @Fork(value = 1, warmups = 1)
    @Benchmark
    public void oldEngine(Engine engine) throws OQLException {
        engine.oldEngine.executeQuery(engine.scriptFileContents, OQLEngine.ObjectVisitor.DEFAULT);
    }

}
