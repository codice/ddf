package org.codice.ddf.configuration.migration;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.migration.ImportPathMigrationException;
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
    public boolean store(boolean required) {
        if (stored == null) {
            LOGGER.debug("Importing {}system property reference [{}] as file [{}] from [{}]...",
                    (required ? "required " : ""),
                    getProperty(),
                    getAbsolutePath(),
                    getPath());
            return super.store();
        }
        return stored;
    }

    @Override
    protected void verifyPropertyAfterCompletion() {
        final MigrationReport report = getReport();

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
                try {
                    if (!getAbsolutePath().toRealPath()
                            .equals(getContext().getPathUtils()
                                    .resolveAgainstDDFHome(val)
                                    .toRealPath())) {
                        r.record(new ImportPathMigrationException(getProperty(),
                                getPath(),
                                "it now references [" + val + ']'));
                    }
                } catch (IOException e) { // cannot determine the location of either so it must not exist or be different anyway
                    r.record(new ImportPathMigrationException(getProperty(),
                            getPath(),
                            "it now references [" + val + ']',
                            e));
                }
            }
        });
    }
}
