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
package org.codice.ddf.admin.insecure.defaults.service;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.codice.ddf.admin.insecure.defaults.service.DefaultUsersDeletionScheduler.getTempTimestampFilePath;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spi.RouteController;
import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.jaas.modules.BackingEngineFactory;
import org.apache.karaf.jaas.modules.properties.PropertiesBackingEngineFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

public class DefaultUsersDeletionSchedulerTest {

  private Path path;
  private Path tempFilePath;
  private CamelContext context;
  private DefaultUsersDeletionScheduler scheduler;

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void setup() throws IOException {
    temporaryFolder.create();
    context = mock(DefaultCamelContext.class);
    when(context.getRouteController()).thenReturn(mock(RouteController.class));
    scheduler = new DefaultUsersDeletionScheduler(context);
    File testFile = temporaryFolder.newFile("users.properties");
    path = Paths.get(testFile.getPath());
    DefaultUsersDeletionScheduler.setUsersPropertiesFilePath(path);
  }

  @After
  public void cleanup() throws IOException {
    temporaryFolder.delete();
  }

  @Test
  public void scheduleDeletionFileExistsTest() throws Exception {
    Mockito.doAnswer(invocationOnMock -> DefaultUsersDeletionScheduler.removeDefaultUsers())
        .when(context)
        .start();
    createTempFile();
    writeTimestamp(Instant.now().toString());

    scheduler.scheduleDeletion();

    assertTrue(!scheduler.defaultUsersExist() && Files.notExists(tempFilePath));
  }

  @Test
  public void getCronTempFileExistsTest() throws Exception {
    createTempFile();
    writeTimestamp(Instant.now().toString());

    String actual = scheduler.getCronOrDelete();
    assertNotNull("Incorrect cron calculation.", actual);
  }

  @Test
  public void getCronTempFileExistsAndOverdueTest() throws IOException {
    createTempFile();
    writeTimestamp(Instant.now().minus(Duration.ofDays(4)).toString());

    String actual = scheduler.getCronOrDelete();
    assertNull("Incorrect cron calculation.", actual);
  }

  @Test(expected = DateTimeParseException.class)
  public void writeToFileNumberFormatException() throws Exception {
    createTempFile();
    writeTimestamp("Incorrect type");

    String actual = scheduler.getCronOrDelete();
    assertNull("Incorrect error handling.", actual);
  }

  @Test
  public void removeDefaultUsersFromLongerFileTest() throws Exception {
    writeUsersPropertiesFile();

    // What removeDefaultUsers does (without having to do service properties)
    BackingEngineFactory backingEngineFactory = new PropertiesBackingEngineFactory();
    BackingEngine backingEngine =
        backingEngineFactory.build(ImmutableMap.of("users", path.toString()));
    backingEngine.listUsers().forEach(user -> backingEngine.deleteUser(user.getName()));

    assertTrue(!scheduler.defaultUsersExist());
  }

  @Test
  public void installationDateTest() throws IOException {
    Instant instant = Instant.ofEpochSecond(1508472513);
    createTempFile();
    writeTimestamp(instant.toString());

    Instant actual = scheduler.installationDate();
    assertEquals("Incorrect Instant read.", instant, actual);
  }

  private void createTempFile() throws IOException {
    File testFile = temporaryFolder.newFile("timestamp.bin");
    tempFilePath = Paths.get(testFile.getPath());
    DefaultUsersDeletionScheduler.setTempTimestampFilePath(tempFilePath);
  }

  private void writeTimestamp(String instant) throws IOException {
    Files.write(getTempTimestampFilePath(), instant.getBytes(StandardCharsets.UTF_8));
  }

  private void writeUsersPropertiesFile() throws Exception {
    try (FileChannel source =
            new FileInputStream(
                    Paths.get(getClass().getResource("/mockLongUsers.properties").toURI()).toFile())
                .getChannel();
        FileChannel destination = new FileOutputStream(path.toFile()).getChannel()) {
      destination.transferFrom(source, 0, source.size());
    }
  }
}
