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
import java.io.OutputStream;
import java.nio.file.PathMatcher;
import java.util.Optional;
import java.util.function.BiPredicate;
import org.codice.ddf.util.function.BiThrowingConsumer;

/**
 * The <code>ExportMigrationEntry</code> interfaces provides support for artifacts that are being
 * exported during migration.
 *
 * <p>Entries created via the export migration context or via the {@link
 * #getPropertyReferencedEntry} methods are not stored in the export file. Storage of these entries
 * is controlled by the {@link Migratable} using the {@link #store} methods. This gives the
 * migratable a chance to make additional checks if need be. In some cases, the migratable might not
 * be responsible for actually migrating the content of a file and might only be responsible for
 * migrating the file being referenced by a given property. This is the case, for example, with Java
 * properties file where a migratable might be responsible for migrating the file being referenced
 * from a property in a properties file but not the properties file itself. In such case, the
 * migratable would create an entry for the properties file that holds the property in question via
 * the {link ExportMigrationContext#getEntry} and then create a migration entry for the file
 * referenced from one of its properties using the {@link #getPropertyReferencedEntry} method.
 *
 * <p>For example:
 *
 * <pre>
 *     public class MyMigratable implements Migratable {
 *         ...
 *
 *         public void doExport(ExportMigrationContext context) {
 *             // get an entry for my properties file
 *             final ExportMigrationEntry entry = context.getEntry(Paths.get("etc",
 * "myfile.properties"));
 *
 *             // get an entry for the file referenced from "my.properties" and store it in the
 * export file
 *             entry.getPropertyReferencedEntry("my.property")
 *                 .ifPresent(ExportMigrationEntry::store);
 *         }
 *
 *         ...
 *     }
 * </pre>
 *
 * <p><b> This code is experimental. While this interface is functional and tested, it may change or
 * be removed in a future version of the library. </b>
 */
public interface ExportMigrationEntry extends MigrationEntry {

  /**
   * Creates or retrieves (if already created) a migration entry referenced from the specified
   * property in the properties file associated with this migration entry to be exported by the
   * corresponding migratable.
   *
   * <p>An error will be automatically recorded with the associated migration report if the property
   * is not defined in the property file referenced by this entry or its value is blank.
   *
   * <p><i>Note:</i> The file referenced from the property is assumed to be relative to ${ddf.home}
   * if not defined as absolute. All paths will automatically be relativized from ${ddf.home} if
   * located underneath.
   *
   * <p>The entry returned would be an entry representing the file that is referenced by the
   * specified property value in the java properties file represented by this entry on the local
   * system. For example:
   *
   * <p>If the properties file (e.g. etc/ws-security/server/encryption.properties) represented by
   * this entry defines the following mapping:
   * org.apache.ws.security.crypto.merlin.x509crl.file=etc/certs/demoCA/crl/crl.pem
   *
   * <p>then the following code:
   *
   * <pre>
   *     final ExportMigrationEntry entry
   *         = context.getEntry("etc/ws-security/server/encryption.properties");
   *
   *     final Optional&lt;ExportMigrationEntry&gt; entry2
   *         = entry.getPropertyReferenceEntry("org.apache.ws.security.crypto.merlin.x509crl.file");
   * </pre>
   *
   * <p>would return an entry representing the local file <code>etc/certs/demoCA/crl/crl.pem</code>
   * giving the migratable a chance to export it alongside the original property value so it can be
   * later restored and the property value can be verified after the import operation such that it
   * would still be defined with the same value in the local
   * etc/ws-security/server/encryption.properties file.
   *
   * @param name the name of the property in the corresponding java properties file referencing a
   *     migration entry to create or retrieve
   * @return a new migration entry or the existing one if already created for the migration entry
   *     referenced from the specified property value or empty if the property is not defined or its
   *     value is blank
   * @throws IllegalArgumentException if <code>name</code> is <code>null</code>
   */
  public default Optional<ExportMigrationEntry> getPropertyReferencedEntry(String name) {
    return getPropertyReferencedEntry(name, (r, n) -> true);
  }

