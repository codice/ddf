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
package org.codice.ddf.platform.migratable.impl;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
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
import org.codice.ddf.migration.MigrationWarning;
import org.codice.ddf.migration.util.MigratableUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.class)
public class PlatformMigratableTest {

    private static final String DDF_BASE_DIR = "ddf";

    private static final String KEYSTORE_SYSTEM_PROP = "javax.net.ssl.keyStore";

    private static final String TRUSTSTORE_SYSTEM_PROP = "javax.net.ssl.trustStore";

    private static final Path WS_SECURITY_DIR_REL_PATH = Paths.get("etc", "ws-security");

    private static final Path SYSTEM_PROPERTIES_REL_PATH = Paths.get("etc", "system.properties");

    private static final Path USERS_PROPERTIES_REL_PATH = Paths.get("etc", "users.properties");

    private static final Path USERS_ATTRIBUTES_REL_PATH = Paths.get("etc", "users.attributes");

    private static final Path APPLICATION_LIST = Paths.get("etc",
            "org.codice.ddf.admin.applicationlist.properties");

    private static final Path EXPORTED_DIR_REL_PATH = Paths.get("etc", "exported");

    private static final String DESCRIPTION = "Exports system files";

    private static final String ORGANIZATION = "organization";

    private static final String TITLE = "title";

    private static final String ID = "id";

    private static final String VERSION = "version";

    private static final DescribableBean DESCRIBABLE_BEAN = new DescribableBean(VERSION,
            ID,
            TITLE,
            DESCRIPTION,
            ORGANIZATION);

    private Path ddfHome;

    private Path exportDirectory;

    @Before
    public void setup() throws Exception {
        ddfHome = Paths.get(DDF_BASE_DIR);
        exportDirectory = ddfHome.resolve(EXPORTED_DIR_REL_PATH);
    }

    @Test
    public void testExportValidRelativePaths() throws Exception {
        // Setup
        MigratableUtil mockMigratableUtil = mock(MigratableUtil.class);
        PlatformMigratable platformMigratable = new PlatformMigratable(DESCRIBABLE_BEAN,
                mockMigratableUtil);

        // Perform Test
        platformMigratable.export(exportDirectory);

        // Verify
        assertWsSecurityExport(mockMigratableUtil);
        assertSystemPropertiesFilesExport(mockMigratableUtil);
        assertKeystoresExport(mockMigratableUtil);
        assertAppListExported(mockMigratableUtil);
    }

    /**
     * Verify that if an absolute path is encountered during the export, a warning is returned.
     */
    @Test
    public void testExportWarningsReturnedWhenExportingKeystoreTruststoreAppList()
            throws Exception {
        // Setup
        MigratableUtil mockMigratableUtil = mock(MigratableUtil.class);
        MigrationWarning expectedMigrationWarning1 = new MigrationWarning("warning 1");
        doAnswer(new MigrationWarningAnswer(expectedMigrationWarning1)).when(mockMigratableUtil)
                .copyFileFromSystemPropertyValue(eq(KEYSTORE_SYSTEM_PROP),
                        eq(exportDirectory),
                        Matchers.<Collection<MigrationWarning>>any());
        MigrationWarning expectedMigrationWarning2 = new MigrationWarning("warning 2");
        doAnswer(new MigrationWarningAnswer(expectedMigrationWarning2)).when(mockMigratableUtil)
                .copyFileFromSystemPropertyValue(eq(TRUSTSTORE_SYSTEM_PROP),
                        eq(exportDirectory),
                        Matchers.<Collection<MigrationWarning>>any());
        MigrationWarning expectedMigrationWarning3 = new MigrationWarning("warning 3");
        doAnswer(new MigrationWarningAnswer(expectedMigrationWarning3)).when(mockMigratableUtil)
                .copyFile(eq(APPLICATION_LIST),
                        eq(exportDirectory),
                        Matchers.<Collection<MigrationWarning>>any());

        PlatformMigratable platformMigratable = new PlatformMigratable(DESCRIBABLE_BEAN,
                mockMigratableUtil);

        // Perform Test
        MigrationMetadata migrationMetadata = platformMigratable.export(exportDirectory);

        // Verify
        List<MigrationWarning> expectedWarnings = new ArrayList<>();
        expectedWarnings.add(expectedMigrationWarning1);
        expectedWarnings.add(expectedMigrationWarning2);
        expectedWarnings.add(expectedMigrationWarning3);
        assertThat(migrationMetadata.getMigrationWarnings(),
                containsInAnyOrder(expectedWarnings.toArray()));
    }

    @Test
    public void testExportWarningsReturnedWhenExportingSystemPropertiesFiles() throws Exception {
        // Setup
        MigratableUtil mockMigratableUtil = mock(MigratableUtil.class);
        MigrationWarning expectedMigrationWarning1 = new MigrationWarning("warning 1");
        doAnswer(new MigrationWarningAnswer(expectedMigrationWarning1)).when(mockMigratableUtil)
                .copyFile(eq(SYSTEM_PROPERTIES_REL_PATH),
                        eq(exportDirectory),
                        Matchers.<Collection<MigrationWarning>>any());
        MigrationWarning expectedMigrationWarning2 = new MigrationWarning("warning 2");
        doAnswer(new MigrationWarningAnswer(expectedMigrationWarning2)).when(mockMigratableUtil)
                .copyFile(eq(USERS_PROPERTIES_REL_PATH),
                        eq(exportDirectory),
                        Matchers.<Collection<MigrationWarning>>any());

        PlatformMigratable platformMigratable = new PlatformMigratable(DESCRIBABLE_BEAN,
                mockMigratableUtil);

        // Perform Test
        MigrationMetadata migrationMetadata = platformMigratable.export(exportDirectory);

        // Verify
        List<MigrationWarning> expectedWarnings = new ArrayList<>();
        expectedWarnings.add(expectedMigrationWarning1);
        expectedWarnings.add(expectedMigrationWarning2);
        assertThat(migrationMetadata.getMigrationWarnings(),
                containsInAnyOrder(expectedWarnings.toArray()));
    }

