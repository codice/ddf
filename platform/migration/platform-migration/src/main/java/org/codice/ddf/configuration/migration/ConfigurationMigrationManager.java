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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.apache.karaf.system.SystemService;
import org.codice.ddf.configuration.migration.util.AccessUtils;
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

  private static final String PRODUCT_BRANDING_FILENAME = "Branding.txt";

  private static final String PRODUCT_VERSION_FILENAME = "Version.txt";

  private static final String MIGRATION_PROPERTIES_FILENAME = "migration.properties";

  private static final String EXPORT_EXTENSION = ".dar";

  private static final String EXPORT_DIR = "exported";

  private static final String INVALID_NULL_EXPORT_DIR = "invalid null export directory";

  private static final String INVALID_NULL_CONSUMER = "invalid null consumer";

  private static final String DDF_HOME_KEY = "ddf.home";

  private static final String SUPPORTED_VERSIONS_KEY = "migration.supported.versions";

  private final List<Migratable> migratables;

  private final SystemService system;

  private final String productBranding;

  private final String productVersion;

  private final Properties migrationProps;

  /**
   * Constructor.
   *
   * @param migratables list of {@link Migratable} services. Needs to be kept up-to-date by the
   *     client of this class.
   * @param system the system service
   * @throws IOError if unable to load the distribution branding or version information.
   */
  public ConfigurationMigrationManager(List<Migratable> migratables, SystemService system) {
    Validate.notNull(migratables, "invalid null migratables");
    Validate.notNull(system, "invalid null system service");
    this.migratables = migratables;
    this.system = system;
    try {
      this.productBranding =
          AccessUtils.doPrivileged(
                  () ->
                      ConfigurationMigrationManager.getProductInfo(
                          Paths.get(
                              System.getProperty(ConfigurationMigrationManager.DDF_HOME_KEY),
                              ConfigurationMigrationManager.PRODUCT_BRANDING_FILENAME),
                          "branding"))
              .toLowerCase();
    } catch (SecurityException | IOException e) {
      LOGGER.error(
          String.format(
              "unable to load product version information from '%s'; ",
              ConfigurationMigrationManager.PRODUCT_VERSION_FILENAME),
          e);
      throw new IOError(e);
    }
    try {
      this.productVersion =
          AccessUtils.doPrivileged(
              () ->
                  ConfigurationMigrationManager.getProductInfo(
                      Paths.get(
                          System.getProperty(ConfigurationMigrationManager.DDF_HOME_KEY),
                          ConfigurationMigrationManager.PRODUCT_VERSION_FILENAME),
                      "version"));
    } catch (SecurityException | IOException e) {
      LOGGER.error(
          String.format(
              "unable to load product version information from '%s'; ",
              ConfigurationMigrationManager.PRODUCT_VERSION_FILENAME),
          e);
      throw new IOError(e);
    }
    try {
      this.migrationProps =
          AccessUtils.doPrivileged(ConfigurationMigrationManager::loadMigrationProperties);
    } catch (SecurityException | IOException e) {
      LOGGER.error(
          String.format(
              "unable to load migration properties from '%s'; ",
              ConfigurationMigrationManager.MIGRATION_PROPERTIES_FILENAME),
          e);
      throw new IOError(e);
    }
  }

  public static <T> BinaryOperator<T> throwingMerger() {
    return (u, v) -> {
      throw new IllegalStateException(String.format("Duplicate key %s", u));
    };
  }

  private static String getProductInfo(Path path, String type) throws IOException {
    try (final Stream<String> stream = Files.lines(path)) {
      return stream
          .findFirst()
          .map(String::trim)
          .filter(s -> !s.isEmpty())
          .orElseThrow(
              () -> new IOException(String.format("missing product %s information", type)));
    }
  }

  @VisibleForTesting
  private static Properties loadMigrationProperties() throws IOException {
    try (InputStream inputStream =
        new FileInputStream(
            Paths.get(
                    System.getProperty(ConfigurationMigrationManager.DDF_HOME_KEY),
                    "etc",
                    ConfigurationMigrationManager.MIGRATION_PROPERTIES_FILENAME)
                .toString())) {
      Properties props = new Properties();
      props.load(inputStream);
      return props;
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
    Validate.notNull(consumer, ConfigurationMigrationManager.INVALID_NULL_CONSUMER);
    // start the access control starting with this class' privileges; thus ignoring whoever called
    // us
    return AccessUtils.doPrivileged(() -> doExport(exportDirectory, Optional.ofNullable(consumer)));
  }

  @Override
  public MigrationReport doImport(Path exportDirectory) {
    // start the access control starting with this class' privileges; thus ignoring whoever called
    // us
    return AccessUtils.doPrivileged(
        () -> doImport(exportDirectory, Collections.emptySet(), Optional.empty()));
  }

  @Override
  public MigrationReport doImport(Path exportDirectory, Consumer<MigrationMessage> consumer) {
    Validate.notNull(consumer, ConfigurationMigrationManager.INVALID_NULL_CONSUMER);
    // start the access control starting with this class' privileges; thus ignoring whoever called
    // us
    return AccessUtils.doPrivileged(
        () -> doImport(exportDirectory, Collections.emptySet(), Optional.of(consumer)));
  }

  @Override
  public MigrationReport doImport(
      Path exportDirectory, Set<String> mandatoryMigratables, Consumer<MigrationMessage> consumer) {
    Validate.notNull(consumer, ConfigurationMigrationManager.INVALID_NULL_CONSUMER);
    Validate.notNull(mandatoryMigratables, "invalid null set of migratable ids");
    // start the access control starting with this class' privileges; thus ignoring whoever called
    // us
    return AccessUtils.doPrivileged(
        () -> doImport(exportDirectory, mandatoryMigratables, Optional.of(consumer)));
  }

  @Override
  public MigrationReport doDecrypt(Path exportDirectory) {
    // start the access control starting with this class' privileges; thus ignoring whoever called
    // us
    return AccessUtils.doPrivileged(() -> doDecrypt(exportDirectory, Optional.empty()));
  }

  @Override
  public MigrationReport doDecrypt(Path exportDirectory, Consumer<MigrationMessage> consumer) {
    Validate.notNull(consumer, ConfigurationMigrationManager.INVALID_NULL_CONSUMER);
    // start the access control starting with this class's privileges; thus ignoring whoever called
    // us
    return AccessUtils.doPrivileged(
        () -> doDecrypt(exportDirectory, Optional.ofNullable(consumer)));
  }

  @VisibleForTesting
  void delegateToImportMigrationManager(
      MigrationReportImpl report, MigrationZipFile zip, Set<String> mandatoryMigratables) {
    final ImportMigrationManagerImpl mgr =
        new ImportMigrationManagerImpl(
            report,
            zip,
            mandatoryMigratables,
            migratables.stream(),
            productBranding,
            productVersion);
    try {
      report.record(Messages.IMPORTING_DATA, productBranding, zip.getZipPath());
      mgr.doImport(migrationProps);
    } finally {
      IOUtils.closeQuietly(mgr); // do not care if we fail to close the mgr/zip file
    }
  }

  @VisibleForTesting
  void delegateToExportMigrationManager(
      MigrationReportImpl report, Path exportFile, CipherUtils cipherUtils) throws IOException {
    try (final ExportMigrationManagerImpl mgr =
        new ExportMigrationManagerImpl(report, exportFile, cipherUtils, migratables.stream())) {
      report.record(Messages.EXPORTING_DATA, productBranding, exportFile);
      mgr.doExport(productBranding, productVersion);
    }
  }

  @VisibleForTesting
  void delegateToDecryptMigrationManager(
      MigrationReportImpl report, MigrationZipFile zip, Path decryptFile) throws IOException {
    try (final DecryptMigrationManagerImpl mgr =
        new DecryptMigrationManagerImpl(report, zip, decryptFile)) {
      report.record(Messages.DECRYPTING_DATA, productBranding, zip.getZipPath(), decryptFile);
      mgr.doDecrypt(productBranding, productVersion);
    }
  }

  @VisibleForTesting
  MigrationZipFile newZipFileFor(Path exportDirectory) {
    Validate.notNull(exportDirectory, "invalid null export file");
    try (Stream<Path> exportFilePathsStream =
        Files.find(exportDirectory, 1, this::exportFileWithBrandingName)) {
      List<Path> exportFilePaths = exportFilePathsStream.collect(Collectors.toList());

      switch (exportFilePaths.size()) {
        case 1:
          return new MigrationZipFile(exportFilePaths.get(0));
        case 0:
          throw new MigrationException(Messages.IMPORT_FILE_MISSING_ERROR, exportDirectory, "");
        default:
          throw new MigrationException(Messages.IMPORT_FILE_MULTIPLE_ERROR, exportDirectory);
      }
    } catch (FileNotFoundException e) {
      throw new MigrationException(Messages.IMPORT_FILE_MISSING_ERROR, exportDirectory, e);
    } catch (SecurityException | IOException e) {
      throw new MigrationException(Messages.IMPORT_FILE_OPEN_ERROR, exportDirectory, e);
    }
  }

  private boolean exportFileWithBrandingName(Path path, BasicFileAttributes basicFileAttributes) {
    return path.toFile()
        .getName()
        .matches(productBranding + "-.*" + ConfigurationMigrationManager.EXPORT_EXTENSION);
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
            productBranding
                + '-'
                + productVersion
                + ConfigurationMigrationManager.EXPORT_EXTENSION);

    final CipherUtils cipherUtils = new CipherUtils(exportFile);

    try {
      delegateToExportMigrationManager(report, exportFile, cipherUtils);
      if (report.wasSuccessful()) {
        cipherUtils.createZipChecksumFile();
      }
    } catch (MigrationException e) {
      report.record(e);
    } catch (IOException e) {
      report.record(new MigrationException(Messages.EXPORT_FILE_CLOSE_ERROR, exportFile, e));
    } catch (SecurityException e) {
      report.record(new MigrationException(Messages.EXPORT_SECURITY_ERROR, exportFile, e));
    } catch (RuntimeException e) {
      report.record(new MigrationException(Messages.EXPORT_INTERNAL_ERROR, exportFile, e));
    } catch (NoSuchAlgorithmException e) {
      report.record(
          new MigrationException(Messages.EXPORT_INTERNAL_ERROR, cipherUtils.getKeyPath(), e));
    }
    report.end();
    if (report.hasErrors()) {
      SecurityLogger.audit("Errors exporting configuration settings to file {}", exportFile);
      // don't leave the zip file there if the export failed
      PathUtils.deleteQuietly(exportFile, ConfigurationMigrationManager.EXPORT_DIR);
      PathUtils.deleteQuietly(cipherUtils.getKeyPath(), ConfigurationMigrationManager.EXPORT_DIR);
      PathUtils.deleteQuietly(
          cipherUtils.getChecksumPath(), ConfigurationMigrationManager.EXPORT_DIR);
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
      Path exportDirectory,
      Set<String> mandatoryMigratables,
      Optional<Consumer<MigrationMessage>> consumer) {
    Validate.notNull(exportDirectory, ConfigurationMigrationManager.INVALID_NULL_EXPORT_DIR);
    final MigrationReportImpl report = new MigrationReportImpl(MigrationOperation.IMPORT, consumer);

    MigrationZipFile zip = null;
    Path exportPath = exportDirectory;
    try {
      zip = newZipFileFor(exportDirectory);
      exportPath = zip.getZipPath();
      if (!zip.isValidChecksum()) {
        throw new MigrationException(Messages.IMPORT_ZIP_CHECKSUM_INVALID, exportPath);
      }

      delegateToImportMigrationManager(report, zip, mandatoryMigratables);
    } catch (MigrationException e) {
      report.record(e);
    } catch (SecurityException e) {
      report.record(new MigrationException(Messages.IMPORT_SECURITY_ERROR, exportPath, e));
    } catch (RuntimeException e) {
      report.record(new MigrationException(Messages.IMPORT_INTERNAL_ERROR, exportPath, e));
    }
    report.end();
    if ((zip == null) || report.hasErrors()) {
      SecurityLogger.audit("Errors importing configuration settings from file {}", exportPath);
      report.record(new MigrationException(Messages.IMPORT_FAILURE, exportPath));
    } else if (report.hasWarnings()) {
      SecurityLogger.audit("Warnings importing configuration settings from file {}", exportPath);
      // don't leave the zip file there if the import succeeded
      zip.deleteQuitetly();
      report.record(new MigrationWarning(Messages.IMPORT_SUCCESS_WITH_WARNINGS, exportPath));
      report.record(new MigrationWarning(Messages.RESTART_SYSTEM_WHEN_WARNINGS));
    } else {
      SecurityLogger.audit("Exported configuration settings from file {}", exportPath);
      // don't leave the zip file there if the import succeeded
      zip.deleteQuitetly();
      report.record(new MigrationSuccessfulInformation(Messages.IMPORT_SUCCESS, exportPath));
      // force a JVM restart
      restart(report);
    }
    return report;
  }

  private MigrationReport doDecrypt(
      Path exportDirectory, Optional<Consumer<MigrationMessage>> consumer) {
    Validate.notNull(exportDirectory, ConfigurationMigrationManager.INVALID_NULL_EXPORT_DIR);
    final MigrationReportImpl report =
        new MigrationReportImpl(MigrationOperation.DECRYPT, consumer);

    Path decryptFile = null;
    MigrationZipFile zip = null;
    Path exportPath = exportDirectory;

    try {
      zip = newZipFileFor(exportDirectory);
      decryptFile = Paths.get(FilenameUtils.removeExtension(zip.getZipPath().toString()) + ".zip");
      exportPath = zip.getZipPath();
      if (!zip.isValidChecksum()) {
        throw new MigrationException(Messages.DECRYPT_ZIP_CHECKSUM_INVALID, exportPath);
      }
      delegateToDecryptMigrationManager(report, zip, decryptFile);
    } catch (MigrationException e) {
      report.record(e);
    } catch (IOException e) {
      report.record(new MigrationException(Messages.DECRYPT_FILE_CLOSE_ERROR, decryptFile, e));
    } catch (SecurityException e) {
      report.record(
          new MigrationException(
              Messages.DECRYPT_SECURITY_ERROR, zip == null ? exportDirectory : exportPath, e));
    } catch (RuntimeException e) {
      report.record(
          new MigrationException(
              Messages.DECRYPT_INTERNAL_ERROR, zip == null ? exportDirectory : exportPath, e));
    }
    report.end();
    if ((zip == null) || report.hasErrors()) {
      SecurityLogger.audit("Errors decrypting configuration settings in file {}", exportPath);
      report.record(new MigrationException(Messages.DECRYPT_FAILURE, exportPath));
      if (decryptFile != null) {
        FileUtils.deleteQuietly(decryptFile.toFile()); // delete the decrypted zip if any
      }
    } else if (report.hasWarnings()) {
      SecurityLogger.audit("Warnings decrypting configuration settings in file {}", exportPath);
      report.record(
          new MigrationWarning(Messages.DECRYPT_SUCCESS_WITH_WARNINGS, exportPath, decryptFile));
    } else {
      SecurityLogger.audit(
          "Decrypted configuration settings from file {} to {}", exportPath, decryptFile);
      report.record(
          new MigrationSuccessfulInformation(Messages.DECRYPT_SUCCESS, exportPath, decryptFile));
    }
    return report;
  }

  private void restart(MigrationReport report) {
    try {
      if (!restartServiceWrapperIfControlled()) {
        // create the restart.jvm file such that we would rely on the ddf script to restart
        // ourselves and not on karaf. This will have the advantage to restart anything else
        // (e.g. solr) that is also managed by that script
        LOGGER.debug("generating restart.jvm file");
        FileUtils.touch(
            Paths.get(
                    System.getProperty(ConfigurationMigrationManager.DDF_HOME_KEY),
                    "bin",
                    "restart.jvm")
                .toFile());
        // make sure Karaf is not going to restart us as we want the ddf script to do it
        System.setProperty("karaf.restart.jvm", "false");
        system.halt();
      }
      SecurityLogger.audit("Restarting system");
      report.record(Messages.RESTARTING_SYSTEM);
    } catch (Exception e) {
      SecurityLogger.audit("Failed to restart system");
      LOGGER.debug("failed to request a restart: ", e);
      report.record(Messages.RESTART_SYSTEM);
    }
  }

  private boolean restartServiceWrapperIfControlled()
      throws InstanceNotFoundException, MBeanException, ReflectionException,
          MalformedObjectNameException {
    if (System.getProperty("wrapper.key") != null) {
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
}
