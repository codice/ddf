package org.codice.ddf.configuration.migration;

import java.nio.file.Path;

import org.apache.commons.lang.Validate;
import org.codice.ddf.migration.ExportPathMigrationException;
import org.codice.ddf.migration.ExportPathMigrationWarning;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines a migration entry representing a property defined in a Java properties file which value
 * references another migration entry.
 */
public class ExportMigrationJavaPropertyReferencedEntryImpl
        extends ExportMigrationPropertyReferencedEntryImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            ExportMigrationJavaPropertyReferencedEntryImpl.class);

    private static final String METADATA_NAME = "name";

    /**
     * Holds the path for the properties file where the reference is defined.
     */
    private final Path propertiesPath;

    ExportMigrationJavaPropertyReferencedEntryImpl(ExportMigrationContextImpl context,
            Path propertiesPath, String property, String val) {
        super(context, property, val);
        Validate.notNull(propertiesPath, "invalid null properties path");
        this.propertiesPath = propertiesPath;
    }

    public Path getPropertiesPath() {
        return propertiesPath;
    }

    @Override
    public boolean store() {
        if (super.stored == null) {
            LOGGER.debug("Exporting Java property reference [{}] from [{}] for [{}]...",
                    getProperty(),
                    propertiesPath,
                    getPath());
            getReport().recordJavaProperty(this);
            super.store();
        }
        return super.stored;
    }

    protected void recordWarning(String reason) {
        getReport().record(new ExportPathMigrationWarning(propertiesPath, getName(), path, reason));
    }

    protected void recordError(String reason, Throwable cause) {
        getReport().record(new ExportPathMigrationException(propertiesPath, getName(), path, reason, cause));
    }
}
