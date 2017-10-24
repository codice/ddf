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
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.codice.ddf.configuration.AbsolutePathResolver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class UsersPropertiesDeletionSchedulerTest {

  private static final Path PATH = UsersPropertiesDeletionScheduler.USERS_PROPERTIES_FILE_PATH;
  private static final Path TEMP_FILE_PATH =
      Paths.get(new AbsolutePathResolver("data/tmp/timestamp.bin").getPath());
  private CamelContext context;
  private UsersPropertiesDeletionScheduler scheduler;

  @Before
  public void setup() throws IOException {
    context = mock(DefaultCamelContext.class);
    scheduler = new UsersPropertiesDeletionScheduler(context);
    File testFile = PATH.toFile();
    File testFileParent = new File(testFile.getParent());
    testFileParent.mkdirs();
    testFile.createNewFile();
    Files.exists(PATH);
  }

  @After
  public void cleanup() throws IOException {
    Files.deleteIfExists(PATH);
    Files.deleteIfExists(PATH.getParent());
    Files.deleteIfExists(TEMP_FILE_PATH);
    Files.deleteIfExists(TEMP_FILE_PATH.getParent());
    Files.deleteIfExists(TEMP_FILE_PATH.getParent().getParent());
  }

  @Test
  public void scheduleDeletionFileExistsTest() throws Exception {
    Mockito.doAnswer(invocationOnMock -> UsersPropertiesDeletionScheduler.deleteFiles())
        .when(context)
        .start();
    createTempFile();
    writeTimestamp(Instant.now());

    scheduler.scheduleDeletion();

    assertTrue("The users.properties file was not deleted in time.", Files.notExists(PATH));
  }

  @Test
  public void getCronTempFileExistsTest() throws IOException, InterruptedException {
    createTempFile();
    writeTimestamp(Instant.now());

    String actual = scheduler.getCron();
    assertNotNull("Incorrect cron calculation.", actual);
  }

  @Test
  public void getCronNoTempFileTest() throws IOException {
    // Create directory for temp file (this is for testing purposes only. In production it will be
    // placed in an already existing directory /data/tmp)
    File tempFileParent = TEMP_FILE_PATH.getParent().toFile();
    assertTrue("Unable to create temp file directories.", tempFileParent.mkdirs());

    assertNotNull("Incorrect cron calculation.", scheduler.getCron());
    assertTrue("No temporary file found.", Files.exists(TEMP_FILE_PATH));
  }

  @Test
  public void getCronTempFileExistsAndOverdueTest() throws IOException {
    createTempFile();
    writeTimestamp(Instant.now().minus(Duration.ofDays(4)));

    String actual = scheduler.getCron();
    assertNull("Incorrect cron calculation.", actual);
  }

  @Test(expected = ClassCastException.class)
  public void writeToFileWithError() throws Exception {
    createTempFile();
    writeIncorrectTimestamp("Incorrect type");

    String actual = scheduler.getCron();
    assertNull("Incorrect error handling.", actual);
  }

  @Test
  public void deleteFileTest() throws IOException {
    UsersPropertiesDeletionScheduler.deleteFiles();
    assertTrue(Files.notExists(PATH));
  }

  @Test
  public void installationDateTest() throws IOException {
    Instant instant = Instant.ofEpochSecond(1508472513);
    createTempFile();
    writeTimestamp(instant);

    Instant actual = scheduler.installationDate();
    assertEquals("Incorrect Instant read.", instant, actual);
  }

  private void createTempFile() throws IOException {
    File tempFile = TEMP_FILE_PATH.toFile();
    File tempFileParent = new File(tempFile.getParent());
    assertTrue("Unable to create temp file directories.", tempFileParent.mkdirs());
    assertTrue("Unable to create temp file.", tempFile.createNewFile());
    assertTrue("No temporary file found.", Files.exists(TEMP_FILE_PATH));
  }

  private void writeTimestamp(Instant instant) throws IOException {
    try (ObjectOutputStream objectOutputStream =
        new ObjectOutputStream(new FileOutputStream(TEMP_FILE_PATH.toFile()))) {
      objectOutputStream.writeObject(instant);
    }
  }

  private void writeIncorrectTimestamp(String string) throws IOException {
    try (ObjectOutputStream objectOutputStream =
        new ObjectOutputStream(new FileOutputStream(TEMP_FILE_PATH.toFile()))) {
      objectOutputStream.writeObject(string);
    }
  }
}
