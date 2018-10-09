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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.apache.commons.lang.Validate;
import org.codice.ddf.migration.Migratable;
import org.codice.ddf.migration.MigrationContext;
import org.codice.ddf.migration.MigrationReport;

/**
 * The migration context base class keeps track of exported migration entries for a given migratable
 * while processing an import migration operation.
 *
 * <p>The export process will generate a zip file. At the root of the zip file will be included
 * metadata files used by the configuration migration manager (e.g. export.json) and folders. Each
 * folder will correspond to a specific {@link Migratable} instance used to export configurations
 * where the folder name will correspond to the identifier of the migratable. Underneath these
 * folders, the structure will be specific to each migratables. By convention files should be in a
 * path corresponding to where they were originally located underneath DDF_HOME.
 *
 * <p>The metadata used by the framework will be located in the root of the zip in a file named
 * export.json and follow the following format:
 *
 * <pre>
 *     {
 *       "version": "1.0",
 *       "product.branding": "ddf",
 *       "product.version": "2.11.0-SNAPSHOT",
 *       "date": "Tue Aug 01 12:39:21 MST 2017"
 *       "migratables": {
 *         "platform-id": {
 *           "version": "1.0",
 *           "title": "",
 *           "description": "",
 *           "organization": "",
 *           "files": [
 *             {
 *               "name": "etc/startup.properties"
 *             },
 *             {
 *               "name": "etc/custom.system.properties"
 *             }
 *           ],
 *           "folders": [
 *             {
 *               "name": "etc/ws-security",
 *               "filtered": false,
 *               "files": [
 *                 "etc/ws-security/issuer/encryption.properties",
 *                 "etc/ws-security/issuer/signature.properties"
 *               ],
 *               "last-modified" : 123456
 *             }
 *           ],
 *           "externals": [
 *             {
 *               "name": "/tmp/some.txt",
 *               "folder": false,
 *               "checksum": "a234f"
 *             },
 *             {
 *               "name": "etc/ws-security/some.link",
 *               "checksum": "123a234f",
 *               "softlink": true
 *             },
 *             {
 *               "name": "../tmp/bob.txt",
 *               "checksum": "bcda234f"
 *             },
 *             {
 *               "name": "../tmp",
 *               "folder": false
 *             }
 *           ],
 *           "system.properties": [
 *             {
 *               "property": "javax.net.ssl.keyStore",
 *               "reference": "etc/keystores/serverKeystore.jks"
 *             }
 *           ]
 *           "java.properties": [
 *             {
 *               "name": "etc/ws-security/server/encryption.properties",
 *               "property": "org.apache.ws.security.crypto.merlin.x509crl.file",
 *               "reference": "etc/certs/demoCA/crl/crl.pem"
 *             }
 *           ],
 *         }
 *       }
 *     }
 * </pre>
 *
 * <p>where:
 *
 * <ul>
 *   <li>'version' is used to keep track of the migration version used during export
 *   <li>'product.branding' is used to keep track of the brand of the system that exported the file
 *   <li>'product.version' is used to keep track of the version of the system the exported zip file
 *       was created from
 *   <li>'date' is used to keep track of the date when the exported zip file was created
 *   <li>'migratables' provides a set of migratables identifier for which additional information is
 *       provided
 *   <li>'title' provides an optional title associated with the migratable
 *   <li>'description' provides an optional description associated with the migratable
 *   <li>'organization' provides an optional organization defining the migratable
 *   <li>'files' provides a list of exported files by the framework on behalf of the migratables
 *   <li>'folders' provides a list of exported folders by the framework on behalf of the migratables
 *       with a list of all the files ('files') that were actually exported
 *   <li>'filtered' is used to indicate if the content of the directory was filtered if <code>true
 *       </code> or if all files in the directory were exported if <code>false</code> or if not
 *       provided
 *   <li>'last-modified' provides the last modified time for the directory in milliseconds from the
 *       epoch if provided
 *   <li>'externals' provides an optional list of external files that should be present on the
 *       destination system as they were not exported
 *   <li>'custom.system.properties' provides an optional list of java properties files containing a
 *       system property that references a file
 *   <li>'java.properties' provides an optional list of java properties files containing a Java
 *       property that references a file
 *   <li>'name' indicates the name of a file (absolute or relative to DDF_HOME). It is required
 *   <li>'folder' indicates the entry represents an external folder if <code>true</code> or a file
 *       if <code>false</code> or if not provided
 *   <li>'checksum' provides the optional MD5 checksum for the file as computed on the original
 *       system
 *   <li>'softlink' provides an optional boolean flag indicating if the file on the original system
 *       was a softlink (defaults to false)
 *   <li>'property' indicates the name of the property containing a reference to another file. It is
 *       required.
 *   <li>'reference' provides the name of the referenced file. It is required.
 *   <li>'properties' provides a list of system properties referencing a file
 * </ul>
 *
 * @param <R> the type of report for this context
 */
public class MigrationContextImpl<R extends MigrationReport> implements MigrationContext {

