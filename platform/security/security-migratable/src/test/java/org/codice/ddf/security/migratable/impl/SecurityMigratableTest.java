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
package org.codice.ddf.security.migratable.impl;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationMetadata;
import org.codice.ddf.migration.MigrationWarning;
import org.codice.ddf.migration.util.MigratableUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class SecurityMigratableTest {

    private static final String DDF_BASE_DIR = "ddf";

    private static final Path PDP_POLICIES_DIR_REL_PATH = Paths.get("etc", "pdp");

    private static final String CRL_PROP_KEY = "org.apache.ws.security.crypto.merlin.x509crl.file";

    private static final Path EXPORTED_REL_PATH = Paths.get("etc", "exported");

    private static final String DESCRIPTION = "Exports Security system files";

    private static final Path FILE_CONTAINING_CRL_LOCATION = Paths.get("etc",
            "ws-security",
            "server",
            "encryption.properties");

    private static final boolean IS_OPTIONAL = false;

    private Path ddfHome;

    private Path exportDirectory;

    @Before
    public void setup() throws Exception {
        ddfHome = Paths.get(DDF_BASE_DIR);
        exportDirectory = ddfHome.resolve(EXPORTED_REL_PATH);
    }

    @Test
    public void testExportValidRelativePaths() throws Exception {
        // Setup
        MigratableUtil migratableUtil = mock(MigratableUtil.class);
        SecurityMigratable securityMigratable = new SecurityMigratable(DESCRIPTION,
                IS_OPTIONAL,
                migratableUtil);

        securityMigratable.export(exportDirectory);

        assertCrlExport(migratableUtil);
        assertPdpDirectoryExport(migratableUtil);
    }

    @Test
    public void testWarningsReturned() throws Exception {
        MigratableUtil migratableUtil = mock(MigratableUtil.class);
        MigrationWarning expectedWarning1 = new MigrationWarning("Expected Warning 1");
        MigrationWarning expectedWarning2 = new MigrationWarning("Expected Warning 2");
        List<MigrationWarning> expectedWarnings = new ArrayList<>();
        expectedWarnings.add(expectedWarning1);
        expectedWarnings.add(expectedWarning2);

        doAnswer(new MigrationWarningAnswerFourArgs(expectedWarning1)).when(migratableUtil)
                .copyFileFromJavaPropertyValue(eq(FILE_CONTAINING_CRL_LOCATION),
                        eq(CRL_PROP_KEY),
                        eq(exportDirectory),
                        Matchers.<Collection<MigrationWarning>>any());

        doAnswer(new MigrationWarningAnswerThreeArgs(expectedWarning2)).when(migratableUtil)
                .copyDirectory(eq(PDP_POLICIES_DIR_REL_PATH),
                        eq(exportDirectory),
                        Matchers.<Collection<MigrationWarning>>any());

        SecurityMigratable securityMigratable = new SecurityMigratable(DESCRIPTION,
                IS_OPTIONAL,
                migratableUtil);
        MigrationMetadata migrationMetadata = securityMigratable.export(exportDirectory);

        assertThat(migrationMetadata.getMigrationWarnings(),
                containsInAnyOrder(expectedWarnings.toArray()));

    }

    @Test(expected = MigrationException.class)
    public void testExportExceptionThrownWhenCopyingDirectory() throws Exception {
        MigratableUtil mockMigratableUtil = mock(MigratableUtil.class);
        doThrow(MigrationException.class).when(mockMigratableUtil)
                .copyDirectory(any(Path.class),
                        eq(exportDirectory),
                        Matchers.<Collection<MigrationWarning>>any());
        SecurityMigratable platformMigratable = new SecurityMigratable(DESCRIPTION,
                IS_OPTIONAL,
                mockMigratableUtil);

        platformMigratable.export(exportDirectory);
    }

    @Test(expected = MigrationException.class)
    public void testExportExceptionThrownWhenCopyingFileFromJavaProperty() throws Exception {
        MigratableUtil mockMigratableUtil = mock(MigratableUtil.class);
        doThrow(MigrationException.class).when(mockMigratableUtil)
                .copyFileFromJavaPropertyValue(any(Path.class),
                        any(String.class),
                        eq(exportDirectory),
                        Matchers.<Collection<MigrationWarning>>any());
        SecurityMigratable platformMigratable = new SecurityMigratable(DESCRIPTION,
                IS_OPTIONAL,
                mockMigratableUtil);

        platformMigratable.export(exportDirectory);
    }

    @Test(expected = MigrationException.class)
    public void testExportExceptionThrownWhenCopyingFile() throws Exception {
        // Setup
        MigratableUtil mockMigratableUtil = mock(MigratableUtil.class);
        doThrow(MigrationException.class).when(mockMigratableUtil)
                .copyFileFromJavaPropertyValue(any(Path.class),
                        any(String.class),
                        eq(exportDirectory),
                        Matchers.<Collection<MigrationWarning>>any());
        SecurityMigratable platformMigratable = new SecurityMigratable(DESCRIPTION,
                IS_OPTIONAL,
                mockMigratableUtil);

        // Perform test
        platformMigratable.export(exportDirectory);
    }

    private void assertCrlExport(MigratableUtil mockMigratableUtil) {
        verify(mockMigratableUtil).copyFileFromJavaPropertyValue(eq(FILE_CONTAINING_CRL_LOCATION),
                eq(CRL_PROP_KEY),
                eq(exportDirectory),
                anyCollectionOf(MigrationWarning.class));
    }

    private void assertPdpDirectoryExport(MigratableUtil mockMigratableUtil) {
        verify(mockMigratableUtil).copyDirectory(eq(PDP_POLICIES_DIR_REL_PATH),
                eq(exportDirectory),
                anyCollectionOf(MigrationWarning.class));
    }

    private class MigrationWarningAnswerThreeArgs implements Answer<Void> {

        private final MigrationWarning expectedWarning;

        private MigrationWarningAnswerThreeArgs(MigrationWarning expectedWarning) {
            this.expectedWarning = expectedWarning;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Void answer(InvocationOnMock invocation) throws Throwable {
            Object[] args = invocation.getArguments();
            ((Collection<MigrationWarning>) args[2]).add(expectedWarning);
            return null;
        }
    }

    private class MigrationWarningAnswerFourArgs implements Answer<Void> {

        private final MigrationWarning expectedWarning;

        private MigrationWarningAnswerFourArgs(MigrationWarning expectedWarning) {
            this.expectedWarning = expectedWarning;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Void answer(InvocationOnMock invocation) throws Throwable {
            Object[] args = invocation.getArguments();
            ((Collection<MigrationWarning>) args[3]).add(expectedWarning);
            return null;
        }
    }
}
