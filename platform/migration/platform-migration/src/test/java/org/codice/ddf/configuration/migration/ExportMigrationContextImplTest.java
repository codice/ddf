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
package org.codice.ddf.configuration.migration;

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

import org.apache.commons.io.FilenameUtils;
import org.codice.ddf.migration.ExportMigrationEntry;
import org.codice.ddf.migration.ExportPathMigrationException;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationOperation;
import org.codice.ddf.migration.MigrationReport;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.github.npathai.hamcrestopt.OptionalMatchers;

public class ExportMigrationContextImplTest extends AbstractMigrationTest {
    private static final String PROPERTY_NAME = "test.property";

    private static final String PROPERTY_NAME2 = "test.property2";

    private static final String[] MIGRATABLE_NAME_DIRS = new String[] {"where", "some", "dir"};

    private static final String MIGRATABLE_NAME = "where/some/dir/test.txt";

    private static final String MIGRATABLE_PROPERTY_PATHNAME = Paths.get("..",
            "ddf",
            "where",
            "some",
            "dir",
            "test.txt")
            .toString();

    private static final Path MIGRATABLE_PATH = Paths.get(FilenameUtils.separatorsToSystem(
            MIGRATABLE_NAME));

    private static final ExportMigrationEntryImpl ENTRY =
            Mockito.mock(ExportMigrationEntryImpl.class);

    private final MigrationReport REPORT = new MigrationReportImpl(MigrationOperation.EXPORT,
            Optional.empty());

    private final ByteArrayOutputStream BAOS = new ByteArrayOutputStream();

    private final ZipOutputStream ZOS = new ZipOutputStream(BAOS);

    private ExportMigrationContextImpl CONTEXT;

    @Before
    public void before() throws Exception {
        initMigratableMock();
        Mockito.when(ENTRY.getName())
                .thenReturn(MIGRATABLE_NAME);
        this.CONTEXT = new ExportMigrationContextImpl(REPORT, MIGRATABLE, ZOS);

        System.setProperty(PROPERTY_NAME, MIGRATABLE_PROPERTY_PATHNAME);
    }

    @After
    public void after() throws Exception {
        ZOS.close();
        System.getProperties()
                .remove(PROPERTY_NAME);
        System.getProperties()
                .remove(PROPERTY_NAME2);
    }

    @Test
    public void testConstructor() throws Exception {
        Assert.assertThat(CONTEXT.getReport()
                .getOperation(), Matchers.equalTo(MigrationOperation.EXPORT));
        Assert.assertThat(CONTEXT.getId(), Matchers.equalTo(MIGRATABLE_ID));
        Assert.assertThat(CONTEXT.getVersion(), Matchers.equalTo(VERSION));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullReport() throws Exception {
        new ExportMigrationContextImpl(null, MIGRATABLE, ZOS);
    }

    @Test
    public void testConstructorWithNullMigratable() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null migratable"));

        new ExportMigrationContextImpl(REPORT, null, ZOS);
    }

    @Test
    public void testConstructorWithNullZipOutputStream() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null zip output stream"));

