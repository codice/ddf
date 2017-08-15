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
    protected void recordEntry() {
        getReport().recordSystemProperty(this);
    }

    @Override
    protected String toDebugString() {
        return String.format("system property reference [%s] as file [%s] to [%s]",
                getProperty(),
                getAbsolutePath(),
                getPath());
    }

    @Override
    protected ExportPathMigrationWarning newWarning(String reason) {
        return new ExportPathMigrationWarning(getProperty(), getPath(), reason);
    }

    @Override
    protected ExportPathMigrationException newError(String reason, Throwable cause) {
        return new ExportPathMigrationException(getProperty(), getPath(), reason, cause);
    }
}
