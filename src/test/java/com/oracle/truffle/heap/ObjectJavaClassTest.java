package com.oracle.truffle.heap;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ObjectJavaClassTest extends HeapLanguageTest {

    @Before
    public void initVars() {
        runJS("var Object = heap.findClass('java.lang.Object');");
        runJS("var File = heap.findClass('java.io.File');");
        runJS("var PrintStream = heap.findClass('java.io.PrintStream');");
        runJS("var OutputStream = heap.findClass('java.io.OutputStream');");
        runJS("var FilterOutputStream = heap.findClass('java.io.FilterOutputStream');");
    }

    @Test
    public void name() {
        assertEquals("java.lang.Object", runJS("Object.name").as(String.class));
        assertEquals("java.io.File", runJS("File.name").as(String.class));
    }

    @Test
    public void superclass() {
        assertFalse(runJS("Object.name").isNull());
        assertEquals("java.io.OutputStream", runJS("FilterOutputStream.superclass.name").as(String.class));
    }

    @Test
    public void statics() {
        // Extract as much information about statics fields of a java.io.File as possible.

        assertTrue(runJS("File.statics.tmpdir").isNull());
        assertTrue(runJS("File.statics['<classLoader>']").isNull());
        assertEquals(Boolean.TRUE, runJS("File.statics.$assertionsDisabled").as(Boolean.class));
        assertEquals(Integer.valueOf(-1), runJS("File.statics.counter").as(Integer.class));
        assertEquals(Long.valueOf( 301077366599181567L), runJS("File.statics.serialVersionUID").as(Long.class));
        assertEquals(Character.valueOf(':'), runJS("File.statics.pathSeparatorChar").as(Character.class));
        assertEquals(Character.valueOf('/'), runJS("File.statics.separatorChar").as(Character.class));

        assertEquals(Integer.valueOf(1), runJS("File.statics.pathSeparator.value.length").as(Integer.class));
        assertEquals(":", runJS("File.statics.pathSeparator.value[0]").as(String.class));

        assertEquals(Integer.valueOf(1), runJS("File.statics.separator.value.length").as(Integer.class));
        assertEquals("/", runJS("File.statics.separator.value[0]").as(String.class));

        assertEquals("java.lang.Object", runJS("classof(File.statics.tmpFileLock).name").as(String.class));
        assertEquals("java.io.UnixFileSystem", runJS("classof(File.statics.fs).name").as(String.class));
    }

    @Test
    public void fields() {
        assertEquals(Integer.valueOf(2), runJS("File.fields.length").as(Integer.class));
        assertEquals("prefixLength", runJS("File.fields[0].name").as(String.class));
        assertEquals("int", runJS("File.fields[0].signature").as(String.class));
        assertEquals("path", runJS("File.fields[1].name").as(String.class));
        assertEquals("object", runJS("File.fields[1].signature").as(String.class));
    }

    @Test
    public void loader() {
        // TODO: Find some object with a class loader
        assertTrue(runJS("File.loader").isNull());
    }

    @Test
    public void isSubclassOf() {
        assertTrue(runJS("File.isSubclassOf(Object)").as(Boolean.class));
        assertTrue(runJS("PrintStream.isSubclassOf(OutputStream)").as(Boolean.class));
        assertFalse(runJS("File.isSubclassOf(OutputStream)").as(Boolean.class));
    }

    @Test
    public void isSuperclassOf() {
        assertTrue(runJS("OutputStream.isSuperclassOf(FilterOutputStream)").as(Boolean.class));
        assertTrue(runJS("Object.isSuperclassOf(OutputStream)").as(Boolean.class));
        assertFalse(runJS("File.isSuperclassOf(OutputStream)").as(Boolean.class));
    }

    @Test
    public void subclasses() {
        assertEquals("java.io.FilterOutputStream", runJS("OutputStream.subclasses()[0].name").as(String.class));
        assertEquals("java.io.ByteArrayOutputStream", runJS("OutputStream.subclasses()[1].name").as(String.class));
        assertEquals("java.io.BufferedOutputStream", runJS("OutputStream.subclasses()[2].name").as(String.class));
        assertEquals("java.io.FileOutputStream", runJS("OutputStream.subclasses()[3].name").as(String.class));
        assertEquals("java.io.PrintStream", runJS("OutputStream.subclasses()[4].name").as(String.class));
    }

    @Test
    public void superclasses() {
        assertEquals("java.io.FilterOutputStream", runJS("PrintStream.superclasses()[0].name").as(String.class));
        assertEquals("java.io.OutputStream", runJS("PrintStream.superclasses()[1].name").as(String.class));
        assertEquals("java.lang.Object", runJS("PrintStream.superclasses()[2].name").as(String.class));
    }

}
