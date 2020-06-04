package com.oracle.truffle.heap.interop.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.heap.HeapLanguage;
import com.oracle.truffle.heap.interop.Args;
import com.oracle.truffle.heap.interop.Errors;
import com.oracle.truffle.heap.interop.Types;

/**
 * Unwrap a callback argument that can be either an executable truffle object, or a string expression.
 */
@GenerateUncached
@ImportStatic(Args.class)
public abstract class ArgCallback extends Node {
    public abstract TruffleObject execute(Object[] arguments, int index, String[] argNames) throws UnsupportedTypeException;

    static boolean isExecutable(Object[] arguments, int index, InteropLibrary interop) {
        assert index < arguments.length;
        return arguments[index] instanceof TruffleObject && interop.isExecutable(arguments[index]);
    }

    static boolean isString(Object[] arguments, int index, InteropLibrary interop) {
        assert index < arguments.length;
        return arguments[index] instanceof String || interop.isString(arguments[index]);
    }

    static String expression(Object[] arguments, int index, InteropLibrary interop) {
        return Types.asString(arguments[index], interop);
    }

    static TruffleObject parseExpression(String expression, String[] argNames) {
        return HeapLanguage.parseArgumentExpression(expression, argNames);
    }

    @Specialization(guards = "isMissing(arguments, index)") @SuppressWarnings("unused")
    static TruffleObject doMissing(Object[] arguments, int index, String[] argNames) {
        return null;
    }

    @Specialization(guards = { "!isMissing(arguments, index)", "isExecutable(arguments, index, interop)" }) @SuppressWarnings("unused")
    static TruffleObject doExecutable(Object[] arguments, int index, String[] argNames,
                                      @CachedLibrary(limit = "3") InteropLibrary interop
    ) {
        return (TruffleObject) arguments[index];
    }

    @Specialization(guards = { "!isMissing(arguments, index)", "isString(arguments, index, interop)", "expression.equals(expression(arguments, index, interop))" })
    static TruffleObject doExpressionCached(Object[] arguments, int index, String[] argNames,
                                            @CachedLibrary(limit = "3") InteropLibrary interop,
                                            @Cached("expression(arguments, index, interop)") String expression,
                                            @Cached("parseExpression(expression, argNames)") TruffleObject callback
    ) {
        return callback;
    }

    @Specialization(replaces = "doExpressionCached")
    static TruffleObject doGeneric(Object[] arguments, int index, String[] argNames,
                                   @CachedLibrary(limit = "3") InteropLibrary interop
    ) throws UnsupportedTypeException {
        Object arg = (index >= arguments.length) ? null : arguments[index];
        if (arg == null) {
            return null;
        } else if (arg instanceof String) {
            return parseExpression((String) arg, argNames);
        } else if (interop.isExecutable(arg)) {
            return (TruffleObject) arg;
        } else if (interop.isString(arg)) {
            return parseExpression(Types.asString(arg, interop), argNames);
        } else if (interop.isNull(arg)) {
            return null;
        } else {
            return Errors.unsupportedType(arguments, "Expected executable/string expression as argument %d, but found %s.", index+1, arg);
        }
    }

}
