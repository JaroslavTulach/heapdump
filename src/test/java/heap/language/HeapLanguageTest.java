package heap.language;

import heap.language.functions.ClassOf;
import heap.language.util.HeapLanguageUtils;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.Before;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

public abstract class HeapLanguageTest {

    protected Context ctx;
    protected Value heap;

    @Before
    public void init() throws URISyntaxException, IOException {
        URL url = getClass().getClassLoader().getResource("heap/language/small_heap.bin");
        File heapFile = new File(url.toURI());
        this.ctx = Context.newBuilder().build();
        Source heapSrc = Source.newBuilder("heap", HeapLanguageUtils.bytesOf(heapFile), heapFile.getName())
                .uri(heapFile.toURI())
                .mimeType("application/x-netbeans-profiler-hprof").build();
        this.heap = this.ctx.eval(heapSrc);
        ctx.getBindings("js").putMember("heap", this.heap);
        ctx.getBindings("js").putMember("classof", ClassOf.INSTANCE);
        initJS();
    }

    protected Value runJS(String javascript) {
        try {
            Source querySrc = Source.newBuilder("js", javascript, "query.js").build();
            return ctx.eval(querySrc);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // here, you can init the JS environment
    protected void initJS() {}

}
