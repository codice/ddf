package org.codice.ddf.configuration.migration;

import org.codice.ddf.migration.ExportPathMigrationException;
import org.codice.ddf.migration.ExportPathMigrationWarning;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines a migration entry representing a property defined in system properties which value
 * references another migration entry.
 */
public class ExportMigrationSystemPropertyReferencedEntryImpl
        extends ExportMigrationPropertyReferencedEntryImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            ExportMigrationSystemPropertyReferencedEntryImpl.class);

    ExportMigrationSystemPropertyReferencedEntryImpl(ExportMigrationContextImpl context,
            String property, String val) {
        super(context, property, val);
    }

    @Override
    public boolean store() {
        if (super.stored == null) {
            LOGGER.debug("Exporting system property reference [{}] for [{}]...",
                    getProperty(),
                    getPath());
            getReport().recordSystemProperty(this);
            super.store();
        }
        return super.stored;
    }

    protected void recordWarning(String reason) {
        getReport().record(new ExportPathMigrationWarning(getName(), path, reason));
    }

    protected void recordError(String reason, Throwable cause) {
        getReport().record(new ExportPathMigrationException(getName(), path, reason, cause));
    }
}
