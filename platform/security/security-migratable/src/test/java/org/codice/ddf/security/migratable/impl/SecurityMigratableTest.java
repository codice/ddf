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
package org.codice.ddf.security.migratable.impl;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.karaf.system.SystemService;
import org.codice.ddf.configuration.migration.ConfigurationMigrationManager;
import org.codice.ddf.migration.ImportMigrationContext;
import org.codice.ddf.migration.Migratable;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationMessage;
import org.codice.ddf.migration.MigrationReport;
import org.codice.ddf.migration.MigrationWarning;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * This test creates a "mock" ddf directory structure for the system exported from (ddfExport) and
 * "mock" directory structure for the system imported into (ddfImport). These directory structures
 * are located under the created TemporaryFolder. For example, if the TemporaryFolder is
 * /private/var/folders/2j/q2gjqn4s2mv53c2q53_m9d_w0000gn/T/junit7611336125982191987, one would see
 * a similar directory structure to the following after initial setup and an import:
 *
 * <p>// This is the system exported from: ./ddfExport ./ddfExport/bin ./ddfExport/etc
 * ./ddfExport/etc/certs ./ddfExport/etc/certs/demoCA/crl/crl.pem ./ddfExport/etc/pdp/xacml.xml
 * ./ddfExport/etc/ws-security/issuer/encryption.properties
 * ./ddfExport/etc/ws-security/issuer/signature.properties
 * ./ddfExport/etc/ws-security/server/encryption.properties
 * ./ddfExport/etc/ws-security/server/signature.properties ./ddfExport/Version.txt
 *
 * <p>// This is the system imported into: ./ddfImport ./ddfImport/bin ./ddfImport/etc
 * ./ddfExport/etc/certs/demoCA/crl/crl.pem ./ddfImport/etc/pdp/xacml.xml
 * ./ddfImport/etc/ws-security/issuer/encryption.properties
 * ./ddfImport/etc/ws-security/issuer/signature.properties
 * ./ddfImport/etc/ws-security/server/encryption.properties
 * ./ddfImport/etc/ws-security/server/signature.properties ./ddfImport/Version.txt
 *
 * <p>// The backup from the imported system will look similar to this:
 * ./exported-1.0-20170814T152846.zip
 *
 * <p>// The exported zip from ddfExport will look similar to this: ./exported-1.0.zip
 */
@RunWith(MockitoJUnitRunner.class)
public class SecurityMigratableTest {
  private static final String SUPPORTED_BRANDING = "test";

  private static final String SUPPORTED_VERSION = "1.0";

  private static final String UNSUPPORTED_VERSION = "666.0";

  private static final String DDF_EXPORTED_HOME = "ddfExport";

  private static final String DDF_IMPORTED_HOME = "ddfImport";

  private static final String DDF_EXPORTED_TAG_TEMPLATE = "exported_from_%s";

  private static final String DDF_IMPORTED_TAG = "original";

  private static final String DDF_HOME_SYSTEM_PROP_KEY = "ddf.home";

  private static final Path PDP_POLICIES_DIR = Paths.get("etc", "pdp");

  private static final Path SECURITY_POLICIES_DIR = Paths.get("security");

  private static final String[] POLICY_FILES =
      new String[] {"default.policy", "configurations.policy", "another.policy"};

  private static final List<Path> PROPERTIES_FILES =
      ImmutableList.of(
          Paths.get("etc", "ws-security", "server", "encryption.properties"),
          Paths.get("etc", "ws-security", "server", "signature.properties"),
          Paths.get("etc", "ws-security", "issuer", "encryption.properties"),
          Paths.get("etc", "ws-security", "issuer", "signature.properties"));

  private static final String CRL_PROP_KEY = "org.apache.ws.security.crypto.merlin.x509crl.file";

  private static final Path CRL = Paths.get("etc", "certs", "demoCA", "crl", "crl.pem");

  private static final String XACML_POLICY = "xacml.xml";

