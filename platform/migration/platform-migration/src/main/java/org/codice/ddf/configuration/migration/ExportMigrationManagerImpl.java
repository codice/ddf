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
import com.google.common.base.Charsets;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.output.CloseShieldOutputStream;
import org.apache.commons.lang.Validate;
import org.codice.ddf.configuration.migration.util.AccessUtils;
import org.codice.ddf.migration.Migratable;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationOperation;
import org.codice.ddf.migration.MigrationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The export migration manager generates an exported file and manages the export migration
 * operation.
 */
public class ExportMigrationManagerImpl implements Closeable {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExportMigrationManagerImpl.class);

  private final MigrationReport report;

  /**
   * Holds export migration contexts for all migratables registered in the system keyed by the
   * migratable id.
   */
  private final Map<String, ExportMigrationContextImpl> contexts;

  /** Holds the exported metadata. The data will be exported to the export.json file. */
  private final Map<String, Object> metadata = new HashMap<>(8);

  private final ZipOutputStream zipOutputStream;

  private final Path exportFile;

  private final CipherUtils cipherUtils;

  private boolean closed = false;

  private static final Path SYSTEM_PROPERTIES_PATH = Paths.get("etc", "system.properties");

  private static final Path CUSTOM_SYSTEM_PROPERTIES_PATH =
      Paths.get("etc", "custom.system.properties");

  /**
   * Creates a new migration manager for an export operation.
   *
   * @param report the migration report where warnings and errors can be recorded
   * @param exportFile the export zip file
   * @param migratables a stream of all migratables in the system in service ranking order
   * @throws MigrationException if a failure occurs while generating the zip file (the error will
   *     not be recorded with the report)
   * @throws IllegalArgumentException if <code>report</code> is <code>null</code> or if it is not
   *     for an export migration operation or if <code>exportFile</code> or <code>migratables</code>
   *     is <code>null</code>
   */
  public ExportMigrationManagerImpl(
      MigrationReport report,
      Path exportFile,
      CipherUtils cipherUtils,
      Stream<? extends Migratable> migratables) {
    this(
        report,
        exportFile,
        cipherUtils,
        migratables,
        ExportMigrationManagerImpl.newZipOutputStreamFor(exportFile));
  }

  @VisibleForTesting
  ExportMigrationManagerImpl(
      MigrationReport report,
      Path exportFile,
      CipherUtils cipherUtils,
      Stream<? extends Migratable> migratables,
      ZipOutputStream zos) {
    Validate.notNull(report, "invalid null report");
    Validate.isTrue(
        report.getOperation() == MigrationOperation.EXPORT, "invalid migration operation");
    Validate.notNull(migratables, "invalid null migratables");
    Validate.notNull(cipherUtils, "invalid null cipher utils");
    this.report = report;
    this.exportFile = exportFile;
    this.cipherUtils = cipherUtils;
    this.zipOutputStream = zos;
    // pre-create contexts for all registered migratables
    this.contexts =
        migratables.collect(
            Collectors.toMap(
                Migratable::getId,
                m ->
                    new ExportMigrationContextImpl(
                        report, m, zipOutputStream, new CipherUtils(exportFile)),
                ConfigurationMigrationManager.throwingMerger(),
                LinkedHashMap::new)); // to preserved ranking order and remove duplicates
  }

  @SuppressWarnings(
      "squid:S2095" /* up to the caller of this method to close it; exception is thrown if the stream cannot be created in the first place */)
  private static ZipOutputStream newZipOutputStreamFor(Path exportFile) {
    Validate.notNull(exportFile, "invalid null export file");
    try {
      return new ZipOutputStream(
          new BufferedOutputStream(new FileOutputStream(exportFile.toFile())));
    } catch (FileNotFoundException e) {
      throw new MigrationException(Messages.EXPORT_FILE_CREATE_ERROR, exportFile, e);
    }
  }

  /**
   * Proceed with the export migration operation.
   *
   * @param productBranding the product branding being exported
   * @param productVersion the product version being exported
   * @throws IllegalArgumentException if <code>productBranding</code> or </code><code>productVersion
   *     </code> is <code>null</code>
   * @throws MigrationException to stop the export operation
   */
  public final void doExport(String productBranding, String productVersion) {
    Validate.notNull(productBranding, "invalid null product branding");
    Validate.notNull(productVersion, "invalid null product version");
    final String ddfHome = System.getProperty("ddf.home");
    final Path ddfHomePath = Paths.get(ddfHome);

    final Properties systemProperties = AccessUtils.doPrivileged(() -> System.getProperties());
    final String systemPropertiesFile =
        getPropertiesFile(ddfHomePath.resolve(SYSTEM_PROPERTIES_PATH));
    final String customSystemPropertiesFile =
        getPropertiesFile(ddfHomePath.resolve(CUSTOM_SYSTEM_PROPERTIES_PATH));

    LOGGER.debug(
        "Exporting {} product [{}] under [{}] with version [{}] to [{}]...",
        productBranding,
        productVersion,
        ddfHome,
        MigrationContextImpl.CURRENT_VERSION,
        exportFile);
    metadata.put(MigrationContextImpl.METADATA_VERSION, MigrationContextImpl.CURRENT_VERSION);
    metadata.put(MigrationContextImpl.METADATA_PRODUCT_BRANDING, productBranding);
    metadata.put(MigrationContextImpl.METADATA_PRODUCT_VERSION, productVersion);
    metadata.put(MigrationContextImpl.METADATA_DATE, new Date().toString());
    metadata.put(MigrationContextImpl.METADATA_DDF_HOME, ddfHome);
    metadata.put(MigrationContextImpl.METADATA_EXPANDED_SYSTEM_PROPERTIES, systemProperties);
    metadata.put(
        MigrationContextImpl.METADATA_SYSTEM_PROPERTIES_PATH.toString(), systemPropertiesFile);
    metadata.put(
        MigrationContextImpl.METADATA_CUSTOM_SYSTEM_PROPERTIES_PATH.toString(),
        customSystemPropertiesFile);
    metadata.put(
        MigrationContextImpl.METADATA_MIGRATABLES,
        contexts
            .values()
            .stream()
            .map(ExportMigrationContextImpl::doExport)
            .collect(
                LinkedHashMap::new,
                LinkedHashMap::putAll,
                LinkedHashMap::putAll)); // preserve order
    LOGGER.debug("Exported metadata: {}", metadata);
  }

  @SuppressWarnings({
    "squid:S2093" /* Not using try-with-resources with CipherOutputStream. Stream is closed inside finally block */
  })
  @Override
  public void close() throws IOException {
    if (!closed) {
      this.closed = true;
      OutputStream cos = null;

      try {
        zipOutputStream.closeEntry();
        try {
          zipOutputStream.putNextEntry(
              new ZipEntry(MigrationContextImpl.METADATA_FILENAME.toString()));
          cos = new CloseShieldOutputStream(zipOutputStream);
          cos = cipherUtils.getCipherOutputStream(cos);
          JsonUtils.MAPPER.writeValue(cos, metadata);
        } catch (IOException e) {
          throw new MigrationException(Messages.EXPORT_METADATA_CREATE_ERROR, e);
        } finally {
          if (cos != null) {
            cos.close();
          }
        }
      } finally {
        zipOutputStream.close();
      }
    }
  }

  public MigrationReport getReport() {
    return report;
  }

  public Path getExportFile() {
    return exportFile;
  }

  @VisibleForTesting
  Collection<ExportMigrationContextImpl> getContexts() {
    return contexts.values();
  }

  @VisibleForTesting
  Map<String, Object> getMetadata() {
    return metadata;
  }

  private String getPropertiesFile(Path path) {
    return AccessUtils.doPrivileged(
        () -> {
          try {
            return new String(Files.readAllBytes(path), Charsets.UTF_8);
          } catch (IOException e) {
            throw new MigrationException(Messages.EXPORT_SYSTEM_PROPERTIES_ERROR, e);
          }
        });
  }
}
