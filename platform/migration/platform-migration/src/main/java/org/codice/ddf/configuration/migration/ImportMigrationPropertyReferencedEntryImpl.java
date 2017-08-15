package org.codice.ddf.configuration.migration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import org.apache.commons.lang.Validate;
import org.codice.ddf.migration.ImportMigrationEntry;
import org.codice.ddf.migration.MigrationEntry;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationReport;
import org.codice.ddf.util.function.EBiConsumer;
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

    private boolean verifierRegistered = false;

    ImportMigrationPropertyReferencedEntryImpl(ImportMigrationContextImpl context,
            Map<String, Object> metadata) {
        super(context,
                JsonUtils.getStringFrom(metadata, MigrationEntryImpl.METADATA_REFERENCE, true));
        this.property = JsonUtils.getStringFrom(metadata,
                MigrationEntryImpl.METADATA_PROPERTY,
                true);
        this.referenced = context.getOptionalEntry(getPath())
                .orElseThrow(() -> new MigrationException(
                        "Invalid metadata file format; reference '" + getName()
                                + "' is missing from export file"));
    }

    @Override
    public long getLastModifiedTime() {
        return referenced.getLastModifiedTime();
    }

    @Override
    public Optional<InputStream> getInputStream() throws IOException {
        final Optional<InputStream> is = referenced.getInputStream();

        verifyPropertyAfterCompletionOnce();
        return is;
    }

    @Override
    public boolean store(boolean required) {
        if (stored == null) {
            super.stored = false; // until proven otherwise in case next line throws exception
            LOGGER.debug("Importing {}{}...",
                    (required ? "required " : ""),
                    toDebugString());
            if (referenced.store(required)) {
                super.stored = true;
                verifyPropertyAfterCompletionOnce();
            }
        }
        return stored;
    }

    @Override
    public boolean store(EBiConsumer<MigrationReport, Optional<InputStream>, IOException> consumer) {
        Validate.notNull(consumer, "invalid null consumer");
        if (stored == null) {
            super.stored = false; // until proven otherwise in case next line throws exception
            if (referenced.store(consumer)) {
                super.stored = true;
                verifyPropertyAfterCompletionOnce();
            }
        }
        return stored;
    }

    @Override
    public Optional<ImportMigrationEntry> getPropertyReferencedEntry(String name) {
        return referenced.getPropertyReferencedEntry(name);
    }

    public String getProperty() {
        return property;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + property.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (!super.equals(o)) {
            return false;
        } // else - they would be at least of the same class
        final ImportMigrationPropertyReferencedEntryImpl me = (ImportMigrationPropertyReferencedEntryImpl) o;

        return property.equals(me.getProperty());
    }

    @Override
    public int compareTo(@Nullable MigrationEntry me) {
        final int c = super.compareTo(me);

        if (c != 0) {
            return c;
        } // else they would be at least of the same class
        final ImportMigrationPropertyReferencedEntryImpl ime = (ImportMigrationPropertyReferencedEntryImpl)me;

        return property.compareTo(ime.getProperty());
    }

    /**
     * Called after the referenced migration entry is stored to register code to be invoked
     * after the migration operation completion to verify if the property value references the
     * referenced migration entry.
     */
    protected abstract void verifyPropertyAfterCompletion();

    private void verifyPropertyAfterCompletionOnce() {
        if (!verifierRegistered) {
            this.verifierRegistered = true;
            verifyPropertyAfterCompletion();
        }
    }
}
