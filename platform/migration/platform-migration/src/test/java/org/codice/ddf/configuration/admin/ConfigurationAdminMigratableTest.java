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
package org.codice.ddf.configuration.admin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import javax.management.MBeanServer;
import org.apache.commons.io.FileUtils;
import org.apache.felix.fileinstall.internal.DirectoryWatcher;
import org.apache.karaf.system.SystemService;
import org.codice.ddf.configuration.migration.ConfigurationMigrationManager;
import org.codice.ddf.configuration.persistence.PersistenceStrategy;
import org.codice.ddf.configuration.persistence.felix.FelixCfgPersistenceStrategy;
import org.codice.ddf.configuration.persistence.felix.FelixConfigPersistenceStrategy;
import org.codice.ddf.migration.ExportMigrationContext;
import org.codice.ddf.migration.Migratable;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationMessage;
import org.codice.ddf.migration.MigrationReport;
import org.codice.ddf.migration.MigrationWarning;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * This test creates a "mock" ddf directory structure for the system exported from and "mock"
 * directory structure for the system imported into. These directory structures are located under
 * the created TemporaryFolder. For example, if the TemporaryFolder is
 * /private/var/folders/2j/q2gjqn4s2mv53c2q53_m9d_w0000gn/T/junit4916060293677644046, one would see
 * a similar directory structure to the following:
 *
 * <p>// This is the system exported from: ./ddf ./ddf/etc/DDF_Custom_Mime_Type_Resolver-csw.config
 * ./ddf/Version.txt
 *
 * <p>// This is the system imported into: ./ddf ./ddf/etc ./ddf/Version.txt
 *
 * <p>// The backup from the imported system will look similar to this:
 * ./exported-1.0-20170816T142400.zip
 *
 * <p>// Thw exported zip from ddfExport will look similar to this: ./exported-1.0.zip
 *
 * <p>NOTE: The directory structure of the system imported into needs to be exactly the same as the
 * system exported from. For example, if they system being exported from has a ddf.home of /opt/ddf,
 * the system being imported into must also have a ddf.home of /opt/ddf.
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfigurationAdminMigratableTest {

  private static final String DDF_HOME_SYSTEM_PROP_KEY = "ddf.home";

  private static final String DDF_HOME = "ddf";

  private static final String DDF_EXPORTED_TAG_TEMPLATE = "exported_from_%s";

  private static final String SUPPORTED_VERSION = "1.0";

  private static final String DEFAULT_FILE_EXT = "config";

  private static final String CFG_FILE_EXT = "cfg";

  private static final String DDF_CUSTOM_MIME_TYPE_RESOLVER_FACTORY_PID =
      "DDF_Custom_Mime_Type_Resolver";

  private static final PrintStream OUT = System.out;

  @Rule public TemporaryFolder tempDir = new TemporaryFolder();

  private Path ddfHome;

  @Mock private ConfigurationAdmin configurationAdmin;

  @Mock private ConfigurationAdmin configurationAdminForExport;

  @Mock private ConfigurationAdmin configurationAdminForImport;

  @Mock private Configuration configurationImportedFromExport;

  @Mock private MBeanServer mBeanServer;

  @Mock private SystemService systemService;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private ExportMigrationContext exportMigrationContext;

  @Mock private MigrationReport migrationReport;

  @Before
  public void setup() throws Exception {
    setup(DDF_HOME);
    setupConfigAdminForExportSystem();
    setupConfigAdminForImportSystem();
  }

  @Test
  public void testGetPersisterForCfg() {
    ConfigurationAdminMigratable cam =
        new ConfigurationAdminMigratable(configurationAdmin, DEFAULT_FILE_EXT);
    PersistenceStrategy strategy = cam.getPersister(CFG_FILE_EXT);
    assertThat(strategy, instanceOf(FelixCfgPersistenceStrategy.class));
  }

  @Test
  public void testGetPersisterForConfig() {
    ConfigurationAdminMigratable cam =
        new ConfigurationAdminMigratable(configurationAdmin, DEFAULT_FILE_EXT);
    PersistenceStrategy strategy = cam.getPersister(DEFAULT_FILE_EXT);
    assertThat(strategy, instanceOf(FelixConfigPersistenceStrategy.class));
  }

  @Test
  public void testGetDefaultPersister() {
    ConfigurationAdminMigratable cam =
        new ConfigurationAdminMigratable(configurationAdmin, DEFAULT_FILE_EXT);
    PersistenceStrategy strategy = cam.getDefaultPersister();
    assertThat(strategy, instanceOf(FelixConfigPersistenceStrategy.class));
  }

  @Test
  public void testDoExportConfigAdminThrowsIOException() throws Exception {
    // Setup
    when(exportMigrationContext.getReport()).thenReturn(migrationReport);
    when(exportMigrationContext.getReport().record(any(MigrationMessage.class)))
        .thenReturn(migrationReport);
    when(configurationAdmin.listConfigurations(isNull())).thenThrow(IOException.class);
    ConfigurationAdminMigratable cam =
        new ConfigurationAdminMigratable(configurationAdmin, DEFAULT_FILE_EXT);

    // Perform Test
    cam.doExport(exportMigrationContext);

    // Verify
    verify(migrationReport).record(any(MigrationMessage.class));
  }

  @Test
  public void testDoExportConfigAdminThrowsInvalidSyntaxException() throws Exception {
    // Setup
    when(exportMigrationContext.getReport()).thenReturn(migrationReport);
    when(exportMigrationContext.getReport().record(any(MigrationMessage.class)))
        .thenReturn(migrationReport);
    when(configurationAdmin.listConfigurations(isNull())).thenThrow(InvalidSyntaxException.class);
    ConfigurationAdminMigratable cam =
        new ConfigurationAdminMigratable(configurationAdmin, DEFAULT_FILE_EXT);

    // Perform Test
    cam.doExport(exportMigrationContext);

    // Verify
    verify(migrationReport).record(any(MigrationMessage.class));
  }

  /**
   * This test has one managed service factory configuration (DDF_Custom_Mime_Type_Resolver) in the
   * system exported from. The system being imported into has one managed service configuration
   * (ddf.platform.ui.config). When the import is complete, the system being imported into should
   * have the managed service factory configuration (DDF_Custom_Mime_Type_Resolver) in its
   * Configuration Admin.
   */
  @Test
  public void testDoExportDoImport() throws Exception {
    // Setup Export
    Path exportDir = tempDir.getRoot().toPath().toRealPath();
    ConfigurationAdminMigratable eCam =
        new ConfigurationAdminMigratable(configurationAdminForExport, DEFAULT_FILE_EXT);
    List<Migratable> eMigratables = Arrays.asList(eCam);
    ConfigurationMigrationManager eConfigurationMigrationManager =
        new ConfigurationMigrationManager(mBeanServer, eMigratables, systemService);
    String tag = String.format(DDF_EXPORTED_TAG_TEMPLATE, DDF_HOME);
    setupConfigFile(tag);

    // Perform Export
    MigrationReport exportReport = eConfigurationMigrationManager.doExport(exportDir, this::print);

    // Verify Export
    Assert.assertThat("The export report has errors.", exportReport.hasErrors(), is(false));
    Assert.assertThat("The export report has warnings.", exportReport.hasWarnings(), is(false));
    Assert.assertThat("Export was not successful.", exportReport.wasSuccessful(), is(true));
    String exportedZipBaseName = String.format("exported-%s.zip", SUPPORTED_VERSION);
    Path exportedZip = exportDir.resolve(exportedZipBaseName).toRealPath();
    Assert.assertThat("Export zip does not exist.", exportedZip.toFile().exists(), is(true));
    Assert.assertThat("Exported zip is empty.", exportedZip.toFile().length(), greaterThan(0L));

    // Setup Import
    // Clean up ddf home, so we can import into a clean directory
    FileUtils.deleteDirectory(ddfHome.toRealPath().toFile());
    setup(DDF_HOME);

    ConfigurationAdminMigratable iCam =
        new ConfigurationAdminMigratable(configurationAdminForImport, DEFAULT_FILE_EXT);
    List<Migratable> iMigratables = Arrays.asList(iCam);
    ConfigurationMigrationManager iConfigurationMigrationManager =
        new ConfigurationMigrationManager(mBeanServer, iMigratables, systemService);

    // Perform Import
    MigrationReport importReport = iConfigurationMigrationManager.doImport(exportDir, this::print);

    // Verify import
    Assert.assertThat("The import report has errors.", importReport.hasErrors(), is(false));
    Assert.assertThat("The import report has warnings.", importReport.hasWarnings(), is(false));
    Assert.assertThat("Import was not successful.", importReport.wasSuccessful(), is(true));
    // Verify that the config admin in the system being imported into gets the factory configuration
    // from the system being exported from.
    verify(configurationAdminForImport)
        .createFactoryConfiguration(eq(DDF_CUSTOM_MIME_TYPE_RESOLVER_FACTORY_PID), isNull());
    ArgumentCaptor<Dictionary<String, ?>> argumentCaptor =
        ArgumentCaptor.forClass(Dictionary.class);
    verify(configurationImportedFromExport).update(argumentCaptor.capture());
    Map<String, ?> dictionayAsMap = convertToMap(argumentCaptor.<Dictionary<String, ?>>getValue());
    assertThat(
        dictionayAsMap,
        allOf(
            aMapWithSize(2),
            hasEntry("schema", "http://www.opengis.net/cat/csw/2.0.2"),
            hasEntry(
                DirectoryWatcher.FILENAME,
                ddfHome
                    .resolve("etc")
                    .toRealPath()
                    .resolve(
                        String.format("%s-csw.config", DDF_CUSTOM_MIME_TYPE_RESOLVER_FACTORY_PID))
                    .toUri()
                    .toString())));
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

  private Map<String, Object> convertToMap(Dictionary<String, ?> dictionary) {
    Map<String, Object> map = new HashMap<>();
    Enumeration<String> keys = dictionary.keys();
    while (keys.hasMoreElements()) {
      String key = keys.nextElement();
      map.put(key, dictionary.get(key));
    }
    return map;
  }

  private void setupConfigAdminForExportSystem() throws Exception {
    Configuration[] configurations = getConfigurationsForExportSystem();
    when(configurationAdminForExport.listConfigurations(isNull())).thenReturn(configurations);
  }

  private void setupConfigAdminForImportSystem() throws Exception {
    Configuration[] configurations = getConfigurationsForImportSystem();
    when(configurationAdminForImport.listConfigurations(isNull())).thenReturn(configurations);
    // Create a new factory Configuration object with a new PID. The properties of the new
    // Configuration
    // object are null until the first time that its Configuration.update(Dictionary) method is
    // called.
    when(configurationImportedFromExport.getPid())
        .thenReturn(
            String.format(
                "%s.8e3c2b12-9807-482a-aaa7-d3d317973581",
                DDF_CUSTOM_MIME_TYPE_RESOLVER_FACTORY_PID));
    when(configurationAdminForImport.createFactoryConfiguration(
            eq(DDF_CUSTOM_MIME_TYPE_RESOLVER_FACTORY_PID), isNull()))
        .thenReturn(configurationImportedFromExport);
  }

  private Configuration[] getConfigurationsForExportSystem() throws IOException {
    Configuration[] configs = new Configuration[1];
    configs[0] = getConfigurationForExportSystem();
    return configs;
  }

  private Configuration[] getConfigurationsForImportSystem() throws IOException {
    Configuration[] configs = new Configuration[1];
    configs[0] = getConfigurationForImportSystem();
    return configs;
  }

  private Configuration getConfigurationForExportSystem() throws IOException {
    Configuration config = mock(Configuration.class);
    Dictionary<String, Object> props = new Hashtable<>();
    props.put(
        DirectoryWatcher.FILENAME,
        ddfHome
            .resolve("etc")
            .toRealPath()
            .resolve(String.format("%s-csw.config", DDF_CUSTOM_MIME_TYPE_RESOLVER_FACTORY_PID))
            .toUri());
    String pid =
        String.format(
            "%s.4039089d-839f-4d52-a174-77c8a19fc03d", DDF_CUSTOM_MIME_TYPE_RESOLVER_FACTORY_PID);
    props.put("service.pid", pid);
    props.put("service.factoryPid", DDF_CUSTOM_MIME_TYPE_RESOLVER_FACTORY_PID);
    props.put("schema", "http://www.opengis.net/cat/csw/2.0.2");
    when(config.getProperties()).thenReturn(props);
    when(config.getPid()).thenReturn(pid);
    when(config.getFactoryPid()).thenReturn(DDF_CUSTOM_MIME_TYPE_RESOLVER_FACTORY_PID);
    return config;
  }

  private Configuration getConfigurationForImportSystem() throws IOException {
    Configuration config = mock(Configuration.class);
    String pid = "ddf.platform.ui.config";
    when(config.getPid()).thenReturn(pid);
    return config;
  }

  private void setup(String ddfHomeStr) throws IOException {
    ddfHome = tempDir.newFolder(ddfHomeStr).toPath().toRealPath();
    System.setProperty(DDF_HOME_SYSTEM_PROP_KEY, ddfHome.toRealPath().toString());
    Path etcDir = ddfHome.resolve("etc");
    Files.createDirectory(etcDir);
    setupVersionFile(SUPPORTED_VERSION);
  }

  private void setupVersionFile(String version) throws IOException {
    Path versionFile = ddfHome.resolve("Version.txt");
    Files.createFile(versionFile);
    FileUtils.writeStringToFile(
        versionFile.toFile().getCanonicalFile(), version, StandardCharsets.UTF_8);
  }

  private void setupConfigFile(String tag) throws IOException {
    Path configFile =
        ddfHome
            .resolve("etc")
            .toRealPath()
            .resolve(String.format("%s-csw.config", DDF_CUSTOM_MIME_TYPE_RESOLVER_FACTORY_PID));
    Files.createFile(configFile);
    List<String> lines = new ArrayList<>(2);
    lines.add(String.format("#%s:%s", configFile.toRealPath().toString(), tag));
    lines.add(String.format("%s=%s", DirectoryWatcher.FILENAME, configFile.toUri().toString()));
    FileUtils.writeLines(
        configFile.toFile(), StandardCharsets.UTF_8.toString(), lines, System.lineSeparator());
  }
}
