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

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.plugin.PostIngestPlugin;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.codice.ddf.configuration.AbsolutePathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The CatalogBackupPlugin backups up metacards to the file system. It is a PostIngestPlugin, so it
 * processes CreateResponses, DeleteResponses, and UpdateResponses.
 *
 * <p>The root backup directory and subdirectory levels can be configured in the Backup Post-Ingest
 * Plugin section in the admin console.
 *
 * <p>This feature can be installed/uninstalled with the following commands:
 *
 * <p>ddf@local>feature:install catalog-core-backupplugin ddf@local>feature:uninstall
 * catalog-core-backupplugin
 */
public class CatalogBackupPlugin implements PostIngestPlugin {

  public static final String CREATE = "CREATE";

  public static final String DELETE = "DELETE";

  private static final Logger LOGGER = LoggerFactory.getLogger(CatalogBackupPlugin.class);

  private static final String TEMP_FILE_EXTENSION = ".tmp";

  private long terminationTimeoutSeconds;

  private int subDirLevels = 0;

  private String rootBackupDir;

  private ExecutorService executor;

  private File rootDirOjbect;

  /**
   * Backs up created metacards to the file system backup.
   *
   * @param input the {@link CreateResponse} to process
   * @return {@link CreateResponse}
   */
  @Override
  public CreateResponse process(CreateResponse input) {

    execute(() -> create(input.getCreatedMetacards()));
    return input;
  }

  /**
   * Backs up updated metacards to the file system backup.
   *
   * @param input the {@link UpdateResponse} to process
   * @return {@link UpdateResponse}
   */
  @Override
  public UpdateResponse process(UpdateResponse input) {
    int size = input.getUpdatedMetacards().size();
    List<Metacard> toDelete = new ArrayList<>(size);
    List<Metacard> toCreate = new ArrayList<>(size);
    for (Update update : input.getUpdatedMetacards()) {
      toDelete.add(update.getOldMetacard());
      toCreate.add(update.getNewMetacard());
    }
    execute(() -> delete(toDelete));
    execute(() -> create(toCreate));
    return input;
  }

  /**
   * Removes deleted metacards from the file system backup.
   *
   * @param input the {@link DeleteResponse} to process
   * @return {@link DeleteResponse}
   */
  @Override
  public DeleteResponse process(DeleteResponse input) {

    execute(() -> delete(input.getDeletedMetacards()));
    return input;
  }

  /**
   * @throws IllegalStateException will be thrown if the tasks in the queue have not completed
   *     before the awaitTermination message times out
   */
  public void shutdown() {

    getExecutor().shutdown();
    if (!executor.isShutdown()) {
      List<Runnable> failures;
      try {
        if (!executor.awaitTermination(getTerminationTimeoutSeconds(), TimeUnit.SECONDS)) {

          if (!executor.awaitTermination(getTerminationTimeoutSeconds(), TimeUnit.SECONDS)) {
            LOGGER.error("Executor service did not terminate.");
          }
        }
      } catch (InterruptedException e) {
        LOGGER.warn("Backup of metacards interrupted. Some metacards might not be backed up.");
        Thread.currentThread().interrupt();
        throw new IllegalStateException(e);
      } finally {
        failures = executor.shutdownNow();
      }

      if (!failures.isEmpty()) {
        LOGGER.warn("Cancelled tasks to backup metacards. Some metacards might not be backed up.");
      }
    }
  }

  ExecutorService getExecutor() {
    return executor;
  }

  public void setExecutor(ExecutorService executor) {
    this.executor = executor;
  }

  private void execute(Runnable task) {

    executor.execute(task);
  }

  private void create(List<Metacard> metacards) {

    List<String> errors = new ArrayList<>();
    for (Metacard metacard : metacards) {
      try {
        createFile(metacard);
      } catch (RuntimeException | IOException e) {
        errors.add(metacard.getId());
      }
    }

    if (!errors.isEmpty()) {
      LOGGER.warn(getExceptionMessage(errors, CREATE));
    }
  }

