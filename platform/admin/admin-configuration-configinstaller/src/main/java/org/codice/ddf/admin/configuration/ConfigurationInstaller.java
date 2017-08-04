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
package org.codice.ddf.admin.configuration;

import static org.osgi.service.cm.ConfigurationEvent.CM_DELETED;
import static org.osgi.service.cm.ConfigurationEvent.CM_UPDATED;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.codice.ddf.admin.configurator.Configurator;
import org.codice.ddf.admin.configurator.ConfiguratorException;
import org.codice.ddf.admin.configurator.ConfiguratorFactory;
import org.codice.ddf.admin.configurator.Operation;
import org.codice.ddf.admin.configurator.OperationReport;
import org.codice.ddf.admin.configurator.Result;
import org.codice.ddf.internal.admin.configurator.actions.ConfigActions;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.SynchronousConfigurationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Addresses shortcomings of the Felix {@code ConfigInstaller} with our own implementation that properly
 * syncs the etc directory using the {@code felix.fileinstall.filename} property.
 *
 * @see #handleUpdate(ConfigurationEvent)
 * @see #handleDelete(ConfigurationEvent)
 */
public class ConfigurationInstaller implements SynchronousConfigurationListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationInstaller.class);

    private final ConfigurationAdmin configurationAdmin;

    private final ConfiguratorFactory configuratorFactory;

    private final ConfigActions configActions;

    private final Map<String, File> pidFileMap;

    public ConfigurationInstaller(ConfigurationAdmin configurationAdmin,
            ConfiguratorFactory configuratorFactory, ConfigActions configActions) {
        this.configurationAdmin = configurationAdmin;
        this.pidFileMap = new ConcurrentHashMap<>();

        this.configuratorFactory = configuratorFactory;
        this.configActions = configActions;
    }

    /**
     * @return a read-only map of configuration pids to felix file install properties.
     */
    Map<String, File> getPidFileMap() {
        return Collections.unmodifiableMap(pidFileMap);
    }

    public void init() throws IOException, InvalidSyntaxException {
        Configuration[] configs = configurationAdmin.listConfigurations(null);
        if (configs == null) {
            return;
        }
        pidFileMap.putAll(Arrays.stream(configs)
                .map(FelixConfig::new)
                .filter(config -> config.getFelixFile() != null)
                .collect(Collectors.toMap(FelixConfig::getPid, FelixConfig::getFelixFile,
                        // API guarantees no duplicates in production but itests may violate this.
                        // Workaround is to shut off admin-configuration-passwordencryption.
                        (file1, file2) -> file2)));
    }

    @Override
    public void configurationEvent(ConfigurationEvent event) {
        String pid = event.getPid();
        switch (event.getType()) {
        case CM_UPDATED:
            LOGGER.debug("Handling update for pid {}", pid);
            handleUpdate(event);
            break;
        case CM_DELETED:
            LOGGER.debug("Handling delete for pid {}", pid);
            handleDelete(event);
            break;
        default:
            LOGGER.debug("Unknown configuration event type, taking no action for pid {}", pid);
        }
    }

    /**
     * Sync the config files in etc when an update occurs. There are four cases, which will be
     * denoted using 2-tuple combinations of variables: [felixFileOriginal, felixFileNew]
     * <p>
     * [null, null]
     * (1) Felix file prop wasn't tracked and is still not tracked. We don't need to
     * do anything, and we further guarantee that beyond this point, one variable must not be null.
     * <p>
     * [null, X]
     * (2) Unrecognized config with a felix file prop to track. The config's presence in
     * etc was previously not being tracked by the system and we should start tracking it.
     * <p>
     * [Y, null]
     * [Y, X]
     * (3) Felix prop was externally changed or removed. Revert this change.
     * <p>
     * [Y, Y]
     * (4) Felix file prop did not change, do nothing. This is the happy path.
     *
     * @param event the configuration event for which an update is needed
     */
    private void handleUpdate(ConfigurationEvent event) {
        Configuration configuration = getConfiguration(event);
        if (configuration == null) {
            LOGGER.debug("Configuration was null for pid {}", event.getPid());
            return;
        }

        FelixConfig felixConfig = new FelixConfig(configuration);
        String pid = felixConfig.getPid();
        File felixFileNew = felixConfig.getFelixFile();
        File felixFileOriginal = pidFileMap.get(pid);

        // Config not tracked and will not be tracked? (1)
        if (felixFileOriginal == null && felixFileNew == null) {
            LOGGER.debug("Was not tracked and will not track pid {}", pid);
            return;
        }

        boolean filePropChanged = felixConfig.filePropChanged(felixFileOriginal);

        // Felix file prop did not change? (4)
        if (filePropChanged) {
            // Are we seeing the config for the first time? (2) (3)
            if (felixFileOriginal != null) {
                try {
                    felixConfig.setFelixFile(felixFileOriginal);
                } catch (IOException e) {
                    LOGGER.error("Could not set felix file name, error writing to config admin: ",
                            e);
                }
                return;
            }
            pidFileMap.put(pid, felixFileNew);
            LOGGER.debug("Tracking pid {}", pid);
        }
    }

    /**
     * Remove config files from etc when a delete occurs but only if felix is tracking the file.
     *
     * @param event the configuration event for which a delete is needed
     */
    private void handleDelete(ConfigurationEvent event) {
        String pid = event.getPid();
        File felixFileOriginal = pidFileMap.remove(pid);
        if (felixFileOriginal != null) {
            LOGGER.debug("Deleting file because config was deleted for pid {}", pid);
            try {
                performOperation(configActions.delete(felixFileOriginal.toPath()));
            } catch (IllegalArgumentException | ConfiguratorException e) {
                LOGGER.debug("Problem deleting config file: ", e);
            }
        }
    }

    private void performOperation(Operation operation) {
        Configurator configurator = configuratorFactory.getConfigurator();
        configurator.add(operation);
        OperationReport report = configurator.commit("Synchronizing the etc directory");
        if (report.containsFailedResults()) {
            LOGGER.error("One or more file operations failed, see debug logs for more info");
            if (LOGGER.isDebugEnabled()) {
                report.getFailedResults()
                        .stream()
                        .map(Result::getError)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .map(Exception.class::cast)
                        .forEach(e -> LOGGER.debug("Failed file operation: ", e));
            }
        }
    }

    private Configuration getConfiguration(ConfigurationEvent configurationEvent) {
        try {
            return configurationAdmin.getConfiguration(configurationEvent.getPid(), null);
        } catch (IOException e) {
            LOGGER.debug("Problem reading from config admin: ", e);
            return null;
        }
    }
}