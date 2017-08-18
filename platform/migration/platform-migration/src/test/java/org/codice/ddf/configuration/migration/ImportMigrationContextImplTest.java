package org.codice.ddf.configuration.migration;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipFile;

import org.apache.commons.io.FilenameUtils;
import org.codice.ddf.migration.ImportMigrationEntry;
import org.codice.ddf.migration.ImportMigrationException;
import org.codice.ddf.migration.Migratable;
import org.codice.ddf.migration.MigrationEntry;
import org.codice.ddf.migration.MigrationOperation;
import org.codice.ddf.test.util.CastingMatchers;
import org.codice.ddf.test.util.MappingMatchers;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.github.npathai.hamcrestopt.OptionalMatchers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class ImportMigrationContextImplTest extends AbstractMigrationTest {
    private static final String[] DIRS = new String[] {"where", "some", "dir"};

    private static final String[] DIRS2 = new String[] {"where", "some"};

    private static final String PROPERTY_NAME = "test.property";

    private static final String MIGRATABLE_NAME = "where/some/dir/test.txt";

    private static final String MIGRATABLE_NAME2 = "where/some/test.txt";

    private static final Path MIGRATABLE_PATH = Paths.get(FilenameUtils.separatorsToSystem(
            MIGRATABLE_NAME));

    private static final Path MIGRATABLE_PATH2 = Paths.get(FilenameUtils.separatorsToSystem(
            MIGRATABLE_NAME2));

    private static final ImportMigrationEntryImpl ENTRY =
            Mockito.mock(ImportMigrationEntryImpl.class);

    private static final ImportMigrationEntryImpl ENTRY2 =
            Mockito.mock(ImportMigrationEntryImpl.class);

    private static final ImportMigrationSystemPropertyReferencedEntryImpl SYS_ENTRY = Mockito.mock(
            ImportMigrationSystemPropertyReferencedEntryImpl.class);

    private final MigrationReportImpl REPORT = new MigrationReportImpl(MigrationOperation.IMPORT,
            Optional.empty());

    private final ZipFile ZIP = Mockito.mock(ZipFile.class);

    private ImportMigrationContextImpl CONTEXT;

    @Before
    public void before() throws Exception {
        initMigratableMock();

        Mockito.when(ENTRY.getPath())
                .thenReturn(MIGRATABLE_PATH);
        Mockito.when(ENTRY2.getPath())
                .thenReturn(MIGRATABLE_PATH2);

        CONTEXT = new ImportMigrationContextImpl(REPORT, ZIP);
    }

    @Test
    public void testConstructorWithNoMigratableOrId() throws Exception {
        Assert.assertThat(CONTEXT.getReport(), Matchers.sameInstance(REPORT));
        Assert.assertThat(CONTEXT.getMigratable(), Matchers.nullValue());
        Assert.assertThat(CONTEXT.getId(), Matchers.nullValue());
        Assert.assertThat(CONTEXT.getVersion(), Matchers.equalTo("?"));
        Assert.assertThat(CONTEXT.getZip(), Matchers.sameInstance(ZIP));
        Assert.assertThat(CONTEXT.getEntries(), Matchers.anEmptyMap());
        Assert.assertThat(CONTEXT.getSystemPropertiesReferencedEntries(), Matchers.anEmptyMap());
    }

    @Test
    public void testConstructorWithNoMigratableOrIdAndNullReport() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null report"));

        new ImportMigrationContextImpl(null, ZIP);
    }

    @Test
    public void testConstructorWithNoMigratableOrIdAndNullZip() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null zip"));

        new ImportMigrationContextImpl(REPORT, null);
    }

    @Test
    public void testConstructorWithId() throws Exception {
        CONTEXT = new ImportMigrationContextImpl(REPORT, ZIP, MIGRATABLE_ID);

        Assert.assertThat(CONTEXT.getReport(), Matchers.sameInstance(REPORT));
        Assert.assertThat(CONTEXT.getMigratable(), Matchers.nullValue());
        Assert.assertThat(CONTEXT.getId(), Matchers.equalTo(MIGRATABLE_ID));
        Assert.assertThat(CONTEXT.getVersion(), Matchers.nullValue());
        Assert.assertThat(CONTEXT.getZip(), Matchers.sameInstance(ZIP));
        Assert.assertThat(CONTEXT.getEntries(), Matchers.anEmptyMap());
        Assert.assertThat(CONTEXT.getSystemPropertiesReferencedEntries(), Matchers.anEmptyMap());
    }

    @Test
    public void testConstructorWithIdAndNullReport() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null "));

        new ImportMigrationContextImpl(null, ZIP, MIGRATABLE_ID);
    }

    @Test
    public void testConstructorWithIdAndNullZip() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null zip"));

        new ImportMigrationContextImpl(REPORT, null, MIGRATABLE_ID);
    }

    @Test
    public void testConstructorWithNullId() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null migratable identifier"));

        new ImportMigrationContextImpl(REPORT, ZIP, (String) null);
    }

    @Test
    public void testConstructorWithMigratable() throws Exception {
        CONTEXT = new ImportMigrationContextImpl(REPORT, ZIP, MIGRATABLE);

        Assert.assertThat(CONTEXT.getReport(), Matchers.sameInstance(REPORT));
        Assert.assertThat(CONTEXT.getMigratable(), Matchers.sameInstance(MIGRATABLE));
        Assert.assertThat(CONTEXT.getId(), Matchers.equalTo(MIGRATABLE_ID));
        Assert.assertThat(CONTEXT.getVersion(), Matchers.nullValue());
        Assert.assertThat(CONTEXT.getZip(), Matchers.sameInstance(ZIP));
        Assert.assertThat(CONTEXT.getEntries(), Matchers.anEmptyMap());
        Assert.assertThat(CONTEXT.getSystemPropertiesReferencedEntries(), Matchers.anEmptyMap());
    }

    @Test
    public void testConstructorWithMigratableAndNullReport() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null "));

        new ImportMigrationContextImpl(null, ZIP, MIGRATABLE);
    }

    @Test
    public void testConstructorWithMigratableAndNullZip() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null zip"));

        new ImportMigrationContextImpl(REPORT, null, MIGRATABLE);
    }

    @Test
    public void testConstructorWithNullMigratable() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null migratable"));

        new ImportMigrationContextImpl(REPORT, ZIP, (Migratable) null);
    }

    @Test
    public void testGetSystemPropertyReferencedEntry() throws Exception {
        CONTEXT.getSystemPropertiesReferencedEntries()
                .put(PROPERTY_NAME, SYS_ENTRY);

        final Optional<ImportMigrationEntry> entry = CONTEXT.getSystemPropertyReferencedEntry(
                PROPERTY_NAME);

        Assert.assertThat(entry, OptionalMatchers.hasValue(Matchers.sameInstance(SYS_ENTRY)));
    }

    @Test
    public void testGetSystemPropertyReferencedEntryWhenNotDefined() throws Exception {
        final Optional<ImportMigrationEntry> entry = CONTEXT.getSystemPropertyReferencedEntry(
                PROPERTY_NAME);

        Assert.assertThat(entry, OptionalMatchers.isEmpty());
    }

    @Test
    public void testGetSystemPropertyReferencedEntryWithNullName() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null system property name"));

        CONTEXT.getSystemPropertyReferencedEntry(null);
    }

    @Test
    public void testGetEntry() throws Exception {
        CONTEXT.getEntries()
                .put(MIGRATABLE_PATH, ENTRY);

        final ImportMigrationEntry entry = CONTEXT.getEntry(MIGRATABLE_PATH);

        Assert.assertThat(entry, Matchers.sameInstance(ENTRY));
    }

    @Test
    public void testGetEntryWhenNotDefined() throws Exception {
        final ImportMigrationEntry entry = CONTEXT.getEntry(MIGRATABLE_PATH);

        Assert.assertThat(entry, Matchers.instanceOf(ImportMigrationEmptyEntryImpl.class));
        Assert.assertThat(entry.getReport(), Matchers.sameInstance(REPORT));
        Assert.assertThat(entry.getPath(), Matchers.equalTo(MIGRATABLE_PATH));
    }

    @Test
    public void testGetEntryWithNullPath() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null path"));

        CONTEXT.getEntry(null);
    }

    @Test
    public void testEntries() throws Exception {
        CONTEXT.getEntries()
                .put(MIGRATABLE_PATH, ENTRY);
        CONTEXT.getEntries()
                .put(MIGRATABLE_PATH2, ENTRY2);

        Assert.assertThat(CONTEXT.entries()
                        .toArray(ImportMigrationEntry[]::new),
                Matchers.arrayContainingInAnyOrder(Matchers.sameInstance(ENTRY),
                        Matchers.sameInstance(ENTRY2)));
    }

    @Test
    public void testEntriesWhenEmpty() throws Exception {
        Assert.assertThat(CONTEXT.entries()
                .count(), Matchers.equalTo(0L));
    }

    @Test
    public void testEntriesWithPath() throws Exception {
        final ImportMigrationEntryImpl ENTRY3 = Mockito.mock(ImportMigrationEntryImpl.class);

        Mockito.when(ENTRY3.getPath())
                .thenReturn(DDF_HOME);

        CONTEXT.getEntries()
                .put(MIGRATABLE_PATH, ENTRY);
        CONTEXT.getEntries()
                .put(MIGRATABLE_PATH2, ENTRY2);
        CONTEXT.getEntries()
                .put(DDF_HOME, ENTRY3);

        Assert.assertThat(CONTEXT.entries(MIGRATABLE_PATH2.getParent())
                        .toArray(ImportMigrationEntry[]::new),
                Matchers.arrayContainingInAnyOrder(Matchers.sameInstance(ENTRY),
                        Matchers.sameInstance(ENTRY2)));
    }

    @Test
    public void testEntriesWithNullPath() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null path"));

        CONTEXT.entries(null);
    }

    @Test
    public void testEntriesWithPathAndFilter() throws Exception {
        final ImportMigrationEntryImpl ENTRY3 = Mockito.mock(ImportMigrationEntryImpl.class);
        final PathMatcher FILTER = Mockito.mock(PathMatcher.class);

        Mockito.when(ENTRY3.getPath())
                .thenReturn(DDF_HOME);
        Mockito.when(FILTER.matches(Mockito.any())).thenReturn(false);
        Mockito.when(FILTER.matches(MIGRATABLE_PATH)).thenReturn(true);

        CONTEXT.getEntries()
                .put(MIGRATABLE_PATH, ENTRY);
        CONTEXT.getEntries()
                .put(MIGRATABLE_PATH2, ENTRY2);
        CONTEXT.getEntries()
                .put(DDF_HOME, ENTRY3);

        Assert.assertThat(CONTEXT.entries(MIGRATABLE_PATH2.getParent(), FILTER)
                        .toArray(ImportMigrationEntry[]::new),
                Matchers.arrayContainingInAnyOrder(Matchers.sameInstance(ENTRY)));

        Mockito.verify(FILTER).matches(MIGRATABLE_PATH);
        Mockito.verify(FILTER).matches(MIGRATABLE_PATH2);
        Mockito.verify(FILTER, Mockito.never()).matches(DDF_HOME);
    }

    @Test
    public void testEntriesWithFilterAndNullPath() throws Exception {
        final PathMatcher FILTER = Mockito.mock(PathMatcher.class);

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null path"));

        CONTEXT.entries(null, FILTER);
    }

    @Test
    public void testEntriesWithPathAndNullFilter() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null filter"));

        CONTEXT.entries(MIGRATABLE_PATH, null);
    }

    @Test
    public void testCleanDirectoryWithAParentAbsolutePath() throws Exception {
        final Path DIR2 = DDF_HOME.resolve(createDirectory(DIRS2));
        final Path DIR = createDirectory(DIRS);
        final Path PATH2 = DDF_HOME.resolve(createFile(MIGRATABLE_PATH2));
        final Path PATH = DDF_HOME.resolve(createFile(MIGRATABLE_PATH));

        Assert.assertThat(CONTEXT.cleanDirectory(DIR2), Matchers.equalTo(true));

        Assert.assertThat(DIR2.toFile()
                .exists(), Matchers.equalTo(true));
        Assert.assertThat(PATH2.toFile()
                .exists(), Matchers.equalTo(false));
        Assert.assertThat(DIR.toFile()
                .exists(), Matchers.equalTo(false));
        Assert.assertThat(PATH.toFile()
                .exists(), Matchers.equalTo(false));
    }

    @Test
    public void testCleanDirectoryWithAChildAbsolutePath() throws Exception {
        final Path DIR2 = DDF_HOME.resolve(createDirectory(DIRS2));
        final Path DIR = createDirectory(DIRS);
        final Path PATH2 = DDF_HOME.resolve(createFile(MIGRATABLE_PATH2));
        final Path PATH = DDF_HOME.resolve(createFile(MIGRATABLE_PATH));

        Assert.assertThat(CONTEXT.cleanDirectory(DIR), Matchers.equalTo(true));

        Assert.assertThat(DIR2.toFile()
                .exists(), Matchers.equalTo(true));
        Assert.assertThat(PATH2.toFile()
                .exists(), Matchers.equalTo(true));
        Assert.assertThat(DIR.toFile()
                .exists(), Matchers.equalTo(true));
        Assert.assertThat(PATH.toFile()
                .exists(), Matchers.equalTo(false));
    }

    @Test
    public void testCleanDirectoryWithRelativePath() throws Exception {
        final Path DIR2 = DDF_HOME.resolve(createDirectory(DIRS2));
        final Path DIR = createDirectory(DIRS);
        final Path PATH2 = DDF_HOME.resolve(createFile(MIGRATABLE_PATH2));
        final Path PATH = DDF_HOME.resolve(createFile(MIGRATABLE_PATH));

        Assert.assertThat(CONTEXT.cleanDirectory(MIGRATABLE_PATH.getParent()),
                Matchers.equalTo(true));

        Assert.assertThat(DIR2.toFile()
                .exists(), Matchers.equalTo(true));
        Assert.assertThat(PATH2.toFile()
                .exists(), Matchers.equalTo(true));
        Assert.assertThat(DIR.toFile()
                .exists(), Matchers.equalTo(true));
        Assert.assertThat(PATH.toFile()
                .exists(), Matchers.equalTo(false));
    }

    @Test
    public void testCleanDirectoryWithNullPath() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null path"));

        CONTEXT.cleanDirectory(null);
    }

    @Test
    public void testCleanDirectoryWithNonExistentPath() throws Exception {
        Assert.assertThat(CONTEXT.cleanDirectory(MIGRATABLE_PATH.getParent()),
                Matchers.equalTo(true));
    }

    @Test
    public void testCleanDirectoryWithAFile() throws Exception {
        final Path DIR = createDirectory(DIRS);
        final Path PATH = DDF_HOME.resolve(createFile(MIGRATABLE_PATH));

        Assert.assertThat(CONTEXT.cleanDirectory(MIGRATABLE_PATH), Matchers.equalTo(false));

        Assert.assertThat(DIR.toFile()
                .exists(), Matchers.equalTo(true));
        Assert.assertThat(PATH.toFile()
                .exists(), Matchers.equalTo(true));
    }

    @Test
    public void testCleanDirectoryWithAReadOnlyPath() throws Exception {
        final Path DIR = createDirectory(DIRS);
        final Path PATH = DDF_HOME.resolve(createFile(MIGRATABLE_PATH));

        DIR.toFile()
                .setReadable(false);

        try {
            Assert.assertThat(CONTEXT.cleanDirectory(DIR), Matchers.equalTo(false));

            Assert.assertThat(DIR.toFile()
                    .exists(), Matchers.equalTo(true));
            Assert.assertThat(PATH.toFile()
                    .exists(), Matchers.equalTo(true));
        } finally {
            DIR.toFile()
                    .setReadable(true);
        }
    }

    @Test
    public void testGetOptionalEntry() throws Exception {
        CONTEXT.getEntries()
                .put(MIGRATABLE_PATH, ENTRY);

        final Optional<ImportMigrationEntry> entry = CONTEXT.getOptionalEntry(MIGRATABLE_PATH);

        Assert.assertThat(entry, OptionalMatchers.hasValue(Matchers.sameInstance(ENTRY)));
    }

    @Test
    public void testGetOptionalEntryWhenNotDefined() throws Exception {
        final Optional<ImportMigrationEntry> entry = CONTEXT.getOptionalEntry(MIGRATABLE_PATH);

        Assert.assertThat(entry, OptionalMatchers.isEmpty());
    }

    private void putMetadata(Map<String, Object> metadata, String name, String checksum) {
        metadata.put(MigrationEntryImpl.METADATA_NAME, name);
        metadata.put(MigrationEntryImpl.METADATA_CHECKSUM, checksum);
    }

    @Test
    public void testProcessMetadataWithNoEntries() throws Exception {
        final Map<String, Object> METADATA = ImmutableMap.of(MigrationContextImpl.METADATA_VERSION,
                VERSION);

        CONTEXT.processMetadata(METADATA);

        Assert.assertThat(CONTEXT.getVersion(), Matchers.equalTo(VERSION));
        Assert.assertThat(CONTEXT.entries()
                .count(), Matchers.equalTo(0L));
        Assert.assertThat(CONTEXT.getSystemPropertiesReferencedEntries(), Matchers.anEmptyMap());
    }

    @Test
    public void testProcessMetadataWithOnlyEntries() throws Exception {
        final String CHECKSUM = "abcdef";
        final String CHECKSUM2 = "12345";
        final boolean SOFTLINK = false;
        final boolean SOFTLINK2 = true;
        final Map<String, Object> METADATA = ImmutableMap.of(MigrationContextImpl.METADATA_VERSION,
                VERSION,
                MigrationContextImpl.METADATA_EXTERNALS,
                ImmutableList.of( //
                        ImmutableMap.of(MigrationEntryImpl.METADATA_NAME,
                                MIGRATABLE_NAME,
                                MigrationEntryImpl.METADATA_CHECKSUM,
                                CHECKSUM), // ommit softlink
                        ImmutableMap.of(MigrationEntryImpl.METADATA_NAME,
                                MIGRATABLE_NAME2,
                                MigrationEntryImpl.METADATA_CHECKSUM,
                                CHECKSUM2,
                                MigrationEntryImpl.METADATA_SOFTLINK,
                                SOFTLINK2)));

        CONTEXT.processMetadata(METADATA);

        Assert.assertThat(CONTEXT.getVersion(), Matchers.equalTo(VERSION));
        Assert.assertThat(CONTEXT.entries()
                .toArray(ImportMigrationEntry[]::new), Matchers.arrayContainingInAnyOrder( //
                Matchers.allOf( //
                        MappingMatchers.map(MigrationEntry::getName,
                                Matchers.equalTo(MIGRATABLE_NAME)),
                        CastingMatchers.cast(ImportMigrationExternalEntryImpl.class,
                                Matchers.allOf( //
                                        MappingMatchers.map(ImportMigrationExternalEntryImpl::getChecksum,
                                                Matchers.equalTo(CHECKSUM)),
                                        MappingMatchers.map(ImportMigrationExternalEntryImpl::isSoftlink,
                                                Matchers.equalTo(SOFTLINK))))), //
                Matchers.allOf( //
                        MappingMatchers.map(MigrationEntry::getName,
                                Matchers.equalTo(MIGRATABLE_NAME2)),
                        CastingMatchers.cast(ImportMigrationExternalEntryImpl.class,
                                Matchers.allOf( //
                                        MappingMatchers.map(ImportMigrationExternalEntryImpl::getChecksum,
                                                Matchers.equalTo(CHECKSUM2)),
                                        MappingMatchers.map(ImportMigrationExternalEntryImpl::isSoftlink,
                                                Matchers.equalTo(SOFTLINK2)))))));
        Assert.assertThat(CONTEXT.getSystemPropertiesReferencedEntries(), Matchers.anEmptyMap());
    }

    @Test
    public void testProcessMetadataWithSystemPropertyReferencedEntries() throws Exception {
        final String PROPERTY = "property.name";
        final String CHECKSUM = "abcdef";
        final String CHECKSUM2 = "12345";
        final boolean SOFTLINK2 = true;
        final Map<String, Object> METADATA = ImmutableMap.of(MigrationContextImpl.METADATA_VERSION,
                VERSION,
                MigrationContextImpl.METADATA_EXTERNALS,
                ImmutableList.of( //
                        ImmutableMap.of(MigrationEntryImpl.METADATA_NAME,
                                MIGRATABLE_NAME,
                                MigrationEntryImpl.METADATA_CHECKSUM,
                                CHECKSUM), // ommit softlink
                        ImmutableMap.of(MigrationEntryImpl.METADATA_NAME,
                                MIGRATABLE_NAME2,
                                MigrationEntryImpl.METADATA_CHECKSUM,
                                CHECKSUM2,
                                MigrationEntryImpl.METADATA_SOFTLINK,
                                SOFTLINK2)),
                MigrationContextImpl.METADATA_SYSTEM_PROPERTIES,
                ImmutableList.of( //
                        ImmutableMap.of(MigrationEntryImpl.METADATA_PROPERTY,
                                PROPERTY,
                                MigrationEntryImpl.METADATA_REFERENCE,
                                MIGRATABLE_NAME)));

        CONTEXT.processMetadata(METADATA);

        Assert.assertThat(CONTEXT.getVersion(), Matchers.equalTo(VERSION));
        Assert.assertThat(CONTEXT.getEntries(), Matchers.aMapWithSize(2));
        Assert.assertThat(CONTEXT.getSystemPropertiesReferencedEntries()
                        .values()
                        .stream()
                        .toArray(ImportMigrationSystemPropertyReferencedEntryImpl[]::new),
                Matchers.arrayContaining( //
                        Matchers.allOf( //
                                MappingMatchers.map(ImportMigrationPropertyReferencedEntryImpl::getProperty,
                                        Matchers.equalTo(PROPERTY)),
                                MappingMatchers.map(ImportMigrationPropertyReferencedEntryImpl::getReferenced,
                                        MappingMatchers.map(MigrationEntry::getName,
                                                Matchers.equalTo(MIGRATABLE_NAME))))));
    }

    @Test
    public void testProcessMetadataWithSystemPropertyReferencedEntriesThatWereNotExported()
            throws Exception {
        final String PROPERTY = "property.name";
        final String CHECKSUM2 = "12345";
        final boolean SOFTLINK2 = true;
        final Map<String, Object> METADATA = ImmutableMap.of(MigrationContextImpl.METADATA_VERSION,
                VERSION,
                MigrationContextImpl.METADATA_EXTERNALS,
                ImmutableList.of( //
                        ImmutableMap.of(MigrationEntryImpl.METADATA_NAME,
                                MIGRATABLE_NAME2,
                                MigrationEntryImpl.METADATA_CHECKSUM,
                                CHECKSUM2,
                                MigrationEntryImpl.METADATA_SOFTLINK,
                                SOFTLINK2)),
                MigrationContextImpl.METADATA_SYSTEM_PROPERTIES,
                ImmutableList.of( //
                        ImmutableMap.of(MigrationEntryImpl.METADATA_PROPERTY,
                                PROPERTY,
                                MigrationEntryImpl.METADATA_REFERENCE,
                                MIGRATABLE_NAME)));

        thrown.expect(ImportMigrationException.class);
        thrown.expectMessage(Matchers.containsString("referenced path [" + MIGRATABLE_NAME + "]"));

        CONTEXT.processMetadata(METADATA);
    }

    @Test
    public void testProcessMetadataWithJavaPropertyReferencedEntriesWhenPropertiesWasAlsoExported()
            throws Exception {
        final String MIGRATABLE_PROPERTY_NAME = "where/some/dir/test.properties";
        final String PROPERTY = "property.name";
        final String CHECKSUM = "abcdef";
        final String CHECKSUM2 = "12345";
        final String PROPERTY_CHECKSUM = "a1b2c3d4e5";
        final boolean SOFTLINK2 = true;
        final boolean PROPERTY_SOFTLINK = false;
        final Map<String, Object> METADATA = ImmutableMap.of(MigrationContextImpl.METADATA_VERSION,
                VERSION,
                MigrationContextImpl.METADATA_EXTERNALS,
                ImmutableList.of( //
                        ImmutableMap.of(MigrationEntryImpl.METADATA_NAME,
                                MIGRATABLE_NAME,
                                MigrationEntryImpl.METADATA_CHECKSUM,
                                CHECKSUM),
                        // ommit softlink
                        ImmutableMap.of(MigrationEntryImpl.METADATA_NAME,
                                MIGRATABLE_NAME2,
                                MigrationEntryImpl.METADATA_CHECKSUM,
                                CHECKSUM2,
                                MigrationEntryImpl.METADATA_SOFTLINK,
                                SOFTLINK2),
                        ImmutableMap.of(MigrationEntryImpl.METADATA_NAME,
                                MIGRATABLE_PROPERTY_NAME,
                                MigrationEntryImpl.METADATA_CHECKSUM,
                                PROPERTY_CHECKSUM,
                                MigrationEntryImpl.METADATA_SOFTLINK,
                                PROPERTY_SOFTLINK)),
                MigrationContextImpl.METADATA_JAVA_PROPERTIES,
                ImmutableList.of( //
                        ImmutableMap.of(MigrationEntryImpl.METADATA_NAME,
                                MIGRATABLE_PROPERTY_NAME,
                                MigrationEntryImpl.METADATA_PROPERTY,
                                PROPERTY,
                                MigrationEntryImpl.METADATA_REFERENCE,
                                MIGRATABLE_NAME)));

        CONTEXT.processMetadata(METADATA);

        Assert.assertThat(CONTEXT.getVersion(), Matchers.equalTo(VERSION));
        Assert.assertThat(CONTEXT.getEntries(), Matchers.aMapWithSize(3));
        Assert.assertThat(CONTEXT.getEntries()
                .values(), Matchers.hasItem( //
                Matchers.allOf( //
                        MappingMatchers.map(MigrationEntry::getName,
                                Matchers.equalTo(MIGRATABLE_PROPERTY_NAME)),
                        CastingMatchers.cast(ImportMigrationExternalEntryImpl.class,
                                Matchers.allOf( //
                                        MappingMatchers.map(ImportMigrationExternalEntryImpl::getChecksum,
                                                Matchers.equalTo(PROPERTY_CHECKSUM)),
                                        MappingMatchers.map(ImportMigrationExternalEntryImpl::isSoftlink,
                                                Matchers.equalTo(PROPERTY_SOFTLINK)))),
                        MappingMatchers.map(ImportMigrationEntryImpl::getJavaPropertyReferencedEntries,
                                Matchers.allOf( //
                                        Matchers.aMapWithSize(1),
                                        Matchers.hasValue(MappingMatchers.map(MigrationEntry::getName,
                                                Matchers.equalTo(MIGRATABLE_NAME))))))));
    }

    @Test
    public void testProcessMetadataWithJavaPropertyReferencedEntriesWhenPropertiesWasNotExported()
            throws Exception {
        final String MIGRATABLE_PROPERTY_NAME = "where/some/dir/test.properties";
        final String PROPERTY = "property.name";
        final String CHECKSUM = "abcdef";
        final String CHECKSUM2 = "12345";
        final String PROPERTY_CHECKSUM = "a1b2c3d4e5";
        final boolean SOFTLINK2 = true;
        final boolean PROPERTY_SOFTLINK = false;
        final Map<String, Object> METADATA = ImmutableMap.of(MigrationContextImpl.METADATA_VERSION,
                VERSION,
                MigrationContextImpl.METADATA_EXTERNALS,
                ImmutableList.of( //
                        ImmutableMap.of(MigrationEntryImpl.METADATA_NAME,
                                MIGRATABLE_NAME,
                                MigrationEntryImpl.METADATA_CHECKSUM,
                                CHECKSUM), // ommit softlink
                        ImmutableMap.of(MigrationEntryImpl.METADATA_NAME,
                                MIGRATABLE_NAME2,
                                MigrationEntryImpl.METADATA_CHECKSUM,
                                CHECKSUM2,
                                MigrationEntryImpl.METADATA_SOFTLINK,
                                SOFTLINK2)),
                MigrationContextImpl.METADATA_JAVA_PROPERTIES,
                ImmutableList.of( //
                        ImmutableMap.of(MigrationEntryImpl.METADATA_NAME,
                                MIGRATABLE_PROPERTY_NAME,
                                MigrationEntryImpl.METADATA_PROPERTY,
                                PROPERTY,
                                MigrationEntryImpl.METADATA_REFERENCE,
                                MIGRATABLE_NAME)));

        CONTEXT.processMetadata(METADATA);

        Assert.assertThat(CONTEXT.getVersion(), Matchers.equalTo(VERSION));
        Assert.assertThat(CONTEXT.getEntries(), Matchers.aMapWithSize(3));
        Assert.assertThat(CONTEXT.getEntries()
                .values(), Matchers.hasItem( //
                Matchers.allOf( //
                        MappingMatchers.map(MigrationEntry::getName,
                                Matchers.equalTo(MIGRATABLE_PROPERTY_NAME)),
                        MappingMatchers.map(ImportMigrationEntryImpl::getJavaPropertyReferencedEntries,
                                Matchers.allOf( //
                                        Matchers.aMapWithSize(1),
                                        Matchers.hasValue(MappingMatchers.map(MigrationEntry::getName,
                                                Matchers.equalTo(MIGRATABLE_NAME))))))));
    }

    @Test
    public void testProcessMetadataWhenExternalsIsNotAList() throws Exception {
        final Map<String, Object> METADATA = ImmutableMap.of(MigrationContextImpl.METADATA_VERSION,
                VERSION,
                MigrationContextImpl.METADATA_EXTERNALS,
                "not a list");

        thrown.expect(Exception.class);
        thrown.expectMessage(Matchers.containsString("invalid metadata"));
        thrown.expectMessage(Matchers.containsString(
                "'" + MigrationContextImpl.METADATA_EXTERNALS + "'"));

        CONTEXT.processMetadata(METADATA);
    }

    @Test
    public void testProcessMetadataWhenSystemPropertiesIsNotAList() throws Exception {
        final Map<String, Object> METADATA = ImmutableMap.of(MigrationContextImpl.METADATA_VERSION,
                VERSION,
                MigrationContextImpl.METADATA_SYSTEM_PROPERTIES,
                "not a list");

        thrown.expect(Exception.class);
        thrown.expectMessage(Matchers.containsString("invalid metadata"));
        thrown.expectMessage(Matchers.containsString(
                "'" + MigrationContextImpl.METADATA_SYSTEM_PROPERTIES + "'"));

        CONTEXT.processMetadata(METADATA);
    }

    @Test
    public void testProcessMetadataWhenJavaPropertiesIsNotAList() throws Exception {
        final Map<String, Object> METADATA = ImmutableMap.of(MigrationContextImpl.METADATA_VERSION,
                VERSION,
                MigrationContextImpl.METADATA_JAVA_PROPERTIES,
                "not a list");

        thrown.expect(Exception.class);
        thrown.expectMessage(Matchers.containsString("invalid metadata"));
        thrown.expectMessage(Matchers.containsString(
                "'" + MigrationContextImpl.METADATA_JAVA_PROPERTIES + "'"));

        CONTEXT.processMetadata(METADATA);
    }

    @Test
    public void testDoImportForSystemContext() throws Exception {
        // no migratable and no id
        CONTEXT.doImport();

        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
    }

    @Test
    public void testDoImportWhenNoMigratableInstalled() throws Exception {
        CONTEXT = new ImportMigrationContextImpl(REPORT, ZIP, MIGRATABLE_ID);

        CONTEXT.doImport();

        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(true));

        thrown.expect(ImportMigrationException.class);
        thrown.expectMessage(Matchers.containsString("not installed"));
        thrown.expectMessage(Matchers.containsString("[" + MIGRATABLE_ID + "]"));

        REPORT.verifyCompletion(); // trigger the exception
    }

    @Test
    public void testDoImportWhenVersionIsCompatible() throws Exception {
        final Map<String, Object> METADATA = ImmutableMap.of(MigrationContextImpl.METADATA_VERSION,
                VERSION);

        CONTEXT = new ImportMigrationContextImpl(REPORT, ZIP, MIGRATABLE);
        CONTEXT.processMetadata(METADATA); // make sure context has a version

        Mockito.when(MIGRATABLE.getVersion()).thenReturn(VERSION);
        Mockito.doNothing().when(MIGRATABLE).doImport(Mockito.any());

        CONTEXT.doImport();

        Mockito.verify(MIGRATABLE).doImport(CONTEXT);
        Mockito.verify(MIGRATABLE, Mockito.never()).doIncompatibleImport(CONTEXT, VERSION);
    }

    @Test
    public void testDoImportWhenVersionIsIncompatible() throws Exception {
        final Map<String, Object> METADATA = ImmutableMap.of(MigrationContextImpl.METADATA_VERSION,
                VERSION);

        CONTEXT = new ImportMigrationContextImpl(REPORT, ZIP, MIGRATABLE);
        CONTEXT.processMetadata(METADATA); // make sure context has a version

        Mockito.when(MIGRATABLE.getVersion()).thenReturn(VERSION + "2");
        Mockito.doNothing().when(MIGRATABLE).doImport(Mockito.any());

        CONTEXT.doImport();

        Mockito.verify(MIGRATABLE, Mockito.never()).doImport(CONTEXT);
        Mockito.verify(MIGRATABLE).doIncompatibleImport(CONTEXT, VERSION);
    }
}
