/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.configuration.migration;

import static org.apache.commons.lang.Validate.notNull;

import java.io.FileInputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.function.BinaryOperator;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.codice.ddf.migration.ConfigurationMigratable;
import org.codice.ddf.migration.DataMigratable;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationOperation;
import org.codice.ddf.migration.MigrationReport;
import org.codice.ddf.migration.MigrationWarning;
import org.codice.ddf.migration.UnexpectedMigrationException;
import org.codice.ddf.platform.services.common.Describable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

/**
 * Implementation of the {@link ConfigurationMigrationService} that allows migration of
 * {@link org.osgi.service.cm.Configuration} objects as well as any other configuration files
 * needed.
 */
public class ConfigurationMigrationManager
        implements ConfigurationMigrationService, ConfigurationMigrationManagerMBean {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ConfigurationMigrationManager.class);

    private static final String CLASS_NAME = ConfigurationMigrationManager.class.getName();

    private static final String OBJECT_NAME = CLASS_NAME + ":service=configuration-migration";

    private static final String PRODUCT_VERSION_FILENAME = "Version.txt";

    private final MBeanServer mBeanServer;

    private final List<ConfigurationMigratable> configurationMigratables;

    private final List<DataMigratable> dataMigratables;

    private final String productVersion;

    private final String filename;

    /**
     * Constructor.
     *
     * @param mBeanServer              object used to register this object as an MBean
     * @param configurationMigratables list of {@link ConfigurationMigratable} services. Needs
     *                                 to be kept up-to-date by the client of this class.
     * @param dataMigratables          list of {@link DataMigratable} services. Needs
     *                                 to be kept up-to-date by the client of this class.
     * @throws IOError if unable to load the distribution version information.
     */
    public ConfigurationMigrationManager(MBeanServer mBeanServer,
            List<ConfigurationMigratable> configurationMigratables,
            List<DataMigratable> dataMigratables) {
        notNull(mBeanServer, "MBeanServer cannot be null");
        notNull(configurationMigratables,
                "List of ConfigurationMigratable services cannot be null");
        notNull(dataMigratables, "List of DataMigratable services cannot be null");

        this.mBeanServer = mBeanServer;
        this.configurationMigratables = configurationMigratables;
        this.dataMigratables = dataMigratables;
        try {
            this.productVersion =
                    ConfigurationMigrationManager.getProductVersion(new FileInputStream(Paths.get(
                            System.getProperty("ddf.home"),
                            ConfigurationMigrationManager.PRODUCT_VERSION_FILENAME)
                            .toFile()));
        } catch (IOException e) {
            LOGGER.warn("unable to load version information; ", e);
            throw new IOError(e);
        }
        this.filename = "exported-" + productVersion + ".zip";
    }

    static <T> BinaryOperator<T> throwingMerger() {
        return (u, v) -> {
            throw new IllegalStateException(String.format("Duplicate key %s", u));
        };
    }

    private static String getProductVersion(InputStream is) throws IOException {
        Validate.notNull(is, "invalid null stream");
        try {
            final List<String> lines = IOUtils.readLines(is, StandardCharsets.UTF_8);

            if (lines.isEmpty()) {
                throw new IOException("missing product version information");
            }
            final String productVersion = lines.get(0)
                    .trim();

            if (productVersion.isEmpty()) {
                throw new IOException("missing product version information");
            }
            return productVersion;
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    public void init() throws Exception {
        ObjectName objectName = new ObjectName(OBJECT_NAME);

        try {
            mBeanServer.registerMBean(this, objectName);
        } catch (InstanceAlreadyExistsException e) {
            LOGGER.debug("{} already registered as an MBean. Re-registering.", CLASS_NAME);

            mBeanServer.unregisterMBean(objectName);
            mBeanServer.registerMBean(this, objectName);

            LOGGER.debug("Successfully re-registered {} as an MBean.", CLASS_NAME);
        }
    }

    @Override
    public Collection<MigrationWarning> doExport(String exportDirectory) throws MigrationException {
        notNull(exportDirectory, "Export directory cannot be null");
        final MigrationReport report = doExport(Paths.get(exportDirectory));

        report.verifyCompletion(); // will throw error if it failed
        return report.getWarnings();
    }

    @Override
    public MigrationReport doExport(Path exportDirectory) {
        Validate.notNull(exportDirectory, "Export directory cannot be null");
        final MigrationReport report = new MigrationReportImpl(MigrationOperation.EXPORT);

        try {
            FileUtils.forceMkdir(exportDirectory.toFile());
        } catch (IOException e) {
            LOGGER.info("unable to create directory: " + exportDirectory + "; ", e);
            report.record(new UnexpectedMigrationException(String.format(
                    "unable to create directory [%s]",
                    exportDirectory), e));
            return report;
        }
        final Path exportFile = exportDirectory.resolve(filename);

        try {
            // purposely leave data migratables out for now
            try (final ExportMigrationManagerImpl mgr = new ExportMigrationManagerImpl(report,
                    exportFile,
                    configurationMigratables.stream())) {
                mgr.doExport(productVersion);
            }
        } catch (MigrationException e) {
            report.record(e);
        } catch (IOException e) {
            report.record(new UnexpectedMigrationException(String.format("failed closing file [%s]",
                    exportFile), e));
        } catch (RuntimeException e) {
            report.record(new UnexpectedMigrationException(String.format(
                    "failed exporting to file [%s]; internal error occurred",
                    exportFile), e));
        }
        return report;
    }

    @Override
    public Collection<MigrationWarning> doImport(String exportDirectory) throws MigrationException {
        notNull(exportDirectory, "Export directory cannot be null");
        final MigrationReport report = doImport(Paths.get(exportDirectory));

        report.verifyCompletion(); // will throw error if it failed
        return report.getWarnings();
    }

    @Override
    public MigrationReport doImport(Path exportDirectory) {
        Validate.notNull(exportDirectory, "Export directory cannot be null");
        final MigrationReport report = new MigrationReportImpl(MigrationOperation.IMPORT);
        final Path exportFile = exportDirectory.resolve(filename);
        ImportMigrationManagerImpl mgr = null;

        try {
            // purposely leave data migratables out for now, we could always leave them in here
            // which would just do no-op if no data is found in the export file
            mgr = new ImportMigrationManagerImpl(report,
                    exportFile,
                    configurationMigratables.stream());
            mgr.doImport(productVersion);
        } catch (MigrationException e) {
            report.record(e);
        } catch (RuntimeException e) {
            report.record(new UnexpectedMigrationException(String.format(
                    "failed importing from file [%s]; internal error occurred",
                    exportFile), e));
        } finally {
            IOUtils.closeQuietly(mgr); // do not care if we fail to close the mgr/zip file!!!
        }
        return report;
    }

    @Override
    public Collection<Describable> getOptionalMigratableInfo() {
        return ImmutableList.copyOf(dataMigratables);
    }
}