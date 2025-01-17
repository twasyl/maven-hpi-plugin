package org.jenkinsci.maven.plugins.hpi;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Places test-dependency plugins into somewhere the test harness can pick up.
 *
 * <p>
 * See {@code TestPluginManager.loadBundledPlugins()} where the test harness uses it.
 *
 * @author Kohsuke Kawaguchi
 */
@Mojo(name="resolve-test-dependencies", requiresDependencyResolution = ResolutionScope.TEST)
public class TestDependencyMojo extends AbstractHpiMojo {
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        File testDir = new File(project.getBuild().getTestOutputDirectory(), "test-dependencies");
        try {
            Files.createDirectories(testDir.toPath());
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to create directories for '" + testDir + "'", e);
        }

        try (FileOutputStream fos = new FileOutputStream(new File(testDir, "index")); Writer w = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            for (MavenArtifact a : getProjectArtfacts()) {
                if (!a.isPluginBestEffort(getLog()))
                    continue;

                String artifactId = a.getActualArtifactId();
                if (artifactId == null) {
                    getLog().debug("Skipping " + artifactId + " with classifier " + a.getClassifier());
                    continue;
                }

                getLog().debug("Copying " + artifactId + " as a test dependency");
                File dst = new File(testDir, artifactId + ".hpi");
                FileUtils.copyFile(a.getHpi().getFile(),dst);
                w.write(artifactId + "\n");
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to copy dependency plugins",e);
        }
    }
}