  public static final String METADATA_PRODUCT_BRANDING = "product.branding";

  public static final String METADATA_PRODUCT_VERSION = "product.version";

  public static final String METADATA_DATE = "date";

  public static final String METADATA_DDF_HOME = "ddf.home";

  public static final String METADATA_MIGRATABLES = "migratables";

  public static final String METADATA_VERSION = "version";

  public static final String METADATA_TITLE = "title";

  public static final String METADATA_DESCRIPTION = "description";

  public static final String METADATA_ORGANIZATION = "organization";

  public static final String METADATA_FILES = "files";

  public static final String METADATA_FOLDERS = "folders";

  public static final String METADATA_EXTERNALS = "externals";

  public static final String METADATA_SYSTEM_PROPERTIES = "system.properties";

  public static final String METADATA_JAVA_PROPERTIES = "java.properties";

  /**
   * Holds the current export version.
   *
   * <p>1.0 - initial version
   */
  protected static final String CURRENT_VERSION = "1.0";

  protected static final Path METADATA_FILENAME = Paths.get("export.json");

  private static final String INVALID_NULL_REPORT = "invalid null report";

  protected final R report;

  @Nullable protected final Migratable migratable;

  /**
   * Holds the current migratable identifier or <code>null</code> if representing the system
   * context.
   */
  @Nullable protected final String id;

  // Forced to define it as non-static to simplify unit testing.
  private final PathUtils pathUtils = new PathUtils();

  /**
   * Holds the current migratable version or empty if representing the system context or if not yet
   * retrieved from exported metadata or again if the corresponding migratable was not exported.
   */
  private Optional<String> version;

  /**
   * Creates a new migration context.
   *
   * @param report the migration report where to record warnings and errors
   * @throws IllegalArgumentException if <code>report</code> is <code>null</code>
   * @throws java.io.IOError if unable to determine ${ddf.home}
   */
  protected MigrationContextImpl(R report) {
    Validate.notNull(report, MigrationContextImpl.INVALID_NULL_REPORT);
    this.report = report;
    this.migratable = null;
    this.id = null;
    this.version = Optional.empty();
  }

  /**
   * Creates a new migration context with no version.
   *
   * @param report the migration report where to record warnings and errors
   * @param id the migratable id
   * @throws IllegalArgumentException if <code>report</code> or <code>id</code> is <code>null</code>
   * @throws java.io.IOError if unable to determine ${ddf.home}
   */
  protected MigrationContextImpl(R report, String id) {
    Validate.notNull(report, MigrationContextImpl.INVALID_NULL_REPORT);
    Validate.notNull(id, "invalid null migratable identifier");
    this.report = report;
    this.migratable = null;
    this.id = id;
    this.version = Optional.empty();
  }

  /**
   * Creates a new migration context with no version.
   *
   * @param report the migration report where to record warnings and errors
   * @param migratable the migratable this context is for
   * @throws IllegalArgumentException if <code>report</code> or <code>migratable</code> is <code>
   * null</code>
   * @throws java.io.IOError if unable to determine ${ddf.home}
   */
  protected MigrationContextImpl(R report, Migratable migratable) {
    Validate.notNull(report, MigrationContextImpl.INVALID_NULL_REPORT);
    Validate.notNull(migratable, "invalid null migratable");
    this.report = report;
    this.migratable = migratable;
    this.id = migratable.getId();
    this.version = Optional.empty();
  }

  /**
   * Creates a new migration context.
   *
   * @param report the migration report where to record warnings and errors
   * @param migratable the migratable this context is for
   * @param version the migratable version
   * @throws IllegalArgumentException if <code>report</code>, <code>migratable</code>, or <code>
   * version</code> is <code>null</code>
   * @throws java.io.IOError if unable to determine ${ddf.home}
   */
  protected MigrationContextImpl(R report, Migratable migratable, String version) {
    Validate.notNull(report, MigrationContextImpl.INVALID_NULL_REPORT);
    Validate.notNull(migratable, "invalid null migratable");
    Validate.notNull(version, "invalid null version");
    this.report = report;
    this.migratable = migratable;
    this.id = migratable.getId();
    this.version = Optional.of(version);
  }

  @Override
  public R getReport() {
    return report;
  }

  @Override
  @Nullable // never externally when passed to any migratable code
  public String getId() {
    return id;
  }

  public Optional<String> getVersion() {
    return version;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof MigrationContextImpl) {
      return Objects.equals(id, ((MigrationContextImpl) o).getId());
    }
    return false;
  }

  protected void processMetadata(Map<String, Object> metadata) {
    this.version =
        Optional.of(JsonUtils.getStringFrom(metadata, MigrationContextImpl.METADATA_VERSION, true));
  }

  @SuppressWarnings("PMD.DefaultPackage" /* designed as an internal service within this package */)
  @VisibleForTesting
  PathUtils getPathUtils() {
    return pathUtils;
  }

  @Nullable
  @VisibleForTesting
  Migratable getMigratable() {
    return migratable;
  }
}
