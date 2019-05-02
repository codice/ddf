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
package org.codice.ddf.platform.migratable.impl;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
 * /private/var/folders/2j/q2gjqn4s2mv53c2q53_m9d_w0000gn/T/junit314771109111926703, one would see a
 * similar directory structure to the following after initial setup and an import:
 *
 * <p>// This is the system exported from: ./ddfExport ./ddfExport/bin ./ddfExport/bin/karaf
 * ./ddfExport/bin/karaf.bat ./ddfExport/etc ./ddfExport/etc/certs ./ddfExport/etc/certs/1
 * ./ddfExport/etc/certs/meta ./ddfExport/etc/config.properties ./ddfExport/etc/custom.properties
 * ./ddfExport/etc/ddf-wrapper.conf ./ddfExport/etc/fipsToIso.properties ./ddfExport/etc/keystores
 * ./ddfExport/etc/keystores/serverKeystore.jks ./ddfExport/etc/keystores/serverTruststore.jks
 * ./ddfExport/etc/log4j2.xml ./ddfExport/etc/org.codice.ddf.admin.applicationlist.properties
 * ./ddfExport/etc/pdp ./ddfExport/etc/pdp/ddf-metacard-attribute-ruleset.cfg
 * ./ddfExport/etc/pdp/ddf-user-attribute-ruleset.cfg ./ddfExport/etc/startup.properties
 * ./ddfExport/etc/custom.system.properties ./ddfExport/etc/users.attributes
 * ./ddfExport/etc/users.properties ./ddfExport/etc/ws-security ./ddfExport/etc/ws-security/issuer
 * ./ddfExport/etc/ws-security/issuer/encryption.properties
 * ./ddfExport/etc/ws-security/issuer/signature.properties ./ddfExport/etc/ws-security/server
 * ./ddfExport/etc/ws-security/server/encryption.properties
 * ./ddfExport/etc/ws-security/server/signature.properties ./ddfExport/Version.txt
 *
 * <p>// This is the system imported into: ./ddfImport ./ddfImport/bin ./ddfImport/bin/karaf
 * ./ddfImport/bin/karaf.bat ./ddfImport/etc ./ddfImport/etc/certs ./ddfImport/etc/certs/1
 * ./ddfImport/etc/certs/meta ./ddfImport/etc/config.properties ./ddfImport/etc/custom.properties
 * ./ddfImport/etc/ddf-wrapper.conf ./ddfImport/etc/fipsToIso.properties ./ddfImport/etc/keystores
 * ./ddfImport/etc/keystores/serverKeystore.jks ./ddfImport/etc/keystores/serverTruststore.jks
 * ./ddfImport/etc/log4j2.xml ./ddfImport/etc/org.codice.ddf.admin.applicationlist.properties
 * ./ddfImport/etc/pdp ./ddfImport/etc/pdp/ddf-metacard-attribute-ruleset.cfg
 * ./ddfImport/etc/pdp/ddf-user-attribute-ruleset.cfg ./ddfImport/etc/startup.properties
 * ./ddfImport/etc/custom.system.properties ./ddfImport/etc/users.attributes
 * ./ddfImport/etc/users.properties ./ddfImport/etc/ws-security ./ddfImport/etc/ws-security/issuer
 * ./ddfImport/etc/ws-security/issuer/encryption.properties
 * ./ddfImport/etc/ws-security/issuer/signature.properties ./ddfImport/etc/ws-security/server
 * ./ddfImport/etc/ws-security/server/encryption.properties
 * ./ddfImport/etc/ws-security/server/signature.properties ./ddfImport/Version.txt
 *
 * <p>// The backup from the imported system will look similar to this:
 * ./exported-1.0-20170810T110012.zip
 *
 * <p>// Thw exported zip from ddfExport will look similar to this: ./exported-1.0.zip
 */
@RunWith(MockitoJUnitRunner.class)
public class PlatformMigratableTest {

  private static final String KEYSTORE_SYSTEM_PROP_KEY = "javax.net.ssl.keyStore";

  private static final String TRUSTSTORE_SYSTEM_PROP_KEY = "javax.net.ssl.trustStore";

  private static final Path KEYSTORE_PATH_SYSTEM_PROP_VALUE =
      Paths.get("etc").resolve("keystores").resolve("serverKeystore.jks");

  private static final Path TRUSTSTORE_PATH_SYSTEM_PROP_VALUE =
      Paths.get("etc").resolve("keystores").resolve("serverTruststore.jks");

