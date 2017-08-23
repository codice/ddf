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
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.apache.karaf.system.SystemService;
import org.codice.ddf.migration.Migratable;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationMessage;
import org.codice.ddf.migration.MigrationOperation;
import org.codice.ddf.migration.MigrationReport;
import org.codice.ddf.migration.MigrationSuccessfulInformation;
import org.codice.ddf.migration.MigrationWarning;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final String EXPORT_EXTENSION = ".zip";

    private static final String EXPORT_DATE_FORMAT = "-%tY%<tm%<tdT%<tH%<tM%<tS";

    private static final String EXPORT_PREFIX = "exported-";

    private static final String REBOOT_DELAY = "1"; // 1 minute

    private final MBeanServer mBeanServer;

    private final List<Migratable> migratables;

    private final SystemService system;

    private final String productVersion;

    /**
     * Constructor.
     *
     * @param mBeanServer object used to register this object as an MBean
     * @param migratables list of {@link Migratable} services. Needs
     *                    to be kept up-to-date by the client of this class.
     * @param system      the system service
     * @throws IOError if unable to load the distribution version information.
     */
    public ConfigurationMigrationManager(MBeanServer mBeanServer, List<Migratable> migratables,
            SystemService system) {
        notNull(mBeanServer, "MBeanServer cannot be null");
        notNull(migratables, "List of migratable services cannot be null");
        notNull(system, "invalid null system service");
        this.mBeanServer = mBeanServer;
        this.migratables = migratables;
        this.system = system;
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

    /**
     * Gets a consumer that will downgrade all error messages to warnings before passing them
     * along to the provided consumer. All informational messages are passed as is.
     *
     * @param consumer the consumer to pass messages to
     * @return a new consumer that will downgrade error messages before passing them to
     * <code>consumer</code>
     */
    private static Consumer<MigrationMessage> downgradeErrorsToWarningsAndRemoveInfosFor(
            Consumer<MigrationMessage> consumer) {
        return m -> consumer.accept(m.downgradeToWarning()
                .map(MigrationMessage.class::cast)
                .orElse(m));
    }

    public void init() throws Exception {
        final ObjectName objectName = new ObjectName(OBJECT_NAME);

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
        Validate.notNull(exportDirectory, "invalid null export directory");
        final MigrationReport report = doExport(Paths.get(exportDirectory));

        report.verifyCompletion(); // will throw error if it failed
        return report.warnings()
                .collect(Collectors.toList());
    }

    @Override
    public MigrationReport doExport(Path exportDirectory,
            Optional<Consumer<MigrationMessage>> consumer) {
        Validate.notNull(exportDirectory, "invalid null export directory");
        return doExport(exportDirectory, consumer, false); // no timestamp on filename
    }

    private MigrationReportImpl doExport(Path exportDirectory,
            Optional<Consumer<MigrationMessage>> consumer, boolean timestamp) {
        final MigrationReportImpl report = new MigrationReportImpl(MigrationOperation.EXPORT,
                consumer);

        try {
            FileUtils.forceMkdir(exportDirectory.toFile());
        } catch (IOException e) {
            LOGGER.info("unable to create directory: " + exportDirectory + "; ", e);
            report.record(new MigrationException(Messages.DIRECTORY_CREATE_ERROR,
                    exportDirectory,
                    e));
            return report;
        }
        StringBuilder filename = new StringBuilder(
                ConfigurationMigrationManager.EXPORT_PREFIX + productVersion);

        if (timestamp) {
            filename.append(String.format(ConfigurationMigrationManager.EXPORT_DATE_FORMAT,
                    report.getStartTime()));
        }
        filename.append(ConfigurationMigrationManager.EXPORT_EXTENSION);
        final Path exportFile = exportDirectory.resolve(filename.toString());

        try {
            delegateToExportMigrationManager(report, exportFile);
        } catch (MigrationException e) {
            report.record(e);
        } catch (IOException e) {
            report.record(new MigrationException(Messages.EXPORT_FILE_CLOSE_ERROR, exportFile, e));
        } catch (RuntimeException e) {
            report.record(new MigrationException(Messages.EXPORT_INTERNAL_ERROR, exportFile, e));
        }
        report.end();
        if (report.hasErrors()) {
            // don't leave the zip file there if the export failed
            FileUtils.deleteQuietly(exportFile.toFile());
            report.record(new MigrationException(Messages.EXPORT_FAILURE, exportFile));
        } else if (report.hasWarnings()) {
            report.record(new MigrationWarning(Messages.EXPORT_SUCCESS_WITH_WARNINGS, exportFile));
        } else {
            report.record(new MigrationSuccessfulInformation(Messages.EXPORT_SUCCESS, exportFile));
        }
        return report;
    }

    @Override
    public Collection<MigrationWarning> doImport(String exportDirectory) throws MigrationException {
        Validate.notNull(exportDirectory, "invalid null export directory");
        final MigrationReport report = doImport(Paths.get(exportDirectory));

        report.verifyCompletion(); // will throw error if it failed
        return report.warnings()
                .collect(Collectors.toList());
    }

    @Override
    public MigrationReport doImport(Path exportDirectory,
            Optional<Consumer<MigrationMessage>> consumer) {
        Validate.notNull(exportDirectory, "invalid null export directory");
        final MigrationReportImpl xreport = doExport(exportDirectory,
                consumer.map(ConfigurationMigrationManager::downgradeErrorsToWarningsAndRemoveInfosFor),
                true); // timestamp the filename
        final MigrationReportImpl report = new MigrationReportImpl(MigrationOperation.IMPORT,
                xreport,
                consumer);
        final Path exportFile = exportDirectory.resolve(
                ConfigurationMigrationManager.EXPORT_PREFIX + productVersion
                        + ConfigurationMigrationManager.EXPORT_EXTENSION);
        ImportMigrationManagerImpl mgr = null;

        try {
            mgr = delegateToImportMigrationManager(report, exportFile);
        } catch (MigrationException e) {
            report.record(e);
        } catch (RuntimeException e) {
            report.record(new MigrationException(Messages.IMPORT_INTERNAL_ERROR, exportFile, e));
        } finally {
            IOUtils.closeQuietly(mgr); // do not care if we fail to close the mgr/zip file!!!
        }
        report.end();
        if (report.hasErrors()) {
            report.record(new MigrationException(Messages.IMPORT_FAILURE, exportFile));
        } else if (report.hasWarnings()) {
            report.record(new MigrationWarning(Messages.IMPORT_SUCCESS_WITH_WARNINGS, exportFile));
            report.record(new MigrationWarning(Messages.RESTART_SYSTEM_WHEN_WARNINGS));
        } else {
            report.record(new MigrationSuccessfulInformation(Messages.IMPORT_SUCCESS, exportFile));
            try {
                System.setProperty("karaf.restart.jvm", "true"); // force a JVM restart
                system.reboot(ConfigurationMigrationManager.REBOOT_DELAY, SystemService.Swipe.NONE);
                report.record(Messages.RESTARTING_SYSTEM,
                        ConfigurationMigrationManager.REBOOT_DELAY);
            } catch (Exception e) { // yeah, their interface declares an exception can be thrown!!!!
                LOGGER.debug("failed to request a reboot: ", e);
                report.record(Messages.RESTART_SYSTEM);
            }
        }

        return report;
    }

    ImportMigrationManagerImpl delegateToImportMigrationManager(MigrationReportImpl report,
            Path exportFile) {
        final ImportMigrationManagerImpl mgr = new ImportMigrationManagerImpl(report,
                exportFile,
                migratables.stream());

        report.record(Messages.IMPORTING_DATA, exportFile);
        mgr.doImport(productVersion);
        return mgr;
    }

    void delegateToExportMigrationManager(MigrationReportImpl report, Path exportFile)
            throws IOException {
        try (final ExportMigrationManagerImpl mgr = new ExportMigrationManagerImpl(report,
                exportFile,
                migratables.stream())) {
            report.record(Messages.EXPORTING_DATA, exportFile);
            mgr.doExport(productVersion);
        }
    }
}