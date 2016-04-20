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
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockComponent;
import org.apache.camel.model.FromDefinition;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.Constants;

public class ContentDirectoryMonitorTest extends CamelTestSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContentDirectoryMonitorTest.class);

    private static final String INPUT_FILENAME = "input.txt";

    private static final String INPUT_FILEPATH = "target/" + INPUT_FILENAME;

    private static final String MONITORED_DIRECTORY = "target/inbox";

    private ModelCamelContext camelContext;

    private ContentDirectoryMonitor contentDirectoryMonitor;

    private Processor noOpProcessor = exchange -> {
    };

    private static final List<String> ATTRIBUTE_OVERRIDES = Arrays.asList("test1=someParameter1",
            "test2=someParameter2");

    @After
    public void tearDown() throws Exception {
        LOGGER.debug("INSIDE tearDown");

        // This will also stop all routes/components/endpoints, etc. 
        // and clear internal state/cache
        camelContext.stop();
        camelContext = null;
    }

    @Test
    public void testRouteCreationWithCopyIngestedFiles() throws Exception {
        boolean copyIngestedFiles = true;

        RouteDefinition routeDefinition = createRoute(MONITORED_DIRECTORY, copyIngestedFiles);

        verifyRoute(routeDefinition, MONITORED_DIRECTORY, copyIngestedFiles);
    }

    @Test
    public void testRouteCreationWithoutCopyIngestedFiles() throws Exception {
        boolean copyIngestedFiles = false;

        RouteDefinition routeDefinition = createRoute(MONITORED_DIRECTORY, copyIngestedFiles);

        verifyRoute(routeDefinition, MONITORED_DIRECTORY, copyIngestedFiles);
    }

    @Test
    public void testRouteCreationMissingMonitoredDirectory() throws Exception {
        String monitoredDirectory = "";
        boolean copyIngestedFiles = true;

        camelContext = (ModelCamelContext) super.createCamelContext();
        camelContext.start();

        // Map the "content" scheme to a mock component so that we do not have to
        // mock the entire custom ContentComponent and include its implementation
        // in pom with scope=test
        camelContext.addComponent("content", new MockComponent());

        contentDirectoryMonitor = new ContentDirectoryMonitor(camelContext);
        contentDirectoryMonitor.systemSubjectBinder = noOpProcessor;
        contentDirectoryMonitor.setMonitoredDirectoryPath(monitoredDirectory);
        contentDirectoryMonitor.setCopyIngestedFiles(copyIngestedFiles);

        // Simulates what container would do once all setters have been invoked
        contentDirectoryMonitor.init();

        assertThat(camelContext.getRouteDefinitions()
                .size(), is(0));
    }

    @Test
    public void testMoveFolder() throws Exception {
        boolean copyIngestedFiles = true;

        createRoute(MONITORED_DIRECTORY, copyIngestedFiles);

        // Put file in monitored directory
        String fileContents = "Dummy data in a text file";
        FileUtils.writeStringToFile(new File(INPUT_FILEPATH), fileContents);

        template.sendBodyAndHeader("file://" + MONITORED_DIRECTORY,
                fileContents,
                Exchange.FILE_NAME,
                INPUT_FILENAME);

        Thread.sleep(3000);

        // Verify the file moved to the .ingested directory
        File target = new File(MONITORED_DIRECTORY + "/.ingested/" + INPUT_FILENAME);
        assertTrue("File not moved to .ingested folder", target.exists());

        // Cleanup
        FileUtils.deleteDirectory(new File(MONITORED_DIRECTORY));
    }

    /**
     * Verify if route has a failure then the file being processed is moved to the .errors
     * directory.
     *
     * @throws Exception
     */
    @Test
    public void testUpdateExistingDirectoryMonitor() throws Exception {
        boolean copyIngestedFiles = true;

        createRoute(MONITORED_DIRECTORY, copyIngestedFiles);

        // Put file in monitored directory
        String fileContents = "Dummy data in a text file";
        FileUtils.writeStringToFile(new File(INPUT_FILEPATH), fileContents);
        template.sendBodyAndHeader("file://" + MONITORED_DIRECTORY,
                fileContents,
                Exchange.FILE_NAME,
                INPUT_FILENAME);

        Thread.sleep(3000);

        // Verify the file moved to the .ingested directory
        File target = new File(MONITORED_DIRECTORY + "/.ingested/" + INPUT_FILENAME);
        assertTrue("File 1 not moved to .ingested folder", target.exists());

        // Update the existing directory monitor to point to different directory
        String newMonitoredDirectory = "target/inbox_2";
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("monitoredDirectoryPath", newMonitoredDirectory);
        properties.put("copyIngestedFiles", true);
        contentDirectoryMonitor.updateCallback(properties);

        // Put file in new monitored directory
        fileContents = "Dummy data in second text file";
        FileUtils.writeStringToFile(new File("target/input_2.txt"), fileContents);
        template.sendBodyAndHeader("file://" + newMonitoredDirectory,
                fileContents,
                Exchange.FILE_NAME,
                "input_2.txt");

        Thread.sleep(3000);

        // Verify the file moved to the .ingested directory
        target = new File(newMonitoredDirectory + "/.ingested/input_2.txt");
        assertTrue("File 2 not moved to .ingested folder", target.exists());

        // Put file in original monitored directory
        fileContents = "Dummy data in third text file";
        FileUtils.writeStringToFile(new File("target/input_3.txt"), fileContents);
        template.sendBodyAndHeader("file://" + MONITORED_DIRECTORY,
                fileContents,
                Exchange.FILE_NAME,
                "input_3.txt");

        Thread.sleep(3000);

        // Verify the file is not moved to the .ingested directory since it is
        // no longer monitored
        target = new File(MONITORED_DIRECTORY + "/.ingested/input_3.txt");
        assertFalse("File 3 moved to .ingested folder", target.exists());
        target = new File(MONITORED_DIRECTORY + "/input_3.txt");
        assertTrue("File 3 not in old monitored folder", target.exists());

        // Cleanup
        FileUtils.deleteDirectory(new File(MONITORED_DIRECTORY));
        FileUtils.deleteDirectory(new File(newMonitoredDirectory));
    }

    @Test
    public void testMultipleDirectoryMonitors() throws Exception {
        String firstMonitoredDirectory = "target/inbox_1";
        boolean copyIngestedFiles = true;

        createRoute(firstMonitoredDirectory, copyIngestedFiles);

        String secondMonitoredDirectory = "target/inbox_2";
        copyIngestedFiles = true;

        createRoute(secondMonitoredDirectory, copyIngestedFiles);

        // Put file in first monitored directory
        String fileContents = "text file 1";
        FileUtils.writeStringToFile(new File(INPUT_FILEPATH), fileContents);
        template.sendBodyAndHeader("file://" + firstMonitoredDirectory,
                fileContents,
                Exchange.FILE_NAME,
                INPUT_FILENAME);

        fileContents = "text file 2";
        FileUtils.writeStringToFile(new File("target/input_2.txt"), fileContents);
        template.sendBodyAndHeader("file://" + secondMonitoredDirectory,
                fileContents,
                Exchange.FILE_NAME,
                "input_2.txt");

        Thread.sleep(3000);

        // Verify the files were moved to the correct .ingested directories
        File target = new File(firstMonitoredDirectory + "/.ingested/" + INPUT_FILENAME);
        assertTrue("File 1 not moved to .ingested folder", target.exists());

        target = new File(secondMonitoredDirectory + "/.ingested/input_2.txt");
        assertTrue("File 2 not moved to .ingested folder", target.exists());

        // Cleanup
        FileUtils.deleteDirectory(new File(firstMonitoredDirectory));
        FileUtils.deleteDirectory(new File(secondMonitoredDirectory));
    }

    @Test
    public void testDirectoryMonitorWithParameters() throws Exception {
        boolean copyIngestedFiles = true;

        camelContext = (ModelCamelContext) super.createCamelContext();
        camelContext.start();
        camelContext.addComponent("content", new MockComponent());

        contentDirectoryMonitor = new ContentDirectoryMonitor(camelContext);
        contentDirectoryMonitor.systemSubjectBinder = noOpProcessor;
        contentDirectoryMonitor.setMonitoredDirectoryPath(MONITORED_DIRECTORY);
        contentDirectoryMonitor.setCopyIngestedFiles(copyIngestedFiles);
        contentDirectoryMonitor.setAttributeOverrides(ATTRIBUTE_OVERRIDES);
        contentDirectoryMonitor.init();
        RouteDefinition routeDefinition = camelContext.getRouteDefinitions()
                .get(0);
        assertThat(routeDefinition.toString(), containsString(
                "SetHeader[" + Constants.ATTRIBUTE_OVERRIDES_KEY + ", simple{Simple: test1=someParameter1,test2=someParameter2}"));
    }

    /**
     * ********************************************************************************
     */

    private RouteDefinition createRouteWithAdvice(String monitoredDirectory,
            boolean copyIngestedFiles) throws Exception {
        camelContext = (ModelCamelContext) super.createCamelContext();
        camelContext.start();

        contentDirectoryMonitor = new ContentDirectoryMonitor(camelContext);
        contentDirectoryMonitor.systemSubjectBinder = noOpProcessor;
        contentDirectoryMonitor.setMonitoredDirectoryPath(monitoredDirectory);
        contentDirectoryMonitor.setCopyIngestedFiles(copyIngestedFiles);

        // Simulates what container would do once all setters have been invoked
        contentDirectoryMonitor.init();

        // Did not work because it expects the route to be "adviced" already exists. So the above
        // init()
        // call created the initial route with the "content:framework" node in it, and then this
        // AdviceWithRouteBuilder replaced the content:framework with mock:result, but created a
        // second
        // route that had this change in it. (So still get NPE because first route fires and cannot
        // resolve the "content" scheme)
        camelContext.getRouteDefinitions()
                .get(0)
                .adviceWith(camelContext, new AdviceWithRouteBuilder() {
                    @Override
                    public void configure() throws Exception {
                        // weave the node in the route which has id = content://framework
                        // and replace it with the following route path
                        weaveByToString(".*content:framework.*").replace()
                                .to("mock:result");
                    }
                });

        // Initial Camel route should now be created
        List<RouteDefinition> routeDefinitions = contentDirectoryMonitor.getRouteDefinitions();
        assertThat(routeDefinitions.size(), is(1));
        LOGGER.debug("routeDefinition = {}", routeDefinitions.get(0));

        return routeDefinitions.get(0);
    }

    private RouteDefinition createRoute(String monitoredDirectory, boolean copyIngestedFiles)
            throws Exception {

        // Simulates what container would do for <camel:camelContext id="camelContext">
        // declaration in beans.xml file
        camelContext = (ModelCamelContext) super.createCamelContext();
        camelContext.start();

        // Map the "content" scheme to a mock component so that we do not have to
        // mock the entire custom ContentComponent and include its implementation
        // in pom with scope=test
        camelContext.addComponent("content", new MockComponent());

        contentDirectoryMonitor = new ContentDirectoryMonitor(camelContext);
        contentDirectoryMonitor.systemSubjectBinder = noOpProcessor;
        contentDirectoryMonitor.setMonitoredDirectoryPath(monitoredDirectory);
        contentDirectoryMonitor.setCopyIngestedFiles(copyIngestedFiles);

        // Simulates what container would do once all setters have been invoked
        contentDirectoryMonitor.init();

        // Initial Camel route should now be created
        List<RouteDefinition> routeDefinitions = contentDirectoryMonitor.getRouteDefinitions();
        assertThat(routeDefinitions.size(), is(1));
        LOGGER.debug("routeDefinition = {}", routeDefinitions.get(0));

        return routeDefinitions.get(0);
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

}
