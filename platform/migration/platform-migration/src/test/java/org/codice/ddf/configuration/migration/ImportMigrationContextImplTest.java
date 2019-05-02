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
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.io.FilenameUtils;
import org.codice.ddf.migration.ImportMigrationEntry;
import org.codice.ddf.migration.Migratable;
import org.codice.ddf.migration.MigrationEntry;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationOperation;
import org.codice.ddf.test.common.matchers.CastingMatchers;
import org.codice.ddf.test.common.matchers.MappingMatchers;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ImportMigrationContextImplTest extends AbstractMigrationSupport {

  private static final String[] DIRS = new String[] {"where", "some", "dir"};

  private static final String[] DIRS2 = new String[] {"where", "some"};

  private static final String PROPERTY_NAME = "test.property";

  private static final String MIGRATABLE_NAME = "where/some/dir/test.txt";

  private static final String MIGRATABLE_NAME2 = "where/some/test.txt";

  private static final String MIGRATABLE_DIR = "where/some/dir";

  private static final String MIGRATABLE_DIR2 = "where/someOther";

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

  private MigrationZipFile mockMigrationZipFile;

  private ImportMigrationContextImpl context;

  @Before
  public void setup() throws Exception {
    mockMigrationZipFile = Mockito.mock(MigrationZipFile.class);
    initMigratableMock();

    Mockito.when(ENTRY.getPath()).thenReturn(MIGRATABLE_PATH);
    Mockito.when(ENTRY2.getPath()).thenReturn(MIGRATABLE_PATH2);

    context = new ImportMigrationContextImpl(report, mockMigrationZipFile);
  }

  @Test
  public void testConstructorWithNoMigratableOrId() throws Exception {
    Assert.assertThat(context.getReport(), Matchers.sameInstance(report));
    Assert.assertThat(context.getMigratable(), Matchers.nullValue());
    Assert.assertThat(context.getId(), Matchers.nullValue());
    Assert.assertThat(context.getVersion(), OptionalMatchers.isEmpty());
    Assert.assertThat(context.getZip(), Matchers.sameInstance(mockMigrationZipFile));
    Assert.assertThat(context.getFiles(), Matchers.empty());
    Assert.assertThat(context.getEntries(), Matchers.anEmptyMap());
    Assert.assertThat(context.getSystemPropertiesReferencedEntries(), Matchers.anEmptyMap());
  }

  @Test
  public void testConstructorWithNoMigratableOrIdAndNullReport() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null report"));

    new ImportMigrationContextImpl(null, mockMigrationZipFile);
  }

  @Test
  public void testConstructorWithId() throws Exception {
    context = new ImportMigrationContextImpl(report, mockMigrationZipFile, MIGRATABLE_ID);

    Assert.assertThat(context.getReport(), Matchers.sameInstance(report));
    Assert.assertThat(context.getMigratable(), Matchers.nullValue());
    Assert.assertThat(context.getId(), Matchers.equalTo(MIGRATABLE_ID));
    Assert.assertThat(context.getVersion(), OptionalMatchers.isEmpty());
    Assert.assertThat(context.getZip(), Matchers.sameInstance(mockMigrationZipFile));
    Assert.assertThat(context.getFiles(), Matchers.empty());
    Assert.assertThat(context.getEntries(), Matchers.anEmptyMap());
    Assert.assertThat(context.getSystemPropertiesReferencedEntries(), Matchers.anEmptyMap());
  }

  @Test
  public void testConstructorWithIdAndNullReport() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null "));

    new ImportMigrationContextImpl(null, mockMigrationZipFile, MIGRATABLE_ID);
  }

  @Test
  public void testConstructorWithNullId() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null migratable identifier"));

    new ImportMigrationContextImpl(report, mockMigrationZipFile, (String) null);
  }

  @Test
  public void testConstructorWithMigratable() throws Exception {
    context = new ImportMigrationContextImpl(report, mockMigrationZipFile, migratable, false);

    Assert.assertThat(context.getReport(), Matchers.sameInstance(report));
    Assert.assertThat(context.getMigratable(), Matchers.sameInstance(migratable));
    Assert.assertThat(context.getId(), Matchers.equalTo(MIGRATABLE_ID));
    Assert.assertThat(context.getVersion(), OptionalMatchers.isEmpty());
    Assert.assertThat(context.getZip(), Matchers.sameInstance(mockMigrationZipFile));
    Assert.assertThat(context.getFiles(), Matchers.empty());
    Assert.assertThat(context.getEntries(), Matchers.anEmptyMap());
    Assert.assertThat(context.getSystemPropertiesReferencedEntries(), Matchers.anEmptyMap());
  }

  @Test
  public void testConstructorWithMigratableAndNullReport() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null "));

    new ImportMigrationContextImpl(null, mockMigrationZipFile, migratable, false);
  }

  @Test
  public void testConstructorWithNullMigratable() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null migratable"));

    new ImportMigrationContextImpl(report, mockMigrationZipFile, (Migratable) null, false);
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
    thrown.expectMessage(Matchers.containsString("null path filter"));

    context.entries(MIGRATABLE_PATH, null);
  }

  @Test
  public void testGetOptionalEntry() throws Exception {
    context.getEntries().put(MIGRATABLE_PATH, ENTRY);

    final Optional<ImportMigrationEntryImpl> entry = context.getOptionalEntry(MIGRATABLE_PATH);

    Assert.assertThat(entry, OptionalMatchers.hasValue(Matchers.sameInstance(ENTRY)));
  }

  @Test
  public void testGetOptionalEntryWhenNotDefined() throws Exception {
    final Optional<ImportMigrationEntryImpl> entry = context.getOptionalEntry(MIGRATABLE_PATH);

    Assert.assertThat(entry, OptionalMatchers.isEmpty());
  }

  @Test
  public void testProcessMetadataWithNoEntries() throws Exception {
    final Map<String, Object> metadata =
        ImmutableMap.of(MigrationContextImpl.METADATA_VERSION, VERSION);

    context.processMetadata(metadata);

    Assert.assertThat(context.getVersion(), OptionalMatchers.hasValue(VERSION));
    Assert.assertThat(context.getFiles(), Matchers.empty());
    Assert.assertThat(context.entries().count(), Matchers.equalTo(0L));
    Assert.assertThat(context.getSystemPropertiesReferencedEntries(), Matchers.anEmptyMap());
  }

  @Test
  public void testProcessMetadataWithOnlyExternalEntries() throws Exception {
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
    Assert.assertThat(context.getFiles(), Matchers.empty());
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
  public void testProcessMetadataWithOnlyFiles() throws Exception {
    final Map<String, Object> metadata =
        ImmutableMap.of(
            MigrationContextImpl.METADATA_VERSION,
            VERSION,
            MigrationContextImpl.METADATA_FILES,
            ImmutableList.of( //
                ImmutableMap.of(MigrationEntryImpl.METADATA_NAME, MIGRATABLE_NAME),
                ImmutableMap.of(MigrationEntryImpl.METADATA_NAME, MIGRATABLE_NAME2)));

    context.processMetadata(metadata);

    Assert.assertThat(context.getVersion(), OptionalMatchers.hasValue(VERSION));
    Assert.assertThat(context.getFiles(), Matchers.contains(MIGRATABLE_NAME, MIGRATABLE_NAME2));
    Assert.assertThat(context.getEntries(), Matchers.anEmptyMap());
    Assert.assertThat(context.getSystemPropertiesReferencedEntries(), Matchers.anEmptyMap());
  }

  @Test
  public void testProcessMetadataWithOnlyFolders() throws Exception {
    final Map<String, Object> metadata =
        ImmutableMap.of(
            MigrationContextImpl.METADATA_VERSION,
            VERSION,
            MigrationContextImpl.METADATA_FOLDERS,
            ImmutableList.of( //
                ImmutableMap.of(
                    MigrationEntryImpl.METADATA_NAME,
                    MIGRATABLE_DIR,
                    MigrationEntryImpl.METADATA_FILES,
                    ImmutableList.of(MIGRATABLE_NAME, MIGRATABLE_NAME2)), // ommit filtered
                ImmutableMap.of(
                    MigrationEntryImpl.METADATA_NAME,
                    MIGRATABLE_DIR2,
                    MigrationEntryImpl.METADATA_FILTERED,
                    true,
                    MigrationEntryImpl.METADATA_FILES,
                    Collections.emptyList())));

    // pre-populate
    context.addEntry(ENTRY);

    context.processMetadata(metadata);

    Assert.assertThat(context.getVersion(), OptionalMatchers.hasValue(VERSION));
    Assert.assertThat(context.getFiles(), Matchers.empty());
    Assert.assertThat(context.getEntries(), Matchers.aMapWithSize(4));
    Assert.assertThat(
        context.entries().toArray(ImportMigrationEntry[]::new),
        Matchers.arrayContainingInAnyOrder( //
            Matchers.sameInstance(ENTRY), //
            Matchers.allOf( //
                MappingMatchers.map(MigrationEntry::getName, Matchers.equalTo(MIGRATABLE_NAME2)),
                Matchers.instanceOf(ImportMigrationEmptyEntryImpl.class)), //
            Matchers.allOf( //
                MappingMatchers.map(MigrationEntry::getName, Matchers.equalTo(MIGRATABLE_DIR)),
                CastingMatchers.cast(
                    ImportMigrationDirectoryEntryImpl.class,
                    Matchers.allOf( //
                        MappingMatchers.map(
                            ImportMigrationDirectoryEntryImpl::isFiltered, Matchers.equalTo(false)),
                        MappingMatchers.map(
                            ImportMigrationDirectoryEntryImpl::getFileEntries,
                            Matchers.containsInAnyOrder(
                                Matchers.sameInstance(ENTRY),
                                Matchers.sameInstance(context.getEntry(MIGRATABLE_PATH2))))))), //
            Matchers.allOf( //
                MappingMatchers.map(MigrationEntry::getName, Matchers.equalTo(MIGRATABLE_DIR2)),
                CastingMatchers.cast(
                    ImportMigrationDirectoryEntryImpl.class,
                    Matchers.allOf( //
                        MappingMatchers.map(
                            ImportMigrationDirectoryEntryImpl::isFiltered, Matchers.equalTo(true)),
                        MappingMatchers.map(
                            ImportMigrationDirectoryEntryImpl::getFileEntries,
                            Matchers.emptyIterable())))) //
            ) //
        );
    Assert.assertThat(context.getSystemPropertiesReferencedEntries(), Matchers.anEmptyMap());
  }

  @Test
  public void testProcessMetadataWithSystemPropertyReferencedEntriesAndNoFiles() throws Exception {
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
    Assert.assertThat(context.getFiles(), Matchers.empty());
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
  public void
      testProcessMetadataWithJavaPropertyReferencedEntriesWhenPropertiesWasAlsoExportedAndNoFiles()
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
    Assert.assertThat(context.getFiles(), Matchers.empty());
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
  public void
      testProcessMetadataWithJavaPropertyReferencedEntriesWhenPropertiesWasNotExportedAndNoFiles()
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
    Assert.assertThat(context.getFiles(), Matchers.empty());
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
  public void testProcessMetadataWhenFilesIsNotAList() throws Exception {
    final Map<String, Object> metadata =
        ImmutableMap.of(
            MigrationContextImpl.METADATA_VERSION,
            VERSION,
            MigrationContextImpl.METADATA_FILES,
            "not a list");

    thrown.expect(MigrationException.class);
    thrown.expectMessage(Matchers.containsString("invalid metadata"));
    thrown.expectMessage(Matchers.containsString("[" + MigrationContextImpl.METADATA_FILES + "]"));

    context.processMetadata(metadata);
  }

  @Test
  public void testProcessMetadataWhenFoldersIsNotAList() throws Exception {
    final Map<String, Object> metadata =
        ImmutableMap.of(
            MigrationContextImpl.METADATA_VERSION,
            VERSION,
            MigrationContextImpl.METADATA_FOLDERS,
            "not a list");

    thrown.expect(MigrationException.class);
    thrown.expectMessage(Matchers.containsString("invalid metadata"));
    thrown.expectMessage(
        Matchers.containsString("[" + MigrationContextImpl.METADATA_FOLDERS + "]"));

    context.processMetadata(metadata);
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
    context = new ImportMigrationContextImpl(report, mockMigrationZipFile, MIGRATABLE_ID);

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

    context = new ImportMigrationContextImpl(report, mockMigrationZipFile, migratable, false);
    context.processMetadata(metadata); // make sure context has a version

    Mockito.when(migratable.getVersion()).thenReturn(VERSION);
    Mockito.doNothing().when(migratable).doImport(Mockito.any());

    context.doImport();

    Mockito.verify(migratable).doImport(context);
    Mockito.verify(migratable, Mockito.never()).doVersionUpgradeImport(context, VERSION);
    Mockito.verify(migratable, Mockito.never()).doMissingImport(context);
  }

  @Test
  public void testDoImportWhenVersionIsIncompatible() throws Exception {
    final Map<String, Object> metadata =
        ImmutableMap.of(MigrationContextImpl.METADATA_VERSION, VERSION);

    context = new ImportMigrationContextImpl(report, mockMigrationZipFile, migratable, false);
    context.processMetadata(metadata); // make sure context has a version

    Mockito.when(migratable.getVersion()).thenReturn(VERSION + "2");
    Mockito.doNothing().when(migratable).doVersionUpgradeImport(Mockito.any(), Mockito.eq(VERSION));

    context.doImport();

    Mockito.verify(migratable, Mockito.never()).doImport(context);
    Mockito.verify(migratable).doVersionUpgradeImport(context, VERSION);
    Mockito.verify(migratable, Mockito.never()).doMissingImport(context);
  }

  @Test
  public void testDoImportWhenNotExported() throws Exception {
    context = new ImportMigrationContextImpl(report, mockMigrationZipFile, migratable, false);

    Mockito.doNothing().when(migratable).doMissingImport(Mockito.any());

    context.doImport();

    Mockito.verify(migratable, Mockito.never()).doImport(context);
    Mockito.verify(migratable, Mockito.never()).doVersionUpgradeImport(context, VERSION);
    Mockito.verify(migratable).doMissingImport(context);
  }

  @Test
  public void testDoImportWhenRequestedToSkip() throws Exception {
    final Map<String, Object> metadata =
        ImmutableMap.of(MigrationContextImpl.METADATA_VERSION, VERSION);

    context = new ImportMigrationContextImpl(report, mockMigrationZipFile, migratable, true);
    context.processMetadata(metadata); // make sure context has a version

    Mockito.when(migratable.getVersion()).thenReturn(VERSION);
    Mockito.doNothing().when(migratable).doImport(Mockito.any());

    context.doImport();

    Mockito.verify(migratable, Mockito.never()).doImport(context);
    Mockito.verify(migratable, Mockito.never()).doVersionUpgradeImport(context, VERSION);
    Mockito.verify(migratable, Mockito.never()).doMissingImport(context);
  }
}
