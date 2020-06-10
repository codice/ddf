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
package org.codice.ddf.persistence.commands;

import static org.codice.ddf.persistence.PersistentItem.BINARY_SUFFIX;
import static org.codice.ddf.persistence.PersistentItem.DATE_SUFFIX;
import static org.codice.ddf.persistence.PersistentItem.INT_SUFFIX;
import static org.codice.ddf.persistence.PersistentItem.LONG_SUFFIX;
import static org.codice.ddf.persistence.PersistentItem.TEXT_SUFFIX;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.karaf.shell.api.console.Session;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentStore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

/** Tests the {@link StoreListCommand} output. */
public class StoreCommandTest extends ConsoleOutputCommon {

  String dateKey = "key" + DATE_SUFFIX;
  String binKey = "key" + BINARY_SUFFIX;
  String txtKey = "key" + TEXT_SUFFIX;
  String intKey = "key" + INT_SUFFIX;
  String longKey = "key" + LONG_SUFFIX;
  final long dateInput = 1589485511000L;
  final String stringInput = "testString";
  final long longInput = 112223242343L;
  final int intInput = 1245;

  @Rule public TemporaryFolder testFolder = new TemporaryFolder();

  PersistentStore persistentStore = mock(PersistentStore.class);
  Session mockSession = mock(Session.class);

  /** Check for listing command. */
  @Test
  public void testListCommand() throws PersistenceException {
    // given
    when(persistentStore.get(anyString(), any(), eq(0), anyInt())).thenReturn(getOneResult());

    StoreListCommand command = new StoreListCommand();
    command.persistentStore = persistentStore;
    command.type = "preferences";
    command.execute();

    // then
    assertThat(consoleOutput.getOutput(), containsString(" found: 1"));
  }

  /** Check for listing command with cql */
  @Test
  public void testListCommandMax() throws PersistenceException {
    // given
    // startIndex as anyInt() will continue to return one result per page until reaching 101
    when(persistentStore.get(anyString(), anyString(), anyInt(), anyInt()))
        .thenReturn(getOneResult());
    when(persistentStore.get(anyString(), anyString(), eq(101), anyInt()))
        .thenReturn(new ArrayList<>());

    StoreListCommand command = new StoreListCommand();
    command.persistentStore = persistentStore;
    command.type = "preferences";
    command.user = "test";
    command.cql = "property LIKE 'value'";
    command.execute();

    // then
    assertThat(consoleOutput.getOutput(), containsString(" found: 101"));
    assertThat(consoleOutput.getOutput(), containsString("Narrow the search criteria"));
  }

  @Test
  public void testListCommandWithCQL() throws PersistenceException {
    // given
    when(persistentStore.get(anyString(), anyString(), anyInt(), anyInt()))
        .thenReturn(new ArrayList<>());

    StoreListCommand command = new StoreListCommand();
    command.persistentStore = persistentStore;
    command.type = "preferences";
    command.user = "test";
    command.cql = "property LIKE 'value'";
    command.execute();

    // then
    assertThat(consoleOutput.getOutput(), containsString(" found: 0"));
  }

  @Test
  public void testExportCommand() throws PersistenceException {
    // given
    String tempDir = "./tmp";
    File directory = new File(tempDir);
    if (!directory.exists()) {
      directory.mkdir();
    }

    when(persistentStore.get(anyString(), any(), eq(0), anyInt())).thenReturn(getResults());
    when(persistentStore.get(anyString(), any(), eq(10), anyInt())).thenReturn(new ArrayList<>());

    StoreExportCommand command = new StoreExportCommand();
    command.persistentStore = persistentStore;
    command.type = "preferences";
    command.user = "test";
    command.dirPath = tempDir;
    command.execute();

    // then
    assertThat(consoleOutput.getOutput(), containsString("Exported: 5"));

    // clean up
    String[] files = directory.list();
    for (String fileName : files) {
      File currentFile = new File(directory.getPath(), fileName);
      currentFile.delete();
    }
    directory.delete();
  }

  @Test
  public void testExportCommandErrors() throws PersistenceException {
    // given
    StoreExportCommand command = new StoreExportCommand();
    command.persistentStore = persistentStore;
    command.type = "preferences";
    command.execute();

    assertThat(consoleOutput.getOutput(), containsString("directory is not specified"));

    command.dirPath = "notExistDir";
    command.execute();

    assertThat(consoleOutput.getOutput(), containsString("Directory does not exist"));

    command.dirPath = "pom.xml";
    command.execute();

    assertThat(consoleOutput.getOutput(), containsString("path is not a directory"));
  }

