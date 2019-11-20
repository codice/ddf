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
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * The import migration context keeps track of exported migration entries for a given migratable
 * while processing an import migration operation.
 *
 * <p>The import migration context is provided to a {@link Migratable} during the import migration
 * operation. The migratable can retrieve new entries representing files or blobs of information
 * that may have been exported. It can also retrieve entries for system properties that reference
 * files on disk. As a result, the migratable can re-import the exported information into the new
 * system.
 *
 * <p>For example:
 *
 * <pre>
 *     public class MyMigratable implements Migratable {
 *         ...
 *
 *         public void doImport(ImportMigrationContext context) {
 *             // import an exported file and restore it back to disk
 *             context.getEntry(Paths.get("etc", "myfile.properties"))
 *                 .restore();
 *             // get all exported files located under a specific sub-directory and restore them
 * back on disk
 *             context.entries(Paths.get("etc", "subdir"))
 *                 .forEach(ImportMigrationEntry::restore);
 *             // restore back on disk the file referenced from the "my.property" system property
 *             context.getSystemPropertyReferencedEntry("my.property")
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
public interface ImportMigrationContext extends MigrationContext {

  /**
   * Retrieves a migration entry referenced from a given system property that was exported by the
   * corresponding migratable.
   *
   * <p>The entry returned would be an entry representing the file that was referenced by the
   * specified system property value on the exported system. For example:
   *
   * <p>If system properties defined the following mapping:
   * javax.net.ssl.keyStore=etc/keystores/serverKeystore.jks
   *
   * <p>then the following code:
   *
   * <pre>
   *     final Optional&lt;ImportMigrationEntry&gt; entry
   *         = context.getSystemPropertyReferenceEntry("javax.net.ssl.keyStore");
   * </pre>
   *
   * <p>would return an entry representing the exported file <code>etc/keystores/serverKeystore.jks
   * </code> allowing the migratable a chance to restore it in its original location and verifying
   * that the system property is still defined with the same value after the import operation has
   * completed.
   *
   * @param name the name of the system property referencing a migration entry to retrieve
   * @return the corresponding migration entry or empty if it was not migrated by the corresponding
   *     migratable
   * @throws IllegalArgumentException if <code>name</code> is <code>null</code>
   */
  public Optional<ImportMigrationEntry> getSystemPropertyReferencedEntry(String name);

  /**
   * Retrieves a migration entry that was exported by the corresponding migratable with the given
   * path.
   *
   * <p>A "fake" entry will still be returned if the requested path was not exported. Errors will be
   * recorded when storing the file later using one of the <code>restore()</code> methods or an
   * exception will be thrown out when attempting to retrieve the corresponding input stream.
   *
   * @param path the path of the file that was exported (must be relative to ${ddf.home} to match an
   *     exported entry otherwise an error or a warning will be recorded later when an attempt is
   *     made to restored to returned migration entry)
   * @return the corresponding migration entry
   * @throws IllegalArgumentException if <code>path</code> is <code>null</code>
   */
  public ImportMigrationEntry getEntry(Path path);

  /**
   * Retrieves all exported migration entries.
   *
   * @return a stream of all migration entries
   */
  public Stream<ImportMigrationEntry> entries();

  /**
   * Retrieves all exported migration entries located underneath the provided relative path.
   *
   * @param path the path to the directory for all recursively exported files (must be relative to
   *     ${ddf.home} otherwise no entries will be found)
   * @return a stream of all migration entries located under <code>path</code>
   * @throws IllegalArgumentException if <code>path</code> is <code>null</code>
   */
  public Stream<ImportMigrationEntry> entries(Path path);

  /**
   * Retrieves all exported migration entries located underneath the provided relative path that
   * matches the provided path filter.
   *
   * @param path the path to the directory to recursively search for exported files that match the
   *     given filter (must be relative to ${ddf.home} otherwise no entries will be found)
   * @param filter the path filter to use
   * @return a stream of all migration entries located under <code>path</code> that matches the
   *     given filter
   * @throws IllegalArgumentException if <code>path</code> or <code>filter</code> is <code>null
   * </code>
   */
  public Stream<ImportMigrationEntry> entries(Path path, PathMatcher filter);

  /**
   * Retrieves exported system property.
   *
   * @param key the key of the system property to retrieve
   * @return the system property requested
   * @throws SecurityException if a security manager is present and it does not permit access to the
   *     property
   */
  @Nullable
  public String getSystemProperty(String key);
}
