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
import com.google.common.base.Charsets;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.output.WriterOutputStream;
import org.codice.ddf.migration.ExportMigrationEntry;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationReport;
import org.codice.ddf.migration.MigrationWarning;
import org.codice.ddf.util.function.BiThrowingConsumer;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;

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

  private static final String MIGRATABLE_PROPERTY_PATHNAME =
      Paths.get("..", "ddf", "where", "some", "dir", "test.txt").toString();

  private static final Path MIGRATABLE_PATH =
      Paths.get(FilenameUtils.separatorsToSystem(MIGRATABLE_NAME));

  private final ExportMigrationReportImpl report = new ExportMigrationReportImpl();

  private final ExportMigrationContextImpl context = Mockito.mock(ExportMigrationContextImpl.class);

  private Path absoluteFilePath;

  private PathUtils pathUtils;

  private ExportMigrationEntryImpl entry;

  @Before
  public void before() throws Exception {
    createFile(createDirectory(DIRS), FILENAME);
    pathUtils = new PathUtils();
    absoluteFilePath = ddfHome.resolve(UNIX_NAME).toRealPath(LinkOption.NOFOLLOW_LINKS);

    Mockito.when(context.getPathUtils()).thenReturn(pathUtils);
    Mockito.when(context.getReport()).thenReturn(report);
    Mockito.when(context.getId()).thenReturn(MIGRATABLE_ID);

    entry = new ExportMigrationEntryImpl(context, FILE_PATH);
  }

  @Test
  public void testConstructorWithRelativePath() throws Exception {
    Assert.assertThat(entry.getContext(), Matchers.sameInstance(context));
    Assert.assertThat(entry.getPath(), Matchers.equalTo(FILE_PATH));
    Assert.assertThat(entry.getAbsolutePath(), Matchers.equalTo(absoluteFilePath));
    Assert.assertThat(entry.getFile(), Matchers.equalTo(absoluteFilePath.toFile()));
    Assert.assertThat(entry.getName(), Matchers.equalTo(UNIX_NAME));
  }

  @Test
  public void testConstructorWithAbsolutePathUnderDDFHome() throws Exception {
    final ExportMigrationEntryImpl entry = new ExportMigrationEntryImpl(context, absoluteFilePath);

    Assert.assertThat(entry.getContext(), Matchers.sameInstance(context));
    Assert.assertThat(entry.getPath(), Matchers.equalTo(FILE_PATH));
    Assert.assertThat(entry.getAbsolutePath(), Matchers.equalTo(absoluteFilePath));
    Assert.assertThat(entry.getFile(), Matchers.equalTo(absoluteFilePath.toFile()));
    Assert.assertThat(entry.getName(), Matchers.equalTo(UNIX_NAME));
  }

  @Test
  public void testConstructorWithAbsolutePathNotUnderDDFHome() throws Exception {
    final Path absoluteFilePath = createFile(root, "test.ext");
    final String absoluteFileName = FilenameUtils.separatorsToUnix(absoluteFilePath.toString());

    final ExportMigrationEntryImpl entry = new ExportMigrationEntryImpl(context, absoluteFilePath);

    Assert.assertThat(entry.getContext(), Matchers.sameInstance(context));
    Assert.assertThat(entry.getPath(), Matchers.equalTo(absoluteFilePath));
    Assert.assertThat(entry.getAbsolutePath(), Matchers.equalTo(absoluteFilePath));
    Assert.assertThat(entry.getFile(), Matchers.equalTo(absoluteFilePath.toFile()));
    Assert.assertThat(entry.getName(), Matchers.equalTo(absoluteFileName));
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

    new ExportMigrationEntryImpl(context, (Path) null);
  }

  @Test
  public void testConstructorWhenPathDoesNotExist() throws Exception {
    absoluteFilePath.toFile().delete();
    final ExportMigrationEntryImpl entry = new ExportMigrationEntryImpl(context, FILE_PATH);

    Assert.assertThat(entry.getContext(), Matchers.sameInstance(context));
    Assert.assertThat(entry.getPath(), Matchers.equalTo(FILE_PATH));
    Assert.assertThat(entry.getAbsolutePath(), Matchers.equalTo(FILE_PATH));
    Assert.assertThat(entry.getFile(), Matchers.equalTo(FILE_PATH.toFile()));
    Assert.assertThat(entry.getName(), Matchers.equalTo(UNIX_NAME));
  }

  @Test
  public void testConstructorWithRelativePathname() throws Exception {
    final ExportMigrationEntryImpl entry =
        new ExportMigrationEntryImpl(context, FILE_PATH.toString());

    Assert.assertThat(entry.getContext(), Matchers.sameInstance(context));
    Assert.assertThat(entry.getPath(), Matchers.equalTo(FILE_PATH));
    Assert.assertThat(entry.getAbsolutePath(), Matchers.equalTo(absoluteFilePath));
    Assert.assertThat(entry.getFile(), Matchers.equalTo(absoluteFilePath.toFile()));
    Assert.assertThat(entry.getName(), Matchers.equalTo(UNIX_NAME));
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

    new ExportMigrationEntryImpl(context, (String) null);
  }

  @Test
  public void testGetReport() throws Exception {
    Assert.assertThat(entry.getReport(), Matchers.sameInstance(report));

    Mockito.verify(context).getReport();
  }

  @Test
  public void testOutputStream() throws Exception {
    final OutputStream mockOutputStream = Mockito.mock(OutputStream.class);

    Mockito.when(context.getOutputStreamFor(Mockito.any())).thenReturn(mockOutputStream);

    final OutputStream os = entry.getOutputStream();

    Assert.assertThat(os, Matchers.sameInstance(mockOutputStream));

    Mockito.verify(context).getOutputStreamFor(Mockito.same(entry));
  }

  @Test
  public void testOutputStreamWhenAlreadyRetrieved() throws Exception {
    final OutputStream mockOutputStream = Mockito.mock(OutputStream.class);

    Mockito.when(context.getOutputStreamFor(Mockito.any())).thenReturn(mockOutputStream);

    entry.getOutputStream(); // pre-cache it

    final OutputStream os = entry.getOutputStream();

    Assert.assertThat(os, Matchers.sameInstance(mockOutputStream));

    Mockito.verify(context).getOutputStreamFor(Mockito.same(entry));
  }

  @Test
  public void testOutputStreamWithException() throws Exception {
    final IOException e = new IOException("testing");

    Mockito.when(context.getOutputStreamFor(Mockito.any())).thenThrow(new UncheckedIOException(e));

    thrown.expect(Matchers.sameInstance(e));

    entry.getOutputStream();
  }

  @Test
  public void testStoreWhenRequiredAndFileExist() throws Exception {
    final StringWriter writer = new StringWriter();

    Mockito.when(context.getOutputStreamFor(Mockito.any()))
        .thenReturn(new WriterOutputStream(writer, Charsets.UTF_8));

    Assert.assertThat(entry.store(true), Matchers.equalTo(true));
    Assert.assertThat(writer.toString(), Matchers.equalTo(FILENAME));
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
    Assert.assertThat(report.wasSuccessful(), Matchers.equalTo(true));
  }

  @Test
  public void testStoreWhenRequiredAndFileIsAbsoluteOutsideDDFHome() throws Exception {
    final StringWriter writer = new StringWriter();

    final Path absoluteFilePath = createFile(testFolder.getRoot().toPath().resolve(FILENAME));

    final ExportMigrationEntryImpl entry = new ExportMigrationEntryImpl(context, absoluteFilePath);

    Assert.assertThat(entry.store(true), Matchers.equalTo(true));
    Assert.assertThat(writer.toString(), Matchers.emptyString());
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(true));
    Assert.assertThat(report.wasSuccessful(), Matchers.equalTo(true));

    Assert.assertThat(
        report.warnings().map(MigrationWarning::getMessage).toArray(String[]::new),
        Matchers.hasItemInArray(Matchers.containsString("is outside")));
  }

  @Test
  public void testStoreWhenRequiredAndFileIsASoftLink() throws Exception {
    final StringWriter writer = new StringWriter();

    final String filename2 = "file2.ext";
    final Path absoluteFilePath2 =
        createSoftLink(absoluteFilePath.getParent(), filename2, absoluteFilePath);

    final ExportMigrationEntryImpl entry = new ExportMigrationEntryImpl(context, absoluteFilePath2);

    Assert.assertThat(entry.store(true), Matchers.equalTo(true));
    Assert.assertThat(writer.toString(), Matchers.emptyString());
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(true));
    Assert.assertThat(report.wasSuccessful(), Matchers.equalTo(true));

    Assert.assertThat(
        report.warnings().map(MigrationWarning::getMessage).toArray(String[]::new),
        Matchers.hasItemInArray(Matchers.containsString("is a symbolic link")));
  }

  @Test
  public void testStoreWhenOptionalAndFileExist() throws Exception {
    final StringWriter writer = new StringWriter();

    Mockito.when(context.getOutputStreamFor(Mockito.any()))
        .thenReturn(new WriterOutputStream(writer, Charsets.UTF_8));

    Assert.assertThat(entry.store(false), Matchers.equalTo(true));
    Assert.assertThat(writer.toString(), Matchers.equalTo(FILENAME));
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
    Assert.assertThat(report.wasSuccessful(), Matchers.equalTo(true));
  }

  @Test
  public void testStoreASecondTimeWhenFirstSucceeded() throws Exception {
    final StringWriter writer = new StringWriter();

    Mockito.when(context.getOutputStreamFor(Mockito.any()))
        .thenReturn(new WriterOutputStream(writer, Charsets.UTF_8));

    entry.store();
    // reset writer's buffer to make sure it will not be re-written
    writer.getBuffer().setLength(0);

    Assert.assertThat(entry.store(true), Matchers.equalTo(true));
    Assert.assertThat(writer.toString(), Matchers.emptyString());
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
    Assert.assertThat(report.wasSuccessful(), Matchers.equalTo(true));
  }

  @Test
  public void testStoreWhenRequiredAndFileDoesNotExist() throws Exception {
    absoluteFilePath.toFile().delete();
    entry = new ExportMigrationEntryImpl(context, FILE_PATH);
    final StringWriter writer = new StringWriter();

    Mockito.when(context.getOutputStreamFor(Mockito.any()))
        .thenReturn(new WriterOutputStream(writer, Charsets.UTF_8));

    Assert.assertThat(entry.store(true), Matchers.equalTo(false));
    Assert.assertThat(writer.toString(), Matchers.emptyString());
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(true));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
    Assert.assertThat(report.wasSuccessful(), Matchers.equalTo(false));

    thrown.expect(MigrationException.class);
    thrown.expectMessage(Matchers.containsString("does not exist"));

    report.verifyCompletion(); // to trigger an exception from the report
  }

  @Test
  public void testStoreWhenOptionalAndFileDoesNotExist() throws Exception {
    absoluteFilePath.toFile().delete();
    final ExportMigrationEntryImpl entry = new ExportMigrationEntryImpl(context, FILE_PATH);

    final StringWriter writer = new StringWriter();

    Mockito.when(context.getOutputStreamFor(Mockito.any()))
        .thenReturn(new WriterOutputStream(writer, Charsets.UTF_8));

    Assert.assertThat(entry.store(false), Matchers.equalTo(true));
    Assert.assertThat(writer.toString(), Matchers.emptyString());
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
    Assert.assertThat(report.wasSuccessful(), Matchers.equalTo(true));
  }

  @Test
  public void testStoreWhenRequiredAndFileRealPathCannotBeDetermined() throws Exception {
    final PathUtils pathUtils = Mockito.mock(PathUtils.class);
    final Path path = Mockito.mock(Path.class);
    final IOException ioe = new IOException("test");

    Mockito.when(context.getPathUtils()).thenReturn(pathUtils);
    Mockito.when(pathUtils.resolveAgainstDDFHome((Path) Mockito.any())).thenReturn(path);
    Mockito.when(pathUtils.relativizeFromDDFHome(Mockito.any())).thenReturn(path);
    Mockito.when(path.toRealPath(LinkOption.NOFOLLOW_LINKS)).thenThrow(ioe);

    final ExportMigrationEntryImpl entry = new ExportMigrationEntryImpl(context, FILE_PATH);

    final StringWriter writer = new StringWriter();

    Mockito.when(context.getOutputStreamFor(Mockito.any()))
        .thenReturn(new WriterOutputStream(writer, Charsets.UTF_8));

    Assert.assertThat(entry.store(true), Matchers.equalTo(false));
    Assert.assertThat(writer.toString(), Matchers.emptyString());
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(true));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
    Assert.assertThat(report.wasSuccessful(), Matchers.equalTo(false));

    thrown.expect(MigrationException.class);
    thrown.expectMessage(Matchers.containsString("cannot be read"));
    thrown.expectCause(Matchers.sameInstance(ioe));

    report.verifyCompletion(); // to trigger an exception from the report
  }

  @Test
  public void testStoreWhenPathIsADirectory() throws Exception {
    final StringWriter writer = new StringWriter();

    Mockito.when(context.getOutputStreamFor(Mockito.any()))
        .thenReturn(new WriterOutputStream(writer, Charsets.UTF_8));

    final ExportMigrationEntryImpl entry =
        new ExportMigrationEntryImpl(context, absoluteFilePath.getParent());

    Assert.assertThat(entry.store(true), Matchers.equalTo(false));
    Assert.assertThat(writer.toString(), Matchers.emptyString());
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(true));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
    Assert.assertThat(report.wasSuccessful(), Matchers.equalTo(false));

    thrown.expect(MigrationException.class);

    report.verifyCompletion(); // to trigger an exception from the report
  }

  @Test
  public void testStoreWithConsumer() throws Exception {
    final BiThrowingConsumer<MigrationReport, OutputStream, IOException> consumer =
        Mockito.mock(BiThrowingConsumer.class);
    final OutputStream mockOutputStream = Mockito.mock(OutputStream.class);

    Mockito.when(context.getOutputStreamFor(Mockito.any())).thenReturn(mockOutputStream);

    Assert.assertThat(entry.store(consumer), Matchers.equalTo(true));
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
    Assert.assertThat(report.wasSuccessful(), Matchers.equalTo(true));

    Mockito.verify(consumer).accept(Mockito.same(report), Mockito.same(mockOutputStream));
  }

  @Test
  public void testStoreWithConsumerReportingError() throws Exception {
    final BiThrowingConsumer<MigrationReport, OutputStream, IOException> consumer =
        Mockito.mock(BiThrowingConsumer.class);
    final OutputStream mockOutputStream = Mockito.mock(OutputStream.class);
    final MigrationException me = Mockito.mock(MigrationException.class);

    Mockito.when(context.getOutputStreamFor(Mockito.any())).thenReturn(mockOutputStream);
    Mockito.doAnswer(
            AdditionalAnswers.<MigrationReport, OutputStream>answerVoid((r, os) -> r.record(me)))
        .when(consumer)
        .accept(Mockito.any(), Mockito.any());

    Assert.assertThat(entry.store(consumer), Matchers.equalTo(false));
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(true));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
    Assert.assertThat(report.wasSuccessful(), Matchers.equalTo(false));

    Mockito.verify(consumer).accept(Mockito.same(report), Mockito.same(mockOutputStream));

    thrown.expect(Matchers.sameInstance(me));

    report.verifyCompletion(); // to trigger an exception from the report
  }

  @Test
  public void testStoreWithConsumerThrowingMigrationException() throws Exception {
    final BiThrowingConsumer<MigrationReport, OutputStream, IOException> consumer =
        Mockito.mock(BiThrowingConsumer.class);
    final BiThrowingConsumer<MigrationReport, OutputStream, IOException> consumer2 =
        Mockito.mock(BiThrowingConsumer.class);
    final OutputStream mockOutputStream = Mockito.mock(OutputStream.class);
    final MigrationException me = Mockito.mock(MigrationException.class);

    Mockito.when(context.getOutputStreamFor(Mockito.any())).thenReturn(mockOutputStream);
    Mockito.doThrow(me).when(consumer).accept(Mockito.any(), Mockito.any());

    thrown.expect(Matchers.sameInstance(me));

    try {
      entry.store(consumer);
    } finally {
      Mockito.verify(consumer).accept(Mockito.same(report), Mockito.same(mockOutputStream));

      // verify that if we were to store a second time, the consumer would not be called and false
      // would be returned
      Assert.assertThat(entry.store(), Matchers.equalTo(false));

      Mockito.verify(consumer2, Mockito.never()).accept(Mockito.any(), Mockito.any());
    }
  }

  @Test
  public void testStoreWithConsumerThrowingIOException() throws Exception {
    final BiThrowingConsumer<MigrationReport, OutputStream, IOException> consumer =
        Mockito.mock(BiThrowingConsumer.class);
    final OutputStream mockOutputStream = Mockito.mock(OutputStream.class);
    final IOException ioe = Mockito.mock(IOException.class);

    Mockito.when(context.getOutputStreamFor(Mockito.any())).thenReturn(mockOutputStream);
    Mockito.doThrow(ioe).when(consumer).accept(Mockito.any(), Mockito.any());

    Assert.assertThat(entry.store(consumer), Matchers.equalTo(false));
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(true));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
    Assert.assertThat(report.wasSuccessful(), Matchers.equalTo(false));

    Mockito.verify(consumer).accept(Mockito.same(report), Mockito.same(mockOutputStream));

    thrown.expect(Matchers.instanceOf(MigrationException.class));
    thrown.expectCause(Matchers.sameInstance(ioe));

    report.verifyCompletion(); // to trigger an exception from the report
  }

  @Test
  public void testStoreWithConsumerThrowingExportIOException() throws Exception {
    final BiThrowingConsumer<MigrationReport, OutputStream, IOException> consumer =
        Mockito.mock(BiThrowingConsumer.class);
    final BiThrowingConsumer<MigrationReport, OutputStream, IOException> consumer2 =
        Mockito.mock(BiThrowingConsumer.class);
    final OutputStream mockOutputStream = Mockito.mock(OutputStream.class);
    final IOException ioe = Mockito.mock(IOException.class);
    final ExportIOException eioe = new ExportIOException(ioe);

    Mockito.when(context.getOutputStreamFor(Mockito.any())).thenReturn(mockOutputStream);
    Mockito.doThrow(eioe).when(consumer).accept(Mockito.any(), Mockito.any());

    thrown.expect(Matchers.instanceOf(MigrationException.class));
    thrown.expectCause(Matchers.sameInstance(ioe));

    try {
      entry.store(consumer);
    } finally {
      Mockito.verify(consumer).accept(Mockito.same(report), Mockito.same(mockOutputStream));

      // verify that if we were to store a second time, the consumer would not be called and false
      // would be returned
      Assert.assertThat(entry.store(), Matchers.equalTo(false));

      Mockito.verify(consumer2, Mockito.never()).accept(Mockito.any(), Mockito.any());
    }
  }

  @Test
  public void testGetPropertyReferencedEntryWhenValueIsRelative() throws Exception {
    storeProperty(PROPERTY_NAME, MIGRATABLE_PROPERTY_PATHNAME);

    createDirectory(MIGRATABLE_NAME_DIRS);
    createFile(MIGRATABLE_NAME);

    final Optional<ExportMigrationEntry> oentry =
        entry.getPropertyReferencedEntry(PROPERTY_NAME, (r, v) -> true);

    Assert.assertThat(oentry, OptionalMatchers.isPresent());
    final ExportMigrationEntry entry = oentry.get();

    Assert.assertThat(entry.getId(), Matchers.equalTo(MIGRATABLE_ID));
    Assert.assertThat(entry.getName(), Matchers.equalTo(MIGRATABLE_NAME));
    Assert.assertThat(entry.getPath(), Matchers.equalTo(MIGRATABLE_PATH));
    // now check that it is a java property referenced entry that references the proper property
    // name
    Assert.assertThat(
        entry, Matchers.instanceOf(ExportMigrationJavaPropertyReferencedEntryImpl.class));
    final ExportMigrationJavaPropertyReferencedEntryImpl jentry =
        (ExportMigrationJavaPropertyReferencedEntryImpl) entry;

    Assert.assertThat(jentry.getProperty(), Matchers.equalTo(PROPERTY_NAME));
    // finally make sure no warnings or errors were recorded
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
  }

  @Test
  public void testGetPropertyReferencedEntryWhenValueIsAbsoluteUnderDDFHome() throws Exception {
    storeProperty(PROPERTY_NAME, ddfHome.resolve(MIGRATABLE_PATH).toAbsolutePath().toString());

    createDirectory(MIGRATABLE_NAME_DIRS);
    createFile(MIGRATABLE_NAME);

    final Optional<ExportMigrationEntry> oentry =
        entry.getPropertyReferencedEntry(PROPERTY_NAME, (r, v) -> true);

    Assert.assertThat(oentry, OptionalMatchers.isPresent());
    final ExportMigrationEntry entry = oentry.get();

    Assert.assertThat(entry.getId(), Matchers.equalTo(MIGRATABLE_ID));
    Assert.assertThat(entry.getName(), Matchers.equalTo(MIGRATABLE_NAME));
    Assert.assertThat(entry.getPath(), Matchers.equalTo(MIGRATABLE_PATH));
    // now check that it is a java property referenced entry that references the proper property
    // name
    Assert.assertThat(
        entry, Matchers.instanceOf(ExportMigrationJavaPropertyReferencedEntryImpl.class));
    final ExportMigrationJavaPropertyReferencedEntryImpl jentry =
        (ExportMigrationJavaPropertyReferencedEntryImpl) entry;

    Assert.assertThat(jentry.getProperty(), Matchers.equalTo(PROPERTY_NAME));
    // finally make sure no warnings or errors were recorded
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
  }

  @Test
  public void testGetPropertyReferencedEntryWhenValueIsAbsoluteNotUnderDDFHome() throws Exception {
    final Path migratablePath =
        testFolder.newFile("test.cfg").toPath().toRealPath(LinkOption.NOFOLLOW_LINKS);
    final String migratableName = FilenameUtils.separatorsToUnix(migratablePath.toString());

    storeProperty(PROPERTY_NAME, migratablePath.toAbsolutePath().toString());

    final Optional<ExportMigrationEntry> oentry =
        entry.getPropertyReferencedEntry(PROPERTY_NAME, (r, v) -> true);

    Assert.assertThat(oentry, OptionalMatchers.isPresent());
    final ExportMigrationEntry entry = oentry.get();

    Assert.assertThat(entry.getId(), Matchers.equalTo(MIGRATABLE_ID));
    Assert.assertThat(entry.getName(), Matchers.equalTo(migratableName));
    Assert.assertThat(entry.getPath(), Matchers.equalTo(migratablePath));
    // now check that it is a java property referenced entry that references the proper property
    // name
    Assert.assertThat(
        entry, Matchers.instanceOf(ExportMigrationJavaPropertyReferencedEntryImpl.class));
    final ExportMigrationJavaPropertyReferencedEntryImpl jentry =
        (ExportMigrationJavaPropertyReferencedEntryImpl) entry;

    Assert.assertThat(jentry.getProperty(), Matchers.equalTo(PROPERTY_NAME));
    // finally make sure no warnings or errors were recorded
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
  }

  @Test
  public void testGetPropertyReferencedEntryWithNullName() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null java property name"));

    entry.getPropertyReferencedEntry(null, (r, v) -> true);
  }

  @Test
  public void testGetPropertyReferencedEntryWithNullValidator() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null validator"));

    entry.getPropertyReferencedEntry(PROPERTY_NAME, null);
  }

  @Test
  public void testGetPropertyReferencedEntryWhenAlreadyCached() throws Exception {
    storeProperty(PROPERTY_NAME, MIGRATABLE_PROPERTY_PATHNAME);
    final ExportMigrationEntry jentry =
        entry.getPropertyReferencedEntry(PROPERTY_NAME, (r, v) -> true).get();

    final Optional<ExportMigrationEntry> oentry =
        entry.getPropertyReferencedEntry(PROPERTY_NAME, (r, v) -> true);

    Assert.assertThat(oentry, OptionalMatchers.isPresent());
    final ExportMigrationEntry entry = oentry.get();

    Assert.assertThat(entry, Matchers.sameInstance(jentry));
    // finally make sure no warnings or errors were recorded
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
  }

  @Test
  public void testGetPropertyReferencedEntryWhenInvalid() throws Exception {
    storeProperty(PROPERTY_NAME, MIGRATABLE_PROPERTY_PATHNAME);
    final Optional<ExportMigrationEntry> oentry =
        entry.getPropertyReferencedEntry(PROPERTY_NAME, (r, v) -> false);

    Assert.assertThat(oentry, OptionalMatchers.isEmpty());
  }

  @Test
  public void testGetPropertyReferencedEntryWhenPropertyIsNotDefined() throws Exception {
    storeProperty(PROPERTY_NAME, MIGRATABLE_PROPERTY_PATHNAME);
    final Optional<ExportMigrationEntry> oentry =
        entry.getPropertyReferencedEntry(PROPERTY_NAME2, (r, v) -> true);

    Assert.assertThat(oentry, OptionalMatchers.isEmpty());
    // finally make sure we got an error (register the thrown expectations after the above to make
    // sure
    // we don't get an exception from the above code under test
    thrown.expect(MigrationException.class);
    thrown.expectMessage(
        Matchers.containsString(
            "Java property [" + PROPERTY_NAME2 + "] from file [" + FILE_PATH + "] is not defined"));

    report.verifyCompletion(); // to get the exception thrown out
  }

  @Test
  public void testGetPropertyReferencedEntryWhenPropertyValueIsEmpty() throws Exception {
    storeProperty(PROPERTY_NAME2, "");

    final Optional<ExportMigrationEntry> oentry =
        entry.getPropertyReferencedEntry(PROPERTY_NAME2, (r, v) -> true);

    Assert.assertThat(oentry, OptionalMatchers.isEmpty());
    // finally make sure we got an error (register the thrown expectations after the above to make
    // sure
    // we don't get an exception from the above code under test
    thrown.expect(MigrationException.class);
    thrown.expectMessage(
        Matchers.containsString(
            "Java property [" + PROPERTY_NAME2 + "] from file [" + FILE_PATH + "] is empty"));

    report.verifyCompletion(); // to get the exception thrown out
  }

  @Test
  public void testGetPropertyReferencedEntryWhenUnableToReadPropertyValue() throws Exception {
    absoluteFilePath.toFile().delete();

    final Optional<ExportMigrationEntry> oentry =
        entry.getPropertyReferencedEntry(PROPERTY_NAME, (r, v) -> true);

    Assert.assertThat(oentry, OptionalMatchers.isEmpty());
    // finally make sure we got an error (register the thrown expectations after the above to make
    // sure
    // we don't get an exception from the above code under test
    thrown.expect(MigrationException.class);
    thrown.expectMessage(
        Matchers.containsString(
            "Java property ["
                + PROPERTY_NAME
                + "] from file ["
                + FILE_PATH
                + "] could not be retrieved"));

    report.verifyCompletion(); // to get the exception thrown out
  }

  @Test
  public void testToDebugString() throws Exception {
    final String debug = entry.toDebugString();

    Assert.assertThat(debug, Matchers.containsString("file"));
    Assert.assertThat(debug, Matchers.containsString("[" + FILE_PATH + "]"));
  }

  @Test
  public void testNewWarning() throws Exception {
    final String reason = "test reason";
    final MigrationWarning warning = entry.newWarning(reason);

    Assert.assertThat(warning.getMessage(), Matchers.containsString("[" + FILE_PATH + "]"));
    Assert.assertThat(warning.getMessage(), Matchers.containsString(reason));
  }

  @Test
  public void testNewError() throws Exception {
    final String reason = "test reason";
    final IllegalArgumentException cause = new IllegalArgumentException("test cause");
    final MigrationException error = entry.newError(reason, cause);

    Assert.assertThat(error.getMessage(), Matchers.containsString("[" + FILE_PATH + "]"));
    Assert.assertThat(error.getMessage(), Matchers.containsString(reason));
    Assert.assertThat(error.getCause(), Matchers.sameInstance(cause));
  }

  @Test
  public void testEqualsWhenEquals() throws Exception {
    final ExportMigrationEntryImpl entry2 = new ExportMigrationEntryImpl(context, FILE_PATH);

    Assert.assertThat(entry.equals(entry2), Matchers.equalTo(true));
  }

  @Test
  public void testEqualsWhenIdentical() throws Exception {
    Assert.assertThat(entry.equals(entry), Matchers.equalTo(true));
  }

  // PMD.EqualsNull - Purposely testing equals() when called with null
  @SuppressWarnings("PMD.EqualsNull")
  @Test
  public void testEqualsWhenNull() throws Exception {
    Assert.assertThat(entry.equals(null), Matchers.equalTo(false));
  }

  // PMD.PositionLiteralsFirstInComparisons - Purposely testing equals() when call with something
  // else than expected
  @SuppressWarnings("PMD.PositionLiteralsFirstInComparisons")
  @Test
  public void testEqualsWithNotAnEntry() throws Exception {
    Assert.assertThat(entry.equals("test"), Matchers.equalTo(false));
  }

  @Test
  public void testEqualsWhenContextsAreDifferent() throws Exception {
    final ExportMigrationContextImpl context2 = Mockito.mock(ExportMigrationContextImpl.class);

    Mockito.when(context2.getPathUtils()).thenReturn(pathUtils);
    Mockito.when(context2.getReport()).thenReturn(report);
    Mockito.when(context2.getId()).thenReturn(MIGRATABLE_ID);

    final ExportMigrationEntryImpl entry2 = new ExportMigrationEntryImpl(context2, FILE_PATH);

    Assert.assertThat(entry.equals(entry2), Matchers.equalTo(false));
  }

  @Test
  public void testEqualsWhenPathsAreDifferent() throws Exception {
    final ExportMigrationEntryImpl entry2 =
        new ExportMigrationEntryImpl(context, FILE_PATH.getParent());

    Assert.assertThat(entry.equals(entry2), Matchers.equalTo(false));
  }

  @Test
  public void testHashCodeWhenEquals() throws Exception {
    final ExportMigrationEntryImpl entry2 = new ExportMigrationEntryImpl(context, FILE_PATH);

    Assert.assertThat(entry.hashCode(), Matchers.equalTo(entry2.hashCode()));
  }

  @Test
  public void testHashCodeWhenDifferent() throws Exception {
    final ExportMigrationEntryImpl entry2 =
        new ExportMigrationEntryImpl(context, FILE_PATH.getParent());

    Assert.assertThat(entry.hashCode(), Matchers.not(Matchers.equalTo(entry2.hashCode())));
  }

  @Test
  public void testGetLastModifiedTime() throws Exception {
    final PathUtils pathUtils = Mockito.mock(PathUtils.class);
    final Path filePath = Mockito.mock(Path.class);
    final File file = Mockito.mock(File.class);
    final long modified = 12345L;

    Mockito.when(context.getPathUtils()).thenReturn(pathUtils);
    Mockito.when(pathUtils.resolveAgainstDDFHome(filePath)).thenReturn(filePath);
    Mockito.when(filePath.toRealPath(Mockito.any())).thenReturn(filePath);
    Mockito.when(pathUtils.relativizeFromDDFHome(filePath)).thenReturn(filePath);
    Mockito.when(filePath.toString()).thenReturn(UNIX_NAME);
    Mockito.when(filePath.toFile()).thenReturn(file);
    Mockito.when(file.lastModified()).thenReturn(modified);

    final ExportMigrationEntryImpl entry = new ExportMigrationEntryImpl(context, filePath);

    Assert.assertThat(entry.getLastModifiedTime(), Matchers.equalTo(modified));

    Mockito.verify(file).lastModified();
  }

  private void storeProperty(String name, String val) throws IOException {
    final Properties props = new Properties();

    props.put(name, val);
    try (final Writer writer = new BufferedWriter(new FileWriter(absoluteFilePath.toFile()))) {
      props.store(writer, "testing");
    }
  }
}
