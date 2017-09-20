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
import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipFile;
import org.apache.commons.io.FilenameUtils;
import org.codice.ddf.migration.ImportMigrationEntry;
import org.codice.ddf.migration.Migratable;
import org.codice.ddf.migration.MigrationEntry;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationOperation;
import org.codice.ddf.test.matchers.CastingMatchers;
import org.codice.ddf.test.matchers.MappingMatchers;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ImportMigrationContextImplTest extends AbstractMigrationTest {
  private static final String[] DIRS = new String[] {"where", "some", "dir"};

  private static final String[] DIRS2 = new String[] {"where", "some"};

  private static final String PROPERTY_NAME = "test.property";

  private static final String MIGRATABLE_NAME = "where/some/dir/test.txt";

  private static final String MIGRATABLE_NAME2 = "where/some/test.txt";

  private static final Path MIGRATABLE_PATH =
      Paths.get(FilenameUtils.separatorsToSystem(MIGRATABLE_NAME));

  private static final Path MIGRATABLE_PATH2 =
      Paths.get(FilenameUtils.separatorsToSystem(MIGRATABLE_NAME2));

  private static final ImportMigrationEntryImpl ENTRY =
      Mockito.mock(ImportMigrationEntryImpl.class);

  private static final ImportMigrationEntryImpl ENTRY2 =
      Mockito.mock(ImportMigrationEntryImpl.class);

  private static final ImportMigrationSystemPropertyReferencedEntryImpl SYS_ENTRY =
      Mockito.mock(ImportMigrationSystemPropertyReferencedEntryImpl.class);

  private final MigrationReportImpl report =
      new MigrationReportImpl(MigrationOperation.IMPORT, Optional.empty());

  private final ZipFile zip = Mockito.mock(ZipFile.class);

  private ImportMigrationContextImpl context;

  @Before
  public void setup() throws Exception {
    initMigratableMock();

    Mockito.when(ENTRY.getPath()).thenReturn(MIGRATABLE_PATH);
    Mockito.when(ENTRY2.getPath()).thenReturn(MIGRATABLE_PATH2);

    context = new ImportMigrationContextImpl(report, zip);
  }

  @Test
  public void testConstructorWithNoMigratableOrId() throws Exception {
    Assert.assertThat(context.getReport(), Matchers.sameInstance(report));
    Assert.assertThat(context.getMigratable(), Matchers.nullValue());
    Assert.assertThat(context.getId(), Matchers.nullValue());
    Assert.assertThat(context.getVersion(), OptionalMatchers.isEmpty());
    Assert.assertThat(context.getZip(), Matchers.sameInstance(zip));
    Assert.assertThat(context.getEntries(), Matchers.anEmptyMap());
    Assert.assertThat(context.getSystemPropertiesReferencedEntries(), Matchers.anEmptyMap());
  }

  @Test
  public void testConstructorWithNoMigratableOrIdAndNullReport() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null report"));

    new ImportMigrationContextImpl(null, zip);
  }

  @Test
  public void testConstructorWithNoMigratableOrIdAndNullZip() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null zip"));

    new ImportMigrationContextImpl(report, null);
  }

  @Test
  public void testConstructorWithId() throws Exception {
    context = new ImportMigrationContextImpl(report, zip, MIGRATABLE_ID);

    Assert.assertThat(context.getReport(), Matchers.sameInstance(report));
    Assert.assertThat(context.getMigratable(), Matchers.nullValue());
    Assert.assertThat(context.getId(), Matchers.equalTo(MIGRATABLE_ID));
    Assert.assertThat(context.getVersion(), OptionalMatchers.isEmpty());
    Assert.assertThat(context.getZip(), Matchers.sameInstance(zip));
    Assert.assertThat(context.getEntries(), Matchers.anEmptyMap());
    Assert.assertThat(context.getSystemPropertiesReferencedEntries(), Matchers.anEmptyMap());
  }

  @Test
  public void testConstructorWithIdAndNullReport() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null "));

    new ImportMigrationContextImpl(null, zip, MIGRATABLE_ID);
  }

  @Test
  public void testConstructorWithIdAndNullZip() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null zip"));

    new ImportMigrationContextImpl(report, null, MIGRATABLE_ID);
  }

  @Test
  public void testConstructorWithNullId() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null migratable identifier"));

    new ImportMigrationContextImpl(report, zip, (String) null);
  }

  @Test
  public void testConstructorWithMigratable() throws Exception {
    context = new ImportMigrationContextImpl(report, zip, migratable);

    Assert.assertThat(context.getReport(), Matchers.sameInstance(report));
    Assert.assertThat(context.getMigratable(), Matchers.sameInstance(migratable));
    Assert.assertThat(context.getId(), Matchers.equalTo(MIGRATABLE_ID));
    Assert.assertThat(context.getVersion(), OptionalMatchers.isEmpty());
    Assert.assertThat(context.getZip(), Matchers.sameInstance(zip));
    Assert.assertThat(context.getEntries(), Matchers.anEmptyMap());
    Assert.assertThat(context.getSystemPropertiesReferencedEntries(), Matchers.anEmptyMap());
  }

  @Test
  public void testConstructorWithMigratableAndNullReport() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null "));

    new ImportMigrationContextImpl(null, zip, migratable);
  }

  @Test
  public void testConstructorWithMigratableAndNullZip() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null zip"));

    new ImportMigrationContextImpl(report, null, migratable);
  }

  @Test
  public void testConstructorWithNullMigratable() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null migratable"));

    new ImportMigrationContextImpl(report, zip, (Migratable) null);
  }

  @Test
  public void testGetSystemPropertyReferencedEntry() throws Exception {
    context.getSystemPropertiesReferencedEntries().put(PROPERTY_NAME, SYS_ENTRY);

    final Optional<ImportMigrationEntry> entry =
        context.getSystemPropertyReferencedEntry(PROPERTY_NAME);

    Assert.assertThat(entry, OptionalMatchers.hasValue(Matchers.sameInstance(SYS_ENTRY)));
  }

  @Test
  public void testGetSystemPropertyReferencedEntryWhenNotDefined() throws Exception {
    final Optional<ImportMigrationEntry> entry =
        context.getSystemPropertyReferencedEntry(PROPERTY_NAME);

    Assert.assertThat(entry, OptionalMatchers.isEmpty());
  }

  @Test
  public void testGetSystemPropertyReferencedEntryWithNullName() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null system property name"));

    context.getSystemPropertyReferencedEntry(null);
  }

  @Test
  public void testGetEntry() throws Exception {
    context.getEntries().put(MIGRATABLE_PATH, ENTRY);

    final ImportMigrationEntry entry = context.getEntry(MIGRATABLE_PATH);

    Assert.assertThat(entry, Matchers.sameInstance(ENTRY));
  }

  @Test
  public void testGetEntryWhenNotDefined() throws Exception {
    final ImportMigrationEntry entry = context.getEntry(MIGRATABLE_PATH);

    Assert.assertThat(entry, Matchers.instanceOf(ImportMigrationEmptyEntryImpl.class));
    Assert.assertThat(entry.getReport(), Matchers.sameInstance(report));
    Assert.assertThat(entry.getPath(), Matchers.equalTo(MIGRATABLE_PATH));
  }

  @Test
  public void testGetEntryWithNullPath() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null path"));

    context.getEntry(null);
  }

  @Test
  public void testEntries() throws Exception {
    context.getEntries().put(MIGRATABLE_PATH, ENTRY);
    context.getEntries().put(MIGRATABLE_PATH2, ENTRY2);

    Assert.assertThat(
        context.entries().toArray(ImportMigrationEntry[]::new),
        Matchers.arrayContainingInAnyOrder(
            Matchers.sameInstance(ENTRY), Matchers.sameInstance(ENTRY2)));
  }

  @Test
  public void testEntriesWhenEmpty() throws Exception {
    Assert.assertThat(context.entries().count(), Matchers.equalTo(0L));
  }

  @Test
  public void testEntriesWithPath() throws Exception {
    final ImportMigrationEntryImpl entry3 = Mockito.mock(ImportMigrationEntryImpl.class);

    Mockito.when(entry3.getPath()).thenReturn(ddfHome);

    context.getEntries().put(MIGRATABLE_PATH, ENTRY);
    context.getEntries().put(MIGRATABLE_PATH2, ENTRY2);
    context.getEntries().put(ddfHome, entry3);

    Assert.assertThat(
        context.entries(MIGRATABLE_PATH2.getParent()).toArray(ImportMigrationEntry[]::new),
        Matchers.arrayContainingInAnyOrder(
            Matchers.sameInstance(ENTRY), Matchers.sameInstance(ENTRY2)));
  }

  @Test
  public void testEntriesWithNullPath() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null path"));

    context.entries(null);
  }

  @Test
  public void testEntriesWithPathAndFilter() throws Exception {
    final ImportMigrationEntryImpl entry3 = Mockito.mock(ImportMigrationEntryImpl.class);
    final PathMatcher filter = Mockito.mock(PathMatcher.class);

    Mockito.when(entry3.getPath()).thenReturn(ddfHome);
    Mockito.when(filter.matches(Mockito.any())).thenReturn(false);
    Mockito.when(filter.matches(MIGRATABLE_PATH)).thenReturn(true);

    context.getEntries().put(MIGRATABLE_PATH, ENTRY);
    context.getEntries().put(MIGRATABLE_PATH2, ENTRY2);
    context.getEntries().put(ddfHome, entry3);

    Assert.assertThat(
        context.entries(MIGRATABLE_PATH2.getParent(), filter).toArray(ImportMigrationEntry[]::new),
        Matchers.arrayContainingInAnyOrder(Matchers.sameInstance(ENTRY)));

    Mockito.verify(filter).matches(MIGRATABLE_PATH);
    Mockito.verify(filter).matches(MIGRATABLE_PATH2);
    Mockito.verify(filter, Mockito.never()).matches(ddfHome);
  }

  @Test
  public void testEntriesWithFilterAndNullPath() throws Exception {
    final PathMatcher filter = Mockito.mock(PathMatcher.class);

    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null path"));

    context.entries(null, filter);
  }

  @Test
  public void testEntriesWithPathAndNullFilter() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null filter"));

    context.entries(MIGRATABLE_PATH, null);
  }

  @Test
  public void testCleanDirectoryWithAParentAbsolutePath() throws Exception {
    final Path dir2 = ddfHome.resolve(createDirectory(DIRS2));
    final Path dir = createDirectory(DIRS);
    final Path path2 = ddfHome.resolve(createFile(MIGRATABLE_PATH2));
    final Path path = ddfHome.resolve(createFile(MIGRATABLE_PATH));

    Assert.assertThat(context.cleanDirectory(dir2), Matchers.equalTo(true));

    Assert.assertThat(dir2.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(path2.toFile().exists(), Matchers.equalTo(false));
    Assert.assertThat(dir.toFile().exists(), Matchers.equalTo(false));
    Assert.assertThat(path.toFile().exists(), Matchers.equalTo(false));
  }

  @Test
  public void testCleanDirectoryWithAChildAbsolutePath() throws Exception {
    final Path dir2 = ddfHome.resolve(createDirectory(DIRS2));
    final Path dir = createDirectory(DIRS);
    final Path path2 = ddfHome.resolve(createFile(MIGRATABLE_PATH2));
    final Path path = ddfHome.resolve(createFile(MIGRATABLE_PATH));

    Assert.assertThat(context.cleanDirectory(dir), Matchers.equalTo(true));

    Assert.assertThat(dir2.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(path2.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(dir.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(path.toFile().exists(), Matchers.equalTo(false));
  }

  @Test
  public void testCleanDirectoryWithAnOutsideAbsolutePath() throws Exception {
    final Path dir2 = ddfHome.resolve(createDirectory(DIRS2));
    final Path dir = root.resolve(Paths.get("where", "something_else"));
    final Path path2 = ddfHome.resolve(createFile(MIGRATABLE_PATH2));
    final Path path = ddfHome.resolve(createFile(MIGRATABLE_PATH));

    dir.toFile().mkdirs();

    Assert.assertThat(context.cleanDirectory(dir), Matchers.equalTo(false));

    Assert.assertThat(dir2.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(path2.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(dir.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(path.toFile().exists(), Matchers.equalTo(true));
  }

  @Test
  public void testCleanDirectoryWithRelativePath() throws Exception {
    final Path dir2 = ddfHome.resolve(createDirectory(DIRS2));
    final Path dir = createDirectory(DIRS);
    final Path path2 = ddfHome.resolve(createFile(MIGRATABLE_PATH2));
    final Path path = ddfHome.resolve(createFile(MIGRATABLE_PATH));

    Assert.assertThat(context.cleanDirectory(MIGRATABLE_PATH.getParent()), Matchers.equalTo(true));

    Assert.assertThat(dir2.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(path2.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(dir.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(path.toFile().exists(), Matchers.equalTo(false));
  }

  @Test
  public void testCleanDirectoryWithNullPath() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null path"));

    context.cleanDirectory(null);
  }

  @Test
  public void testCleanDirectoryWithNonExistentPath() throws Exception {
    Assert.assertThat(context.cleanDirectory(MIGRATABLE_PATH.getParent()), Matchers.equalTo(true));
  }

  @Test
  public void testCleanDirectoryWithNonExistentFile() throws Exception {
    final PathUtils pathUtils = Mockito.mock(PathUtils.class);
    final Path resolvedPath = Mockito.mock(Path.class);

    context = Mockito.spy(context);

    Mockito.when(context.getPathUtils()).thenReturn(pathUtils);
    Mockito.when(pathUtils.resolveAgainstDDFHome(MIGRATABLE_PATH)).thenReturn(resolvedPath);
    Mockito.when(resolvedPath.toFile()).thenReturn(MIGRATABLE_PATH.toFile());
    Mockito.when(resolvedPath.toRealPath(Mockito.any())).thenReturn(resolvedPath);
    Mockito.when(pathUtils.isRelativeToDDFHome(resolvedPath)).thenReturn(true);

    MIGRATABLE_PATH.toFile().delete();

    Assert.assertThat(context.cleanDirectory(MIGRATABLE_PATH), Matchers.equalTo(true));
  }

  @Test
  public void testCleanDirectoryWhenUnableToDetermineRealPath() throws Exception {
    final PathUtils pathUtils = Mockito.mock(PathUtils.class);
    final Path resolvedPath = Mockito.mock(Path.class);
    final IOException exception = new IOException("testing");

    context = Mockito.spy(context);

    Mockito.when(context.getPathUtils()).thenReturn(pathUtils);
    Mockito.when(pathUtils.resolveAgainstDDFHome(MIGRATABLE_PATH)).thenReturn(resolvedPath);
    Mockito.when(resolvedPath.toFile()).thenReturn(MIGRATABLE_PATH.toFile());
    Mockito.when(resolvedPath.toRealPath(Mockito.any())).thenThrow(exception);

    Assert.assertThat(context.cleanDirectory(MIGRATABLE_PATH), Matchers.equalTo(false));
  }

  @Test
  public void testCleanDirectoryWithAFile() throws Exception {
    final Path dir = createDirectory(DIRS);
    final Path path = ddfHome.resolve(createFile(MIGRATABLE_PATH));

    Assert.assertThat(context.cleanDirectory(MIGRATABLE_PATH), Matchers.equalTo(false));

    Assert.assertThat(dir.toFile().exists(), Matchers.equalTo(true));
    Assert.assertThat(path.toFile().exists(), Matchers.equalTo(true));
  }

  @Test
  public void testCleanDirectoryWhenCleaningItThrowsException() throws Exception {
    final PathUtils pathUtils = Mockito.mock(PathUtils.class);
    final File fdir = Mockito.mock(File.class);
    final Path dir = Mockito.mock(Path.class);

    context = Mockito.spy(context);

    Mockito.when(context.getPathUtils()).thenReturn(pathUtils);
    Mockito.when(pathUtils.resolveAgainstDDFHome(dir)).thenReturn(dir);
    Mockito.when(dir.toFile()).thenReturn(fdir);
    Mockito.when(dir.toRealPath(LinkOption.NOFOLLOW_LINKS)).thenReturn(dir);
    Mockito.when(pathUtils.isRelativeToDDFHome(dir)).thenReturn(true);
    Mockito.when(fdir.exists()).thenReturn(true);
    Mockito.when(fdir.isDirectory()).thenReturn(true);
    Mockito.when(fdir.listFiles()).thenReturn(null); // should trigger an I/O exception

    Assert.assertThat(context.cleanDirectory(dir), Matchers.equalTo(false));
  }

  @Test
  public void testGetOptionalEntry() throws Exception {
    context.getEntries().put(MIGRATABLE_PATH, ENTRY);

    final Optional<ImportMigrationEntry> entry = context.getOptionalEntry(MIGRATABLE_PATH);

    Assert.assertThat(entry, OptionalMatchers.hasValue(Matchers.sameInstance(ENTRY)));
  }

  @Test
  public void testGetOptionalEntryWhenNotDefined() throws Exception {
    final Optional<ImportMigrationEntry> entry = context.getOptionalEntry(MIGRATABLE_PATH);

    Assert.assertThat(entry, OptionalMatchers.isEmpty());
  }

  @Test
  public void testProcessMetadataWithNoEntries() throws Exception {
    final Map<String, Object> metadata =
        ImmutableMap.of(MigrationContextImpl.METADATA_VERSION, VERSION);

    context.processMetadata(metadata);

    Assert.assertThat(context.getVersion(), OptionalMatchers.hasValue(VERSION));
    Assert.assertThat(context.entries().count(), Matchers.equalTo(0L));
    Assert.assertThat(context.getSystemPropertiesReferencedEntries(), Matchers.anEmptyMap());
  }

  @Test
  public void testProcessMetadataWithOnlyEntries() throws Exception {
    final String checksum = "abcdef";
    final String checksum2 = "12345";
    final boolean softlink = false;
    final boolean softlink2 = true;
    final Map<String, Object> metadata =
        ImmutableMap.of(
            MigrationContextImpl.METADATA_VERSION,
            VERSION,
            MigrationContextImpl.METADATA_EXTERNALS,
            ImmutableList.of( //
                ImmutableMap.of(
                    MigrationEntryImpl.METADATA_NAME,
                    MIGRATABLE_NAME,
                    MigrationEntryImpl.METADATA_CHECKSUM,
                    checksum), // ommit softlink
                ImmutableMap.of(
                    MigrationEntryImpl.METADATA_NAME,
                    MIGRATABLE_NAME2,
                    MigrationEntryImpl.METADATA_CHECKSUM,
                    checksum2,
                    MigrationEntryImpl.METADATA_SOFTLINK,
                    softlink2)));

    context.processMetadata(metadata);

    Assert.assertThat(context.getVersion(), OptionalMatchers.hasValue(VERSION));
    Assert.assertThat(
        context.entries().toArray(ImportMigrationEntry[]::new),
        Matchers.arrayContainingInAnyOrder( //
            Matchers.allOf( //
                MappingMatchers.map(MigrationEntry::getName, Matchers.equalTo(MIGRATABLE_NAME)),
                CastingMatchers.cast(
                    ImportMigrationExternalEntryImpl.class,
                    Matchers.allOf( //
                        MappingMatchers.map(
                            ImportMigrationExternalEntryImpl::getChecksum,
                            Matchers.equalTo(checksum)),
                        MappingMatchers.map(
                            ImportMigrationExternalEntryImpl::isSoftlink,
                            Matchers.equalTo(softlink))))), //
            Matchers.allOf( //
                MappingMatchers.map(MigrationEntry::getName, Matchers.equalTo(MIGRATABLE_NAME2)),
                CastingMatchers.cast(
                    ImportMigrationExternalEntryImpl.class,
                    Matchers.allOf( //
                        MappingMatchers.map(
                            ImportMigrationExternalEntryImpl::getChecksum,
                            Matchers.equalTo(checksum2)),
                        MappingMatchers.map(
                            ImportMigrationExternalEntryImpl::isSoftlink,
                            Matchers.equalTo(softlink2)))))));
    Assert.assertThat(context.getSystemPropertiesReferencedEntries(), Matchers.anEmptyMap());
  }

  @Test
  public void testProcessMetadataWithSystemPropertyReferencedEntries() throws Exception {
    final String property = "property.name";
    final String checksum = "abcdef";
    final String checksum2 = "12345";
    final boolean softlink2 = true;
    final Map<String, Object> metadata =
        ImmutableMap.of(
            MigrationContextImpl.METADATA_VERSION,
            VERSION,
            MigrationContextImpl.METADATA_EXTERNALS,
            ImmutableList.of( //
                ImmutableMap.of(
                    MigrationEntryImpl.METADATA_NAME,
                    MIGRATABLE_NAME,
                    MigrationEntryImpl.METADATA_CHECKSUM,
                    checksum), // ommit softlink
                ImmutableMap.of(
                    MigrationEntryImpl.METADATA_NAME,
                    MIGRATABLE_NAME2,
                    MigrationEntryImpl.METADATA_CHECKSUM,
                    checksum2,
                    MigrationEntryImpl.METADATA_SOFTLINK,
                    softlink2)),
            MigrationContextImpl.METADATA_SYSTEM_PROPERTIES,
            ImmutableList.of( //
                ImmutableMap.of(
                    MigrationEntryImpl.METADATA_PROPERTY,
                    property,
                    MigrationEntryImpl.METADATA_REFERENCE,
                    MIGRATABLE_NAME)));

    context.processMetadata(metadata);

    Assert.assertThat(context.getVersion(), OptionalMatchers.hasValue(VERSION));
    Assert.assertThat(context.getEntries(), Matchers.aMapWithSize(2));
    Assert.assertThat(
        context
            .getSystemPropertiesReferencedEntries()
            .values()
            .stream()
            .toArray(ImportMigrationSystemPropertyReferencedEntryImpl[]::new),
        Matchers.arrayContaining( //
            Matchers.allOf( //
                MappingMatchers.map(
                    ImportMigrationPropertyReferencedEntryImpl::getProperty,
                    Matchers.equalTo(property)),
                MappingMatchers.map(
                    ImportMigrationPropertyReferencedEntryImpl::getReferencedEntry,
                    MappingMatchers.map(
                        MigrationEntry::getName, Matchers.equalTo(MIGRATABLE_NAME))))));
  }

  @Test
  public void testProcessMetadataWithSystemPropertyReferencedEntriesThatWereNotExported()
      throws Exception {
    final String property = "property.name";
    final String checksum2 = "12345";
    final boolean softlink2 = true;
    final Map<String, Object> metadata =
        ImmutableMap.of(
            MigrationContextImpl.METADATA_VERSION,
            VERSION,
            MigrationContextImpl.METADATA_EXTERNALS,
            ImmutableList.of( //
                ImmutableMap.of(
                    MigrationEntryImpl.METADATA_NAME,
                    MIGRATABLE_NAME2,
                    MigrationEntryImpl.METADATA_CHECKSUM,
                    checksum2,
                    MigrationEntryImpl.METADATA_SOFTLINK,
                    softlink2)),
            MigrationContextImpl.METADATA_SYSTEM_PROPERTIES,
            ImmutableList.of( //
                ImmutableMap.of(
                    MigrationEntryImpl.METADATA_PROPERTY,
                    property,
                    MigrationEntryImpl.METADATA_REFERENCE,
                    MIGRATABLE_NAME)));

    thrown.expect(MigrationException.class);
    thrown.expectMessage(Matchers.containsString("referenced path [" + MIGRATABLE_NAME + "]"));

    context.processMetadata(metadata);
  }

  @Test
  public void testProcessMetadataWithJavaPropertyReferencedEntriesWhenPropertiesWasAlsoExported()
      throws Exception {
    final String migratablePropertyName = "where/some/dir/test.properties";
    final String property = "property.name";
    final String checksum = "abcdef";
    final String checksum2 = "12345";
    final String propertyChecksum = "a1b2c3d4e5";
    final boolean softlink2 = true;
    final boolean propertySoftlink = false;
    final Map<String, Object> metadata =
        ImmutableMap.of(
            MigrationContextImpl.METADATA_VERSION,
            VERSION,
            MigrationContextImpl.METADATA_EXTERNALS,
            ImmutableList.of( //
                ImmutableMap.of(
                    MigrationEntryImpl.METADATA_NAME,
                    MIGRATABLE_NAME,
                    MigrationEntryImpl.METADATA_CHECKSUM,
                    checksum),
                // ommit softlink
                ImmutableMap.of(
                    MigrationEntryImpl.METADATA_NAME,
                    MIGRATABLE_NAME2,
                    MigrationEntryImpl.METADATA_CHECKSUM,
                    checksum2,
                    MigrationEntryImpl.METADATA_SOFTLINK,
                    softlink2),
                ImmutableMap.of(
                    MigrationEntryImpl.METADATA_NAME,
                    migratablePropertyName,
                    MigrationEntryImpl.METADATA_CHECKSUM,
                    propertyChecksum,
                    MigrationEntryImpl.METADATA_SOFTLINK,
                    propertySoftlink)),
            MigrationContextImpl.METADATA_JAVA_PROPERTIES,
            ImmutableList.of( //
                ImmutableMap.of(
                    MigrationEntryImpl.METADATA_NAME,
                    migratablePropertyName,
                    MigrationEntryImpl.METADATA_PROPERTY,
                    property,
                    MigrationEntryImpl.METADATA_REFERENCE,
                    MIGRATABLE_NAME)));

    context.processMetadata(metadata);

    Assert.assertThat(context.getVersion(), OptionalMatchers.hasValue(VERSION));
    Assert.assertThat(context.getEntries(), Matchers.aMapWithSize(3));
    Assert.assertThat(
        context.getEntries().values(),
        Matchers.hasItem( //
            Matchers.allOf( //
                MappingMatchers.map(
                    MigrationEntry::getName, Matchers.equalTo(migratablePropertyName)),
                CastingMatchers.cast(
                    ImportMigrationExternalEntryImpl.class,
                    Matchers.allOf( //
                        MappingMatchers.map(
                            ImportMigrationExternalEntryImpl::getChecksum,
                            Matchers.equalTo(propertyChecksum)),
                        MappingMatchers.map(
                            ImportMigrationExternalEntryImpl::isSoftlink,
                            Matchers.equalTo(propertySoftlink)))),
                MappingMatchers.map(
                    ImportMigrationEntryImpl::getJavaPropertyReferencedEntries,
                    Matchers.allOf( //
                        Matchers.aMapWithSize(1),
                        Matchers.hasValue(
                            MappingMatchers.map(
                                MigrationEntry::getName, Matchers.equalTo(MIGRATABLE_NAME))))))));
  }

  @Test
  public void testProcessMetadataWithJavaPropertyReferencedEntriesWhenPropertiesWasNotExported()
      throws Exception {
    final String migratablePropertyName = "where/some/dir/test.properties";
    final String property = "property.name";
    final String checksum = "abcdef";
    final String checksum2 = "12345";
    final boolean softlink2 = true;
    final Map<String, Object> metadata =
        ImmutableMap.of(
            MigrationContextImpl.METADATA_VERSION,
            VERSION,
            MigrationContextImpl.METADATA_EXTERNALS,
            ImmutableList.of( //
                ImmutableMap.of(
                    MigrationEntryImpl.METADATA_NAME,
                    MIGRATABLE_NAME,
                    MigrationEntryImpl.METADATA_CHECKSUM,
                    checksum), // ommit softlink
                ImmutableMap.of(
                    MigrationEntryImpl.METADATA_NAME,
                    MIGRATABLE_NAME2,
                    MigrationEntryImpl.METADATA_CHECKSUM,
                    checksum2,
                    MigrationEntryImpl.METADATA_SOFTLINK,
                    softlink2)),
            MigrationContextImpl.METADATA_JAVA_PROPERTIES,
            ImmutableList.of( //
                ImmutableMap.of(
                    MigrationEntryImpl.METADATA_NAME,
                    migratablePropertyName,
                    MigrationEntryImpl.METADATA_PROPERTY,
                    property,
                    MigrationEntryImpl.METADATA_REFERENCE,
                    MIGRATABLE_NAME)));

    context.processMetadata(metadata);

    Assert.assertThat(context.getVersion(), OptionalMatchers.hasValue(VERSION));
    Assert.assertThat(context.getEntries(), Matchers.aMapWithSize(3));
    Assert.assertThat(
        context.getEntries().values(),
        Matchers.hasItem( //
            Matchers.allOf( //
                MappingMatchers.map(
                    MigrationEntry::getName, Matchers.equalTo(migratablePropertyName)),
                MappingMatchers.map(
                    ImportMigrationEntryImpl::getJavaPropertyReferencedEntries,
                    Matchers.allOf( //
                        Matchers.aMapWithSize(1),
                        Matchers.hasValue(
                            MappingMatchers.map(
                                MigrationEntry::getName, Matchers.equalTo(MIGRATABLE_NAME))))))));
  }

  @Test
  public void testProcessMetadataWhenExternalsIsNotAList() throws Exception {
    final Map<String, Object> metadata =
        ImmutableMap.of(
            MigrationContextImpl.METADATA_VERSION,
            VERSION,
            MigrationContextImpl.METADATA_EXTERNALS,
            "not a list");

    thrown.expect(MigrationException.class);
    thrown.expectMessage(Matchers.containsString("invalid metadata"));
    thrown.expectMessage(
        Matchers.containsString("[" + MigrationContextImpl.METADATA_EXTERNALS + "]"));

    context.processMetadata(metadata);
  }

  @Test
  public void testProcessMetadataWhenSystemPropertiesIsNotAList() throws Exception {
    final Map<String, Object> metadata =
        ImmutableMap.of(
            MigrationContextImpl.METADATA_VERSION,
            VERSION,
            MigrationContextImpl.METADATA_SYSTEM_PROPERTIES,
            "not a list");

    thrown.expect(MigrationException.class);
    thrown.expectMessage(Matchers.containsString("invalid metadata"));
    thrown.expectMessage(
        Matchers.containsString("[" + MigrationContextImpl.METADATA_SYSTEM_PROPERTIES + "]"));

    context.processMetadata(metadata);
  }

  @Test
  public void testProcessMetadataWhenJavaPropertiesIsNotAList() throws Exception {
    final Map<String, Object> metadata =
        ImmutableMap.of(
            MigrationContextImpl.METADATA_VERSION,
            VERSION,
            MigrationContextImpl.METADATA_JAVA_PROPERTIES,
            "not a list");

    thrown.expect(MigrationException.class);
    thrown.expectMessage(Matchers.containsString("invalid metadata"));
    thrown.expectMessage(
        Matchers.containsString("[" + MigrationContextImpl.METADATA_JAVA_PROPERTIES + "]"));

    context.processMetadata(metadata);
  }

  @Test
  public void testDoImportForSystemContext() throws Exception {
    // no migratable and no id
    context.doImport();

    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
  }

  @Test
  public void testDoImportWhenNoMigratableInstalled() throws Exception {
    context = new ImportMigrationContextImpl(report, zip, MIGRATABLE_ID);

    context.doImport();

    Assert.assertThat(report.hasErrors(), Matchers.equalTo(true));

    thrown.expect(MigrationException.class);
    thrown.expectMessage(Matchers.equalTo("Import error: unknown data found in exported file."));

    report.verifyCompletion(); // trigger the exception
  }

  @Test
  public void testDoImportWhenVersionIsCompatible() throws Exception {
    final Map<String, Object> metadata =
        ImmutableMap.of(MigrationContextImpl.METADATA_VERSION, VERSION);

    context = new ImportMigrationContextImpl(report, zip, migratable);
    context.processMetadata(metadata); // make sure context has a version

    Mockito.when(migratable.getVersion()).thenReturn(VERSION);
    Mockito.doNothing().when(migratable).doImport(Mockito.any());

    context.doImport();

    Mockito.verify(migratable).doImport(context);
    Mockito.verify(migratable, Mockito.never()).doIncompatibleImport(context, VERSION);
    Mockito.verify(migratable, Mockito.never()).doMissingImport(context);
  }

  @Test
  public void testDoImportWhenVersionIsIncompatible() throws Exception {
    final Map<String, Object> metadata =
        ImmutableMap.of(MigrationContextImpl.METADATA_VERSION, VERSION);

    context = new ImportMigrationContextImpl(report, zip, migratable);
    context.processMetadata(metadata); // make sure context has a version

    Mockito.when(migratable.getVersion()).thenReturn(VERSION + "2");
    Mockito.doNothing().when(migratable).doIncompatibleImport(Mockito.any(), Mockito.eq(VERSION));

    context.doImport();

    Mockito.verify(migratable, Mockito.never()).doImport(context);
    Mockito.verify(migratable).doIncompatibleImport(context, VERSION);
    Mockito.verify(migratable, Mockito.never()).doMissingImport(context);
  }

  @Test
  public void testDoImportWhenNotExported() throws Exception {
    context = new ImportMigrationContextImpl(report, zip, migratable);

    Mockito.doNothing().when(migratable).doMissingImport(Mockito.any());

    context.doImport();

    Mockito.verify(migratable, Mockito.never()).doImport(context);
    Mockito.verify(migratable, Mockito.never()).doIncompatibleImport(context, VERSION);
    Mockito.verify(migratable).doMissingImport(context);
  }
}
