package com.oracle.truffle.heap.interop;

import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.heap.interop.MemberDescriptor;
import org.junit.Test;

import static org.junit.Assert.*;

public class MemberDescriptorTest {

    @Test(expected = InvalidArrayIndexException.class)
    public void invalidIndex() throws InvalidArrayIndexException {
        MemberDescriptor items = MemberDescriptor.properties("p1", "p2");
        MemberDescriptor.readArrayElement(items, 2);
    }

    @Test
    public void propertyDescriptors() throws InvalidArrayIndexException {
        MemberDescriptor items = MemberDescriptor.properties("prop1", "value", "my_name");
        // Basic element access
        assertTrue(items.contains("prop1"));
        assertTrue(items.contains("value"));
        assertTrue(items.contains("my_name"));
        assertFalse(items.contains("prop2"));
        assertTrue(items.hasProperty("value"));
        assertFalse(items.hasFunction("value"));

        // Interop library protocol
        assertTrue(MemberDescriptor.hasArrayElements(items));
        assertEquals(3, MemberDescriptor.getArraySize(items));
        assertTrue(MemberDescriptor.isArrayElementReadable(items, 1));
        assertFalse(MemberDescriptor.isArrayElementReadable(items, 3));
        assertEquals("value", MemberDescriptor.readArrayElement(items, 1));
    }

    @Test
    public void functionDescriptors() throws InvalidArrayIndexException {
        MemberDescriptor items = MemberDescriptor.functions("fun1", "my_fun", "next");
        // Basic element access
        assertTrue(items.contains("fun1"));
        assertTrue(items.contains("my_fun"));
        assertTrue(items.contains("next"));
        assertFalse(items.contains("fun2"));
        assertFalse(items.hasProperty("my_fun"));
        assertTrue(items.hasFunction("my_fun"));

        // Interop library protocol
        assertTrue(MemberDescriptor.hasArrayElements(items));
        assertEquals(3, MemberDescriptor.getArraySize(items));
        assertTrue(MemberDescriptor.isArrayElementReadable(items, 1));
        assertFalse(MemberDescriptor.isArrayElementReadable(items, 3));
        assertEquals("my_fun", MemberDescriptor.readArrayElement(items, 1));
    }

    @Test
    public void mixedDescriptors() throws InvalidArrayIndexException {
        MemberDescriptor items = MemberDescriptor.build(new String[] { "prop1", "prop2" }, new String[] { "fun1", "fun2" });
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
        assertTrue(MemberDescriptor.hasArrayElements(items));
        assertEquals(4, MemberDescriptor.getArraySize(items));
        assertTrue(MemberDescriptor.isArrayElementReadable(items, 1));
        assertTrue(MemberDescriptor.isArrayElementReadable(items, 3));
        assertEquals("prop2", MemberDescriptor.readArrayElement(items, 1));
        assertEquals("fun2", MemberDescriptor.readArrayElement(items, 3));
    }

}
