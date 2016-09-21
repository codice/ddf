/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.catalog.content.monitor;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.component.mock.MockComponent;
import org.apache.camel.model.FromDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.Constants;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;

@RunWith(JUnit4.class)
public class ContentDirectoryMonitorTest extends CamelTestSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContentDirectoryMonitorTest.class);

    private static final String PROTOCOL = "file://";

    private static final String DUMMY_DATA = "Dummy data in a text file. ";

    private static final List<String> ATTRIBUTE_OVERRIDES = Arrays.asList("test1=someParameter1",
            "test2=someParameter2");

    private static final int MAX_SECONDS_FOR_FILE_COPY = 5;

    private static final int MAX_CHECKS_FOR_FILE_COPY = 10;

    private String monitoredDirectoryPath;

    private File monitoredDirectory;

    private CamelContext camelContext;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setup() throws Exception {
        monitoredDirectory = temporaryFolder.newFolder("inbox");
        monitoredDirectoryPath = monitoredDirectory.getAbsolutePath();

        camelContext = super.createCamelContext();
        camelContext.start();

        MockComponent contentComponent = new MockComponent();
        camelContext.addComponent("content", contentComponent);
    }

    @After
    public void destroy() throws Exception {
        camelContext.stop();
    }

    @Test
    public void testRouteCreationWithoutContentComponent() throws Exception {
        camelContext.removeComponent("content");
        ContentDirectoryMonitor monitor = createContentDirectoryMonitor();
        submitConfigOptions(monitor, monitoredDirectoryPath, false);
        assertThat("The content directory monitor should not have any route definitions",
                monitor.getRouteDefinitions(),
                empty());
        assertThat("The camel context should not have any route definitions",
                camelContext.getRouteDefinitions(),
                empty());
    }

    @Test
    public void testRouteCreationWithCopyIngestedFiles() throws Exception {
        testRouteCreationWithGivenCopyStatus(true);
    }

    @Test
    public void testRouteCreationWithoutCopyIngestedFiles() throws Exception {
        testRouteCreationWithGivenCopyStatus(false);
    }

    private void testRouteCreationWithGivenCopyStatus(boolean copyIngestedFiles) throws Exception {
        ContentDirectoryMonitor monitor = createContentDirectoryMonitor();
        submitConfigOptions(monitor, monitoredDirectoryPath, copyIngestedFiles);
        assertThat("The content directory monitor should only have one route definition",
                monitor.getRouteDefinitions(),
                hasSize(1));
        RouteDefinition routeDefinition = monitor.getRouteDefinitions()
                .get(0);
        verifyRoute(routeDefinition, monitoredDirectoryPath, copyIngestedFiles);
    }

    @Test
    public void testMoveFile() throws Exception {
        ContentDirectoryMonitor monitor = createContentDirectoryMonitor();
        submitConfigOptions(monitor, monitoredDirectoryPath, true);
        doAndVerifyFileMove(monitoredDirectory, monitoredDirectory, "input1.txt");
    }

    @Test
    public void testUpdateExistingContentDirectoryMonitor() throws Exception {
        File monitoredDirectory1 = temporaryFolder.newFolder("inbox1");
        File monitoredDirectory2 = temporaryFolder.newFolder("inbox2");

        ContentDirectoryMonitor monitor = createContentDirectoryMonitor();

        submitConfigOptions(monitor, monitoredDirectory1.getAbsolutePath(), true);
        doAndVerifyFileMove(monitoredDirectory1, monitoredDirectory1, "input1.txt");

        submitConfigOptions(monitor, monitoredDirectory2.getAbsolutePath(), true);
        doAndVerifyFileMove(monitoredDirectory2, monitoredDirectory2, "input2.txt");

        doAndVerifyFileDidNotMove(monitoredDirectory1, monitoredDirectory2, "input3.txt");
    }

    @Test
    public void testMultipleContentDirectoryMonitors() throws Exception {
        File monitoredDirectory1 = temporaryFolder.newFolder("inbox1");
        File monitoredDirectory2 = temporaryFolder.newFolder("inbox2");

        ContentDirectoryMonitor monitor1 = createContentDirectoryMonitor();
        ContentDirectoryMonitor monitor2 = createContentDirectoryMonitor();

        submitConfigOptions(monitor1, monitoredDirectory1.getAbsolutePath(), true);
        submitConfigOptions(monitor2, monitoredDirectory2.getAbsolutePath(), true);

        doAndVerifyFileMove(monitoredDirectory1, monitoredDirectory1, "input1.txt");
        doAndVerifyFileMove(monitoredDirectory2, monitoredDirectory2, "input2.txt");
    }

    @Test
    public void testDirectoryMonitorWithParameters() throws Exception {
        ContentDirectoryMonitor monitor = createContentDirectoryMonitor();
        submitConfigOptions(monitor, monitoredDirectoryPath, true, ATTRIBUTE_OVERRIDES);
        RouteDefinition routeDefinition = camelContext.getRouteDefinitions()
                .get(0);
        assertThat(routeDefinition.toString(),
                containsString("SetHeader[" + Constants.ATTRIBUTE_OVERRIDES_KEY
                        + ", simple{Simple: test1=someParameter1,test2=someParameter2}"));
    }

    @Test
    public void testRouteCreationMissingMonitoredDirectory() throws Exception {
        ContentDirectoryMonitor monitor = createContentDirectoryMonitor();
        submitConfigOptions(monitor, "", true);
        assertThat("Camel context should not have any route definitions",
                camelContext.getRouteDefinitions(),
                empty());
        assertThat("Content directory monitor should not have any route definitions",
                monitor.getRouteDefinitions(),
                empty());
    }

    private void doAndVerifyFileMove(File destinationFolder, File monitoredFolder,
            String inputFileName) throws Exception {
        doFileMove(destinationFolder, inputFileName);
        Failsafe.with(new RetryPolicy().retryWhen(false)
                .withMaxRetries(MAX_CHECKS_FOR_FILE_COPY)
                .withDelay(1, TimeUnit.SECONDS))
            .withFallback(() -> {
                throw new RuntimeException("File did not get moved in time");
            })
            .get(() -> verifyFileMovedToIngestedDirectory(monitoredFolder, inputFileName));

        assertThat("File SHOULD have been moved to the /.ingested directory",
                verifyFileMovedToIngestedDirectory(monitoredFolder, inputFileName),
                is(true));
    }

    private void doAndVerifyFileDidNotMove(File destinationFolder, File monitoredFolder,
            String inputFileName) throws Exception {
        doFileMove(destinationFolder, inputFileName);
        TimeUnit.SECONDS.sleep(MAX_SECONDS_FOR_FILE_COPY);
        assertThat("File SHOULD NOT have been moved to the /.ingested directory",
                verifyFileMovedToIngestedDirectory(monitoredFolder, inputFileName),
                is(false));
    }

    private void doFileMove(File destinationFolder, String inputFileName) throws Exception {
        FileUtils.writeStringToFile(new File(destinationFolder, inputFileName), DUMMY_DATA);
        template.sendBodyAndHeader(PROTOCOL + destinationFolder.getAbsolutePath(),
                DUMMY_DATA,
                Exchange.FILE_NAME,
                inputFileName);
    }

    private boolean verifyFileMovedToIngestedDirectory(File monitoredFolder, String fileName)
            throws Exception {
        File target = new File(monitoredFolder.getAbsolutePath() + "/.ingested/" + fileName);
        return target.exists();
    }

    private void verifyRoute(RouteDefinition routeDefinition, String monitoredDirectory,
            boolean copyIngestedFiles) {
        List<FromDefinition> fromDefinitions = routeDefinition.getInputs();
        assertThat(fromDefinitions.size(), is(1));
        String uri = fromDefinitions.get(0)
                .getUri();

        LOGGER.debug("uri = {}", uri);

        String expectedUri = "file:" + monitoredDirectory + "?moveFailed=.errors";
        if (copyIngestedFiles) {
            expectedUri += "&move=.ingested";
        } else {
            expectedUri += "&delete=true";
        }

        assertThat(uri, equalTo(expectedUri));
        List<ProcessorDefinition<?>> processorDefinitions = routeDefinition.getOutputs();
        assertThat(processorDefinitions.size(), is(2));
    }

    private void submitConfigOptions(ContentDirectoryMonitor monitor, String monitoredDirectory,
            boolean copyIngestedFiles) throws Exception {
        Map<String, Object> properties = new HashMap<>();
        properties.put("monitoredDirectoryPath", monitoredDirectory);
        properties.put("copyIngestedFiles", copyIngestedFiles);
        monitor.updateCallback(properties);
    }

    private void submitConfigOptions(ContentDirectoryMonitor monitor, String monitoredDirectory,
            boolean copyIngestedFiles, List<String> attributeOverrides) throws Exception {
        Map<String, Object> properties = new HashMap<>();
        properties.put("monitoredDirectoryPath", monitoredDirectory);
        properties.put("copyIngestedFiles", copyIngestedFiles);
        properties.put("attributeOverrides", attributeOverrides.toArray());
        monitor.updateCallback(properties);
    }

    private ContentDirectoryMonitor createContentDirectoryMonitor() {
        ContentDirectoryMonitor monitor = new ContentDirectoryMonitor(camelContext,
                1,
                1,
                Runnable::run);
        monitor.systemSubjectBinder = exchange -> {
        };
        return monitor;
    }
}
