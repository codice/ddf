package org.codice.ddf.configuration.migration;

import org.apache.commons.lang.Validate;
import org.codice.ddf.migration.ExportPathMigrationException;
import org.codice.ddf.migration.ExportPathMigrationWarning;
import org.codice.ddf.migration.MigrationExporter;
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

    /**
     * Instantiated a new system property referenced migration entry given a migratable context, property
     * name and pathname.
     *
     * @param context  the migration context associated with this entry
     * @param property the property name for this entry
     * @param pathname the pathname for this entry
     * @throws IllegalArgumentException if <code>context</code>, <code>property</code>, or
     *                                  <code>pathname</code> is <code>null</code>
     */
    ExportMigrationSystemPropertyReferencedEntryImpl(ExportMigrationContextImpl context,
            String property, String pathname) {
        super(context, property, pathname);
    }

    @Override
    public void store() {
        if (!stored) {
            LOGGER.debug("Exporting system property reference [{}] as file [{}] to [{}]...",
                    getProperty(),
                    getAbsolutePath(),
                    getPath());
            getReport().recordSystemProperty(this);
            super.store();
        }
    }

    @Override
    public void store(MigrationExporter exporter) {
        Validate.notNull(exporter, "invalid null exporter");
        if (!stored) {
            super.store(exporter);
        }
    }

    @Override
    protected void recordWarning(String reason) {
        getReport().record(new ExportPathMigrationWarning(getProperty(), getPath(), reason));
    }

    @Override
    protected ExportPathMigrationException newError(String reason, Throwable cause) {
        return new ExportPathMigrationException(getProperty(), getPath(), reason, cause);
    }
}
