package org.codice.ddf.configuration.migration;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.migration.ImportPathMigrationException;
import org.codice.ddf.migration.MigrationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines a migration entry representing a property defined in a Java properties file which value
 * references another migration entry.
 */
public class ImportMigrationJavaPropertyReferencedEntryImpl
        extends ImportMigrationPropertyReferencedEntryImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            ImportMigrationJavaPropertyReferencedEntryImpl.class);

    /**
     * Holds the path for the properties file where the reference is defined.
     */
    private final Path propertiesPath;

    ImportMigrationJavaPropertyReferencedEntryImpl(ImportMigrationContextImpl context,
            Map<String, Object> metadata) {
        super(context, metadata);
        this.propertiesPath = Paths.get(FilenameUtils.separatorsToSystem(JsonUtils.getStringFrom(
                metadata,
                MigrationEntryImpl.METADATA_NAME,
                true)));
    }

    public Path getPropertiesPath() {
        return propertiesPath;
    }

    protected String toDebugString() {
        return String.format("Java property reference [%s] from [%s] as file [%s] from [%s]",
                getProperty(),
                propertiesPath,
                getAbsolutePath(),
                getPath());
    }

    @Override
    protected void verifyPropertyAfterCompletion() {
        final MigrationReport report = getReport();

        report.doAfterCompletion(r -> {
            LOGGER.debug("Verifying {}...", toDebugString());
            final String val = getJavaPropertyValue();

            if (val == null) {
                r.record(new ImportPathMigrationException(propertiesPath,
                        getProperty(),
                        getPath(),
                        "it is no longer defined"));
            } else if (StringUtils.isBlank(val)) {
                r.record(new ImportPathMigrationException(propertiesPath,
                        getProperty(),
                        getPath(),
                        "it is empty or blank"));
            } else {
                try {
                    if (!getAbsolutePath().toRealPath(LinkOption.NOFOLLOW_LINKS)
                            .equals(getContext().getPathUtils()
                                    .resolveAgainstDDFHome(Paths.get(val)))) {
                        r.record(new ImportPathMigrationException(propertiesPath,
                                getProperty(),
                                getPath(),
                                "it now references [" + val + ']'));
                    }
                } catch (IOException e) { // cannot determine the location of either so it must not exist or be different anyway
                    r.record(new ImportPathMigrationException(propertiesPath,
                            getProperty(),
                            getPath(),
                            "it now references [" + val + ']',
                            e));
                }
            }
        });
    }

    private String getJavaPropertyValue() {
        final Properties props = new Properties();
        InputStream is = null;

        try {
            is = new BufferedInputStream(new FileInputStream(getContext().getPathUtils()
                    .resolveAgainstDDFHome(propertiesPath)
                    .toFile()));
            props.load(is);
        } catch (IOException e) {
            getReport().record(new ImportPathMigrationException(propertiesPath,
                    getProperty(),
                    getPath(),
                    "failed to load property file",
                    e));
        } finally {
            IOUtils.closeQuietly(is); // we do not care if we cannot close it
        }
        return props.getProperty(getProperty());
    }
}
