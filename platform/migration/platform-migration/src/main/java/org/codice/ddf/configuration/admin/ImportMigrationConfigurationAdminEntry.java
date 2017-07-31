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
import java.util.Objects;

import javax.annotation.Nullable;

import org.apache.commons.lang.Validate;
import org.codice.ddf.configuration.persistence.PersistenceStrategy;
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
            ImportMigrationEntry entry, PersistenceStrategy persister) {
        super(entry);
        Validate.notNull(context, "invalid null context");
        Validate.notNull(persister, "invalid null persister");
        this.context = context;
        try {
            this.properties = persister.read(entry.getInputStream());
        } catch (IOException e) {
            throw new ImportPathMigrationException(getPath(),
                    String.format("unable to read %s configuration", persister.getExtension()),
                    e);
        }
        this.factoryPid = Objects.toString(properties.get(ConfigurationAdmin.SERVICE_FACTORYPID),
                null);
        this.pid = Objects.toString(properties.get(Constants.SERVICE_PID), null);
        this.memoryConfiguration = context.getMemoryConfiguration(this);
    }

    @Override
    public void store() {
        if (!stored) {
            this.stored = true;
            final Configuration cfg;

            if (memoryConfiguration != null) {
                cfg = memoryConfiguration;
                LOGGER.debug(
                        "Importing configuration for [{}] from [{}]; updating existing configuration...",
                        getPid(),
                        getPath());
            } else {
                LOGGER.debug(
                        "Importing configuration for [{}] from [{}]; creating new configuration...",
                        getPid(),
                        getPath());
                try {
                    cfg = context.createConfiguration(this);
                } catch (IOException e) {
                    if (factoryPid != null) {
                        getReport().record(new ImportPathMigrationException(getPath(),
                                String.format("failed to create factory configuration [%s]", factoryPid),
                                e));
                    } else {
                        getReport().record(new ImportPathMigrationException(getPath(),
                                String.format("failed to create configuration [%s]", pid),
                                e));
                    }
                    return;
                }
            }
            try {
                cfg.update(properties);
            } catch (IOException e) {
                getReport().record(new ImportPathMigrationException(getPath(),
                        String.format("failed to update configuration [%s]", getPid()),
                        e));
            }
        }
    }

    public String getFactoryPid() {
        return factoryPid;
    }

    public String getPid() {
        return pid;
    }

    public Dictionary<String, Object> getProperties() {
        return properties;
    }
}
