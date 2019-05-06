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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.HeapFactory;
import org.openide.filesystems.FileUtil;

public class MainTest {

    public MainTest() {
    }

    @Test
    public void loadHeapDump() throws Exception {
        File heapFile = sampleHprofFile(getClass());

        int count = Main.analyzeHeap(heapFile, 1);

        assertEquals("Four long arrays in the heap", 4, count);
    }

    private static File sampleHprofFile(Class<?> type) throws IOException {
        File heapFile = File.createTempFile("sample", ".hprof");
        try (
                InputStream is = type.getResourceAsStream("sample.hprof");
                OutputStream os = new FileOutputStream(heapFile)) {
            assertNotNull("Input stream", is);
            FileUtil.copy(is, os);
        }
        return heapFile;
    }


    @Test
    public void testMain() throws Exception {
        File heapFile = sampleHprofFile(getClass());
        Heap heap = HeapFactory.createHeap(heapFile);

        PrintStream prev = System.out;
        try {
            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            System.setOut(new PrintStream(bs));
            Main.queryHeap(heap, 1);
            assertEquals("Found 4 long int arrays\n", bs.toString(StandardCharsets.UTF_8));
        } finally {
            System.setOut(prev);
        }
    }

}
