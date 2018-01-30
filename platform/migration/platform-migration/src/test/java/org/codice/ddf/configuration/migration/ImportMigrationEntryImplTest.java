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

import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Charsets;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.function.Function;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.migration.ImportMigrationEntry;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationOperation;
import org.codice.ddf.migration.MigrationReport;
import org.codice.ddf.util.function.BiThrowingConsumer;
import org.codice.ddf.util.function.ThrowingRunnable;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ImportMigrationEntryImplTest extends AbstractMigrationSupport {

  private static final long MODIFIED_TIME = 13243546L;

  public static final String IMPORT_CONTENTS = "import contents";

  public static final String ENTRY_NAME = "test_name";

  public Path absolutePath;

  @Mock public ImportMigrationContextImpl mockContext;

  @Mock public ImportMigrationContextImpl mockBlankContext;

  @Mock public PathUtils mockPathUtils;

  public Function<String, ImportMigrationContextImpl> getContextFunction =
      (n) -> n == null ? mockBlankContext : mockContext;

  public MigrationReport report;

  public File importedFile;

  @Before
  public void setup() throws Exception {
    PathUtils pathUtils = new PathUtils();
    when(mockContext.getPathUtils()).thenReturn(pathUtils);
    when(mockBlankContext.getPathUtils()).thenReturn(pathUtils);

    givenARealMigrationReport();

    importedFile = ddfHome.resolve(createFile(ENTRY_NAME)).toFile();
    // resolving against DDF_HOME ensures that on Windows the absolute path gets the same drive as
    // DDF_HOME
    absolutePath = ddfHome.resolve(Paths.get("/opt", "ddf", ENTRY_NAME));
  }

  @Test
  public void constructorWithZipEntry() throws Exception {
    givenMockedPathUtils();
    final java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry("migratable/" + ENTRY_NAME);

    zipEntry.setTime(MODIFIED_TIME);
    final ImportMigrationEntryImpl entry =
        new ImportMigrationEntryImpl(getContextFunction, zipEntry);

    assertThat(entry.getContext(), sameInstance(mockContext));
    verifyNameAndPathInformation(entry, mockContext);
    assertThat(entry.isFile(), equalTo(true));
    assertThat(entry.isDirectory(), equalTo(false));
    assertThat(entry.getLastModifiedTime(), equalTo(MODIFIED_TIME));
  }

  @Test
  public void constructorWithZipEntryForSystemEntry() throws Exception {
    givenMockedPathUtils();
    final java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry(ENTRY_NAME);

    final ImportMigrationEntryImpl entry =
        new ImportMigrationEntryImpl(getContextFunction, zipEntry);

    assertThat(entry.getContext(), sameInstance(mockBlankContext));
    verifyNameAndPathInformation(entry, mockBlankContext);
    assertThat(entry.isFile(), equalTo(true));
    assertThat(entry.isDirectory(), equalTo(false));
  }

  @Test
  public void constructorWithNameWhenAFile() throws Exception {
    givenMockedPathUtils();

    final ImportMigrationEntryImpl entry =
        new ImportMigrationEntryImpl(mockContext, ENTRY_NAME, true);

    assertThat(entry.getContext(), sameInstance(mockContext));
    verifyNameAndPathInformation(entry, mockContext);
    assertThat(entry.isFile(), equalTo(true));
    assertThat(entry.isDirectory(), equalTo(false));
    assertThat(entry.getLastModifiedTime(), equalTo(-1L));
  }

  @Test
  public void constructorWithNameWhenADirectory() throws Exception {
    givenMockedPathUtils();

    final ImportMigrationEntryImpl entry =
        new ImportMigrationEntryImpl(mockContext, ENTRY_NAME, false);

    assertThat(entry.getContext(), sameInstance(mockContext));
    verifyNameAndPathInformation(entry, mockContext);
    assertThat(entry.isFile(), equalTo(false));
    assertThat(entry.isDirectory(), equalTo(true));
    assertThat(entry.getLastModifiedTime(), equalTo(-1L));
  }

  @Test
  public void constructorWithPathWhenAFile() throws Exception {
    givenMockedPathUtils();

    final ImportMigrationEntryImpl entry =
        new ImportMigrationEntryImpl(mockContext, Paths.get(ENTRY_NAME), true);

    assertThat(entry.getContext(), sameInstance(mockContext));
    verifyNameAndPathInformation(entry, mockContext);
    assertThat(entry.isFile(), equalTo(true));
    assertThat(entry.isDirectory(), equalTo(false));
    assertThat(entry.getLastModifiedTime(), equalTo(-1L));
  }

  @Test
  public void constructorWithPathWhenADirectory() throws Exception {
    givenMockedPathUtils();

    final ImportMigrationEntryImpl entry =
        new ImportMigrationEntryImpl(mockContext, Paths.get(ENTRY_NAME), false);

    assertThat(entry.getContext(), sameInstance(mockContext));
    verifyNameAndPathInformation(entry, mockContext);
    assertThat(entry.isFile(), equalTo(false));
    assertThat(entry.isDirectory(), equalTo(true));
    assertThat(entry.getLastModifiedTime(), equalTo(-1L));
  }

  @Test
  public void getInputStream() throws Exception {
    final java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry("migratable/" + ENTRY_NAME);
    final ImportMigrationEntryImpl entry =
        new ImportMigrationEntryImpl(getContextFunction, zipEntry);
    InputStream mockInputStream = mock(InputStream.class);

    when(mockContext.getInputStreamFor(Mockito.same(entry), Mockito.anyBoolean()))
        .thenReturn(mockInputStream);

    Optional<InputStream> optionalInput = entry.getInputStream();

    assertThat(optionalInput, isPresent());
    assertThat(optionalInput.get(), sameInstance(mockInputStream));
  }

  @Test
  public void getLastModifiedTime() {
    givenMockedPathUtils();
    final java.util.zip.ZipEntry mockZipEntry = mock(java.util.zip.ZipEntry.class);
    when(mockZipEntry.getTime()).thenReturn(10L);
    when(mockZipEntry.getName()).thenReturn(ENTRY_NAME);

    final ImportMigrationEntryImpl entry =
        new ImportMigrationEntryImpl(getContextFunction, mockZipEntry);

    assertThat(entry.getLastModifiedTime(), equalTo(10L));
  }

  @Test
  public void restoreWhenRequired() throws Exception {
    InputStream inputStream = IOUtils.toInputStream(IMPORT_CONTENTS, Charsets.UTF_8);
    final ImportMigrationEntryImpl entry =
        new ImportMigrationEntryImpl(mockContext, importedFile.getName(), true);

    when(mockContext.getInputStreamFor(Mockito.same(entry), Mockito.anyBoolean()))
        .thenReturn(inputStream);

    assertThat(entry.restore(true), equalTo(true));

    assertThat(FileUtils.readFileToString(importedFile, Charsets.UTF_8), equalTo(IMPORT_CONTENTS));
  }

  @Test
  public void restoreWhenOptional() throws Exception {
    InputStream inputStream = IOUtils.toInputStream(IMPORT_CONTENTS, Charsets.UTF_8);
    final ImportMigrationEntryImpl entry =
        new ImportMigrationEntryImpl(mockContext, importedFile.getName(), true);

    when(mockContext.getInputStreamFor(Mockito.same(entry), Mockito.anyBoolean()))
        .thenReturn(inputStream);

    assertThat(entry.restore(false), equalTo(true));

    assertThat(FileUtils.readFileToString(importedFile, Charsets.UTF_8), equalTo(IMPORT_CONTENTS));
  }

  @Test
  public void restoreWithFilterWhenRequiredAndMatching() throws Exception {
    InputStream inputStream = IOUtils.toInputStream(IMPORT_CONTENTS, Charsets.UTF_8);
    final ImportMigrationEntryImpl entry =
        new ImportMigrationEntryImpl(mockContext, importedFile.getName(), true);

    when(mockContext.getInputStreamFor(Mockito.same(entry), Mockito.anyBoolean()))
        .thenReturn(inputStream);

    assertThat(entry.restore(true, p -> true), equalTo(true));

    assertThat(FileUtils.readFileToString(importedFile, Charsets.UTF_8), equalTo(IMPORT_CONTENTS));
  }

  @Test
  public void restoreWithFilterWhenOptionalAndMatching() throws Exception {
    InputStream inputStream = IOUtils.toInputStream(IMPORT_CONTENTS, Charsets.UTF_8);
    final ImportMigrationEntryImpl entry =
        new ImportMigrationEntryImpl(mockContext, importedFile.getName(), true);

    when(mockContext.getInputStreamFor(Mockito.same(entry), Mockito.anyBoolean()))
        .thenReturn(inputStream);

    assertThat(entry.restore(false, p -> true), equalTo(true));

    assertThat(FileUtils.readFileToString(importedFile, Charsets.UTF_8), equalTo(IMPORT_CONTENTS));
  }

  @Test
  public void restoreWithFilterWhenRequiredAndNotMatching() throws Exception {
    final ImportMigrationEntryImpl entry =
        new ImportMigrationEntryImpl(mockContext, importedFile.getName(), true);

    importedFile.delete();

    assertThat(entry.restore(true, p -> false), equalTo(false));

    assertThat(importedFile.exists(), equalTo(false));

    verify(mockContext, Mockito.never())
        .getInputStreamFor(Mockito.same(entry), Mockito.anyBoolean());
  }

  @Test
  public void restoreWithFilterWhenOptionalAndNotMatching() throws Exception {
    final ImportMigrationEntryImpl entry =
        new ImportMigrationEntryImpl(mockContext, importedFile.getName(), true);

    importedFile.delete();

    assertThat(entry.restore(false, p -> false), equalTo(true));

    assertThat(importedFile.exists(), equalTo(false));

    verify(mockContext, Mockito.never())
        .getInputStreamFor(Mockito.same(entry), Mockito.anyBoolean());
  }

  @Test
  public void restoreWhenRequiredEntryWasNotImported() throws Exception {
    final ImportMigrationEntryImpl entry =
        new ImportMigrationEntryImpl(mockContext, ENTRY_NAME, true);

    when(mockContext.getInputStreamFor(Mockito.same(entry), Mockito.anyBoolean())).thenReturn(null);

    assertThat(entry.restore(true), equalTo(false));

    verifyReportHasMatchingError(report, "was not exported");
  }

  @Test
  public void restoreWithFilterWhenRequiredEntryWasNotImportedAndMatching() throws Exception {
    final ImportMigrationEntryImpl entry =
        new ImportMigrationEntryImpl(mockContext, ENTRY_NAME, true);

    when(mockContext.getInputStreamFor(Mockito.same(entry), Mockito.anyBoolean())).thenReturn(null);

    assertThat(entry.restore(true, p -> true), equalTo(false));

    verifyReportHasMatchingError(report, "was not exported");
  }

  @Test
  public void restoreWithFilterWhenRequiredEntryWasNotImportedAndNotMatching() throws Exception {
    final ImportMigrationEntryImpl entry =
        new ImportMigrationEntryImpl(mockContext, ENTRY_NAME, true);

    importedFile.delete();

    assertThat(entry.restore(true, p -> false), equalTo(false));

    assertThat(importedFile.exists(), equalTo(false));

    verify(mockContext, Mockito.never())
        .getInputStreamFor(Mockito.same(entry), Mockito.anyBoolean());
  }

  @Test
  public void restoreWhenOptionalEntryWasNotImported() throws Exception {
    final ImportMigrationEntryImpl entry =
        new ImportMigrationEntryImpl(mockContext, importedFile.getName(), true);

    when(mockContext.getInputStreamFor(Mockito.same(entry), Mockito.anyBoolean())).thenReturn(null);

    assertThat(entry.restore(false), equalTo(true));

    assertThat("The file has not been deleted.", importedFile.exists(), is(false));
  }

  @Test
  public void restoreWithFilterWhenOptionalEntryWasNotImportedAndMatching() throws Exception {
    final ImportMigrationEntryImpl entry =
        new ImportMigrationEntryImpl(mockContext, importedFile.getName(), true);

    when(mockContext.getInputStreamFor(Mockito.same(entry), Mockito.anyBoolean())).thenReturn(null);

    assertThat(entry.restore(false, p -> true), equalTo(true));

    assertThat("The file has not been deleted.", importedFile.exists(), is(false));
  }

  @Test
  public void restoreWithFilterWhenOptionalEntryWasNotImportedAndNotMatching() throws Exception {
    final ImportMigrationEntryImpl entry =
        new ImportMigrationEntryImpl(mockContext, importedFile.getName(), true);

    assertThat(entry.restore(false, p -> false), equalTo(true));

    assertThat("The file has been deleted.", importedFile.exists(), is(true));
  }

  @Test
  public void restoreWhenFileIsReadOnly() throws Exception {
    final Path importedPath = Paths.get(importedFile.getName());
    final PathUtils pathUtils = mock(PathUtils.class);
    final Path absolutePath = mock(Path.class);
    final File absoluteFile = spy(new File(importedFile.getPath()));
    InputStream inputStream = IOUtils.toInputStream(IMPORT_CONTENTS, Charsets.UTF_8);

    when(mockContext.getPathUtils()).thenReturn(pathUtils);
    when(pathUtils.resolveAgainstDDFHome(Mockito.eq(importedPath))).thenReturn(absolutePath);
    when(absolutePath.toFile()).thenReturn(absoluteFile);

    final ImportMigrationEntryImpl entry =
        new ImportMigrationEntryImpl(mockContext, importedFile.getName(), true);

    when(mockContext.getInputStreamFor(Mockito.same(entry), Mockito.anyBoolean()))
        .thenReturn(inputStream);
    when(absoluteFile.canWrite()).thenReturn(false, false, true);
    // must reverse mockito calls otherwise using when() will actually call the setWritable() on the
    // spy and affect the file system
    doReturn(true).when(absoluteFile).setWritable(true);
    doReturn(true).when(absoluteFile).setWritable(false);

    entry.restore(true);

    final InOrder inOrder = inOrder(absoluteFile);

    inOrder.verify(absoluteFile).setWritable(true);
    inOrder.verify(absoluteFile).setWritable(false);

    assertThat(FileUtils.readFileToString(importedFile, Charsets.UTF_8), equalTo(IMPORT_CONTENTS));
  }

  @Test
  public void restoreWhenFileIsReadOnlyAndCannotMakeItWriteable() throws Exception {
    final Path importedPath = Paths.get(importedFile.getName());
    final PathUtils pathUtils = mock(PathUtils.class);
    final Path absolutePath = mock(Path.class);
    final File absoluteFile = spy(new File(importedFile.getPath()));
    InputStream inputStream = IOUtils.toInputStream(IMPORT_CONTENTS, Charsets.UTF_8);
    MigrationReport mockReport = mock(MigrationReport.class);
    when(mockReport.wasIOSuccessful(any(ThrowingRunnable.class)))
        .thenAnswer(
            it -> {
              ((ThrowingRunnable) it.getArgument(0)).run();
              throw new IOException();
            });
    when(mockContext.getReport()).thenReturn(mockReport);

    when(mockContext.getPathUtils()).thenReturn(pathUtils);
    when(pathUtils.resolveAgainstDDFHome(Mockito.eq(importedPath))).thenReturn(absolutePath);
    when(absolutePath.toFile()).thenReturn(absoluteFile);

    final ImportMigrationEntryImpl entry =
        new ImportMigrationEntryImpl(mockContext, importedFile.getName(), true);

    when(mockContext.getInputStreamFor(Mockito.same(entry), Mockito.anyBoolean()))
        .thenReturn(inputStream);
    when(absoluteFile.canWrite()).thenReturn(false, false, true);
    // must reverse mockito calls otherwise using when() will actually call the setWritable() on the
    // spy and affect the file system
    doReturn(false).when(absoluteFile).setWritable(true);

    entry.restore(true);

    final InOrder inOrder = inOrder(absoluteFile);

    inOrder.verify(absoluteFile).setWritable(true);

    assertThat("The restored flag is set to false", entry.restored, is(false));
    verify(mockReport).record(any(MigrationException.class));
  }

  @Test
  public void testRestoreWithFilterWhenFilterIsNull() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null path filter"));

    final ImportMigrationEntryImpl entry =
        new ImportMigrationEntryImpl(mockContext, ENTRY_NAME, true);

    entry.restore(true, null);
  }

  @Test
  public void testRestoreWithFilterVerifyingFilterReceivingEntryPath() throws Exception {
    final PathMatcher filter = mock(PathMatcher.class);

    final ImportMigrationEntryImpl entry =
        new ImportMigrationEntryImpl(mockContext, ENTRY_NAME, true);

    when(filter.matches(entry.getPath())).thenReturn(false);

    entry.restore(false, filter);

    verify(filter).matches(entry.getPath());
  }

  @Test
  public void restoreWithConsumer() throws Exception {
    MigrationReport mockReport = mock(MigrationReport.class);
    when(mockReport.wasIOSuccessful(any(ThrowingRunnable.class)))
        .thenAnswer(
            it -> {
              ((ThrowingRunnable) it.getArgument(0)).run();
              return true;
            });
    when(mockContext.getReport()).thenReturn(mockReport);
    BiThrowingConsumer<MigrationReport, Optional<InputStream>, IOException> mockConsumer =
        mock(BiThrowingConsumer.class);
    final ImportMigrationEntryImpl entry =
        new ImportMigrationEntryImpl(mockContext, ENTRY_NAME, true);

    when(mockContext.getInputStreamFor(Mockito.same(entry), Mockito.anyBoolean()))
        .thenReturn(mock(InputStream.class));

    entry.restore(mockConsumer);

    assertThat("The restored flag is set to true", entry.restored, is(true));
    verify(mockConsumer).accept(any(MigrationReport.class), any(Optional.class));
  }

  @Test
  public void restoreWithConsumerHandlesIOException() throws Exception {
    MigrationReport mockReport = mock(MigrationReport.class);
    when(mockReport.wasIOSuccessful(any(ThrowingRunnable.class)))
        .thenAnswer(
            it -> {
              ((ThrowingRunnable) it.getArgument(0)).run();
              throw new IOException();
            });
    when(mockContext.getReport()).thenReturn(mockReport);
    BiThrowingConsumer<MigrationReport, Optional<InputStream>, IOException> mockConsumer =
        mock(BiThrowingConsumer.class);
    final ImportMigrationEntryImpl entry =
        new ImportMigrationEntryImpl(mockContext, ENTRY_NAME, true);

    when(mockContext.getInputStreamFor(Mockito.same(entry), Mockito.anyBoolean()))
        .thenReturn(mock(InputStream.class));

    entry.restore(mockConsumer);

    assertThat("The restored flag is set to false", entry.restored, is(false));
    verify(mockConsumer).accept(any(MigrationReport.class), any(Optional.class));
    verify(mockReport).record(any(MigrationException.class));
  }

  @Test
  public void getPropertyReferencedEntry() {
    final ImportMigrationEntryImpl entry =
        new ImportMigrationEntryImpl(mockContext, ENTRY_NAME, true);
    ImportMigrationJavaPropertyReferencedEntryImpl mockRefEntry =
        mock(ImportMigrationJavaPropertyReferencedEntryImpl.class);
    entry.addPropertyReferenceEntry(ENTRY_NAME, mockRefEntry);

    final Optional<ImportMigrationEntry> optionalEntry =
        entry.getPropertyReferencedEntry(ENTRY_NAME);
    assertThat(optionalEntry, isPresent());
    assertThat(optionalEntry.get(), sameInstance(mockRefEntry));
  }

  @Test
  public void isMigratable() {
    final ImportMigrationEntryImpl entry =
        new ImportMigrationEntryImpl(mockContext, ENTRY_NAME, true);
    assertThat("The entry is migratable.", entry.isMigratable(), is(true));
  }

  @Test
  public void isNotMigratableWithAbsolutePath() {
    // resolving against DDF_HOME ensures that the absolute path gets the same drive on windows
    final ImportMigrationEntryImpl entry =
        new ImportMigrationEntryImpl(
            mockContext,
            ddfHome.resolve(Paths.get(File.separatorChar + ENTRY_NAME)).toString(),
            true);

    assertThat("The entry is not migratable.", entry.isMigratable(), is(false));
    reportHasWarningWithMessage(entry.getReport(), "is outside");
  }

  @Test
  public void isNotMigratableWithSymbolicLink() throws Exception {
    Path symlink = createSoftLink("symbolic-link", Paths.get("path1/"));

    final ImportMigrationEntryImpl entry = new ImportMigrationEntryImpl(mockContext, symlink, true);

    assertThat("The entry is not migratable.", entry.isMigratable(), is(false));
    reportHasWarningWithMessage(entry.getReport(), "symbolic link");
  }

  private void givenMockedPathUtils() {
    when(mockContext.getPathUtils()).thenReturn(mockPathUtils);
    when(mockBlankContext.getPathUtils()).thenReturn(mockPathUtils);
    when(mockPathUtils.resolveAgainstDDFHome(any(Path.class))).thenReturn(absolutePath);
  }

  private void givenARealMigrationReport() {
    report = new MigrationReportImpl(MigrationOperation.IMPORT, Optional.empty());
    when(mockContext.getReport()).thenReturn(report);
  }

  private void verifyNameAndPathInformation(
      ImportMigrationEntryImpl entry, ImportMigrationContextImpl expectedContext)
      throws IOException {
    assertThat(entry.getName(), equalTo(ENTRY_NAME));
    assertThat(entry.getPath().toString(), equalTo(ENTRY_NAME));
    assertThat(entry.getAbsolutePath(), sameInstance(absolutePath));
    assertThat(entry.getFile().getAbsolutePath(), equalTo(absolutePath.toString()));
    verify(expectedContext).getPathUtils();
    verify(mockPathUtils).resolveAgainstDDFHome(any(Path.class));
  }

  private void reportHasWarningWithMessage(MigrationReport report, String message) {
    report
        .warnings()
        .filter((w) -> w.getMessage().contains(message))
        .findFirst()
        .orElseThrow(
            () -> new AssertionError("There is no matching warning in the migration report"));
  }
}
