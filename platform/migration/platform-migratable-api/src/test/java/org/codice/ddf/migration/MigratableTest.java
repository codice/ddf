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

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class MigratableTest {
  private static final String MIGRATABLE_ID = "test-id";

  private static final String VERSION = "1.0";

  private static final String INCOMPATIBLE_VERSION = "1.1";

  private final Migratable migratable = Mockito.mock(Migratable.class, Mockito.CALLS_REAL_METHODS);

  private final MigrationReport report = Mockito.mock(MigrationReport.class);

  private final ImportMigrationContext context = Mockito.mock(ImportMigrationContext.class);

  @Before
  public void setup() {
    Mockito.when(migratable.getId()).thenReturn(MIGRATABLE_ID);
    Mockito.when(migratable.getVersion()).thenReturn(VERSION);
    Mockito.when(context.getReport()).thenReturn(report);
  }

  @Test
  public void testDoIncompatibleImport() throws Exception {
    Mockito.when(report.record(Mockito.any(MigrationMessage.class))).thenReturn(report);
    Mockito.when(report.record(Mockito.any(MigrationException.class))).thenReturn(report);

    migratable.doVersionUpgradeImport(context, INCOMPATIBLE_VERSION);

    final ArgumentCaptor<MigrationException> capture =
        ArgumentCaptor.forClass(MigrationException.class);

    Mockito.verify(report).record(capture.capture());

    Assert.assertThat(capture.getValue(), Matchers.instanceOf(MigrationException.class));
    Assert.assertThat(
        capture.getValue().getMessage(),
        Matchers.equalTo(
            "Incompatibility error: unsupported exported migrated version ["
                + INCOMPATIBLE_VERSION
                + "] for migratable ["
                + MIGRATABLE_ID
                + "]; currently supporting ["
                + VERSION
                + "]."));
  }

  @Test
  public void testDoMissingImport() throws Exception {
    Mockito.when(report.record(Mockito.any(MigrationMessage.class))).thenReturn(report);
    Mockito.when(report.record(Mockito.any(MigrationException.class))).thenReturn(report);

    migratable.doMissingImport(context);

    final ArgumentCaptor<MigrationException> capture =
        ArgumentCaptor.forClass(MigrationException.class);

    Mockito.verify(report).record(capture.capture());

    Assert.assertThat(capture.getValue(), Matchers.instanceOf(MigrationException.class));
    Assert.assertThat(
        capture.getValue().getMessage(),
        Matchers.equalTo(
            "Incompatibility error: missing exported data for migratable ["
                + MIGRATABLE_ID
                + "]; currently supporting ["
                + VERSION
                + "]."));
  }
}
