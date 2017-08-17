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

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipFile;

import javax.management.MBeanServer;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.karaf.system.SystemService;
import org.codice.ddf.configuration.migration.ConfigurationMigrationManager;
import org.codice.ddf.migration.ConfigurationMigratable;
import org.codice.ddf.migration.DataMigratable;
import org.codice.ddf.migration.ExportMigrationContext;
import org.codice.ddf.migration.ImportMigrationContext;
import org.codice.ddf.migration.IncompatibleMigrationException;
import org.codice.ddf.migration.MigrationReport;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableList;

/**
 * This test creates a "mock" ddf directory structure for the system exported from (ddfExport) and "mock"
 * directory structure for the system imported into (ddfImport).  These directory structures are
 * located under the created TemporaryFolder.
 * For example, if the TemporaryFolder is /private/var/folders/2j/q2gjqn4s2mv53c2q53_m9d_w0000gn/T/junit7611336125982191987,
 * one would see a similar directory structure to the following after initial setup and an import:
 *
 *  // This is the system exported from:
 * ./ddfExport
 * ./ddfExport/bin
 * ./ddfExport/etc
 * ./ddfExport/etc/certs
 * ./ddfExport/etc/certs/demoCA/crl/crl.pem
 * ./ddfExport/etc/pdp/xacml.xml
 * ./ddfExport/etc/ws-security/issuer/encryption.properties
 * ./ddfExport/etc/ws-security/issuer/signature.properties
 * ./ddfExport/etc/ws-security/server/encryption.properties
 * ./ddfExport/etc/ws-security/server/signature.properties
 * ./ddfExport/Version.txt
 *
 *  // This is the system imported into:
 * ./ddfImport
 * ./ddfImport/bin
 * ./ddfImport/etc
 * ./ddfExport/etc/certs/demoCA/crl/crl.pem
 * ./ddfImport/etc/pdp/xacml.xml
 * ./ddfImport/etc/ws-security/issuer/encryption.properties
 * ./ddfImport/etc/ws-security/issuer/signature.properties
 * ./ddfImport/etc/ws-security/server/encryption.properties
 * ./ddfImport/etc/ws-security/server/signature.properties
 * ./ddfImport/Version.txt
 *
 *  // The backup from the imported system will look similar to this:
 * ./exported-1.0-20170814T152846.zip
 *
 *  // Thw exported zip from ddfExport will look similar to this:
 * ./exported-1.0.zip
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class SecurityMigratableTest {
    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    private static final String SUPPORTED_VERSION = "1.0";

    private static final String UNSUPPORTED_VERSION = "666.0";

    private static final String DDF_EXPORTED_HOME = "ddfExport";

    private static final String DDF_IMPORTED_HOME = "ddfImport";

    private static final String DDF_EXPORTED_TAG_TEMPLATE = "exported_from_%s";

    private static final String DDF_IMPORTED_TAG = "original";

    private static final String DDF_HOME_SYSTEM_PROP_KEY = "ddf.home";

    private Path ddfHome;

    @Mock
    private ExportMigrationContext mockExportMigrationContext;

    @Mock
    private ImportMigrationContext mockImportMigrationContext;

    @Mock
    private MigrationReport mockMigrationReport;

    @Mock
    private ZipFile mockZipFile;

    @Mock
    private MBeanServer mBeanServer;

    @Mock
    private SystemService systemService;

    private static final Path PDP_POLICIES_DIR = Paths.get("etc", "pdp");

    private static final List<Path> PROPERTIES_FILES = ImmutableList.of(
            Paths.get("etc", "ws-security", "server", "encryption.properties"),
            Paths.get("etc", "ws-security", "server", "signature.properties"),
            Paths.get("etc", "ws-security", "issuer", "encryption.properties"),
            Paths.get("etc", "ws-security", "issuer", "signature.properties"));

    private static final String CRL_PROP_KEY = "org.apache.ws.security.crypto.merlin.x509crl.file";

    private static final Path CRL = Paths.get("etc", "certs", "demoCA", "crl", "crl.pem");

    private static final String XACML_POLICY = "xacml.xml";

    @Before
    public void before() throws IOException {
        // The tag is written to all exported files so that we can verify the file actually
        // got copied on import (ie. read the tags in files in the imported system and
        // verify that they are the ones from the exported system).
        String tag = String.format(DDF_EXPORTED_TAG_TEMPLATE, DDF_EXPORTED_HOME);
        setup(DDF_EXPORTED_HOME, tag);
    }

    /**
     * Verify that when an unsupported exported version is imported, an exception is recorded in
     * the migration report.
     */
    @Test
    public void testDoImportUnsupportedMigratedVersion() {
        // Setup
        when(mockImportMigrationContext.getReport()).thenReturn(mockMigrationReport);
        SecurityMigratable securityMigratable = new SecurityMigratable();

        // Perform Test
        securityMigratable.doIncompatibleImport(mockImportMigrationContext, UNSUPPORTED_VERSION);

        // Verify
        verify(mockImportMigrationContext).getReport();
        verify(mockMigrationReport).record(any(IncompatibleMigrationException.class));
        verifyNoMoreInteractions(mockImportMigrationContext);
    }

    /**
     * Verify the when the system is in a default configuration, all of Security Migratable's files
     * are successfully exported from system ddfExport and successfully imported into system ddfImport.
     */
    @Test
    public void testDoExportAndDoImport() throws IOException {
        // Setup export
        Path exportDir = tempDir.getRoot()
                .toPath()
                .toRealPath();

        SecurityMigratable eSecurityMigratable = new SecurityMigratable();
        List<ConfigurationMigratable> eConfigMigratables = Arrays.asList(eSecurityMigratable);
        List<DataMigratable> eDataMigratables = Collections.emptyList();
        ConfigurationMigrationManager eConfigurationMigrationManager =
                new ConfigurationMigrationManager(mBeanServer,
                        eConfigMigratables,
                        eDataMigratables,
                        systemService);

        // Perform export
        MigrationReport exportReport = eConfigurationMigrationManager.doExport(exportDir);

        // Verify export
        assertThat("The export report has errors.", exportReport.hasErrors(), is(false));
        assertThat("The export report has warnings.", exportReport.hasWarnings(), is(false));
        assertThat("Export was not successful.", exportReport.wasSuccessful(), is(true));
        String exportedZipBaseName = String.format("exported-%s.zip", SUPPORTED_VERSION);
        Path exportedZip = exportDir.resolve(exportedZipBaseName).toRealPath();
        assertThat("Export zip does not exist.", exportedZip.toFile().exists(), is(true));
        assertThat("Exported zip is empty.", exportedZip.toFile().length(), greaterThan(0L));

        // Setup import
        setup(DDF_IMPORTED_HOME, DDF_IMPORTED_TAG);

        SecurityMigratable iSecurityMigratable = new SecurityMigratable();
        List<ConfigurationMigratable> iConfigMigratables = Arrays.asList(iSecurityMigratable);
        List<DataMigratable> iDataMigratables = Collections.emptyList();
        ConfigurationMigrationManager iConfigurationMigrationManager =
                new ConfigurationMigrationManager(mBeanServer,
                        iConfigMigratables,
                        iDataMigratables,
                        systemService);

        MigrationReport importReport = iConfigurationMigrationManager.doImport(exportDir);

        // Verify import
        assertThat("The import report has errors.", importReport.hasErrors(), is(false));
        assertThat("The import report has warnings.", importReport.hasWarnings(), is(false));
        assertThat("Import was not successful.", importReport.wasSuccessful(), is(true));
        verifyPdpFilesImported();
        verifyCrlImported();
    }

    /**
     * Verifies that when no CRL (optional) is specified, the export and import both succeed.
     */
    @Test
    public void testDoExportAndDoImportPdpFileExistsNoCrl() throws IOException {
        // Setup export
        Path exportDir = tempDir.getRoot()
                .toPath()
                .toRealPath();

        // Comment out CRL in etc/ws-security/server/encryption.properties
        Path serverEncptProps = ddfHome.resolve(Paths.get("etc", "ws-security", "server", "encryption.properties"));
        String tag = String.format(DDF_EXPORTED_TAG_TEMPLATE, DDF_EXPORTED_HOME);
        List<String> lines = new ArrayList<>(2);
        lines.add(String.format("#%s:%s", serverEncptProps.toRealPath().toString(), tag));
        lines.add(String.format("#%s=%s", CRL_PROP_KEY, CRL.toString()));
        FileUtils.writeLines(serverEncptProps.toFile(), StandardCharsets.UTF_8.toString(), lines, System.lineSeparator());

        SecurityMigratable eSecurityMigratable = new SecurityMigratable();
        List<ConfigurationMigratable> eConfigMigratables = Arrays.asList(eSecurityMigratable);
        List<DataMigratable> eDataMigratables = Collections.emptyList();
        ConfigurationMigrationManager eConfigurationMigrationManager =
                new ConfigurationMigrationManager(mBeanServer,
                        eConfigMigratables,
                        eDataMigratables,
                        systemService);

        // Perform export
        MigrationReport exportReport = eConfigurationMigrationManager.doExport(exportDir);

        // Verify export
        assertThat("The export report has errors.", exportReport.hasErrors(), is(false));
        assertThat("The export report has warnings.", exportReport.hasWarnings(), is(false));
        assertThat("Export was not successful.", exportReport.wasSuccessful(), is(true));
        String exportedZipBaseName = String.format("exported-%s.zip", SUPPORTED_VERSION);
        Path exportedZip = exportDir.resolve(exportedZipBaseName).toRealPath();
        assertThat("Export zip does not exist.", exportedZip.toFile().exists(), is(true));
        assertThat("Exported zip is empty.", exportedZip.toFile().length(), greaterThan(0L));

        // Setup import
        setup(DDF_IMPORTED_HOME, DDF_IMPORTED_TAG);

        SecurityMigratable iSecurityMigratable = new SecurityMigratable();
        List<ConfigurationMigratable> iConfigMigratables = Arrays.asList(iSecurityMigratable);
        List<DataMigratable> iDataMigratables = Collections.emptyList();
        ConfigurationMigrationManager iConfigurationMigrationManager =
                new ConfigurationMigrationManager(mBeanServer,
                        iConfigMigratables,
                        iDataMigratables,
                        systemService);

        MigrationReport importReport = iConfigurationMigrationManager.doImport(exportDir);

        // Verify import
        assertThat("The import report has errors.", importReport.hasErrors(), is(false));
        assertThat("The import report has warnings.", importReport.hasWarnings(), is(false));
        assertThat("Import was not successful.", importReport.wasSuccessful(), is(true));
        verifyPdpFilesImported();
    }

    /**
     * Verifies that when no PDP file exists, the export and import both succeed.
     */
    @Test
    public void testDoExportAndDoImportNoPdpFileCrlExists() throws IOException {
        // Setup export
        Path exportDir = tempDir.getRoot()
                .toPath()
                .toRealPath();

        // Remove PDP file
        Path xacmlPolicy = ddfHome.resolve(PDP_POLICIES_DIR).resolve(XACML_POLICY);
        Files.delete(xacmlPolicy);

        SecurityMigratable eSecurityMigratable = new SecurityMigratable();
        List<ConfigurationMigratable> eConfigMigratables = Arrays.asList(eSecurityMigratable);
        List<DataMigratable> eDataMigratables = Collections.emptyList();
        ConfigurationMigrationManager eConfigurationMigrationManager =
                new ConfigurationMigrationManager(mBeanServer,
                        eConfigMigratables,
                        eDataMigratables,
                        systemService);

        // Perform export
        MigrationReport exportReport = eConfigurationMigrationManager.doExport(exportDir);

        // Verify export
        assertThat("The export report has errors.", exportReport.hasErrors(), is(false));
        assertThat("The export report has warnings.", exportReport.hasWarnings(), is(false));
        assertThat("Export was not successful.", exportReport.wasSuccessful(), is(true));
        String exportedZipBaseName = String.format("exported-%s.zip", SUPPORTED_VERSION);
        Path exportedZip = exportDir.resolve(exportedZipBaseName).toRealPath();
        assertThat("Export zip does not exist.", exportedZip.toFile().exists(), is(true));
        assertThat("Exported zip is empty.", exportedZip.toFile().length(), greaterThan(0L));

        // Setup import
        setup(DDF_IMPORTED_HOME, DDF_IMPORTED_TAG);

        SecurityMigratable iSecurityMigratable = new SecurityMigratable();
        List<ConfigurationMigratable> iConfigMigratables = Arrays.asList(iSecurityMigratable);
        List<DataMigratable> iDataMigratables = Collections.emptyList();
        ConfigurationMigrationManager iConfigurationMigrationManager =
                new ConfigurationMigrationManager(mBeanServer,
                        iConfigMigratables,
                        iDataMigratables,
                        systemService);

        MigrationReport importReport = iConfigurationMigrationManager.doImport(exportDir);

        // Verify import
        assertThat("The import report has errors.", importReport.hasErrors(), is(false));
        assertThat("The import report has warnings.", importReport.hasWarnings(), is(false));
        assertThat("Import was not successful.", importReport.wasSuccessful(), is(true));
        verifyCrlImported();
    }

    /**
     * Verify that when both the PDP file and CRL do not exists, the export and import succeed.
     */
    @Test
    public void testDoExportAndDoImportNoPdpFileNoCrl() throws IOException {
        // Setup export
        Path exportDir = tempDir.getRoot()
                .toPath()
                .toRealPath();

        // Comment out CRL in etc/ws-security/server/encryption.properties
        Path serverEncptProps = ddfHome.resolve(Paths.get("etc", "ws-security", "server", "encryption.properties"));
        String tag = String.format(DDF_EXPORTED_TAG_TEMPLATE, DDF_EXPORTED_HOME);
        List<String> lines = new ArrayList<>(2);
        lines.add(String.format("#%s:%s", serverEncptProps.toRealPath().toString(), tag));
        lines.add(String.format("#%s=%s", CRL_PROP_KEY, CRL.toString()));
        FileUtils.writeLines(serverEncptProps.toFile(), StandardCharsets.UTF_8.toString(), lines, System.lineSeparator());

        // Remove PDP file
        Path xacmlPolicy = ddfHome.resolve(PDP_POLICIES_DIR).resolve(XACML_POLICY);
        Files.delete(xacmlPolicy);

        SecurityMigratable eSecurityMigratable = new SecurityMigratable();
        List<ConfigurationMigratable> eConfigMigratables = Arrays.asList(eSecurityMigratable);
        List<DataMigratable> eDataMigratables = Collections.emptyList();
        ConfigurationMigrationManager eConfigurationMigrationManager =
                new ConfigurationMigrationManager(mBeanServer,
                        eConfigMigratables,
                        eDataMigratables,
                        systemService);

        // Perform export
        MigrationReport exportReport = eConfigurationMigrationManager.doExport(exportDir);

        // Verify export
        assertThat("The export report has errors.", exportReport.hasErrors(), is(false));
        assertThat("The export report has warnings.", exportReport.hasWarnings(), is(false));
        assertThat("Export was not successful.", exportReport.wasSuccessful(), is(true));
        String exportedZipBaseName = String.format("exported-%s.zip", SUPPORTED_VERSION);
        Path exportedZip = exportDir.resolve(exportedZipBaseName).toRealPath();
        assertThat("Export zip does not exist.", exportedZip.toFile().exists(), is(true));
        assertThat("Exported zip is empty.", exportedZip.toFile().length(), greaterThan(0L));

        // Setup import
        setup(DDF_IMPORTED_HOME, DDF_IMPORTED_TAG);

        SecurityMigratable iSecurityMigratable = new SecurityMigratable();
        List<ConfigurationMigratable> iConfigMigratables = Arrays.asList(iSecurityMigratable);
        List<DataMigratable> iDataMigratables = Collections.emptyList();
        ConfigurationMigrationManager iConfigurationMigrationManager =
                new ConfigurationMigrationManager(mBeanServer,
                        iConfigMigratables,
                        iDataMigratables,
                        systemService);

        MigrationReport importReport = iConfigurationMigrationManager.doImport(exportDir);

        // Verify import
        assertThat("The import report has errors.", importReport.hasErrors(), is(false));
        assertThat("The import report has warnings.", importReport.hasWarnings(), is(false));
        assertThat("Import was not successful.", importReport.wasSuccessful(), is(true));
    }

    private void setup(String ddfHomeStr, String tag) throws IOException {
        ddfHome = tempDir.newFolder(ddfHomeStr).toPath().toRealPath();
        Path binDir = ddfHome.resolve("bin");
        Files.createDirectory(binDir);
        System.setProperty(DDF_HOME_SYSTEM_PROP_KEY, ddfHome.toRealPath().toString());
        setupVersionFile(SUPPORTED_VERSION);
        for (Path path : PROPERTIES_FILES) {
            Path p = ddfHome.resolve(path);
            Files.createDirectories(p.getParent());
            Files.createFile(p);
            if (p.endsWith(Paths.get("server", "encryption.properties"))) {
                List<String> lines = new ArrayList<>(2);
                lines.add(String.format("#%s:%s", p.toRealPath().toString(), tag));
                lines.add(String.format("%s=%s", CRL_PROP_KEY, CRL.toString()));
                FileUtils.writeLines(p.toFile(), StandardCharsets.UTF_8.toString(), lines, System.lineSeparator());
            } else {
                FileUtils.writeStringToFile(p.toFile(), String.format("#%s:%s", p.toRealPath().toString(), tag), StandardCharsets.UTF_8);
            }
        }
        Files.createDirectory(ddfHome.resolve(PDP_POLICIES_DIR));

        setupCrl(tag);
        setupPdpFiles(tag);
    }

    private void setupVersionFile(String version) throws IOException {
        Path versionFile = ddfHome.resolve("Version.txt");
        Files.createFile(versionFile);
        FileUtils.writeStringToFile(versionFile.toFile().getCanonicalFile(), version, StandardCharsets.UTF_8);
    }

    private void setupCrl(String tag) throws IOException {
        Files.createDirectories(ddfHome.resolve(CRL).getParent());
        Files.createFile(ddfHome.resolve(CRL));
        FileUtils.writeStringToFile(ddfHome.resolve(CRL).toRealPath().toFile(), String.format("#%s:%s", ddfHome.resolve(CRL).toRealPath().toString(), tag), StandardCharsets.UTF_8);
    }

    private void setupPdpFiles(String tag) throws IOException {
        Files.createDirectories(ddfHome.resolve(PDP_POLICIES_DIR).getParent());
        Path xacmlPolicy = ddfHome.resolve(PDP_POLICIES_DIR).resolve(XACML_POLICY);
        Files.createFile(xacmlPolicy);
        FileUtils.writeStringToFile(xacmlPolicy.toRealPath().toFile(), String.format("#%s:%s", xacmlPolicy.toRealPath().toString(), tag), StandardCharsets.UTF_8);
    }

    private void verifyPdpFilesImported() throws IOException {
        Path xacmlPolicy = ddfHome.resolve(PDP_POLICIES_DIR).resolve(XACML_POLICY).toRealPath();
        assertThat(String.format("%s does not exist.", xacmlPolicy), xacmlPolicy.toFile().exists(), is(true));
        assertThat(String.format("%s was not imported.", xacmlPolicy), verifiyImported(xacmlPolicy), is(true));
    }

    private void verifyCrlImported() throws IOException {
        Path crl = ddfHome.resolve(CRL).toRealPath();
        assertThat(String.format("%s does not exist.", crl), crl.toFile().exists(), is(true));
        assertThat(String.format("%s was not imported.", crl), verifiyImported(crl), is(true));
    }

    private boolean verifiyImported(Path p) throws IOException {
        List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
        String tag = lines.get(0).split(":")[1];
        return StringUtils.equals(tag, String.format(DDF_EXPORTED_TAG_TEMPLATE, DDF_EXPORTED_HOME));
    }
}
