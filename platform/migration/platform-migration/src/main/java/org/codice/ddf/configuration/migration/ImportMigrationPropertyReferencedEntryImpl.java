package org.codice.ddf.configuration.migration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang.Validate;
import org.codice.ddf.migration.ImportMigrationEntry;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationImporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines a migration entry representing a property which value references another migration entry.
 */
public abstract class ImportMigrationPropertyReferencedEntryImpl extends ImportMigrationEntryImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            ImportMigrationPropertyReferencedEntryImpl.class);

    private final String property;

    private final ImportMigrationEntry referenced;

    ImportMigrationPropertyReferencedEntryImpl(ImportMigrationContextImpl context,
            Map<String, Object> metadata) {
        super(context,
                JsonUtils.getStringFrom(metadata,
                        MigrationEntryImpl.METADATA_REFERENCE,
                        true));
        this.property = JsonUtils.getStringFrom(metadata,
                MigrationEntryImpl.METADATA_PROPERTY,
                true);
        this.referenced = context.getEntry(getPath())
                .orElseThrow(() -> new MigrationException(
                        "Invalid metadata file format; reference '" + getName()
                                + "' is missing from export file"));
    }

    @Override
    public long getLastModifiedTime() {
        return referenced.getLastModifiedTime();
    }

    @Override
    public long getSize() {
        return referenced.getSize();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return referenced.getInputStream();
    }

    @Override
    public void store() {
        if (!stored) {
            super.stored = true;
            referenced.store();
            verifyPropertyAfterCompletion();
        }
    }

    @Override
    public void store(MigrationImporter importer) {
        Validate.notNull(importer, "invalid null importer");
        if (!stored) {
            super.stored = true;
            referenced.store(importer);
            verifyPropertyAfterCompletion();
        }
    }

    @Override
    public Optional<ImportMigrationEntry> getPropertyReferencedEntry(String name) {
        return referenced.getPropertyReferencedEntry(name);
    }

    /**
     * Called after the referenced migration entry is stored to register code to be invoked
     * after the migration operation completion to verify if the property value references the
     * referenced migration entry.
     */
    protected abstract void verifyPropertyAfterCompletion();

    protected String getProperty() {
        return property;
    }
}