  /**
   * Creates or retrieves (if already created) a migration entry referenced from the specified
   * property in the properties file associated with this migration entry to be exported by the
   * corresponding migratable.
   *
   * <p>The provided predicate is always invoked (as long as the entry was not previously created)
   * to validate the property value which may be <code>null</code> if not defined. Returning <code>
   * false</code> will abort the process and yield an {@link Optional#empty} being returned out of
   * this method. In such case, it is up to the validator to record any required errors or warnings.
   * Returning <code>true</code> from the predicate will allow for a new entry to be created for the
   * corresponding system property unless the property is not defined or its value is blank in which
   * case an error will be recorded and an {@link Optional#empty} is returned from this method. In
   * other words:
   *
   * <ol>
   *   <li>If the predicate returns <code>false</code>, no migration entry is created and no error
   *       is recorded.
   *   <li>If the predicate returns <code>true</code> and ...
   *       <ol>
   *         <li>If the property is not defined or its value is blank, no migration entry is created
   *             and an error is recorded
   *         <li>Otherwise, a new migration entry is created and returned
   *       </ol>
   * </ol>
   *
   * <p><i>Note:</i> The file referenced from the property is assumed to be relative to ${ddf.home}
   * if not defined as absolute. All paths will automatically be relativized from ${ddf.home} if
   * located underneath.
   *
   * <p>The entry returned would be an entry representing the file that is referenced by the
   * specified property value in the java properties file represented by this entry on the local
   * system. For example:
   *
   * <p>If the properties file (e.g. etc/ws-security/server/encryption.properties) represented by
   * this entry defines the following mapping:
   * org.apache.ws.security.crypto.merlin.x509crl.file=etc/certs/demoCA/crl/crl.pem
   *
   * <p>then the following code:
   *
   * <pre>
   *     final ExportMigrationEntry entry
   *         = context.getEntry("etc/ws-security/server/encryption.properties");
   *
   *     final Optional&lt;ExportMigrationEntry&gt; entry2
   *         = entry.getPropertyReferenceEntry("org.apache.ws.security.crypto.merlin.x509crl.file");
   * </pre>
   *
   * <p>would return an entry representing the local file <code>etc/certs/demoCA/crl/crl.pem</code>
   * allowing the migratable a chance to export it alongside the original property value so it can
   * be later restored and the property value can be verified after the import operation such that
   * it would still be defined with the same value in the local
   * etc/ws-security/server/encryption.properties file.
   *
   * @param name the name of the property in the corresponding java properties file referencing a
   *     migration entry to create or retrieve
   * @param validator a predicate to be invoked to validate the property value further which must
   *     return <code>true</code> to have a migration entry created
   * @return a new migration entry or the existing one if already created for the referenced file
   *     from the specified property value or empty if the property is not defined, is blank or is
   *     invalid
   * @throws IllegalArgumentException if <code>name</code> or <code>validator</code> is <code>null
   * </code>
   * @throws SecurityException if a security manager exists and its <code>checkRead()</code> method
   *     denies read access to the file represented by this entry
   */
  public Optional<ExportMigrationEntry> getPropertyReferencedEntry(
      String name, BiPredicate<MigrationReport, String> validator);

  /**
   * Stores this entry's content in the export based on this entry's path which can include
   * sub-directories. If the entry represents a directory then all files recursively located
   * underneath the directory will automatically be exported.
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
   * <p><i>Note:</i> Calling <code>store()</code> twice will not store the entry twice. The second
   * time it is called, the same result will be returned as the first time no matter which <code>
   * store()</code> method was called.
   *
   * @return <code>true</code> if no errors were recorded as a result of processing this command;
   *     <code>false</code> otherwise
   * @throws MigrationException if a failure that prevents the operation from continuing occurred
   */
  public default boolean store() {
    return store(true);
  }

  /**
   * Stores this entry's content in the export based on this entry's path which can include
   * sub-directories. If the entry represents a directory then all files recursively located
   * underneath the directory will automatically be exported.
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
   * <p><i>Note:</i> Calling <code>store()</code> twice will not store the entry twice. The second
   * time it is called, the same result will be returned as the first time no matter which <code>
   * store()</code> method was called.
   *
   * @param required <code>true</code> if the file or directory is required to exist on disk and if
   *     it doesn't an error should be recorded; <code>false</code> if the file or directory is
   *     optional and may not be present in which case calling this method will do nothing
   * @return <code>true</code> if no errors were recorded as a result of processing this command;
   *     <code>false</code> otherwise
   * @throws MigrationException if a failure that prevents the operation from continuing occurred
   */
  public boolean store(boolean required);

