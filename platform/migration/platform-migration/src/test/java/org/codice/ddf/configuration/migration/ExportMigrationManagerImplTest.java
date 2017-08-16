package org.codice.ddf.configuration.migration;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.codice.ddf.migration.ExportMigrationException;
import org.codice.ddf.migration.Migratable;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationOperation;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ExportMigrationManagerImplTest extends AbstractMigrationReportTest {
    private static final String MIGRATABLE_ID2 = "test-migratable-2";

    private static final String MIGRATABLE_ID3 = "test-migratable-3";

    private final Migratable MIGRATABLE2 = Mockito.mock(Migratable.class);

    private final Migratable MIGRATABLE3 = Mockito.mock(Migratable.class);

    private final Migratable[] MIGRATABLES =
            new Migratable[] {MIGRATABLE, MIGRATABLE2, MIGRATABLE3};

    private Path EXPORT_FILE;

    private ExportMigrationManagerImpl MGR;

    @Before
    public void before() throws Exception {
        EXPORT_FILE = DDF_HOME.resolve(createDirectory("exported"))
                .resolve("exported.zip");
        initMigratableMock();
        initMigratableMock(MIGRATABLE2, MIGRATABLE_ID2);
        initMigratableMock(MIGRATABLE3, MIGRATABLE_ID3);

        MGR = new ExportMigrationManagerImpl(REPORT, EXPORT_FILE, Stream.of(MIGRATABLES));
    }

    @Test
    public void testConstructor() throws Exception {
        Assert.assertThat(MGR.getReport(), Matchers.sameInstance(REPORT));
        Assert.assertThat(MGR.getExportFile(), Matchers.equalTo(EXPORT_FILE));
        Assert.assertThat(MGR.getContexts()
                        .stream()
                        .map(ExportMigrationContextImpl::getMigratable)
                        .toArray(Migratable[]::new),
                Matchers.arrayContaining(Matchers.sameInstance(MIGRATABLE),
                        Matchers.sameInstance(MIGRATABLE2),
                        Matchers.sameInstance(MIGRATABLE3)));
    }

    @Test
    public void testConstructorWithDuplicateMigratableIds() throws Exception {
        final Migratable MIGRATABLE2_2 = Mockito.mock(Migratable.class);

        initMigratableMock(MIGRATABLE2_2, MIGRATABLE_ID2);
        Assert.assertThat(MGR.getReport(), Matchers.sameInstance(REPORT));
        Assert.assertThat(MGR.getExportFile(), Matchers.equalTo(EXPORT_FILE));
        Assert.assertThat(MGR.getContexts()
                        .stream()
                        .map(ExportMigrationContextImpl::getMigratable)
                        .toArray(Migratable[]::new),
                Matchers.arrayContaining(Matchers.sameInstance(MIGRATABLE),
                        Matchers.sameInstance(MIGRATABLE2),
                        Matchers.sameInstance(MIGRATABLE3)));
    }

    @Test
    public void testConstructorWithNullReport() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null report"));

        new ExportMigrationManagerImpl(null, EXPORT_FILE, Stream.empty());
    }

    @Test
    public void testConstructorWithInvalidReport() throws Exception {
        final MigrationReportImpl REPORT = new MigrationReportImpl(MigrationOperation.IMPORT,
                Optional.empty());

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("invalid migration operation"));

        new ExportMigrationManagerImpl(REPORT, EXPORT_FILE, Stream.empty());
    }

    @Test
    public void testConstructorWithNullExportFile() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null export file"));

        new ExportMigrationManagerImpl(REPORT, null, Stream.empty());
    }

    @Test
    public void testConstructorWithNullMigratables() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null migratables"));

        new ExportMigrationManagerImpl(REPORT, EXPORT_FILE, null);
    }

    @Test
    public void testConstructorWhenUnableToCreateZipFile() throws Exception {
        FileUtils.deleteQuietly(EXPORT_FILE.getParent()
                .toFile());

        thrown.expect(ExportMigrationException.class);
        thrown.expectMessage(Matchers.containsString("unable to create"));
        thrown.expectCause(Matchers.instanceOf(FileNotFoundException.class));

        new ExportMigrationManagerImpl(REPORT, EXPORT_FILE, Stream.empty());
    }

    @Test
    public void testDoExport() throws Exception {
        MGR.doExport(VERSION);

        assertMetaData(MGR.getMetadata());
    }

    @Test
    public void testDoExportWhenOneMigratableAborts() throws Exception {
        final MigrationException ME = new MigrationException("testing");

        Mockito.doThrow(ME)
                .when(MIGRATABLE2)
                .doExport(Mockito.any());

        thrown.expect(Matchers.sameInstance(ME));

        MGR.doExport(VERSION);
    }

    @Test
    public void testDoExportWithNullProductVersion() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null product version"));

        MGR.doExport(null);
    }

    @Test
    public void testClose() throws Exception {
        MGR.doExport(VERSION);

        MGR.close();

        final Map<String, ZipEntry> entries = AbstractMigrationTest.getEntriesFrom(EXPORT_FILE);

        Assert.assertThat(entries, Matchers.aMapWithSize(1));
        Assert.assertThat(entries,
                Matchers.hasKey(MigrationContextImpl.METADATA_FILENAME.toString()));
        final Object ometadata =
                JsonUtils.MAPPER.fromJson(entries.get(MigrationContextImpl.METADATA_FILENAME.toString())
                        .getContent());

        Assert.assertThat(ometadata, Matchers.instanceOf(Map.class));
        assertMetaData((Map<String, Object>) ometadata);
    }

    @Test
    public void testCloseWhenAlreadyClosed() throws Exception {
        final ZipOutputStream ZOS = Mockito.mock(ZipOutputStream.class);
        final ExportMigrationManagerImpl MGR = new ExportMigrationManagerImpl(REPORT, EXPORT_FILE, Stream.of(MIGRATABLES), ZOS);

        Mockito.doNothing().when(ZOS).closeEntry();

        MGR.close();

        MGR.close();

        Mockito.verify(ZOS).closeEntry();
    }

    @Test
    public void testCloseWhileFailingToCreateMetadataEntry() throws Exception {
        final ZipOutputStream ZOS = Mockito.mock(ZipOutputStream.class);
        final ExportMigrationManagerImpl MGR = new ExportMigrationManagerImpl(REPORT, EXPORT_FILE, Stream.of(MIGRATABLES), ZOS);
        final IOException IOE = new IOException("testing");

        Mockito.doNothing().when(ZOS).closeEntry();
        Mockito.doThrow(IOE).when(ZOS).putNextEntry(Mockito.any());

        thrown.expect(ExportMigrationException.class);
        thrown.expectMessage(Matchers.containsString("unable to create metadata"));
        thrown.expectCause(Matchers.sameInstance(IOE));

        MGR.close();
    }

    @Test
    public void testCloseWhileFailingToCloseLastEntry() throws Exception {
        final ZipOutputStream ZOS = Mockito.mock(ZipOutputStream.class);
        final ExportMigrationManagerImpl MGR = new ExportMigrationManagerImpl(REPORT, EXPORT_FILE, Stream.of(MIGRATABLES), ZOS);
        final IOException IOE = new IOException("testing");

        Mockito.doThrow(IOE).when(ZOS).closeEntry();

        thrown.expect(Matchers.sameInstance(IOE));

        MGR.close();
    }

    private void assertMetaData(Map<String, Object> metadata) {
        Assert.assertThat(metadata, Matchers.aMapWithSize(4));
        Assert.assertThat(metadata,
                Matchers.hasEntry(MigrationContextImpl.METADATA_VERSION,
                        MigrationContextImpl.VERSION));
        Assert.assertThat(metadata,
                Matchers.hasEntry(MigrationContextImpl.METADATA_PRODUCT_VERSION, VERSION));
        Assert.assertThat(metadata, Matchers.hasKey(MigrationContextImpl.METADATA_DATE));
        Assert.assertThat(metadata,
                Matchers.hasEntry(Matchers.equalTo(MigrationContextImpl.METADATA_MIGRATABLES),
                        Matchers.instanceOf(Map.class)));
        final Map<String, Object> mmetadatas = (Map<String, Object>) metadata.get(
                MigrationContextImpl.METADATA_MIGRATABLES);

        Assert.assertThat(mmetadatas, Matchers.aMapWithSize(MIGRATABLES.length));
        Stream.of(MIGRATABLES)
                .forEach(m -> {
                    Assert.assertThat(mmetadatas,
                            Matchers.hasEntry(Matchers.equalTo(m.getId()),
                                    Matchers.instanceOf(Map.class)));
                    final Map<String, Object> mmetadata =
                            (Map<String, Object>) mmetadatas.get(m.getId());

                    Assert.assertThat(mmetadata, Matchers.aMapWithSize(4));
                    Assert.assertThat(mmetadata,
                            Matchers.hasEntry(MigrationContextImpl.METADATA_VERSION, VERSION));
                    Assert.assertThat(mmetadata,
                            Matchers.hasEntry(MigrationContextImpl.METADATA_TITLE, TITLE));
                    Assert.assertThat(mmetadata,
                            Matchers.hasEntry(MigrationContextImpl.METADATA_DESCRIPTION,
                                    DESCRIPTION));
                    Assert.assertThat(mmetadata,
                            Matchers.hasEntry(MigrationContextImpl.METADATA_ORGANIZATION,
                                    ORGANIZATION));
                });
    }
}
