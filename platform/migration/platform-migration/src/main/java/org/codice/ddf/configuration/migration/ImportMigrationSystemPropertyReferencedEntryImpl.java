package org.codice.ddf.configuration.migration;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.codice.ddf.migration.ImportPathMigrationException;
import org.codice.ddf.migration.MigrationImporter;
import org.codice.ddf.migration.MigrationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines a migration entry representing a property defined in system properties which value
 * references another migration entry.
 */
public class ImportMigrationSystemPropertyReferencedEntryImpl
        extends ImportMigrationPropertyReferencedEntryImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            ImportMigrationSystemPropertyReferencedEntryImpl.class);

    ImportMigrationSystemPropertyReferencedEntryImpl(ImportMigrationContextImpl context,
            Map<String, Object> metadata) {
        super(context, metadata);
    }

    @Override
    public void store() {
        if (!stored) {
            LOGGER.debug("Importing system property reference [{}] for [{}]...",
                    getProperty(),
                    getPath());
            super.store();
        }
    }

    @Override
    public void store(MigrationImporter importer) {
        Validate.notNull(importer, "invalid null importer");
        if (!stored) {
            LOGGER.debug("Importing system property reference [{}] for [{}]...",
                    getProperty(),
                    getPath());
            super.store(importer);
        }
    }

    @Override
    protected void verifyPropertyAfterCompletion() {
        final MigrationReport report = getReport();
        final Path apath = getAbsolutePath();

        report.doAfterCompletion(r -> {
            final String val = System.getProperty(getProperty());

            if (val == null) {
                r.record(new ImportPathMigrationException(getProperty(),
                        getPath(),
                        "it is no longer defined"));
            } else if (StringUtils.isBlank(val)) {
                r.record(new ImportPathMigrationException(getProperty(),
                        getPath(),
                        "it is empty or blank"));
            } else {
                if (!apath.equals(MigrationEntryImpl.DDF_HOME.resolve(Paths.get(val)))) {
                    r.record(new ImportPathMigrationException(getProperty(),
                            getPath(),
                            "it now references [" + val + ']'));
                }
            }
        });
    }
}
