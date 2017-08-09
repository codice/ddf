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

import java.io.IOError;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

import org.apache.commons.lang.Validate;
import org.codice.ddf.migration.Migratable;
import org.codice.ddf.migration.MigrationContext;
import org.codice.ddf.migration.MigrationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The migration context base class keeps track of exported migration entries for a given migratable
 * while processing an import migration operation.
 * <p>
 * The export process will generate a zip file. At the root of the zip file will be included metadata
 * files used by the configuration migration manager (e.g. export.json) and folders. Each folder
 * will correspond to a specific {@link Migratable} instance used to export configurations where the
 * folder name will correspond to the identifier of the migratable. Underneath these folders, the
 * structure will be specific to each migratables. By convention files should be in a path corresponding
 * to where it was originally located underneath DDF_HOME.
 * <p>
 * The metadata used by the framework will be located in the root of the zip in a file named export.json
 * and follow the following format:
 * <pre>
 *     {
 *       "version": "1.0",
 *       "product.version": "2.11.0-SNAPSHOT",
 *       "date": "Tue Aug 01 12:39:21 MST 2017"
 *       "migratables": {
 *         "platform-id": {
 *           "version": "1.0",
 *           "title": "",
 *           "description": "",
 *           "organization": "",
 *           "externals": [
 *             {
 *               "name": "/tmp/some.txt",
 *               "checksum": "a234f",
 *               "size": 2147483647
 *             },
 *             {
 *               "name": "etc/ws-security/some.link",
 *               "checksum": "123a234f",
 *               "softlink": true,
 *               "size": 2147483650
 *             },
 *             {
 *               "name": "../tmp/bob.txt",
 *               "checksum": "bcda234f"
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
 * </pre><p>
 * where:
 * <ul>
 * <li>'version' is used to keep track of the migration version used during export</li>
 * <li>'product.version' is used to keep track of the version of the system the exported zip file was created from</li>
 * <li>'date' is used to keep track of the date when the exported zip file was created</li>
 * <li>'migratables' provides a set of migratables identifier for which additional information is provided</li>
 * <li>'title' provides an optional title associated with the migratable</li>
 * <li>'description' provides an optional description associated with the migratable</li>
 * <li>'organization' provides an optional organization defining the migratable</li>
 * <li>'externals' provides an optional list of external files that should be present on the destination system as they were not exported</li>
 * <li>'system.properties' provides an optional list of java properties files containing a system property that references a file</li>
 * <li>'java.properties' provides an optional list of java properties files containing a Java property that references a file</li>
 * <li>'name' indicates the name of a file (absolute or relative to DDF_HOME). It is required</li>
 * <li>'checksum' provides the optional MD5 checksum for the file as computed on the original system</li>
 * <li>'size' provides the optional size of the file on the original system (<code>0</code> if the file didn't exist and <code>-1</code> if it could not be determined).</li>
 * <li>'softlink' provides an optional boolean flag indicating if the file on the original system was a softlink (defaults to false)</li>
 * <li>'property' indicates the name of the property containing a reference to another file. It is required.</li>
 * <li>'reference' provides the name of the referenced file. It is required.</li>
 * <li>'properties' provides a list of system properties referencing a file</li>
 * </ul>
 */
public class MigrationContextImpl implements MigrationContext {
    /**
     * Holds the current export version.
     * <p>
     * 1.0 - initial version
     */
    protected static final String VERSION = "1.0";

    protected static final Path METADATA_FILENAME = Paths.get("export.json");

    protected static final String METADATA_PRODUCT_VERSION = "product.version";

    protected static final String METADATA_DATE = "date";

    protected static final String METADATA_MIGRATABLES = "migratables";

    protected static final String METADATA_VERSION = "version";

    protected static final String METADATA_TITLE = "title";

    protected static final String METADATA_DESCRIPTION = "description";

    protected static final String METADATA_ORGANIZATION = "organization";

    protected static final String METADATA_EXTERNALS = "externals";

    protected static final String METADATA_SYSTEM_PROPERTIES = "system.properties";

    protected static final String METADATA_JAVA_PROPERTIES = "java.properties";

    private static final Logger LOGGER = LoggerFactory.getLogger(MigrationContextImpl.class);

    protected final MigrationReport report;

    @Nullable
    protected final Migratable migratable;

    /**
     * Holds the current migratable identifier or <code>null</code> if representing the system context.
     */
    @Nullable
    protected final String id;

    // Forced to define it as non-static to simplify unit testing.
    private final PathUtils pathUtils = new PathUtils();

    /**
     * Holds the current migratable version or <code>?</code> if representing the system context and
     * <code>null</code> if not yet retrieved from exported metadata or if the corresponding migratable
     * was not exported.
     */
    @Nullable
    private String version;

    /**
     * Creates a new migration context.
     *
     * @param report the migration report where to record warnings and errors
     * @throws IllegalArgumentException if <code>report</code> is <code>null</code>
     * @throws IOError                  if unable to determine ${ddf.home} or the current working directory
     */
    protected MigrationContextImpl(MigrationReport report) {
        Validate.notNull(report, "invalid null report");
        this.report = report;
        this.migratable = null;
        this.id = null;
        this.version = "?";
    }

    /**
     * Creates a new migration context.
     *
     * @param report the migration report where to record warnings and errors
     * @param id     the migratable id
     * @throws IllegalArgumentException if <code>report</code> or <code>id</code> is <code>null</code>
     * @throws IOError                  if unable to determine ${ddf.home} or the current working directory
     */
    protected MigrationContextImpl(MigrationReport report, String id, @Nullable String version) {
        Validate.notNull(report, "invalid null report");
        Validate.notNull(id, "invalid null migratable identifier");
        this.report = report;
        this.migratable = null;
        this.id = id;
        this.version = version;
    }

    /**
     * Creates a new migration context.
     *
     * @param report     the migration report where to record warnings and errors
     * @param migratable the migratable this context is for
     * @throws IllegalArgumentException if <code>report</code> or <code>migratable</code> is <code>null</code>
     * @throws IOError                  if unable to determine ${ddf.home} or the current working directory
     */
    protected MigrationContextImpl(MigrationReport report, Migratable migratable) {
        Validate.notNull(report, "invalid null report");
        Validate.notNull(migratable, "invalid null migratable");
        this.report = report;
        this.migratable = migratable;
        this.id = migratable.getId();
        this.version = "?";
    }

    /**
     * Creates a new migration context.
     *
     * @param report     the migration report where to record warnings and errors
     * @param migratable the migratable this context is for
     * @param version    the migratable version
     * @throws IllegalArgumentException if <code>report</code> or <code>migratable</code> is <code>null</code>
     * @throws IOError                  if unable to determine ${ddf.home} or the current working directory
     */
    protected MigrationContextImpl(MigrationReport report, Migratable migratable,
            @Nullable String version) {
        this(report, migratable);
        this.version = version;
    }

    @Override
    public MigrationReport getReport() {
        return report;
    }

    @Override
    @Nullable // never externally when passed to any migratable code
    public String getId() {
        return id;
    }

    @Override
    public String getVersion() {
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

    PathUtils getPathUtils() {
        return pathUtils;
    }

    protected void processMetadata(Map<String, Object> metadata) {
        this.version = JsonUtils.getStringFrom(metadata,
                MigrationContextImpl.METADATA_VERSION,
                true);
    }
}