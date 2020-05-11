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
package ddf.catalog.backup;

import static junit.framework.Assert.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.plugin.PluginExecutionException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.codice.ddf.platform.util.StandardThreadFactoryBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CatalogBackupPluginTest {

  private static final String[] METACARD_IDS = {
    "6c3810b98a3b4208b51bc0a96d61e5", "1b6482b1b8f730e343a96d61e0e089"
  };

  private static final String BASE_OLD_TITLE = "oldTitle";

  private static final String BASE_NEW_TITLE = "newTitle";

  @Rule public TemporaryFolder backupDirObject = new TemporaryFolder();

  @Test
  public void testCreateResponseNotNull() throws PluginExecutionException {

    assertThat(getPlugin().process(getCreateResponse(METACARD_IDS)), is(notNullValue()));
  }

  @Test
  public void testCreateResponseSubdirectoryLevelsIsZero() throws Exception {
    // Setup
    int subDirLevels = 0;

    // Perform test
    getPlugin(0).process(getCreateResponse(METACARD_IDS));

    // Verify
    assertFilesExist(METACARD_IDS, subDirLevels);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateResponseSubdirectoryLevelsIsNegative() throws Exception {
    // Setup
    getPlugin(-1).process(getCreateResponse(METACARD_IDS));
  }

  /**
   * If too many subdirectories are specified (two characters from metacardId are used to name each
   * subdirectory), then the CatalogBackupPlugin makes as many subdirectories as it can for each
   * metacardId.
   */
  @Test
  public void testCreateResponseSubdirectoryLevelsTooBigForMetacardId() throws Exception {
    // Setup
    String[] ids = {"6c38", "0283"};
    getPlugin(1000).process(getCreateResponse(ids));

    // Verify
    assertFilesExist(ids, 2);
  }

  @Test
  public void testCreateResponseCreateSuccessful() throws Exception {

    int subDirLevels = 3;
    getPlugin(subDirLevels).process(getCreateResponse(METACARD_IDS));
    assertFilesExist(METACARD_IDS, subDirLevels);
  }

  @Test
  public void testDeleteResponseDeleteSuccessful() throws Exception {

    // Setup
    int subDirLevels = 3;
    CatalogBackupPlugin plugin = getPlugin(subDirLevels);
    DeleteResponse mockDeleteResponse = getDeleteResponse(Arrays.asList(METACARD_IDS));
    plugin.process(getCreateResponse(METACARD_IDS));

    // Delete files
    DeleteResponse postPluginDeleteResponse = plugin.process(mockDeleteResponse);

    // Ensure files were deleted
    assertThat(postPluginDeleteResponse, is(notNullValue()));
    assertFilesDoNotExist(METACARD_IDS, subDirLevels);
  }

  @Test
  public void testDeleteResponseFailToDeleteAllMetacards() {

    DeleteResponse mockDeleteResponse = getDeleteResponse(Arrays.asList(METACARD_IDS));

    // Perform Test
    getPlugin().process(mockDeleteResponse);
  }

  @Test
  public void testDeleteResponseFailToDeleteOneMetacard() throws PluginExecutionException {

    // Verify assumptions about hard-codes indexes
    assertThat("The list of metacard IDs has changed sized", METACARD_IDS, arrayWithSize(2));

    CatalogBackupPlugin plugin = getPlugin();
    DeleteResponse mockDeleteResponse = getDeleteResponse(Arrays.asList(METACARD_IDS));

    // Perform Test
    plugin.process(getCreateResponse(new String[] {METACARD_IDS[0]}));
    plugin.process(mockDeleteResponse);
  }

  @Test
  public void testUpdateResponseUpdateSuccessful() throws Exception {
    // Setup
    int subDirLevels = 3;
    CatalogBackupPlugin plugin = getPlugin(3);
    plugin.process(getCreateResponse(METACARD_IDS));
    UpdateResponse mockUpdateResponse = getUpdateResponse(Arrays.asList(METACARD_IDS));

    // Perform Test
    UpdateResponse postPluginUpdateResponse = plugin.process(mockUpdateResponse);

    // Verify
    assertThat(postPluginUpdateResponse, is(notNullValue()));

    IntStream.range(0, METACARD_IDS.length)
        .forEach(
            index -> {
              String oldId = METACARD_IDS[index];

              // Perform test
              File updatedFile = getFileFor(oldId, subDirLevels);
              Metacard updatedCard = readMetacard(updatedFile);

              // Verify expected file exists
              assertThat(updatedFile.exists(), is(Boolean.TRUE));

              // Verify that the metacard id has not changed
              assertThat(updatedCard.getId(), is(oldId));

              // Verify that the metacard title has been updated to "newTitle" + index
              assertThat(
                  updatedCard.getAttribute(Metacard.TITLE).getValue(), is(BASE_NEW_TITLE + index));
            });
  }

  @Test
  public void testUpdatingAllMissingMetacards() {
    // Setup
    UpdateResponse mockUpdateResponse = getUpdateResponse(Arrays.asList(METACARD_IDS));

    // Perform Test
    getPlugin().process(mockUpdateResponse);
  }

  @Test
  public void testUpdatingOneMissingMetacard() throws PluginExecutionException {
    assertThat("The list of metacard IDs has changed sized", METACARD_IDS, arrayWithSize(2));
    // Setup
    CatalogBackupPlugin plugin = getPlugin();
    plugin.process(getCreateResponse(new String[] {METACARD_IDS[0]}));
    UpdateResponse mockUpdateResponse = getUpdateResponse(Arrays.asList(METACARD_IDS));

    // Perform Test
    plugin.process(mockUpdateResponse);
  }

  @Test
  public void testShutdownAndTermination() throws InterruptedException {

    CatalogBackupPlugin plugin = getSpecialMock();
    ExecutorService executor = plugin.getExecutor();
    when(executor.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(true);

    plugin.shutdown();

    verify(executor).shutdown();
    verify(executor).awaitTermination(plugin.getTerminationTimeoutSeconds(), TimeUnit.SECONDS);
    verify(executor).shutdownNow();
  }

  @Test
  public void testInterruptedExecution() throws InterruptedException {
    CatalogBackupPlugin plugin = getSpecialMock();
    ExecutorService executor = plugin.getExecutor();
    when(executor.awaitTermination(anyLong(), any(TimeUnit.class)))
        .thenThrow(new InterruptedException());

    try {
      plugin.shutdown();
      fail("Should have thrown exception");
    } catch (IllegalStateException e) {
    }
  }

  /** Helper Methods */
  private List<Metacard> getMetacards(String[] ids, String title) {

    return Arrays.asList(ids).stream().map(m -> getMetacard(m, title)).collect(Collectors.toList());
  }

  private CreateResponse getCreateResponse(String[] ids) {

    CreateResponse mockCreateResponse = mock(CreateResponse.class);
    when(mockCreateResponse.getCreatedMetacards()).thenReturn(getMetacards(ids, BASE_OLD_TITLE));
    when(mockCreateResponse.getRequest()).thenReturn(mock(CreateRequest.class));
    return mockCreateResponse;
  }

  private DeleteResponse getDeleteResponse(List<String> metacardIds) {
    MetacardType mockMetacardType = mock(MetacardType.class);
    when(mockMetacardType.getName()).thenReturn(MetacardType.DEFAULT_METACARD_TYPE_NAME);

    List<Metacard> deletedMetacards = new ArrayList<>(metacardIds.size());

    for (String metacardId : metacardIds) {
      Metacard mockMetacard = mock(Metacard.class);
      when(mockMetacard.getId()).thenReturn(metacardId);
      when(mockMetacard.getMetacardType()).thenReturn(mockMetacardType);

      deletedMetacards.add(mockMetacard);
    }
    DeleteRequest request = mock(DeleteRequest.class);
    DeleteResponse mockDeleteResponse = mock(DeleteResponse.class);
    when(mockDeleteResponse.getDeletedMetacards()).thenReturn(deletedMetacards);
    when(mockDeleteResponse.getRequest()).thenReturn(request);

    return mockDeleteResponse;
  }

  private UpdateResponse getUpdateResponse(List<String> oldMetacardIds) {
    List<Update> updatedMetacards = new ArrayList<>(oldMetacardIds.size());
    int i = 0;
    for (String oldMetacardId : oldMetacardIds) {
      Metacard oldMetacard = getMetacard(oldMetacardId, BASE_OLD_TITLE + i);
      Metacard newMetacard = getMetacard(oldMetacardId, BASE_NEW_TITLE + i);

      // Create UpdateResponse
      Update mockUpdate = mock(Update.class);
      when(mockUpdate.getOldMetacard()).thenReturn(oldMetacard);
      when(mockUpdate.getNewMetacard()).thenReturn(newMetacard);
      updatedMetacards.add(mockUpdate);

      i++;
    }
    UpdateRequest request = mock(UpdateRequest.class);
    UpdateResponse mockUpdateResponse = mock(UpdateResponse.class);
    when(mockUpdateResponse.getUpdatedMetacards()).thenReturn(updatedMetacards);
    when(mockUpdateResponse.getRequest()).thenReturn(request);
    return mockUpdateResponse;
  }

  private Metacard getMetacard(String metacardId, String title) {
    Metacard metacard = new MetacardImpl();
    metacard.setAttribute(new AttributeImpl(Metacard.ID, metacardId));
    metacard.setAttribute((new AttributeImpl(Metacard.TITLE, title)));

    return metacard;
  }

  private String getMetacardPath(String metacardId, int subDirLevels) {
    StringBuilder builder = new StringBuilder();

    if (subDirLevels < 0) {
      subDirLevels = 0;
    } else if (metacardId.length() == 1 || metacardId.length() < subDirLevels * 2) {
      subDirLevels = (int) Math.floor(metacardId.length() / 2);
    }

    for (int i = 0; i < subDirLevels; i++) {
      builder.append(File.separator);
      builder.append(metacardId.substring(i * 2, i * 2 + 2));
    }

    builder.append(File.separator);

    return builder.toString();
  }

  private Metacard readMetacard(File file) {

    Metacard metacard = null;
    try (FileInputStream fis = new FileInputStream(file);
        ObjectInputStream ois = new ObjectInputStream(fis)) {
      metacard = (Metacard) ois.readObject();
    } catch (ClassNotFoundException | IOException e) {
      fail();
    }

    return metacard;
  }

  private File getFileFor(String metacardId, int subDirLevels) {
    return new File(
        backupDirObject.getRoot().getAbsolutePath()
            + getMetacardPath(metacardId, subDirLevels)
            + metacardId);
  }

  private void assertFilesDoNotExist(String[] metacardIds, int subDirLevels) {
    assertFiles(Boolean.FALSE, metacardIds, subDirLevels);
  }

  private void assertFilesExist(String[] metacardIds, int subDirLevels) {
    assertFiles(Boolean.TRUE, metacardIds, subDirLevels);
  }

  private void assertFiles(Boolean test, String[] metacardIds, int subDirLevels) {
    for (String id : metacardIds) {
      assertThat(getFileFor(id, subDirLevels).exists(), is(test));
    }
  }

  private CatalogBackupPlugin getPlugin() {

    return getPlugin(3);
  }

  private CatalogBackupPlugin getPlugin(int level) {
    CatalogBackupPlugin plugin = new CatalogBackupPlugin();
    plugin.setExecutor(getSynchronousExecutor());
    plugin.setRootBackupDir(backupDirObject.getRoot().getAbsolutePath());
    plugin.setSubDirLevels(level);
    return plugin;
  }

  private CatalogBackupPlugin getSpecialMock() {

    CatalogBackupPlugin plugin = getPlugin();
    List<Runnable> failedItems = new ArrayList<>();
    failedItems.add(() -> {});
    ExecutorService executor = mock(ExecutorService.class);
    plugin.setTerminationTimeoutSeconds((long) 0);
    plugin.setExecutor(executor);
    when(executor.isShutdown()).thenReturn(false);
    when(executor.shutdownNow()).thenReturn(failedItems);
    return plugin;
  }

  private ExecutorService getSynchronousExecutor() {
    // Create an executor that synchronously executes the task.
    return new ThreadPoolExecutor(
        1,
        1,
        1,
        TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(),
        StandardThreadFactoryBuilder.newThreadFactory("catalogBackupPluginThread")) {
      @Override
      public void execute(Runnable command) {
        command.run();
      }
    };
  }
}
