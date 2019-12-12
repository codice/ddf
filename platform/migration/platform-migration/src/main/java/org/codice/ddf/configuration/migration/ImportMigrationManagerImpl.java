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
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.apache.karaf.util.config.PropertiesLoader;
import org.codice.ddf.configuration.migration.util.AccessUtils;
import org.codice.ddf.migration.ImportMigrationContext;
import org.codice.ddf.migration.ImportMigrationEntry;
import org.codice.ddf.migration.Migratable;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationOperation;
import org.codice.ddf.migration.MigrationReport;
import org.codice.ddf.migration.OptionalMigratable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The import migration manager process an exported file and manages the import migration operation.
 */
public class ImportMigrationManagerImpl implements Closeable {

  private static final Logger LOGGER = LoggerFactory.getLogger(ImportMigrationManagerImpl.class);

  private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

  private final MigrationReport report;

  /**
   * Holds import migration contexts for all migratables registered in the system keyed by the
   * migratable id. <code>null</code> key is used to represent the system context.
   */
  private final Map<String, ImportMigrationContextImpl> contexts;

  private final MigrationZipFile zip;

  private final String version;

  private final String productBranding;

  private final String productVersion;

  private final String currentProductBranding;

  private final String currentProductVersion;

  private final Map<String, ?> systemProperties;

  private final Path exportFile;

  /**
   * Creates a new migration manager for an import operation.
   *
   * @param report the migration report where warnings and errors can be recorded
   * @param zip the exported zip file
   * @param mandatoryMigratables a set of mandatory migratable identifiers
   * @param migratables a stream of all migratables in the system
   * @throws MigrationException if a failure occurs while processing the zip file (the error will
   *     not be recorded with the report)
   * @throws IllegalArgumentException if <code>report</code> is <code>null</code> or if it is not
   *     for an import migration operation or if <code>zip</code> or <code>migratables</code> is
   *     <code>null</code>
   */
  public ImportMigrationManagerImpl(
      MigrationReport report,
      MigrationZipFile zip,
      Set<String> mandatoryMigratables,
      Stream<? extends Migratable> migratables,
      String currentProductBranding,
      String currentProductVersion) {
    Validate.notNull(report, "invalid null report");
    Validate.notNull(zip, "invalid null zip file");
    Validate.isTrue(
        report.getOperation() == MigrationOperation.IMPORT, "invalid migration operation");
    Validate.notNull(migratables, "invalid null migratables");
    Validate.notNull(currentProductBranding, "invalid null product branding");
    Validate.notNull(currentProductVersion, "invalid null product version");
    this.report = report;
    this.exportFile = zip.getZipPath();
    this.zip = zip;
    Map<String, Object> metadata;

    try {
      // pre-create contexts for all registered migratables
      this.contexts =
          migratables.collect(
              Collectors.toMap(
                  Migratable::getId,
                  m ->
                      new ImportMigrationContextImpl(
                          report,
                          zip,
                          m,
                          (m instanceof OptionalMigratable)
                              && !mandatoryMigratables.contains(m.getId())),
                  ConfigurationMigrationManager.throwingMerger(),
                  LinkedHashMap::new)); // to preserved ranking order and remove duplicates
      // add a system contexts
      contexts.put(null, new ImportMigrationContextImpl(report, zip));
      zip.stream()
          .filter(ze -> !ze.isDirectory())
          .map(ze -> new ImportMigrationEntryImpl(this::getContextFor, ze))
          .forEach(me -> me.getContext().addEntry(me));
      metadata = retrieveMetadata(); // do this after retrieving all exported entries
      this.version = JsonUtils.getStringFrom(metadata, MigrationContextImpl.METADATA_VERSION, true);
      if (!MigrationContextImpl.SUPPORTED_VERSIONS.contains(version)) {
        IOUtils.closeQuietly(zip);
        throw new MigrationException(
            Messages.IMPORT_UNSUPPORTED_VERSION_ERROR,
            version,
            MigrationContextImpl.CURRENT_VERSION);
      }
      this.productBranding =
          JsonUtils.getStringFrom(metadata, MigrationContextImpl.METADATA_PRODUCT_BRANDING, true);
      this.productVersion =
          JsonUtils.getStringFrom(metadata, MigrationContextImpl.METADATA_PRODUCT_VERSION, true);
      this.currentProductBranding = currentProductBranding;
      this.currentProductVersion = currentProductVersion;
      // process migratables' metadata
      JsonUtils.getMapFrom(metadata, MigrationContextImpl.METADATA_MIGRATABLES, false)
          .forEach((id, o) -> getContextFor(id).processMetadata(JsonUtils.convertToMap(o)));
      systemProperties = this.getSystemProperties(metadata);
      contexts.forEach((id, m) -> m.setSystemProperties(systemProperties));
    } catch (IOException e) {
      IOUtils.closeQuietly(zip);
      throw new MigrationException(Messages.IMPORT_FILE_READ_ERROR, exportFile, e);
    } catch (RuntimeException e) {
      IOUtils.closeQuietly(zip);
      throw e;
    }
  }