  private static final PrintStream OUT = System.out;

  @Rule public TemporaryFolder tempDir = new TemporaryFolder();

  private Path ddfHome;

  @Mock private ImportMigrationContext mockImportMigrationContext;

  @Mock private MigrationReport mockMigrationReport;

  @Mock private SystemService systemService;

  @Before
  public void setup() throws IOException {
    // The tag is written to all exported files so that we can verify the file actually
    // got copied on import (ie. read the tags in files in the imported system and
    // verify that they are the ones from the exported system).
    String tag = String.format(DDF_EXPORTED_TAG_TEMPLATE, DDF_EXPORTED_HOME);
    setup(DDF_EXPORTED_HOME, tag);
  }

  /**
   * Verify that when an unsupported exported version is imported, an exception is recorded in the
   * migration report.
   */
  @Test
  public void testDoImportUnsupportedMigratedVersion() {
    // Setup
    when(mockImportMigrationContext.getReport()).thenReturn(mockMigrationReport);
    SecurityMigratable securityMigratable = new SecurityMigratable();

    // Perform Test
    securityMigratable.doVersionUpgradeImport(mockImportMigrationContext, UNSUPPORTED_VERSION);

    // Verify
    verify(mockImportMigrationContext).getReport();
    verify(mockMigrationReport).record(any(MigrationException.class));
    verifyNoMoreInteractions(mockImportMigrationContext);
  }

  /**
   * Verify the when the system is in a default configuration, all of Security Migratable's files
   * are successfully exported from system ddfExport and successfully imported into system
   * ddfImport.
   */
  @Test
  public void testDoExportAndDoImport() throws IOException {
    // Setup export
    Path exportDir = tempDir.getRoot().toPath().toRealPath();

    SecurityMigratable eSecurityMigratable = new SecurityMigratable();
    List<Migratable> eMigratables = Arrays.asList(eSecurityMigratable);
    ConfigurationMigrationManager eConfigurationMigrationManager =
        new ConfigurationMigrationManager(eMigratables, systemService);

    // Perform export
    MigrationReport exportReport = eConfigurationMigrationManager.doExport(exportDir, this::print);

    // Verify export
    assertThat("The export report has errors.", exportReport.hasErrors(), is(false));
    assertThat("The export report has warnings.", exportReport.hasWarnings(), is(false));
    assertThat("Export was not successful.", exportReport.wasSuccessful(), is(true));
    String exportedZipBaseName = String.format("%s-%s.dar", SUPPORTED_BRANDING, SUPPORTED_VERSION);
    Path exportedZip = exportDir.resolve(exportedZipBaseName).toRealPath();
    assertThat("Export zip does not exist.", exportedZip.toFile().exists(), is(true));
    assertThat("Exported zip is empty.", exportedZip.toFile().length(), greaterThan(0L));

    // Setup import
    setup(DDF_IMPORTED_HOME, DDF_IMPORTED_TAG);

    SecurityMigratable iSecurityMigratable = new SecurityMigratable();
    List<Migratable> iMigratables = Arrays.asList(iSecurityMigratable);
    ConfigurationMigrationManager iConfigurationMigrationManager =
        new ConfigurationMigrationManager(iMigratables, systemService);

    MigrationReport importReport = iConfigurationMigrationManager.doImport(exportDir, this::print);

    // Verify import
    assertThat("The import report has errors.", importReport.hasErrors(), is(false));
    assertThat("The import report has warnings.", importReport.hasWarnings(), is(false));
    assertThat("Import was not successful.", importReport.wasSuccessful(), is(true));
    verifyPolicyFilesImported();
    verifyPdpFilesImported();
    verifyCrlImported();
  }

