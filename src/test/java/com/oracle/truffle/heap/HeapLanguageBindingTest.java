package com.oracle.truffle.heap;

import com.oracle.truffle.heap.HeapLanguage;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test that global symbols of heap language really initialize as they should.
 */
public class HeapLanguageBindingTest {

    @Test
    public void testBindingsPermissive() {
        Context ctx = Context.newBuilder("heap", "js").allowAllAccess(true).build();
        ctx.initialize("heap");
        ctx.initialize("js");

        Value bindings = ctx.getBindings("heap");
        Value polyglot = ctx.getPolyglotBindings();
        Value js = ctx.getBindings("js");

        assertTrue(bindings.hasMember(HeapLanguage.Globals.CLASS_OF));
        assertTrue(polyglot.hasMember("Heap."+HeapLanguage.Globals.CLASS_OF));

        assertTrue(bindings.getMember(HeapLanguage.Globals.SET_SCRIPT_LANGUAGE).canExecute());
        assertTrue(bindings.getMember(HeapLanguage.Globals.BIND_GLOBAL_SYMBOLS).canExecute());

        // Disable scripting
        bindings.getMember(HeapLanguage.Globals.SET_SCRIPT_LANGUAGE).execute((Object) null);
        // Enable scripting in js
        bindings.getMember(HeapLanguage.Globals.SET_SCRIPT_LANGUAGE).execute("js");

        // Add bindings to JS via polyglot bridge
        assertFalse(js.hasMember(HeapLanguage.Globals.CLASS_OF));
        ctx.eval("js", "Polyglot.import('Heap.bindGlobalSymbols')(this);");
        assertTrue(js.hasMember(HeapLanguage.Globals.CLASS_OF));
    }

    @Test(expected = PolyglotException.class)
    public void testUnknownScriptLanguage() {
        Context ctx = Context.newBuilder("heap", "js").allowAllAccess(true).build();
        ctx.initialize("heap");
        ctx.initialize("js");

        ctx.getBindings("heap").getMember(HeapLanguage.Globals.SET_SCRIPT_LANGUAGE).execute("python");
    }

    @Test(expected = PolyglotException.class)
    public void testJSNotAvailable() {
        Context ctx = Context.newBuilder("heap").allowAllAccess(true).build();
        ctx.initialize("heap");

        ctx.getBindings("heap").getMember(HeapLanguage.Globals.SET_SCRIPT_LANGUAGE).execute("js");
    }

    @Test(expected = PolyglotException.class)
    public void testPolyglotDisabled() {
        Context ctx = Context.newBuilder("heap", "js")
                .allowPolyglotAccess(PolyglotAccess.NONE)
                .build();

        assertTrue(ctx.getBindings("heap").hasMember(HeapLanguage.Globals.CLASS_OF));
        assertFalse(ctx.getPolyglotBindings().hasMember("Heap."+HeapLanguage.Globals.CLASS_OF));

        // Should fail because scripting is disabled
        ctx.getBindings("heap").getMember(HeapLanguage.Globals.SET_SCRIPT_LANGUAGE).execute("js");
    }

    @Test(expected = PolyglotException.class)
    public void testSetScriptLanguageIllegalArgs1() {
        Context ctx = Context.newBuilder("heap", "js").allowAllAccess(true).build();
        ctx.initialize("heap");

        ctx.eval("js", "Polyglot.import('Heap.setScriptLanguage')(3)"); // invalid primitive value
    }

    @Test(expected = PolyglotException.class)
    public void testSetScriptLanguageIllegalArgs2() {
        Context ctx = Context.newBuilder("heap", "js").allowAllAccess(true).build();
        ctx.initialize("heap");

        ctx.eval("js", "Polyglot.import('Heap.setScriptLanguage')('js', 'python')");    // invalid number of arguments
    }

    @Test(expected = PolyglotException.class)
    public void testSetScriptLanguageIllegalArgs3() {
        Context ctx = Context.newBuilder("heap", "js").allowAllAccess(true).build();
        ctx.initialize("heap");

        ctx.eval("js", "Polyglot.import('Heap.setScriptLanguage')(Polyglot.import('Heap.classof'))");   // non-trivial object which is not string
    }

    @Test(expected = PolyglotException.class)
    public void testBindGlobalSymbolsIllegalArgs1() {
        Context ctx = Context.newBuilder("heap", "js").allowAllAccess(true).build();
        ctx.initialize("heap");

        ctx.eval("js", "Polyglot.import('Heap.bindGlobalSymbols')(null)");  // missing bindings
    }

    @Test(expected = PolyglotException.class)
    public void testBindGlobalSymbolsIllegalArgs2() {
        Context ctx = Context.newBuilder("heap", "js").allowAllAccess(true).build();
        ctx.initialize("heap");

        ctx.eval("js", "Polyglot.import('Heap.bindGlobalSymbols')()");  // invalid number of arguments
    }

}
