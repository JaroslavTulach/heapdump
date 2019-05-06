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

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.RootNode;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.HeapFactory;
import org.openide.util.Exceptions;

@TruffleLanguage.Registration(
    byteMimeTypes = "application/x-netbeans-profiler-hprof",
    name = "heap",
    id = "heap"
)
public class HeapLanguage extends TruffleLanguage<Data> {
    @Override
    protected Data createContext(Env env) {
        return new Data();
    }

    @Override
    protected boolean isObjectOfLanguage(Object object) {
        return false;
    }

    @Override
    protected CallTarget parse(ParsingRequest request) throws Exception {
        final URI at = request.getSource().getURI();
        File file = new File(at);
        return Truffle.getRuntime().createCallTarget(new HeapRoot(this, file));
    }


    static <E extends Exception> E raise(Class<E> type, Exception ex) throws E {
        throw (E) ex;
    }

}

final class HeapRoot extends RootNode {
    private final File file;

    HeapRoot(HeapLanguage language, File file) {
        super(language);
        this.file = file;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        Heap heap;
        try {
            heap = HeapFactory.createHeap(file);
        } catch (IOException ex) {
            throw HeapLanguage.raise(RuntimeException.class, ex);
        }
        return new HeapObject(heap);
    }
}

final class HeapObject implements TruffleObject {
    final Heap object;

    HeapObject(Heap object) {
        this.object = object;
    }
}

class Data {
}
