package com.oracle.truffle.gradle;

import org.gradle.api.JavaVersion;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * <p>Some basic utility methods used in the plugins.</p>
 */
class PluginUtils {

    /**
     * <p>Returns true if the current JVM is Graal.</p>
     */
    static boolean isGraalVM() {
        // We don't want to use vendor name because GraalVM vendor also releases non-graal JVMs
        // (JVMCI enabled OpenJDK8). For Graal based on JDK11, java.vendor.version is set. For older JDKs,
        // java.vm.name should contain GraalVM as well.
        return System.getProperty("java.vendor.version", "").contains("GraalVM") || System.getProperty("java.vm.name", "").contains("GraalVM");
    }

    /**
     * <p>Returns true if the current JVM supports JVMCI (Java Virtual Machine Compiler Interface).</p>
     */
    static boolean hasJVMCI() {
        // We assume a JVM supports CI if the version is at least 11 (first version where publicly available) or
        // if the `java.vm.version` contains jvmci (indicates custom builds or graal vm).
        return JavaVersion.current().compareTo(JavaVersion.VERSION_11) >= 0 || System.getProperty("java.vm.version", "").contains("jvmci");
    }

    /**
     * <p>A simple find-replace in a file (used to set-up jvm args in run scripts).</p>
     */
    static void replaceInFile(File f, String find, String replace) throws IOException {
        String content = new String(Files.readAllBytes(f.toPath()), Charset.defaultCharset());
        content = content.replace(find, replace);
        Files.write(f.toPath(), content.getBytes(Charset.defaultCharset()));
    }


    /**
     * <p>Append given strings at the end of a given (possibly null) iterable.</p>
     */
    static Iterable<String> appendIterable(@Nullable Iterable<String> original, String... items) {
        ArrayList<String> result = new ArrayList<>();
        if (original != null) {
            for (String i : original) {
                result.add(i);
            }
        }
        result.addAll(Arrays.asList(items));
        return result;
    }

}