    @Test
    public void testExportWarningReturnedWhenExportingWsSecurity() throws Exception {
        // Setup
        MigratableUtil mockMigratableUtil = mock(MigratableUtil.class);
        MigrationWarning expectedMigrationWarning = new MigrationWarning("warning 1");
        doAnswer(new MigrationWarningAnswer(expectedMigrationWarning)).when(mockMigratableUtil)
                .copyDirectory(eq(WS_SECURITY_DIR_REL_PATH),
                        eq(exportDirectory),
                        Matchers.<Collection<MigrationWarning>>any());

        PlatformMigratable platformMigratable = new PlatformMigratable(DESCRIBABLE_BEAN,
                mockMigratableUtil);

        // Perform Test
        MigrationMetadata migrationMetadata = platformMigratable.export(exportDirectory);

        // Verify
        assertThat(migrationMetadata.getMigrationWarnings()
                .contains(expectedMigrationWarning), is(true));
    }

    @Test(expected = MigrationException.class)
    public void testExportExceptionThrownWhenCopyingFile() throws Exception {
        // Setup
        MigratableUtil mockMigratableUtil = mock(MigratableUtil.class);
        doThrow(MigrationException.class).when(mockMigratableUtil)
                .copyFile(any(Path.class),
                        eq(exportDirectory),
                        Matchers.<Collection<MigrationWarning>>any());
        PlatformMigratable platformMigratable = new PlatformMigratable(DESCRIBABLE_BEAN,
                mockMigratableUtil);

        // Perform test
        platformMigratable.export(exportDirectory);
    }

    @Test(expected = MigrationException.class)
    public void testExportExceptionThrownWhenCopyingDirectory() throws Exception {
        // Setup
        MigratableUtil mockMigratableUtil = mock(MigratableUtil.class);
        doThrow(MigrationException.class).when(mockMigratableUtil)
                .copyDirectory(any(Path.class),
                        eq(exportDirectory),
                        Matchers.<Collection<MigrationWarning>>any());
        PlatformMigratable platformMigratable = new PlatformMigratable(DESCRIBABLE_BEAN,
                mockMigratableUtil);

        // Perform test
        platformMigratable.export(exportDirectory);
    }

    @Test(expected = MigrationException.class)
    public void testExportExceptionThrownWhenCopyingFileFromSystemPropertyValue() throws Exception {
        // Setup
        MigratableUtil mockMigratableUtil = mock(MigratableUtil.class);
        doThrow(MigrationException.class).when(mockMigratableUtil)
                .copyFileFromSystemPropertyValue(any(String.class),
                        eq(exportDirectory),
                        Matchers.<Collection<MigrationWarning>>any());
        PlatformMigratable platformMigratable = new PlatformMigratable(DESCRIBABLE_BEAN,
                mockMigratableUtil);

        // Perform test
        platformMigratable.export(exportDirectory);
    }

    private void assertWsSecurityExport(MigratableUtil mockMigratableUtil) {
        verify(mockMigratableUtil).copyDirectory(eq(WS_SECURITY_DIR_REL_PATH),
                eq(exportDirectory),
                Matchers.<Collection<MigrationWarning>>any());
    }

    private void assertSystemPropertiesFilesExport(MigratableUtil mockMigratableUtil) {
        verify(mockMigratableUtil).copyFile(eq(SYSTEM_PROPERTIES_REL_PATH),
                eq(exportDirectory),
                Matchers.any());
        verify(mockMigratableUtil).copyFile(eq(USERS_PROPERTIES_REL_PATH),
                eq(exportDirectory),
                Matchers.any());
        verify(mockMigratableUtil).copyFile(eq(USERS_ATTRIBUTES_REL_PATH),
                eq(exportDirectory),
                Matchers.any());
    }

    private void assertKeystoresExport(MigratableUtil mockMigratableUtil) {
        verify(mockMigratableUtil).copyFileFromSystemPropertyValue(eq(KEYSTORE_SYSTEM_PROP),
                eq(exportDirectory),
                Matchers.<Collection<MigrationWarning>>any());
        verify(mockMigratableUtil).copyFileFromSystemPropertyValue(eq(TRUSTSTORE_SYSTEM_PROP),
                eq(exportDirectory),
                Matchers.<Collection<MigrationWarning>>any());
    }

    private void assertAppListExported(MigratableUtil mockMigratableUtil) {
        verify(mockMigratableUtil).copyFile(eq(APPLICATION_LIST),
                eq(exportDirectory),
                Matchers.<Collection<MigrationWarning>>any());
    }

    private class MigrationWarningAnswer implements Answer<Void> {

        private final MigrationWarning expectedWarning;

        private MigrationWarningAnswer(MigrationWarning expectedWarning) {
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
}
