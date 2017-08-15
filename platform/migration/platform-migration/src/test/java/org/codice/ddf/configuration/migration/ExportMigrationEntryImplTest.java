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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.output.WriterOutputStream;
import org.codice.ddf.migration.ExportMigrationEntry;
import org.codice.ddf.migration.ExportPathMigrationException;
import org.codice.ddf.migration.ExportPathMigrationWarning;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationReport;
import org.codice.ddf.migration.MigrationWarning;
import org.codice.ddf.util.function.EBiConsumer;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;

import com.github.npathai.hamcrestopt.OptionalMatchers;
import com.google.common.base.Charsets;

public class ExportMigrationEntryImplTest extends AbstractMigrationTest {
    private static final String[] DIRS = new String[] {"path", "path2"};

    private static final String FILENAME = "file.ext";

    private static final String UNIX_NAME = "path/path2/" + FILENAME;

    private static final Path FILE_PATH = Paths.get(FilenameUtils.separatorsToSystem(UNIX_NAME));

    private static final String PROPERTY_NAME = "test.property";

    private static final String PROPERTY_NAME2 = "test.property2";

    private static final String MIGRATABLE_ID = "test-migratable";

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

    private final ExportMigrationReportImpl REPORT = new ExportMigrationReportImpl();

    private final ZipOutputStream ZOS = Mockito.mock(ZipOutputStream.class);

    private final ExportMigrationContextImpl CONTEXT =
            Mockito.mock(ExportMigrationContextImpl.class);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private Path ABSOLUTE_FILE_PATH;

    private PathUtils PATH_UTILS;

    private ExportMigrationEntryImpl ENTRY;

    private void storeProperty(String name, String val) throws IOException {
        FileUtils.writeStringToFile(ABSOLUTE_FILE_PATH.toFile(), name + '=' + val, Charsets.UTF_8);
    }

    @Before
    public void before() throws Exception {
        createFile(createDirectory(DIRS), FILENAME);
        PATH_UTILS = new PathUtils();
        ABSOLUTE_FILE_PATH = DDF_HOME.resolve(UNIX_NAME)
                .toRealPath(LinkOption.NOFOLLOW_LINKS);

        Mockito.when(CONTEXT.getPathUtils())
                .thenReturn(PATH_UTILS);
        Mockito.when(CONTEXT.getReport())
                .thenReturn(REPORT);
        Mockito.when(CONTEXT.getId())
                .thenReturn(MIGRATABLE_ID);

        ENTRY = new ExportMigrationEntryImpl(CONTEXT, FILE_PATH);
    }

    @Test
    public void testConstructorWithRelativePath() throws Exception {
        Assert.assertThat(ENTRY.getContext(), Matchers.sameInstance(CONTEXT));
        Assert.assertThat(ENTRY.getPath(), Matchers.equalTo(FILE_PATH));
        Assert.assertThat(ENTRY.getAbsolutePath(), Matchers.equalTo(ABSOLUTE_FILE_PATH));
        Assert.assertThat(ENTRY.getFile(), Matchers.equalTo(ABSOLUTE_FILE_PATH.toFile()));
        Assert.assertThat(ENTRY.getName(), Matchers.equalTo(UNIX_NAME));
    }

    @Test
    public void testConstructorWithAbsolutePathUnderDDFHome() throws Exception {
        final ExportMigrationEntryImpl ENTRY = new ExportMigrationEntryImpl(CONTEXT,
                ABSOLUTE_FILE_PATH);

        Assert.assertThat(ENTRY.getContext(), Matchers.sameInstance(CONTEXT));
        Assert.assertThat(ENTRY.getPath(), Matchers.equalTo(FILE_PATH));
        Assert.assertThat(ENTRY.getAbsolutePath(), Matchers.equalTo(ABSOLUTE_FILE_PATH));
        Assert.assertThat(ENTRY.getFile(), Matchers.equalTo(ABSOLUTE_FILE_PATH.toFile()));
        Assert.assertThat(ENTRY.getName(), Matchers.equalTo(UNIX_NAME));
    }

