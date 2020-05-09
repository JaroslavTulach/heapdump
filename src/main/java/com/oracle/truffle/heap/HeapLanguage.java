package com.oracle.truffle.heap;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Scope;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.*;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NonNull;
import org.netbeans.api.annotations.common.NullAllowed;
import org.netbeans.lib.profiler.heap.Heap;
import org.netbeans.lib.profiler.heap.HeapFactory;
import org.netbeans.lib.profiler.heap.JavaClass;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A Truffle Language object for natively manipulating heap dumps.
 *
 * <p>Heap Language treats data from heap dumps as "code" of a new language called
 * "heap". As such, a heap dump can be "executed", result of which is a native
 * heap object that can be (via truffle) shared with other languages.</p>
 *
 * <p>All supported functionality is described in the
 * <a href="http://cr.openjdk.java.net/~sundar/8022483/webrev.01/raw_files/new/src/share/classes/com/sun/tools/hat/resources/oqlhelp.html">OQL specification</a>.</p>
 *
 * <h2>Exported symbols</h2>
 *
 * <p>OQL describes several top level functions ({@code classof, length, map, ...}) which heap language exports in its
 * top level context as well:</p>
 *
 *<pre>
 *Value classOnHeap = context.getBindings("heap").getMember("classof").execute(valueOnHeap);
 *</pre>
 *
 * <p>If polyglot bindings are available, heap language also exports these symbols as polyglot with a {@code Heap.} prefix.
 * Such bindings can be then accessed from other guest languages (for example JavaScript):</p>
 *
 *<pre>
 *let classof = Polyglot.import("Heap.classof");
 *let classOnHeap = classof(valueOnHeap);
 *</pre>
 *
 * <h3>Script Expressions</h3>
 *
 * <p>OQL specification allows certain arguments to be evaluable string expressions. These are evaluated
 * in JavaScript by default (if available in the polyglot context). However, you can specify the language manually
 * using {@link HeapLanguage#setScriptLanguage(String)} (or disable expression arguments by setting
 * the value to {@code null}).</p>
 *
 * <p>{@link HeapLanguage#setScriptLanguage(String)} is exported the same way as other OQL global symbols. You can
 * therefore call it from other guest languages as well:</p>
 *
 *<pre>
 *let Heap_setScriptLanguage = Polyglot.import("Heap.setScriptLanguage");
 *Heap_setScriptLanguage("python");
 *</pre>
 *
 * <h3>Automatic binding</h3>
 *
 * <p>If you want to import all global symbols specified by OQL using one call, heap language also
 * exports an executable {@code bindGlobalSymbols(Bindings)} symbol. As an argument, the function accepts global
 * bindings of another language and it automatically adds all OQL specific functions into this global scope:</p>
 *
 * <pre>
 *context.getBindings("heap").getMember("bindGlobalSymbols").execute(context.getBindings("python"));    // Java
 *Polyglot.import("Heap_bindGlobalSymbols")(this);                                                      // JavaScript
 * </pre>
 *
 */
@TruffleLanguage.Registration(
        byteMimeTypes = "application/x-netbeans-profiler-hprof",
        name = "heap",
        id = "heap"
)
public class HeapLanguage extends TruffleLanguage<HeapLanguage.State> {

    /** A singleton null instance used by heap language. */
    public static final TruffleObject NULL = Interop.Primitives.Null.INSTANCE;

    /**
     * <p>Set the script language which should be used when evaluating string expression arguments.
     * Defaults to JavaScript (if available).</p>
     *
     * <p>Set to {@code null} to disable string expression arguments.</p>
     *
     * @throws IllegalArgumentException The specified language is not installed or polyglot eval is disabled.
     * @param language Canonical name of the scripting language.
     */
    static void setScriptLanguage(@NullAllowed String language) {
        State state = TruffleLanguage.getCurrentContext(HeapLanguage.class);
        if (language == null) {                             // disable scripting
            state.setScriptLanguage(null);
        } else if (state.hasPublicLanguage(language)) {     // language available - enable!
            state.setScriptLanguage(language);
        } else {
            throw new IllegalArgumentException(
                    String.format("Language '%s' is not installed or polyglot eval is disabled.", language)
            );
        }
    }

    /**
     * <p>Parse the given expression string using the currently specified script language.</p>
     *
     * @param expression String expression to parse.
     * @param argNames Optional argument names which appear in the expression.
     * @return An executable truffle object representing the parsed function.
     * @throws IllegalStateException Expression arguments are disabled.
     */
    static TruffleObject parseArgumentExpression(@NonNull String expression, String... argNames) {
        State state = TruffleLanguage.getCurrentContext(HeapLanguage.class);
        String scriptLanguage = state.getScriptLanguage();
        if (scriptLanguage == null) {
            throw new IllegalStateException(
                    String.format("Expression arguments are disabled. Cannot evaluate '%s'.", expression)
            );
        }
        Source source = Source.newBuilder(scriptLanguage, expression, "expression."+scriptLanguage).build();
        TruffleLanguage.Env env = state.getEnvironment();
        return Interop.wrapCallTarget(env.parsePublic(source, argNames), env);
    }

    /**
     * <p>Resolve callback/string expression arguments. If the argument is already executable, just return it.
     * If not, try to parse it as executable string expression.</p>
     */
    static TruffleObject unwrapCallbackArgument(Object[] arguments, int argIndex, String... argNames) throws UnsupportedTypeException {
        Object argument = arguments[argIndex];
        TruffleObject executable = Types.tryAsExecutable(argument);
        if (executable != null) return executable;
        String expression = Types.tryAsString(argument);
        if (expression != null) return parseArgumentExpression(expression, argNames);
        throw UnsupportedTypeException.create(arguments, "Expected executable or string expression as argument "+(argIndex+1)+", but found "+argument+".");
    }

    /**
     * <p>Resolve an object id argument. Object id can be either a numeric value or, it can be a string.</p>
     */
    static long unwrapObjectIdArgument(Object[] arguments, int argIndex) throws UnsupportedTypeException {
        Object argument = arguments[argIndex];
        Long id = Types.tryAsIntegralNumber(argument);
        if (id == null) {
            String stringId = Types.tryAsString(argument);
            try {   // throws number format exception even if stringId is null...
                id = Long.parseLong(stringId);
            } catch (NumberFormatException e) {
                // do nothing
            }
        }
        if (id != null) return id; else {
            throw UnsupportedTypeException.create(arguments, "Expected object id as argument "+(argIndex+1)+", but got "+argument+".");
        }
    }

    /**
     * <p>Resolve a {@link JavaClass} argument. This can be either a direct object, or a string reference into
     * the list of classes on the heap.</p>
     */
    // TODO: We should probably do something similar and accept object ids instead of instance objects wherever possible...
    static JavaClass unwrapJavaClassArgument(Object[] arguments, int argIndex, Heap heap) throws UnsupportedTypeException {
        Object argument = arguments[argIndex];
        if (argument instanceof ObjectJavaClass) {
            return ((ObjectJavaClass) argument).getJavaClass();
        } else {
            String className = Types.tryAsString(argument);
            if (className != null) {
                JavaClass found = HeapUtils.findClass(heap, className);
                if (found == null) throw new IllegalArgumentException("Unknown java class: "+className+".");
                return found;
            } else {
                throw UnsupportedTypeException.create(arguments, "Expected java class as argument "+(argIndex+1)+", but found "+argument+".");
            }
        }
    }

    /**
     * <p>Convert given Java object into a truffle guest value.</p>
     */
    public static Object asGuestValue(Object value) {
        return HeapLanguage.getCurrentContext(HeapLanguage.class).getEnvironment().asGuestValue(value);
    }

    @Override
    protected State createContext(Env env) {
        State state = new State(env);
        // Export global symbols into polyglot bindings, if allowed
        if (env.isPolyglotBindingsAccessAllowed()) {
            for (Map.Entry<String, TruffleObject> entry : Globals.INSTANCES.entrySet()) {
                env.exportSymbol("Heap." + entry.getKey(), entry.getValue());
            }
        }
        // Set default script language to JavaScript if JavaScript is available and if polyglot eval is allowed
        if (env.isPolyglotEvalAllowed() && env.getPublicLanguages().containsKey("js")) {
            state.setScriptLanguage("js");
        }
        return state;
    }

    @Override
    protected Iterable<Scope> findTopScopes(State context) {    // export globals into our bindings
        return Collections.singletonList(Scope.newBuilder("global", new Globals()).build());
    }

    @Override
    protected boolean isObjectOfLanguage(Object object) {
        // TODO
        return false;
    }

    @Override
    protected CallTarget parse(ParsingRequest request) {
        final URI location = request.getSource().getURI();
        final File file = new File(location);
        return Truffle.getRuntime().createCallTarget(new HeapLanguage.HeapNode(this, file));
    }

    /**
     * An executable root node of the "AST" of our Heap Language.
     *
     * Technically, there is no AST to execute, just a reference to the heap dump that needs to be
     * loaded and processed.
     */
    private static final class HeapNode extends RootNode {

        @NonNull
        private final File file;

        HeapNode(HeapLanguage language, @NonNull File file) {
            super(language);
            this.file = file;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            try {
                Heap heap = HeapFactory.createHeap(file);
                return new ObjectHeap(heap);
            } catch (IOException e) {
                throw new RuntimeException("Error while reading heap dump.", e);
            }
        }
    }

    /**
     * State of a {@link HeapLanguage} instance. Currently, it stores the name of the language used
     * when evaluating script expressions in the arguments of built-in functions.
     *
     * Note that the script language can be null, in which case, expression parameters are not allowed.
     *
     * It also stores references the {@link TruffleLanguage.Env} instance so that we can perform some
     * utility operations when we have access to language state.
     */
    public static final class State {

        @NonNull
        private final TruffleLanguage.Env environment;

        @NullAllowed
        private String scriptLanguage;

        public State(@NonNull Env environment) {
            this.environment = environment;
        }

        @CheckForNull
        public String getScriptLanguage() {
            return scriptLanguage;
        }

        public void setScriptLanguage(@NullAllowed String scriptLanguage) {
            this.scriptLanguage = scriptLanguage;
        }

        @NonNull
        public Env getEnvironment() {
            return environment;
        }

        /**
         * Return true if given language is installed and available.
         */
        public boolean hasPublicLanguage(@NonNull String language) {
            return this.environment.isPolyglotEvalAllowed() && this.environment.getPublicLanguages().containsKey(language);
        }

    }

    /**
     * A Truffle object holding references to all exported global symbols of the heap language.
     */
    @ExportLibrary(InteropLibrary.class)
    public static final class Globals implements TruffleObject {
        // This is an inner class mostly to allow "nice" access to global symbol names via
        // HeapLanguage.Globals.SYMBOL_NAME

        // OQL requested symbols:
        public static final String ALLOC_TRACE = "allocTrace";
        public static final String CLASS_OF = "classof";
        public static final String FOR_EACH_REFERRER = "forEachReferrer";
        public static final String FOR_EACH_REFEREE = "forEachReferee";
        public static final String IDENTICAL = "identical";
        public static final String OBJECT_ID = "objectid";
        public static final String REACHABLES = "reachables";
        public static final String REFERRERS = "referrers";
        public static final String REFEREES = "referees";
        public static final String REFERS = "refers";
        public static final String ROOT = "root";
        public static final String SIZE_OF = "sizeof";
        public static final String TO_HTML = "toHtml";

        public static final String CONCAT = "concat";
        public static final String CONTAINS = "contains";
        public static final String COUNT = "count";
        public static final String FILTER = "filter";
        public static final String LENGTH = "length";
        public static final String MAP = "map";
        public static final String MAX = "max";
        public static final String MIN = "min";
        public static final String SORT = "sort";
        public static final String SUM = "sum";
        public static final String TO_ARRAY = "toArray";
        public static final String UNIQUE = "unique";

        // Extra symbols provided by us:
        public static final String SET_SCRIPT_LANGUAGE = "setScriptLanguage";
        public static final String BIND_GLOBAL_SYMBOLS = "bindGlobalSymbols";

        private static final MemberDescriptor MEMBERS = MemberDescriptor.properties(
                ALLOC_TRACE, CLASS_OF, FOR_EACH_REFERRER, FOR_EACH_REFEREE, IDENTICAL, OBJECT_ID, REACHABLES, REFERRERS, REFEREES, REFERS, ROOT, SIZE_OF, TO_HTML,
                CONCAT, CONTAINS, COUNT, FILTER, LENGTH, MAP, MAX, MIN, SORT, SUM, TO_ARRAY, UNIQUE,
                SET_SCRIPT_LANGUAGE, BIND_GLOBAL_SYMBOLS
        );

        public static final Map<String, TruffleObject> INSTANCES = new HashMap<>();

        static {
            INSTANCES.put(ALLOC_TRACE, OQLGlobalSymbols.AllocTrace.INSTANCE);
            INSTANCES.put(CLASS_OF, OQLGlobalSymbols.ClassOf.INSTANCE);
            INSTANCES.put(FOR_EACH_REFERRER, OQLGlobalSymbols.ForEachReferrer.INSTANCE);
            INSTANCES.put(FOR_EACH_REFEREE, OQLGlobalSymbols.ForEachReferee.INSTANCE);
            INSTANCES.put(IDENTICAL, OQLGlobalSymbols.Identical.INSTANCE);
            INSTANCES.put(OBJECT_ID, OQLGlobalSymbols.ObjectId.INSTANCE);
            INSTANCES.put(REACHABLES, OQLGlobalSymbols.Reachables.INSTANCE);
            INSTANCES.put(REFERRERS, OQLGlobalSymbols.Referrers.INSTANCE);
            INSTANCES.put(REFEREES, OQLGlobalSymbols.Referees.INSTANCE);
            INSTANCES.put(REFERS, OQLGlobalSymbols.Refers.INSTANCE);
            INSTANCES.put(ROOT, OQLGlobalSymbols.Root.INSTANCE);
            INSTANCES.put(SIZE_OF, OQLGlobalSymbols.SizeOf.INSTANCE);
            INSTANCES.put(TO_HTML, OQLGlobalSymbols.ToHtml.INSTANCE);

            INSTANCES.put(CONCAT, OQLSequenceGlobals.Concat.INSTANCE);
            INSTANCES.put(CONTAINS, OQLSequenceGlobals.Contains.INSTANCE);
            INSTANCES.put(COUNT, OQLSequenceGlobals.Count.INSTANCE);
            INSTANCES.put(FILTER, OQLSequenceGlobals.Filter.INSTANCE);
            INSTANCES.put(LENGTH, OQLSequenceGlobals.Length.INSTANCE);
            INSTANCES.put(MAP, OQLSequenceGlobals.Map.INSTANCE);
            INSTANCES.put(MAX, OQLSequenceGlobals.Max.INSTANCE);
            INSTANCES.put(MIN, OQLSequenceGlobals.Min.INSTANCE);
            INSTANCES.put(SORT, OQLSequenceGlobals.Sort.INSTANCE);
            INSTANCES.put(SUM, OQLSequenceGlobals.Sum.INSTANCE);
            INSTANCES.put(TO_ARRAY, OQLSequenceGlobals.ToArray.INSTANCE);
            INSTANCES.put(UNIQUE, OQLSequenceGlobals.Unique.INSTANCE);

            INSTANCES.put(SET_SCRIPT_LANGUAGE, BuiltIns.SetScriptLanguage.INSTANCE);
            INSTANCES.put(BIND_GLOBAL_SYMBOLS, BuiltIns.BindGlobalSymbols.INSTANCE);
        }

        @ExportMessage
        static boolean hasMembers(@SuppressWarnings("unused") Globals receiver) {
            return true;
        }

        @ExportMessage
        static Object getMembers(@SuppressWarnings("unused") Globals receiver, @SuppressWarnings("unused") boolean includeInternal) {
            return MEMBERS;
        }

        @ExportMessage
        static boolean isMemberReadable(@SuppressWarnings("unused") Globals receiver, String member) {
            return MEMBERS.hasProperty(member);
        }

        @ExportMessage
        static Object readMember(@SuppressWarnings("unused") Globals receiver, String member) throws UnknownIdentifierException {
            TruffleObject instance = INSTANCES.get(member);
            if (instance != null) {
                return instance;
            } else {
                throw UnknownIdentifierException.create(member);
            }
        }

    }
}

