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
package org.codice.ddf.migration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.PathMatcher;
import java.util.Optional;
import org.codice.ddf.util.function.BiThrowingConsumer;

/**
 * The <code>ImportMigrationEntry</code> interface provides support for artifacts that are being
 * imported during migration.
 *
 * <p>Entries retrieved via the import migration context or via the {@link
 * #getPropertyReferencedEntry} methods are not restored back on disk. Storage of these entries is
 * controlled by the {@link Migratable} using the {@link #restore} methods. This allows the
 * migratable a chance to make additional checks if need be. In some cases, the migratable might not
 * be responsible for actually migrating the content of a file and might only be responsible for
 * migrating the file being referenced by a given property in it. This is the case, for example,
 * with Java properties file where a migratable might be responsible for migrating the file being
 * referenced from a property in a properties file but not the properties file itself. In such case,
 * the migratable would retrieve an entry for the properties file that holds the property in
 * question via the {@link ImportMigrationContext#getEntry} and then retrieve a migration entry for
 * the file referenced from one of its properties using the {@link #getPropertyReferencedEntry}
 * method.
 *
 * <p>For example:
 *
 * <pre>
 *     public class MyMigratable implements Migratable {
 *         ...
 *
 *         public void doImport(ImportMigrationContext context) {
 *             // get an entry for my properties file
 *             final ImportMigrationEntry entry = context.getEntry(Paths.get("etc",
 * "myfile.properties"));
 *
 *             // get an entry for the file referenced from "my.properties" and restore it back on
 * disk
 *             entry.getPropertyReferencedEntry("my.property")
 *                 .ifPresent(ImportMigrationEntry::restore);
 *         }
 *
 *         ...
 *     }
 * </pre>
 *
 * <p><b> This code is experimental. While this interface is functional and tested, it may change or
 * be removed in a future version of the library. </b>
 */
public interface ImportMigrationEntry extends MigrationEntry {

  /**
   * Retrieves a migration entry referenced from a property value defined in the properties file
   * associated with this migration entry.
   *
   * <p>The entry returned would be an entry representing the file that was referenced by the
   * specified property value in the java properties file represented by this entry on the exported
   * system. For example:
   *
   * <p>If the properties file (e.g. etc/ws-security/server/encryption.properties) represented by
   * this entry defined the following mapping:
   * org.apache.ws.security.crypto.merlin.x509crl.file=etc/certs/demoCA/crl/crl.pem
   *
   * <p>then the following code:
   *
   * <pre>
   *     final ImportMigrationEntry entry
   *         = context.getEntry("etc/ws-security/server/encryption.properties");
   *
   *     final Optional&lt;ImportMigrationEntry&gt; entry2
   *         = entry.getPropertyReferenceEntry("org.apache.ws.security.crypto.merlin.x509crl.file");
   * </pre>
   *
   * <p>would return an entry representing the exported file <code>etc/certs/demoCA/crl/crl.pem
   * </code> allowing the migratable a chance to restore it in its original location and verifying
   * that the property in the local etc/ws-security/server/encryption.properties file is still
   * defined with the same value after the import operation has completed.
   *
   * @param name the name of the property that contains the reference to a migration entry
   * @return the migration entry corresponding to the property value defined by <code>name</code> in
   *     the properties file referenced by this entry or empty if it was not exported
   * @throws IllegalArgumentException if <code>name</code> is <code>null</code>
   */
  public Optional<ImportMigrationEntry> getPropertyReferencedEntry(String name);

  /**
   * Restores this entry's content underneath the distribution root directory based on this entry's
   * path which can include sub-directories. If the entry represents a directory than all files
   * recursively located underneath the directory will automatically be imported.
   *
   * <p>This entry's sub-directories (if any) will be created if they don't already exist. The
   * destination file will be overwritten if it already exists.
   *
   * <p>All errors and warnings are automatically recorded with the associated migration report.
   *
   * <p>Errors can be reported in two ways:
   *
   * <ol>
   *   <li>Errors that abort the whole operation would be thrown out as {@link MigrationException}
   *       (e.g. failure to write to the exported file)
   *   <li>Errors that are specific to this specific entry and that will eventually fail the export
   *       operation at the end. Such errors are simply recorded with the report and <code>false
   *       </code> is returned from this method. This allows for the accumulation of as many issues
   *       as possible to report to the user before aborting the operation.
   * </ol>
   *
   * <p><i>Note:</i> If this entry represents a directory which had been completely exported using
   * {@link ExportMigrationEntry#store} or {@link ExportMigrationEntry#store(boolean)} then in
   * addition to restoring all entries recursively, calling this method will also remove any
   * existing files or directories that were not on the original system.
   *
   * <p><i>Note:</i> Calling <code>restore()</code> twice will not restore the entry two times. The
   * second time it is called, the same result will be returned as the first time no matter which
   * <code>restore()</code> method was called.
   *
   * @return <code>true</code> if no errors were recorded as a result of processing this command;
   *     <code>false</code> otherwise
   * @throws MigrationException if a failure that prevents the operation from continuing occurred
   * @throws SecurityException if a security manager exists and the file entry was exported by the
   *     migratable directly using {@link ExportMigrationEntry#store(BiThrowingConsumer)} or {@link
   *     ExportMigrationEntry#getOutputStream()}) and its <code>checkWrite()</code> method denies
   *     write access or its <code>checkDelete()</code> denies delete access to the file represented
   *     by this entry
   */
  public default boolean restore() {
    return restore(true);
  }

