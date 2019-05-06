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
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RepeatingNode;
import com.oracle.truffle.api.nodes.RootNode;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Iterator;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.HeapFactory;
import org.netbeans.lib.profiler.heap.JavaClass;
import org.netbeans.lib.profiler.heap.PrimitiveArrayInstance;

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

@ExportLibrary(InteropLibrary.class)
final class HeapObject implements TruffleObject {
    final Heap object;

    HeapObject(Heap object) {
        this.object = object;
    }

    @ExportMessage
    static boolean hasMembers(HeapObject receiver) {
        return true;
    }

    @ExportMessage
    static boolean isMemberInvocable(HeapObject receiver, String member) {
        return "forEachObject".equals(member);
    }

    @ExportMessage
    static boolean getMembers(HeapObject receiver, boolean includeInternal) {
        return true;
    }

    @ExportMessage
    static class InvokeMember {
        @Specialization
        static Object invokeMember(
            HeapObject receiver, String member, Object[] arguments,
            @Cached(value = "createLoopNode()") CallTarget callFn
        ) throws UnknownIdentifierException {
            if (!"forEachObject".equals(member)) {
                throw UnknownIdentifierException.create(member);
            }
            TruffleObject fn = (TruffleObject) arguments[0];

            String type = (String) arguments[1];
            JavaClass classType = receiver.object.getJavaClassByName(type);
            Iterator it = classType.getInstancesIterator();

            callFn.call(fn, new Object[1024], new int[1], new int[1], it);

            return receiver;
        }
        
        static CallTarget getUncached() {
            return null;
        }
    
        static CallTarget createLoopNode() {
            final class LoopArray extends Node implements RepeatingNode {

                @Child
                private InteropLibrary callFn = InteropLibrary.getFactory().createDispatched(3);

                @Override
                public boolean executeRepeating(VirtualFrame frame) {
                    Object fn = frame.getArguments()[0];
                    Object[] arr = (Object[]) frame.getArguments()[1];
                    int[] len = (int[]) frame.getArguments()[2];
                    int[] at = (int[]) frame.getArguments()[3];
                    Iterator it = (Iterator) frame.getArguments()[4];

                    if (at[0] < len[0]) {
                        PrimitiveArrayObject wrapper = new PrimitiveArrayObject((PrimitiveArrayInstance) arr[at[0]++]);
                        try {
                            callFn.execute(fn, wrapper);
                        } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException ex) {
                            throw HeapLanguage.raise(RuntimeException.class, ex);
                        }
                        return true;
                    }
                    len[0] = readObjects(arr, it);
                    at[0] = 0;

                    return len[0] > at[0];
                }
            }

            final class LoopRoot extends RootNode {
                LoopRoot() {
                    super(null);
                }

                @Child
                private LoopNode loop = Truffle.getRuntime().createLoopNode(new LoopArray());

                @Override
                public Object execute(VirtualFrame frame) {
                    loop.executeLoop(frame);
                    return null;
                }
            }
            return Truffle.getRuntime().createCallTarget(new LoopRoot());
        }

        @CompilerDirectives.TruffleBoundary
        static int readObjects(Object[] arr, Iterator<?> it) {
            for (int i = 0; i < arr.length; i++) {
                if (!it.hasNext()) {
                    return i;
                }
                arr[i] = it.next();
            }
            return arr.length;
        }
    }
}

@ExportLibrary(InteropLibrary.class)
final class PrimitiveArrayObject implements TruffleObject {
    final PrimitiveArrayInstance array;

    PrimitiveArrayObject(PrimitiveArrayInstance array) {
        this.array = array;
    }

    @ExportMessage
    static boolean hasArrayElements(PrimitiveArrayObject receiver) {
        return true;
    }

    @ExportMessage
    static int getArraySize(PrimitiveArrayObject receiver) {
        return receiver.array.getLength();
    }

    @ExportMessage
    static Object readArrayElement(PrimitiveArrayObject receiver, long at) {
        return null;
    }

    @ExportMessage
    static boolean isArrayElementReadable(PrimitiveArrayObject receiver, long at) {
        return true;
    }
}

class Data {
}
