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

import org.apache.commons.lang.Validate;
import org.codice.ddf.configuration.persistence.PersistenceStrategy;
import org.codice.ddf.migration.ExportMigrationEntry;
import org.codice.ddf.migration.ExportMigrationEntryProxy;
import org.osgi.service.cm.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class extends on the {@link ExportMigrationEntry} interface to represent a configuration
 * object in memory.
 */
public class ExportMigrationConfigurationAdminEntry extends ExportMigrationEntryProxy {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            ExportMigrationConfigurationAdminEntry.class);

    private final Configuration configuration;

    private final PersistenceStrategy persister;

    private boolean stored = false;

    public ExportMigrationConfigurationAdminEntry(ExportMigrationEntry entry,
            Configuration configuration, PersistenceStrategy persister) {
        super(entry);
        Validate.notNull(configuration, "invalid null configuration");
        Validate.notNull(persister, "invalid null persister");
        this.configuration = configuration;
        this.persister = persister;
    }

    @Override
    public boolean store() {
        if (!stored) {
            LOGGER.debug("Exporting configuration [{}] to [{}]...",
                    configuration.getPid(),
                    getPath());
            this.stored = super.store((r, out) -> persister.write(out,
                    configuration.getProperties()));
        }
        return stored;
    }
}
