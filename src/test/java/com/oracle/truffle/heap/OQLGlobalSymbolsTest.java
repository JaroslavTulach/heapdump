package com.oracle.truffle.heap;

import com.oracle.truffle.heap.HeapLanguageTest;
import org.graalvm.polyglot.PolyglotException;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test the functionality of the global functions built-into OQL.
 */
public class OQLGlobalSymbolsTest extends HeapLanguageTest {

    @Test
    public void testClassOf() {
        assertEquals("java.io.File", runJS("classof(heap.findObject(1684166976)).name").as(String.class));
    }

    @Test(expected = PolyglotException.class)
    public void testClassOfInvalidArgs1() {
        runJS("classof(heap.findClass('java.io.File'))");
    }

    @Test(expected = PolyglotException.class)
    public void testClassOfInvalidArgs2() {
        runJS("classof(heap.findObject(1684166976), heap.findObject(1684166976))");
    }

    @Test
    public void testLength() {
        assertEquals((Integer) 3, runJS("length([0,1,2])").as(Integer.class));
        assertEquals((Integer) 2, runJS("length({a: 'a', b: 'b'})").as(Integer.class));
    }

    @Test
    public void testMap() {
        assertEquals((Integer) 4, runJS("map([0,1,2], function(it) { return it * 2; })[2]").as(Integer.class));
        assertEquals((Integer) 2, runJS("map([0,1,2], 'it * 2')[1]").as(Integer.class));
    }

    @Test
    public void testToHtml() {
        assertEquals("null", runJS("toHtml(null)").as(String.class));
        assertEquals("[0, 1, 2]", runJS("toHtml([0,1,2])").as(String.class));
        assertEquals("{a: valA, b: valB}", runJS("toHtml({ a: 'valA', b: 'valB'})").as(String.class));
        assertEquals((Character) ':', runJS("toHtml(':')").as(Character.class));
    }

    @Test
    public void testConcat() {
        assertEquals("hello", runJS("concat({ a: 1, b: 2 }, ['hello', { foo: true }])[2]").as(String.class));
    }

    @Test
    public void testContains() {
        assertTrue(runJS("contains([0,1,2], 'it == 1')").as(Boolean.class));
        assertFalse(runJS("contains({ a: 'foo', b: 'goo' }, function(it, i) { return it == i; })").as(Boolean.class));
    }

    @Test
    public void testCount() {
        assertEquals((Integer) 3, runJS("count([1,2,3])").as(Integer.class));
        assertEquals((Integer) 2, runJS("count({ valA: 'foo', valB: 'foo2', extra: 4 }, \"index.startsWith('val')\")").as(Integer.class));
        assertEquals((Integer) 3, runJS("count([3,2,4,1,5,0], function(it) { return it > 2; })").as(Integer.class));
    }

    @Test
    public void testFilter() {
        assertEquals((Integer) 4, runJS("filter([0,1,4,2], 'it > 1')[0]").as(Integer.class));
        assertEquals("value", runJS("filter({ a: 'a', b: 'b', c: 'value', d: 'test' }, function(it) { return it.length > 2; })[0]").as(String.class));
    }

    @Test
    public void testMax() {
        assertEquals((Integer) 4, runJS("max([0,3,1,4,-2,2])").as(Integer.class));
        assertEquals("you", runJS("max(['hello', 'there', 'you', 'foo', 'value'], 'lhs.length < rhs.length')").as(String.class));
    }

}