  @Test
  public void testImportCommand() throws PersistenceException {
    // given
    ArgumentCaptor<List> argument = ArgumentCaptor.forClass(List.class);
    StoreImportCommand command = new StoreImportCommand();
    command.persistentStore = persistentStore;
    command.type = "preferences";

    command.filePath = "src/test/resources/GoodSet";
    command.execute();

    verify(persistentStore, times(1)).add(anyString(), argument.capture());
    List<Map<String, Object>> items = argument.getValue();
    assertThat(items.size(), equalTo(5));
    for (Map<String, Object> item : items) {
      assertThat(item.keySet().size(), equalTo(1));
      for (String key : item.keySet()) {
        Object value = item.get(key);
        if (value instanceof Date) {
          Date dateValue = (Date) value;
          assertThat(dateValue.getTime(), equalTo(dateInput));
        } else if (value instanceof byte[]) {
          byte[] byteArray = (byte[]) value;
          assertThat(new String(byteArray), equalTo(stringInput));
        } else if (value instanceof Long) {
          Long longValue = (long) value;
          assertThat(longValue, equalTo(longInput));
        } else if (value instanceof Integer) {
          int intValue = (int) value;
          assertThat(intValue, equalTo(intInput));
        } else if (value instanceof String) {
          assertThat(value, equalTo(stringInput));
        } else {
          assertFalse(false);
        }
      }
    }
    // then
    assertThat(consoleOutput.getOutput(), containsString("Found 5 files"));
    assertThat(consoleOutput.getOutput(), containsString("Processing"));
  }

  @Test
  public void testImportCommandError() {
    // given
    StoreImportCommand command = new StoreImportCommand();
    command.persistentStore = persistentStore;
    command.type = "preferences";

    command.filePath = "src/test/resources/badFile";
    command.execute();
    // then
    assertThat(consoleOutput.getOutput(), containsString("If the file does indeed exist"));

    command.filePath = "src/test/resources/ErrorSet/BadJson";
    command.execute();
    // then
    assertThat(consoleOutput.getOutput(), containsString("Unable to parse json file"));

    command.filePath = "src/test/resources/ErrorSet";
    command.execute();
    // then
    assertThat(consoleOutput.getOutput(), containsString("Unable to parse json file"));
  }

  @Test
  public void testDeleteCommand() throws PersistenceException, IOException {
    // given
    String tempDir = "./tmp";
    File directory = new File(tempDir);
    if (!directory.exists()) {
      directory.mkdir();
    }

    when(persistentStore.get(anyString(), any(), eq(0), anyInt())).thenReturn(new ArrayList<>());

    StoreDeleteCommand command = new StoreDeleteCommand();
    command.persistentStore = persistentStore;
    command.type = "preferences";
    command.session = mockSession;
    command.execute();

    assertThat(consoleOutput.getOutput(), containsString("0 results matched cql statement"));

    when(persistentStore.get(anyString(), any(), eq(0), anyInt())).thenReturn(getResults());
    when(persistentStore.get(anyString(), any(), eq(10), anyInt())).thenReturn(new ArrayList<>());
    when(persistentStore.delete(anyString(), any(), eq(0), anyInt())).thenReturn(5);
    when(persistentStore.delete(anyString(), any(), eq(1000), anyInt())).thenReturn(0);
    when(mockSession.readLine(anyString(), any())).thenReturn("YES");

    command.execute();

    // then
    assertThat(consoleOutput.getOutput(), containsString("5 results matched cql"));

    when(mockSession.readLine(anyString(), any())).thenReturn("NO");
    command.execute();

    // then
    assertThat(consoleOutput.getOutput(), containsString("Delete canceled"));
  }

  /** Check for listing command. */
  @Test
  public void testPersistenceException() throws PersistenceException {
    // given
    when(persistentStore.get(anyString(), any(), eq(0), anyInt()))
        .thenThrow(new PersistenceException());

    StoreListCommand command = new StoreListCommand();
    command.persistentStore = persistentStore;
    command.type = "preferences";
    command.execute();

    // then
    assertThat(consoleOutput.getOutput(), containsString("Encountered an error when"));
  }

  private List<Map<String, Object>> getOneResult() {
    List<Map<String, Object>> results = new ArrayList<>();
    Map<String, Object> item = new HashMap<>();
    item.put(txtKey, "value");
    results.add(item);
    return results;
  }

  private List<Map<String, Object>> getResults() {
    List<Map<String, Object>> results = new ArrayList<>();
    Map<String, Object> dateItem = new HashMap<>();
    dateItem.put(dateKey, new Date(dateInput));
    results.add(dateItem);

    Map<String, Object> binItem = new HashMap<>();
    binItem.put(binKey, stringInput.getBytes());
    results.add(binItem);

    Map<String, Object> textItem = new HashMap<>();
    textItem.put(txtKey, stringInput);
    results.add(textItem);

    Map<String, Object> longItem = new HashMap<>();
    longItem.put(longKey, longInput);
    results.add(longItem);

    Map<String, Object> intItem = new HashMap<>();
    intItem.put(intKey, intInput);
    results.add(intItem);

    return results;
  }
}
