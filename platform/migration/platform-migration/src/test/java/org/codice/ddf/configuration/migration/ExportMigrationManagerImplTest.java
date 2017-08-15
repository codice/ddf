package org.codice.ddf.configuration.migration;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.codice.ddf.migration.ExportMigrationException;
import org.codice.ddf.migration.Migratable;
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

    private Path EXPORT_FILE;

    private ExportMigrationManagerImpl MGR;

    @Before
    public void before() throws Exception {
        EXPORT_FILE = DDF_HOME.resolve(createDirectory("exported"))
                .resolve("exported.zip");
        initMigratableMock();
        initMigratableMock(MIGRATABLE2, MIGRATABLE_ID2);
        initMigratableMock(MIGRATABLE3, MIGRATABLE_ID3);

        MGR = new ExportMigrationManagerImpl(REPORT,
                EXPORT_FILE,
                Stream.of(MIGRATABLE, MIGRATABLE2, MIGRATABLE3));
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
        FileUtils.deleteQuietly(EXPORT_FILE.getParent().toFile());

        thrown.expect(ExportMigrationException.class);
        thrown.expectMessage(Matchers.containsString("unable to create"));
        thrown.expectCause(Matchers.instanceOf(FileNotFoundException.class));

        new ExportMigrationManagerImpl(REPORT, EXPORT_FILE, Stream.empty());
    }

    @Test
    public void testDoExport() throws Exception {
        MGR.doExport(VERSION);
    }
}


