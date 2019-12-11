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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.apache.commons.lang.Validate;
import org.codice.ddf.migration.ImportMigrationContext;
import org.codice.ddf.migration.ImportMigrationEntry;

/**
 * Custom URLConnection used in {@link ImportMigrationManagerImpl}. It is used to translate dummy
 * URLs to a {@link ImportMigrationEntry} of an exported file.
 */
public class MigrationURLConnection extends URLConnection {

  private final ImportMigrationEntry entry;

  public MigrationURLConnection(URL url, ImportMigrationContext context)
      throws FileNotFoundException {
    super(url);
    Validate.notNull(url, "invalid null URL");
    final Path path = Paths.get("etc" + url.getPath());
    this.entry =
        Optional.ofNullable(context.getEntry(path)).orElseThrow(FileNotFoundException::new);
  }

  @Override
  public void connect() throws IOException {}

  /**
   * Gets the input stream of the entry specified by the URL.
   *
   * @return the input stream requested
   * @throws FileNotFoundException if the entry could not be found or the entry does not have an
   *     input stream
   */
  @Override
  public InputStream getInputStream() throws IOException {
    return entry.getInputStream().orElseThrow(FileNotFoundException::new);
  }
}
