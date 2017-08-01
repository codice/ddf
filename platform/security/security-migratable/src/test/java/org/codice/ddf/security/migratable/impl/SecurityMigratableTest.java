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
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationWarning;
import org.codice.ddf.migration.util.MigratableUtil;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class SecurityMigratableTest {

    private static final String DDF_BASE_DIR = "ddf";

    private static final Path PDP_POLICIES_DIR_REL_PATH = Paths.get("etc", "pdp");

    private static final String CRL_PROP_KEY = "org.apache.ws.security.crypto.merlin.x509crl.file";

    private static final String DESCRIPTION = "Exports Security system files";

    private static final String ORGANIZATION = "organization";

    private static final String TITLE = "title";

    private static final String ID = "id";

    private static final String VERSION = "version";

    private static final DescribableBean DESCRIBABLE_BEAN = new DescribableBean(VERSION,
            ID,
            TITLE,
            DESCRIPTION,
            ORGANIZATION);

    private static final Path SERVER_ENCRYPTION_PROPERTIES_PATH = Paths.get("etc",
            "ws-security",
            "server",
            "encryption.properties");

    private static final Path SERVER_SIGNATURE_PROPERTIES_PATH = Paths.get("etc",
            "ws-security",
            "server",
            "signature.properties");

    private static final Path ISSUER_ENCRYPTION_PROPERTIES_PATH = Paths.get("etc",
            "ws-security",
            "issuer",
            "encryption.properties");

    private static final Path ISSUER_SIGNATURE_PROPERTIES_PATH = Paths.get("etc",
            "ws-security",
            "issuer",
            "signature.properties");

    private static final Path EXPECTED_SERVER_SIGNATURE_CRL_PATH = Paths.get("ddf",
            "crl",
            "serverSignature",
            "crl.pem");

    private static final Path EXPECTED_SERVER_ENCRYPTION_CRL_PATH = Paths.get("ddf",
            "crl",
            "serverEncryption",
            "crl.pem");

    private static final Path EXPECTED_ISSUER_SIGNATURE_CRL_PATH = Paths.get("ddf",
            "crl",
            "issuerSignature",
            "crl.pem");

    private static final Path EXPECTED_ISSUER_ENCRYPTION_CRL_PATH = Paths.get("ddf",
            "crl",
            "issuerEncryption",
            "crl.pem");

    private static final Path DDF_HOME = Paths.get(DDF_BASE_DIR);

    private static final Path EXPORT_DIRECTORY = DDF_HOME.resolve(Paths.get("etc", "exported"));

    @Test
    public void testExportValidRelativePaths() throws Exception {
        // Setup
        MigratableUtil mockMigratableUtil = mock(MigratableUtil.class);
        when(mockMigratableUtil.getJavaPropertyValue(SERVER_ENCRYPTION_PROPERTIES_PATH,
                CRL_PROP_KEY)).thenReturn(EXPECTED_SERVER_ENCRYPTION_CRL_PATH.toString());
        when(mockMigratableUtil.getJavaPropertyValue(SERVER_SIGNATURE_PROPERTIES_PATH,
                CRL_PROP_KEY)).thenReturn(EXPECTED_SERVER_SIGNATURE_CRL_PATH.toString());
        when(mockMigratableUtil.getJavaPropertyValue(ISSUER_ENCRYPTION_PROPERTIES_PATH,
                CRL_PROP_KEY)).thenReturn(EXPECTED_ISSUER_ENCRYPTION_CRL_PATH.toString());
        when(mockMigratableUtil.getJavaPropertyValue(ISSUER_SIGNATURE_PROPERTIES_PATH,
                CRL_PROP_KEY)).thenReturn(EXPECTED_ISSUER_SIGNATURE_CRL_PATH.toString());
        SecurityMigratable securityMigratable = new SecurityMigratable(DESCRIBABLE_BEAN,
                mockMigratableUtil);

        // Perform Test
        securityMigratable.export(EXPORT_DIRECTORY);

        // Verify
        assertCrlExport(mockMigratableUtil);
        assertPdpDirectoryExport(mockMigratableUtil);
    }

    @Test
    public void testWarningsReturned() throws Exception {
        MigratableUtil migratableUtil = mock(MigratableUtil.class);
        MigrationWarning expectedWarning = new MigrationWarning("Expected Warning");
        doAnswer(new MigrationWarningAnswer(expectedWarning)).when(migratableUtil)
                .copyDirectory(eq(PDP_POLICIES_DIR_REL_PATH),
                        eq(EXPORT_DIRECTORY),
                        Matchers.<Collection<MigrationWarning>>any());

        SecurityMigratable securityMigratable = new SecurityMigratable(DESCRIBABLE_BEAN,
                migratableUtil);
        MigrationMetadata migrationMetadata = securityMigratable.export(EXPORT_DIRECTORY);

        assertThat(migrationMetadata.getMigrationWarnings(), containsInAnyOrder(expectedWarning));

    }

    @Test(expected = MigrationException.class)
    public void testExportExceptionThrownWhenCopyingDirectory() throws Exception {
        MigratableUtil mockMigratableUtil = mock(MigratableUtil.class);
        doThrow(MigrationException.class).when(mockMigratableUtil)
                .copyDirectory(any(Path.class),
                        eq(EXPORT_DIRECTORY),
                        Matchers.<Collection<MigrationWarning>>any());
        SecurityMigratable securityMigratable = new SecurityMigratable(DESCRIBABLE_BEAN,
                mockMigratableUtil);

        securityMigratable.export(EXPORT_DIRECTORY);
    }

    @Test(expected = MigrationException.class)
    public void testExportExceptionThrownWhenCopyingFile() throws Exception {
        // Setup
        MigratableUtil mockMigratableUtil = mock(MigratableUtil.class);
        when(mockMigratableUtil.getJavaPropertyValue(SERVER_ENCRYPTION_PROPERTIES_PATH,
                CRL_PROP_KEY)).thenReturn(EXPECTED_SERVER_ENCRYPTION_CRL_PATH.toString());
        doThrow(MigrationException.class).when(mockMigratableUtil)
                .copyFile(any(Path.class),
                        eq(EXPORT_DIRECTORY),
                        Matchers.<Collection<MigrationWarning>>any());
        SecurityMigratable securityMigratable = new SecurityMigratable(DESCRIBABLE_BEAN,
                mockMigratableUtil);

        // Perform test
        securityMigratable.export(EXPORT_DIRECTORY);
    }

    @Test
    public void testExportCrlIsNull() {
        // Setup
        MigratableUtil mockMigratableUtil = mock(MigratableUtil.class);
        when(mockMigratableUtil.getJavaPropertyValue(SERVER_ENCRYPTION_PROPERTIES_PATH,
                CRL_PROP_KEY)).thenReturn(null);
        SecurityMigratable securityMigratable = new SecurityMigratable(DESCRIBABLE_BEAN,
                mockMigratableUtil);

        // Perform test
        MigrationMetadata migrationMetadata = securityMigratable.export(EXPORT_DIRECTORY);

        // Verify
        verify(mockMigratableUtil, never()).copyFile(eq(EXPECTED_SERVER_ENCRYPTION_CRL_PATH),
                eq(EXPORT_DIRECTORY),
                anyCollectionOf(MigrationWarning.class));
        assertThat(migrationMetadata.getMigrationWarnings()
                .size(), is(0));
    }

    @Test(expected = MigrationException.class)
    public void testExportCrlIsBlank() {
        // Setup
        MigratableUtil mockMigratableUtil = mock(MigratableUtil.class);
        when(mockMigratableUtil.getJavaPropertyValue(SERVER_ENCRYPTION_PROPERTIES_PATH,
                CRL_PROP_KEY)).thenReturn("");
        SecurityMigratable securityMigratable = new SecurityMigratable(DESCRIBABLE_BEAN,
                mockMigratableUtil);

        // Perform test
        securityMigratable.export(EXPORT_DIRECTORY);
    }

    @Test(expected = MigrationException.class)
    public void testExportExceptionThrownWhenReadingCrlPropsFile() {
        // Setup
        MigratableUtil mockMigratableUtil = mock(MigratableUtil.class);
        doThrow(MigrationException.class).when(mockMigratableUtil)
                .getJavaPropertyValue(SERVER_ENCRYPTION_PROPERTIES_PATH, CRL_PROP_KEY);
        SecurityMigratable securityMigratable = new SecurityMigratable(DESCRIBABLE_BEAN,
                mockMigratableUtil);

        // Perform test
        securityMigratable.export(EXPORT_DIRECTORY);
    }

    private void assertCrlExport(MigratableUtil mockMigratableUtil) {
        verify(mockMigratableUtil).copyFile(eq(EXPECTED_SERVER_ENCRYPTION_CRL_PATH),
                eq(EXPORT_DIRECTORY),
                anyCollectionOf(MigrationWarning.class));
        verify(mockMigratableUtil).copyFile(eq(EXPECTED_SERVER_SIGNATURE_CRL_PATH),
                eq(EXPORT_DIRECTORY),
                anyCollectionOf(MigrationWarning.class));
        verify(mockMigratableUtil).copyFile(eq(EXPECTED_ISSUER_ENCRYPTION_CRL_PATH),
                eq(EXPORT_DIRECTORY),
                anyCollectionOf(MigrationWarning.class));
        verify(mockMigratableUtil).copyFile(eq(EXPECTED_ISSUER_SIGNATURE_CRL_PATH),
                eq(EXPORT_DIRECTORY),
                anyCollectionOf(MigrationWarning.class));
    }

    private void assertPdpDirectoryExport(MigratableUtil mockMigratableUtil) {
        verify(mockMigratableUtil).copyDirectory(eq(PDP_POLICIES_DIR_REL_PATH),
                eq(EXPORT_DIRECTORY),
                anyCollectionOf(MigrationWarning.class));
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
