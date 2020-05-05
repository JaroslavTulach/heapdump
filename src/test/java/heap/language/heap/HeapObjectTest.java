package heap.language.heap;

import heap.language.HeapLanguageTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HeapObjectTest extends HeapLanguageTest {

    @Override
    protected void initJS() {
        // Add a utility method for counting items in an iterator
        runJS("function count(it) { var c = 0; while(it.hasNext()) { c += 1; it.next(); }; return c; }");
    }

    @Test
    public void findClass() {
        String clazzName = runJS("heap.findClass('java.io.FilterOutputStream').name").as(String.class);
        assertEquals("java.io.FilterOutputStream", clazzName);
    }

    @Test
    public void forEachClass() {
        assertEquals((Integer) 443, runJS("var c = 0; heap.forEachClass(function(v) { c += 1; }); c").as(Integer.class));
    }

    @Test
    public void classes() {
        assertEquals((Integer) 443, runJS("count(heap.classes())").as(Integer.class));
        assertEquals("sun.nio.cs.HistoricallyNamedCharset", runJS("heap.classes().next().name").as(String.class));
    }

    @Test
    public void findObject() {
        // The object is a File with a path of length 57.
        assertEquals((Integer) 57, runJS("heap.findObject(1684166976).path.value.length").as(Integer.class));
    }

    @Test
    public void forEachObject() {
        assertEquals((Integer) 6, runJS("var c = 0; heap.forEachObject(function(f) { c += 1; }, 'java.io.File'); c").as(Integer.class));
        assertEquals((Integer) 7, runJS("var c = 0; heap.forEachObject(function(f) { c += 1; }, 'java.io.OutputStream', true); c").as(Integer.class));
        assertEquals((Integer) 0, runJS("var c = 0; heap.forEachObject(function(f) { c += 1; }, 'java.io.OutputStream', false); c").as(Integer.class));
        assertEquals((Integer) 2208, runJS("var c = 0; heap.forEachObject(function(f) { c += 1; }); c").as(Integer.class));
    }

    @Test
    public void objects() {
        assertEquals((Integer) 6, runJS("count(heap.objects('java.io.File'))").as(Integer.class));
        assertEquals((Integer) 7, runJS("count(heap.objects('java.io.OutputStream', true))").as(Integer.class));
        assertEquals((Integer) 0, runJS("count(heap.objects('java.io.OutputStream', false))").as(Integer.class));
        // Test filter expressions
        assertEquals((Integer) 0, runJS("count(heap.objects('java.lang.String', false, 'false'))").as(Integer.class));
        assertEquals((Integer) 295, runJS("count(heap.objects('java.lang.String', false, 'true'))").as(Integer.class));
        assertEquals((Integer) 214, runJS("count(heap.objects('java.lang.String', false, 'it.value.length > 10'))").as(Integer.class));
        assertEquals((Integer) 8, runJS("count(heap.objects('java.lang.String', false, 'it.value.length > 100'))").as(Integer.class));
        // The same filters, but as callbacks
        assertEquals((Integer) 0, runJS("count(heap.objects('java.lang.String', false, function(s) { return false; }))").as(Integer.class));
        assertEquals((Integer) 295, runJS("count(heap.objects('java.lang.String', false, function(s) { return true; }))").as(Integer.class));
        assertEquals((Integer) 214, runJS("count(heap.objects('java.lang.String', false, function(s) { return s.value.length > 10; }))").as(Integer.class));
        assertEquals((Integer) 8, runJS("count(heap.objects('java.lang.String', false, function(s) { return s.value.length > 100; }))").as(Integer.class));
    }

    @Test
    public void finalizables() {
        // TODO: Something is not right - Snapshot implementation is not working and queue on Finalizer seems to be empty.
        //runJS("print(heap.findClass('java.lang.ref.Finalizer').statics)");
    }

}
