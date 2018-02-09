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

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

/**
 * The export migration context provides functionality for creating new migration entries for system
 * files and configurations, entries for references in system properties files, and tracking
 * exported migration entries for a given migratable while processing an export migration operation.
 *
 * <p>The export migration context is provided to a {@link Migratable} during the export migration
 * operation to give a migratable a chance to create new entries representing files or blobs of
 * information required for export. It also allows a migratable to indicate which system properties
 * are expected to reference files on disk that should also be migrated.
 *
 * <p>For example:
 *
 * <pre>
 *     public class MyMigratable implements Migratable {
 *         ...
 *
 *         public void doExport(ExportMigrationContext context) {
 *             // export a file located on disk
 *             context.getEntry(Paths.get("etc", "myfile.properties"))
 *                 .store();
 *             // export all files located under a specific sub-directory
 *             context.entries(Paths.get("etc", "subdir"))
 *                 .forEach(ExportMigrationEntry::store);
 *             // export the file referenced from the "my.property" system property
 *             context.getSystemPropertyReferencedEntry("my.property")
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
public interface ExportMigrationContext extends MigrationContext {

  /**
   * Creates or retrieves (if already created) a migration entry referenced from the specified
   * system property to be exported by the corresponding migratable.
   *
   * <p>An error will automatically be recorded with the associated migration report if the system
   * property is not defined or its value is blank.
   *
   * <p><i>Note:</i> The file referenced from the property is assumed to be relative to ${ddf.home}
   * if not defined as absolute. All paths will automatically be relativized from ${ddf.home} if
   * located underneath.
   *
   * <p>The entry returned would be an entry representing the file that was referenced by the
   * specified system property value on the current system. For example:
   *
   * <p>If system properties defines the following mapping:
   * javax.net.ssl.keyStore=etc/keystores/serverKeystore.jks
   *
   * <p>when the following code:
   *
   * <pre>
   *     final Optional&lt;ExportMigrationEntry&gt; entry
   *         = context.getSystemPropertyReferenceEntry("javax.net.ssl.keyStore");
   * </pre>
   *
   * <p>would return an entry representing the local file <code>etc/keystores/serverKeystore.jks
   * </code> giving the migratable a chance to export it alongside the original system property
   * value so it can be later restored and the property value can be verified after the import
   * operation such that it would still be defined with the same value.
   *
   * @param name the name of the system property referencing a migration entry to create or retrieve
   * @return a new migration entry or the existing one if already created for the migration entry
   *     referenced from the specified system property value or empty if the property is not defined
   *     or its value is blank
   * @throws IllegalArgumentException if <code>name</code> is <code>null</code>
   */
  public default Optional<ExportMigrationEntry> getSystemPropertyReferencedEntry(String name) {
    return getSystemPropertyReferencedEntry(name, (r, v) -> true);
  }

  /**
   * Creates or retrieves (if already created) a migration entry referenced from the specified
   * system property to be exported by the corresponding migratable.
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
   * @param name the name of the system property referencing a migration entry to create or retrieve
   * @param validator a predicate to be invoked to validate the property value further which must
   *     return <code>true</code> to have a migration entry created and returned
   * @return a new migration entry or the existing one if already created for the migration entry
   *     referenced from the specified system property value or empty if the property is not defined
   *     or is invalid
   * @throws IllegalArgumentException if <code>name</code> or <code>validator</code> is <code>null
   * </code>
   * @throws SecurityException if a security manager exists and its <code>checkPropertyAccess()
   *     </code> method doesn't allow read access to the specified system property
   */
  public Optional<ExportMigrationEntry> getSystemPropertyReferencedEntry(
      String name, BiPredicate<MigrationReport, String> validator);

  /**
   * Creates or retrieves (if already created) a migration entry to be exported by the corresponding
   * migratable corresponding to the specified path.
   *
   * <p><i>Note:</i> The file referenced is assumed to be relative to ${ddf.home} if not defined as
   * absolute. All paths will automatically be relativized from ${ddf.home} if located underneath.
   *
   * @param path the path of the file to be exported
   * @return a new migration entry for the corresponding migratable or the existing one if already
   *     created
   * @throws IllegalArgumentException if <code>path</code> is <code>null</code>
   */
  public ExportMigrationEntry getEntry(Path path);

  /**
   * Recursively walks the provided path's tree to create or retrieve (if already created) entries
   * for all files found and returns existing or new migration entries for each one of them.
   *
   * <p><i>Note:</i> Files referenced are assumed to be relative to ${ddf.home} if not defined as
   * absolute. All paths will automatically be relativized from ${ddf.home} if located underneath.
   *
   * @param path the path to the directory to recursively walk
   * @return a stream of all created or retrieved entries corresponding to all files recursively
   *     found under <code>path</code>
   * @throws IllegalArgumentException if <code>path</code> is <code>null</code>
   * @throws SecurityException if a security manager exists and its <code>checkRead()</code> method
   *     doesn't allow read access to the specified path
   */
  public default Stream<ExportMigrationEntry> entries(Path path) {
    return entries(path, true);
  }

  /**
   * Recursively walks the provided path's tree to create or retrieve (if already created) entries
   * for all files found that match the provided path filter and returns existing or new migration
   * entries for each one of them.
   *
   * <p><i>Note:</i> Files referenced are assumed to be relative to ${ddf.home} if not defined as
   * absolute. All paths will automatically be relativized from ${ddf.home} if located underneath.
   *
   * @param path the path to the directory to recursively walk
   * @param filter the path filter to use
   * @return a stream of all created or retrieved entries corresponding to all files recursively
   *     found under <code>path</code> which matches the given filter
   * @throws IllegalArgumentException if <code>path</code> or <code>filter</code> is <code>null
   * </code>
   * @throws SecurityException if a security manager exists and its <code>checkRead()</code> method
   *     doesn't allow read access to the specified path
   */
  public default Stream<ExportMigrationEntry> entries(Path path, PathMatcher filter) {
    return entries(path, true, filter);
  }

  /**
   * Recursively walks (or not) the provided path's tree to create or retrieve (if already created)
   * entries for all files found and returns existing or new migration entries for each one of them.
   *
   * <p><i>Note:</i> Files referenced are assumed to be relative to ${ddf.home} if not defined as
   * absolute. All paths will automatically be relativized from ${ddf.home} if located underneath.
   *
   * @param path the path to the directory to recursively walk
   * @param recurse <code>true</code> to recurse the specified path's tree; <code>false</code> to
   *     only list the files located in the specified path
   * @return a stream of all created or retrieved entries corresponding to all files recursively
   *     found under <code>path</code>
   * @throws IllegalArgumentException if <code>path</code> is <code>null</code>
   * @throws SecurityException if a security manager exists and its <code>checkRead()</code> method
   *     doesn't allow read access to the specified path
   */
  public Stream<ExportMigrationEntry> entries(Path path, boolean recurse);

  /**
   * Recursively walks (or not) the provided path's tree to create or retrieve (if already created)
   * entries for all files found that match the provided path filter and returns existing or new
   * migration entries for each one of them.
   *
   * <p><i>Note:</i> Files referenced are assumed to be relative to ${ddf.home} if not defined as
   * absolute. All paths will automatically be relativized from ${ddf.home} if located underneath.
   *
   * @param path the path to the directory to recursively walk
   * @param recurse <code>true</code> to recurse the specified path's tree; <code>false</code> to
   *     only list the files located in the specified path
   * @param filter the path filter to use
   * @return a stream of all created or retrieved entries corresponding to all files recursively
   *     found under <code>path</code> which matches the given filter
   * @throws IllegalArgumentException if <code>path</code> or <code>filter</code> is <code>null
   * </code>
   * @throws SecurityException if a security manager exists and its <code>checkRead()</code> method
   *     doesn't allow read access to the specified path
   */
  public Stream<ExportMigrationEntry> entries(Path path, boolean recurse, PathMatcher filter);
}
