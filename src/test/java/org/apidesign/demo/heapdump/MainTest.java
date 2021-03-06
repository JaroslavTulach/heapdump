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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.HeapFactory;
import org.openide.filesystems.FileUtil;

public class MainTest {

    public MainTest() {
    }

    @Test
    public void testMain() throws Exception {
        File heapFile = File.createTempFile("sample", ".hprof");
        try (
            InputStream is = getClass().getResourceAsStream("sample.hprof");
            OutputStream os = new FileOutputStream(heapFile)
        ) {
            assertNotNull("Input stream", is);
            FileUtil.copy(is, os);
        }

        Heap heap = HeapFactory.createHeap(heapFile);
        int res = Main.queryHeap(heap, 1);
        assertEquals("Four large arrays", 4, res);
    }

}
