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
package org.codice.ddf.security.handler.pki.migratable.impl;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.codice.ddf.migration.MigrationMetadata;
import org.codice.ddf.migration.util.MigratableUtil;
import org.codice.ddf.security.handler.pki.CrlChecker;
import org.junit.Test;

public class CrlMigratableTest {

    private static final String DESCRIPTION = "Exports the CRL";

    private static final boolean IS_OPTIONAL = false;
    
    private static final Path DDF_HOME = Paths.get("/", "ddf");

    private static final Path CRL_PATH = Paths.get("my", "crl.pem");
    
    private static final Path EXPORT_DIRECTORY = DDF_HOME.resolve(Paths.get("ddf", "etc", "exported"));

    @Test
    public void testExportCrlEnabled() throws Exception {
        // Setup
        MigratableUtil mockMigratableUtil = mock(MigratableUtil.class);
        CrlChecker mockCrlChecker = mock(CrlChecker.class);
        when(mockCrlChecker.getCrlPath()).thenReturn(CRL_PATH); 
        when(mockCrlChecker.isCrlEnabled()).thenReturn(true);

        CrlMigratable crlMigratable = new CrlMigratable(DESCRIPTION, IS_OPTIONAL, mockCrlChecker,
                mockMigratableUtil);

        // Perform Test
        MigrationMetadata migrationMetadata = crlMigratable.export(EXPORT_DIRECTORY);
        
        // Verify
        verify(mockMigratableUtil).copyFile(eq(CRL_PATH), eq(EXPORT_DIRECTORY), any());
        assertThat(migrationMetadata.getMigrationWarnings().size(), is(0));
    }
    
    @Test
    public void testExportCrlDisabled() throws Exception {
        // Setup
        MigratableUtil mockMigratableUtil = mock(MigratableUtil.class);
        CrlChecker mockCrlChecker = mock(CrlChecker.class);
        when(mockCrlChecker.getCrlPath()).thenReturn(null); 
        when(mockCrlChecker.isCrlEnabled()).thenReturn(false);

        CrlMigratable crlMigratable = new CrlMigratable(DESCRIPTION, IS_OPTIONAL, mockCrlChecker,
                mockMigratableUtil);

        // Perform Test
        MigrationMetadata migrationMetadata = crlMigratable.export(EXPORT_DIRECTORY);
        
        // Verify
        verify(mockMigratableUtil, times(0)).copyFile(any(), eq(EXPORT_DIRECTORY), any());
        assertThat(migrationMetadata.getMigrationWarnings().size(), is(1));
    }
}
