package heap.language;

import heap.language.util.HeapLanguageUtils;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.netbeans.modules.profiler.oql.engine.api.OQLEngine;
import org.netbeans.modules.profiler.oql.engine.api.OQLException;

import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;

public class OQLEngineImpl {

    private final Context ctx;

    public OQLEngineImpl(File heapFile) throws IOException {
        this.ctx = Context.newBuilder().allowAllAccess(true).allowPolyglotAccess(PolyglotAccess.ALL).build();
        ctx.initialize("heap");
        ctx.initialize("js");

        ctx.getBindings("heap").getMember(HeapLanguage.Globals.SET_SCRIPT_LANGUAGE).execute("js");
        ctx.eval("js", "Polyglot.import('Heap.bindGlobalSymbols')(this)");
        // Should be the same as
        ctx.getBindings("heap").getMember(HeapLanguage.Globals.BIND_GLOBAL_SYMBOLS).execute(ctx.getBindings("js"));

        Source heapSrc = Source.newBuilder("heap", HeapLanguageUtils.bytesOf(heapFile), heapFile.getName())
                .uri(heapFile.toURI())
                .mimeType("application/x-netbeans-profiler-hprof").build();
        Value heap = this.ctx.eval(heapSrc);
        ctx.getBindings("js").putMember("heap", heap);
    }

    public void executeJsQuery(String javascript) throws IOException {
        Source querySrc = Source.newBuilder("js", javascript, "fn.js").build();
        ctx.eval(querySrc);
    }

    public void executeQuery(String query, OQLEngine.ObjectVisitor visitor) throws OQLException, IOException {
        OQLQuery parsed = parseQuery(query);
        if (parsed == null) {
            executeJsQuery(query);
            return;
        }
        visitor = (visitor == null) ? OQLEngine.ObjectVisitor.DEFAULT : visitor;
        ctx.getBindings("js").putMember("visitor", visitor);
        System.out.println("Query:"+parsed.buildJS());
        Source querySrc = Source.newBuilder("js", parsed.buildJS(), "fn.js").build();
        Value result = ctx.eval(querySrc);
        if (result.hasMember("done") && result.canInvokeMember("next")) {   // result is an iterator!
            while (!result.getMember("done").as(Boolean.class)) {
                if (visitor.visit(HeapLanguageUtils.truffleToHeap(result.invokeMember("next")))) {
                    return;
                }
            }
        } else if (result.hasArrayElements()) {    // result is an array
            int length = (int) result.getArraySize();
            for (int i=0; i < length; i++) {
                if (visitor.visit(HeapLanguageUtils.truffleToHeap(result.getArrayElement(i)))) {
                    return;
                }
            }
        } else if (parsed.getClassName() == null) { // result is just an object - but we return it only if this effectively "pureJS" query
            // TODO: This needs to be handled better - but how?
            if (visitor.visit(HeapLanguageUtils.truffleToHeap(result))) {
                return;
            };
        }
    }

    private OQLQuery parseQuery(String query) throws OQLException {
        StringTokenizer st = new StringTokenizer(query);
        if (st.hasMoreTokens()) {
            String first = st.nextToken();
            if (!first.equals("select")) { // NOI18N
                // Query does not start with 'select' keyword.
                // Just treat it as plain JavaScript and eval it.
                return null;
            }
        } else {
            throw new OQLException("TODO");
        }

        StringBuilder selectExpr = new StringBuilder(); // NOI18N
        boolean seenFrom = false;
        while (st.hasMoreTokens()) {
            String tok = st.nextToken();
            if (tok.equals("from")) { // NOI18N
                seenFrom = true;
                break;
            }
            selectExpr.append(" ").append(tok); // NOI18N
        }

        if (selectExpr.length() == 0) { // NOI18N
            throw new OQLException("TODO");
        }

        String className = null;
        boolean isInstanceOf = false;
        StringBuilder whereExpr = null;
        String identifier = null;

        if (seenFrom) {
            if (st.hasMoreTokens()) {
                String tmp = st.nextToken();
                if (tmp.equals("instanceof")) { // NOI18N
                    isInstanceOf = true;
                    if (!st.hasMoreTokens()) {
                        throw new OQLException("TODO");
                    }
                    className = st.nextToken();
                } else {
                    className = tmp;
                }
            } else {
                throw new OQLException("TODO");
            }

            if (st.hasMoreTokens()) {
                identifier = st.nextToken();
                if (identifier.equals("where")) { // NOI18N
                    throw new OQLException("TODO");
                }
                if (st.hasMoreTokens()) {
                    String tmp = st.nextToken();
                    if (!tmp.equals("where")) { // NOI18N
                        throw new OQLException("TODO");
                    }

                    whereExpr = new StringBuilder();  // NOI18N
                    while (st.hasMoreTokens()) {
                        whereExpr.append(" ").append(st.nextToken()); // NOI18N
                    }
                    if (whereExpr.length() == 0) { // NOI18N
                        throw new OQLException("TODO");
                    }
                }
            } else {
                throw new OQLException("TODO");
            }
        }
        return new OQLQuery(selectExpr.toString(), isInstanceOf, className, identifier, whereExpr != null ? whereExpr.toString() : null);
    }

}

