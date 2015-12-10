/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.test.itests.platform;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static ddf.common.test.WaitCondition.expect;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import ddf.common.test.BeforeExam;
import ddf.common.test.KarafConsole;
import ddf.test.itests.AbstractIntegrationTest;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class TestConfigStatusCommand extends AbstractIntegrationTest {

    private static final String PLATFORM_CONFIG_STATUS_COMMAND = "platform:config-status";

    private static final String SUCCESSFUL_IMPORT_MESSAGE = "All config files imported successfully.";

    private static final String FAILED_IMPORT_MESSAGE = "Failed to import file [%s]. ";

    private static final String INVALID_CONFIG_FILE_1 = "ddf.test.itests.platform.TestPlatform.invalid.config";

    private static final String INVALID_CONFIG_FILE_2 = "ddf.test.itests.platform.TestPlatform.startup.invalid.config";

    private static final String VALID_CONFIG_FILE_1 = "ddf.test.itests.platform.TestPlatform.startup.config";

    private static KarafConsole console;

    @BeforeExam
    public void beforeExam() throws Exception {
        basePort = getBasePort();
        getAdminConfig().setLogLevels();
        getServiceManager().waitForAllBundles();
        console = new KarafConsole(bundleCtx);
    }

    @After
    public void cleanup() throws Exception {
        FileUtils.cleanDirectory(getPathToProcessedDirectory().toFile());
        FileUtils.cleanDirectory(getPathToFailedDirectory().toFile());
    }

    @Test
    public void testConfigStatusImportSuccessful() throws Exception {
        addConfigurationFileAndWaitForSuccessfulProcessing(VALID_CONFIG_FILE_1,
                getResourceAsStream(VALID_CONFIG_FILE_1));
        String output = console.runCommand(PLATFORM_CONFIG_STATUS_COMMAND);
        assertThat(output, containsString(SUCCESSFUL_IMPORT_MESSAGE));
        assertThat(Files.exists(getPathToProcessedDirectory().resolve(VALID_CONFIG_FILE_1)), is(true));
    }

    @Test
    public void testConfigStatusFailedImports() throws Exception {
        addConfigurationFileAndWaitForFailedProcessing(INVALID_CONFIG_FILE_1,
                getResourceAsStream(INVALID_CONFIG_FILE_1));
        addConfigurationFileAndWaitForFailedProcessing(INVALID_CONFIG_FILE_2,
                getResourceAsStream(INVALID_CONFIG_FILE_2));
        String output = console.runCommand(PLATFORM_CONFIG_STATUS_COMMAND);
        assertThat(output,
                containsString(String.format(FAILED_IMPORT_MESSAGE, INVALID_CONFIG_FILE_1)));
        assertThat(output,
                containsString(String.format(FAILED_IMPORT_MESSAGE, INVALID_CONFIG_FILE_2)));
        assertThat(Files.exists(getPathToFailedDirectory().resolve(INVALID_CONFIG_FILE_1)), is(true));
        assertThat(Files.exists(getPathToFailedDirectory().resolve(INVALID_CONFIG_FILE_2)), is(true));
    }

    @Test
    public void testConfigStatusFailedImportReimportSuccessful() throws Exception {
        SECONDS.sleep(10);
        InputStream is = getResourceAsStream(VALID_CONFIG_FILE_1);
        InputStream invalidConfigFileAsInputStream = replaceTextInResource(is, "service.pid",
                "invalid");
        String invalidConfigFileName = VALID_CONFIG_FILE_1;
        addConfigurationFileAndWaitForFailedProcessing(invalidConfigFileName,
                invalidConfigFileAsInputStream);
        String output1 = console.runCommand(PLATFORM_CONFIG_STATUS_COMMAND);
        assertThat(output1,
                containsString(String.format(FAILED_IMPORT_MESSAGE, invalidConfigFileName)));
        assertThat(Files.exists(getPathToFailedDirectory().resolve(invalidConfigFileName)), is(true));
        SECONDS.sleep(10);
        addConfigurationFileAndWaitForSuccessfulProcessing(VALID_CONFIG_FILE_1,
                getResourceAsStream(VALID_CONFIG_FILE_1));
        String output2 = console.runCommand(PLATFORM_CONFIG_STATUS_COMMAND);
        assertThat(output2, containsString(SUCCESSFUL_IMPORT_MESSAGE));
        assertThat(Files.exists(getPathToProcessedDirectory().resolve(VALID_CONFIG_FILE_1)), is(true));
    }

    private Path getPathToConfigDirectory() {
        return Paths.get(ddfHome).resolve("etc");
    }

    private Path getPathToProcessedDirectory() {
        return getPathToConfigDirectory().resolve("processed");
    }

    private Path getPathToFailedDirectory() {
        return getPathToConfigDirectory().resolve("failed");
    }

    private InputStream getResourceAsStream(String resource) {
        return getClass().getResourceAsStream("/" + resource);
    }

    private void addConfigurationFile(String fileName, InputStream inputStream) throws IOException {
        FileUtils.copyInputStreamToFile(inputStream, getPathToConfigDirectory().resolve(fileName)
                .toFile());
        inputStream.close();
    }

    private void addConfigurationFileAndWaitForSuccessfulProcessing(String resourceName,
            InputStream inputStream) throws IOException {
        addConfigurationFile(resourceName, inputStream);

        expect("File " + getPathToProcessedDirectory().resolve(resourceName).toString() + " exists")
                .within(20, SECONDS).until(
                        () -> Files.exists(getPathToProcessedDirectory().resolve(resourceName)));
    }

    private void addConfigurationFileAndWaitForFailedProcessing(String resourceName,
            InputStream inputStream) throws IOException {
        addConfigurationFile(resourceName, inputStream);

        expect("File " + getPathToFailedDirectory().resolve(resourceName).toString() + " exists").within(20, SECONDS).until(
                () -> Files.exists(getPathToFailedDirectory().resolve(resourceName)));
    }

    private InputStream replaceTextInResource(InputStream is, String textToReplace,
            String replacement) throws IOException {
        StringWriter writer = new StringWriter();
        IOUtils.copy(is, writer);
        String original = writer.toString();
        String modified = original.replace(textToReplace, replacement);
        return IOUtils.toInputStream(modified, "UTF-8");
    }
}
