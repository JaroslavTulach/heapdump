package heap.language.util;

import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import org.junit.Test;
import static org.junit.Assert.*;

public class DescriptorsTest {

    @Test(expected = InvalidArrayIndexException.class)
    public void invalidIndex() throws InvalidArrayIndexException {
        Descriptors items = Descriptors.properties("p1", "p2");
        Descriptors.readArrayElement(items, 2);
    }

    @Test
    public void propertyDescriptors() throws InvalidArrayIndexException {
        Descriptors items = Descriptors.properties("prop1", "value", "my_name");
        // Basic element access
        assertTrue(items.contains("prop1"));
        assertTrue(items.contains("value"));
        assertTrue(items.contains("my_name"));
        assertFalse(items.contains("prop2"));
        assertTrue(items.hasProperty("value"));
        assertFalse(items.hasFunction("value"));

        // Interop library protocol
        assertTrue(Descriptors.hasArrayElements(items));
        assertEquals(3, Descriptors.getArraySize(items));
        assertTrue(Descriptors.isArrayElementReadable(items, 1));
        assertFalse(Descriptors.isArrayElementReadable(items, 3));
        assertEquals("value", Descriptors.readArrayElement(items, 1));
    }

    @Test
    public void functionDescriptors() throws InvalidArrayIndexException {
        Descriptors items = Descriptors.functions("fun1", "my_fun", "next");
        // Basic element access
        assertTrue(items.contains("fun1"));
        assertTrue(items.contains("my_fun"));
        assertTrue(items.contains("next"));
        assertFalse(items.contains("fun2"));
        assertFalse(items.hasProperty("my_fun"));
        assertTrue(items.hasFunction("my_fun"));

        // Interop library protocol
        assertTrue(Descriptors.hasArrayElements(items));
        assertEquals(3, Descriptors.getArraySize(items));
        assertTrue(Descriptors.isArrayElementReadable(items, 1));
        assertFalse(Descriptors.isArrayElementReadable(items, 3));
        assertEquals("my_fun", Descriptors.readArrayElement(items, 1));
    }

    @Test
    public void mixedDescriptors() throws InvalidArrayIndexException {
        Descriptors items = Descriptors.build(new String[] { "prop1", "prop2" }, new String[] { "fun1", "fun2" });
        // Basic element access
        assertTrue(items.contains("prop1"));
        assertTrue(items.contains("prop2"));
        assertTrue(items.contains("fun1"));
        assertTrue(items.contains("fun2"));
        assertFalse(items.contains("prop3"));
        assertTrue(items.hasFunction("fun1"));
        assertFalse(items.hasFunction("prop1"));
        assertTrue(items.hasProperty("prop1"));
        assertFalse(items.hasFunction("prop1"));

        // Interop library protocol
        assertTrue(Descriptors.hasArrayElements(items));
        assertEquals(4, Descriptors.getArraySize(items));
        assertTrue(Descriptors.isArrayElementReadable(items, 1));
        assertTrue(Descriptors.isArrayElementReadable(items, 3));
        assertEquals("prop2", Descriptors.readArrayElement(items, 1));
        assertEquals("fun2", Descriptors.readArrayElement(items, 3));
    }

}
