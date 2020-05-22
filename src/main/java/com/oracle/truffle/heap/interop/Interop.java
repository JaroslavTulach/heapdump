package com.oracle.truffle.heap.interop;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.DirectCallNode;

/**
 * Provides utility methods for manipulating values transferred between languages.
 */
public interface Interop {

    /**
     * Create an executable truffle object wrapping the given call target. The language environment
     * is needed to wrap unknown Java objects into guest values.
     */
    static TruffleObject wrapCallTarget(CallTarget call) {
        return new WrappedCall(call);
    }

    /**
     * Create a read-only array-like truffle object from the given list of items. Items are not converted,
     * so make sure list contents are already valid interop values.
     */
    static TruffleObject wrapArray(Object[] items) {
        return new ReadOnlyArray(items);
    }

    /**
     * Used by {@link Interop#wrapCallTarget(CallTarget)}
     * to create executable truffle objects.
     */
    @ExportLibrary(InteropLibrary.class)
    final class WrappedCall implements TruffleObject {

        private final CallTarget callTarget;

        public WrappedCall(CallTarget callTarget) {
            this.callTarget = callTarget;
        }

        @ExportMessage
        static boolean isExecutable(WrappedCall receiver) {
            return true;
        }

        public static DirectCallNode makeCall(WrappedCall receiver) {
            return Truffle.getRuntime().createDirectCallNode(receiver.callTarget);
        }

        @ExportMessage
        static Object execute(WrappedCall receiver, Object[] arguments,
                              @Cached(value = "makeCall(receiver)", allowUncached = true) DirectCallNode callNode
        ) {
            return callNode.call(arguments);
        }

    }

    @ExportLibrary(InteropLibrary.class)
    final class Null implements TruffleObject {
        public static final Null INSTANCE = new Null();
        private Null() { }

        @ExportMessage
        public static boolean isNull(@SuppressWarnings("unused") Null receiver) {
            return true;
        }

        @Override
        public String toString() {
            return "null";
        }
    }

}
