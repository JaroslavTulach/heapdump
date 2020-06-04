package com.oracle.truffle.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.application.CreateStartScripts;
import org.gradle.process.JavaForkOptions;

import javax.annotation.Nonnull;
import java.io.File;

/**
 * <p>Truffle plugin allows you to specify Truffle languages as dependencies of your project. These dependencies
 * are then properly configured when running or distributing the project. Additionally, Graal compiler will
 * be included for usage on non-Graal JVMs.</p>
 *
 * <p>To declare a language dependency, simply specify {@code truffleLanguage "my.awesome.package:language:VERSION"}
 * in the dependency block of your {@code build.gradle} file.</p>
 */
public class TrufflePlugin implements Plugin<Project> {

    @Override
    public void apply(@Nonnull Project project) {
        project.getPluginManager().apply(GraalCompilerPlugin.class);
        if (project.getPluginManager().findPlugin("truffle-language") != null) {
            throw new RuntimeException("Cannot apply both truffle and truffle-language plugins on one project.");
        }

        // Create a truffleLanguage configuration which will hold language dependencies for the truffle classpath.
        Configuration truffle = project.getConfigurations().create("truffleLanguage")
                .setVisible(true)
                .setDescription("Truffle languages used by this project.");

        // Actually, runtime configuration also inherits everything in truffle, because on non-Graal JVMs,
        // there is no truffle classpath.
        project.getConfigurations().getByName("runtime").extendsFrom(truffle);

        // Set truffle classpath JavaForkOptions tasks
        project.getTasks().all(task -> {
            if (PluginUtils.isGraalVM() && task instanceof JavaForkOptions) {  // Only set truffle classpath on Graal
                task.doFirst(it -> {
                    JavaForkOptions fork = (JavaForkOptions) it;
                    fork.systemProperty("truffle.class.path.append", truffle.getAsPath());
                });
            }
        });

        // Truffle language files should be already part of distribution (because runtime depends on truffle),
        // but we also have to make sure the start scripts include them in the truffle classpath (avoiding
        // other dependencies).
        project.getTasks().all(task -> {
            if (task instanceof CreateStartScripts) {
                task.doFirst(it -> {
                    CreateStartScripts scripts = (CreateStartScripts) it;
                    // Build the truffle classpath for the start script. Note that this is different from the
                    // runtime classpath used in Fork tasks, because here the path is relative to the APP_HOME folder.
                    StringBuilder classpath = new StringBuilder();
                    for (File f : truffle.getFiles()) {
                        classpath.append("__APP_HOME__/lib/");
                        classpath.append(f.getName());
                        classpath.append(":");
                    }
                    scripts.setDefaultJvmOpts(PluginUtils.appendIterable(
                            scripts.getDefaultJvmOpts(),
                            "-Dtruffle.class.path.append="+classpath.toString()
                    ));
                });
            }
        });

    }

}