  /** Verifies that when no CRL (optional) is specified, the export and import both succeed. */
  @Test
  public void testDoExportAndDoImportWhenNoCrlExists() throws IOException {
    // Setup export
    Path exportDir = tempDir.getRoot().toPath().toRealPath();

    // Comment out CRL in etc/ws-security/server/encryption.properties
    Path serverEncptProps =
        ddfHome.resolve(Paths.get("etc", "ws-security", "server", "encryption.properties"));
    String tag = String.format(DDF_EXPORTED_TAG_TEMPLATE, DDF_EXPORTED_HOME);
    writeProperties(
        serverEncptProps,
        "_" + CRL_PROP_KEY,
        CRL.toString(),
        String.format("%s&%s", serverEncptProps.toRealPath().toString(), tag));

    SecurityMigratable eSecurityMigratable = new SecurityMigratable();
    List<Migratable> eMigratables = Arrays.asList(eSecurityMigratable);
    ConfigurationMigrationManager eConfigurationMigrationManager =
        new ConfigurationMigrationManager(eMigratables, systemService);

    // Perform export
    MigrationReport exportReport = eConfigurationMigrationManager.doExport(exportDir, this::print);

    // Verify export
    assertThat("The export report has errors.", exportReport.hasErrors(), is(false));
    assertThat("The export report has warnings.", exportReport.hasWarnings(), is(false));
    assertThat("Export was not successful.", exportReport.wasSuccessful(), is(true));
    String exportedZipBaseName = String.format("%s-%s.dar", SUPPORTED_BRANDING, SUPPORTED_VERSION);
    Path exportedZip = exportDir.resolve(exportedZipBaseName).toRealPath();
    assertThat("Export zip does not exist.", exportedZip.toFile().exists(), is(true));
    assertThat("Exported zip is empty.", exportedZip.toFile().length(), greaterThan(0L));

    // Setup import
    setup(DDF_IMPORTED_HOME, DDF_IMPORTED_TAG);

    SecurityMigratable iSecurityMigratable = new SecurityMigratable();
    List<Migratable> iMigratables = Arrays.asList(iSecurityMigratable);
    ConfigurationMigrationManager iConfigurationMigrationManager =
        new ConfigurationMigrationManager(iMigratables, systemService);

    MigrationReport importReport = iConfigurationMigrationManager.doImport(exportDir, this::print);

    // Verify import
    assertThat("The import report has errors.", importReport.hasErrors(), is(false));
    assertThat("The import report has warnings.", importReport.hasWarnings(), is(false));
    assertThat("Import was not successful.", importReport.wasSuccessful(), is(true));
    verifyPolicyFilesImported();
    verifyPdpFilesImported();
  }

  /** Verifies that when no PDP file exists, the export and import both succeed. */
  @Test
  public void testDoExportAndDoImportWhenNoPdpFileExists() throws IOException {
    // Setup export
    Path exportDir = tempDir.getRoot().toPath().toRealPath();

    // Remove PDP file
    Path xacmlPolicy = ddfHome.resolve(PDP_POLICIES_DIR).resolve(XACML_POLICY);
    Files.delete(xacmlPolicy);

    SecurityMigratable eSecurityMigratable = new SecurityMigratable();
    List<Migratable> eMigratables = Arrays.asList(eSecurityMigratable);
    ConfigurationMigrationManager eConfigurationMigrationManager =
        new ConfigurationMigrationManager(eMigratables, systemService);

    // Perform export
    MigrationReport exportReport = eConfigurationMigrationManager.doExport(exportDir, this::print);

    // Verify export
    assertThat("The export report has errors.", exportReport.hasErrors(), is(false));
    assertThat("The export report has warnings.", exportReport.hasWarnings(), is(false));
    assertThat("Export was not successful.", exportReport.wasSuccessful(), is(true));
    String exportedZipBaseName = String.format("%s-%s.dar", SUPPORTED_BRANDING, SUPPORTED_VERSION);
    Path exportedZip = exportDir.resolve(exportedZipBaseName).toRealPath();
    assertThat("Export zip does not exist.", exportedZip.toFile().exists(), is(true));
    assertThat("Exported zip is empty.", exportedZip.toFile().length(), greaterThan(0L));

    // Setup import
    setup(DDF_IMPORTED_HOME, DDF_IMPORTED_TAG);

    SecurityMigratable iSecurityMigratable = new SecurityMigratable();
    List<Migratable> iMigratables = Arrays.asList(iSecurityMigratable);
    ConfigurationMigrationManager iConfigurationMigrationManager =
        new ConfigurationMigrationManager(iMigratables, systemService);

    MigrationReport importReport = iConfigurationMigrationManager.doImport(exportDir, this::print);

    // Verify import
    assertThat("The import report has errors.", importReport.hasErrors(), is(false));
    assertThat("The import report has warnings.", importReport.hasWarnings(), is(false));
    assertThat("Import was not successful.", importReport.wasSuccessful(), is(true));
    verifyPolicyFilesImported();
    verifyCrlImported();
  }

