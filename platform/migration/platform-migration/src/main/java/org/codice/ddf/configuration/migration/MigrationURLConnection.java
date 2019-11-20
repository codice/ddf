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

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import org.codice.ddf.migration.ImportMigrationContext;

/**
 * Custom URLConnection used in {@link ImportMigrationManagerImpl}. It is used to translate dummy
 * URLs to a {@link Path} to an exported file.
 */
public class MigrationURLConnection extends URLConnection {

  private final ImportMigrationContext context;

  private final Path path;

  public static final String URL_BASE = "http://ddf";

  public static final String SYSTEM_PROPERTIES_PATH = "/system.properties";

  public static final String CUSTOM_SYSTEM_PROPERTIES_PATH = "/custom.system.properties";

  public static final String SYSTEM_PROPERTIES_URL = URL_BASE + SYSTEM_PROPERTIES_PATH;

  public static final Map<String, Path> URL_TO_PATHS =
      ImmutableMap.of(
          SYSTEM_PROPERTIES_PATH, Paths.get("etc", "system.properties"),
          CUSTOM_SYSTEM_PROPERTIES_PATH, Paths.get("etc", "custom.system.properties"));

  public MigrationURLConnection(URL url, ImportMigrationContext context) {
    super(url);
    this.context = context;
    this.path = URL_TO_PATHS.get(url.getPath());
  }

  @Override
  public void connect() throws IOException {
    throw new IOException();
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return context.getEntry(path).getInputStream().get();
  }
}
