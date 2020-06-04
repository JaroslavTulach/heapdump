package com.oracle.truffle.gradle;

import org.gradle.api.Project;
import org.gradle.internal.extensibility.DefaultExtraPropertiesExtension;

import java.io.File;

/**
 * <p>Stores data which are used to configure Graal/Truffle plugins.</p>
 */
public class GraalExtension {

    public static final String DEFAULT_GRAAL_VERSION = "20.1.0";

    // Version string of graal that should be used.
    private String graalVersion;

    /**
     * <p>A directory where graal compiler jars are stored. Defaults to {@code buildDir/graalCompiler}.</p>
     */
    public File graalCompilerDir;

    /**
     * <p>Set the version string for the Graal version which should be used by the plugins.</p>
     */
    public void setGraalVersion(String version) {
        this.graalVersion = version;
    }

    /**
     * <p>Get the Graal version which should be used by the plugins, or default if not provided.</p>
     */
    public String getGraalVersion() {
        if (this.graalVersion == null) {
            System.err.println("WARNING: Graal version not set. Defaulting to "+DEFAULT_GRAAL_VERSION+".");
            System.err.println("Set graalVersion using: graal { graalVersion = 'version_string' } in the build.gradle file.");
            return DEFAULT_GRAAL_VERSION;
        } else {
            return this.graalVersion;
        }
    }

    /**
     * <p>Set default value of graalCompilerDir based on project configuration and try to load graalVersion
     * from default values.</p>
     */
    void initDefault(Project project) {
        // Set default path to graal-compiler folder.
        if (this.graalCompilerDir == null) {
            this.graalCompilerDir = new File(project.getBuildDir(), "graal-compiler");
        }
        // Try to load graalVersion from default extra properties.
        Object ext = project.getExtensions().findByName("ext");
        if (ext instanceof DefaultExtraPropertiesExtension) {
            DefaultExtraPropertiesExtension props = (DefaultExtraPropertiesExtension) ext;
            Object versionCandidate = props.find("graalVersion");
            if (versionCandidate instanceof String) {
                this.graalVersion = (String) versionCandidate;
            }
        }
    }

}