  /** Verifies that when no policy file exists, the export and import both succeed. */
  @Test
  public void testDoExportAndDoImportWhenNoPolicyFileExists() throws IOException {
    // Setup export
    Path exportDir = tempDir.getRoot().toPath().toRealPath();

    // Remove Policy file
    for (final String file : POLICY_FILES) {
      Path policy = ddfHome.resolve(SECURITY_POLICIES_DIR).resolve(file);
      Files.delete(policy);
    }

    SecurityMigratable eSecurityMigratable = new SecurityMigratable();
    List<Migratable> eMigratables = Arrays.asList(eSecurityMigratable);
    ConfigurationMigrationManager eConfigurationMigrationManager =
        new ConfigurationMigrationManager(eMigratables, systemService);

    // Perform export
    MigrationReport exportReport = eConfigurationMigrationManager.doExport(exportDir, this::print);

    // Verify export
    assertThat("The export report has errors.", exportReport.hasErrors(), is(false));
    assertThat("The export report has warnings.", exportReport.hasWarnings(), is(false));
    assertThat("Export was not successful.", exportReport.wasSuccessful(), is(true));
    String exportedZipBaseName = String.format("%s-%s.dar", SUPPORTED_BRANDING, SUPPORTED_VERSION);
    Path exportedZip = exportDir.resolve(exportedZipBaseName).toRealPath();
    assertThat("Export zip does not exist.", exportedZip.toFile().exists(), is(true));
    assertThat("Exported zip is empty.", exportedZip.toFile().length(), greaterThan(0L));

    // Setup import
    setup(DDF_IMPORTED_HOME, DDF_IMPORTED_TAG);

    SecurityMigratable iSecurityMigratable = new SecurityMigratable();
    List<Migratable> iMigratables = Arrays.asList(iSecurityMigratable);
    ConfigurationMigrationManager iConfigurationMigrationManager =
        new ConfigurationMigrationManager(iMigratables, systemService);

    MigrationReport importReport = iConfigurationMigrationManager.doImport(exportDir, this::print);

    // Verify import
    assertThat("The import report has errors.", importReport.hasErrors(), is(false));
    assertThat("The import report has warnings.", importReport.hasWarnings(), is(false));
    assertThat("Import was not successful.", importReport.wasSuccessful(), is(true));
    verifyPdpFilesImported();
    verifyCrlImported();
  }

