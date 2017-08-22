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
package org.codice.ddf.migration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import org.codice.ddf.util.function.EBiConsumer;

/**
 * The <code>ImportMigrationEntry</code> interfaces provides support for artifacts that are being
 * imported during migration.
 * <p>
 * Entries created via the import migration context or via the {@link #getPropertyReferencedEntry}
 * methods are not stored back on disk. Storage of these entries is controlled by the
 * {@link Migratable} using the {@link #store} methods. This allows the migratable a chance to make
 * additional checks if need be. In some cases, the migratable might not be responsible for actually
 * migrating the content of a file and might only be responsible for migrating the file being referenced
 * by a given property. This is the case, for example, with Java properties file where a migratable
 * might be responsible for migrating the file being referenced from a property in a properties file
 * but not the properties file itself. In such case, the migratable would create an entry for the
 * properties file that holds the property in question via the {link ExportMigrationContext#getEntry}
 * and then create a migration entry for the file referenced from one of its property using the
 * {@link #getPropertyReferencedEntry} method.
 * <p>
 * For example:
 * <pre>
 *     public class MyMigratable implements Migratable {
 *         ...
 *
 *         public void doImport(ImportMigrationContext context) {
 *             // get an entry for my properties file
 *             final ImportMigrationEntry entry = context.getEntry(Paths.get("etc", "myfile.properties"));
 *
 *             // get an entry for the file referenced from "my.properties" and store it back on disk
 *             entry.getPropertyReferencedEntry("my.property")
 *                 .ifPresent(MigrationEntry::store);
 *         }
 *
 *         ...
 *     }
 * </pre>
 * <p>
 * <b>
 * This code is experimental. While this interface is functional
 * and tested, it may change or be removed in a future version of the
 * library.
 * </b>
 * </p>
 */
public interface ImportMigrationEntry extends MigrationEntry {
    /**
     * Retrieves a migration entry referenced from a property value defined in the properties file
     * associated with this migration entry.
     * <p>
     * The entry returned would be an entry representing the file that was referenced by the specified
     * property value in the java properties file represented by this entry on the exported system.
     * For example:
     * <p>
     * If the properties file (e.g. etc/ws-security/server/encryption.properties) represented by this
     * entry defined the following mapping:
     * org.apache.ws.security.crypto.merlin.x509crl.file=etc/certs/demoCA/crl/crl.pem
     * <p>
     * then the following code:
     * <pre>
     *     final ImportMigrationEntry entry
     *         = context.getEntry("etc/ws-security/server/encryption.properties");
     *
     *     final Optional&lt;ImportMigrationEntry&gt; entry2
     *         = entry.getPropertyReferenceEntry("org.apache.ws.security.crypto.merlin.x509crl.file");
     * </pre>
     * <p>
     * would return an entry representing the exported file <code>etc/certs/demoCA/crl/crl.pem</code>
     * allowing the migratable a chance to restore it in its original location and verifying that the
     * property in the local etc/ws-security/server/encryption.properties file is still defined with
     * the same value after the import operation has completed.
     *
     * @param name the name of the property that contains the reference to a migration entry
     * @return the migration entry corresponding to the property value defined by <code>name</code>
     * in the property file referenced by this entry or empty if it was not exported
     * @throws IllegalArgumentException if <code>name</code> is <code>null</code>
     */
    public Optional<ImportMigrationEntry> getPropertyReferencedEntry(String name);

    /**
     * Stores this required entry's content appropriately based on this entry's path which can include
     * sub-directories using the specified consumer.
     * <p>
     * All errors and warnings are automatically recorded with the associated migration report including
     * those thrown by the exporter logic.
     * <p>
     * <i>Note:</i> The input stream will automatically be closed (if not closed already) when the
     * operation completes successfully or not.
     *
     * @param consumer a consumer capable of importing the content of this entry from a provided input
     *                 stream which might be empty if the entry was not exported otherwise an error
     *                 will automatically be recorded)
     * @return <code>true</code> if no errors were recorded as a result of processing this command;
     * <code>false</code> otherwise
     * @throws MigrationException       if a failure that prevents the operation from continuing occurred
     * @throws IllegalArgumentException if <code>consumer</code> is <code>null</code>
     */
    public boolean store(EBiConsumer<MigrationReport, Optional<InputStream>, IOException> consumer);

    /**
     * Gets an input stream for this entry.
     * <p>
     * <i>Note:</i> The input stream provided will automatically be closed (if not closed already) when
     * the import operation completes successfully or not for the associated migratable.
     *
     * @return an input stream for this entry or empty if it was not exported
     * @throws IOException if an I/O error has occurred
     */
    public Optional<InputStream> getInputStream() throws IOException;
}