    @Test
    public void testConstructorWithAbsolutePathNotUnderDDFHome() throws Exception {
        final Path ABSOLUTE_FILE_PATH = createFile(ROOT, "test.ext");
        final String ABSOLUTE_FILE_NAME =
                FilenameUtils.separatorsToUnix(ABSOLUTE_FILE_PATH.toString());

        final ExportMigrationEntryImpl ENTRY = new ExportMigrationEntryImpl(CONTEXT,
                ABSOLUTE_FILE_PATH);

        Assert.assertThat(ENTRY.getContext(), Matchers.sameInstance(CONTEXT));
        Assert.assertThat(ENTRY.getPath(), Matchers.equalTo(ABSOLUTE_FILE_PATH));
        Assert.assertThat(ENTRY.getAbsolutePath(), Matchers.equalTo(ABSOLUTE_FILE_PATH));
        Assert.assertThat(ENTRY.getFile(), Matchers.equalTo(ABSOLUTE_FILE_PATH.toFile()));
        Assert.assertThat(ENTRY.getName(), Matchers.equalTo(ABSOLUTE_FILE_NAME));
    }

    @Test
    public void testConstructorWithNullContext() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null context"));

        new ExportMigrationEntryImpl(null, FILE_PATH);
    }

    @Test
    public void testConstructorWithNullPath() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null path"));

        new ExportMigrationEntryImpl(CONTEXT, (Path)null);
    }

    @Test
    public void testConstructorWhenPathDoesNotExist() throws Exception {
        ABSOLUTE_FILE_PATH.toFile()
                .delete();
        final ExportMigrationEntryImpl ENTRY = new ExportMigrationEntryImpl(CONTEXT, FILE_PATH);

        Assert.assertThat(ENTRY.getContext(), Matchers.sameInstance(CONTEXT));
        Assert.assertThat(ENTRY.getPath(), Matchers.equalTo(FILE_PATH));
        Assert.assertThat(ENTRY.getAbsolutePath(), Matchers.equalTo(FILE_PATH));
        Assert.assertThat(ENTRY.getFile(), Matchers.equalTo(FILE_PATH.toFile()));
        Assert.assertThat(ENTRY.getName(), Matchers.equalTo(UNIX_NAME));
    }

    @Test
    public void testConstructorWithRelativePathname() throws Exception {
        final ExportMigrationEntryImpl ENTRY = new ExportMigrationEntryImpl(CONTEXT, FILE_PATH.toString());

        Assert.assertThat(ENTRY.getContext(), Matchers.sameInstance(CONTEXT));
        Assert.assertThat(ENTRY.getPath(), Matchers.equalTo(FILE_PATH));
        Assert.assertThat(ENTRY.getAbsolutePath(), Matchers.equalTo(ABSOLUTE_FILE_PATH));
        Assert.assertThat(ENTRY.getFile(), Matchers.equalTo(ABSOLUTE_FILE_PATH.toFile()));
        Assert.assertThat(ENTRY.getName(), Matchers.equalTo(UNIX_NAME));
    }

    @Test
    public void testConstructorWithPathnameAndNullContext() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null context"));

        new ExportMigrationEntryImpl(null, UNIX_NAME);
    }

    @Test
    public void testConstructorWithNullPathname() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null pathname"));

        new ExportMigrationEntryImpl(CONTEXT, (String)null);
    }

    @Test
    public void testGetReport() throws Exception {
        Assert.assertThat(ENTRY.getReport(), Matchers.sameInstance(REPORT));

        Mockito.verify(CONTEXT)
                .getReport();
    }

    @Test
    public void testOutputStream() throws Exception {
        final OutputStream OS = Mockito.mock(OutputStream.class);

        Mockito.when(CONTEXT.getOutputStreamFor(Mockito.any()))
                .thenReturn(OS);

        final OutputStream os = ENTRY.getOutputStream();

        Assert.assertThat(os, Matchers.sameInstance(OS));

        Mockito.verify(CONTEXT)
                .getOutputStreamFor(Mockito.same(ENTRY));
    }

    @Test
    public void testOutputStreamWhenAlreadyRetrieved() throws Exception {
        final OutputStream OS = Mockito.mock(OutputStream.class);

        Mockito.when(CONTEXT.getOutputStreamFor(Mockito.any()))
                .thenReturn(OS);

        ENTRY.getOutputStream(); // pre-cache it

        final OutputStream os = ENTRY.getOutputStream();

        Assert.assertThat(os, Matchers.sameInstance(OS));

        Mockito.verify(CONTEXT)
                .getOutputStreamFor(Mockito.same(ENTRY));
    }

    @Test
    public void testOutputStreamWithException() throws Exception {
        final IOException E = new IOException("testing");

        Mockito.when(CONTEXT.getOutputStreamFor(Mockito.any()))
                .thenThrow(new UncheckedIOException(E));

        thrown.expect(Matchers.sameInstance(E));

        ENTRY.getOutputStream();
    }

    @Test
    public void testStoreWhenRequiredAndFileExist() throws Exception {
        final StringWriter WRITER = new StringWriter();

        Mockito.when(CONTEXT.getOutputStreamFor(Mockito.any()))
                .thenReturn(new WriterOutputStream(WRITER, Charsets.UTF_8));

        Assert.assertThat(ENTRY.store(true), Matchers.equalTo(true));
        Assert.assertThat(WRITER.toString(), Matchers.equalTo(FILENAME));
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.wasSuccessful(), Matchers.equalTo(true));
    }

    @Test
    public void testStoreWhenRequiredAndFileIsAbsoluteOutsideDDFHome() throws Exception {
        final StringWriter WRITER = new StringWriter();

        final Path ABSOLUTE_FILE_PATH = createFile(testFolder.getRoot()
                .toPath()
                .resolve(FILENAME));

        final ExportMigrationEntryImpl ENTRY = new ExportMigrationEntryImpl(CONTEXT,
                ABSOLUTE_FILE_PATH);

        Assert.assertThat(ENTRY.store(true), Matchers.equalTo(true));
        Assert.assertThat(WRITER.toString(), Matchers.emptyString());
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(true));
        Assert.assertThat(REPORT.wasSuccessful(), Matchers.equalTo(true));

        Assert.assertThat(REPORT.warnings()
                        .map(MigrationWarning::getMessage)
                        .toArray(String[]::new),
                Matchers.hasItemInArray(Matchers.containsString("is outside")));
    }

    @Test
    public void testStoreWhenRequiredAndFileIsASoftLink() throws Exception {
        final StringWriter WRITER = new StringWriter();

        final String FILENAME2 = "file2.ext";
        final Path ABSOLUTE_FILE_PATH2 = createSoftLink(ABSOLUTE_FILE_PATH.getParent(),
                FILENAME2,
                ABSOLUTE_FILE_PATH);

        final ExportMigrationEntryImpl ENTRY = new ExportMigrationEntryImpl(CONTEXT,
                ABSOLUTE_FILE_PATH2);

        Assert.assertThat(ENTRY.store(true), Matchers.equalTo(true));
        Assert.assertThat(WRITER.toString(), Matchers.emptyString());
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(true));
        Assert.assertThat(REPORT.wasSuccessful(), Matchers.equalTo(true));

        Assert.assertThat(REPORT.warnings()
                        .map(MigrationWarning::getMessage)
                        .toArray(String[]::new),
                Matchers.hasItemInArray(Matchers.containsString("is a symbolic link")));
    }

    @Test
    public void testStoreWhenOptionalAndFileExist() throws Exception {
        final StringWriter WRITER = new StringWriter();

        Mockito.when(CONTEXT.getOutputStreamFor(Mockito.any()))
                .thenReturn(new WriterOutputStream(WRITER, Charsets.UTF_8));

        Assert.assertThat(ENTRY.store(false), Matchers.equalTo(true));
        Assert.assertThat(WRITER.toString(), Matchers.equalTo(FILENAME));
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.wasSuccessful(), Matchers.equalTo(true));
    }

    @Test
    public void testStoreASecondTimeWhenFirstSucceeded() throws Exception {
        final StringWriter WRITER = new StringWriter();

        Mockito.when(CONTEXT.getOutputStreamFor(Mockito.any()))
                .thenReturn(new WriterOutputStream(WRITER, Charsets.UTF_8));

        ENTRY.store();
        // reset writer's buffer to make sure it will not be re-written
        WRITER.getBuffer()
                .setLength(0);

        Assert.assertThat(ENTRY.store(true), Matchers.equalTo(true));
        Assert.assertThat(WRITER.toString(), Matchers.emptyString());
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.wasSuccessful(), Matchers.equalTo(true));
    }

    @Test
    public void testStoreWhenRequiredAndFileDoesNotExist() throws Exception {
        ABSOLUTE_FILE_PATH.toFile()
                .delete();
        ENTRY = new ExportMigrationEntryImpl(CONTEXT, FILE_PATH);
        final StringWriter WRITER = new StringWriter();

        Mockito.when(CONTEXT.getOutputStreamFor(Mockito.any()))
                .thenReturn(new WriterOutputStream(WRITER, Charsets.UTF_8));

        Assert.assertThat(ENTRY.store(true), Matchers.equalTo(false));
        Assert.assertThat(WRITER.toString(), Matchers.emptyString());
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(true));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.wasSuccessful(), Matchers.equalTo(false));

        thrown.expect(ExportPathMigrationException.class);
        thrown.expectMessage(Matchers.containsString("does not exist"));

        REPORT.verifyCompletion(); // to trigger an exception from the report
    }

    @Test
    public void testStoreWhenOptionalAndFileDoesNotExist() throws Exception {
        ABSOLUTE_FILE_PATH.toFile()
                .delete();
        final ExportMigrationEntryImpl ENTRY = new ExportMigrationEntryImpl(CONTEXT, FILE_PATH);

        final StringWriter WRITER = new StringWriter();

        Mockito.when(CONTEXT.getOutputStreamFor(Mockito.any()))
                .thenReturn(new WriterOutputStream(WRITER, Charsets.UTF_8));

        Assert.assertThat(ENTRY.store(false), Matchers.equalTo(true));
        Assert.assertThat(WRITER.toString(), Matchers.emptyString());
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.wasSuccessful(), Matchers.equalTo(true));
    }

    @Test
    public void testStoreWhenRequiredAndFileRealPathCannotBeDetermined() throws Exception {
        final PathUtils PATH_UTILS = Mockito.mock(PathUtils.class);
        final Path PATH = Mockito.mock(Path.class);
        final IOException IOE = new IOException("test");

        Mockito.when(CONTEXT.getPathUtils())
                .thenReturn(PATH_UTILS);
        Mockito.when(PATH_UTILS.resolveAgainstDDFHome((Path) Mockito.any()))
                .thenReturn(PATH);
        Mockito.when(PATH_UTILS.relativizeFromDDFHome(Mockito.any()))
                .thenReturn(PATH);
        Mockito.when(PATH.toRealPath(LinkOption.NOFOLLOW_LINKS))
                .thenThrow(IOE);

        final ExportMigrationEntryImpl ENTRY = new ExportMigrationEntryImpl(CONTEXT, FILE_PATH);

        final StringWriter WRITER = new StringWriter();

        Mockito.when(CONTEXT.getOutputStreamFor(Mockito.any()))
                .thenReturn(new WriterOutputStream(WRITER, Charsets.UTF_8));

        Assert.assertThat(ENTRY.store(true), Matchers.equalTo(false));
        Assert.assertThat(WRITER.toString(), Matchers.emptyString());
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(true));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.wasSuccessful(), Matchers.equalTo(false));

        thrown.expect(ExportPathMigrationException.class);
        thrown.expectMessage(Matchers.containsString("cannot be read"));
        thrown.expectCause(Matchers.sameInstance(IOE));

        REPORT.verifyCompletion(); // to trigger an exception from the report
    }

    @Test
    public void testStoreWhenPathIsADirectory() throws Exception {
        final StringWriter WRITER = new StringWriter();

        Mockito.when(CONTEXT.getOutputStreamFor(Mockito.any()))
                .thenReturn(new WriterOutputStream(WRITER, Charsets.UTF_8));

        final ExportMigrationEntryImpl ENTRY = new ExportMigrationEntryImpl(CONTEXT,
                ABSOLUTE_FILE_PATH.getParent());

        Assert.assertThat(ENTRY.store(true), Matchers.equalTo(false));
        Assert.assertThat(WRITER.toString(), Matchers.emptyString());
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(true));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.wasSuccessful(), Matchers.equalTo(false));

        thrown.expect(ExportPathMigrationException.class);

        REPORT.verifyCompletion(); // to trigger an exception from the report
    }

    @Test
    public void testStoreWithConsumer() throws Exception {
        final EBiConsumer<MigrationReport, OutputStream, IOException> CONSUMER = Mockito.mock(
                EBiConsumer.class);
        final OutputStream OS = Mockito.mock(OutputStream.class);

        Mockito.when(CONTEXT.getOutputStreamFor(Mockito.any()))
                .thenReturn(OS);

        Assert.assertThat(ENTRY.store(CONSUMER), Matchers.equalTo(true));
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.wasSuccessful(), Matchers.equalTo(true));

        Mockito.verify(CONSUMER)
                .accept(Mockito.same(REPORT), Mockito.same(OS));
    }

    @Test
    public void testStoreWithConsumerReportingError() throws Exception {
        final EBiConsumer<MigrationReport, OutputStream, IOException> CONSUMER = Mockito.mock(
                EBiConsumer.class);
        final OutputStream OS = Mockito.mock(OutputStream.class);
        final MigrationException ME = Mockito.mock(MigrationException.class);

        Mockito.when(CONTEXT.getOutputStreamFor(Mockito.any()))
                .thenReturn(OS);
        Mockito.doAnswer(AdditionalAnswers.<MigrationReport, OutputStream>answerVoid((r, os) -> r.record(
                ME)))
                .when(CONSUMER)
                .accept(Mockito.any(), Mockito.any());

        Assert.assertThat(ENTRY.store(CONSUMER), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(true));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.wasSuccessful(), Matchers.equalTo(false));

        Mockito.verify(CONSUMER)
                .accept(Mockito.same(REPORT), Mockito.same(OS));

        thrown.expect(Matchers.sameInstance(ME));

        REPORT.verifyCompletion(); // to trigger an exception from the report
    }

    @Test
    public void testStoreWithConsumerThrowingMigrationException() throws Exception {
        final EBiConsumer<MigrationReport, OutputStream, IOException> CONSUMER = Mockito.mock(
                EBiConsumer.class);
        final EBiConsumer<MigrationReport, OutputStream, IOException> CONSUMER2 = Mockito.mock(
                EBiConsumer.class);
        final OutputStream OS = Mockito.mock(OutputStream.class);
        final MigrationException ME = Mockito.mock(MigrationException.class);

        Mockito.when(CONTEXT.getOutputStreamFor(Mockito.any()))
                .thenReturn(OS);
        Mockito.doThrow(ME)
                .when(CONSUMER)
                .accept(Mockito.any(), Mockito.any());

        thrown.expect(Matchers.sameInstance(ME));

        try {
            ENTRY.store(CONSUMER);
        } finally {
            Mockito.verify(CONSUMER)
                    .accept(Mockito.same(REPORT), Mockito.same(OS));

            // verify that if we were to store a second time, the consumer would not be called and false would be returned
            Assert.assertThat(ENTRY.store(), Matchers.equalTo(false));

            Mockito.verify(CONSUMER2, Mockito.never())
                    .accept(Mockito.any(), Mockito.any());
        }
    }

    @Test
    public void testStoreWithConsumerThrowingIOException() throws Exception {
        final EBiConsumer<MigrationReport, OutputStream, IOException> CONSUMER = Mockito.mock(
                EBiConsumer.class);
        final OutputStream OS = Mockito.mock(OutputStream.class);
        final IOException IOE = Mockito.mock(IOException.class);

        Mockito.when(CONTEXT.getOutputStreamFor(Mockito.any()))
                .thenReturn(OS);
        Mockito.doThrow(IOE)
                .when(CONSUMER)
                .accept(Mockito.any(), Mockito.any());

        Assert.assertThat(ENTRY.store(CONSUMER), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(true));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.wasSuccessful(), Matchers.equalTo(false));

        Mockito.verify(CONSUMER)
                .accept(Mockito.same(REPORT), Mockito.same(OS));

        thrown.expect(Matchers.instanceOf(MigrationException.class));
        thrown.expectCause(Matchers.sameInstance(IOE));

        REPORT.verifyCompletion(); // to trigger an exception from the report
    }

    @Test
    public void testStoreWithConsumerThrowingExportIOException() throws Exception {
        final EBiConsumer<MigrationReport, OutputStream, IOException> CONSUMER = Mockito.mock(
                EBiConsumer.class);
        final EBiConsumer<MigrationReport, OutputStream, IOException> CONSUMER2 = Mockito.mock(
                EBiConsumer.class);
        final OutputStream OS = Mockito.mock(OutputStream.class);
        final IOException IOE = Mockito.mock(IOException.class);
        final ExportIOException EIOE = new ExportIOException(IOE);

        Mockito.when(CONTEXT.getOutputStreamFor(Mockito.any()))
                .thenReturn(OS);
        Mockito.doThrow(EIOE)
                .when(CONSUMER)
                .accept(Mockito.any(), Mockito.any());

        thrown.expect(Matchers.instanceOf(MigrationException.class));
        thrown.expectCause(Matchers.sameInstance(IOE));

        try {
            ENTRY.store(CONSUMER);
        } finally {
            Mockito.verify(CONSUMER)
                    .accept(Mockito.same(REPORT), Mockito.same(OS));

            // verify that if we were to store a second time, the consumer would not be called and false would be returned
            Assert.assertThat(ENTRY.store(), Matchers.equalTo(false));

            Mockito.verify(CONSUMER2, Mockito.never())
                    .accept(Mockito.any(), Mockito.any());
        }
    }

    @Test
    public void testGetPropertyReferencedEntryWhenValueIsRelative() throws Exception {
        storeProperty(PROPERTY_NAME, MIGRATABLE_PROPERTY_PATHNAME);

        createDirectory(MIGRATABLE_NAME_DIRS);
        createFile(MIGRATABLE_NAME);

        final Optional<ExportMigrationEntry> oentry =
                ENTRY.getPropertyReferencedEntry(PROPERTY_NAME, (r, v) -> true);

        Assert.assertThat(oentry, OptionalMatchers.isPresent());
        final ExportMigrationEntry entry = oentry.get();

        Assert.assertThat(entry.getId(), Matchers.equalTo(MIGRATABLE_ID));
        Assert.assertThat(entry.getName(), Matchers.equalTo(MIGRATABLE_NAME));
        Assert.assertThat(entry.getPath(), Matchers.equalTo(MIGRATABLE_PATH));
        // now check that it is a java property referenced entry that references the proper property name
        Assert.assertThat(entry,
                Matchers.instanceOf(ExportMigrationJavaPropertyReferencedEntryImpl.class));
        final ExportMigrationJavaPropertyReferencedEntryImpl jentry =
                (ExportMigrationJavaPropertyReferencedEntryImpl) entry;

        Assert.assertThat(jentry.getProperty(), Matchers.equalTo(PROPERTY_NAME));
        // finally make sure no warnings or errors were recorded
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
    }

    @Test
    public void testGetPropertyReferencedEntryWhenValueIsAbsoluteUnderDDFHome() throws Exception {
        storeProperty(PROPERTY_NAME,
                DDF_HOME.resolve(MIGRATABLE_PATH)
                        .toAbsolutePath()
                        .toString());

        createDirectory(MIGRATABLE_NAME_DIRS);
        createFile(MIGRATABLE_NAME);

        final Optional<ExportMigrationEntry> oentry =
                ENTRY.getPropertyReferencedEntry(PROPERTY_NAME, (r, v) -> true);

        Assert.assertThat(oentry, OptionalMatchers.isPresent());
        final ExportMigrationEntry entry = oentry.get();

        Assert.assertThat(entry.getId(), Matchers.equalTo(MIGRATABLE_ID));
        Assert.assertThat(entry.getName(), Matchers.equalTo(MIGRATABLE_NAME));
        Assert.assertThat(entry.getPath(), Matchers.equalTo(MIGRATABLE_PATH));
        // now check that it is a java property referenced entry that references the proper property name
        Assert.assertThat(entry,
                Matchers.instanceOf(ExportMigrationJavaPropertyReferencedEntryImpl.class));
        final ExportMigrationJavaPropertyReferencedEntryImpl jentry =
                (ExportMigrationJavaPropertyReferencedEntryImpl) entry;

        Assert.assertThat(jentry.getProperty(), Matchers.equalTo(PROPERTY_NAME));
        // finally make sure no warnings or errors were recorded
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
    }

    @Test
    public void testGetPropertyReferencedEntryWhenValueIsAbsoluteNotUnderDDFHome()
            throws Exception {
        final Path MIGRATABLE_PATH = testFolder.newFile("test.cfg")
                .toPath()
                .toRealPath(LinkOption.NOFOLLOW_LINKS);
        final String MIGRATABLE_NAME = MIGRATABLE_PATH.toString();

        storeProperty(PROPERTY_NAME,
                MIGRATABLE_PATH.toAbsolutePath()
                        .toString());

        final Optional<ExportMigrationEntry> oentry =
                ENTRY.getPropertyReferencedEntry(PROPERTY_NAME, (r, v) -> true);

        Assert.assertThat(oentry, OptionalMatchers.isPresent());
        final ExportMigrationEntry entry = oentry.get();

        Assert.assertThat(entry.getId(), Matchers.equalTo(MIGRATABLE_ID));
        Assert.assertThat(entry.getName(), Matchers.equalTo(MIGRATABLE_NAME));
        Assert.assertThat(entry.getPath(), Matchers.equalTo(MIGRATABLE_PATH));
        // now check that it is a java property referenced entry that references the proper property name
        Assert.assertThat(entry,
                Matchers.instanceOf(ExportMigrationJavaPropertyReferencedEntryImpl.class));
        final ExportMigrationJavaPropertyReferencedEntryImpl jentry =
                (ExportMigrationJavaPropertyReferencedEntryImpl) entry;

        Assert.assertThat(jentry.getProperty(), Matchers.equalTo(PROPERTY_NAME));
        // finally make sure no warnings or errors were recorded
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
    }

    @Test
    public void testGetPropertyReferencedEntryWithNullName() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null java property name"));

        ENTRY.getPropertyReferencedEntry(null, (r, v) -> true);
    }

    @Test
    public void testGetPropertyReferencedEntryWithNullValidator() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null validator"));

        ENTRY.getPropertyReferencedEntry(PROPERTY_NAME, null);
    }

    @Test
    public void testGetPropertyReferencedEntryWhenAlreadyCached() throws Exception {
        storeProperty(PROPERTY_NAME, MIGRATABLE_PROPERTY_PATHNAME);
        final ExportMigrationEntry JENTRY = ENTRY.getPropertyReferencedEntry(PROPERTY_NAME,
                (r, v) -> true)
                .get();

        final Optional<ExportMigrationEntry> oentry =
                ENTRY.getPropertyReferencedEntry(PROPERTY_NAME, (r, v) -> true);

        Assert.assertThat(oentry, OptionalMatchers.isPresent());
        final ExportMigrationEntry entry = oentry.get();

        Assert.assertThat(entry, Matchers.sameInstance(JENTRY));
        // finally make sure no warnings or errors were recorded
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
    }

    @Test
    public void testGetPropertyReferencedEntryWhenInvalid() throws Exception {
        storeProperty(PROPERTY_NAME, MIGRATABLE_PROPERTY_PATHNAME);
        final Optional<ExportMigrationEntry> oentry =
                ENTRY.getPropertyReferencedEntry(PROPERTY_NAME, (r, v) -> false);

        Assert.assertThat(oentry, OptionalMatchers.isEmpty());
    }

    @Test
    public void testGetPropertyReferencedEntryWhenPropertyIsNotDefined() throws Exception {
        storeProperty(PROPERTY_NAME, MIGRATABLE_PROPERTY_PATHNAME);
        final Optional<ExportMigrationEntry> oentry = ENTRY.getPropertyReferencedEntry(
                PROPERTY_NAME2,
                (r, v) -> true);

        Assert.assertThat(oentry, OptionalMatchers.isEmpty());
        // finally make sure we got an error (register the thrown expectations after the above to make sure
        // we don't get an exception from the above code under test
        thrown.expect(MigrationException.class);
        thrown.expectMessage(Matchers.containsString(
                "Java property [" + PROPERTY_NAME2 + "] from [" + FILE_PATH + "] is not defined"));

        REPORT.verifyCompletion(); // to get the exception thrown out
    }

    @Test
    public void testGetPropertyReferencedEntryWhenPropertyValueIsEmpty() throws Exception {
        storeProperty(PROPERTY_NAME2, "");

        final Optional<ExportMigrationEntry> oentry = ENTRY.getPropertyReferencedEntry(
                PROPERTY_NAME2,
                (r, v) -> true);

        Assert.assertThat(oentry, OptionalMatchers.isEmpty());
        // finally make sure we got an error (register the thrown expectations after the above to make sure
        // we don't get an exception from the above code under test
        thrown.expect(MigrationException.class);
        thrown.expectMessage(Matchers.containsString(
                "Java property [" + PROPERTY_NAME2 + "] from [" + FILE_PATH + "] is empty"));

        REPORT.verifyCompletion(); // to get the exception thrown out
    }

    @Test
    public void testGetPropertyReferencedEntryWhenUnableToReadPropertyValue() throws Exception {
        ABSOLUTE_FILE_PATH.toFile()
                .delete();

        final Optional<ExportMigrationEntry> oentry =
                ENTRY.getPropertyReferencedEntry(PROPERTY_NAME, (r, v) -> true);

        Assert.assertThat(oentry, OptionalMatchers.isEmpty());
        // finally make sure we got an error (register the thrown expectations after the above to make sure
        // we don't get an exception from the above code under test
        thrown.expect(MigrationException.class);
        thrown.expectMessage(Matchers.containsString(
                "Java property [" + PROPERTY_NAME + "] from [" + FILE_PATH + "]"));
        thrown.expectMessage(Matchers.containsString("failed to load property file"));

        REPORT.verifyCompletion(); // to get the exception thrown out
    }

    @Test
    public void testToDebugString() throws Exception {
        final String debug = ENTRY.toDebugString();

        Assert.assertThat(debug, Matchers.containsString("file"));
        Assert.assertThat(debug, Matchers.containsString("[" + UNIX_NAME + "]"));
    }

    @Test
    public void testNewWarning() throws Exception {
        final String REASON = "test reason";
        final ExportPathMigrationWarning warning = ENTRY.newWarning(REASON);

        Assert.assertThat(warning.getMessage(), Matchers.containsString("[" + UNIX_NAME + "]"));
        Assert.assertThat(warning.getMessage(), Matchers.containsString(REASON));
    }

    @Test
    public void testNewError() throws Exception {
        final String REASON = "test reason";
        final IllegalArgumentException CAUSE = new IllegalArgumentException("test cause");
        final ExportPathMigrationException error = ENTRY.newError(REASON, CAUSE);

        Assert.assertThat(error.getMessage(), Matchers.containsString("[" + UNIX_NAME + "]"));
        Assert.assertThat(error.getMessage(), Matchers.containsString(REASON));
        Assert.assertThat(error.getCause(), Matchers.sameInstance(CAUSE));
    }

    @Test
    public void testEqualsWhenEquals() throws Exception {
        final ExportMigrationEntryImpl ENTRY2 = new ExportMigrationEntryImpl(CONTEXT, FILE_PATH);

        Assert.assertThat(ENTRY.equals(ENTRY2), Matchers.equalTo(true));
    }

    @Test
    public void testEqualsWhenIdentical() throws Exception {
        Assert.assertThat(ENTRY.equals(ENTRY), Matchers.equalTo(true));
    }

    @Test
    public void testEqualsWhenNull() throws Exception {
        Assert.assertThat(ENTRY.equals(null), Matchers.equalTo(false));
    }

    @Test
    public void testEqualsWithNotAnEntry() throws Exception {
        Assert.assertThat(ENTRY.equals("test"), Matchers.equalTo(false));
    }

    @Test
    public void testEqualsWhenContextsAreDifferent() throws Exception {
        final ExportMigrationContextImpl CONTEXT2 =
                Mockito.mock(ExportMigrationContextImpl.class);

        Mockito.when(CONTEXT2.getPathUtils())
                .thenReturn(PATH_UTILS);
        Mockito.when(CONTEXT2.getReport())
                .thenReturn(REPORT);
        Mockito.when(CONTEXT2.getId())
                .thenReturn(MIGRATABLE_ID);

        final ExportMigrationEntryImpl ENTRY2 = new ExportMigrationEntryImpl(CONTEXT2, FILE_PATH);

        Assert.assertThat(ENTRY.equals(ENTRY2), Matchers.equalTo(false));
    }

    @Test
    public void testEqualsWhenPathsAreDifferent() throws Exception {
        final ExportMigrationEntryImpl ENTRY2 = new ExportMigrationEntryImpl(CONTEXT, FILE_PATH.getParent());

        Assert.assertThat(ENTRY.equals(ENTRY2), Matchers.equalTo(false));
    }

    @Test
    public void testHashCodeWhenEquals() throws Exception {
        final ExportMigrationEntryImpl ENTRY2 = new ExportMigrationEntryImpl(CONTEXT, FILE_PATH);

        Assert.assertThat(ENTRY.hashCode(), Matchers.equalTo(ENTRY2.hashCode()));
    }

    @Test
    public void testHashCodeWhenDifferent() throws Exception {
        final ExportMigrationEntryImpl ENTRY2 = new ExportMigrationEntryImpl(CONTEXT, FILE_PATH.getParent());

        Assert.assertThat(ENTRY.hashCode(), Matchers.not(Matchers.equalTo(ENTRY2.hashCode())));
    }

    @Test
    public void testGetLastModifiedTime() throws Exception {
        final PathUtils PATH_UTILS = Mockito.mock(PathUtils.class);
        final Path FILE_PATH = Mockito.mock(Path.class);
        final File FILE = Mockito.mock(File.class);
        final long MODIFIED = 12345L;

        Mockito.when(CONTEXT.getPathUtils()).thenReturn(PATH_UTILS);
        Mockito.when(PATH_UTILS.resolveAgainstDDFHome(FILE_PATH)).thenReturn(FILE_PATH);
        Mockito.when(FILE_PATH.toRealPath(Mockito.any())).thenReturn(FILE_PATH);
        Mockito.when(PATH_UTILS.relativizeFromDDFHome(FILE_PATH)).thenReturn(FILE_PATH);
        Mockito.when(FILE_PATH.toString()).thenReturn(UNIX_NAME);
        Mockito.when(FILE_PATH.toFile()).thenReturn(FILE);
        Mockito.when(FILE.lastModified()).thenReturn(MODIFIED);

        final ExportMigrationEntryImpl ENTRY = new ExportMigrationEntryImpl(CONTEXT, FILE_PATH);

        Assert.assertThat(ENTRY.getLastModifiedTime(), Matchers.equalTo(MODIFIED));

        Mockito.verify(FILE).lastModified();
    }
}
