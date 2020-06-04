package com.oracle.truffle.heap;

import org.graalvm.polyglot.*;
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
        this.ctx = Context.newBuilder()
                .allowAllAccess(true)
                .build();
        ctx.initialize("heap");
        ctx.getBindings("heap").getMember(HeapLanguage.Globals.SET_SCRIPT_LANGUAGE).execute("js");
        ctx.getBindings("heap").getMember(HeapLanguage.Globals.BIND_GLOBAL_SYMBOLS).execute(ctx.getBindings("js"));

        URL url = getClass().getResource("/com/oracle/truffle/heap/small_heap.bin");
        File heapFile = new File(url.toURI());
        Source heapSrc = Source.newBuilder("heap", HeapUtils.bytesOf(heapFile), heapFile.getName())
                .uri(heapFile.toURI())
                .mimeType("application/x-netbeans-profiler-hprof").build();
        this.heap = this.ctx.eval(heapSrc);
        ctx.getBindings("js").putMember("heap", this.heap);
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
