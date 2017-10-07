/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.configuration.migration;

import com.google.common.annotations.VisibleForTesting;
import ddf.security.common.audit.SecurityLogger;
import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
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
 * Implementation of the {@link ConfigurationMigrationService} that allows migration of {@link
 * org.osgi.service.cm.Configuration} objects as well as any other configuration files needed.
 */
public class ConfigurationMigrationManager implements ConfigurationMigrationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationMigrationManager.class);

  private static final String PRODUCT_VERSION_FILENAME = "Version.txt";

  private static final String EXPORT_EXTENSION = ".zip";

  private static final String EXPORT_PREFIX = "exported-";

  private static final String INVALID_NULL_EXPORT_DIR = "invalid null export directory";

  private final List<Migratable> migratables;

  private final SystemService system;

  private final String productVersion;

  /**
   * Constructor.
   *
   * @param migratables list of {@link Migratable} services. Needs to be kept up-to-date by the
   *     client of this class.
   * @param system the system service
   * @throws IOError if unable to load the distribution version information.
   */
  public ConfigurationMigrationManager(List<Migratable> migratables, SystemService system) {
    Validate.notNull(migratables, "invalid null migratables");
    Validate.notNull(system, "invalid null system service");
    this.migratables = migratables;
    this.system = system;
    try {
      this.productVersion =
          AccessUtils.doPrivileged(
              () ->
                  ConfigurationMigrationManager.getProductVersion(
                      Paths.get(
                          System.getProperty("ddf.home"),
                          ConfigurationMigrationManager.PRODUCT_VERSION_FILENAME)));
    } catch (SecurityException | IOException e) {
      LOGGER.error(
          String.format(
              "unable to load product version information from '%s'; ",
              ConfigurationMigrationManager.PRODUCT_VERSION_FILENAME),
          e);
      throw new IOError(e);
    }
  }

  public static <T> BinaryOperator<T> throwingMerger() {
    return (u, v) -> {
      throw new IllegalStateException(String.format("Duplicate key %s", u));
    };
  }

  private static String getProductVersion(Path path) throws IOException {
    try (final Stream<String> stream = Files.lines(path)) {
      return stream
          .findFirst()
          .map(String::trim)
          .filter(s -> !s.isEmpty())
          .orElseThrow(() -> new IOException("missing product version information"));
    }
  }

  @Override
  public MigrationReport doExport(Path exportDirectory) {
    // start the access control starting with this class' privileges; thus ignoring whoever called
    // us
    return AccessUtils.doPrivileged(() -> doExport(exportDirectory, Optional.empty()));
  }

  @Override
  public MigrationReport doExport(Path exportDirectory, Consumer<MigrationMessage> consumer) {
    Validate.notNull(consumer, "invalid null consumer");
    // start the access control starting with this class' privileges; thus ignoring whoever called
    // us
    return AccessUtils.doPrivileged(() -> doExport(exportDirectory, Optional.ofNullable(consumer)));
  }

  @Override
  public MigrationReport doImport(Path exportDirectory) {
    // start the access control starting with this class' privileges; thus ignoring whoever called
    // us
    return AccessUtils.doPrivileged(() -> doImport(exportDirectory, Optional.empty()));
  }

  @Override
  public MigrationReport doImport(Path exportDirectory, Consumer<MigrationMessage> consumer) {
    Validate.notNull(consumer, "invalid null consumer");
    // start the access control starting with this class' privileges; thus ignoring whoever called
    // us
    return AccessUtils.doPrivileged(() -> doImport(exportDirectory, Optional.of(consumer)));
  }

  @SuppressWarnings(
      "squid:S2093" /* try-with-resource will throw IOException with InputStream and we do not care to get that exception */)
  @VisibleForTesting
  void delegateToImportMigrationManager(MigrationReportImpl report, Path exportFile) {
    final ImportMigrationManagerImpl mgr =
        new ImportMigrationManagerImpl(report, exportFile, migratables.stream());

    try {
      report.record(Messages.IMPORTING_DATA, exportFile);
      mgr.doImport(productVersion);
    } finally {
      IOUtils.closeQuietly(mgr); // do not care if we fail to close the mgr/zip file!!!
    }
  }

  @VisibleForTesting
  void delegateToExportMigrationManager(MigrationReportImpl report, Path exportFile)
      throws IOException {
    try (final ExportMigrationManagerImpl mgr =
        new ExportMigrationManagerImpl(report, exportFile, migratables.stream())) {
      report.record(Messages.EXPORTING_DATA, exportFile);
      mgr.doExport(productVersion);
    }
  }

  private MigrationReportImpl doExport(
      Path exportDirectory, Optional<Consumer<MigrationMessage>> consumer) {
    Validate.notNull(exportDirectory, ConfigurationMigrationManager.INVALID_NULL_EXPORT_DIR);
    final MigrationReportImpl report = new MigrationReportImpl(MigrationOperation.EXPORT, consumer);

    try {
      FileUtils.forceMkdir(exportDirectory.toFile());
      SecurityLogger.audit("Created export directory {}", exportDirectory);
    } catch (SecurityException | IOException e) {
      LOGGER.warn("unable to create directory: " + exportDirectory + "; ", e);
      SecurityLogger.audit("Failed to create export directory {}", exportDirectory);
      report.record(new MigrationException(Messages.DIRECTORY_CREATE_ERROR, exportDirectory, e));
      return report;
    }
    final Path exportFile =
        exportDirectory.resolve(
            ConfigurationMigrationManager.EXPORT_PREFIX
                + productVersion
                + ConfigurationMigrationManager.EXPORT_EXTENSION);

    try {
      delegateToExportMigrationManager(report, exportFile);
    } catch (MigrationException e) {
      report.record(e);
    } catch (IOException e) {
      report.record(new MigrationException(Messages.EXPORT_FILE_CLOSE_ERROR, exportFile, e));
    } catch (SecurityException e) {
      report.record(new MigrationException(Messages.EXPORT_SECURITY_ERROR, exportFile, e));
    } catch (RuntimeException e) {
      report.record(new MigrationException(Messages.EXPORT_INTERNAL_ERROR, exportFile, e));
    }
    report.end();
    if (report.hasErrors()) {
      SecurityLogger.audit("Errors exporting configuration settings to file {}", exportFile);
      // don't leave the zip file there if the export failed
      if (FileUtils.deleteQuietly(exportFile.toFile())) {
        SecurityLogger.audit("Deleted exported file {}", exportFile);
      } else {
        SecurityLogger.audit("Failed to delete exported file {}", exportFile);
      }
      report.record(new MigrationException(Messages.EXPORT_FAILURE, exportFile));
    } else if (report.hasWarnings()) {
      SecurityLogger.audit("Warnings exporting configuration settings to file {}", exportFile);
      report.record(new MigrationWarning(Messages.EXPORT_SUCCESS_WITH_WARNINGS, exportFile));
    } else {
      SecurityLogger.audit("Exported configuration settings to file {}", exportFile);
      report.record(new MigrationSuccessfulInformation(Messages.EXPORT_SUCCESS, exportFile));
    }
    return report;
  }

  private MigrationReport doImport(
      Path exportDirectory, Optional<Consumer<MigrationMessage>> consumer) {
    Validate.notNull(exportDirectory, ConfigurationMigrationManager.INVALID_NULL_EXPORT_DIR);
    final MigrationReportImpl report = new MigrationReportImpl(MigrationOperation.IMPORT, consumer);
    final Path exportFile =
        exportDirectory.resolve(
            ConfigurationMigrationManager.EXPORT_PREFIX
                + productVersion
                + ConfigurationMigrationManager.EXPORT_EXTENSION);

    try {
      delegateToImportMigrationManager(report, exportFile);
    } catch (MigrationException e) {
      report.record(e);
    } catch (SecurityException e) {
      report.record(new MigrationException(Messages.IMPORT_SECURITY_ERROR, exportFile, e));
    } catch (RuntimeException e) {
      report.record(new MigrationException(Messages.IMPORT_INTERNAL_ERROR, exportFile, e));
    }
    report.end();
    if (report.hasErrors()) {
      SecurityLogger.audit("Errors importing configuration settings from file {}", exportFile);
      report.record(new MigrationException(Messages.IMPORT_FAILURE, exportFile));
    } else if (report.hasWarnings()) {
      SecurityLogger.audit("Warnings importing configuration settings from file {}", exportFile);
      // don't leave the zip file there if the import succeeded
      deleteQuietly(exportFile.toFile());
      report.record(new MigrationWarning(Messages.IMPORT_SUCCESS_WITH_WARNINGS, exportFile));
      report.record(new MigrationWarning(Messages.RESTART_SYSTEM_WHEN_WARNINGS));
    } else {
      SecurityLogger.audit("Exported configuration settings from file {}", exportFile);
      // don't leave the zip file there if the import succeeded
      deleteQuietly(exportFile.toFile());
      report.record(new MigrationSuccessfulInformation(Messages.IMPORT_SUCCESS, exportFile));
      // force a JVM restart
      restart(report);
    }
    return report;
  }

  private void restart(MigrationReport report) {
    try {
      if (!restartServiceWrapperIfControlled()) {
        LOGGER.debug("asking karaf to restart");
        System.setProperty("karaf.restart.jvm", "true");
        system.reboot();
      }
      SecurityLogger.audit("Rebooting system");
      report.record(Messages.RESTARTING_SYSTEM);
    } catch (Exception e) {
      SecurityLogger.audit("Failed to reboot system");
      LOGGER.debug("failed to request a reboot: ", e);
      report.record(Messages.RESTART_SYSTEM);
    }
  }

  private boolean restartServiceWrapperIfControlled()
      throws InstanceNotFoundException, MBeanException, ReflectionException,
          MalformedObjectNameException {
    if ((System.getProperty("wrapper.key")) != null) {
      LOGGER.debug("asking service wrapper to restart");
      ManagementFactory.getPlatformMBeanServer()
          .invoke(
              new ObjectName("org.tanukisoftware.wrapper:type=WrapperManager"),
              "restart",
              null,
              null);
      return true;
    }
    return false;
  }

  private void deleteQuietly(File exportFile) {
    if (FileUtils.deleteQuietly(exportFile)) {
      SecurityLogger.audit("Exported file {} deleted", exportFile);
    } else {
      SecurityLogger.audit("Failed to delete exported file {}", exportFile);
    }
  }
}