  private static final ImmutableMap<String, Path> KEYSTORES_MAP =
      ImmutableMap.<String, Path>builder()
          .put("keystore", Paths.get("etc", "keystores", "serverKeystore.jks"))
          .put("truststore", Paths.get("etc", "keystores", "serverTruststore.jks"))
          .build();

  private static final Path WS_SECURITY_DIR_REL_PATH = Paths.get("etc", "ws-security");

  private static final List<Path> WS_SECURITY_FILES =
      ImmutableList.of(
          Paths.get("etc", "ws-security", "issuer", "encryption.properties"),
          Paths.get("etc", "ws-security", "issuer", "signature.properties"),
          Paths.get("etc", "ws-security", "server", "encryption.properties"),
          Paths.get("etc", "ws-security", "server", "signature.properties"));

  private static final List<Path> REQUIRED_SYSTEM_FILES =
      ImmutableList.of(
          Paths.get("etc", "custom.system.properties"),
          Paths.get("etc", "system.properties"),
          Paths.get("etc", "startup.properties"),
          Paths.get("etc", "custom.properties"),
          Paths.get("etc", "config.properties"));

  private static final List<Path> OPTIONAL_SYSTEM_FILES =
      ImmutableList.of(
          Paths.get("etc", "users.properties"),
          Paths.get("etc", "users.attributes"),
          Paths.get("etc", "pdp", "ddf-metacard-attribute-ruleset.cfg"),
          Paths.get("etc", "pdp", "ddf-user-attribute-ruleset.cfg"),
          Paths.get("etc", "org.codice.ddf.admin.applicationlist.properties"),
          Paths.get("etc", "fipsToIso.properties"),
          Paths.get("etc", "log4j2.xml"),
          Paths.get("etc", "certs", "meta"),
          Paths.get("etc", "certs", "1"),
          Paths.get("bin", "karaf"),
          Paths.get("bin", "karaf.bat"));

  private static final Path SERVICE_WRAPPER = Paths.get("etc", "ddf-wrapper.conf");

  private static final Path SERVICE_WRAPPER_2 = Paths.get("bin", "setenv-wrapper.conf");

  private static final String SUPPORTED_BRANDING = "test";

  private static final String SUPPORTED_VERSION = "1.0";

  private static final String UNSUPPORTED_VERSION = "666.0";

  private static final String DDF_EXPORTED_HOME = "ddfExport";

  private static final String DDF_IMPORTED_HOME = "ddfImport";

  private static final String DDF_EXPORTED_TAG_TEMPLATE = "exported_from_%s";

  private static final String DDF_IMPORTED_TAG = "original";

  private static final String DDF_HOME_SYSTEM_PROP_KEY = "ddf.home";

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
    PlatformMigratable platformMigratable = new PlatformMigratable();

    // Perform Test
    platformMigratable.doVersionUpgradeImport(mockImportMigrationContext, UNSUPPORTED_VERSION);

