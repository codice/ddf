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
package org.codice.ddf.configuration.migration.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.codice.ddf.migration.ImportMigrationContext;
import org.codice.ddf.migration.MigrationReport;
import org.junit.Test;

public class VersionUtilsTest {

  @Test
  public void testIsValidMigratableVersionWithValidVersions() {
    ImportMigrationContext mockContext = mock(ImportMigrationContext.class);
    when(mockContext.getMigratableVersion()).thenReturn(Optional.of("1.9"));

    assertThat(VersionUtils.isValidMigratableFloatVersion(mockContext, "2.0", null), equalTo(true));
  }

  @Test
  public void testIsValidMigratableVersionWithInvalidVersions() {
    ImportMigrationContext mockContext = mock(ImportMigrationContext.class);
    MigrationReport mockReport = mock(MigrationReport.class);
    when(mockContext.getReport()).thenReturn(mockReport);
    when(mockContext.getMigratableVersion()).thenReturn(Optional.of("2.1"));

    assertThat(
        VersionUtils.isValidMigratableFloatVersion(mockContext, "2.0", null), equalTo(false));
  }

  @Test
  public void testIsValidMigratableVersionWithNonFloatVersions() {
    ImportMigrationContext mockContext = mock(ImportMigrationContext.class);
    MigrationReport mockReport = mock(MigrationReport.class);
    when(mockContext.getReport()).thenReturn(mockReport);
    when(mockContext.getMigratableVersion()).thenReturn(Optional.of("asdf"));

    assertThat(
        VersionUtils.isValidMigratableFloatVersion(mockContext, "2.0", null), equalTo(false));
  }
}
