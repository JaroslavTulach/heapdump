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
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.io.ByteSequence;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.HeapFactory;
import org.netbeans.modules.profiler.oql.engine.api.OQLEngine;
import org.netbeans.modules.profiler.oql.engine.api.OQLException;

public class Main {
    public static void main(String... args) throws Exception {
        String fileName = args.length > 0 ? args[0] : "dump.hprof";
        File file = new File(fileName);
        if (!file.exists()) {
            File homeFile = new File(System.getProperty("user.home"), fileName);
            if (homeFile.exists()) {
                file = homeFile;
            } else {
                throw new IOException("Cannot find " + file + " specify as mvn exec:exec -Dheap=path_to_heap_dump");
            }
        }

        String language = (args.length > 1) ? args[1] : "js";

        System.err.println("Analyzing the heap with " + language);
        analyzeHeap(file, language, 20);
        System.setProperty("polyglot.js.nashorn-compat", "true");
        System.err.println("Loading " + file);
        Heap heap = HeapFactory.createHeap(file);
        System.err.println("Querying the heap");
        queryHeap(heap, 20);
    }

    static int queryHeap(Heap heap, int count) throws OQLException {
        int[] res = { -1 };
        final OQLEngine eng = new OQLEngine(heap);
        for (int i = 1; i <= count; i++) {
            long now = System.currentTimeMillis();
            eng.executeQuery("""
                var arr = [];
                heap.forEachObject(function(o) {
                    if (o.length > 255) {
                        arr.push(o);
                    }
                }, 'int[]')
                print('Found ' + arr.length + ' long int arrays');
                arr.length
                """, (arrLength) -> {
                    res[0] = (int) arrLength;
                    return true;
                }
            );
            long took = System.currentTimeMillis() - now;
            System.err.println("Round #" + i + " took " + took + " ms with Nashorn like wrapper");
        }
        return res[0];
    }

    static int analyzeHeap(File heapFile, String language, int count) throws IOException {
        Context ctx = Context.newBuilder().build();
        if (!ctx.getEngine().getLanguages().containsKey(language)) {
            System.err.println("No support for " + language + " skipping");
            return -1;
        }

        System.err.println("Parsing the " + heapFile);
        Source heapSrc = Source.newBuilder("demoheap", bytesOf(heapFile), heapFile.getName())
                .uri(heapFile.toURI())
                .mimeType("application/x-netbeans-profiler-hprof").build();
        Value heap = ctx.eval(heapSrc);

        String[] langCodeExt = createHugeArrayFn(language);

        final Source fnSrc = Source.newBuilder(langCodeExt[0], langCodeExt[1], langCodeExt[2]).build();
        Value fn = ctx.eval(fnSrc);

        Value res = null;
        for (int i = 1; i <= count; i++) {
            long now = System.currentTimeMillis();
            res = fn.execute(heap);
            long took = System.currentTimeMillis() - now;
            System.err.println("Found " + res.asInt() + " long int arrays");
            System.err.println("Round #" + i + " took " + took + " ms with " + language);
        }
        return res.asInt();
    }

    private static ByteSequence bytesOf(File heapFile) throws IOException {
        long length = heapFile.length();
        try (RandomAccessFile file = new RandomAccessFile(heapFile, "r")) {
            MappedByteBuffer out = file.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, length);
            return new ByteSequence() {
                @Override
                public int length() {
                    return (int) heapFile.length();
                }

                @Override
                public byte byteAt(int index) {
                    return out.get(index);
                }
            };
        }
    }

    static {
        System.setProperty("truffle.class.path.append", System.getProperty("java.class.path"));
    }

    private static String[] createHugeArrayFn(String language) {
        switch (language) {
            case "ruby":
                return new String[] {
                    "ruby", """
                    def hugeArrays(heap)
                      arr = []
                      heap.forEachObject(-> (o) { if o.size > 255 then arr.push(o); end }, 'int[]')
                      return arr.size
                    end
                    method(:hugeArrays)
                    """, "fn.js"
                };
            case "js":
                return new String[] {
                    "js", """
                    (function(heap) {
                        var arr = [];
                        heap.forEachObject(function(o) {
                          if (o.length > 255) {
                            arr.push(o);
                          }
                        }, 'int[]');
                        return arr.length;
                    })
                    """, "fn.js"
                };
            case "python":
                return new String[] {
                    "python", """
                    def hugeArrays(heap):
                      arr = []
                      heap.forEachObject(lambda o: arr.append(o) if len(o) > 255 else None, 'int[]')
                      return len(arr)

                    hugeArrays
                    """, "fn.py"
                };
            default:
                throw new IllegalStateException("Unknown language: " + language);
        }
    }
}
