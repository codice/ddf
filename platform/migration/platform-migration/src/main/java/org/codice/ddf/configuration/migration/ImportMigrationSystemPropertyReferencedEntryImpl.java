package org.codice.ddf.configuration.migration;

import java.io.IOException;
import java.nio.file.LinkOption;
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

    protected String toDebugString() {
        return String.format("system property reference [%s] as file [%s] from [%s]",
                getProperty(),
                getAbsolutePath(),
                getPath());
    }

    @Override
    protected void verifyPropertyAfterCompletion() {
        final MigrationReport report = getReport();

        report.doAfterCompletion(r -> {
            LOGGER.debug("Verifying {}...", toDebugString());
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
                    if (!getAbsolutePath().toRealPath(LinkOption.NOFOLLOW_LINKS)
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