  /**
   * Verify that when the PDP file, polciy files, and CRL do not exists, the export and import
   * succeed.
   */
  @Test
  public void testDoExportAndDoImportWhenNoFiles() throws IOException {
    // Setup export
    Path exportDir = tempDir.getRoot().toPath().toRealPath();

    // Comment out CRL in etc/ws-security/server/encryption.properties
    Path serverEncptProps =
        ddfHome.resolve(Paths.get("etc", "ws-security", "server", "encryption.properties"));
    String tag = String.format(DDF_EXPORTED_TAG_TEMPLATE, DDF_EXPORTED_HOME);

    writeProperties(
        serverEncptProps,
        "_" + CRL_PROP_KEY,
        CRL.toString(),
        String.format("%s&%s", serverEncptProps.toRealPath().toString(), tag));

    // Remove PDP file
    Path xacmlPolicy = ddfHome.resolve(PDP_POLICIES_DIR).resolve(XACML_POLICY);
    Files.delete(xacmlPolicy);

    // Remove policy files
    for (final String file : POLICY_FILES) {
      Path policy = ddfHome.resolve(SECURITY_POLICIES_DIR).resolve(file);
      Files.delete(policy);
    }

    SecurityMigratable eSecurityMigratable = new SecurityMigratable();
    List<Migratable> eMigratables = Arrays.asList(eSecurityMigratable);
    ConfigurationMigrationManager eConfigurationMigrationManager =
        new ConfigurationMigrationManager(eMigratables, systemService);

    // Perform export
    MigrationReport exportReport = eConfigurationMigrationManager.doExport(exportDir, this::print);

    // Verify export
    assertThat("The export report has errors.", exportReport.hasErrors(), is(false));
    assertThat("The export report has warnings.", exportReport.hasWarnings(), is(false));
    assertThat("Export was not successful.", exportReport.wasSuccessful(), is(true));
    String exportedZipBaseName = String.format("%s-%s.dar", SUPPORTED_BRANDING, SUPPORTED_VERSION);
    Path exportedZip = exportDir.resolve(exportedZipBaseName).toRealPath();
    assertThat("Export zip does not exist.", exportedZip.toFile().exists(), is(true));
    assertThat("Exported zip is empty.", exportedZip.toFile().length(), greaterThan(0L));

    // Setup import
    setup(DDF_IMPORTED_HOME, DDF_IMPORTED_TAG);

    SecurityMigratable iSecurityMigratable = new SecurityMigratable();
    List<Migratable> iMigratables = Arrays.asList(iSecurityMigratable);
    ConfigurationMigrationManager iConfigurationMigrationManager =
        new ConfigurationMigrationManager(iMigratables, systemService);

    MigrationReport importReport = iConfigurationMigrationManager.doImport(exportDir, this::print);

    // Verify import
    assertThat("The import report has errors.", importReport.hasErrors(), is(false));
    assertThat("The import report has warnings.", importReport.hasWarnings(), is(false));
    assertThat("Import was not successful.", importReport.wasSuccessful(), is(true));
  }

  private void print(MigrationMessage msg) {
    if (msg instanceof MigrationException) {
      ((MigrationException) msg).printStackTrace(System.out);
    } else if (msg instanceof MigrationWarning) {
      OUT.println("Warning: " + msg);
    } else {
      OUT.println("Info: " + msg);
    }
  }

  private void setup(String ddfHomeStr, String tag) throws IOException {
    ddfHome = tempDir.newFolder(ddfHomeStr).toPath().toRealPath();
    Path binDir = ddfHome.resolve("bin");
    Files.createDirectory(binDir);
    System.setProperty(DDF_HOME_SYSTEM_PROP_KEY, ddfHome.toRealPath().toString());
    setupBrandingFile(SUPPORTED_BRANDING);
    setupVersionFile(SUPPORTED_VERSION);
    for (Path path : PROPERTIES_FILES) {
      Path p = ddfHome.resolve(path);
      Files.createDirectories(p.getParent());
      Files.createFile(p);
      if (p.endsWith(Paths.get("server", "encryption.properties"))) {
        writeProperties(
            p,
            CRL_PROP_KEY,
            CRL.toString(),
            String.format("%s&%s", p.toRealPath().toString(), tag));
      } else {
        writeProperties(
            p, "something", "else", String.format("%s&%s", p.toRealPath().toString(), tag));
      }
    }
    Files.createDirectory(ddfHome.resolve(PDP_POLICIES_DIR));

    setupCrl(tag);
    setupPdpFiles(tag);
    Files.createDirectory(ddfHome.resolve(SECURITY_POLICIES_DIR));
    setupPolicyFiles(tag);
  }

