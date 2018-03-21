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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.io.FileUtils;
import org.codice.ddf.migration.MigrationOperation;
import org.codice.ddf.migration.MigrationReport;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ImportMigrationExternalEntryImplTest extends AbstractMigrationSupport {

  public static final String ENTRY_NAME = "Entry name";

  public static final String CHECKSUM = "Checksum";

  private static final Map<String, Object> METADATA_MAP =
      ImmutableMap.of(
          MigrationEntryImpl.METADATA_NAME,
          ENTRY_NAME,
          MigrationEntryImpl.METADATA_FOLDER,
          false,
          MigrationEntryImpl.METADATA_CHECKSUM,
          CHECKSUM,
          MigrationEntryImpl.METADATA_SOFTLINK,
          false);

  private static final Map<String, Object> METADATA_DIR_MAP =
      ImmutableMap.of(
          MigrationEntryImpl.METADATA_NAME,
          ENTRY_NAME,
          MigrationEntryImpl.METADATA_FOLDER,
          true,
          MigrationEntryImpl.METADATA_CHECKSUM,
          CHECKSUM,
          MigrationEntryImpl.METADATA_SOFTLINK,
          false);

  @Mock public ImportMigrationContextImpl mockContext;

  @Mock public PathUtils mockPathUtils;

  public ImportMigrationExternalEntryImpl entry;

  public Path path;

  public MigrationReport report;

  @Before
  public void setup() throws Exception {
    final File file = new File(root.toFile(), "testname");

    FileUtils.writeStringToFile(file, file.getName(), Charsets.UTF_8);
    path = file.toPath().toRealPath(LinkOption.NOFOLLOW_LINKS);

    when(mockContext.getPathUtils()).thenReturn(mockPathUtils);

    report = new MigrationReportImpl(MigrationOperation.IMPORT, Optional.empty());
    when(mockContext.getReport()).thenReturn(report);

    when(mockPathUtils.resolveAgainstDDFHome(any(Path.class))).thenReturn(path);
    when(mockPathUtils.getChecksumFor(any(Path.class))).thenReturn(CHECKSUM);

    entry = new ImportMigrationExternalEntryImpl(mockContext, METADATA_MAP);
  }

  @Test
  public void getLastModifiedTime() {
    assertThat(entry.getLastModifiedTime(), equalTo(-1L));
  }

  @Test
  public void getInputStream() throws Exception {
    assertThat(entry.getInputStream(), equalTo(Optional.empty()));
  }

  @Test
  public void restoreSuccessfullyWithMatchingChecksum() throws Exception {
    assertThat(entry.restore(true), equalTo(true));

    verify(mockPathUtils).getChecksumFor(any(Path.class));
  }

  @Test
  public void restoreWithFilterSuccessfullyWithMatchingChecksumWhenMatching() throws Exception {
    assertThat(entry.restore(true, p -> true), equalTo(true));

    verify(mockPathUtils).getChecksumFor(any(Path.class));
  }

  @Test
  public void restoreWithFilterSuccessfullyWithMatchingChecksumWhenNotMatching() throws Exception {
    assertThat(entry.restore(true, p -> false), equalTo(false));

    verify(mockPathUtils, never()).getChecksumFor(any(Path.class));

    verifyReportHasMatchingError(report, "does not match filter");
  }

  @Test
  public void restoreSuccessfullyWithoutChecksum() throws Exception {
    entry =
        new ImportMigrationExternalEntryImpl(
            mockContext,
            ImmutableMap.of(
                MigrationEntryImpl.METADATA_NAME,
                ENTRY_NAME,
                MigrationEntryImpl.METADATA_SOFTLINK,
                false));
    assertThat(entry.restore(true), equalTo(true));
  }

  @Test
  public void restoreWithFilterSuccessfullyWithoutChecksumAndMatching() throws Exception {
    entry =
        new ImportMigrationExternalEntryImpl(
            mockContext,
            ImmutableMap.of(
                MigrationEntryImpl.METADATA_NAME,
                ENTRY_NAME,
                MigrationEntryImpl.METADATA_SOFTLINK,
                false));
    assertThat(entry.restore(true, p -> true), equalTo(true));
  }

  @Test
  public void restoreWithFilterSuccessfullyWithoutChecksumAndNotMatching() throws Exception {
    entry =
        new ImportMigrationExternalEntryImpl(
            mockContext,
            ImmutableMap.of(
                MigrationEntryImpl.METADATA_NAME,
                ENTRY_NAME,
                MigrationEntryImpl.METADATA_SOFTLINK,
                false));
    assertThat(entry.restore(true, p -> false), equalTo(false));

    verifyReportHasMatchingError(report, "does not match filter");
  }

  @Test
  public void restoreSuccessfullyWhenOptionalFileDoesNotExist() throws Exception {
    if (path.toFile().delete()) {
      assertThat(entry.restore(false), equalTo(true));

      verify(mockPathUtils, never()).getChecksumFor(any(Path.class));
    } else {
      throw new AssertionError("Was unable to delete the file.");
    }
  }

  @Test
  public void restoreWithFilterSuccessfullyWhenOptionalFileDoesNotExistAndMatching()
      throws Exception {
    if (path.toFile().delete()) {
      assertThat(entry.restore(false, p -> true), equalTo(true));

      verify(mockPathUtils, never()).getChecksumFor(any(Path.class));
    } else {
      throw new AssertionError("Was unable to delete the file.");
    }
  }

  @Test
  public void restoreWithFilterSuccessfullyWhenOptionalFileDoesNotExistAndNotMatching()
      throws Exception {
    if (path.toFile().delete()) {
      assertThat(entry.restore(false, p -> false), equalTo(true));

      verify(mockPathUtils, never()).getChecksumFor(any(Path.class));
    } else {
      throw new AssertionError("Was unable to delete the file.");
    }
  }

  @Test
  public void restoreFailsWhenRequiredFileDoesNotExist() throws Exception {
    if (path.toFile().delete()) {
      assertThat(entry.restore(true), equalTo(false));

      verify(mockPathUtils, never()).getChecksumFor(any(Path.class));
      verifyReportHasMatchingError(report, "does not exist");
    } else {
      throw new AssertionError("Was unable to delete the file.");
    }
  }

  @Test
  public void restoreWithFilterFailsWhenRequiredFileDoesNotExistAndMatching() throws Exception {
    if (path.toFile().delete()) {
      assertThat(entry.restore(true, p -> true), equalTo(false));

      verify(mockPathUtils, never()).getChecksumFor(any(Path.class));
      verifyReportHasMatchingError(report, "does not exist");
    } else {
      throw new AssertionError("Was unable to delete the file.");
    }
  }

  @Test
  public void restoreWithFilterFailsWhenRequiredFileDoesNotExistAndNotMatching() throws Exception {
    if (path.toFile().delete()) {
      assertThat(entry.restore(true, p -> false), equalTo(false));

      verify(mockPathUtils, never()).getChecksumFor(any(Path.class));
      verifyReportHasMatchingError(report, "does not match filter");
    } else {
      throw new AssertionError("Was unable to delete the file.");
    }
  }

  @Test
  public void restoreFailsWhenFileIsNotSoftLink() throws Exception {
    entry =
        new ImportMigrationExternalEntryImpl(
            mockContext,
            ImmutableMap.of(
                MigrationEntryImpl.METADATA_NAME,
                ENTRY_NAME,
                MigrationEntryImpl.METADATA_SOFTLINK,
                true));
    assertThat(entry.restore(true), equalTo(false));

    verify(mockPathUtils, never()).getChecksumFor(any(Path.class));
    verifyReportHasMatchingWarning(report, "not a symbolic link");
  }

  @Test
  public void restoreWithFilterFailsWhenFileIsNotSoftLinkAndMatching() throws Exception {
    entry =
        new ImportMigrationExternalEntryImpl(
            mockContext,
            ImmutableMap.of(
                MigrationEntryImpl.METADATA_NAME,
                ENTRY_NAME,
                MigrationEntryImpl.METADATA_SOFTLINK,
                true));
    assertThat(entry.restore(true, p -> true), equalTo(false));

    verify(mockPathUtils, never()).getChecksumFor(any(Path.class));
    verifyReportHasMatchingWarning(report, "not a symbolic link");
  }

  @Test
  public void restoreWithFilterFailsWhenFileIsNotSoftLinkAndNotMatching() throws Exception {
    entry =
        new ImportMigrationExternalEntryImpl(
            mockContext,
            ImmutableMap.of(
                MigrationEntryImpl.METADATA_NAME,
                ENTRY_NAME,
                MigrationEntryImpl.METADATA_SOFTLINK,
                true));
    assertThat(entry.restore(true, p -> false), equalTo(false));

    verify(mockPathUtils, never()).getChecksumFor(any(Path.class));
    verifyReportHasMatchingError(report, "does not match filter");
  }

  @Test
  public void restoreRecordsWarningWhenFileIsNotNormal() throws Exception {
    Path symlink = ddfHome.resolve(createSoftLink("symlink", path));

    when(mockPathUtils.resolveAgainstDDFHome(any(Path.class))).thenReturn(symlink);

    entry = new ImportMigrationExternalEntryImpl(mockContext, METADATA_MAP);
    assertThat(entry.restore(true), equalTo(true));

    verify(mockPathUtils).getChecksumFor(any(Path.class));
    verifyReportHasMatchingWarning(report, "is not a regular file");
  }

  @Test
  public void restoreWithFilterRecordsWarningWhenFileIsNotNormalAndMatching() throws Exception {
    Path symlink = ddfHome.resolve(createSoftLink("symlink", path));

    when(mockPathUtils.resolveAgainstDDFHome(any(Path.class))).thenReturn(symlink);

    entry = new ImportMigrationExternalEntryImpl(mockContext, METADATA_MAP);
    assertThat(entry.restore(true, p -> true), equalTo(true));

    verify(mockPathUtils).getChecksumFor(any(Path.class));
    verifyReportHasMatchingWarning(report, "is not a regular file");
  }

  @Test
  public void restoreWithFilterRecordsWarningWhenFileIsNotNormalAndNotMatching() throws Exception {
    Path symlink = ddfHome.resolve(createSoftLink("symlink", path));

    when(mockPathUtils.resolveAgainstDDFHome(any(Path.class))).thenReturn(symlink);

    entry = new ImportMigrationExternalEntryImpl(mockContext, METADATA_MAP);
    assertThat(entry.restore(true, p -> false), equalTo(false));

    verifyReportHasMatchingError(report, "does not match filter");
  }

  @Test
  public void restoreRecordsWarningWhenDirectoryIsNotNormal() throws Exception {
    Path symlink = ddfHome.resolve(createSoftLink("symlink", path));

    when(mockPathUtils.resolveAgainstDDFHome(any(Path.class))).thenReturn(symlink);

    entry = new ImportMigrationExternalEntryImpl(mockContext, METADATA_DIR_MAP);
    assertThat(entry.restore(true), equalTo(true));

    verify(mockPathUtils).getChecksumFor(any(Path.class));
    verifyReportHasMatchingWarning(report, "is not a regular directory");
  }

  @Test
  public void restoreWithFilterRecordsWarningWhenDirectoryIsNotNormalAndMatching()
      throws Exception {
    Path symlink = ddfHome.resolve(createSoftLink("symlink", path));

    when(mockPathUtils.resolveAgainstDDFHome(any(Path.class))).thenReturn(symlink);

    entry = new ImportMigrationExternalEntryImpl(mockContext, METADATA_DIR_MAP);
    assertThat(entry.restore(true, p -> true), equalTo(true));

    verify(mockPathUtils).getChecksumFor(any(Path.class));
    verifyReportHasMatchingWarning(report, "is not a regular directory");
  }

  @Test
  public void restoreWithFilterRecordsWarningWhenDirectoryIsNotNormalAndNotMatching()
      throws Exception {
    Path symlink = ddfHome.resolve(createSoftLink("symlink", path));

    when(mockPathUtils.resolveAgainstDDFHome(any(Path.class))).thenReturn(symlink);

    entry = new ImportMigrationExternalEntryImpl(mockContext, METADATA_DIR_MAP);
    assertThat(entry.restore(true, p -> false), equalTo(false));

    verifyReportHasMatchingError(report, "does not match filter");
  }

  @Test
  public void restoreFailsWhenChecksumDoesNotMatch() throws Exception {
    when(mockPathUtils.getChecksumFor(any(Path.class))).thenReturn("Different-Checksum");

    assertThat(entry.restore(true), equalTo(false));

    verify(mockPathUtils).getChecksumFor(any(Path.class));
    verifyReportHasMatchingWarning(report, "doesn't match");
  }

  @Test
  public void restoreWithFilterFailsWhenChecksumDoesNotMatchAndMatching() throws Exception {
    when(mockPathUtils.getChecksumFor(any(Path.class))).thenReturn("Different-Checksum");

    assertThat(entry.restore(true, p -> true), equalTo(false));

    verify(mockPathUtils).getChecksumFor(any(Path.class));
    verifyReportHasMatchingWarning(report, "doesn't match");
  }

  @Test
  public void restoreWithFilterFailsWhenChecksumDoesNotMatchAndNotMatching() throws Exception {
    assertThat(entry.restore(true, p -> false), equalTo(false));

    verifyReportHasMatchingError(report, "does not match filter");
  }

  @Test
  public void restoreFailsWhenChecksumCheckThrowsIOException() throws Exception {
    when(mockPathUtils.getChecksumFor(any(Path.class))).thenThrow(IOException.class);

    assertThat(entry.restore(true), equalTo(false));

    verify(mockPathUtils).getChecksumFor(any(Path.class));
    verifyReportHasMatchingWarning(report, "Failed to compute checksum");
  }

  @Test
  public void testRestoreWithFilterWhenFilterIsNull() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null path filter"));

    entry.restore(true, null);
  }

  @Test
  public void testRestoreWithFilterVerifyingFilterReceivingEntryPath() throws Exception {
    final PathMatcher filter = Mockito.mock(PathMatcher.class);

    when(filter.matches(entry.getPath())).thenReturn(false);

    entry.restore(false, filter);

    verify(filter).matches(entry.getPath());
  }
}
