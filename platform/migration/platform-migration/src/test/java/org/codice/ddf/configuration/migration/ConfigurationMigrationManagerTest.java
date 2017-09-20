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
package org.codice.ddf.configuration.migration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.apache.karaf.system.SystemService;
import org.codice.ddf.migration.Migratable;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationInformation;
import org.codice.ddf.migration.MigrationMessage;
import org.codice.ddf.migration.MigrationReport;
import org.codice.ddf.migration.MigrationWarning;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.StringDescription;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationMigrationManagerTest extends AbstractMigrationTest {

  public static final String TEST_VERSION = "1.0";

  public static final String TEST_MESSAGE = "Test message.";

  public static final String TEST_DIRECTORY = "exported";

  private ConfigurationMigrationManager configurationMigrationManager;

  private ObjectName configMigrationServiceObjectName;

  private List<Migratable> migratables;

  @Mock private MBeanServer mockMBeanServer;

  @Mock private SystemService mockSystemService;

  @Before
  public void setup() throws MalformedObjectNameException {
    configMigrationServiceObjectName =
        new ObjectName(
            ConfigurationMigrationManager.class.getName() + ":service=configuration-migration");
    migratables = Collections.emptyList();
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorWithNullMBeanServer() {
    new ConfigurationMigrationManager(null, new ArrayList<>(), mockSystemService);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorWithNullConfigurationMigratablesList() {
    new ConfigurationMigrationManager(mockMBeanServer, null, mockSystemService);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorWithNullSystemService() {
    new ConfigurationMigrationManager(mockMBeanServer, new ArrayList<>(), null);
  }

  @Test(expected = IOError.class)
  public void constructorWithoutProductVersion() {
    new ConfigurationMigrationManager(mockMBeanServer, new ArrayList<>(), mockSystemService);
  }

  @Test
  public void init() throws Exception {
    configurationMigrationManager = getConfigurationMigrationManager();
    configurationMigrationManager.init();

    verify(mockMBeanServer)
        .registerMBean(configurationMigrationManager, configMigrationServiceObjectName);
  }

  @Test
  public void initWhenServiceAlreadyRegisteredAsMBean() throws Exception {
    configurationMigrationManager = getConfigurationMigrationManager();

    when(mockMBeanServer.registerMBean(
            configurationMigrationManager, configMigrationServiceObjectName))
        .thenThrow(new InstanceAlreadyExistsException())
        .thenReturn(null);

    configurationMigrationManager.init();

    verify(mockMBeanServer, times(2))
        .registerMBean(configurationMigrationManager, configMigrationServiceObjectName);
    verify(mockMBeanServer).unregisterMBean(configMigrationServiceObjectName);
  }

  @Test
  public void doExportWithStringSucceedsWithWarnings() throws Exception {
    configurationMigrationManager = spy(getConfigurationMigrationManager());
    MigrationReport mockReport = mock(MigrationReport.class);

    when(mockReport.warnings()).thenReturn(Stream.of(new MigrationWarning(TEST_MESSAGE)));
    doReturn(mockReport).when(configurationMigrationManager).doExport(any(Path.class));

    Collection<MigrationWarning> warnings = configurationMigrationManager.doExport(TEST_DIRECTORY);

    reportHasWarningMessage(warnings.stream(), equalTo(TEST_MESSAGE));
    verify(configurationMigrationManager).doExport(any(Path.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void doExportWithNullString() throws Exception {
    configurationMigrationManager = getConfigurationMigrationManager();

    configurationMigrationManager.doExport((String) null);
  }

  @Test(expected = MigrationException.class)
  public void doExportWithStringThrowsException() throws Exception {
    configurationMigrationManager = spy(getConfigurationMigrationManager());
    MigrationReport mockReport = mock(MigrationReport.class);
    doThrow(MigrationException.class).when(mockReport).verifyCompletion();
    doReturn(mockReport).when(configurationMigrationManager).doExport(any(Path.class));

    configurationMigrationManager.doExport(TEST_DIRECTORY);
  }

  @Test
  public void doExportSucceeds() throws Exception {
    configurationMigrationManager = spy(getConfigurationMigrationManager());

    expectExportDelegationIsSuccessful();

    MigrationReport report =
        configurationMigrationManager.doExport(ddfHome.resolve(TEST_DIRECTORY));

    assertThat("Export was not successful", report.wasSuccessful(), is(true));
    verify(configurationMigrationManager)
        .delegateToExportMigrationManager(any(MigrationReportImpl.class), any(Path.class));
  }

  @Test
  public void doExportSucceedsWithConsumer() throws Exception {
    final Consumer<MigrationMessage> consumer = mock(Consumer.class);

    configurationMigrationManager = spy(getConfigurationMigrationManager());

    expectExportDelegationIsSuccessful();

    MigrationReport report =
        configurationMigrationManager.doExport(ddfHome.resolve(TEST_DIRECTORY), consumer);

    assertThat("Export was not successful", report.wasSuccessful(), is(true));
    verify(configurationMigrationManager)
        .delegateToExportMigrationManager(any(MigrationReportImpl.class), any(Path.class));
    verify(consumer, Mockito.atLeastOnce()).accept(Mockito.notNull());
  }

  @Test
  public void doExportSucceedsWithWarnings() throws Exception {
    final Path path = ddfHome.resolve(TEST_DIRECTORY);
    configurationMigrationManager = spy(getConfigurationMigrationManager());

    doAnswer(
            invocation -> {
              MigrationReport report = invocation.getArgument(0);
              report.record(new MigrationWarning(TEST_MESSAGE));
              return null;
            })
        .when(configurationMigrationManager)
        .delegateToExportMigrationManager(any(MigrationReportImpl.class), any(Path.class));

    MigrationReport report = configurationMigrationManager.doExport(path);

    assertThat("Export was not successful", report.wasSuccessful(), is(true));
    reportHasWarningMessage(report.warnings(), equalTo(TEST_MESSAGE));
    reportHasWarningMessage(
        report.warnings(),
        equalTo(
            "Successfully exported to file ["
                + path.resolve("exported")
                + "-"
                + TEST_VERSION
                + ".zip] with warnings; make sure to review."));
    verify(configurationMigrationManager)
        .delegateToExportMigrationManager(any(MigrationReportImpl.class), any(Path.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void doExportWithNullPath() throws Exception {
    configurationMigrationManager = getConfigurationMigrationManager();

    configurationMigrationManager.doExport((Path) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void doExportWithNullConsumer() throws Exception {
    configurationMigrationManager = getConfigurationMigrationManager();

    configurationMigrationManager.doExport(ddfHome.resolve(TEST_DIRECTORY), null);
  }

  @Test
  public void doExportFailsToCreateDirectory() throws Exception {
    final Path path = ddfHome.resolve("invalid-directory");

    path.toFile().createNewFile(); // create it as a file

    configurationMigrationManager = getConfigurationMigrationManager();

    MigrationReport report = configurationMigrationManager.doExport(path);

    reportHasErrorMessage(
        report.errors(),
        Matchers.matchesPattern(
            "Unexpected error: unable to create directory \\["
                + path.toString().replace("\\", "\\\\")
                + "\\]; .*\\."));
  }

  @Test
  public void doExportRecordsErrorForMigrationException() throws Exception {
    configurationMigrationManager = spy(getConfigurationMigrationManager());

    doThrow(new MigrationException(TEST_MESSAGE))
        .when(configurationMigrationManager)
        .delegateToExportMigrationManager(any(MigrationReportImpl.class), any(Path.class));

    MigrationReport report =
        configurationMigrationManager.doExport(ddfHome.resolve(TEST_DIRECTORY));

    reportHasErrorMessage(report.errors(), equalTo(TEST_MESSAGE));
    verify(configurationMigrationManager)
        .delegateToExportMigrationManager(any(MigrationReportImpl.class), any(Path.class));
  }

  @Test
  public void doExportRecordsErrorForIOException() throws Exception {
    final Path path = ddfHome.resolve(TEST_DIRECTORY);

    configurationMigrationManager = spy(getConfigurationMigrationManager());

    doThrow(new IOException("testing"))
        .when(configurationMigrationManager)
        .delegateToExportMigrationManager(any(MigrationReportImpl.class), any(Path.class));

    MigrationReport report = configurationMigrationManager.doExport(path);

    reportHasErrorMessage(
        report.errors(),
        equalTo(
            "Export error: failed to close export file ["
                + path.resolve("exported")
                + "-"
                + TEST_VERSION
                + ".zip]; testing."));
    verify(configurationMigrationManager)
        .delegateToExportMigrationManager(any(MigrationReportImpl.class), any(Path.class));
  }

  @Test
  public void doExportRecordsErrorForRuntimeException() throws Exception {
    final Path path = ddfHome.resolve(TEST_DIRECTORY);

    configurationMigrationManager = spy(getConfigurationMigrationManager());

    doThrow(new RuntimeException("testing."))
        .when(configurationMigrationManager)
        .delegateToExportMigrationManager(any(MigrationReportImpl.class), any(Path.class));

    MigrationReport report = configurationMigrationManager.doExport(path);

    reportHasErrorMessage(
        report.errors(),
        equalTo(
            "Unexpected internal error: failed to export to file ["
                + path.resolve("exported")
                + "-"
                + TEST_VERSION
                + ".zip]; testing."));
    verify(configurationMigrationManager)
        .delegateToExportMigrationManager(any(MigrationReportImpl.class), any(Path.class));
  }

  @Test
  public void doImportWithStringSucceedsWithWarnings() throws Exception {
    configurationMigrationManager = spy(getConfigurationMigrationManager());
    MigrationReport mockReport = mock(MigrationReport.class);

    when(mockReport.warnings()).thenReturn(Stream.of(new MigrationWarning(TEST_MESSAGE)));
    doReturn(mockReport).when(configurationMigrationManager).doImport(any(Path.class));

    Collection<MigrationWarning> warnings = configurationMigrationManager.doImport(TEST_DIRECTORY);

    reportHasWarningMessage(warnings.stream(), equalTo(TEST_MESSAGE));
    verify(configurationMigrationManager).doImport(any(Path.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void doImportWithNullString() throws Exception {
    configurationMigrationManager = getConfigurationMigrationManager();

    configurationMigrationManager.doImport((String) null);
  }

  @Test(expected = MigrationException.class)
  public void doImportWithStringThrowsException() throws Exception {
    configurationMigrationManager = spy(getConfigurationMigrationManager());
    MigrationReport mockReport = mock(MigrationReport.class);

    doThrow(MigrationException.class).when(mockReport).verifyCompletion();
    doReturn(mockReport).when(configurationMigrationManager).doImport(any(Path.class));

    configurationMigrationManager.doImport(TEST_DIRECTORY);
  }

  @Test
  public void doImportSucceeds() throws Exception {
    final Path path = ddfHome.resolve(TEST_DIRECTORY);

    configurationMigrationManager = spy(getConfigurationMigrationManager());

    expectImportDelegationIsSuccessful();

    MigrationReport report = configurationMigrationManager.doImport(path);

    assertThat("Import was not successful", report.wasSuccessful(), is(true));
    assertThat(
        "Restart system property was not set",
        System.getProperty("karaf.restart.jvm"),
        equalTo("true"));
    reportHasInfoMessage(
        report.infos(),
        equalTo(
            "Successfully imported from file ["
                + path.resolve("exported")
                + "-"
                + TEST_VERSION
                + ".zip]."));
    reportHasInfoMessage(
        report.infos(),
        equalTo("Restarting the system in 1 minute(s) for changes to take effect."));
    verify(mockSystemService).reboot(any(String.class), any(SystemService.Swipe.class));
    verify(configurationMigrationManager)
        .delegateToImportMigrationManager(any(MigrationReportImpl.class), any(Path.class));
  }

  @Test
  public void doImportSucceedsWithConsumer() throws Exception {
    final Path path = ddfHome.resolve(TEST_DIRECTORY);
    final Consumer<MigrationMessage> consumer = mock(Consumer.class);

    configurationMigrationManager = spy(getConfigurationMigrationManager());

    expectImportDelegationIsSuccessful();

    MigrationReport report = configurationMigrationManager.doImport(path, consumer);

    assertThat("Import was not successful", report.wasSuccessful(), is(true));
    assertThat(
        "Restart system property was not set",
        System.getProperty("karaf.restart.jvm"),
        equalTo("true"));
    reportHasInfoMessage(
        report.infos(),
        equalTo(
            "Successfully imported from file ["
                + path.resolve("exported")
                + "-"
                + TEST_VERSION
                + ".zip]."));
    reportHasInfoMessage(
        report.infos(),
        equalTo("Restarting the system in 1 minute(s) for changes to take effect."));
    verify(mockSystemService).reboot(any(String.class), any(SystemService.Swipe.class));
    verify(configurationMigrationManager)
        .delegateToImportMigrationManager(any(MigrationReportImpl.class), any(Path.class));
    verify(consumer, Mockito.atLeastOnce()).accept(Mockito.notNull());
  }

  @Test
  public void doImportSucceedsWithWarnings() throws Exception {
    final Path path = ddfHome.resolve(TEST_DIRECTORY);
    configurationMigrationManager = spy(getConfigurationMigrationManager());

    doAnswer(
            invocation -> {
              MigrationReport report = invocation.getArgument(0);
              report.record(new MigrationWarning(TEST_MESSAGE));
              return null;
            })
        .when(configurationMigrationManager)
        .delegateToImportMigrationManager(any(MigrationReportImpl.class), any(Path.class));

    MigrationReport report = configurationMigrationManager.doImport(path);

    assertThat("Import was not successful", report.wasSuccessful(), is(true));
    reportHasWarningMessage(report.warnings(), equalTo(TEST_MESSAGE));
    reportHasWarningMessage(
        report.warnings(),
        equalTo(
            "Successfully imported from file ["
                + path.resolve("exported")
                + "-"
                + TEST_VERSION
                + ".zip] with warnings; make sure to review."));
    verify(configurationMigrationManager)
        .delegateToImportMigrationManager(any(MigrationReportImpl.class), any(Path.class));
  }

  @Test
  public void doImportSucceedsAndFailsToReboot() throws Exception {
    final Path path = ddfHome.resolve(TEST_DIRECTORY);
    configurationMigrationManager = spy(getConfigurationMigrationManager());

    expectImportDelegationIsSuccessful();
    doThrow(Exception.class)
        .when(mockSystemService)
        .reboot(any(String.class), any(SystemService.Swipe.class));

    MigrationReport report = configurationMigrationManager.doImport(path);

    assertThat("Import was successful", report.wasSuccessful(), is(true));
    assertThat(
        "Restart system property was set",
        System.getProperty("karaf.restart.jvm"),
        equalTo("true"));
    reportHasInfoMessage(
        report.infos(),
        equalTo(
            "Successfully imported from file ["
                + path.resolve("exported")
                + "-"
                + TEST_VERSION
                + ".zip]."));
    reportHasInfoMessage(
        report.infos(), equalTo("Please restart the system for changes to take effect."));
    verify(mockSystemService).reboot(any(String.class), any(SystemService.Swipe.class));
    verify(configurationMigrationManager)
        .delegateToImportMigrationManager(any(MigrationReportImpl.class), any(Path.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void doImportWithNullPath() throws Exception {
    configurationMigrationManager = getConfigurationMigrationManager();

    configurationMigrationManager.doImport((Path) null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void doImportWithNullConsumer() throws Exception {
    configurationMigrationManager = getConfigurationMigrationManager();

    configurationMigrationManager.doImport(ddfHome.resolve(TEST_DIRECTORY), null);
  }

  @Test
  public void doImportRecordsErrorForMigrationException() throws Exception {
    configurationMigrationManager = spy(getConfigurationMigrationManager());

    expectExportDelegationIsSuccessful();
    doThrow(new MigrationException(TEST_MESSAGE))
        .when(configurationMigrationManager)
        .delegateToImportMigrationManager(any(MigrationReportImpl.class), any(Path.class));

    MigrationReport report =
        configurationMigrationManager.doImport(ddfHome.resolve(TEST_DIRECTORY));

    reportHasErrorMessage(report.errors(), equalTo(TEST_MESSAGE));
    verify(configurationMigrationManager)
        .delegateToImportMigrationManager(any(MigrationReportImpl.class), any(Path.class));
    verifyZeroInteractions(mockSystemService);
  }

  @Test
  public void doImportRecordsErrorForRuntimeException() throws Exception {
    final Path path = ddfHome.resolve(TEST_DIRECTORY);

    configurationMigrationManager = spy(getConfigurationMigrationManager());

    expectExportDelegationIsSuccessful();
    doThrow(new RuntimeException("testing"))
        .when(configurationMigrationManager)
        .delegateToImportMigrationManager(any(MigrationReportImpl.class), any(Path.class));

    MigrationReport report = configurationMigrationManager.doImport(path);

    reportHasErrorMessage(
        report.errors(),
        equalTo(
            "Unexpected internal error: failed to import from file ["
                + path.resolve("exported")
                + "-"
                + TEST_VERSION
                + ".zip]; testing."));
    verify(configurationMigrationManager)
        .delegateToImportMigrationManager(any(MigrationReportImpl.class), any(Path.class));
    verifyZeroInteractions(mockSystemService);
  }

  private void expectExportDelegationIsSuccessful() throws IOException {
    doNothing()
        .when(configurationMigrationManager)
        .delegateToExportMigrationManager(any(MigrationReportImpl.class), any(Path.class));
  }

  private void expectImportDelegationIsSuccessful() {
    doNothing()
        .when(configurationMigrationManager)
        .delegateToImportMigrationManager(any(MigrationReportImpl.class), any(Path.class));
  }

  private void reportHasInfoMessage(Stream<MigrationInformation> infos, Matcher<String> matcher) {
    final List<MigrationInformation> is = infos.collect(Collectors.toList());
    final long count = is.stream().filter((w) -> matcher.matches(w.getMessage())).count();
    final Description d = new StringDescription();

    matcher.describeTo(d);
    assertThat(
        "There are "
            + count
            + " matching info message(s) with "
            + d
            + " in the migration report.\nWarnings are: "
            + is.stream()
                .map(MigrationInformation::getMessage)
                .collect(Collectors.joining("\",\n\t\"", "[\n\t\"", "\"\n]")),
        count,
        equalTo(1L));
  }

  private void reportHasWarningMessage(Stream<MigrationWarning> warnings, Matcher<String> matcher) {
    final List<MigrationWarning> ws = warnings.collect(Collectors.toList());
    final long count = ws.stream().filter((w) -> matcher.matches(w.getMessage())).count();
    final Description d = new StringDescription();

    matcher.describeTo(d);
    assertThat(
        "There are "
            + count
            + " matching warning(s) with "
            + d
            + " in the migration report.\nWarnings are: "
            + ws.stream()
                .map(MigrationWarning::getMessage)
                .collect(Collectors.joining("\",\n\t\"", "[\n\t\"", "\"\n]")),
        count,
        equalTo(1L));
  }

  private void reportHasErrorMessage(Stream<MigrationException> errors, Matcher<String> matcher) {
    final List<MigrationException> es = errors.collect(Collectors.toList());
    final long count = es.stream().filter((e) -> matcher.matches(e.getMessage())).count();
    final Description d = new StringDescription();

    matcher.describeTo(d);

    assertThat(
        "There are "
            + count
            + " matching error(s) with "
            + d
            + "in the migration report.\nErrors are: "
            + es.stream()
                .map(MigrationException::getMessage)
                .collect(Collectors.joining("\",\n\t\"", "[\n\t\"", "\"\n]")),
        count,
        equalTo(1L));
  }

  private ConfigurationMigrationManager getConfigurationMigrationManager() throws IOException {
    File versionFile = new File(ddfHome.resolve("Version.txt").toString());
    versionFile.createNewFile();
    Files.write(versionFile.toPath(), TEST_VERSION.getBytes(), StandardOpenOption.APPEND);

    return new ConfigurationMigrationManager(mockMBeanServer, migratables, mockSystemService);
  }
}
