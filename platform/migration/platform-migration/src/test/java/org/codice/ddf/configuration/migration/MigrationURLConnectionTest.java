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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.codice.ddf.migration.ImportMigrationContext;
import org.codice.ddf.migration.ImportMigrationEntry;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

public class MigrationURLConnectionTest {

  private final ImportMigrationContext context = Mockito.mock(ImportMigrationContext.class);

  private final Path sysPropsPath = Paths.get("etc", "system.properties");

  private final ImportMigrationEntry entry = Mockito.mock(ImportMigrationEntry.class);

  private final InputStream stream =
      new ByteArrayInputStream(("test system properties").getBytes());

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setup() throws Exception {
    Mockito.when(context.getEntry(sysPropsPath)).thenReturn(entry);
    Mockito.when(entry.getInputStream()).thenReturn(Optional.of(stream));
  }

  @Test
  public void testConstructor() throws Exception {
    URL systemPropsURL =
        new URL(
            null,
            MigrationURLConnection.SYSTEM_PROPERTIES_URL,
            new URLStreamHandler() {
              @Override
              protected URLConnection openConnection(URL u) throws IOException {
                return new MigrationURLConnection(u, context);
              }
            });

    Assert.assertThat(systemPropsURL.openStream(), Matchers.sameInstance(stream));
  }

  @Test
  public void testConstructorWhenNullURL() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("invalid null URL"));

    new MigrationURLConnection(null, context);
  }

  @Test
  public void testConstructorWhenUnsupportedURL() throws Exception {
    thrown.expect(MalformedURLException.class);
    thrown.expectMessage(Matchers.containsString("Could not resolve URL"));

    URL url = new URL("http://ddf/unsupported.path");
    new MigrationURLConnection(url, context);
  }
}