  /**
   * Restores this entry's content underneath the distribution root directory based on this entry's
   * path which can include sub-directories. If the entry represents a directory then all files
   * recursively located underneath the directory will automatically be imported.
   *
   * <p>This entry's sub-directories (if any) will be created if they don't already exist. The
   * destination file will be overwritten if it already exists.
   *
   * <p>All errors and warnings are automatically recorded with the associated migration report.
   *
   * <p>Errors can be reported in two ways:
   *
   * <ol>
   *   <li>Errors that abort the whole operation would be thrown out as {@link MigrationException}
   *       (e.g. failure to write to the exported file)
   *   <li>Errors that are specific to this specific entry and that will eventually fail the export
   *       operation at the end. Such errors are simply recorded with the report and <code>false
   *       </code> is returned from this method. This allows for the accumulation of as many issues
   *       as possible to report to the user before aborting the operation.
   * </ol>
   *
   * <p><i>Note:</i> If this entry represents a directory which had been completely exported using
   * {@link ExportMigrationEntry#store} or {@link ExportMigrationEntry#store(boolean)} then in
   * addition to restoring all entries recursively, calling this method will also remove any
   * existing files or directories that were not on the original system.
   *
   * <p><i>Note:</i> Calling <code>restore()</code> twice will not restore the entry two times. The
   * second time it is called, the same result will be returned as the first time no matter which
   * <code>restore()</code> method was called.
   *
   * @param required <code>true</code> if the file or directory is required to exist in the export
   *     and if it doesn't an error should be recorded; <code>false</code> if the file or directory
   *     is optional and may not be exported in which case calling this method will do nothing
   * @return <code>true</code> if no errors were recorded as a result of processing this command;
   *     <code>false</code> otherwise
   * @throws MigrationException if a failure that prevents the operation from continuing occurred
   * @throws SecurityException if a security manager exists and the file entry was exported by the
   *     migratable directly using {@link ExportMigrationEntry#store(BiThrowingConsumer)} or {@link
   *     ExportMigrationEntry#getOutputStream()}) and its <code>checkWrite()</code> method denies
   *     write access or its <code>checkDelete()</code> denies delete access to the file represented
   *     by this entry
   */
  public boolean restore(boolean required);

  /**
   * Restores all files that recursively match the provided path filter underneath the distribution
   * root directory based on this entry's path which can include sub-directories. If the entry
   * represents a file that does not match the provided path filter then nothing will be imported
   * and <code>true</code> will be returned.
   *
   * <p>This entry's sub-directories (if any) will be created if they don't already exist. The
   * destination file will be overwritten if it already exists.
   *
   * <p>All errors and warnings are automatically recorded with the associated migration report.
   *
   * <p>Errors can be reported in two ways:
   *
   * <ol>
   *   <li>Errors that abort the whole operation would be thrown out as {@link MigrationException}
   *       (e.g. failure to write to the exported file)
   *   <li>Errors that are specific to this specific entry and that will eventually fail the export
   *       operation at the end. Such errors are simply recorded with the report and <code>false
   *       </code> is returned from this method. This allows for the accumulation of as many issues
   *       as possible to report to the user before aborting the operation.
   * </ol>
   *
   * <p><i>Note:</i> Calling <code>restore()</code> twice will not restore the entry two times. The
   * second time it is called, the same result will be returned as the first time no matter which
   * <code>restore()</code> method was called.
   *
   * @param filter the path filter to use
   * @return <code>true</code> if no errors were recorded as a result of processing this command;
   *     <code>false</code> otherwise
   * @throws IllegalArgumentException if <code>filter</code> is <code>null </code>
   * @throws MigrationException if a failure that prevents the operation from continuing occurred
   * @throws SecurityException if a security manager exists and the file entry was exported by the
   *     migratable directly using {@link ExportMigrationEntry#store(BiThrowingConsumer)} or {@link
   *     ExportMigrationEntry#getOutputStream()}) and its <code>checkWrite()</code> method denies
   *     write access or its <code>checkDelete()</code> denies delete access to the file represented
   *     by this entry
   */
  public default boolean restore(PathMatcher filter) {
    return restore(true, filter);
  }

