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
package org.codice.ddf.configuration.listener;

import java.io.IOException;
import java.util.Collection;
import java.util.Dictionary;

import org.codice.ddf.configuration.store.ChangeListener;
import org.codice.ddf.configuration.store.FileHandler;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class listens for changes in configuration files and updates configAdmin with the changes
 */
public class ConfigurationFileListener implements ChangeListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationFileListener.class);

    private final FileHandler fileHandler;

    private final ConfigurationAdmin configAdmin;

    public ConfigurationFileListener(FileHandler fileHandler, ConfigurationAdmin configAdmin) {
        this.fileHandler = fileHandler;
        this.configAdmin = configAdmin;
    }

    /**
     * Iterates over the entire configuration directory at the beginning to push all the files through ConfigAdmin
     */
    public void init() {
        Collection<String> filePids = fileHandler.getConfigurationPids();

        for (String filePid : filePids) {
            updateConfig(filePid);
        }

        fileHandler.registerForChanges(this);
    }

    /**
     * Called by the ConfigurationFilesPoller when there is an update to a config file.
     * Determines what action to take based on the change type.
     *
     * @param filePid pid of the configuration file
     * @param changeType whether the change was an update, creation, or deletion
     */
    @Override
    public void update(String filePid, ChangeType changeType) {
        try {
            switch (changeType) {
            case CREATED:
            case UPDATED:
                updateConfig(filePid);
                break;
            case DELETED:
                deleteConfig(filePid);
                break;
            }
        } catch (RuntimeException e) {
            LOGGER.error("A runtime exception occured", e);
        }

    }

    /*
     * Reads the properties from the config file and makes the same change in configAdmin
     */
    private void updateConfig(String filePid) {
        Dictionary<String, Object> props;
        props = fileHandler.read(filePid);
        LOGGER.debug("Updating configuration for file PID [{}].", filePid);

        try {
            Configuration configuration = configAdmin.getConfiguration(filePid, null);
            configuration.update(props);
        } catch (IOException ex) {
            LOGGER.error("[{}] configuration could not be found, or failed to update.", filePid,
                    ex);
        }
    }

    /*
     * Deletes configuration from configAdmin when the config file is deleted
     */
    private void deleteConfig(String filePid) {
        try {
            configAdmin.getConfiguration(filePid, null).delete();
            LOGGER.debug("[{}] was deleted successfully.", filePid);
        } catch (IOException ex) {
            LOGGER.error("There was an issue deleting [{}].", filePid, ex);
        }
    }
}
