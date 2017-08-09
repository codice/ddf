/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
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

    private final Migratable MIGRATABLE = Mockito.mock(Migratable.class,
            Mockito.CALLS_REAL_METHODS);

    private final MigrationReport REPORT = Mockito.mock(MigrationReport.class);

    private final ImportMigrationContext CONTEXT = Mockito.mock(ImportMigrationContext.class);

    @Before
    public void before() {
        Mockito.when(MIGRATABLE.getId())
                .thenReturn(MIGRATABLE_ID);
        Mockito.when(MIGRATABLE.getVersion())
                .thenReturn(VERSION);
        Mockito.when(CONTEXT.getReport())
                .thenReturn(REPORT);
    }

    @Test
    public void testDoIncompatibleImport() throws Exception {
        Mockito.when(REPORT.record(Mockito.any(MigrationMessage.class)))
                .thenReturn(REPORT);
        Mockito.when(REPORT.record(Mockito.any(MigrationException.class)))
                .thenReturn(REPORT);

        MIGRATABLE.doIncompatibleImport(CONTEXT, INCOMPATIBLE_VERSION);

        final ArgumentCaptor<MigrationException> CAPTURE = ArgumentCaptor.forClass(
                MigrationException.class);

        Mockito.verify(REPORT)
                .record(CAPTURE.capture());

        Assert.assertThat(CAPTURE.getValue(), Matchers.instanceOf(IncompatibleMigrationException.class));
        Assert.assertThat(CAPTURE.getValue()
                        .getMessage(),
                Matchers.matchesPattern(
                        ".*\\[" + INCOMPATIBLE_VERSION + "\\].*migratable \\[" + MIGRATABLE_ID
                                + "\\].*supporting \\[" + VERSION + "\\]"));
    }

    @Test
    public void testDoIncompatibleImportWithNullVersion() throws Exception {
        final String VERSION = null;

        Mockito.when(MIGRATABLE.getVersion())
                .thenReturn(VERSION);
        Mockito.when(REPORT.record(Mockito.any(MigrationMessage.class)))
                .thenReturn(REPORT);
        Mockito.when(REPORT.record(Mockito.any(MigrationException.class)))
                .thenReturn(REPORT);

        MIGRATABLE.doIncompatibleImport(CONTEXT, INCOMPATIBLE_VERSION);

        final ArgumentCaptor<MigrationException> CAPTURE = ArgumentCaptor.forClass(
                MigrationException.class);

        Mockito.verify(REPORT)
                .record(CAPTURE.capture());

        Assert.assertThat(CAPTURE.getValue(), Matchers.instanceOf(IncompatibleMigrationException.class));
        Assert.assertThat(CAPTURE.getValue()
                        .getMessage(),
                Matchers.matchesPattern(
                        ".*\\[" + INCOMPATIBLE_VERSION + "\\].*migratable \\[" + MIGRATABLE_ID
                                + "\\].*supporting \\[" + VERSION + "\\]"));
    }

    @Test
    public void testDoIncompatibleImportWithNullIncompatibleVersion() throws Exception {
        final String INCOMPATIBLE_VERSION = null;

        Mockito.when(REPORT.record(Mockito.any(MigrationMessage.class)))
                .thenReturn(REPORT);
        Mockito.when(REPORT.record(Mockito.any(MigrationException.class)))
                .thenReturn(REPORT);

        MIGRATABLE.doIncompatibleImport(CONTEXT, INCOMPATIBLE_VERSION);

        final ArgumentCaptor<MigrationException> CAPTURE = ArgumentCaptor.forClass(
                MigrationException.class);

        Mockito.verify(REPORT)
                .record(CAPTURE.capture());

        Assert.assertThat(CAPTURE.getValue(), Matchers.instanceOf(IncompatibleMigrationException.class));
        Assert.assertThat(CAPTURE.getValue()
                        .getMessage(),
                Matchers.matchesPattern(
                        ".*\\[" + INCOMPATIBLE_VERSION + "\\].*migratable \\[" + MIGRATABLE_ID
                                + "\\].*supporting \\[" + VERSION + "\\]"));
    }

    @Test
    public void testDoIncompatibleImportWithNullVersions() throws Exception {
        final String VERSION = null;
        final String INCOMPATIBLE_VERSION = null;

        Mockito.when(MIGRATABLE.getVersion())
                .thenReturn(VERSION);
        Mockito.when(REPORT.record(Mockito.any(MigrationMessage.class)))
                .thenReturn(REPORT);
        Mockito.when(REPORT.record(Mockito.any(MigrationException.class)))
                .thenReturn(REPORT);

        MIGRATABLE.doIncompatibleImport(CONTEXT, INCOMPATIBLE_VERSION);

        final ArgumentCaptor<MigrationException> CAPTURE = ArgumentCaptor.forClass(
                MigrationException.class);

        Mockito.verify(REPORT)
                .record(CAPTURE.capture());

        Assert.assertThat(CAPTURE.getValue(), Matchers.instanceOf(IncompatibleMigrationException.class));
        Assert.assertThat(CAPTURE.getValue()
                        .getMessage(),
                Matchers.matchesPattern(
                        ".*\\[" + INCOMPATIBLE_VERSION + "\\].*migratable \\[" + MIGRATABLE_ID
                                + "\\].*supporting \\[" + VERSION + "\\]"));
    }
}
