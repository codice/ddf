/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.catalog.backup;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PostIngestPlugin;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class CatalogBackupPlugin implements PostIngestPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogBackupPlugin.class);

    private static final String TEMP_FILE_EXTENSION = ".tmp";

    private File rootBackupDir;

    private int subDirLevels;

    public CatalogBackupPlugin() {
        subDirLevels = 0;
    }

    @Override
    public CreateResponse process(CreateResponse input) throws PluginExecutionException {
        LOGGER.debug("Performing backup of metacards in CreateResponse.");

        if(rootBackupDir == null) {
            throw new PluginExecutionException("No root backup directory configured in " + CatalogBackupPlugin.class.getName() + ".");
        }

        List<String> errors = new ArrayList<String>();

        List<Metacard> metacards = input.getCreatedMetacards();

        for(Metacard metacard : metacards) {
            try {
                backupMetacard(metacard);
            } catch(IOException e) {
                errors.add(metacard.getId());
            }
        }

        if(errors.size() > 0) {
            throw new PluginExecutionException("Error processing CreateResponse. Unable to backup metacards [" + StringUtils.join(errors, ",") + "].");
        }

        return input;
    }

    @Override
    public UpdateResponse process(UpdateResponse input) throws PluginExecutionException {
        LOGGER.debug("Updating metacards contained in UpdateRespone in backup.");

        if(rootBackupDir == null) {
            throw new PluginExecutionException("No root backup directory configured in " + CatalogBackupPlugin.class.getName() + ".");
        }

        List<String> deleteErrors = new ArrayList<String>();
        List<String> backupErrors = new ArrayList<String>();

        List<Update> updates = input.getUpdatedMetacards();

        for (Update update : updates) {
            try {
                deleteMetacard(update.getOldMetacard());
            } catch(IOException e) {
                deleteErrors.add(update.getOldMetacard().getId());
            }

            try {
                backupMetacard(update.getNewMetacard());
            } catch (IOException e) {
                backupErrors.add(update.getNewMetacard().getId());
            }
        }

        String exceptionMsg = null;

        if(deleteErrors.size() > 0 || backupErrors.size() > 0) {
            exceptionMsg = getExceptionMessage(deleteErrors, backupErrors);
        }

        if(exceptionMsg != null) {
            throw new PluginExecutionException("Error processing UpdateResponse. " + exceptionMsg);
        }

        return input;
    }

    @Override
    public DeleteResponse process(DeleteResponse input) throws PluginExecutionException {
        LOGGER.debug("Deleting metacards contained in DeleteResponse from backup.");

        if(rootBackupDir == null) {
            throw new PluginExecutionException("No root backup directory configured in " + CatalogBackupPlugin.class.getName() + ".");
        }

        List<String> errors = new ArrayList<String>();

        List<Metacard> metacards = input.getDeletedMetacards();

        for(Metacard metacard : metacards) {
            try {
                deleteMetacard(metacard);
            } catch(IOException e) {
                errors.add(metacard.getId());
            }
        }

        if(errors.size() > 0) {
            throw new PluginExecutionException("Error processing DeleteResponse. Unable to delete metacards ["
                    + StringUtils.join(errors, ",") + "] from backup.");
        }

        return input;
    }

    public void setRootBackupDir(String dir) {
        if (StringUtils.isBlank(dir)) {
            LOGGER.error("The root backup directory is blank.");
            return;
        }

        this.rootBackupDir = new File(dir);
        LOGGER.debug("Set root backup directory to: {}", this.rootBackupDir.toString());
    }

    public void setSubDirLevels(int levels) {
        this.subDirLevels = levels;
        LOGGER.debug("Set subdirectory levels to: {}", this.subDirLevels);
    }

    private void backupMetacard(Metacard metacard) throws IOException {

        // Write metacard to a temp file.  When write is complete, rename (remove temp extension).
        File tempFile = getTempFile(metacard);

        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(tempFile));

        try {
            LOGGER.debug("Writing temp metacard [{}] to [{}].", tempFile.getName(), tempFile.getParent());
            oos.writeObject(new MetacardImpl(metacard));
        } finally {
            oos.flush();
            oos.close();
        }

        removeTempExtension(tempFile);
    }

    private void deleteMetacard(Metacard metacard) throws IOException {
        File metacardToDelete = getBackupFile(metacard);
        FileUtils.forceDelete(metacardToDelete);
    }

    private File getTempFile(Metacard metacard) throws IOException {
        return new File(getBackupFile(metacard).getAbsolutePath() + TEMP_FILE_EXTENSION);
    }

    private File getBackupFile(Metacard metacard) throws IOException {

        String metacardId = metacard.getId();
        File parent = rootBackupDir;
        int levels = this.subDirLevels;

        if(this.subDirLevels < 0) {
            levels = 0;
        } else if (metacardId.length() == 1 || metacardId.length() < this.subDirLevels * 2) {
            levels = (int) Math.floor(metacardId.length()/2);
        }

        for (int i = 0; i < levels; i++) {
            parent = new File(parent, metacardId.substring(i * 2, i * 2 + 2));
            FileUtils.forceMkdir(parent);
        }

        LOGGER.debug("Backup directory for metacard  [{}] is [{}].", metacard.getId(), parent.getAbsolutePath());
        return new File(parent, metacardId);
    }

    private void removeTempExtension(File source) throws IOException {
        LOGGER.debug("Removing {} file extension.", TEMP_FILE_EXTENSION);
        File destination = new File(StringUtils.removeEnd(source.getAbsolutePath(), TEMP_FILE_EXTENSION));
        FileUtils.moveFile(source, destination);
        LOGGER.debug("Moved {} to {}.", source.getAbsolutePath(), destination.getAbsolutePath());
    }

    private String getExceptionMessage(List<String> deleteErrors, List<String> backupErrors) {
        String deleteErrorMsg = null;
        if(deleteErrors.size() > 0) {
            deleteErrorMsg = "Error processing UpdateResponse. Unable to delete metacards [" + StringUtils.join(deleteErrors, ",") + "] from backup. ";
        }

        String backupErrorMsg = null;
        if(backupErrors.size() > 0) {
            backupErrorMsg = "Error processing UpdateResponse. Unable to backup metacards [" + StringUtils.join(deleteErrors, ",") + "]. ";
        }

        String exceptionMsg = null;

        if(deleteErrorMsg != null) {
            exceptionMsg = deleteErrorMsg;
        }

        if(backupErrorMsg != null) {
            exceptionMsg+=backupErrorMsg;
        }

        return exceptionMsg;
    }
}