    // Verify
    verify(mockImportMigrationContext).getReport();
    verify(mockMigrationReport).record(any(MigrationException.class));
    verifyNoMoreInteractions(mockImportMigrationContext);
  }

  /**
   * Verify the when the system is in a default configuration, all of Platform Migratable's files
   * are successfully exported from system ddfExport and successfully imported into system
   * ddfImport.
   */
  @Test
  public void testDoExportAndDoImport() throws IOException {
    // Setup export
    Path exportDir = tempDir.getRoot().toPath().toRealPath();

    PlatformMigratable ePlatformMigratable = new PlatformMigratable();
    List<Migratable> eMigratables = Arrays.asList(ePlatformMigratable);
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

    PlatformMigratable iPlatformMigratable = new PlatformMigratable();
    List<Migratable> iMigratables = Arrays.asList(iPlatformMigratable);
    ConfigurationMigrationManager iConfigurationMigrationManager =
        new ConfigurationMigrationManager(iMigratables, systemService);

    MigrationReport importReport = iConfigurationMigrationManager.doImport(exportDir, this::print);

    // Verify import
    assertThat("The import report has errors.", importReport.hasErrors(), is(false));
    assertThat("The import report has warnings.", importReport.hasWarnings(), is(false));
    assertThat("Import was not successful.", importReport.wasSuccessful(), is(true));
    verifyRequiredSystemFilesImported();
    verifyOptionalSystemFilesImported();
    verifyWsSecurityFilesImported();
    verifyKeystoresImported();
    verifyServiceWrapperImported();
  }

  /** Verifies that when an optional file is missing the export succeeds and so does the import. */
  @Test
  public void testDoExportAndDoImportOptionalFileNotExported() throws IOException {
    // Setup export
    Path exportDir = tempDir.getRoot().toPath().toRealPath();

    // Delete etc/users.properties. This file is optional, so it will not generate
    // an error during export.
    Files.delete(ddfHome.resolve("etc").resolve("users.properties").toRealPath());
    List<Path> optionalFilesExported =
        OPTIONAL_SYSTEM_FILES
            .stream()
            .filter(p -> !p.equals(Paths.get("etc", "users.properties")))
            .collect(Collectors.toList());

    PlatformMigratable ePlatformMigratable = new PlatformMigratable();
    List<Migratable> eMigratables = Arrays.asList(ePlatformMigratable);
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

    PlatformMigratable iPlatformMigratable = new PlatformMigratable();
    List<Migratable> iMigratables = Arrays.asList(iPlatformMigratable);
    ConfigurationMigrationManager iConfigurationMigrationManager =
        new ConfigurationMigrationManager(iMigratables, systemService);

    MigrationReport importReport = iConfigurationMigrationManager.doImport(exportDir, this::print);

    // Verify import
    assertThat("The import report has errors.", importReport.hasErrors(), is(false));
    assertThat("The import report has warnings.", importReport.hasWarnings(), is(false));
    assertThat("Import was not successful.", importReport.wasSuccessful(), is(true));
    verifyRequiredSystemFilesImported();
    verifyImported(optionalFilesExported);
    verifyWsSecurityFilesImported();
    verifyKeystoresImported();
    verifyServiceWrapperImported();
  }

  /**
   * Verifies that when a required files is missing, the export fails and no zip file is created.
   */
  @Test
  public void testDoExportRequiredFileNotExported() throws IOException {
    // Setup export
    Path exportDir = tempDir.getRoot().toPath().toRealPath();

    // Delete etc/system.properties. This file is required, so we should
    // should get an export error.
    Files.delete(ddfHome.resolve("etc").resolve("custom.system.properties").toRealPath());

    PlatformMigratable platformMigratable = new PlatformMigratable();
    List<Migratable> configMigratables = Arrays.asList(platformMigratable);
    ConfigurationMigrationManager configurationMigrationManager =
        new ConfigurationMigrationManager(configMigratables, systemService);

    // Perform export
    MigrationReport exportReport = configurationMigrationManager.doExport(exportDir, this::print);

    // Verify export
    assertThat("The export report doesn't not have errors.", exportReport.hasErrors(), is(true));
    assertThat("The export report has warnings.", exportReport.hasWarnings(), is(false));
    assertThat("Export was successful.", exportReport.wasSuccessful(), is(false));
    String exportedZipBaseName = String.format("%s-%s.dar", SUPPORTED_BRANDING, SUPPORTED_VERSION);
    Path exportedZip = exportDir.resolve(exportedZipBaseName);
    assertThat(
        String.format("Export zip [%s] exists.", exportedZip),
        exportedZip.toFile().exists(),
        is(false));
  }

  /**
   * Verify that when the keystore and truststore are located outside of they system home directory,
   * warnings are recorded on export but not on import. Both the export and import will still be
   * successful.
   */
  @Test
  public void testDoExportAndDoImportKeystoresOutsideOfDdfHome() throws IOException {
    // Setup export
    Path exportDir = tempDir.getRoot().toPath().toRealPath();

    // For export, move keystore and truststore into tempDir and reset system properties
    for (Map.Entry<String, Path> entry : KEYSTORES_MAP.entrySet()) {
      Path source = ddfHome.resolve(entry.getValue()).toRealPath();
      Files.move(
          source, tempDir.getRoot().toPath().toRealPath().resolve(entry.getValue().getFileName()));

      if ("keystore".equals(entry.getKey())) {
        System.setProperty(
            KEYSTORE_SYSTEM_PROP_KEY,
            tempDir
                .getRoot()
                .toPath()
                .resolve(entry.getValue().getFileName())
                .toRealPath()
                .toString());
      } else if ("truststore".equals(entry.getKey())) {
        System.setProperty(
            TRUSTSTORE_SYSTEM_PROP_KEY,
            tempDir
                .getRoot()
                .toPath()
                .resolve(entry.getValue().getFileName())
                .toRealPath()
                .toString());
      }
    }

    PlatformMigratable ePlatformMigratable = new PlatformMigratable();
    List<Migratable> eMigratables = Arrays.asList(ePlatformMigratable);
    ConfigurationMigrationManager eConfigurationMigrationManager =
        new ConfigurationMigrationManager(eMigratables, systemService);

    // Perform export
    MigrationReport exportReport = eConfigurationMigrationManager.doExport(exportDir, this::print);

    // Verify export
    assertThat("The export report has errors.", exportReport.hasErrors(), is(false));
    assertThat("The export report does not have warnings.", exportReport.hasWarnings(), is(true));
    assertThat("Export was not successful.", exportReport.wasSuccessful(), is(true));
    String exportedZipBaseName = String.format("%s-%s.dar", SUPPORTED_BRANDING, SUPPORTED_VERSION);
    Path exportedZip = exportDir.resolve(exportedZipBaseName).toRealPath();
    assertThat(
        String.format("Export zip [%s] does not exist.", exportedZip),
        exportedZip.toFile().exists(),
        is(true));
    assertThat(
        String.format("Exported zip [%s] is empty.", exportedZip),
        exportedZip.toFile().length(),
        greaterThan(0L));

    // Setup import
    setup(DDF_IMPORTED_HOME, DDF_IMPORTED_TAG);
    // For import, delete keystore and truststore since they are already in tempDir and reset system
    // properties.
    // Since these are outside of ddf.home, they should not be imported. A checksum should be
    // computed
    // to verify that they are the same as the exported files.
    for (Map.Entry<String, Path> entry : KEYSTORES_MAP.entrySet()) {
      Path keystore = ddfHome.resolve(entry.getValue()).toRealPath();
      Files.delete(ddfHome.resolve(keystore));

      if ("keystore".equals(entry.getKey())) {
        System.setProperty(
            KEYSTORE_SYSTEM_PROP_KEY,
            tempDir
                .getRoot()
                .toPath()
                .resolve(entry.getValue().getFileName())
                .toRealPath()
                .toString());
      } else if ("truststore".equals(entry.getKey())) {
        System.setProperty(
            TRUSTSTORE_SYSTEM_PROP_KEY,
            tempDir
                .getRoot()
                .toPath()
                .resolve(entry.getValue().getFileName())
                .toRealPath()
                .toString());
      }
    }

    PlatformMigratable iPlatformMigratable = new PlatformMigratable();
    List<Migratable> iMigratables = Arrays.asList(iPlatformMigratable);
    ConfigurationMigrationManager iConfigurationMigrationManager =
        new ConfigurationMigrationManager(iMigratables, systemService);

    MigrationReport importReport = iConfigurationMigrationManager.doImport(exportDir, this::print);

    // Verify import
    assertThat("The import report has errors.", importReport.hasErrors(), is(false));
    assertThat("The import report does have warnings.", importReport.hasWarnings(), is(false));
    assertThat("Import was not successful.", importReport.wasSuccessful(), is(true));
    verifyRequiredSystemFilesImported();
    verifyOptionalSystemFilesImported();
    verifyWsSecurityFilesImported();
    verifyServiceWrapperImported();
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
    setupKeystores(tag);
    for (Path path : REQUIRED_SYSTEM_FILES) {
      Path p = ddfHome.resolve(path);
      Files.createDirectories(p.getParent());
      Files.createFile(p);
      FileUtils.writeStringToFile(
          p.toFile(),
          String.format("#%s&%s", p.toRealPath().toString(), tag),
          StandardCharsets.UTF_8);
    }
    for (Path path : OPTIONAL_SYSTEM_FILES) {
      Path p = ddfHome.resolve(path);
      Files.createDirectories(p.getParent());
      Files.createFile(p);
      FileUtils.writeStringToFile(
          p.toFile(),
          String.format("#%s&%s", p.toRealPath().toString(), tag),
          StandardCharsets.UTF_8);
    }
    setupWsSecurity(tag);
    setupServiceWrapper(tag);
  }

  private void setupWsSecurity(String tag) throws IOException {
    Path wsSecurity = Files.createDirectories(ddfHome.resolve(WS_SECURITY_DIR_REL_PATH));
    Files.createDirectories(wsSecurity.resolve("issuer"));
    Files.createDirectories(wsSecurity.resolve("server"));

    for (Path securityFile : WS_SECURITY_FILES) {
      Path p = ddfHome.resolve(securityFile);
      Files.createFile(p);
      FileUtils.writeStringToFile(
          p.toFile(),
          String.format("#%s&%s", p.toRealPath().toString(), tag),
          StandardCharsets.UTF_8);
    }
  }

  private void setupServiceWrapper(String tag) throws IOException {
    Path serviceWrapperConfig = ddfHome.resolve(SERVICE_WRAPPER);
    Files.createFile(serviceWrapperConfig);
    FileUtils.writeStringToFile(
        serviceWrapperConfig.toFile(),
        String.format("#%s&%s", serviceWrapperConfig.toRealPath().toString(), tag),
        StandardCharsets.UTF_8);
    serviceWrapperConfig = ddfHome.resolve(SERVICE_WRAPPER_2);
    Files.createFile(serviceWrapperConfig);
    FileUtils.writeStringToFile(
        serviceWrapperConfig.toFile(),
        String.format("#%s&%s", serviceWrapperConfig.toRealPath().toString(), tag),
        StandardCharsets.UTF_8);
  }

  private void setupKeystores(String tag) throws IOException {
    Path keystores = Files.createDirectories(ddfHome.resolve("etc").resolve("keystores"));
    Files.createDirectories(keystores);

    for (Map.Entry<String, Path> entry : KEYSTORES_MAP.entrySet()) {
      Path keystore = ddfHome.resolve(entry.getValue());
      Files.createFile(keystore);
      FileUtils.writeStringToFile(
          keystore.toFile(),
          String.format("#%s&%s", keystore.toRealPath().toString(), tag),
          StandardCharsets.UTF_8);

      if ("keystore".equals(entry.getKey())) {
        System.setProperty(KEYSTORE_SYSTEM_PROP_KEY, KEYSTORE_PATH_SYSTEM_PROP_VALUE.toString());
      } else if ("truststore".equals(entry.getKey())) {
        System.setProperty(
            TRUSTSTORE_SYSTEM_PROP_KEY, TRUSTSTORE_PATH_SYSTEM_PROP_VALUE.toString());
      }
    }
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

  private void verifyRequiredSystemFilesImported() throws IOException {
    for (Path sysFile : REQUIRED_SYSTEM_FILES) {
      Path p = ddfHome.resolve(sysFile).toRealPath();
      assertThat(String.format("%s does not exist.", p), p.toFile().exists(), is(true));
      assertThat(String.format("%s was not imported.", p), verifiyImported(p), is(true));
    }
  }

  private void verifyOptionalSystemFilesImported() throws IOException {
    for (Path sysFile : OPTIONAL_SYSTEM_FILES) {
      Path p = ddfHome.resolve(sysFile).toRealPath();
      assertThat(String.format("%s does not exist.", p), p.toFile().exists(), is(true));
      assertThat(String.format("%s was not imported.", p), verifiyImported(p), is(true));
    }
  }

  private void verifyImported(List<Path> files) throws IOException {
    for (Path sysFile : files) {
      Path p = ddfHome.resolve(sysFile).toRealPath();
      assertThat(String.format("%s does not exist.", p), p.toFile().exists(), is(true));
      assertThat(String.format("%s was not imported.", p), verifiyImported(p), is(true));
    }
  }

  private void verifyWsSecurityFilesImported() throws IOException {
    for (Path securityFile : WS_SECURITY_FILES) {
      Path p = ddfHome.resolve(securityFile).toRealPath();
      assertThat(String.format("%s does not exist.", p), p.toFile().exists(), is(true));
      assertThat(String.format("%s was not imported.", p), verifiyImported(p), is(true));
    }
  }

  private void verifyKeystoresImported() throws IOException {
    for (Map.Entry<String, Path> entry : KEYSTORES_MAP.entrySet()) {
      Path keystore = ddfHome.resolve(entry.getValue()).toRealPath();
      assertThat(
          String.format("%s does not exist.", keystore), keystore.toFile().exists(), is(true));
      assertThat(
          String.format("%s was not imported.", keystore), verifiyImported(keystore), is(true));
    }
  }

  private void verifyServiceWrapperImported() throws IOException {
    Path p = ddfHome.resolve(SERVICE_WRAPPER).toRealPath();
    assertThat(String.format("%s does not exist.", p), p.toFile().exists(), is(true));
    assertThat(String.format("%s was not imported.", p), verifiyImported(p), is(true));
    p = ddfHome.resolve(SERVICE_WRAPPER_2).toRealPath();
    assertThat(String.format("%s does not exist.", p), p.toFile().exists(), is(true));
    assertThat(String.format("%s was not imported.", p), verifiyImported(p), is(true));
  }

  private boolean verifiyImported(Path p) throws IOException {
    List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
    String tag = lines.get(0).split("&")[1];
    return StringUtils.equals(tag, String.format(DDF_EXPORTED_TAG_TEMPLATE, DDF_EXPORTED_HOME));
  }
}