  private void delete(List<Metacard> cards) {
    List<String> errors = new ArrayList<>();
    for (Metacard metacard : cards) {
      try {
        deleteFile(metacard);
      } catch (IOException | RuntimeException e) {
        errors.add(metacard.getId());
      }
    }

    if (!errors.isEmpty()) {
      LOGGER.warn(getExceptionMessage(errors, DELETE));
    }
  }

  private void renameTempFile(File source) {
    File destination =
        new File(StringUtils.removeEnd(source.getAbsolutePath(), TEMP_FILE_EXTENSION));
    boolean success = source.renameTo(destination);
    if (!success) {
      LOGGER.debug(
          "Failed to move {} to {}.", source.getAbsolutePath(), destination.getAbsolutePath());
    }
  }

  private String getExceptionMessage(List<String> metacardsIdsInError, String operation) {

    return "Catalog Backup Plugin processing error."
        + " "
        + "Unable to "
        + operation
        + " metacard(s) ["
        + StringUtils.join(metacardsIdsInError, ",")
        + "]. ";
  }

  private void createFile(Metacard metacard) throws IOException {

    // Metacards written to temp files. Each temp file is renamed when the write is
    // complete. This makes it easy to find and remove failed files.
    File tempFile = getFile(metacard.getId(), TEMP_FILE_EXTENSION);

    try (ObjectOutputStream oos = new ObjectOutputStream(FileUtils.openOutputStream(tempFile))) {
      oos.writeObject(new MetacardImpl(metacard));
    }

    renameTempFile(tempFile);
  }

  private void deleteFile(Metacard metacard) throws IOException {

    File metacardToDelete = getFile(metacard.getId(), "");
    FileUtils.forceDelete(metacardToDelete);
  }

  private File getFile(String id, String extension) throws IOException {

    int depth = getDepth(id.length());
    return new File(getCompleteDirectory(depth, id), id + extension);
  }

  private File getCompleteDirectory(int depth, String id) throws IOException {

    File parent = getRootDirObject();
    for (int i = 0; i < depth; i++) {
      parent = new File(parent, id.substring(i * 2, i * 2 + 2));
    }

    return parent;
  }

  private int getDepth(int idLength) {
    int levels = getSubDirLevels();
    if (idLength == 1 || idLength < getSubDirLevels() * 2) {
      levels = idLength / 2;
    }
    return levels;
  }

  private File getRootDirObject() throws IOException {
    if (rootDirOjbect == null) {
      Validate.notNull(getRootBackupDir());
      File directory = new File(getRootBackupDir());
      FileUtils.forceMkdir(directory);
      validateDirectory(directory);
      rootDirOjbect = directory;
    }
    return rootDirOjbect;
  }

  private void validateDirectory(File directory) {

    if (!(directory.isDirectory() && directory.canWrite())) {
      throw new IllegalArgumentException(
          "Directory " + directory.getAbsolutePath() + " does not exist or is not writable");
    }
  }

  public long getTerminationTimeoutSeconds() {
    return terminationTimeoutSeconds;
  }

  public void setTerminationTimeoutSeconds(long terminationTimeoutSeconds) {
    this.terminationTimeoutSeconds = terminationTimeoutSeconds;
  }

  public String getRootBackupDir() {
    return rootBackupDir;
  }

  /**
   * Sets the root file system backup directory. The directory will be created when it is needed. Do
   * not validate the existence of the directory until then.
   *
   * @param dir absolute path for the root file system backup directory.
   */
  public void setRootBackupDir(String dir) {

    rootBackupDir = new AbsolutePathResolver(dir).getPath();
    rootDirOjbect = null;
  }

  public int getSubDirLevels() {
    return subDirLevels;
  }

  /**
   * Sets the number of subdirectory levels to create. Two characters from each metacard ID will be
   * used to name each subdirectory level.
   *
   * @param levels number of subdirectory levels to create
   */
  public void setSubDirLevels(int levels) {
    Validate.isTrue(
        levels >= 0,
        "Depth of directory hierarchy for the catalog backup plugin must be zero or greater. Actual value was ",
        levels);
    this.subDirLevels = levels;
  }
}
