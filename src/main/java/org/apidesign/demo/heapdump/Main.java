/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apidesign.demo.heapdump;

import java.io.File;
import java.io.IOException;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.HeapFactory;
import org.netbeans.modules.profiler.oql.engine.api.OQLEngine;
import org.netbeans.modules.profiler.oql.engine.api.OQLException;

public class Main {
    public static void main(String... args) throws Exception {
        String fileName = args.length > 0 ? args[0] : "dump.hprof";
        final File file = new File(fileName);
        if (!file.exists()) {
            throw new IOException("Cannot find " + file);
        }
        System.err.println("Analyzing the heap");
        analyzeHeap(file, 20);
        System.setProperty("polyglot.js.nashorn-compat", "true");
        System.err.println("Loading " + file);
        Heap heap = HeapFactory.createHeap(file);
        System.err.println("Querying the heap");
        queryHeap(heap, 20);
    }

    static void queryHeap(Heap heap, int count) throws OQLException {
        final OQLEngine eng = new OQLEngine(heap);
        for (int i = 1; i <= count; i++) {
            long now = System.currentTimeMillis();
            eng.executeQuery(
                    "var arr = [];\n" +
                            "heap.forEachObject(function(o) {\n" +
                            "  if (o.length > 255) {\n" +
                            "    arr.push(o);\n" +
                            "  }\n" +
                            "}, 'int[]')\n" +
                            "print('Found ' + arr.length + ' long int arrays');"
                    , OQLEngine.ObjectVisitor.DEFAULT);
            long took = System.currentTimeMillis() - now;
            System.err.println("Round #" + i + " took " + took + " ms");
        }
    }

    static int analyzeHeap(File heapFile, int count) throws IOException {
        Context ctx = Context.newBuilder().build();
        System.err.println("Parsing the " + heapFile);
        Source heapSrc = Source.newBuilder("heap", heapFile)
                .mimeType("application/x-netbeans-profiler-hprof").build();
        Value heap = ctx.eval(heapSrc);

        final Source jsFnSrc = Source.newBuilder("js", "(function(heap) {\n" +
                "var arr = [];\n" +
                "heap.forEachObject(function(o) {\n" +
                "  if (o.length > 255) {\n" +
                "    arr.push(o);\n" +
                "  }\n" +
                "}, 'int[]')\n" +
                "return arr.length;" +
                "})", "fn.js").build();
        Value fn = ctx.eval(jsFnSrc);

        Value res = null;
        for (int i = 1; i <= count; i++) {
            long now = System.currentTimeMillis();
            res = fn.execute(heap);
            long took = System.currentTimeMillis() - now;
            System.err.println("Found " + res.asInt() + " long int arrays");
            System.err.println("Round #" + i + " took " + took + " ms");
        }
        return res.asInt();
    }
}
