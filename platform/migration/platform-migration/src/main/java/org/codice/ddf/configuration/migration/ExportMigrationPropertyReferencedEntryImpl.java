package org.codice.ddf.configuration.migration;

import java.nio.file.Paths;

import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines a migration entry representing a property which value references another migration entry.
 */
public abstract class ExportMigrationPropertyReferencedEntryImpl extends ExportMigrationEntryImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            ExportMigrationPropertyReferencedEntryImpl.class);

    private static final String METADATA_PROPERTY = "property";

    private static final String METADATA_REFERENCE = "reference";

    private final String property;

    ExportMigrationPropertyReferencedEntryImpl(ExportMigrationContextImpl context, String property,
            String val) {
        super(context, Paths.get(val));
        Validate.notNull(property, "invalid null property");
        this.property = property;
    }

    protected String getProperty() {
        return property;
    }
}
