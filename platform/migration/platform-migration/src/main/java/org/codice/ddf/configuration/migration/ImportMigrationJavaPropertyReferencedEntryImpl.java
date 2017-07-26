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
        this.propertiesPath =
                Paths.get(MigrationEntryImpl.sanitizeSeparators(JsonUtils.getStringFrom(metadata,
                        MigrationEntryImpl.METADATA_NAME,
                        true)));
    }

    public Path getPropertiesPath() {
        return propertiesPath;
    }

    @Override
    public boolean store() {
        if (super.stored == null) {
            LOGGER.debug("Importing Java property reference [{}] from [{}] for [{}]...",
                    getProperty(),
                    propertiesPath,
                    getPath());
            super.store();
        }
        return super.stored;
    }

    @Override
    protected void verifyPropertyAfterCompletion() {
        final MigrationReport report = getReport();
        final Path apath = getAbsolutePath();

        report.verifyAfterCompletion(r -> {
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
                Path vpath = Paths.get(val);

                vpath = vpath.isAbsolute() ? vpath : MigrationEntryImpl.DDF_HOME.resolve(vpath);
                if (!apath.equals(vpath)) {
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
            is = new BufferedInputStream(new FileInputStream((propertiesPath.isAbsolute() ?
                    propertiesPath :
                    MigrationEntryImpl.DDF_HOME.resolve(propertiesPath)).toFile()));
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
