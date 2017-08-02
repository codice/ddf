package org.codice.ddf.configuration.migration;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.codice.ddf.migration.ImportPathMigrationException;
import org.codice.ddf.migration.MigrationImporter;
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
        this.propertiesPath =
                Paths.get(MigrationEntryImpl.sanitizeSeparators(JsonUtils.getStringFrom(metadata,
                        MigrationEntryImpl.METADATA_NAME,
                        true)));
    }

    public Path getPropertiesPath() {
        return propertiesPath;
    }

    @Override
    public void store() {
        if (!stored) {
            LOGGER.debug("Importing Java property reference [{}] from [{}] as file [{}] from [{}]...",
                    getProperty(),
                    propertiesPath,
                    getAbsolutePath(),
                    getPath());
            super.store();
        }
    }

    @Override
    public void store(MigrationImporter importer) {
        Validate.notNull(importer, "invalid null importer");
        if (!stored) {
            super.store(importer);
        }
    }

    @Override
    protected void verifyPropertyAfterCompletion() {
        final MigrationReport report = getReport();
        final Path apath = getAbsolutePath();

        report.doAfterCompletion(r -> {
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
                if (!apath.equals(MigrationEntryImpl.DDF_HOME.resolve(Paths.get(val)))) {
                    r.record(new ImportPathMigrationException(propertiesPath,
                            getProperty(),
                            getPath(),
                            "it now references [" + val + ']'));
                }
            }
        });
    }

    private String getJavaPropertyValue() {
        final Properties props = new Properties();
        InputStream is = null;

        try {
            is = new BufferedInputStream(new FileInputStream(MigrationEntryImpl.DDF_HOME.resolve(
                    propertiesPath)
                    .toFile()));
            props.load(is);
        } catch (IOException e) {
            getReport().record(new ImportPathMigrationException(propertiesPath,
                    getProperty(),
                    getPath(),
                    "failed to load property file",
                    e));
        } finally {
            IOUtils.closeQuietly(is);
        }
        return props.getProperty(getProperty());
    }
}
