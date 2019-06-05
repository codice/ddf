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

import com.github.npathai.hamcrestopt.OptionalMatchers;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipOutputStream;
import javax.crypto.CipherOutputStream;
import org.apache.commons.io.FilenameUtils;
import org.codice.ddf.migration.ExportMigrationEntry;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationOperation;
import org.codice.ddf.migration.MigrationReport;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ExportMigrationContextImplTest extends AbstractMigrationSupport {

  private static final String PROPERTY_NAME = "test.property";

  private static final String PROPERTY_NAME2 = "test.property2";

  private static final String[] MIGRATABLE_NAME_DIRS = new String[] {"where", "some", "dir"};

  private static final String MIGRATABLE_NAME = "where/some/dir/test.txt";

  private static final String MIGRATABLE_PROPERTY_PATHNAME =
      Paths.get("..", "ddf", "where", "some", "dir", "test.txt").toString();

  private static final Path MIGRATABLE_PATH =
      Paths.get(FilenameUtils.separatorsToSystem(MIGRATABLE_NAME));

  private static final ExportMigrationEntryImpl ENTRY =
      Mockito.mock(ExportMigrationEntryImpl.class);

  private final MigrationReport report =
      new MigrationReportImpl(MigrationOperation.EXPORT, Optional.empty());

  private final ByteArrayOutputStream baos = new ByteArrayOutputStream();

  private final ZipOutputStream zos = new ZipOutputStream(baos);

  private CipherUtils mockCipherUtils;

  private ExportMigrationContextImpl context;

  @Before
  public void before() throws Exception {
    initMigratableMock();

    Mockito.when(ENTRY.getName()).thenReturn(MIGRATABLE_NAME);

    mockCipherUtils = Mockito.mock(CipherUtils.class);
    CipherOutputStream cos = Mockito.mock(CipherOutputStream.class);
    Mockito.when(mockCipherUtils.getCipherOutputStream(Mockito.any(OutputStream.class)))
        .thenReturn(cos);
    this.context = new ExportMigrationContextImpl(report, migratable, zos, mockCipherUtils);

    System.setProperty(PROPERTY_NAME, MIGRATABLE_PROPERTY_PATHNAME);
  }

  @After
  public void after() throws Exception {
    zos.close();
    System.getProperties().remove(PROPERTY_NAME);
    System.getProperties().remove(PROPERTY_NAME2);
  }

  @Test
  public void testConstructor() throws Exception {
    Assert.assertThat(
        context.getReport().getOperation(), Matchers.equalTo(MigrationOperation.EXPORT));
    Assert.assertThat(context.getId(), Matchers.equalTo(MIGRATABLE_ID));
    Assert.assertThat(context.getMigratableVersion(), OptionalMatchers.hasValue(VERSION));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorWithNullReport() throws Exception {
    new ExportMigrationContextImpl(null, migratable, zos, mockCipherUtils);
  }

  @Test
  public void testConstructorWithNullMigratable() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null migratable"));

    new ExportMigrationContextImpl(report, null, zos, mockCipherUtils);
  }

  @Test
  public void testConstructorWithNullZipOutputStream() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null zip output stream"));

    new ExportMigrationContextImpl(report, migratable, null, mockCipherUtils);
  }

  @Test
  public void testConstructorWithNullCipherUtils() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null cipher utils"));

    new ExportMigrationContextImpl(report, migratable, zos, null);
  }

  @Test
  public void testGetSystemPropertyReferencedEntryWhenValueIsRelative() throws Exception {
    createDirectory(MIGRATABLE_NAME_DIRS);
    createFile(MIGRATABLE_NAME);

    final Optional<ExportMigrationEntry> oentry =
        context.getSystemPropertyReferencedEntry(PROPERTY_NAME, (r, v) -> true);

    Assert.assertThat(oentry, OptionalMatchers.isPresent());
    final ExportMigrationEntry entry = oentry.get();

    Assert.assertThat(entry.getId(), Matchers.equalTo(MIGRATABLE_ID));
    Assert.assertThat(entry.getName(), Matchers.equalTo(MIGRATABLE_NAME));
    Assert.assertThat(entry.getPath(), Matchers.equalTo(MIGRATABLE_PATH));
    // now check that it is a system property referenced entry that references the proper property
    // name
    Assert.assertThat(
        entry, Matchers.instanceOf(ExportMigrationSystemPropertyReferencedEntryImpl.class));
    final ExportMigrationSystemPropertyReferencedEntryImpl sentry =
        (ExportMigrationSystemPropertyReferencedEntryImpl) entry;

    Assert.assertThat(sentry.getProperty(), Matchers.equalTo(PROPERTY_NAME));
    // finally make sure no warnings or errors were recorded
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
  }

  @Test
  public void testGetSystemPropertyReferencedEntryWhenValueIsAbsoluteUnderDDFHome()
      throws Exception {
    System.setProperty(PROPERTY_NAME, ddfHome.resolve(MIGRATABLE_PATH).toAbsolutePath().toString());

    createDirectory(MIGRATABLE_NAME_DIRS);
    createFile(MIGRATABLE_NAME);

    final Optional<ExportMigrationEntry> oentry =
        context.getSystemPropertyReferencedEntry(PROPERTY_NAME, (r, v) -> true);

    Assert.assertThat(oentry, OptionalMatchers.isPresent());
    final ExportMigrationEntry entry = oentry.get();

    Assert.assertThat(entry.getId(), Matchers.equalTo(MIGRATABLE_ID));
    Assert.assertThat(entry.getName(), Matchers.equalTo(MIGRATABLE_NAME));
    Assert.assertThat(entry.getPath(), Matchers.equalTo(MIGRATABLE_PATH));
    // now check that it is a system property referenced entry that references the proper property
    // name
    Assert.assertThat(
        entry, Matchers.instanceOf(ExportMigrationSystemPropertyReferencedEntryImpl.class));
    final ExportMigrationSystemPropertyReferencedEntryImpl sentry =
        (ExportMigrationSystemPropertyReferencedEntryImpl) entry;

    Assert.assertThat(sentry.getProperty(), Matchers.equalTo(PROPERTY_NAME));
    // finally make sure no warnings or errors were recorded
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
  }

  @Test
  public void testGetSystemPropertyReferencedEntryWhenValueIsAbsoluteNotUnderDDFHome()
      throws Exception {
    final Path migratablePath =
        testFolder.newFile("test.cfg").toPath().toRealPath(LinkOption.NOFOLLOW_LINKS);
    final String migratableName = FilenameUtils.separatorsToUnix(migratablePath.toString());

    System.setProperty(PROPERTY_NAME, migratablePath.toAbsolutePath().toString());

    final Optional<ExportMigrationEntry> oentry =
        context.getSystemPropertyReferencedEntry(PROPERTY_NAME, (r, v) -> true);

    Assert.assertThat(oentry, OptionalMatchers.isPresent());
    final ExportMigrationEntry entry = oentry.get();

    Assert.assertThat(entry.getId(), Matchers.equalTo(MIGRATABLE_ID));
    Assert.assertThat(entry.getName(), Matchers.equalTo(migratableName));
    Assert.assertThat(entry.getPath(), Matchers.equalTo(migratablePath));
    // now check that it is a system property referenced entry that references the proper property
    // name
    Assert.assertThat(
        entry, Matchers.instanceOf(ExportMigrationSystemPropertyReferencedEntryImpl.class));
    final ExportMigrationSystemPropertyReferencedEntryImpl sentry =
        (ExportMigrationSystemPropertyReferencedEntryImpl) entry;

    Assert.assertThat(sentry.getProperty(), Matchers.equalTo(PROPERTY_NAME));
    // finally make sure no warnings or errors were recorded
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
  }

  @Test
  public void testGetSystemPropertyReferencedEntryWithNullName() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null system property name"));

    context.getSystemPropertyReferencedEntry(null, (r, v) -> true);
  }

  @Test
  public void testGetSystemPropertyReferencedEntryWithNullValidator() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null validator"));

    context.getSystemPropertyReferencedEntry(PROPERTY_NAME, null);
  }

  @Test
  public void testGetSystemPropertyReferencedEntryWhenAlreadyCached() throws Exception {
    final ExportMigrationEntry systemPropEntry =
        context.getSystemPropertyReferencedEntry(PROPERTY_NAME, (r, v) -> true).get();

    final Optional<ExportMigrationEntry> oentry =
        context.getSystemPropertyReferencedEntry(PROPERTY_NAME, (r, v) -> true);

    Assert.assertThat(oentry, OptionalMatchers.isPresent());
    final ExportMigrationEntry entry = oentry.get();

    Assert.assertThat(entry, Matchers.sameInstance(systemPropEntry));
    // finally make sure no warnings or errors were recorded
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
  }

  @Test
  public void testGetSystemPropertyReferencedEntryWhenInvalid() throws Exception {
    final Optional<ExportMigrationEntry> oentry =
        context.getSystemPropertyReferencedEntry(PROPERTY_NAME, (r, v) -> false);

    Assert.assertThat(oentry, OptionalMatchers.isEmpty());
  }

  @Test
  public void testGetSystemPropertyReferencedEntryWhenPropertyIsNotDefined() throws Exception {
    final Optional<ExportMigrationEntry> oentry =
        context.getSystemPropertyReferencedEntry(PROPERTY_NAME2, (r, v) -> true);

    Assert.assertThat(oentry, OptionalMatchers.isEmpty());

    // finally make sure we got an error (register the thrown expectations after the above to make
    // sure
    // we don't get an exception from the above code under test
    thrown.expect(MigrationException.class);
    thrown.expectMessage(
        Matchers.containsString("system property [" + PROPERTY_NAME2 + "] is not defined"));

    report.verifyCompletion(); // to get the exception thrown out
  }

  @Test
  public void testGetSystemPropertyReferencedEntryWhenPropertyValueIsEmpty() throws Exception {
    System.setProperty(PROPERTY_NAME2, "");

    final Optional<ExportMigrationEntry> oentry =
        context.getSystemPropertyReferencedEntry(PROPERTY_NAME2, (r, v) -> true);

    Assert.assertThat(oentry, OptionalMatchers.isEmpty());

    // finally make sure we got an error (register the thrown expectations after the above to make
    // sure
    // we don't get an exception from the above code under test
    thrown.expect(MigrationException.class);
    thrown.expectMessage(
        Matchers.containsString("system property [" + PROPERTY_NAME2 + "] is empty"));

    report.verifyCompletion(); // to get the exception thrown out
  }

  @Test
  public void testGetEntryWithRelativePath() throws Exception {
    final ExportMigrationEntry entry = context.getEntry(MIGRATABLE_PATH);

    Assert.assertThat(entry.getId(), Matchers.equalTo(MIGRATABLE_ID));
    Assert.assertThat(entry.getName(), Matchers.equalTo(MIGRATABLE_NAME));
    Assert.assertThat(entry.getPath(), Matchers.equalTo(MIGRATABLE_PATH));
    // now check that it is a standard export entry
    Assert.assertThat(entry, Matchers.instanceOf(ExportMigrationEntryImpl.class));
    // finally make sure no warnings or errors were recorded
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
  }

  @Test
  public void testGetEntryWithAbsolutePathUnderDDFHome() throws Exception {
    final ExportMigrationEntry entry =
        context.getEntry(ddfHome.resolve(MIGRATABLE_PATH).toAbsolutePath());

    Assert.assertThat(entry.getId(), Matchers.equalTo(MIGRATABLE_ID));
    Assert.assertThat(entry.getName(), Matchers.equalTo(MIGRATABLE_NAME));
    Assert.assertThat(entry.getPath(), Matchers.equalTo(MIGRATABLE_PATH));
    // now check that it is a standard export entry
    Assert.assertThat(entry, Matchers.instanceOf(ExportMigrationEntryImpl.class));
    // finally make sure no warnings or errors were recorded
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
  }

  @Test
  public void testGetEntryWithAbsolutePathNotUnderDDFHome() throws Exception {
    final Path migratablePath = Paths.get(MIGRATABLE_NAME).toAbsolutePath();
    final String migratableName = FilenameUtils.separatorsToUnix(migratablePath.toString());

    final ExportMigrationEntry entry = context.getEntry(migratablePath);

    Assert.assertThat(entry.getId(), Matchers.equalTo(MIGRATABLE_ID));
    Assert.assertThat(entry.getName(), Matchers.equalTo(migratableName));
    Assert.assertThat(entry.getPath(), Matchers.equalTo(migratablePath));
    // now check that it is a standard export entry
    Assert.assertThat(entry, Matchers.instanceOf(ExportMigrationEntryImpl.class));
    // finally make sure no warnings or errors were recorded
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
  }

  @Test
  public void testGetEntryWithRelativePathAlreadyExist() throws Exception {
    final ExportMigrationEntry firstEntry = context.getEntry(MIGRATABLE_PATH);

    final ExportMigrationEntry entry = context.getEntry(MIGRATABLE_PATH);

    Assert.assertThat(entry, Matchers.sameInstance(firstEntry));
    // finally make sure no warnings or errors were recorded
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
  }

  @Test
  public void testGetEntryWithAbsolutePathUnderDDFHomePathAlreadyExist() throws Exception {
    final ExportMigrationEntry firstEntry = context.getEntry(MIGRATABLE_PATH);

    final ExportMigrationEntry entry =
        context.getEntry(ddfHome.resolve(MIGRATABLE_PATH).toAbsolutePath());

    Assert.assertThat(entry, Matchers.sameInstance(firstEntry));
    // finally make sure no warnings or errors were recorded
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
  }

  @Test
  public void testGetEntryWithAbsolutePathNotUnderDDFHomeAlreadyExist() throws Exception {
    final Path migratablePath = Paths.get(MIGRATABLE_NAME).toAbsolutePath();
    final ExportMigrationEntry firstEntry = context.getEntry(migratablePath);

    final ExportMigrationEntry entry = context.getEntry(migratablePath);

    Assert.assertThat(entry, Matchers.sameInstance(firstEntry));
    // finally make sure no warnings or errors were recorded
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
  }

  @Test
  public void testGetEntryWithNullPath() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null path"));

    context.getEntry(null);
  }

  @Test
  public void testEntriesWhenRecursingWithRelativePathAndSubDirectories() throws Exception {
    final Path etc = createDirectory("etc");
    final Path ks = createDirectory("etc", "keystores");
    final Path other = createDirectory("other");
    final List<Path> paths = createFiles(etc, "test.cfg", "test2.config", "test3.properties");

    createFiles(paths, ks, "serverKeystore.jks", "serverTruststore.jks");
    createFiles(other, "a", "b");
    createDirectory("etc", "keystores", "empty");

    final List<ExportMigrationEntry> entries = context.entries(etc).collect(Collectors.toList());

    Assert.assertThat(
        entries.stream().map(ExportMigrationEntry::getPath).collect(Collectors.toList()),
        Matchers.containsInAnyOrder(paths.toArray()));
    // finally make sure no warnings or errors were recorded
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
  }

  @Test
  public void testEntriesWhenNotRecursingWithRelativePathAndSubDirectories() throws Exception {
    final Path etc = createDirectory("etc");
    final Path ks = createDirectory("etc", "keystores");
    final Path other = createDirectory("other");
    final List<Path> paths = createFiles(etc, "test.cfg", "test2.config", "test3.properties");

    createFiles(ks, "serverKeystore.jks", "serverTruststore.jks");
    createFiles(other, "a", "b");
    createDirectory("etc", "keystores", "empty");

    final List<ExportMigrationEntry> entries =
        context.entries(etc, false).collect(Collectors.toList());

    Assert.assertThat(
        entries.stream().map(ExportMigrationEntry::getPath).collect(Collectors.toList()),
        Matchers.containsInAnyOrder(paths.toArray()));
    // finally make sure no warnings or errors were recorded
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
  }

  @Test
  public void testEntriesWithAbsolutePathUnderDDFHome() throws Exception {
    final Path etc = createDirectory("etc");
    final Path ks = createDirectory("etc", "keystores");
    final Path other = createDirectory("other");
    final List<Path> paths = createFiles(etc, "test.cfg", "test2.config", "test3.properties");

    createFiles(paths, ks, "serverKeystore.jks", "serverTruststore.jks");
    createFiles(other, "a", "b");
    createDirectory("etc", "keystores", "empty");

    final List<ExportMigrationEntry> entries =
        context.entries(etc.toAbsolutePath()).collect(Collectors.toList());

    Assert.assertThat(
        entries.stream().map(ExportMigrationEntry::getPath).collect(Collectors.toList()),
        Matchers.containsInAnyOrder(paths.toArray()));
    // finally make sure no warnings or errors were recorded
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
  }

  @Test
  public void testEntriesWhenPathDoesNotExist() throws Exception {
    final Path notFound = ddfHome.resolve("not-found");

    final Stream<ExportMigrationEntry> entries = context.entries(notFound);

    Assert.assertThat(entries.count(), Matchers.equalTo(0L));
    // verify we got an error and no warnings
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(true));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));

    // finally make sure we got an error (register the thrown expectations after the above to make
    // sure
    // we don't get an exception from the above code under test
    thrown.expect(MigrationException.class);
    thrown.expectMessage(Matchers.containsString("[not-found]"));
    thrown.expectMessage(Matchers.containsString("does not exist"));

    report.verifyCompletion(); // to get the exception thrown out
  }

  @Test
  public void testEntriesWhenPathIsNotADirectory() throws Exception {
    final Path etc = createDirectory("etc");
    final Path notADir = createFile(etc, "not-a-dir");

    final Stream<ExportMigrationEntry> entries = context.entries(notADir);

    Assert.assertThat(entries.count(), Matchers.equalTo(0L));
    // verify we got an error and no warnings
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(true));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));

    // finally make sure we got an error (register the thrown expectations after the above to make
    // sure
    // we don't get an exception from the above code under test
    thrown.expect(MigrationException.class);
    thrown.expectMessage(Matchers.containsString("[" + notADir + "]"));
    thrown.expectMessage(Matchers.containsString("is not a directory"));

    report.verifyCompletion(); // to get the exception thrown out
  }

  @Test
  public void testEntriesWhenSomeAlreadyExist() throws Exception {
    final Path etc = createDirectory("etc");
    final Path ks = createDirectory("etc", "keystores");
    final Path other = createDirectory("other");
    final List<Path> paths = createFiles(etc, "test.cfg", "test2.config", "test3.properties");

    createFiles(paths, ks, "serverKeystore.jks", "serverTruststore.jks");
    createFiles(other, "a", "b");
    createDirectory("etc", "keystores", "empty");
    final ExportMigrationEntry entry = context.getEntry(etc.resolve("test.cfg"));

    final List<ExportMigrationEntry> entries = context.entries(etc).collect(Collectors.toList());

    Assert.assertThat(
        entries.stream().map(ExportMigrationEntry::getPath).collect(Collectors.toList()),
        Matchers.containsInAnyOrder(paths.toArray()));
    Assert.assertThat(entries, Matchers.hasItem(Matchers.sameInstance(entry)));
    // finally make sure no warnings or errors were recorded
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
  }

  @Test
  public void testEntriesWithNullPath() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null path"));

    context.entries(null);
  }

  @Test
  public void testEntriesWhenRecursingWithFilterAndRelativePath() throws Exception {
    final Path etc = createDirectory("etc");
    final Path ks = createDirectory("etc", "keystores");
    final Path other = createDirectory("other");
    final List<Path> paths = createFiles(etc, "test.cfg", "test2.config", "test3.properties");

    createFiles(paths, ks, "testServerKeystore.jks");
    createFiles(ks, "serverKeystore.jks", "serverTruststore.jks");
    createFiles(other, "a", "b");
    createDirectory("etc", "keystores", "empty");

    final List<ExportMigrationEntry> entries =
        context
            .entries(etc, p -> p.getFileName().toString().startsWith("test"))
            .collect(Collectors.toList());

    Assert.assertThat(
        entries.stream().map(ExportMigrationEntry::getPath).collect(Collectors.toList()),
        Matchers.containsInAnyOrder(paths.toArray()));
    // finally make sure no warnings or errors were recorded
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
  }

  @Test
  public void testEntriesWhenNotRecursingWithFilterAndRelativePath() throws Exception {
    final Path etc = createDirectory("etc");
    final Path ks = createDirectory("etc", "keystores");
    final Path other = createDirectory("other");
    final List<Path> paths = createFiles(etc, "test.cfg", "test2.config", "test3.properties");

    createFiles(etc, "another.cfg", "not-exported.properties");
    createFiles(ks, "testServerKeystore.jks");
    createFiles(ks, "serverKeystore.jks", "serverTruststore.jks");
    createFiles(other, "a", "b");
    createDirectory("etc", "keystores", "empty");

    final List<ExportMigrationEntry> entries =
        context
            .entries(etc, false, p -> p.getFileName().toString().startsWith("test"))
            .collect(Collectors.toList());

    Assert.assertThat(
        entries.stream().map(ExportMigrationEntry::getPath).collect(Collectors.toList()),
        Matchers.containsInAnyOrder(paths.toArray()));
    // finally make sure no warnings or errors were recorded
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
  }

  @Test
  public void testEntriesWithFilterAndAbsolutePathUnderDDFHome() throws Exception {
    final Path etc = createDirectory("etc");
    final Path ks = createDirectory("etc", "keystores");
    final Path other = createDirectory("other");
    final List<Path> paths = createFiles(etc, "test.cfg", "test2.config", "test3.properties");

    createFiles(ks, "serverKeystore.jks", "serverTruststore.jks");
    createFiles(other, "a", "b");
    createDirectory("etc", "keystores", "empty");

    final List<ExportMigrationEntry> entries =
        context
            .entries(etc.toAbsolutePath(), p -> p.getFileName().toString().startsWith("test"))
            .collect(Collectors.toList());

    Assert.assertThat(
        entries.stream().map(ExportMigrationEntry::getPath).collect(Collectors.toList()),
        Matchers.containsInAnyOrder(paths.toArray()));
    // finally make sure no warnings or errors were recorded
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
  }

  @Test
  public void testEntriesWithFilterWhenPathDoesNotExist() throws Exception {
    final Path notFound = ddfHome.resolve("not-found");

    final Stream<ExportMigrationEntry> entries =
        context.entries(notFound, p -> p.getFileName().toString().startsWith("test"));

    Assert.assertThat(entries.count(), Matchers.equalTo(0L));
    // verify we got an error and no warnings
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(true));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));

    // finally make sure we got an error (register the thrown expectations after the above to make
    // sure
    // we don't get an exception from the above code under test
    thrown.expect(MigrationException.class);
    thrown.expectMessage(Matchers.containsString("[not-found]"));
    thrown.expectMessage(Matchers.containsString("does not exist"));

    report.verifyCompletion(); // to get the exception thrown out
  }

  @Test
  public void testEntriesWithFilterWhenPathIsNotADirectory() throws Exception {
    final Path etc = createDirectory("etc");
    final Path notADir = createFile(etc, "not-a-dir");

    final Stream<ExportMigrationEntry> entries =
        context.entries(notADir, p -> p.getFileName().toString().startsWith("test"));

    Assert.assertThat(entries.count(), Matchers.equalTo(0L));
    // verify we got an error and no warnings
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(true));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));

    // finally make sure we got an error (register the thrown expectations after the above to make
    // sure
    // we don't get an exception from the above code under test
    thrown.expect(MigrationException.class);
    thrown.expectMessage(Matchers.containsString("[" + notADir + "]"));
    thrown.expectMessage(Matchers.containsString("is not a directory"));

    report.verifyCompletion(); // to get the exception thrown out
  }

  @Test
  public void testEntriesWithFilterWhenSomeAlreadyExist() throws Exception {
    final Path etc = createDirectory("etc");
    final Path ks = createDirectory("etc", "keystores");
    final Path other = createDirectory("other");
    final List<Path> paths = createFiles(etc, "test.cfg", "test2.config", "test3.properties");

    createFiles(ks, "serverKeystore.jks", "serverTruststore.jks");
    createFiles(other, "a", "b");
    createDirectory("etc", "keystores", "empty");
    final ExportMigrationEntry entry = context.getEntry(etc.resolve("test.cfg"));

    final List<ExportMigrationEntry> entries =
        context
            .entries(etc, p -> p.getFileName().toString().startsWith("test"))
            .collect(Collectors.toList());

    Assert.assertThat(
        entries.stream().map(ExportMigrationEntry::getPath).collect(Collectors.toList()),
        Matchers.containsInAnyOrder(paths.toArray()));
    Assert.assertThat(entries, Matchers.hasItem(Matchers.sameInstance(entry)));
    // finally make sure no warnings or errors were recorded
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
  }

  @Test
  public void testEntriesWithFilterAndNullPath() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null path"));

    context.entries(null, p -> p.getFileName().toString().startsWith("test"));
  }

  @Test
  public void testEntriesWithNullFilter() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null path filter"));

    final Path etc = createDirectory("etc");

    context.entries(etc, null);
  }

  @Test
  public void testClose() throws Exception {
    context.getOutputStreamFor(ENTRY);

    zos.close();

    final Map<String, MigrationZipEntry> zentries = AbstractMigrationSupport.getEntriesFrom(baos);

    Assert.assertThat(zentries.keySet(), Matchers.contains(MIGRATABLE_ID + '/' + MIGRATABLE_NAME));
  }

  @Test
  public void testCloseWithNoEntries() throws Exception {
    zos.close();

    final Map<String, MigrationZipEntry> zentries = AbstractMigrationSupport.getEntriesFrom(baos);

    Assert.assertThat(zentries, Matchers.aMapWithSize(0));
  }

  @Test
  public void testDoExport() throws Exception {
    Mockito.doNothing().when(migratable).doExport(Mockito.any());

    final Map<String, Map<String, Object>> metadata = context.doExport();

    Assert.assertThat(metadata.keySet(), Matchers.contains(MIGRATABLE_ID));
    final Map<String, Object> mmetadata = (Map<String, Object>) metadata.get(MIGRATABLE_ID);

    Assert.assertThat(mmetadata, Matchers.aMapWithSize(4));
    Assert.assertThat(mmetadata, Matchers.hasEntry(MigrationContextImpl.METADATA_VERSION, VERSION));
    Assert.assertThat(mmetadata, Matchers.hasEntry(MigrationContextImpl.METADATA_TITLE, TITLE));
    Assert.assertThat(
        mmetadata, Matchers.hasEntry(MigrationContextImpl.METADATA_DESCRIPTION, DESCRIPTION));
    Assert.assertThat(
        mmetadata, Matchers.hasEntry(MigrationContextImpl.METADATA_ORGANIZATION, ORGANIZATION));

    Mockito.verify(migratable).doExport(Mockito.same(context));
  }

  @Test
  public void testGetOutputStreamFor() throws Exception {
    final OutputStream out = context.getOutputStreamFor(ENTRY);

    zos.close();
    final Map<String, MigrationZipEntry> zentries = AbstractMigrationSupport.getEntriesFrom(baos);

    Assert.assertThat(out, Matchers.notNullValue());
    Assert.assertThat(zentries.keySet(), Matchers.contains(MIGRATABLE_ID + '/' + MIGRATABLE_NAME));
  }

  @Test
  public void testGetOutputStreamForWillClosePreviousEntry() throws Exception {
    final String migratableName2 = "etc/some/dir/test.txt2";
    final ExportMigrationEntryImpl entry2 = Mockito.mock(ExportMigrationEntryImpl.class);

    Mockito.when(entry2.getName()).thenReturn(migratableName2);

    final OutputStream out = context.getOutputStreamFor(ENTRY);
    final OutputStream out2 = context.getOutputStreamFor(entry2);

    zos.close();
    final Map<String, MigrationZipEntry> zentries = AbstractMigrationSupport.getEntriesFrom(baos);

    Assert.assertThat(out, Matchers.notNullValue());
    Assert.assertThat(out2, Matchers.notNullValue());
    Assert.assertThat(
        zentries.keySet(),
        Matchers.containsInAnyOrder(
            MIGRATABLE_ID + '/' + MIGRATABLE_NAME, MIGRATABLE_ID + '/' + migratableName2));
  }

  @Test(expected = UncheckedIOException.class)
  public void testGetOutputStreamForWhenAlreadyClosed() throws Exception {
    zos.close(); // to trigger IOException when trying to create a new entry

    context.getOutputStreamFor(ENTRY);
  }

  @Test
  public void testGetOutputStreamForWhenClosingReturnedStream() throws Exception {
    final OutputStream out = context.getOutputStreamFor(ENTRY);

    out.close();
    zos.close();
    final Map<String, MigrationZipEntry> zentries = AbstractMigrationSupport.getEntriesFrom(baos);

    Assert.assertThat(zentries.keySet(), Matchers.contains(MIGRATABLE_ID + '/' + MIGRATABLE_NAME));
  }

  @Test
  public void testGetOutputStreamForWhenDoubleClosingReturnedStream() throws Exception {
    final OutputStream out = context.getOutputStreamFor(ENTRY);

    out.close();
    out.close();
    zos.close();
    final Map<String, MigrationZipEntry> zentries = AbstractMigrationSupport.getEntriesFrom(baos);

    Assert.assertThat(zentries.keySet(), Matchers.contains(MIGRATABLE_ID + '/' + MIGRATABLE_NAME));
  }

  @Test
  public void testGetOutputStreamForWhenIOErrorsOccursFromReturnedStreamThrownAsCause()
      throws Exception {
    final OutputStream out = context.getOutputStreamFor(ENTRY);

    out.close();

    try {
      out.write(1);
    } catch (ExportIOException e) {
      Assert.assertThat(e.getIOException(), Matchers.instanceOf(IOException.class));
      Assert.assertThat(e.getIOException(), Matchers.sameInstance(e.getCause()));
    }
  }
}
