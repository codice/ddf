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
package org.codice.ddf.configuration.admin;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Objects;

import javax.annotation.Nullable;

import org.apache.commons.lang.Validate;
import org.codice.ddf.migration.ImportMigrationEntry;
import org.codice.ddf.migration.ImportPathMigrationException;
import org.codice.ddf.migration.ProxyImportMigrationEntry;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImportMigrationConfigurationAdminEntry extends ProxyImportMigrationEntry {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            ImportMigrationConfigurationAdminEntry.class);

    private final ImportMigrationConfigurationAdminContext context;

    private final Dictionary<String, Object> properties;

    @Nullable
    private final String factoryPid;

    private final String pid;

    @Nullable
    private final Configuration memoryConfiguration;

    private boolean stored = false;

    public ImportMigrationConfigurationAdminEntry(ImportMigrationConfigurationAdminContext context,
            ImportMigrationEntry entry, Dictionary<String, Object> properties) {
        super(entry);
        Validate.notNull(context, "invalid null context");
        Validate.notNull(properties, "invalid null properties");
        this.context = context;
            this.properties = properties;
        // note: we also remove factory pid and pid from the dictionary as we do not want to restore those later
        this.factoryPid = Objects.toString(properties.remove(ConfigurationAdmin.SERVICE_FACTORYPID),
                null);
        this.pid = Objects.toString(properties.remove(Constants.SERVICE_PID), null);
        this.memoryConfiguration = context.getMemoryConfiguration(this);
        // TODO: InterpolationHelper.performSubstitution(strMap, context); where context is a BundleContext
    }

    @Override
    public boolean store() {
        if (!stored) {
            this.stored = false; // until proven otherwise
            final Configuration cfg;

            if (memoryConfiguration != null) {
                if (propertiesMatch()) {
                    LOGGER.debug("Importing configuration for [{}] from [{}]; no update required",
                            memoryConfiguration.getPid(),
                            getPath());
                    this.stored = true;
                    return true;
                }
                cfg = memoryConfiguration;
                LOGGER.debug(
                        "Importing configuration for [{}] from [{}]; updating existing configuration...",
                        memoryConfiguration.getPid(),
                        getPath());
            } else {
                if (LOGGER.isDebugEnabled()) {
                    if (isManagedServiceFactory()) {
                        LOGGER.debug(
                                "Importing configuration for [{}-?] from [{}]; creating new factory configuration...",
                                factoryPid,
                                getPath());
                    } else {
                        LOGGER.debug(
                                "Importing configuration for [{}] from [{}]; creating new configuration...",
                                pid,
                                getPath());
                    }
                }
                try {
                    cfg = context.createConfiguration(this);
                } catch (IOException e) {
                    if (isManagedServiceFactory()) {
                        getReport().record(new ImportPathMigrationException(getPath(),
                                String.format("failed to create factory configuration [%s]",
                                        factoryPid),
                                e));
                    } else {
                        getReport().record(new ImportPathMigrationException(getPath(),
                                String.format("failed to create configuration [%s]", pid),
                                e));
                    }
                    return false;
                }
                LOGGER.debug(
                        "Importing configuration for [{}] from [{}]; initializing configuration...",
                        cfg.getPid(),
                        getPath());
            }
            try {
                cfg.update(properties);
                this.stored = true;
            } catch (IOException e) {
                getReport().record(new ImportPathMigrationException(getPath(),
                        String.format("failed to update configuration [%s]", getPid()),
                        e));
            }
        }
        return stored;
    }

    public String getFactoryPid() {
        return factoryPid;
    }

    public String getPid() {
        return pid;
    }

    public boolean isManagedServiceFactory() {
        return factoryPid != null;
    }

    public boolean isManagedService() {
        return factoryPid == null;
    }

    public Dictionary<String, Object> getProperties() {
        return properties;
    }

    private boolean propertiesMatch() {
        final Dictionary<String, Object> memprops = memoryConfiguration.getProperties();

        if (memprops == null) {
            return false;
        }
        // remove factory pid and pid from the dictionary as we do not want to match these
        memprops.remove(ConfigurationAdmin.SERVICE_FACTORYPID);
        memprops.remove(Constants.SERVICE_PID);
        if (properties.size() != memprops.size()) {
            return false;
        }
        for (final Enumeration<String> e = properties.keys(); e.hasMoreElements(); ) {
            final String key = e.nextElement();

            if (!Objects.deepEquals(properties.get(key), memprops.get(key))) {
                return false;
            }
        }
        return true;
    }
}
