/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.catalog.content.monitor;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

import ddf.catalog.Constants;
import ddf.catalog.data.AttributeRegistry;
import java.io.File;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.component.mock.MockComponent;
import org.apache.camel.model.FromDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.apache.commons.io.FileUtils;
import org.codice.ddf.platform.serviceflag.ServiceFlag;
import org.codice.junit.rules.RestoreSystemProperties;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ContentDirectoryMonitorTest extends CamelTestSupport {
  private static final String PROTOCOL = "file://";

  private static final String DUMMY_DATA = "Dummy data in a text file. ";

  private static final String[] ATTRIBUTE_OVERRIDES =
      new String[] {
        "test1=someParameter1", "test1=someParameter0", "test2=(some,parameter,with,commas)"
      };

  private static final int MAX_SECONDS_FOR_FILE_COPY = 5;

  private static final int MAX_CHECKS_FOR_FILE_COPY = 10;

  private String monitoredDirectoryPath;

  private File monitoredDirectory;

  private CamelContext camelContext;

  private ContentDirectoryMonitor monitor;

  @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

  @Before
  public void setup() throws Exception {
    monitoredDirectory = temporaryFolder.newFolder("inbox");
    monitoredDirectoryPath = monitoredDirectory.getCanonicalPath();

    camelContext = super.createCamelContext();
    camelContext.start();

    MockComponent contentComponent = new MockComponent();
    camelContext.addComponent("content", contentComponent);
    MockComponent catalogComponent = new MockComponent();
    camelContext.addComponent("catalog", catalogComponent);

    monitor = createContentDirectoryMonitor();
  }

  @After
  public void destroy() throws Exception {
    monitor.destroy(0);
    camelContext.stop();
  }

  @Test
  public void testUpdateCallbackNullProperties() {
    assertThat(monitor.getNumThreads(), is(1));
    assertThat(monitor.getReadLockIntervalMilliseconds(), is(1000));
    monitor.updateCallback(null);
    assertThat(monitor.getNumThreads(), is(1));
    assertThat(monitor.getReadLockIntervalMilliseconds(), is(1000));
  }

  @Test
  public void testUpdateCallback() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("numThreads", 2);
    properties.put("readLockIntervalMilliseconds", 2000);
    monitor.updateCallback(properties);
    assertThat(monitor.getNumThreads(), is(2));
    assertThat(monitor.getReadLockIntervalMilliseconds(), is(2000));
  }

  @Test
  public void testRouteCreationWithoutContentComponent() {
    camelContext.removeComponent("content");
    submitConfigOptions(monitor, monitoredDirectoryPath, ContentDirectoryMonitor.DELETE);
    assertThat(
        "The content directory monitor should not have any route definitions",
        camelContext.getRouteDefinitions(),
        empty());
    assertThat(
        "The camel context should not have any route definitions",
        camelContext.getRouteDefinitions(),
        empty());
  }

  @Test
  public void testRouteCreationWithCopyIngestedFiles() {
    testRouteCreationWithGivenCopyStatus(ContentDirectoryMonitor.MOVE);
  }

  @Test
  public void testRouteCreationWithoutCopyIngestedFiles() {
    testRouteCreationWithGivenCopyStatus(ContentDirectoryMonitor.DELETE);
  }

  @Test
  public void testRouteCreationWithKeepIngestedFiles() {
    testRouteCreationWithGivenCopyStatus(ContentDirectoryMonitor.IN_PLACE);
  }

  private void testRouteCreationWithGivenCopyStatus(String processingMechanism) {
    submitConfigOptions(monitor, monitoredDirectoryPath, processingMechanism);
    assertThat(
        "The content directory monitor should only have one route definition",
        camelContext.getRouteDefinitions(),
        hasSize(1));
    RouteDefinition routeDefinition = camelContext.getRouteDefinitions().get(0);
    verifyRoute(routeDefinition, monitoredDirectoryPath, processingMechanism);
  }

  @Test
  public void testMoveFile() throws Exception {
    submitConfigOptions(monitor, monitoredDirectoryPath, ContentDirectoryMonitor.MOVE);
    doAndVerifyFileMove(monitoredDirectory, monitoredDirectory, "input1.txt");
  }

  @Test
  public void testBlackListedFileExtension() throws Exception {
    System.setProperty("bad.file.extensions", ".txt");
    monitor = createContentDirectoryMonitor();
    submitConfigOptions(monitor, monitoredDirectoryPath, ContentDirectoryMonitor.MOVE);
    doAndVerifyFileDidNotMove(monitoredDirectory, monitoredDirectory, "input1.txt");
  }

  @Test
  public void testBlackListedFile() throws Exception {
    System.setProperty("bad.files", "input1.txt");
    monitor = createContentDirectoryMonitor();
    submitConfigOptions(monitor, monitoredDirectoryPath, ContentDirectoryMonitor.MOVE);
    doAndVerifyFileDidNotMove(monitoredDirectory, monitoredDirectory, "input1.txt");
  }

  @Test
  public void testBlackListedFileExtensions() throws Exception {
    System.setProperty("bad.file.extensions", ".txt,.bin");
    monitor = createContentDirectoryMonitor();
    submitConfigOptions(monitor, monitoredDirectoryPath, ContentDirectoryMonitor.MOVE);
    doAndVerifyFileDidNotMove(monitoredDirectory, monitoredDirectory, "input1.txt");
    doAndVerifyFileDidNotMove(monitoredDirectory, monitoredDirectory, "input1.bin");
  }

  @Test
  public void testBlackListedEmptyFileExtensions() throws Exception {
    System.setProperty("bad.file.extensions", "");
    monitor = createContentDirectoryMonitor();
    submitConfigOptions(monitor, monitoredDirectoryPath, ContentDirectoryMonitor.MOVE);
    doAndVerifyFileMove(monitoredDirectory, monitoredDirectory, "input1.txt");
  }

  @Test
  public void testUpdateExistingContentDirectoryMonitor() throws Exception {
    File monitoredDirectory1 = temporaryFolder.newFolder("inbox1");
    File monitoredDirectory2 = temporaryFolder.newFolder("inbox2");

    submitConfigOptions(
        monitor, monitoredDirectory1.getCanonicalPath(), ContentDirectoryMonitor.MOVE);
    doAndVerifyFileMove(monitoredDirectory1, monitoredDirectory1, "input1.txt");

    submitConfigOptions(
        monitor, monitoredDirectory2.getCanonicalPath(), ContentDirectoryMonitor.MOVE);
    doAndVerifyFileMove(monitoredDirectory2, monitoredDirectory2, "input2.txt");

    doAndVerifyFileDidNotMove(monitoredDirectory1, monitoredDirectory2, "input3.txt");
  }

  @Test
  public void testMultipleContentDirectoryMonitors() throws Exception {
    File monitoredDirectory1 = temporaryFolder.newFolder("inbox1");
    File monitoredDirectory2 = temporaryFolder.newFolder("inbox2");

    ContentDirectoryMonitor monitor1 = createContentDirectoryMonitor();
    ContentDirectoryMonitor monitor2 = createContentDirectoryMonitor();

    submitConfigOptions(
        monitor1, monitoredDirectory1.getCanonicalPath(), ContentDirectoryMonitor.MOVE);
    submitConfigOptions(
        monitor2, monitoredDirectory2.getCanonicalPath(), ContentDirectoryMonitor.MOVE);

    doAndVerifyFileMove(monitoredDirectory1, monitoredDirectory1, "input1.txt");
    doAndVerifyFileMove(monitoredDirectory2, monitoredDirectory2, "input2.txt");
  }

  @Test
  public void testDirectoryMonitorWithParameters() {
    ContentDirectoryMonitor monitor = createContentDirectoryMonitor();
    submitConfigOptions(
        monitor,
        monitoredDirectoryPath,
        ContentDirectoryMonitor.MOVE,
        ATTRIBUTE_OVERRIDES,
        1,
        1000);
    RouteDefinition routeDefinition = camelContext.getRouteDefinitions().get(0);
    assertThat(
        routeDefinition.toString(),
        containsString(
            "SetHeader["
                + Constants.ATTRIBUTE_OVERRIDES_KEY
                + ", {{test2=[(some,parameter,with,commas)], test1=[someParameter1, someParameter0]}}"));
  }

  @Test
  public void testDirectoryMonitorThreadNumFallback() {
    ContentDirectoryMonitor monitor = createContentDirectoryMonitor();
    submitConfigOptions(
        monitor,
        monitoredDirectoryPath,
        ContentDirectoryMonitor.MOVE,
        ATTRIBUTE_OVERRIDES,
        16,
        1000);
    assertThat(monitor.getNumThreads(), is(8));
  }

  @Test
  public void testDirectoryMonitorThreadNumMinimum() {
    ContentDirectoryMonitor monitor = createContentDirectoryMonitor();
    submitConfigOptions(
        monitor,
        monitoredDirectoryPath,
        ContentDirectoryMonitor.MOVE,
        ATTRIBUTE_OVERRIDES,
        0,
        1000);
    assertThat(monitor.getNumThreads(), is(1));
  }

  @Test
  public void testDirectoryMonitorReadLockIntervalMinimum() {
    ContentDirectoryMonitor monitor = createContentDirectoryMonitor();
    submitConfigOptions(
        monitor, monitoredDirectoryPath, ContentDirectoryMonitor.MOVE, ATTRIBUTE_OVERRIDES, 1, 1);
    assertThat(monitor.getReadLockIntervalMilliseconds(), is(100));
  }

  @Test
  public void testRouteCreationMissingMonitoredDirectory() {
    submitConfigOptions(monitor, "", ContentDirectoryMonitor.MOVE);
    assertThat(
        "Camel context should not have any route definitions",
        camelContext.getRouteDefinitions(),
        empty());
    assertThat(
        "Content directory monitor should not have any route definitions",
        camelContext.getRouteDefinitions(),
        empty());
  }

  private void doAndVerifyFileMove(
      File destinationFolder, File monitoredFolder, String inputFileName) throws Exception {
    doFileMove(destinationFolder, inputFileName);
    Failsafe.with(
            new RetryPolicy()
                .retryWhen(false)
                .withMaxRetries(MAX_CHECKS_FOR_FILE_COPY)
                .withDelay(5, TimeUnit.SECONDS))
        .withFallback(
            () -> {
              throw new RuntimeException("File did not get moved in time");
            })
        .get(() -> verifyFileMovedToIngestedDirectory(monitoredFolder, inputFileName));

    assertThat(
        "File SHOULD have been moved to the /.ingested directory",
        verifyFileMovedToIngestedDirectory(monitoredFolder, inputFileName),
        is(true));
  }

  private void doAndVerifyFileDidNotMove(
      File destinationFolder, File monitoredFolder, String inputFileName) throws Exception {
    doFileMove(destinationFolder, inputFileName);
    TimeUnit.SECONDS.sleep(MAX_SECONDS_FOR_FILE_COPY);
    assertThat(
        "File SHOULD NOT have been moved to the /.ingested directory",
        verifyFileMovedToIngestedDirectory(monitoredFolder, inputFileName),
        is(false));
  }

  private void doFileMove(File destinationFolder, String inputFileName) throws Exception {
    FileUtils.writeStringToFile(
        new File(destinationFolder, inputFileName), DUMMY_DATA, Charset.defaultCharset());
    template.sendBodyAndHeader(
        PROTOCOL + destinationFolder.getCanonicalPath(),
        DUMMY_DATA,
        Exchange.FILE_NAME,
        inputFileName);
  }

  private boolean verifyFileMovedToIngestedDirectory(File monitoredFolder, String fileName)
      throws Exception {
    File target = new File(monitoredFolder.getCanonicalPath() + "/.ingested/" + fileName);
    return target.exists();
  }

  private void verifyRoute(
      RouteDefinition routeDefinition, String monitoredDirectory, String processingMechanism) {
    List<FromDefinition> fromDefinitions = routeDefinition.getInputs();
    assertThat(fromDefinitions.size(), is(1));
    String uri = fromDefinitions.get(0).getUri();

    String expectedUri =
        "file:"
            + monitoredDirectory
            + "?recursive=true&moveFailed=.errors&readLockMinLength=1&readLock=changed&readLockTimeout=2000&readLockCheckInterval=1000";
    if (ContentDirectoryMonitor.DELETE.equals(processingMechanism)) {
      expectedUri += "&delete=true";
    } else if (ContentDirectoryMonitor.MOVE.equals(processingMechanism)) {
      expectedUri += "&move=.ingested";
    } else if (ContentDirectoryMonitor.IN_PLACE.equals(processingMechanism)) {
      expectedUri = "durable:" + monitoredDirectory;
    }

    assertThat(uri, equalTo(expectedUri));
    List<ProcessorDefinition<?>> processorDefinitions = routeDefinition.getOutputs();
    assertThat(processorDefinitions.size(), is(1));
  }

  private void submitConfigOptions(
      ContentDirectoryMonitor monitor, String monitoredDirectory, String processingMechanism) {
    Map<String, Object> properties = new HashMap<>();
    properties.put("monitoredDirectoryPath", monitoredDirectory);
    properties.put("processingMechanism", processingMechanism);
    properties.put("numThreads", 1);
    properties.put("readLockIntervalMilliseconds", 1000);
    monitor.updateCallback(properties);
  }

  private void submitConfigOptions(
      ContentDirectoryMonitor monitor,
      String monitoredDirectory,
      String processingMechanism,
      String[] attributeOverrides,
      int numThreads,
      int readLockIntervalMilliseconds) {
    Map<String, Object> properties = new HashMap<>();
    properties.put("monitoredDirectoryPath", monitoredDirectory);
    properties.put("processingMechanism", processingMechanism);
    properties.put("attributeOverrides", attributeOverrides);
    properties.put("numThreads", numThreads);
    properties.put("readLockIntervalMilliseconds", readLockIntervalMilliseconds);
    monitor.updateCallback(properties);
  }

  private ContentDirectoryMonitor createContentDirectoryMonitor() {
    ContentDirectoryMonitor monitor =
        new ContentDirectoryMonitor(
            camelContext,
            mock(AttributeRegistry.class),
            1,
            1,
            Runnable::run,
            mock(ServiceFlag.class));

    monitor.systemSubjectBinder = exchange -> {};
    monitor.setNumThreads(1);
    monitor.setReadLockIntervalMilliseconds(1000);
    return monitor;
  }
}
