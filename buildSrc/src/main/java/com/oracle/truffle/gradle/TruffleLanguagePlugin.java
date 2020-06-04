package com.oracle.truffle.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.application.CreateStartScripts;
import org.gradle.process.JavaForkOptions;

import javax.annotation.Nonnull;
import java.io.File;

/**
 * <p>Truffle language plugin allows the project to be used as a custom truffle language. It automatically
 * includes the Graal compiler on non-Graal JVMs and it will configure Graal to load your custom language.</p>
 *
 * <p>This is mainly done by modifying all tasks implementing {@code JavaForkOptions} to configure
 * the {@code truffle.class.path.append} property, pointing to this project and all its dependencies.</p>
 *
 * <p>Furthermore, all start scripts and distributions are also modified to include this project and all
 * dependencies on the truffle class path.</p>
 *
 * <p>In the future, this plugin should also support building the Graal component for this language,
 * but this is not implemented yet.</p>
 *
 * <p>If you are not writing a custom language, just using one in your project, refer to the
 * {@code TrufflePlugin} instead.</p>
 */
public class TruffleLanguagePlugin implements Plugin<Project> {

    @Override
    public void apply(@Nonnull Project project) {
        project.getPluginManager().apply(GraalCompilerPlugin.class);
        if (project.getPluginManager().findPlugin("truffle") != null) { // If you are writing a language, no need to apply truffle.
            throw new RuntimeException("Cannot apply both truffle and truffle-language plugins on one project.");
        }

        // Set truffle classpath on all JavaForkOptions tasks.
        project.getTasks().all(task -> {
            if (PluginUtils.isGraalVM() && task instanceof JavaForkOptions) {  // Only set truffle classpath on Graal
                task.doFirst(it -> {
                    JavaForkOptions fork = (JavaForkOptions) it;
                    JavaPluginConvention javaPlugin = project.getConvention().findPlugin(JavaPluginConvention.class);
                    if (javaPlugin == null) return;
                    SourceSet mainSources = javaPlugin.getSourceSets().findByName("main");
                    if (mainSources == null) return;
                    String truffleClassPath = mainSources.getRuntimeClasspath().getAsPath();
                    fork.systemProperty("truffle.class.path.append", truffleClassPath);
                });
            }
        });

        // Include the whole /lib folder on truffle classpath when building distributions.
        project.getTasks().all(task -> {
            if (task instanceof CreateStartScripts) {
                task.doFirst(it -> {
                    CreateStartScripts scripts = (CreateStartScripts) it;
                    // Build the truffle classpath for the start script. Note that this is different from the
                    // runtime classpath used in Fork tasks, because here our project is also a jar and the path
                    // is relative to the APP_HOME folder.
                    StringBuilder classpath = new StringBuilder();
                    FileCollection scriptsClasspath = scripts.getClasspath();
                    if (scriptsClasspath != null) {
                        for (File f : scriptsClasspath.getFiles()) {
                            classpath.append("__APP_HOME__/lib/");
                            classpath.append(f.getName());
                            classpath.append(":");
                        }
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
