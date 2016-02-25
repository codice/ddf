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
package ddf.catalog.backup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

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
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PostIngestPlugin;
import ddf.catalog.util.impl.Requests;

/**
 * The CatalogBackupPlugin backups up metacards to the file system. It is a
 * PostIngestPlugin, so it processes CreateResponses, DeleteResponses, and
 * UpdateResponses.
 * <p/>
 * The root backup directory and subdirectory levels can be configured in the
 * Backup Post-Ingest Plugin section in the admin console.
 * <p/>
 * This feature can be installed/uninstalled with the following commands:
 * <p/>
 * ddf@local>features:install catalog-core-backupplugin
 * ddf@local>features:uninstall catalog-core-backupplugin
 */

public class CatalogBackupPlugin implements PostIngestPlugin {

    public static final String CREATE = "CREATE";

    public static final String DELETE = "DELETE";

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogBackupPlugin.class);

    private static final String TEMP_FILE_EXTENSION = ".tmp";

    private File rootBackupDir;

    private int subDirLevels;

    private PeriodicBatchExecutor<Metacard> executor;

    private boolean enableBackupPlugin;

    public CatalogBackupPlugin() {
        subDirLevels = 0;
        enableBackupPlugin = true;
    }

    @SuppressWarnings("unused")
    public void initialize() {
        getExecutor().setTask(this::backup);
    }

    /**
     * Backs up created metacards to the file system backup.
     *
     * @param input the {@link CreateResponse} to process
     * @return {@link CreateResponse}
     * @throws PluginExecutionException if fail to create metacards
     */
    @Override
    public synchronized CreateResponse process(CreateResponse input)
            throws PluginExecutionException {

        if (enableBackupPlugin && Requests.isLocal(input.getRequest())) {
            LOGGER.trace("Performing backup of metacards in CreateResponse.");
        }

        getExecutor().addAll(input.getCreatedMetacards());

        return input;

    }

    void backup(List<Metacard> metacards) {

        List<String> errors = new ArrayList<>();
        for (Metacard metacard : metacards) {
            try {
                backupMetacard(metacard);
            } catch (RuntimeException | IOException e) {
                errors.add(metacard.getId());
            }
        }

        if (!errors.isEmpty()) {
            LOGGER.info("Plugin processing failed. This is allowable. Skipping to next plugin.",
                    new PluginExecutionException(getExceptionMessage(CreateResponse.class.getSimpleName(),
                            null,
                            errors,
                            CREATE)));
        }
    }

    /**
     * Backs up updated metacards to the file system backup.
     *
     * @param input the {@link UpdateResponse} to process
     * @return {@link UpdateResponse}
     * @throws PluginExecutionException if failed to updated metacards
     */
    @Override
    public UpdateResponse process(UpdateResponse input) throws PluginExecutionException {
        if (enableBackupPlugin && Requests.isLocal(input.getRequest())) {
            LOGGER.trace("Updating metacards contained in UpdateResponse in backup.");

            List<String> deleteErrors = new ArrayList<>();
            List<String> backupErrors = new ArrayList<>();

            List<Update> updates = input.getUpdatedMetacards();

            for (Update update : updates) {
                try {
                    deleteMetacard(update.getOldMetacard());
                } catch (IOException e) {
                    deleteErrors.add(update.getOldMetacard()
                            .getId());
                }

                try {
                    backupMetacard(update.getNewMetacard());
                } catch (IOException e) {
                    backupErrors.add(update.getNewMetacard()
                            .getId());
                }
            }

            String exceptionMessage = null;

            if (!deleteErrors.isEmpty()) {
                exceptionMessage = getExceptionMessage(UpdateResponse.class.getSimpleName(),
                        null,
                        deleteErrors,
                        DELETE);
            }

            if (!backupErrors.isEmpty()) {
                exceptionMessage = getExceptionMessage(UpdateResponse.class.getSimpleName(),
                        exceptionMessage,
                        backupErrors,
                        CREATE);
            }

            if (!(deleteErrors.isEmpty() && backupErrors.isEmpty())) {
                throw new PluginExecutionException(exceptionMessage);
            }
        }
        return input;
    }

    /**
     * Removes deleted metacards from the file system backup.
     *
     * @param input the {@link DeleteResponse} to process
     * @return {@link DeleteResponse}
     * @throws PluginExecutionException if failed to delete metacards
     */
    @Override
    public DeleteResponse process(DeleteResponse input) throws PluginExecutionException {

        if (enableBackupPlugin && Requests.isLocal(input.getRequest())) {
            LOGGER.trace("Deleting metacards contained in DeleteResponse from backup.");

            List<String> errors = new ArrayList<>();
            for (Metacard metacard : input.getDeletedMetacards()) {
                try {
                    deleteMetacard(metacard);
                } catch (IOException e) {
                    errors.add(metacard.getId());
                }
            }

            if (errors.size() > 0) {
                throw new PluginExecutionException(getExceptionMessage(DeleteResponse.class.getSimpleName(),
                        null,
                        errors,
                        DELETE));
            }
        }
        return input;
    }

    @SuppressWarnings("unused")
    public void setEnableBackupPlugin(boolean enablePlugin) {
        enableBackupPlugin = enablePlugin;
    }

    /**
     * Sets the number of subdirectory levels to create. Two characters from
     * each metacard ID will be used to name each subdirectory level.
     *
     * @param levels number of subdirectory levels to create
     */
    public void setSubDirLevels(int levels) {
        this.subDirLevels = levels;
        LOGGER.debug("Set subdirectory levels to: {}", this.subDirLevels);
    }

    @SuppressWarnings("unused")
    public void shutdown() {
        getExecutor().shutdown();
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

        LOGGER.trace("Set root backup directory to: {}", this.rootBackupDir.toString());
    }

    public PeriodicBatchExecutor<Metacard> getExecutor() {
        return executor;
    }

    public void setExecutor(PeriodicBatchExecutor<Metacard> executor) {
        this.executor = executor;
    }

    private void removeTempExtension(File source) throws IOException {
        LOGGER.debug("Removing {} file extension.", TEMP_FILE_EXTENSION);
        File destination = new File(StringUtils.removeEnd(source.getAbsolutePath(),
                TEMP_FILE_EXTENSION));
        boolean success = source.renameTo(destination);
        if (success) {
            LOGGER.debug("Moved {} to {}.",
                    source.getAbsolutePath(),
                    destination.getAbsolutePath());
        } else {
            LOGGER.debug("Failed to move {} to {}.",
                    source.getAbsolutePath(),
                    destination.getAbsolutePath());
        }
    }

    private String getExceptionMessage(String responseType, String previousExceptionMessage,
            List<String> metacardsIdsInError, String operation) {

        StringBuilder builder = new StringBuilder();
        if (StringUtils.isNotBlank(previousExceptionMessage)) {
            builder.append(previousExceptionMessage)
                    .append(" ");
        }
        return builder.append("Error processing ")
                .append(responseType)
                .append(". ")
                .append("Unable to ")
                .append(operation)
                .append(" metacard(s) [")
                .append(StringUtils.join(metacardsIdsInError, ","))
                .append("]. ")
                .toString();
    }

    private void backupMetacard(Metacard metacard) throws IOException {

        // Write metacard to a temp file. When write is complete, rename (remove
        // temp extension).
        File tempFile = getTempFile(metacard);

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(tempFile))) {
            LOGGER.trace("Writing temp metacard [{}] to [{}].",
                    tempFile.getName(),
                    tempFile.getParent());
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
    private void deleteMetacard(Metacard metacard) throws IOException {
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

        if (this.subDirLevels < 0) {
            levels = 0;
        } else if (metacardId.length() == 1 || metacardId.length() < this.subDirLevels * 2) {
            levels = (int) Math.floor(metacardId.length() / 2);
        }

        for (int i = 0; i < levels; i++) {
            parent = new File(parent, metacardId.substring(i * 2, i * 2 + 2));
            FileUtils.forceMkdir(parent);
        }

        LOGGER.trace("Backup directory for metacard  [{}] is [{}].",
                metacard.getId(),
                parent.getAbsolutePath());
        return new File(parent, metacardId);
    }

    private void validateDirectory(File directory) {
        if (!(directory.exists() && directory.canWrite())) {
            throw new IllegalArgumentException("Directory " + directory.getAbsolutePath()
                    + " does not exist or is not writable");
        }
    }

}