  /**
   * Restores all entries that recursively match the provided path filter underneath the
   * distribution root directory based on this entry's path which can include sub-directories. If
   * the entry represents a file that does not match the provided path filter then nothing will be
   * imported and the entry will be considered as non-existent when the <code>required</code>
   * parameter is interpreted for recording errors and deciding what to return.
   *
   * <p>This entry's sub-directories (if any) will be created if they don't already exist. The
   * destination file will be overwritten if it already exists.
   *
   * <p>All errors and warnings are automatically recorded with the associated migration report.
   *
   * <p>Errors can be reported in two ways:
   *
   * <ol>
   *   <li>Errors that abort the whole operation would be thrown out as {@link MigrationException}
   *       (e.g. failure to write to the exported file)
   *   <li>Errors that are specific to this specific entry and that will eventually fail the export
   *       operation at the end. Such errors are simply recorded with the report and <code>false
   *       </code> is returned from this method. This allows for the accumulation of as many issues
   *       as possible to report to the user before aborting the operation.
   * </ol>
   *
   * <p><i>Note:</i> Calling <code>restore()</code> twice will not restore the entry two times. The
   * second time it is called, the same result will be returned as the first time no matter which
   * <code>restore()</code> method was called.
   *
   * @param required <code>true</code> if the file or directory is required to exist in the export
   *     and if it doesn't an error should be recorded; <code>false</code> if the file or directory
   *     is optional and may not be exported in which case calling this method will do nothing
   * @param filter the path filter to use
   * @return <code>true</code> if no errors were recorded as a result of processing this command;
   *     <code>false</code> otherwise
   * @throws IllegalArgumentException if <code>filter</code> is <code>null </code>
   * @throws MigrationException if a failure that prevents the operation from continuing occurred
   * @throws SecurityException if a security manager exists and the entry was exported by the
   *     migratable directly using {@link ExportMigrationEntry#store(BiThrowingConsumer)} or {@link
   *     ExportMigrationEntry#getOutputStream()}) and its <code>checkWrite()</code> method denies
   *     write access or its <code>checkDelete()</code> denies delete access to the file represented
   *     by this entry
   */
  public boolean restore(boolean required, PathMatcher filter);

  /**
   * Restores this required entry's content appropriately based on this entry's path which can
   * include sub-directories using the specified consumer.
   *
   * <p>All errors and warnings are automatically recorded with the associated migration report
   * including those thrown by the consumer logic.
   *
   * <p>Errors can be reported in two ways:
   *
   * <ol>
   *   <li>Errors that abort the whole operation would be thrown out as {@link MigrationException}
   *       (e.g. failure to read from the exported file)
   *   <li>Errors that are specific to this specific entry and that will eventually fail the import
   *       operation at the end. Such errors are simply recorded with the report and <code>false
   *       </code> is returned from this method. This allows for the accumulation of as many issues
   *       as possible to report to the user before aborting the operation.
   * </ol>
   *
   * <p><i>Note:</i> The input stream will automatically be closed (if not closed already) when the
   * operation completes successfully or not.
   *
   * @param consumer a consumer capable of importing the content of this entry from a provided input
   *     stream which might be empty if the entry was not exported or if the entry represents a
   *     directory (otherwise an error will automatically be recorded)
   * @return <code>true</code> if no errors were recorded as a result of processing this command;
   *     <code>false</code> otherwise
   * @throws IllegalArgumentException if <code>consumer</code> is <code>null</code>
   * @throws MigrationException if a failure that prevents the operation from continuing occurred
   * @throws SecurityException if a security manager exists and the entry was exported by the
   *     framework using {@link ExportMigrationEntry#store()} or {@link
   *     ExportMigrationEntry#store(boolean)} and its <code>checkRead()</code> method denies read
   *     access to the file represented by this entry
   */
  public boolean restore(
      BiThrowingConsumer<MigrationReport, Optional<InputStream>, IOException> consumer);

  /**
   * Gets an input stream for this entry.
   *
   * <p><i>Note:</i> The input stream provided will automatically be closed (if not closed already)
   * regardless of whether or not the operation completes successfully for the associated
   * migratable.
   *
   * @return an input stream for this entry or empty if it was not exported or if it represents a
   *     directory
   * @throws IOException if an I/O error has occurred
   * @throws SecurityException if a security manager exists and the entry was exported by the
   *     framework using {@link ExportMigrationEntry#store()} or {@link
   *     ExportMigrationEntry#store(boolean)} and its <code>checkRead()</code> method denies read
   *     access to the file represented by this entry
   */
  public Optional<InputStream> getInputStream() throws IOException;
}
