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
import com.google.common.collect.ImmutableList;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.io.FilenameUtils;
import org.codice.ddf.migration.MigrationOperation;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ImportMigrationDirectoryEntryImplTest extends AbstractMigrationSupport {

  private static final String MIGRATABLE_DIR_NAME = "where";

  private static final String MIGRATABLE_NAME = "where/some/dir/test.txt";

  private static final String MIGRATABLE_NAME2 = "where/some/test2.txt";

  private static final Path MIGRATABLE_DIR_PATH =
      Paths.get(FilenameUtils.separatorsToSystem(MIGRATABLE_DIR_NAME));

  private static final Path MIGRATABLE_PATH =
      Paths.get(FilenameUtils.separatorsToSystem(MIGRATABLE_NAME));

  private static final Path MIGRATABLE_PATH2 =
      Paths.get(FilenameUtils.separatorsToSystem(MIGRATABLE_NAME2));

  private static final long LAST_MODIFIED = 12343536L;

  private final ImportMigrationEntryImpl entry = Mockito.mock(ImportMigrationEntryImpl.class);

  private final ImportMigrationEntryImpl entry2 = Mockito.mock(ImportMigrationEntryImpl.class);

  private final MigrationReportImpl report =
      Mockito.mock(
          MigrationReportImpl.class,
          Mockito.withSettings()
              .useConstructor(MigrationOperation.IMPORT, Optional.empty())
              .defaultAnswer(Mockito.CALLS_REAL_METHODS));

  private final Map<String, Object> metadata = new HashMap<>();

  private ImportMigrationContextImpl context;

  private ImportMigrationDirectoryEntryImpl dirEntry;

  @Before
  public void setup() throws Exception {
    metadata.put(MigrationEntryImpl.METADATA_NAME, MIGRATABLE_DIR_NAME);
    metadata.put(MigrationEntryImpl.METADATA_FILTERED, false);
    metadata.put(
        MigrationEntryImpl.METADATA_FILES, ImmutableList.of(MIGRATABLE_NAME, MIGRATABLE_NAME2));
    metadata.put(MigrationEntryImpl.METADATA_LAST_MODIFIED, LAST_MODIFIED);

    context = Mockito.mock(ImportMigrationContextImpl.class);

    Mockito.when(context.getPathUtils()).thenReturn(new PathUtils());
    Mockito.when(context.getReport()).thenReturn(report);
    Mockito.when(context.getId()).thenReturn(MIGRATABLE_ID);
    Mockito.when(context.getEntry(MIGRATABLE_PATH)).thenReturn(entry);
    Mockito.when(context.getEntry(MIGRATABLE_PATH2)).thenReturn(entry2);

    Mockito.when(entry.getPath()).thenReturn(MIGRATABLE_PATH);
    Mockito.when(entry2.getPath()).thenReturn(MIGRATABLE_PATH2);

    dirEntry = new ImportMigrationDirectoryEntryImpl(context, metadata);
  }

  @Test
  public void testConstructor() throws Exception {
    Assert.assertThat(dirEntry.getName(), Matchers.equalTo(MIGRATABLE_DIR_NAME));
    Assert.assertThat(dirEntry.getPath(), Matchers.equalTo(MIGRATABLE_DIR_PATH));
    Assert.assertThat(dirEntry.getContext(), Matchers.sameInstance(context));
    Assert.assertThat(dirEntry.getReport(), Matchers.sameInstance(report));
    Assert.assertThat(dirEntry.isFiltered(), Matchers.equalTo(false));
    Assert.assertThat(dirEntry.isFile(), Matchers.equalTo(false));
    Assert.assertThat(dirEntry.isDirectory(), Matchers.equalTo(true));
    Assert.assertThat(
        dirEntry.getFileEntries(),
        Matchers.containsInAnyOrder(Matchers.sameInstance(entry), Matchers.sameInstance(entry2)));
    Assert.assertThat(dirEntry.getLastModifiedTime(), Matchers.equalTo(LAST_MODIFIED));
  }

  @Test
  public void testConstructorWithOmmitedFilteredMetadata() throws Exception {
    metadata.remove(MigrationEntryImpl.METADATA_FILTERED);

    final ImportMigrationDirectoryEntryImpl dirEntry =
        new ImportMigrationDirectoryEntryImpl(context, metadata);

    Assert.assertThat(dirEntry.getName(), Matchers.equalTo(MIGRATABLE_DIR_NAME));
    Assert.assertThat(dirEntry.getPath(), Matchers.equalTo(MIGRATABLE_DIR_PATH));
    Assert.assertThat(dirEntry.getContext(), Matchers.sameInstance(context));
    Assert.assertThat(dirEntry.getReport(), Matchers.sameInstance(report));
    Assert.assertThat(dirEntry.isFiltered(), Matchers.equalTo(false));
    Assert.assertThat(dirEntry.isFile(), Matchers.equalTo(false));
    Assert.assertThat(dirEntry.isDirectory(), Matchers.equalTo(true));
    Assert.assertThat(
        dirEntry.getFileEntries(),
        Matchers.containsInAnyOrder(Matchers.sameInstance(entry), Matchers.sameInstance(entry2)));
    Assert.assertThat(dirEntry.getLastModifiedTime(), Matchers.equalTo(LAST_MODIFIED));
  }

  @Test
  public void testConstructorWithOmmitedLastModifiedMetadata() throws Exception {
    metadata.remove(MigrationEntryImpl.METADATA_LAST_MODIFIED);

    final ImportMigrationDirectoryEntryImpl dirEntry =
        new ImportMigrationDirectoryEntryImpl(context, metadata);

    Assert.assertThat(dirEntry.getName(), Matchers.equalTo(MIGRATABLE_DIR_NAME));
    Assert.assertThat(dirEntry.getPath(), Matchers.equalTo(MIGRATABLE_DIR_PATH));
    Assert.assertThat(dirEntry.getContext(), Matchers.sameInstance(context));
    Assert.assertThat(dirEntry.getReport(), Matchers.sameInstance(report));
    Assert.assertThat(dirEntry.isFiltered(), Matchers.equalTo(false));
    Assert.assertThat(dirEntry.isFile(), Matchers.equalTo(false));
    Assert.assertThat(dirEntry.isDirectory(), Matchers.equalTo(true));
    Assert.assertThat(
        dirEntry.getFileEntries(),
        Matchers.containsInAnyOrder(Matchers.sameInstance(entry), Matchers.sameInstance(entry2)));
    Assert.assertThat(dirEntry.getLastModifiedTime(), Matchers.equalTo(-1L));
  }

  @Test
  public void testRestoreWhenNotFiltered() throws Exception {
    final Path dir = createDirectory("where");
    final Path dir2 = createDirectory("where", "some");
    final Path dir3 = createDirectory("where", "some", "other");
    final Path file = ddfHome.resolve(createFile(dir, "other.txt"));
    final Path file2 = ddfHome.resolve(createFile(dir2, "other2.txt"));
    final Path file3 = ddfHome.resolve(createFile(dir3, "other3.txt"));
    final Path file0 = ddfHome.resolve(createFile(dir2, "test2.txt"));

    Mockito.when(entry.restore(false)).thenReturn(true);
    Mockito.when(entry2.restore(false)).thenReturn(true);

    Assert.assertThat(dirEntry.restore(true), Matchers.equalTo(true));
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
    Assert.assertThat(report.wasSuccessful(), Matchers.equalTo(true));

    Assert.assertThat(dir.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(dir2.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(dir2.resolve("test2.txt").toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(dir3.toFile().exists(), Matchers.equalTo(false));
    Assert.assertThat(file.toFile().exists(), Matchers.equalTo(false));
    Assert.assertThat(file2.toFile().exists(), Matchers.equalTo(false));
    Assert.assertThat(file3.toFile().exists(), Matchers.equalTo(false));
    Assert.assertThat(file0.toFile().exists(), Matchers.equalTo(true));

    Mockito.verify(entry).restore(false);
    Mockito.verify(entry2).restore(false);
  }

  @Test
  public void testRestoreWhenFiltered() throws Exception {
    metadata.put(MigrationEntryImpl.METADATA_FILTERED, true);

    final ImportMigrationDirectoryEntryImpl dirEntry =
        new ImportMigrationDirectoryEntryImpl(context, metadata);

    final Path dir = createDirectory("where");
    final Path dir2 = createDirectory("where", "some");
    final Path dir3 = createDirectory("where", "some", "other");
    final Path file = ddfHome.resolve(createFile(dir, "other.txt"));
    final Path file2 = ddfHome.resolve(createFile(dir2, "other2.txt"));
    final Path file3 = ddfHome.resolve(createFile(dir3, "other3.txt"));
    final Path file0 = ddfHome.resolve(createFile(dir2, "test2.txt"));

    Mockito.when(entry.restore(false)).thenReturn(true);
    Mockito.when(entry2.restore(false)).thenReturn(true);

    Assert.assertThat(dirEntry.restore(true), Matchers.equalTo(true));
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
    Assert.assertThat(report.wasSuccessful(), Matchers.equalTo(true));

    Assert.assertThat(dir.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(dir2.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(dir2.resolve("test2.txt").toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(dir3.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(file.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(file2.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(file3.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(file0.toFile().exists(), Matchers.equalTo(true));

    Mockito.verify(entry).restore(false);
    Mockito.verify(entry2).restore(false);
  }

  @Test
  public void testRestoreWhenFileRestoredFailed() throws Exception {
    final Path dir = createDirectory("where");
    final Path dir2 = createDirectory("where", "some");
    final Path dir3 = createDirectory("where", "some", "other");
    final Path file = ddfHome.resolve(createFile(dir, "other.txt"));
    final Path file2 = ddfHome.resolve(createFile(dir2, "other2.txt"));
    final Path file3 = ddfHome.resolve(createFile(dir3, "other3.txt"));
    final Path file0 = ddfHome.resolve(createFile(dir2, "test2.txt"));

    Mockito.when(entry.restore(false)).thenReturn(true);
    Mockito.when(entry2.restore(false)).thenReturn(false);

    Assert.assertThat(dirEntry.restore(true), Matchers.equalTo(false));
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(true));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
    Assert.assertThat(report.wasSuccessful(), Matchers.equalTo(false));

    Assert.assertThat(dir.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(dir2.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(dir2.resolve("test2.txt").toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(dir3.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(file.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(file2.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(file3.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(file0.toFile().exists(), Matchers.equalTo(true));

    Mockito.verify(entry).restore(false);
    Mockito.verify(entry2).restore(false);
  }

  @Test
  public void testRestoreWithFilterWhenSomeDoNotMatch() throws Exception {
    final Path dir = createDirectory("where");
    final Path dir2 = createDirectory("where", "some");
    final Path dir3 = createDirectory("where", "some", "other");
    final Path file = ddfHome.resolve(createFile(dir, "other.txt"));
    final Path file2 = ddfHome.resolve(createFile(dir2, "other2.txt"));
    final Path file3 = ddfHome.resolve(createFile(dir3, "other3.txt"));
    final Path file0 = ddfHome.resolve(createFile(dir2, "test2.txt"));

    Mockito.when(entry.restore(false)).thenReturn(true);
    Mockito.when(entry2.restore(false)).thenReturn(true);

    Assert.assertThat(
        dirEntry.restore(true, p -> p.equals(MIGRATABLE_PATH)), Matchers.equalTo(true));
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
    Assert.assertThat(report.wasSuccessful(), Matchers.equalTo(true));

    Assert.assertThat(dir.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(dir2.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(dir2.resolve("test2.txt").toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(dir3.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(file.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(file2.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(file3.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(file0.toFile().exists(), Matchers.equalTo(true));

    Mockito.verify(entry).restore(false);
    Mockito.verify(entry2, Mockito.never()).restore(false);
  }

  @Test
  public void testRestoreWithFilterWhenFileRestoredFailed() throws Exception {
    final Path dir = createDirectory("where");
    final Path dir2 = createDirectory("where", "some");
    final Path dir3 = createDirectory("where", "some", "other");
    final Path file = ddfHome.resolve(createFile(dir, "other.txt"));
    final Path file2 = ddfHome.resolve(createFile(dir2, "other2.txt"));
    final Path file3 = ddfHome.resolve(createFile(dir3, "other3.txt"));
    final Path file0 = ddfHome.resolve(createFile(dir2, "test2.txt"));

    Mockito.when(entry.restore(false)).thenReturn(false);
    Mockito.when(entry2.restore(false)).thenReturn(true);

    Assert.assertThat(dirEntry.restore(true, p -> true), Matchers.equalTo(false));
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(true));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
    Assert.assertThat(report.wasSuccessful(), Matchers.equalTo(false));

    Assert.assertThat(dir.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(dir2.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(dir2.resolve("test2.txt").toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(dir3.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(file.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(file2.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(file3.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(file0.toFile().exists(), Matchers.equalTo(true));

    Mockito.verify(entry).restore(false);
    Mockito.verify(entry2).restore(false);
  }

  @Test
  public void testRestoreWithFilterWhenNoFilesMatch() throws Exception {
    final Path dir = createDirectory("where");
    final Path dir2 = createDirectory("where", "some");
    final Path dir3 = createDirectory("where", "some", "other");
    final Path file = ddfHome.resolve(createFile(dir, "other.txt"));
    final Path file2 = ddfHome.resolve(createFile(dir2, "other2.txt"));
    final Path file3 = ddfHome.resolve(createFile(dir3, "other3.txt"));
    final Path file0 = ddfHome.resolve(createFile(dir2, "test2.txt"));

    Mockito.when(entry.restore(false)).thenReturn(true);
    Mockito.when(entry2.restore(false)).thenReturn(true);

    Assert.assertThat(dirEntry.restore(true, p -> false), Matchers.equalTo(true));
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
    Assert.assertThat(report.wasSuccessful(), Matchers.equalTo(true));

    Assert.assertThat(dir.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(dir2.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(dir2.resolve("test2.txt").toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(dir3.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(file.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(file2.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(file3.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(file0.toFile().exists(), Matchers.equalTo(true));

    Mockito.verify(entry, Mockito.never()).restore(false);
    Mockito.verify(entry2, Mockito.never()).restore(false);
  }

  @Test
  public void testGetInputStream() throws Exception {
    Assert.assertThat(dirEntry.getInputStream(), OptionalMatchers.isEmpty());
  }
}
