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
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

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
                .map(this::processFelixConfig)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(FelixConfig::getPid, FelixConfig::getFelixFile,
                        // API guarantees no duplicates in production but itests may violate this.
                        // Workaround is to provide the following merge function just in case.
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
     * Syncs the state of a config in {@link ConfigurationAdmin} with the state of the etc directory.
     *
     * @param config the config under scrutiny.
     * @return the config if etc claims it should exist, or null if it was deleted.
     */
    @Nullable
    private FelixConfig processFelixConfig(FelixConfig config) {
        File file = config.getFelixFile();
        if (file != null) {
            if (!file.exists()) {
                config.delete();
                return null;
            }
            return config;
        }

        file = config.generateDefaultFelixFile()
                .getFelixFile();
        if (file == null) {
            return null;
        }

        performOperation(configActions.create(file.toPath(), config.getSanitizedProperties()));
        return config;
    }

    /**
     * Sync the config files in etc when an update occurs. There are four cases, which will be
     * denoted using 2-tuple combinations of variables: [felixFileOriginal, felixFileNew]
     * <p>
     * [null, null]
     * (1) Felix file prop is not being tracked. Generate a felix file prop so
     * the config can be tracked. This case boils down to case (2) every time.
     * <p>
     * [null, X]
     * (2) Brand new config with a felix file prop to track, either generated from felix or by this
     * installer. Create the file in etc and start tracking the config in this installer's cache.
     * <p>
     * [Y, null]
     * [Y, X]
     * (3) Felix prop was externally changed or removed. Revert this change. This case boils down to
     * case (4) every time.
     * <p>
     * [Y, Y]
     * (4) Regardless if the Felix file prop changed, the config properties were updated, so
     * update the corresponding file in etc.
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

        if (felixFileOriginal == null) {
            // Config not tracked? (1)
            if (felixFileNew == null) {
                felixFileNew = felixConfig.generateDefaultFelixFile()
                        .getFelixFile();
            }

            // We are seeing the config for the first time (2)
            LOGGER.debug("Tracking pid {}", pid);
            pidFileMap.put(pid, felixFileNew);
            performOperation(configActions.create(felixFileNew.toPath(),
                    felixConfig.getSanitizedProperties()));
            return;
        }

        // Felix file prop changed and we should revert it? (3)
        if (felixConfig.filePropChanged(felixFileOriginal)) {
            felixConfig.setFelixFile(felixFileOriginal);
        }

        // Respond to config properties being updated (4)
        // Updating properties on disk, if they haven't changed, causes an infinite loop
        // with the felix DirectoryWatcher
        Path path = felixFileOriginal.toPath();
        Dictionary<String, Object> fileState = configActions.getProperties(path);
        Dictionary<String, Object> configState = felixConfig.getSanitizedProperties();
        if (!equalDictionaries(configState, fileState)) {
            performOperation(configActions.update(path, configState, false));
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
        if (felixFileOriginal != null && felixFileOriginal.exists()) {
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
        OperationReport report = configurator.commit("Synchronizing the etc directory. {}",
                operation.toString());
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

    private static boolean equalDictionaries(Dictionary x, Dictionary y) {
        if (x.size() != y.size()) {
            return false;
        }
        for (final Enumeration e = x.keys(); e.hasMoreElements(); ) {
            final Object key = e.nextElement();
            if (!Objects.deepEquals(x.get(key), y.get(key))) {
                return false;
            }
        }
        return true;
    }
}