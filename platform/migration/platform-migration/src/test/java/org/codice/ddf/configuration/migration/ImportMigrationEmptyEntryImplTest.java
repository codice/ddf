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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ImportMigrationEmptyEntryImplTest {

  @Mock public ImportMigrationContextImpl mockContext;

  @Mock public PathUtils mockPathUtils;

  public ImportMigrationEmptyEntryImpl entry;

  @Before
  public void setup() {
    when(mockContext.getPathUtils()).thenReturn(mockPathUtils);
    when(mockPathUtils.resolveAgainstDDFHome(any(Path.class))).thenReturn(Paths.get("test"));

    entry = new ImportMigrationEmptyEntryImpl(mockContext, Paths.get("test"));
  }

  @Test
  public void getLastModifiedTime() {
    assertThat(entry.getLastModifiedTime(), equalTo(-1L));
  }

  @Test
  public void getInputStream() throws Exception {
    assertThat(entry.getInputStream(), equalTo(Optional.empty()));
  }
}