  private void setupBrandingFile(String branding) throws IOException {
    final Path brandingFile = ddfHome.resolve("Branding.txt");

    Files.createFile(brandingFile);
    FileUtils.writeStringToFile(
        brandingFile.toFile().getCanonicalFile(), branding, StandardCharsets.UTF_8);
  }

  private void setupVersionFile(String version) throws IOException {
    Path versionFile = ddfHome.resolve("Version.txt");
    Files.createFile(versionFile);
    FileUtils.writeStringToFile(
        versionFile.toFile().getCanonicalFile(), version, StandardCharsets.UTF_8);
  }

  private void setupCrl(String tag) throws IOException {
    Files.createDirectories(ddfHome.resolve(CRL).getParent());
    Files.createFile(ddfHome.resolve(CRL));
    FileUtils.writeStringToFile(
        ddfHome.resolve(CRL).toRealPath().toFile(),
        String.format("#%s&%s", ddfHome.resolve(CRL).toRealPath().toString(), tag),
        StandardCharsets.UTF_8);
  }

  private void setupPdpFiles(String tag) throws IOException {
    Files.createDirectories(ddfHome.resolve(PDP_POLICIES_DIR).getParent());
    Path xacmlPolicy = ddfHome.resolve(PDP_POLICIES_DIR).resolve(XACML_POLICY);
    Files.createFile(xacmlPolicy);
    FileUtils.writeStringToFile(
        xacmlPolicy.toRealPath().toFile(),
        String.format("#%s&%s", xacmlPolicy.toRealPath().toString(), tag),
        StandardCharsets.UTF_8);
  }

  private void setupPolicyFiles(String tag) throws IOException {
    Files.createDirectories(ddfHome.resolve(SECURITY_POLICIES_DIR).getParent());
    for (final String file : POLICY_FILES) {
      Path policy = ddfHome.resolve(SECURITY_POLICIES_DIR).resolve(file);
      Files.createFile(policy);
      FileUtils.writeStringToFile(
          policy.toRealPath().toFile(),
          String.format("#%s&%s", policy.toRealPath().toString(), tag),
          StandardCharsets.UTF_8);
    }
  }

  private void verifyPolicyFilesImported() throws IOException {
    for (final String file : POLICY_FILES) {
      Path policy = ddfHome.resolve(SECURITY_POLICIES_DIR).resolve(file).toRealPath();
      assertThat(String.format("%s does not exist.", policy), policy.toFile().exists(), is(true));
      assertThat(String.format("%s was not imported.", policy), verifiyImported(policy), is(true));
    }
  }

  private void verifyPdpFilesImported() throws IOException {
    Path xacmlPolicy = ddfHome.resolve(PDP_POLICIES_DIR).resolve(XACML_POLICY).toRealPath();
    assertThat(
        String.format("%s does not exist.", xacmlPolicy), xacmlPolicy.toFile().exists(), is(true));
    assertThat(
        String.format("%s was not imported.", xacmlPolicy), verifiyImported(xacmlPolicy), is(true));
  }

  private void verifyCrlImported() throws IOException {
    Path crl = ddfHome.resolve(CRL).toRealPath();
    assertThat(String.format("%s does not exist.", crl), crl.toFile().exists(), is(true));
    assertThat(String.format("%s was not imported.", crl), verifiyImported(crl), is(true));
  }

  private boolean verifiyImported(Path p) throws IOException {
    List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
    String tag = lines.get(0).split("&")[1];
    return StringUtils.equals(tag, String.format(DDF_EXPORTED_TAG_TEMPLATE, DDF_EXPORTED_HOME));
  }

  private void writeProperties(Path path, String key, String val, String comment)
      throws IOException {
    final Properties props = new Properties();

    props.put(key, val);
    try (final Writer writer = new BufferedWriter(new FileWriter(path.toFile()))) {
      props.store(writer, comment);
    }
  }
}
