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
package ddf.catalog.backup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.plugin.PostIngestPlugin;

/**
 * The CatalogBackupPlugin backups up metacards to the file system. It is a
 * PostIngestPlugin, so it processes CreateResponses, DeleteResponses, and
 * UpdateResponses.
 * <p>
 * The root backup directory and subdirectory levels can be configured in the
 * Backup Post-Ingest Plugin section in the admin console.
 * <p>
 * This feature can be installed/uninstalled with the following commands:
 * <p>
 * ddf@local>features:install catalog-core-backupplugin
 * ddf@local>features:uninstall catalog-core-backupplugin
 */

public class CatalogBackupPlugin implements PostIngestPlugin {

    public static final String CREATE = "CREATE";

    public static final String DELETE = "DELETE";

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogBackupPlugin.class);

    private static final String TEMP_FILE_EXTENSION = ".tmp";

    private long terminationTimeoutSeconds;

    private int subDirLevels;

    private File rootBackupDir;

    private ExecutorService executor;

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
        int size = input.getUpdatedMetacards()
                .size();
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
     * Sets the number of subdirectory levels to create. Two characters from
     * each metacard ID will be used to name each subdirectory level.
     *
     * @param levels number of subdirectory levels to create
     */
    public void setSubDirLevels(int levels) {
        Validate.isTrue(levels >= 0,
                "Depth of directory hierarchy for the catalog backup plugin must be zero or greater. Actual vaule was ",
                levels);
        this.subDirLevels = levels;
    }

    /**
     * @throws IllegalStateException will be thrown if the tasks in the queue have not
     *                               completed before the awaitTermination message times out
     */
    public void shutdown() {

        getExecutor().shutdown();
        if (!executor.isShutdown()) {
            try {
                executor.awaitTermination(getTerminationTimeoutSeconds(), TimeUnit.SECONDS);
                // If the remaining jobs in the queue are not completed TERMINATION_TIMEOUT elapses
                // The JVM will throw an Interrupted Exception.
            } catch (InterruptedException e) {
                LOGGER.warn(
                        "Backup of metacards interrupted. Some metacards might not be backed up.");
                throw new IllegalStateException(e);
            }
            final List<Runnable> failures = executor.shutdownNow();
            if (failures.size() > 0) {
                LOGGER.warn(
                        "Cancelled tasks to backup metacards. Some metacards might not be backed up.");
            }
        }
    }

    /**
     * Sets the root file system backup directory.
     *
     * @param dir absolute path for the root file system backup directory.
     */
    public void setRootBackupDir(String dir) {
        Validate.notNull(dir);
        File directory = new File(dir);
        validateDirectory(directory);
        this.rootBackupDir = directory;
    }

    ExecutorService getExecutor() {
        return executor;
    }

    void setExecutor(ExecutorService executor) {
        this.executor = executor;
    }

    private synchronized void execute(Runnable task) {

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
            LOGGER.debug(getExceptionMessage(errors, CREATE));
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

        if (errors.size() > 0) {
            LOGGER.warn(getExceptionMessage(errors, DELETE));
        }
    }

    private void removeTempExtension(File source) throws IOException {
        File destination = new File(StringUtils.removeEnd(source.getAbsolutePath(),
                TEMP_FILE_EXTENSION));
        boolean success = source.renameTo(destination);
        if (!success) {
            LOGGER.warn("Failed to move {} to {}.",
                    source.getAbsolutePath(),
                    destination.getAbsolutePath());
        }
    }

    private String getExceptionMessage(List<String> metacardsIdsInError, String operation) {

        return "Catalog Backup Plugin processing error." + " " + "Unable to " + operation
                + " metacard(s) [" + StringUtils.join(metacardsIdsInError, ",") + "]. ";
    }

    private void createFile(Metacard metacard) throws IOException {

        // Write metacard to a temp file. When write is complete, rename (remove
        // temp extension).
        File tempFile = getTempFile(metacard);

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(tempFile))) {
            oos.writeObject(new MetacardImpl(metacard));
        }

        removeTempExtension(tempFile);
    }

    /**
     * Deletes metacard from file system backup.
     *
     * @param metacard the metacard to be deleted.
     * @throws IOException
     */
    private void deleteFile(Metacard metacard) throws IOException {
        File metacardToDelete = getBackupFile(metacard);
        FileUtils.forceDelete(metacardToDelete);
    }

    /**
     * While metacards are being written to the file system for backup, they are
     * written to temp files. Each temp file is renamed when the write is
     * complete. This makes it easy to find and remove failed files.
     *
     * @param metacard the metacard to create a temp file for.
     * @return File
     * @throws IOException
     */
    private File getTempFile(Metacard metacard) throws IOException {
        return new File(getBackupFile(metacard).getAbsolutePath() + TEMP_FILE_EXTENSION);
    }

    private File getBackupFile(Metacard metacard) throws IOException {

        String metacardId = metacard.getId();
        File parent = rootBackupDir;
        int levels = this.subDirLevels;

        if (metacardId.length() == 1 || metacardId.length() < levels * 2) {
            levels = (int) Math.floor(metacardId.length() / 2);
        }

        for (int i = 0; i < levels; i++) {
            parent = new File(parent, metacardId.substring(i * 2, i * 2 + 2));
            FileUtils.forceMkdir(parent);
        }

        return new File(parent, metacardId);
    }

    private void validateDirectory(File directory) {

        if (!(directory.isDirectory() && directory.canWrite())) {
            throw new IllegalArgumentException("Directory " + directory.getAbsolutePath()
                    + " does not exist or is not writable");
        }
    }

    public long getTerminationTimeoutSeconds() {
        return terminationTimeoutSeconds;
    }

    public void setTerminationTimeoutSeconds(long terminationTimeoutSeconds) {
        this.terminationTimeoutSeconds = terminationTimeoutSeconds;
    }
}

