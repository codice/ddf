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

import static org.codice.ddf.configuration.admin.ConfigurationAdminMigratable.ACCEPTED_ENTRY_PIDS;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.io.FileUtils;
import org.apache.felix.fileinstall.internal.DirectoryWatcher;
import org.apache.karaf.system.SystemService;
import org.codice.ddf.configuration.migration.ConfigurationMigrationManager;
import org.codice.ddf.migration.ExportMigrationContext;
import org.codice.ddf.migration.ImportMigrationContext;
import org.codice.ddf.migration.ImportMigrationEntry;
import org.codice.ddf.migration.Migratable;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationMessage;
import org.codice.ddf.migration.MigrationReport;
import org.codice.ddf.migration.MigrationWarning;
import org.codice.ddf.platform.io.CfgStrategy;
import org.codice.ddf.platform.io.ConfigStrategy;
import org.codice.ddf.platform.io.internal.PersistenceStrategy;
import org.hamcrest.Matchers;
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
 * <p>// This is the system exported from: ./ddf
 * ./ddf/admin/DDF_Custom_Mime_Type_Resolver-csw.config ./ddf/Version.txt
 *
 * <p>// This is the system imported into: ./ddf ./ddf/admin ./ddf/Version.txt
 *
 * <p>// The backup from the imported system will look similar to this:
 * ./exported-1.0-20170816T142400.dar
 *
 * <p>// The exported zip from ddfExport will look similar to this: ./exported-1.0.dar
 *
 * <p>NOTE: The directory structure of the system imported into needs to be exactly the same as the
 * system exported from. For example, if they system being exported from has a ddf.home of /opt/ddf,
 * the system being imported into must also have a ddf.home of /opt/ddf.
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfigurationAdminMigratableTest {

  private static final String DDF_HOME_SYSTEM_PROP_KEY = "ddf.home";

  private static final String DDF_HOSTNAME_PROP_KEY = "org.codice.ddf.system.hostname";

  private static final String DDF_HOME = "ddf";

  private static final String DDF_EXPORTED_TAG_TEMPLATE = "exported_from_%s";

  private static final String SUPPORTED_BRANDING = "test";

  // needs to be 2.13.3, 2.13.4, or 2.13.5 in order to test hostname insertion
  private static final String SUPPORTED_VERSION = "2.13.3";

  private static final String IMPORTING_PRODUCT_VERSION = "3.0";

  private static final String DEFAULT_FILE_EXT = "config";

  private static final String CFG_FILE_EXT = "cfg";

  private static final String CONTENT_DIRECTORY_MONITOR_FACTORY_PID =
      "org.codice.ddf.catalog.content.monitor.ContentDirectoryMonitor";

  private static final String CONTENT_DIRECTORY_MONITOR_FILENAME =
      String.format("%s-csw.config", CONTENT_DIRECTORY_MONITOR_FACTORY_PID);

  private static final String URL_RESOURCE_READER_FACTORY_PID =
      "ddf.catalog.resource.impl.URLResourceReader";

  private static final String URL_RESOURCE_READER_FILENAME =
      String.format("%s-csw.config", URL_RESOURCE_READER_FACTORY_PID);

  private static final String DDF_CUSTOM_MIME_TYPE_RESOLVER_FACTORY_PID =
      "DDF_Custom_Mime_Type_Resolver";

  private static final String DDF_CUSTOM_MIME_TYPE_RESOLVER_FILENAME =
      String.format("%s-csw.config", DDF_CUSTOM_MIME_TYPE_RESOLVER_FACTORY_PID);

  private static final String WEB_CONTEXT_POLICY_MANAGER_PID =
      "org.codice.ddf.security.policy.context.impl.PolicyManager";

  private static final String METACARD_VALIDITY_FILTER_PLUGIN_PID =
      "ddf.catalog.metacard.validation.MetacardValidityFilterPlugin";

  private static final List<PersistenceStrategy> STRATEGIES =
      ImmutableList.of(new CfgStrategy(), new ConfigStrategy());

  private static final PrintStream OUT = System.out;

  @Rule public TemporaryFolder tempDir = new TemporaryFolder();

  private Path ddfHome;

  @Mock private ConfigurationAdmin configurationAdmin;

  @Mock private ConfigurationAdmin configurationAdminForExport;

  @Mock private ConfigurationAdmin configurationAdminForImport;

  @Mock private SystemService systemService;

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private ExportMigrationContext exportMigrationContext;

  @Mock private MigrationReport migrationReport;

  private List<Configuration> configurations;

  @Before
  public void setup() throws Exception {
    setup(DDF_HOME, SUPPORTED_VERSION);
    setupConfigAdminForExportSystem(
        Collections.emptyList(),
        Collections.singletonList(DDF_CUSTOM_MIME_TYPE_RESOLVER_FACTORY_PID));
    setupConfigAdminForImportSystem(
        Collections.emptyList(),
        Collections.singletonList(DDF_CUSTOM_MIME_TYPE_RESOLVER_FACTORY_PID));
  }

  @Test
  public void testGetPersisterForCfg() {
    ConfigurationAdminMigratable cam =
        new ConfigurationAdminMigratable(configurationAdmin, STRATEGIES, DEFAULT_FILE_EXT);
    PersistenceStrategy strategy = cam.getPersister(CFG_FILE_EXT);
    assertThat(strategy, instanceOf(CfgStrategy.class));
  }

  @Test
  public void testGetPersisterForConfig() {
    ConfigurationAdminMigratable cam =
        new ConfigurationAdminMigratable(configurationAdmin, STRATEGIES, DEFAULT_FILE_EXT);
    PersistenceStrategy strategy = cam.getPersister(DEFAULT_FILE_EXT);
    assertThat(strategy, instanceOf(ConfigStrategy.class));
  }

  @Test
  public void testDoExportConfigAdminThrowsIOException() throws Exception {
    // Setup
    when(exportMigrationContext.getReport()).thenReturn(migrationReport);
    when(exportMigrationContext.getReport().record(any(MigrationMessage.class)))
        .thenReturn(migrationReport);
    when(configurationAdmin.listConfigurations(isNull())).thenThrow(IOException.class);
    ConfigurationAdminMigratable cam =
        new ConfigurationAdminMigratable(configurationAdmin, STRATEGIES, DEFAULT_FILE_EXT);

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
        new ConfigurationAdminMigratable(configurationAdmin, STRATEGIES, DEFAULT_FILE_EXT);

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
    final String tag = String.format(DDF_EXPORTED_TAG_TEMPLATE, DDF_HOME);
    final Path configFile = setupConfigFile(tag, DDF_CUSTOM_MIME_TYPE_RESOLVER_FILENAME);

    // override to intercept doImport() and verify exported files from etc. Must be done before zip
    // file is closed
    ConfigurationAdminMigratable eCam =
        new ConfigurationAdminMigratable(
            configurationAdminForExport, STRATEGIES, DEFAULT_FILE_EXT) {
          @Override
          public void doImport(ImportMigrationContext context) {
            super.doImport(context);
            assertThat(configFile.toFile(), Matchers.equalTo(false));
            // Verify exported files from etc since we are currently not re-importing them
            context.entries(Paths.get("etc")).forEach(ImportMigrationEntry::restore);
            try {
              verifyConfigFile(configFile, tag);
            } catch (IOException e) {
              throw new AssertionError(e);
            }
          }
        };
    List<Migratable> eMigratables = Arrays.asList(eCam);
    ConfigurationMigrationManager eConfigurationMigrationManager =
        new ConfigurationMigrationManager(eMigratables, systemService);

    // Perform Export
    MigrationReport exportReport = eConfigurationMigrationManager.doExport(exportDir, this::print);

    // Verify Export
    Assert.assertThat("The export report has errors.", exportReport.hasErrors(), is(false));
    Assert.assertThat("The export report has warnings.", exportReport.hasWarnings(), is(false));
    Assert.assertThat("Export was not successful.", exportReport.wasSuccessful(), is(true));
    String exportedZipBaseName = String.format("%s-%s.dar", SUPPORTED_BRANDING, SUPPORTED_VERSION);
    Path exportedZip = exportDir.resolve(exportedZipBaseName).toRealPath();
    Assert.assertThat("Export zip does not exist.", exportedZip.toFile().exists(), is(true));
    Assert.assertThat("Exported zip is empty.", exportedZip.toFile().length(), greaterThan(0L));

    // Setup Import
    // Clean up ddf home, so we can import into a clean directory
    FileUtils.deleteDirectory(ddfHome.toRealPath().toFile());
    setup(DDF_HOME, SUPPORTED_VERSION);

    // intercept doImport() to verify exported files from etc
    ConfigurationAdminMigratable iCam =
        new ConfigurationAdminMigratable(configurationAdminForImport, STRATEGIES, DEFAULT_FILE_EXT);

    List<Migratable> iMigratables = Arrays.asList(iCam);
    ConfigurationMigrationManager iConfigurationMigrationManager =
        new ConfigurationMigrationManager(iMigratables, systemService);

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
    verify(configurations.get(0)).update(argumentCaptor.capture());
    Map<String, ?> dictionaryAsMap = convertToMap(argumentCaptor.getValue());
    assertThat(
        dictionaryAsMap,
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

  @Test
  public void testDoExportDoVersionUpgradeImport() throws Exception {
    // Setup Export
    List<String> factoryPids =
        Arrays.asList(
            CONTENT_DIRECTORY_MONITOR_FACTORY_PID,
            "Csw_Registry_Store",
            "Csw_Federation_Profile_Source",
            "Test_Service",
            DDF_CUSTOM_MIME_TYPE_RESOLVER_FACTORY_PID);

    setupConfigAdminForExportSystem(ACCEPTED_ENTRY_PIDS, factoryPids);
    Path exportDir = tempDir.getRoot().toPath().toRealPath();
    final String tag = String.format(DDF_EXPORTED_TAG_TEMPLATE, DDF_HOME);
    final Path configFile = setupConfigFile(tag, CONTENT_DIRECTORY_MONITOR_FILENAME);
    final Path configFile2 = setupConfigFile(tag, URL_RESOURCE_READER_FILENAME);

    // override to intercept doImport() and verify exported files from etc. Must be done before zip
    // file is closed
    ConfigurationAdminMigratable eCam =
        new ConfigurationAdminMigratable(
            configurationAdminForExport, STRATEGIES, DEFAULT_FILE_EXT) {
          @Override
          public void doVersionUpgradeImport(ImportMigrationContext context) {
            super.doVersionUpgradeImport(context);
            assertThat(configFile.toFile(), Matchers.equalTo(false));
            assertThat(configFile2.toFile(), Matchers.equalTo(false));
            // Verify exported files from etc since we are currently not re-importing them
            context.entries(Paths.get("etc")).forEach(ImportMigrationEntry::restore);
            try {
              verifyConfigFile(configFile, tag);
              verifyConfigFile(configFile2, tag);
            } catch (IOException e) {
              throw new AssertionError(e);
            }
          }
        };
    List<Migratable> eMigratables = Arrays.asList(eCam);
    ConfigurationMigrationManager eConfigurationMigrationManager =
        new ConfigurationMigrationManager(eMigratables, systemService);

    // Perform Export
    MigrationReport exportReport = eConfigurationMigrationManager.doExport(exportDir, this::print);

    // Verify Export
    Assert.assertThat("The export report has errors.", exportReport.hasErrors(), is(false));
    Assert.assertThat("The export report has warnings.", exportReport.hasWarnings(), is(false));
    Assert.assertThat("Export was not successful.", exportReport.wasSuccessful(), is(true));
    String exportedZipBaseName = String.format("%s-%s.dar", SUPPORTED_BRANDING, SUPPORTED_VERSION);
    Path exportedZip = exportDir.resolve(exportedZipBaseName).toRealPath();
    Assert.assertThat("Export zip does not exist.", exportedZip.toFile().exists(), is(true));
    Assert.assertThat("Exported zip is empty.", exportedZip.toFile().length(), greaterThan(0L));

    // Setup Import
    // Clean up ddf home, so we can import into a clean directory
    FileUtils.deleteDirectory(ddfHome.toRealPath().toFile());
    setup(DDF_HOME, IMPORTING_PRODUCT_VERSION);
    setupConfigAdminForImportSystem(ACCEPTED_ENTRY_PIDS, factoryPids);

    // intercept doImport() to verify exported files from etc
    ConfigurationAdminMigratable iCam =
        spy(
            new ConfigurationAdminMigratable(
                configurationAdminForImport, STRATEGIES, DEFAULT_FILE_EXT));
    doReturn(
            ImmutableMap.of("whiteListContexts", new String[] {"/login", "/logout", "/idp"}),
            ImmutableMap.of("guestAccess", "true"),
            ImmutableMap.of("sessionAccess", "true"))
        .when(iCam)
        .getDefaultProperties(WEB_CONTEXT_POLICY_MANAGER_PID);
    List<Migratable> iMigratables = Collections.singletonList(iCam);
    ConfigurationMigrationManager iConfigurationMigrationManager =
        new ConfigurationMigrationManager(iMigratables, systemService);

    // Perform Import
    MigrationReport importReport = iConfigurationMigrationManager.doImport(exportDir, this::print);

    // Verify import
    Assert.assertThat("The import report has errors.", importReport.hasErrors(), is(false));
    Assert.assertThat("The import report has warnings.", importReport.hasWarnings(), is(false));
    Assert.assertThat("Import was not successful.", importReport.wasSuccessful(), is(true));
    verify(iCam).doVersionUpgradeImport(any(ImportMigrationContext.class));

    // Does not have DDF_CUSTOM_MIME_TYPE_RESOLVER_FACTORY_PID because not acceptable version
    verify(configurationAdminForImport, never())
        .createFactoryConfiguration(
            eq(DDF_CUSTOM_MIME_TYPE_RESOLVER_FACTORY_PID), nullable(String.class));

    // Verify that the config admin in the system being imported into gets the factory configuration
    // from the system being exported from.
    // Using legacy for loops due to exception handling
    for (String fPid : factoryPids) {
      if (!fPid.equals(DDF_CUSTOM_MIME_TYPE_RESOLVER_FACTORY_PID)) {
        verify(configurationAdminForImport).createFactoryConfiguration(eq(fPid), isNull());
      }
    }
    for (Configuration config : configurations) {
      if (config.getPid().contains(WEB_CONTEXT_POLICY_MANAGER_PID)) {
        ArgumentCaptor<Dictionary<String, ?>> argumentCaptor =
            ArgumentCaptor.forClass(Dictionary.class);
        verify(config).update(argumentCaptor.capture());
        Map<String, ?> dictionaryAsMap = convertToMap(argumentCaptor.getValue());
        assertThat(
            dictionaryAsMap,
            allOf(
                aMapWithSize(5),
                hasEntry("schema", "http://www.opengis.net/cat/csw/2.0.2"),
                hasEntry("guestAccess", "true"),
                hasEntry("sessionAccess", "true"),
                hasEntry("whiteListContexts", "/idp,/login,/logout"),
                hasEntry("authenticationTypes", "/=SAML,/admin=SAML|basic")));
        // Does not have filename because felix.fileinstall is removed
      } else if (config.getPid().contains(METACARD_VALIDITY_FILTER_PLUGIN_PID)) {
        ArgumentCaptor<Dictionary<String, ?>> argumentCaptor =
            ArgumentCaptor.forClass(Dictionary.class);
        verify(config).update(argumentCaptor.capture());
        Map<String, ?> dictionaryAsMap = convertToMap(argumentCaptor.getValue());
        String[] attributeMap = {
          "invalid-state=test-host-data-manager,system-user",
          "invalid-state=hostname-data-manager,system-user"
        };
        assertThat(
            dictionaryAsMap,
            allOf(
                aMapWithSize(4),
                hasEntry("schema", (Object) "http://www.opengis.net/cat/csw/2.0.2"),
                hasEntry("filterWarnings", (Object) "false"),
                hasEntry("filterErrors", (Object) "true"),
                hasEntry("attributeMap", (Object) attributeMap)));
      } else if (!config.getPid().contains(DDF_CUSTOM_MIME_TYPE_RESOLVER_FACTORY_PID)) {
        ArgumentCaptor<Dictionary<String, ?>> argumentCaptor =
            ArgumentCaptor.forClass(Dictionary.class);
        verify(config).update(argumentCaptor.capture());
        Map<String, ?> dictionaryAsMap = convertToMap(argumentCaptor.getValue());
        assertThat(
            dictionaryAsMap,
            allOf(aMapWithSize(1), hasEntry("schema", "http://www.opengis.net/cat/csw/2.0.2")));
        // Does not have filename because felix.fileinstall is removed
      }
    }
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

  private void setupConfigAdminForExportSystem(List<String> pids, List<String> configFactoryPids)
      throws Exception {
    List<Configuration> configs =
        pids.stream().map(this::getConfigurationForExportSystem).collect(Collectors.toList());
    List<Configuration> factoryConfigs =
        configFactoryPids
            .stream()
            .map(this::getFactoryConfigurationForExportSystem)
            .collect(Collectors.toList());
    configurations = ListUtils.union(configs, factoryConfigs);
    when(configurationAdminForExport.listConfigurations(isNull()))
        .thenReturn(configurations.toArray(new Configuration[0]));
    when(exportMigrationContext.entries(any(), eq(false), any())).thenReturn(Stream.empty());
  }

  private void setupConfigAdminForImportSystem(List<String> pids, List<String> configFactoryPids)
      throws Exception {
    List<Configuration> configs =
        pids.stream().map(this::getConfigurationForImportSystem).collect(Collectors.toList());
    List<Configuration> factoryConfigs =
        configFactoryPids
            .stream()
            .map(this::getFactoryConfigurationForImportSystem)
            .collect(Collectors.toList());
    configurations = ListUtils.union(configs, factoryConfigs);
    when(configurationAdminForImport.listConfigurations(isNull()))
        .thenReturn(configurations.toArray(new Configuration[0]));
  }

  private Configuration getFactoryConfigurationForExportSystem(String configFactoryPid) {
    Configuration config = mock(Configuration.class);
    Dictionary<String, Object> props = new Hashtable<>();
    try {
      props.put(
          DirectoryWatcher.FILENAME,
          ddfHome
              .resolve("etc")
              .toRealPath()
              .resolve(String.format("%s-csw.config", configFactoryPid))
              .toUri());
    } catch (IOException e) {
      fail("Unable to resolve configuration path");
    }
    String pid = String.format("%s.4039089d-839f-4d52-a174-77c8a19fc03d", configFactoryPid);
    props.put("service.pid", pid);
    props.put("service.factoryPid", configFactoryPid);
    props.put("schema", "http://www.opengis.net/cat/csw/2.0.2");
    when(config.getProperties()).thenReturn(props);
    when(config.getPid()).thenReturn(pid);
    when(config.getFactoryPid()).thenReturn(configFactoryPid);
    return config;
  }

  private Configuration getConfigurationForExportSystem(String pid) {
    return getConfigurationForExportSystem(pid, null);
  }

  private Configuration getConfigurationForExportSystem(
      String pid, Dictionary<String, Object> props) {
    Configuration config = mock(Configuration.class);
    if (props == null) {
      props = new Hashtable<>();
      props.put("service.pid", pid);
      props.put("schema", "http://www.opengis.net/cat/csw/2.0.2");
      if (pid.equals(WEB_CONTEXT_POLICY_MANAGER_PID)) {
        props.put("authenticationTypes", new String[] {"/=IDP|GUEST", "/admin=IDP|basic"});
        props.put("whiteListContexts", new String[] {"/login", "/logout", "/idp"});
        props.put("guestAccess", "true");
        props.put("sessionAccess", "true");
      }
      if (pid.equals(METACARD_VALIDITY_FILTER_PLUGIN_PID)) {
        props.put(
            "attributeMap",
            new String[] {
              "invalid-state=data-manager,system-user",
              "invalid-state=hostname-data-manager,system-user"
            });
        props.put("filterWarnings", "false");
        props.put("filterErrors", "true");
      }
    }

    try {
      props.put(
          DirectoryWatcher.FILENAME,
          ddfHome.resolve("etc").toRealPath().resolve(String.format("%s.config", pid)).toUri());
    } catch (IOException e) {
      fail("Unable to resolve configuration path");
    }
    when(config.getProperties()).thenReturn(props);
    when(config.getPid()).thenReturn(pid);
    return config;
  }

  private Configuration getFactoryConfigurationForImportSystem(String configFactoryPid) {
    Configuration config = mock(Configuration.class);
    // Create a new factory Configuration object with a new PID. The properties of the new
    // Configuration
    // object are null until the first time that its Configuration.update(Dictionary) method is
    // called.
    when(config.getPid())
        .thenReturn(String.format("%s.8e3c2b12-9807-482a-aaa7-d3d317973581", configFactoryPid));
    when(config.getFactoryPid()).thenReturn(configFactoryPid);
    try {
      when(configurationAdminForImport.createFactoryConfiguration(eq(configFactoryPid), isNull()))
          .thenReturn(config);
    } catch (IOException e) {
      // Can't happen since this is mocking the createFactoryConfiguration method.
    }
    return config;
  }

  private Configuration getConfigurationForImportSystem(String pid) {
    Configuration config = mock(Configuration.class);
    when(config.getPid()).thenReturn(pid);
    return config;
  }

  private void setup(String ddfHomeStr, String productVersion) throws IOException {
    ddfHome = tempDir.newFolder(ddfHomeStr).toPath().toRealPath();
    System.setProperty(DDF_HOME_SYSTEM_PROP_KEY, ddfHome.toRealPath().toString());
    System.setProperty(DDF_HOSTNAME_PROP_KEY, "test-host");
    Path etcDir = ddfHome.resolve("etc");
    Files.createDirectory(etcDir);
    setupSystemPropertiesFiles();
    setupBrandingFile(SUPPORTED_BRANDING);
    setupVersionFile(productVersion);
    setupMigrationPropertiesFile(SUPPORTED_VERSION);
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

  private void setupMigrationPropertiesFile(String version) throws IOException {
    final Path migrationPropsFile = ddfHome.resolve(Paths.get("etc", "migration.properties"));

    Files.createFile(migrationPropsFile);
    FileUtils.writeStringToFile(
        migrationPropsFile.toFile().getCanonicalFile(),
        "supported.versions=" + version,
        StandardCharsets.UTF_8);
  }

  private void setupSystemPropertiesFiles() throws IOException {
    final Path systemPropsFile = ddfHome.resolve(Paths.get("etc", "system.properties"));
    Files.createFile(systemPropsFile);

    final Path customSystemPropsFile =
        ddfHome.resolve(Paths.get("etc", "custom.system.properties"));
    Files.createFile(customSystemPropsFile);
  }

  private Path setupConfigFile(String tag, String configFileName) throws IOException {
    Path configFile = ddfHome.resolve("etc").toRealPath().resolve(configFileName);
    Files.createFile(configFile);
    List<String> lines = new ArrayList<>(2);
    lines.add(String.format("#%s:%s", configFile.toRealPath().toString(), tag));
    lines.add(String.format("%s=%s", DirectoryWatcher.FILENAME, configFile.toUri().toString()));
    FileUtils.writeLines(
        configFile.toFile(), StandardCharsets.UTF_8.toString(), lines, System.lineSeparator());
    return configFile;
  }

  private void verifyConfigFile(Path configFile, String tag) throws IOException {
    assertThat(
        FileUtils.readLines(configFile.toFile(), StandardCharsets.UTF_8.toString()),
        Matchers.contains(
            String.format("#%s:%s", configFile.toRealPath().toString(), tag),
            String.format("%s=%s", DirectoryWatcher.FILENAME, configFile.toUri().toString())));
  }
}