  /**
   * Stores all files that recursively match the provided path filter in the export based on this
   * entry's path which can include sub-directories. If this entry represents a file that does not
   * match the provided path filter then nothing will be exported and <code>true</code> will be
   * returned.
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
   * <p><i>Note:</i> Calling <code>store()</code> twice will not store the entry twice. The second
   * time it is called, the same result will be returned as the first time no matter which <code>
   * store()</code> method was called.
   *
   * @param filter the path filter to use
   * @return <code>true</code> if no errors were recorded as a result of processing this command;
   *     <code>false</code> otherwise
   * @throws IllegalArgumentException if <code>filter</code> is <code>null </code>
   * @throws MigrationException if a failure that prevents the operation from continuing occurred
   */
  public default boolean store(PathMatcher filter) {
    return store(true, filter);
  }

  /**
   * Stores all files that recursively match the provided path filter in the export based on this
   * entry's path which can include sub-directories. If this entry represents a file that doesn't
   * match the provided path filter then nothing will be exported and the entry will be considered
   * as non-existent when the <code>required</code> parameter is interpreted for recording errors
   * and deciding what to return.
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
   * <p><i>Note:</i> Calling <code>store()</code> twice will not store the entry twice. The second
   * time it is called, the same result will be returned as the first time no matter which <code>
   * store()</code> method was called.
   *
   * @param required <code>true</code> if the file or directory is required to exist on disk and if
   *     it doesn't an error should be recorded; <code>false</code> if the file or directory is
   *     optional and may not be present in which case calling this method will do nothing
   * @param filter the path filter to use
   * @return <code>true</code> if no errors were recorded as a result of processing this command;
   *     <code>false</code> otherwise
   * @throws IllegalArgumentException if <code>filter</code> is <code>null </code>
   * @throws MigrationException if a failure that prevents the operation from continuing occurred
   */
  public boolean store(boolean required, PathMatcher filter);

  /**
   * Stores this entry's content in the export using the specified consumer based on this entry's
   * path which can include sub-directories.
   *
   * <p>All errors and warnings are automatically recorded with the associated migration report
   * including those thrown by the consumer's logic.
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
   * <p><i>Note:</i> The output stream will automatically be closed (if not closed already) when the
   * output stream for another entry is retrieved, when calling {@link #store} on another entry, or
   * when the export operation completes regardless of outcome for the associated migratable.
   *
   * <p><i>Note:</i> Calling <code>store()</code> twice will not store the entry twice. The second
   * time it is called, the same result will be returned as the first time no matter which <code>
   * store()</code> method was called.
   *
   * <p><i>Note:</i> Storing an entry in this fashion will automatically mark the entry as a file
   * even though on disk it might have represented a directory.
   *
   * @param consumer a consumer capable of exporting the content of this entry to a provided output
   *     stream
   * @return <code>true</code> if no errors were recorded as a result of processing this command;
   *     <code>false</code> otherwise
   * @throws MigrationException if a failure that prevents the operation from continuing occurred
   * @throws IllegalArgumentException if <code>consumer</code> is <code>null</code>
   */
  public boolean store(BiThrowingConsumer<MigrationReport, OutputStream, IOException> consumer);

  /**
   * Gets an output stream for this entry which provides a low-level way for the migratable to store
   * its own content in the export.
   *
   * <p><i>Note:</i> The output stream will automatically be closed (if not closed already) wwhen
   * the output stream for another entry is retrieved, when calling {@link #store} on another entry,
   * or when the export operation completes regardless of outcome for the associated migratable.
   *
   * <p><i>Note:</i> Storing an entry via the output stream returned by this method will
   * automatically mark the entry as a file even though on disk it might have represented a
   * directory.
   *
   * @return an output stream for this entry
   * @throws IOException if an I/O error has occurred
   */
  public OutputStream getOutputStream() throws IOException;
}
