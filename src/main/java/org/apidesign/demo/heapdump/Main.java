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

    static int queryHeap(Heap heap, int count) throws OQLException {
        int[] res = { -1 };
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
                "print('Found ' + arr.length + ' long int arrays');" +
                "arr.length"
                , (arrLength) -> {
                    res[0] = (int) arrLength;
                    return true;
                }
            );
            long took = System.currentTimeMillis() - now;
            System.err.println("Round #" + i + " took " + took + " ms");
        }
        return res[0];
    }

    static int analyzeHeap(File heapFile, int count) throws IOException {
        Context ctx = Context.newBuilder().build();
        System.err.println("Parsing the " + heapFile);
        Source heapSrc = Source.newBuilder("heap", bytesOf(heapFile), heapFile.getName())
                .uri(heapFile.toURI())
                .mimeType("application/x-netbeans-profiler-hprof").build();
        Value heap = ctx.eval(heapSrc);

        String[] langCodeExt = createHugeArrayFn();

        final Source fnSrc = Source.newBuilder(langCodeExt[0], langCodeExt[1], langCodeExt[2]).build();
        Value fn = ctx.eval(fnSrc);

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

    private static String[] createHugeArrayFn() {
        return new String[] {
            "ruby", ""
            + "def hugeArrays(heap)\n"
            + "  arr = []\n"
            + "  heap.forEachObject(-> (o) { if o.size > 255 then arr.push(o); end }, 'int[]')\n"
            + "  return arr.size\n"
            + "end\n"
            + "method(:hugeArrays)\n",
            "fn.js"
        };
/*
        return new String[] {
            "js",
            "(function(heap) {\n"
            + "var arr = [];\n"
            + "heap.forEachObject(function(o) {\n"
            + "  if (o.length > 255) {\n"
            + "    arr.push(o);\n"
            + "  }\n"
            + "}, 'int[]')\n"
            + "return arr.length;"
            + "})",
            "fn.js"
        };
/*
        return new String[] {
            "python",
            "def hugeArrays(heap):\n"
            + "  arr = []\n"
            + "  heap.forEachObject(lambda o: arr.append(o) if len(o) > 255 else None, 'int[]')\n"
            + "  return len(arr)\n"
            + "\n"
            + "hugeArrays\n"
            + "",
            "fn.py"
        };
*/
    }
}