        new ExportMigrationContextImpl(REPORT, MIGRATABLE, null);
    }

    @Test
    public void testGetSystemPropertyReferencedEntryWhenValueIsRelative() throws Exception {
        createDirectory(MIGRATABLE_NAME_DIRS);
        createFile(MIGRATABLE_NAME);

        final Optional<ExportMigrationEntry> oentry = CONTEXT.getSystemPropertyReferencedEntry(
                PROPERTY_NAME,
                (r, v) -> true);

        Assert.assertThat(oentry, OptionalMatchers.isPresent());
        final ExportMigrationEntry entry = oentry.get();

        Assert.assertThat(entry.getId(), Matchers.equalTo(MIGRATABLE_ID));
        Assert.assertThat(entry.getName(), Matchers.equalTo(MIGRATABLE_NAME));
        Assert.assertThat(entry.getPath(), Matchers.equalTo(MIGRATABLE_PATH));
        // now check that it is a system property referenced entry that references the proper property name
        Assert.assertThat(entry,
                Matchers.instanceOf(ExportMigrationSystemPropertyReferencedEntryImpl.class));
        final ExportMigrationSystemPropertyReferencedEntryImpl sentry =
                (ExportMigrationSystemPropertyReferencedEntryImpl) entry;

        Assert.assertThat(sentry.getProperty(), Matchers.equalTo(PROPERTY_NAME));
        // finally make sure no warnings or errors were recorded
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
    }

    @Test
    public void testGetSystemPropertyReferencedEntryWhenValueIsAbsoluteUnderDDFHome()
            throws Exception {
        System.setProperty(PROPERTY_NAME,
                DDF_HOME.resolve(MIGRATABLE_PATH)
                        .toAbsolutePath()
                        .toString());

        createDirectory(MIGRATABLE_NAME_DIRS);
        createFile(MIGRATABLE_NAME);

        final Optional<ExportMigrationEntry> oentry = CONTEXT.getSystemPropertyReferencedEntry(
                PROPERTY_NAME,
                (r, v) -> true);

        Assert.assertThat(oentry, OptionalMatchers.isPresent());
        final ExportMigrationEntry entry = oentry.get();

        Assert.assertThat(entry.getId(), Matchers.equalTo(MIGRATABLE_ID));
        Assert.assertThat(entry.getName(), Matchers.equalTo(MIGRATABLE_NAME));
        Assert.assertThat(entry.getPath(), Matchers.equalTo(MIGRATABLE_PATH));
        // now check that it is a system property referenced entry that references the proper property name
        Assert.assertThat(entry,
                Matchers.instanceOf(ExportMigrationSystemPropertyReferencedEntryImpl.class));
        final ExportMigrationSystemPropertyReferencedEntryImpl sentry =
                (ExportMigrationSystemPropertyReferencedEntryImpl) entry;

        Assert.assertThat(sentry.getProperty(), Matchers.equalTo(PROPERTY_NAME));
        // finally make sure no warnings or errors were recorded
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
    }

    @Test
    public void testGetSystemPropertyReferencedEntryWhenValueIsAbsoluteNotUnderDDFHome()
            throws Exception {
        final Path MIGRATABLE_PATH = testFolder.newFile("test.cfg")
                .toPath()
                .toRealPath(LinkOption.NOFOLLOW_LINKS);
        final String MIGRATABLE_NAME = MIGRATABLE_PATH.toString();

        System.setProperty(PROPERTY_NAME,
                MIGRATABLE_PATH.toAbsolutePath()
                        .toString());

        final Optional<ExportMigrationEntry> oentry = CONTEXT.getSystemPropertyReferencedEntry(
                PROPERTY_NAME,
                (r, v) -> true);

        Assert.assertThat(oentry, OptionalMatchers.isPresent());
        final ExportMigrationEntry entry = oentry.get();

        Assert.assertThat(entry.getId(), Matchers.equalTo(MIGRATABLE_ID));
        Assert.assertThat(entry.getName(), Matchers.equalTo(MIGRATABLE_NAME));
        Assert.assertThat(entry.getPath(), Matchers.equalTo(MIGRATABLE_PATH));
        // now check that it is a system property referenced entry that references the proper property name
        Assert.assertThat(entry,
                Matchers.instanceOf(ExportMigrationSystemPropertyReferencedEntryImpl.class));
        final ExportMigrationSystemPropertyReferencedEntryImpl sentry =
                (ExportMigrationSystemPropertyReferencedEntryImpl) entry;

        Assert.assertThat(sentry.getProperty(), Matchers.equalTo(PROPERTY_NAME));
        // finally make sure no warnings or errors were recorded
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
    }

    @Test
    public void testGetSystemPropertyReferencedEntryWithNullName() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null system property name"));

        CONTEXT.getSystemPropertyReferencedEntry(null, (r, v) -> true);
    }

    @Test
    public void testGetSystemPropertyReferencedEntryWithNullValidator() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null validator"));

        CONTEXT.getSystemPropertyReferencedEntry(PROPERTY_NAME, null);
    }

    @Test
    public void testGetSystemPropertyReferencedEntryWhenAlreadyCached() throws Exception {
        final ExportMigrationEntry ENTRY = CONTEXT.getSystemPropertyReferencedEntry(PROPERTY_NAME,
                (r, v) -> true)
                .get();

        final Optional<ExportMigrationEntry> oentry = CONTEXT.getSystemPropertyReferencedEntry(
                PROPERTY_NAME,
                (r, v) -> true);

        Assert.assertThat(oentry, OptionalMatchers.isPresent());
        final ExportMigrationEntry entry = oentry.get();

        Assert.assertThat(entry, Matchers.sameInstance(ENTRY));
        // finally make sure no warnings or errors were recorded
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
    }

    @Test
    public void testGetSystemPropertyReferencedEntryWhenInvalid() throws Exception {
        final Optional<ExportMigrationEntry> oentry = CONTEXT.getSystemPropertyReferencedEntry(
                PROPERTY_NAME,
                (r, v) -> false);

        Assert.assertThat(oentry, OptionalMatchers.isEmpty());
    }

    @Test
    public void testGetSystemPropertyReferencedEntryWhenPropertyIsNotDefined() throws Exception {
        final Optional<ExportMigrationEntry> oentry = CONTEXT.getSystemPropertyReferencedEntry(
                PROPERTY_NAME2,
                (r, v) -> true);

        Assert.assertThat(oentry, OptionalMatchers.isEmpty());
        // finally make sure we got an error (register the thrown expectations after the above to make sure
        // we don't get an exception from the above code under test
        thrown.expect(MigrationException.class);
        thrown.expectMessage(Matchers.containsString(
                "System property [" + PROPERTY_NAME2 + "] is not defined"));

        REPORT.verifyCompletion(); // to get the exception thrown out
    }

    @Test
    public void testGetSystemPropertyReferencedEntryWhenPropertyValueIsEmpty() throws Exception {
        System.setProperty(PROPERTY_NAME2, "");

        final Optional<ExportMigrationEntry> oentry = CONTEXT.getSystemPropertyReferencedEntry(
                PROPERTY_NAME2,
                (r, v) -> true);

        Assert.assertThat(oentry, OptionalMatchers.isEmpty());
        // finally make sure we got an error (register the thrown expectations after the above to make sure
        // we don't get an exception from the above code under test
        thrown.expect(MigrationException.class);
        thrown.expectMessage(Matchers.containsString(
                "System property [" + PROPERTY_NAME2 + "] is empty"));

        REPORT.verifyCompletion(); // to get the exception thrown out
    }

    @Test
    public void testGetEntryWithRelativePath() throws Exception {
        final ExportMigrationEntry entry = CONTEXT.getEntry(MIGRATABLE_PATH);

        Assert.assertThat(entry.getId(), Matchers.equalTo(MIGRATABLE_ID));
        Assert.assertThat(entry.getName(), Matchers.equalTo(MIGRATABLE_NAME));
        Assert.assertThat(entry.getPath(), Matchers.equalTo(MIGRATABLE_PATH));
        // now check that it is a standard export entry
        Assert.assertThat(entry, Matchers.instanceOf(ExportMigrationEntryImpl.class));
        // finally make sure no warnings or errors were recorded
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
    }

    @Test
    public void testGetEntryWithAbsolutePathUnderDDFHome() throws Exception {
        final ExportMigrationEntry entry = CONTEXT.getEntry(DDF_HOME.resolve(MIGRATABLE_PATH)
                .toAbsolutePath());

        Assert.assertThat(entry.getId(), Matchers.equalTo(MIGRATABLE_ID));
        Assert.assertThat(entry.getName(), Matchers.equalTo(MIGRATABLE_NAME));
        Assert.assertThat(entry.getPath(), Matchers.equalTo(MIGRATABLE_PATH));
        // now check that it is a standard export entry
        Assert.assertThat(entry, Matchers.instanceOf(ExportMigrationEntryImpl.class));
        // finally make sure no warnings or errors were recorded
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
    }

    @Test
    public void testGetEntryWithAbsolutePathNotUnderDDFHome() throws Exception {
        final Path MIGRATABLE_PATH = Paths.get(MIGRATABLE_NAME)
                .toAbsolutePath();
        final String MIGRATABLE_NAME = MIGRATABLE_PATH.toString();

        final ExportMigrationEntry entry = CONTEXT.getEntry(MIGRATABLE_PATH);

        Assert.assertThat(entry.getId(), Matchers.equalTo(MIGRATABLE_ID));
        Assert.assertThat(entry.getName(), Matchers.equalTo(MIGRATABLE_NAME));
        Assert.assertThat(entry.getPath(), Matchers.equalTo(MIGRATABLE_PATH));
        // now check that it is a standard export entry
        Assert.assertThat(entry, Matchers.instanceOf(ExportMigrationEntryImpl.class));
        // finally make sure no warnings or errors were recorded
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
    }

    @Test
    public void testGetEntryWithRelativePathAlreadyExist() throws Exception {
        final ExportMigrationEntry ENTRY = CONTEXT.getEntry(MIGRATABLE_PATH);

        final ExportMigrationEntry entry = CONTEXT.getEntry(MIGRATABLE_PATH);

        Assert.assertThat(entry, Matchers.sameInstance(ENTRY));
        // finally make sure no warnings or errors were recorded
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
    }

    @Test
    public void testGetEntryWithAbsolutePathUnderDDFHomePathAlreadyExist() throws Exception {
        final ExportMigrationEntry ENTRY = CONTEXT.getEntry(MIGRATABLE_PATH);

        final ExportMigrationEntry entry = CONTEXT.getEntry(DDF_HOME.resolve(MIGRATABLE_PATH)
                .toAbsolutePath());

        Assert.assertThat(entry, Matchers.sameInstance(ENTRY));
        // finally make sure no warnings or errors were recorded
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
    }

    @Test
    public void testGetEntryWithAbsolutePathNotUnderDDFHomeAlreadyExist() throws Exception {
        final Path MIGRATABLE_PATH = Paths.get(MIGRATABLE_NAME)
                .toAbsolutePath();
        final ExportMigrationEntry ENTRY = CONTEXT.getEntry(MIGRATABLE_PATH);

        final ExportMigrationEntry entry = CONTEXT.getEntry(MIGRATABLE_PATH);

        Assert.assertThat(entry, Matchers.sameInstance(ENTRY));
        // finally make sure no warnings or errors were recorded
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
    }

    @Test
    public void testGetEntryWithNullPath() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null path"));

        CONTEXT.getEntry(null);
    }

    @Test
    public void testEntriesWithRelativePath() throws Exception {
        final Path ETC = createDirectory("etc");
        final Path KS = createDirectory("etc", "keystores");
        final Path OTHER = createDirectory("other");
        final List<Path> PATHS = createFiles(ETC, "test.cfg", "test2.config", "test3.properties");

        createFiles(PATHS, KS, "serverKeystore.jks", "serverTruststore.jks");
        createFiles(OTHER, "a", "b");
        createDirectory("etc", "keystores", "empty");

        final List<ExportMigrationEntry> entries = CONTEXT.entries(ETC)
                .collect(Collectors.toList());

        Assert.assertThat(entries.stream()
                .map(ExportMigrationEntry::getPath)
                .collect(Collectors.toList()), Matchers.containsInAnyOrder(PATHS.toArray()));
        // finally make sure no warnings or errors were recorded
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
    }

    @Test
    public void testEntriesWithAbsolutePathUnderDDFHome() throws Exception {
        final Path ETC = createDirectory("etc");
        final Path KS = createDirectory("etc", "keystores");
        final Path OTHER = createDirectory("other");
        final List<Path> PATHS = createFiles(ETC, "test.cfg", "test2.config", "test3.properties");

        createFiles(PATHS, KS, "serverKeystore.jks", "serverTruststore.jks");
        createFiles(OTHER, "a", "b");
        createDirectory("etc", "keystores", "empty");

        final List<ExportMigrationEntry> entries = CONTEXT.entries(ETC.toAbsolutePath())
                .collect(Collectors.toList());

        Assert.assertThat(entries.stream()
                .map(ExportMigrationEntry::getPath)
                .collect(Collectors.toList()), Matchers.containsInAnyOrder(PATHS.toArray()));
        // finally make sure no warnings or errors were recorded
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
    }

    @Test
    public void testEntriesWhenPathDoesNotExist() throws Exception {
        final Path NOT_FOUND = DDF_HOME.resolve("not-found");

        final Stream<ExportMigrationEntry> entries = CONTEXT.entries(NOT_FOUND);

        Assert.assertThat(entries.count(), Matchers.equalTo(0L));
        // verify we got an error and no warnings
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(true));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
        // finally make sure we got an error (register the thrown expectations after the above to make sure
        // we don't get an exception from the above code under test
        thrown.expect(ExportPathMigrationException.class);
        thrown.expectMessage(Matchers.containsString("[not-found]"));
        thrown.expectMessage(Matchers.containsString("does not exist"));

        REPORT.verifyCompletion(); // to get the exception thrown out
    }

    @Test
    public void testEntriesWhenPathIsNotADirectory() throws Exception {
        final Path ETC = createDirectory("etc");
        final Path NOT_A_DIR = createFile(ETC, "not-a-dir");

        final Stream<ExportMigrationEntry> entries = CONTEXT.entries(NOT_A_DIR);

        Assert.assertThat(entries.count(), Matchers.equalTo(0L));
        // verify we got an error and no warnings
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(true));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
        // finally make sure we got an error (register the thrown expectations after the above to make sure
        // we don't get an exception from the above code under test
        thrown.expect(ExportPathMigrationException.class);
        thrown.expectMessage(Matchers.containsString("[etc/not-a-dir]"));
        thrown.expectMessage(Matchers.containsString("is not a directory"));

        REPORT.verifyCompletion(); // to get the exception thrown out
    }

    @Test
    public void testEntriesWhenSomeAlreadyExist() throws Exception {
        final Path ETC = createDirectory("etc");
        final Path KS = createDirectory("etc", "keystores");
        final Path OTHER = createDirectory("other");
        final List<Path> PATHS = createFiles(ETC, "test.cfg", "test2.config", "test3.properties");

        createFiles(PATHS, KS, "serverKeystore.jks", "serverTruststore.jks");
        createFiles(OTHER, "a", "b");
        createDirectory("etc", "keystores", "empty");
        final ExportMigrationEntry ENTRY = CONTEXT.getEntry(ETC.resolve("test.cfg"));

        final List<ExportMigrationEntry> entries = CONTEXT.entries(ETC)
                .collect(Collectors.toList());

        Assert.assertThat(entries.stream()
                .map(ExportMigrationEntry::getPath)
                .collect(Collectors.toList()), Matchers.containsInAnyOrder(PATHS.toArray()));
        Assert.assertThat(entries, Matchers.hasItem(Matchers.sameInstance(ENTRY)));
        // finally make sure no warnings or errors were recorded
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
    }

    @Test
    public void testEntriesWithNullPath() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null path"));

        CONTEXT.entries(null);
    }

    @Test
    public void testEntriesWithFilterAndRelativePath() throws Exception {
        final Path ETC = createDirectory("etc");
        final Path KS = createDirectory("etc", "keystores");
        final Path OTHER = createDirectory("other");
        final List<Path> PATHS = createFiles(ETC, "test.cfg", "test2.config", "test3.properties");

        createFiles(KS, "serverKeystore.jks", "serverTruststore.jks");
        createFiles(OTHER, "a", "b");
        createDirectory("etc", "keystores", "empty");

        final List<ExportMigrationEntry> entries = CONTEXT.entries(ETC,
                p -> p.getFileName()
                        .toString()
                        .startsWith("test"))
                .collect(Collectors.toList());

        Assert.assertThat(entries.stream()
                .map(ExportMigrationEntry::getPath)
                .collect(Collectors.toList()), Matchers.containsInAnyOrder(PATHS.toArray()));
        // finally make sure no warnings or errors were recorded
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
    }

    @Test
    public void testEntriesWithFilterAndAbsolutePathUnderDDFHome() throws Exception {
        final Path ETC = createDirectory("etc");
        final Path KS = createDirectory("etc", "keystores");
        final Path OTHER = createDirectory("other");
        final List<Path> PATHS = createFiles(ETC, "test.cfg", "test2.config", "test3.properties");

        createFiles(KS, "serverKeystore.jks", "serverTruststore.jks");
        createFiles(OTHER, "a", "b");
        createDirectory("etc", "keystores", "empty");

        final List<ExportMigrationEntry> entries = CONTEXT.entries(ETC.toAbsolutePath(),
                p -> p.getFileName()
                        .toString()
                        .startsWith("test"))
                .collect(Collectors.toList());

        Assert.assertThat(entries.stream()
                .map(ExportMigrationEntry::getPath)
                .collect(Collectors.toList()), Matchers.containsInAnyOrder(PATHS.toArray()));
        // finally make sure no warnings or errors were recorded
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
    }

    @Test
    public void testEntriesWithFilterWhenPathDoesNotExist() throws Exception {
        final Path NOT_FOUND = DDF_HOME.resolve("not-found");

        final Stream<ExportMigrationEntry> entries = CONTEXT.entries(NOT_FOUND,
                p -> p.getFileName()
                        .toString()
                        .startsWith("test"));

        Assert.assertThat(entries.count(), Matchers.equalTo(0L));
        // verify we got an error and no warnings
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(true));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
        // finally make sure we got an error (register the thrown expectations after the above to make sure
        // we don't get an exception from the above code under test
        thrown.expect(ExportPathMigrationException.class);
        thrown.expectMessage(Matchers.containsString("[not-found]"));
        thrown.expectMessage(Matchers.containsString("does not exist"));

        REPORT.verifyCompletion(); // to get the exception thrown out
    }

    @Test
    public void testEntriesWithFilterWhenPathIsNotADirectory() throws Exception {
        final Path ETC = createDirectory("etc");
        final Path NOT_A_DIR = createFile(ETC, "not-a-dir");

        final Stream<ExportMigrationEntry> entries = CONTEXT.entries(NOT_A_DIR,
                p -> p.getFileName()
                        .toString()
                        .startsWith("test"));

        Assert.assertThat(entries.count(), Matchers.equalTo(0L));
        // verify we got an error and no warnings
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(true));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
        // finally make sure we got an error (register the thrown expectations after the above to make sure
        // we don't get an exception from the above code under test
        thrown.expect(ExportPathMigrationException.class);
        thrown.expectMessage(Matchers.containsString("[etc/not-a-dir]"));
        thrown.expectMessage(Matchers.containsString("is not a directory"));

        REPORT.verifyCompletion(); // to get the exception thrown out
    }

    @Test
    public void testEntriesWithFilterWhenSomeAlreadyExist() throws Exception {
        final Path ETC = createDirectory("etc");
        final Path KS = createDirectory("etc", "keystores");
        final Path OTHER = createDirectory("other");
        final List<Path> PATHS = createFiles(ETC, "test.cfg", "test2.config", "test3.properties");

        createFiles(KS, "serverKeystore.jks", "serverTruststore.jks");
        createFiles(OTHER, "a", "b");
        createDirectory("etc", "keystores", "empty");
        final ExportMigrationEntry ENTRY = CONTEXT.getEntry(ETC.resolve("test.cfg"));

        final List<ExportMigrationEntry> entries = CONTEXT.entries(ETC,
                p -> p.getFileName()
                        .toString()
                        .startsWith("test"))
                .collect(Collectors.toList());

        Assert.assertThat(entries.stream()
                .map(ExportMigrationEntry::getPath)
                .collect(Collectors.toList()), Matchers.containsInAnyOrder(PATHS.toArray()));
        Assert.assertThat(entries, Matchers.hasItem(Matchers.sameInstance(ENTRY)));
        // finally make sure no warnings or errors were recorded
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
    }

    @Test
    public void testEntriesWithFilterAndNullPath() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null path"));

        CONTEXT.entries(null,
                p -> p.getFileName()
                        .toString()
                        .startsWith("test"));
    }

    @Test
    public void testEntriesWithNullFilter() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null filter"));

        final Path ETC = createDirectory("etc");

        CONTEXT.entries(ETC, null);
    }

    @Test
    public void testClose() throws Exception {
        CONTEXT.getOutputStreamFor(ENTRY);

        ZOS.close();

        final Map<String, ZipEntry> zentries = AbstractMigrationTest.getEntriesFrom(BAOS);

        Assert.assertThat(zentries.keySet(),
                Matchers.contains(MIGRATABLE_ID + '/' + MIGRATABLE_NAME));
    }

    @Test
    public void testCloseWithNoEntries() throws Exception {
        ZOS.close();

        final Map<String, ZipEntry> zentries = AbstractMigrationTest.getEntriesFrom(BAOS);

        Assert.assertThat(zentries, Matchers.aMapWithSize(0));
    }

    @Test
    public void testDoExport() throws Exception {
        Mockito.doNothing()
                .when(MIGRATABLE)
                .doExport(Mockito.any());

        final Map<String, Map<String, Object>> metadata = CONTEXT.doExport();

        Assert.assertThat(metadata.keySet(), Matchers.contains(MIGRATABLE_ID));
        final Map<String, Object> mmetadata = (Map<String, Object>) metadata.get(MIGRATABLE_ID);

        Assert.assertThat(mmetadata, Matchers.aMapWithSize(4));
        Assert.assertThat(mmetadata,
                Matchers.hasEntry(MigrationContextImpl.METADATA_VERSION, VERSION));
        Assert.assertThat(mmetadata, Matchers.hasEntry(MigrationContextImpl.METADATA_TITLE, TITLE));
        Assert.assertThat(mmetadata,
                Matchers.hasEntry(MigrationContextImpl.METADATA_DESCRIPTION, DESCRIPTION));
        Assert.assertThat(mmetadata,
                Matchers.hasEntry(MigrationContextImpl.METADATA_ORGANIZATION, ORGANIZATION));

        Mockito.verify(MIGRATABLE)
                .doExport(Mockito.same(CONTEXT));
    }

    @Test
    public void testGetOutputStreamFor() throws Exception {
        final OutputStream out = CONTEXT.getOutputStreamFor(ENTRY);

        ZOS.close();
        final Map<String, ZipEntry> zentries = AbstractMigrationTest.getEntriesFrom(BAOS);

        Assert.assertThat(zentries.keySet(),
                Matchers.contains(MIGRATABLE_ID + '/' + MIGRATABLE_NAME));
    }

    @Test
    public void testGetOutputStreamForWillClosePreviousEntry() throws Exception {
        final String MIGRATABLE_NAME2 = "etc/some/dir/test.txt2";
        final ExportMigrationEntryImpl ENTRY2 = Mockito.mock(ExportMigrationEntryImpl.class);

        Mockito.when(ENTRY2.getName())
                .thenReturn(MIGRATABLE_NAME2);
        final OutputStream OUT = CONTEXT.getOutputStreamFor(ENTRY);

        final OutputStream out = CONTEXT.getOutputStreamFor(ENTRY2);

        ZOS.close();
        final Map<String, ZipEntry> zentries = AbstractMigrationTest.getEntriesFrom(BAOS);

        Assert.assertThat(zentries.keySet(),
                Matchers.containsInAnyOrder(MIGRATABLE_ID + '/' + MIGRATABLE_NAME,
                        MIGRATABLE_ID + '/' + MIGRATABLE_NAME2));
    }

    @Test(expected = UncheckedIOException.class)
    public void testGetOutputStreamForWhenAlreadyClosed() throws Exception {
        ZOS.close(); // to trigger IOException when trying to create a new entry

        CONTEXT.getOutputStreamFor(ENTRY);
    }

    @Test
    public void testGetOutputStreamForWhenClosingReturnedStream() throws Exception {
        final OutputStream out = CONTEXT.getOutputStreamFor(ENTRY);

        out.close();
        ZOS.close();
        final Map<String, ZipEntry> zentries = AbstractMigrationTest.getEntriesFrom(BAOS);

        Assert.assertThat(zentries.keySet(),
                Matchers.contains(MIGRATABLE_ID + '/' + MIGRATABLE_NAME));
    }

    @Test
    public void testGetOutputStreamForWhenDoubleClosingReturnedStream() throws Exception {
        final OutputStream out = CONTEXT.getOutputStreamFor(ENTRY);

        out.close();
        out.close();
        ZOS.close();
        final Map<String, ZipEntry> zentries = AbstractMigrationTest.getEntriesFrom(BAOS);

        Assert.assertThat(zentries.keySet(),
                Matchers.contains(MIGRATABLE_ID + '/' + MIGRATABLE_NAME));
    }

    @Test
    public void testGetOutputStreamForWhenIOErrorsOccursFromReturnedStream() throws Exception {
        thrown.expect(ExportIOException.class);
        thrown.expectCause(Matchers.instanceOf(IOException.class));
        thrown.expectCause(Matchers.not(Matchers.instanceOf(ExportIOException.class)));

        final OutputStream out = CONTEXT.getOutputStreamFor(ENTRY);

        out.close();
        out.write(1);
    }

    @Test
    public void testGetOutputStreamForWhenIOErrorsOccursFromReturnedStreamThrownAsCause()
            throws Exception {
        final OutputStream out = CONTEXT.getOutputStreamFor(ENTRY);

        out.close();

        try {
            out.write(1);
        } catch (ExportIOException e) {
            Assert.assertThat(e.getIOException(), Matchers.instanceOf(IOException.class));
            Assert.assertThat(e.getIOException(), Matchers.sameInstance(e.getCause()));
        }
    }
}
