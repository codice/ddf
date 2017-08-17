package org.codice.ddf.configuration.migration;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

import org.codice.ddf.migration.ImportMigrationException;
import org.codice.ddf.migration.Migratable;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationOperation;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.common.base.Charsets;

public class ImportMigrationManagerImplTest extends AbstractMigrationReportTest {
    private static final String MIGRATABLE_ID2 = "test-migratable-2";

    private static final String MIGRATABLE_ID3 = "test-migratable-3";

    private final Migratable MIGRATABLE2 = Mockito.mock(Migratable.class);

    private final Migratable MIGRATABLE3 = Mockito.mock(Migratable.class);

    private final Migratable[] MIGRATABLES =
            new Migratable[] {MIGRATABLE, MIGRATABLE2, MIGRATABLE3};

    private Path EXPORT_FILE;

    private ZipFile ZIP;

    private ZipEntry ZE;

    private ImportMigrationManagerImpl MGR;

    public ImportMigrationManagerImplTest() {
        super(MigrationOperation.IMPORT);
    }

    @Before
    public void before() throws Exception {
        EXPORT_FILE = DDF_HOME.resolve(createDirectory("exported"))
                .resolve("exported.zip");
        initMigratableMock();
        initMigratableMock(MIGRATABLE2, MIGRATABLE_ID2);
        initMigratableMock(MIGRATABLE3, MIGRATABLE_ID3);

        ZIP = Mockito.mock(ZipFile.class);
        ZE = getMetadataZipEntry(ZIP,
                Optional.of(MigrationContextImpl.VERSION),
                Optional.of(PRODUCT_VERSION));
        // use answer to ensure we create a new stream each time if called multiple times
        Mockito.doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return Stream.of(ZE);
            }
        })
                .when(ZIP)
                .stream();

        MGR = new ImportMigrationManagerImpl(REPORT, EXPORT_FILE, Stream.of(MIGRATABLES), ZIP);
    }

    @Test
    public void testConstructor() throws Exception {
        Assert.assertThat(MGR.getReport(), Matchers.sameInstance(REPORT));
        Assert.assertThat(MGR.getExportFile(), Matchers.sameInstance(EXPORT_FILE));
        Assert.assertThat(MGR.getContexts()
                        .stream()
                        .map(ImportMigrationContextImpl::getMigratable)
                        .toArray(Migratable[]::new),
                Matchers.arrayContaining(Matchers.sameInstance(MIGRATABLE),
                        Matchers.sameInstance(MIGRATABLE2),
                        Matchers.sameInstance(MIGRATABLE3),
                        Matchers.nullValue())); // null correspond to the system context
    }

    @Test
    public void testConstructorWithAdditionalMigratable() throws Exception {
        final Migratable MIGRATABLE4 = Mockito.mock(Migratable.class);

        initMigratableMock(MIGRATABLE4, "test-migratable-4");

        MGR = new ImportMigrationManagerImpl(REPORT,
                EXPORT_FILE,
                Stream.concat(Stream.of(MIGRATABLES), Stream.of(MIGRATABLE4)),
                ZIP);

        Assert.assertThat(MGR.getReport(), Matchers.sameInstance(REPORT));
        Assert.assertThat(MGR.getExportFile(), Matchers.sameInstance(EXPORT_FILE));
        Assert.assertThat(MGR.getContexts()
                        .stream()
                        .map(ImportMigrationContextImpl::getMigratable)
                        .toArray(Migratable[]::new),
                Matchers.arrayContaining(Matchers.sameInstance(MIGRATABLE),
                        Matchers.sameInstance(MIGRATABLE2),
                        Matchers.sameInstance(MIGRATABLE3),
                        Matchers.sameInstance(MIGRATABLE4),
                        Matchers.nullValue())); // null correspond to the system context
    }

    @Test
    public void testConstructorWithLessMigratables() throws Exception {
        MGR = new ImportMigrationManagerImpl(REPORT,
                EXPORT_FILE,
                Stream.of(MIGRATABLE, MIGRATABLE3),
                ZIP);

        Assert.assertThat(MGR.getReport(), Matchers.sameInstance(REPORT));
        Assert.assertThat(MGR.getExportFile(), Matchers.sameInstance(EXPORT_FILE));
        Assert.assertThat(MGR.getContexts()
                        .stream()
                        .map(ImportMigrationContextImpl::getMigratable)
                        .toArray(Migratable[]::new),
                Matchers.arrayContaining(Matchers.sameInstance(MIGRATABLE),
                        Matchers.sameInstance(MIGRATABLE3),
                        Matchers.nullValue(), // null correspond to the system context
                        Matchers.nullValue())); // null for migratable2
        Assert.assertThat(MGR.getContexts()
                        .stream()
                        .map(ImportMigrationContextImpl::getId)
                        .toArray(String[]::new),
                Matchers.arrayContaining(Matchers.equalTo(MIGRATABLE_ID),
                        Matchers.equalTo(MIGRATABLE_ID3),
                        Matchers.nullValue(), // null correspond to the system context
                        Matchers.equalTo(MIGRATABLE_ID2)));
    }

    @Test
    public void testConstructorWithDuplicateMigratableIds() throws Exception {
        final Migratable MIGRATABLE2_2 = Mockito.mock(Migratable.class);

        initMigratableMock(MIGRATABLE2_2, MIGRATABLE_ID2);
        Assert.assertThat(MGR.getReport(), Matchers.sameInstance(REPORT));
        Assert.assertThat(MGR.getExportFile(), Matchers.sameInstance(EXPORT_FILE));
        Assert.assertThat(MGR.getContexts()
                        .stream()
                        .map(ImportMigrationContextImpl::getMigratable)
                        .toArray(Migratable[]::new),
                Matchers.arrayContaining(Matchers.sameInstance(MIGRATABLE),
                        Matchers.sameInstance(MIGRATABLE2),
                        Matchers.sameInstance(MIGRATABLE3),
                        Matchers.nullValue())); // null correspond to the system context
    }

    @Test
    public void testConstructorWithNullReport() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null report"));

        new ImportMigrationManagerImpl(null, EXPORT_FILE, Stream.empty(), ZIP);
    }

    @Test
    public void testConstructorWithInvalidReport() throws Exception {
        final MigrationReportImpl REPORT = new MigrationReportImpl(MigrationOperation.EXPORT,
                Optional.empty());

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("invalid migration operation"));

        new ImportMigrationManagerImpl(REPORT, EXPORT_FILE, Stream.empty(), ZIP);
    }

    @Test
    public void testConstructorWithNullExportFile() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null export file"));

        new ImportMigrationManagerImpl(REPORT, null, Stream.empty());
    }

    @Test
    public void testConstructorWithNullMigratables() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null migratables"));

        new ImportMigrationManagerImpl(REPORT, EXPORT_FILE, null, ZIP);
    }

    @Test
    public void testConstructorWhenZipFileNotFound() throws Exception {
        thrown.expect(ImportMigrationException.class);
        thrown.expectCause(Matchers.instanceOf(FileNotFoundException.class));

        new ImportMigrationManagerImpl(REPORT, EXPORT_FILE, Stream.empty());
    }

    @Test
    public void testConstructorWhenUnableToProcessMetadata() throws Exception {
        final IOException IOE = new IOException("testing");

        Mockito.doThrow(IOE)
                .when(ZIP)
                .getInputStream(ZE);

        thrown.expect(ImportMigrationException.class);
        thrown.expectCause(Matchers.sameInstance(IOE));

        new ImportMigrationManagerImpl(REPORT, EXPORT_FILE, Stream.empty(), ZIP);
    }

    @Test
    public void testConstructorWhenZipIsOfInvalidVersion() throws Exception {
        ZE = getMetadataZipEntry(ZIP, Optional.of(VERSION), Optional.of(PRODUCT_VERSION));

        thrown.expect(ImportMigrationException.class);
        thrown.expectMessage("unsupported exported migrated version");

        new ImportMigrationManagerImpl(REPORT, EXPORT_FILE, Stream.empty(), ZIP);
    }

    @Test
    public void testDoImport() throws Exception {
        MGR.doImport(PRODUCT_VERSION);

        Mockito.verify(MIGRATABLE)
                .doImport(Mockito.notNull());
        Mockito.verify(MIGRATABLE2)
                .doImport(Mockito.notNull());
        Mockito.verify(MIGRATABLE3)
                .doImport(Mockito.notNull());
    }

    @Test
    public void testDoImportWhenOneMigratableAborts() throws Exception {
        final MigrationException ME = new MigrationException("testing");

        Mockito.doThrow(ME)
                .when(MIGRATABLE2)
                .doImport(Mockito.any());

        thrown.expect(Matchers.sameInstance(ME));

        MGR.doImport(PRODUCT_VERSION);

        Mockito.verify(MIGRATABLE)
                .doImport(Mockito.notNull());
        Mockito.verify(MIGRATABLE2)
                .doImport(Mockito.notNull());
        Mockito.verify(MIGRATABLE3, Mockito.never())
                .doImport(Mockito.notNull());
    }

    @Test
    public void testDoImportWithNullProductVersion() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null product version"));

        MGR.doImport(null);
    }

    @Test
    public void testDoImportWithInvalidProductVersion() throws Exception {
        thrown.expect(ImportMigrationException.class);
        thrown.expectMessage(Matchers.containsString("mismatched exported product version"));

        MGR.doImport(PRODUCT_VERSION + "2");
    }

    @Test
    public void testClose() throws Exception {
        MGR.close();

        Mockito.verify(ZIP)
                .close();
    }

    private ZipEntry getMetadataZipEntry(ZipFile zip, Optional<String> version,
            Optional<String> productVersion) throws IOException {
        final StringBuilder sb = new StringBuilder();

        sb.append("{\"dummy\":\"dummy");
        version.ifPresent(v -> sb.append("\",\"")
                .append(MigrationContextImpl.METADATA_VERSION)
                .append("\":\"")
                .append(v));
        productVersion.ifPresent(v -> sb.append("\",\"")
                .append(MigrationContextImpl.METADATA_PRODUCT_VERSION)
                .append("\":\"")
                .append(v));
        sb.append("\",\"")
                .append(MigrationContextImpl.METADATA_MIGRATABLES)
                .append("\":{");
        boolean first = true;

        for (final Migratable m : MIGRATABLES) {
            if (first) {
                first = false;
            } else {
                sb.append(",");
            }
            sb.append("\"")
                    .append(m.getId())
                    .append("\":{\"")
                    .append(MigrationContextImpl.METADATA_VERSION)
                    .append("\":\"")
                    .append(m.getVersion())
                    .append("\",\"")
                    .append(MigrationContextImpl.METADATA_TITLE)
                    .append("\":\"")
                    .append(m.getTitle())
                    .append("\",\"")
                    .append(MigrationContextImpl.METADATA_DESCRIPTION)
                    .append("\":\"")
                    .append(m.getDescription())
                    .append("\",\"")
                    .append(MigrationContextImpl.METADATA_ORGANIZATION)
                    .append("\":\"")
                    .append(m.getOrganization())
                    .append("\"}");
        }
        sb.append("\"}");
        final ZipEntry ze = Mockito.mock(ZipEntry.class);

        Mockito.when(ze.getName())
                .thenReturn(MigrationContextImpl.METADATA_FILENAME.toString());
        // use answer to ensure we create a new stream each time if called multiple times
        Mockito.doAnswer(AdditionalAnswers.answer(zea -> new ByteArrayInputStream(sb.toString()
                .getBytes(Charsets.UTF_8))))
                .when(zip)
                .getInputStream(ze);
        return ze;
    }
}
