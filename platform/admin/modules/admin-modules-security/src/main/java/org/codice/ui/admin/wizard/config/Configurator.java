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
 **/
package org.codice.ui.admin.wizard.config;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.codice.ui.admin.wizard.config.handlers.AdminConfigHandler;
import org.codice.ui.admin.wizard.config.handlers.BundleConfigHandler;
import org.codice.ui.admin.wizard.config.handlers.FeatureConfigHandler;
import org.codice.ui.admin.wizard.config.handlers.ManagedServiceHandler;
import org.codice.ui.admin.wizard.config.handlers.PropertyConfigHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Configurator {
    private static final Logger LOGGER = LoggerFactory.getLogger(Configurator.class);

    private final Map<String, ConfigHandler> configHandlers = new LinkedHashMap<>();

    public ConfigReport commit() {
        ConfigReport configReport = new ConfigReport();
        for (Map.Entry<String, ConfigHandler> row : configHandlers.entrySet()) {
            try {
                Object commitResult = row.getValue()
                        .commit();
                if (commitResult instanceof String) {
                    configReport.putResult(row.getKey(),
                            ConfigReport.Result.passManagedService((String) commitResult));
                } else {
                    configReport.putResult(row.getKey(), ConfigReport.Result.pass());
                }
            } catch (ConfiguratorException e) {
                LOGGER.debug("Error committing configuration change", e);

                // On failure, attempt to rollback any config changes that have already been made
                // and then break out of loop processing, only reporting the remaining as skipped
                rollback(row.getKey(), configReport, e);
                break;
            }
        }

        return configReport;
    }

    public String startBundle(String bundleSymName) {
        return registerHandler(BundleConfigHandler.forStart(bundleSymName));
    }

    public String stopBundle(String bundleSymName) {
        return registerHandler(BundleConfigHandler.forStop(bundleSymName));
    }

    public String startFeature(String featureName) {
        return registerHandler(FeatureConfigHandler.forStart(featureName));
    }

    public String stopFeature(String featureName) {
        return registerHandler(FeatureConfigHandler.forStop(featureName));
    }

    public String updatePropertyFile(Path propFile, Map<String, String> properties,
            boolean keepIgnored) {
        return registerHandler(PropertyConfigHandler.instance(propFile, properties, keepIgnored));
    }

    public String updateConfigFile(String configPid, Map<String, String> properties,
            boolean keepIgnored) {
        return registerHandler(AdminConfigHandler.instance(configPid, properties, keepIgnored));
    }

    public String createManagedService(String factoryPid, Map<String, String> properties) {
        return registerHandler(ManagedServiceHandler.forCreate(factoryPid, properties));
    }

    public String deleteManagedService(String configPid) {
        return registerHandler(ManagedServiceHandler.forDelete(configPid));
    }

    private String registerHandler(ConfigHandler handler) {
        String key = UUID.randomUUID()
                .toString();
        configHandlers.put(key, handler);
        return key;
    }

    private void rollback(String failedStep, ConfigReport configReport,
            ConfiguratorException exception) {
        configReport.putResult(failedStep, ConfigReport.Result.fail(exception));

        Deque<Map.Entry<String, ConfigHandler>> undoStack = new ArrayDeque<>();
        boolean skipRest = false;

        for (Map.Entry<String, ConfigHandler> row : configHandlers.entrySet()) {
            if (failedStep.equals(row.getKey())) {
                skipRest = true;
            }

            if (!skipRest) {
                undoStack.push(row);
            } else if (!failedStep.equals(row.getKey())) {
                configReport.putResult(row.getKey(), ConfigReport.Result.skip());
            }
        }

        for (Map.Entry<String, ConfigHandler> row : undoStack) {
            try {
                row.getValue()
                        .rollback();

                configReport.putResult(row.getKey(), ConfigReport.Result.rollback());
            } catch (ConfiguratorException e) {
                String configId = configReport.getResult(row.getKey())
                        .getConfigId();
                if (configId == null) {
                    configReport.putResult(row.getKey(), ConfigReport.Result.rollbackFail(e));
                } else {
                    configReport.putResult(row.getKey(),
                            ConfigReport.Result.rollbackFailManagedService(e, configId));
                }
            }
        }
    }
}
