package org.netbeans.modules.profiler.oql.engine.api.impl.truffle;

import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.modules.profiler.oql.engine.api.OQLEngine;

public class OQLQuery extends OQLEngine.OQLQuery {

    @NonNull
    private final String selectExpression;

    private final boolean isInstanceOf;

    @NullAllowed
    private final String className;

    @NullAllowed
    private final String instanceName;

    @NullAllowed
    private final String whereExpression;

    public OQLQuery(@NonNull String selectExpression, boolean isInstanceOf, @NullAllowed String className, @NullAllowed String instanceName, @NullAllowed String whereExpression) {
        this.selectExpression = selectExpression;
        this.isInstanceOf = isInstanceOf;
        this.className = className;
        this.instanceName = instanceName;
        this.whereExpression = whereExpression;
    }

    @NonNull
    public String getSelectExpression() {
        return selectExpression;
    }

    public boolean isInstanceOf() {
        return isInstanceOf;
    }

    @CheckForNull
    public String getClassName() {
        return className;
    }

    @CheckForNull
    public String getInstanceName() {
        return instanceName;
    }

    @CheckForNull
    public String getWhereExpression() {
        return whereExpression;
    }

    /** Build a JS representation of this query */
    public String buildJS() {
        if (className == null || instanceName == null) {
            // The query is only "select `JS_expression`" - in that case, just eval it
            return "{ visitor = wrapVisitor(visitor); let result = "+selectExpression+"; let isIterable = result != null && typeof result[Symbol.iterator] === 'function'; if (isIterable) { for (r in result) { if (visitor.visit(result[r])) { break }; }} else { visitor.visit(result); } }";
        } else {
            // The query is "select `JS_expression` from `class_name` `identifier`
            // visitor is
            String selectFunction = "function __select__("+instanceName+") { return "+selectExpression+" };";
            String iteratorConstruction = "let iterator = heap.objects('"+className+"', "+isInstanceOf+");";
            String whereFunction;
            String resultsIterator;
            if (whereExpression == null) {
                whereFunction = "";
                resultsIterator = "while (iterator.hasNext()) { let item = iterator.next(); if (visitor.visit(__select__(item))) { break; }; };";
            } else {
                whereFunction = "function __where__("+instanceName+") { return "+whereExpression+" };";
                resultsIterator = "while (iterator.hasNext()) { let item = iterator.next(); if(__where__(item)) { if (visitor.visit(__select__(item))) { break; } } };";
            }
            return "{ visitor = wrapVisitor(visitor); " + selectFunction + whereFunction + iteratorConstruction + resultsIterator + "}";
        }
    }

}