  /**
   * Proceed with the import migration operation.
   *
   * @param migrationProps migration properties loaded from etc/migration.properties
   * @throws IllegalArgumentException if <code>currentProductBranding</code> or <code>
   *     currentProductVersion</code> is <code>null</code>
   * @throws MigrationException if the versions don't match or if a failure occurred that required
   *     interrupting the operation right away
   */
  public void doImport(Properties migrationProps) {
    Validate.notNull(migrationProps, "invalid null migration properties");
    if (!currentProductBranding.equals(this.productBranding)) {
      throw new MigrationException(
          Messages.IMPORT_MISMATCH_PRODUCT_ERROR, this.productBranding, currentProductBranding);
    }

    LOGGER.debug(
        "Importing {} product [{}] from version [{}]...",
        currentProductBranding,
        currentProductVersion,
        version);
    if (this.currentProductVersion.equals(this.productVersion)) {
      contexts.values().forEach(ImportMigrationContextImpl::doImport);
    } else {
      Set<String> supportedVersions =
          getSupportedVersions(migrationProps.getProperty("supported.versions"));
      if (!supportedVersions.contains(this.productVersion)) {
        throw new MigrationException(
            Messages.IMPORT_UNSUPPORTED_PRODUCT_VERSION_ERROR,
            this.productVersion,
            supportedVersions);
      }
      contexts.values().forEach(ImportMigrationContextImpl::doVersionUpgradeImport);
    }
  }

  @Override
  public void close() throws IOException {
    zip.close();
  }

  public MigrationReport getReport() {
    return report;
  }

  public Path getExportFile() {
    return exportFile;
  }

  @VisibleForTesting
  Collection<ImportMigrationContextImpl> getContexts() {
    return contexts.values();
  }

  private Map<String, ?> getSystemProperties(Map<String, Object> metadata) {
    if (version.equals(MigrationContextImpl.SYSTEM_PROPERTIES_NOT_EXPORTED_VERSION)) {
      ImportMigrationContext platformMigrationContext =
          contexts.get(MigrationContextImpl.PLATFORM_MIGRATABLE_ID);
      return AccessUtils.doPrivileged(
          () -> getSystemPropertiesFromContext(platformMigrationContext));
    }
    return JsonUtils.getMapFrom(
        metadata, MigrationContextImpl.METADATA_EXPANDED_SYSTEM_PROPERTIES, true);
  }

  @VisibleForTesting
  Map<String, ?> getSystemPropertiesFromContext(ImportMigrationContext platformMigrationContext) {
    if (platformMigrationContext == null) {
      throw new MigrationException(
          Messages.IMPORT_SYSTEM_PROPERTIES_ERROR, "Platform migration context does not exist");
    }

    try {
      URL systemPropsURL =
          new URL(
              null,
              "http://ddf/system.properties",
              new URLStreamHandler() {
                @Override
                protected URLConnection openConnection(URL u) throws IOException {
                  return new MigrationURLConnection(u, platformMigrationContext);
                }
              });
      return PropertiesLoader.loadPropertiesFile(systemPropsURL, true);
    } catch (Exception e) {
      throw new MigrationException(Messages.IMPORT_SYSTEM_PROPERTIES_ERROR, e);
    }
  }

  private Set<String> getSupportedVersions(@Nullable String commaDelimitedVersions) {
    if (commaDelimitedVersions == null) {
      return Collections.emptySet();
    } else {
      return Arrays.asList(commaDelimitedVersions.split(","))
          .stream()
          .map(String::trim)
          .collect(Collectors.toSet());
    }
  }

  private ImportMigrationContextImpl getContextFor(String id) {
    // create a dummy context with no migratable and bind it to its migratable id if none exist
    return contexts.computeIfAbsent(id, mid -> new ImportMigrationContextImpl(report, zip, mid));
  }

  private Map<String, Object> retrieveMetadata() throws IOException {
    final ImportMigrationEntry me =
        contexts
            .get(null) // metadata entries have no migratable id and will always exist see ctor
            .getEntry(MigrationContextImpl.METADATA_FILENAME);
    InputStream is = null;

    try {
      is =
          me.getInputStream()
              .orElseThrow(() -> new MigrationException(Messages.IMPORT_METADATA_MISSING_ERROR));
      return JsonUtils.MAPPER
          .parser()
          .parseMap(IOUtils.toString(is, ImportMigrationManagerImpl.DEFAULT_CHARSET));
    } finally {
      IOUtils.closeQuietly(is);
    }
  }
}
