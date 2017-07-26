package org.codice.ddf.configuration.migration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

import org.codice.ddf.migration.ImportMigrationEntry;
import org.codice.ddf.migration.MigrationEntry;
import org.codice.ddf.migration.MigrationException;
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
    public boolean store() {
        if (super.stored == null) {
            super.stored = getReferencedEntry().store();
            verifyPropertyAfterCompletion();
        }
        return super.stored;
    }

    @Override
    public Optional<ImportMigrationEntry> getPropertyReferencedEntry(String name) {
        return referenced.getPropertyReferencedEntry(name);
    }

    /**
     * Called after the referenced migration entry is stored to register a verifier to be invoked
     * after the migration operation completion to verify if the property value references the
     * referenced migration entry.
     */
    protected abstract void verifyPropertyAfterCompletion();

    protected MigrationEntry getReferencedEntry() {
        return referenced;
    }

    protected String getProperty() {
        return property;
    }
}
