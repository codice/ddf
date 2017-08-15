package org.codice.ddf.configuration.migration;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.commons.io.FilenameUtils;
import org.codice.ddf.migration.MigrationInformation;
import org.codice.ddf.migration.MigrationReport;
import org.codice.ddf.util.function.ERunnable;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

public class ExportMigrationReportImplTest extends AbstractMigrationTest {
    private static final String[] DIRS = new String[] {"path", "path2"};

    private static final String FILENAME = "file.ext";

    private static final String UNIX_NAME = "path/path2/" + FILENAME;

    private static final Path FILE_PATH = Paths.get(FilenameUtils.separatorsToSystem(UNIX_NAME));

    private static final String PROPERTY = "property";

    private final MigrationReportImpl REPORT = Mockito.mock(MigrationReportImpl.class);

    private final ExportMigrationContextImpl CONTEXT =
            Mockito.mock(ExportMigrationContextImpl.class);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private ExportMigrationReportImpl XREPORT;

    @Before
    public void before() throws Exception {
        initMigratableMock();
        XREPORT = new ExportMigrationReportImpl(REPORT, MIGRATABLE);
    }

    @Test
    public void testConstructor() throws Exception {
        final Map<String, Object> metadata = XREPORT.getMetadata();

        Assert.assertThat(XREPORT.getReport(), Matchers.sameInstance(REPORT));
        Assert.assertThat(metadata, Matchers.aMapWithSize(4));
        Assert.assertThat(metadata, Matchers.hasEntry("version", MIGRATABLE.getVersion()));
        Assert.assertThat(metadata, Matchers.hasEntry("title", MIGRATABLE.getTitle()));
        Assert.assertThat(metadata, Matchers.hasEntry("description", MIGRATABLE.getDescription()));
        Assert.assertThat(metadata,
                Matchers.hasEntry("organization", MIGRATABLE.getOrganization()));
    }

    @Test
    public void testConstructorWithNullReport() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null report"));

        new ExportMigrationReportImpl(null, MIGRATABLE);
    }

    @Test
    public void testConstructorWithNullMigratable() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null migratable"));

        new ExportMigrationReportImpl(REPORT, null);
    }

    @Test
    public void testGetOperation() throws Exception {
        XREPORT.getOperation();

        Mockito.verify(REPORT)
                .getOperation();
    }

    @Test
    public void testGetStartTime() throws Exception {
        XREPORT.getStartTime();

        Mockito.verify(REPORT)
                .getStartTime();
    }

    @Test
    public void testEndTime() throws Exception {
        XREPORT.getEndTime();

        Mockito.verify(REPORT)
                .getEndTime();
    }

    @Test
    public void testRecordWithInfoString() throws Exception {
        final String INFO = "info";

        Assert.assertThat(XREPORT.record(INFO), Matchers.sameInstance(XREPORT));

        Mockito.verify(REPORT)
                .record(Mockito.same(INFO));
    }

    @Test
    public void testRecord() throws Exception {
        final MigrationInformation INFO = new MigrationInformation("info");

        Assert.assertThat(XREPORT.record(INFO), Matchers.sameInstance(XREPORT));

        Mockito.verify(REPORT)
                .record(Mockito.same(INFO));
    }

    @Test
    public void testDoAfterCompletion() throws Exception {
        final Consumer<MigrationReport> CODE = Mockito.mock(Consumer.class);

        Assert.assertThat(XREPORT.doAfterCompletion(CODE), Matchers.sameInstance(XREPORT));

        Mockito.verify(REPORT)
                .doAfterCompletion(Mockito.same(CODE));
    }

    @Test
    public void testWasSucessful() throws Exception {
        XREPORT.wasSuccessful();

        Mockito.verify(REPORT)
                .wasSuccessful();
    }

    @Test
    public void testWasSucessfulWithCode() throws Exception {
        final Runnable CODE = Mockito.mock(Runnable.class);

        XREPORT.wasSuccessful(CODE);

        Mockito.verify(REPORT)
                .wasSuccessful(Mockito.same(CODE));
    }

    @Test
    public void testWasIOSucessfulWithCode() throws Exception {
        final ERunnable<IOException> CODE = Mockito.mock(ERunnable.class);

        XREPORT.wasIOSuccessful(CODE);

        Mockito.verify(REPORT)
                .wasIOSuccessful(Mockito.same(CODE));
    }

    @Test
    public void testHasInfos() throws Exception {
        XREPORT.hasInfos();

        Mockito.verify(REPORT)
                .hasInfos();
    }

    @Test
    public void testHasWarnings() throws Exception {
        XREPORT.hasWarnings();

        Mockito.verify(REPORT)
                .hasWarnings();
    }

    @Test
    public void testHasErrors() throws Exception {
        XREPORT.hasErrors();

        Mockito.verify(REPORT)
                .hasErrors();
    }

    @Test
    public void testVerifyCompletion() throws Exception {
        XREPORT.verifyCompletion();

        Mockito.verify(REPORT)
                .verifyCompletion();
    }

    @Test
    public void testGetReport() throws Exception {
        Assert.assertThat(XREPORT.getReport(), Matchers.sameInstance(REPORT));
    }

    private void initContext() {
        Mockito.when(CONTEXT.getPathUtils())
                .thenReturn(new PathUtils());
        Mockito.when(CONTEXT.getReport())
                .thenReturn(XREPORT);
        Mockito.when(CONTEXT.getId())
                .thenReturn(MIGRATABLE_ID);
    }

    @Test
    public void testRecordExternalFile() throws Exception {
        createFile(createDirectory(DIRS), FILENAME);
        initContext();
        final ExportMigrationEntryImpl ENTRY = new ExportMigrationEntryImpl(CONTEXT, FILE_PATH);

        Assert.assertThat(XREPORT.recordExternal(ENTRY, false), Matchers.sameInstance(XREPORT));

        final Map<String, Object> metadata = XREPORT.getMetadata();

        Assert.assertThat(metadata,
                Matchers.hasEntry(Matchers.equalTo("externals"), Matchers.instanceOf(List.class)));
        final List<Object> xmetadata = (List<Object>) metadata.get("externals");

        Assert.assertThat(xmetadata,
                Matchers.allOf(Matchers.iterableWithSize(1),
                        Matchers.contains(Matchers.instanceOf(Map.class))));
        final Map<String, Object> emetadata = (Map<String, Object>) xmetadata.get(0);

        Assert.assertThat(emetadata,
                Matchers.allOf(Matchers.aMapWithSize(3),
                        Matchers.hasEntry("name", ENTRY.getName()),
                        Matchers.hasKey("checksum"),
                        Matchers.hasEntry("softlink", (Object) false)));
    }

    @Test
    public void testRecordExternalFileWhenUnableToComputeChecksum() throws Exception {
        createFile(createDirectory(DIRS), FILENAME);
        initContext();
        final ExportMigrationEntryImpl ENTRY = new ExportMigrationEntryImpl(CONTEXT, FILE_PATH);

        ENTRY.getFile()
                .delete(); // will ensure the checksum cannot be computed

        Assert.assertThat(XREPORT.recordExternal(ENTRY, false), Matchers.sameInstance(XREPORT));
        ;

        final Map<String, Object> metadata = XREPORT.getMetadata();

        Assert.assertThat(metadata,
                Matchers.hasEntry(Matchers.equalTo("externals"), Matchers.instanceOf(List.class)));
        final List<Object> xmetadata = (List<Object>) metadata.get("externals");

        Assert.assertThat(xmetadata,
                Matchers.allOf(Matchers.iterableWithSize(1),
                        Matchers.contains(Matchers.instanceOf(Map.class))));
        final Map<String, Object> emetadata = (Map<String, Object>) xmetadata.get(0);

        Assert.assertThat(emetadata,
                Matchers.allOf(Matchers.aMapWithSize(2),
                        Matchers.hasEntry("name", ENTRY.getName()),
                        Matchers.hasEntry("softlink", (Object) false)));
    }

    @Test
    public void testRecordExternalSoftlink() throws Exception {
        final Path ABSOLUTE_FILE_PATH = DDF_HOME.resolve(createFile(createDirectory(DIRS),
                FILENAME))
                .toAbsolutePath();
        final String FILENAME2 = "file2.ext";
        final Path ABSOLUTE_FILE_PATH2 = createSoftLink(ABSOLUTE_FILE_PATH.getParent(),
                FILENAME2,
                ABSOLUTE_FILE_PATH);

        initContext();
        final ExportMigrationEntryImpl ENTRY = new ExportMigrationEntryImpl(CONTEXT,
                ABSOLUTE_FILE_PATH2);

        Assert.assertThat(XREPORT.recordExternal(ENTRY, true), Matchers.sameInstance(XREPORT));

        final Map<String, Object> metadata = XREPORT.getMetadata();

        Assert.assertThat(metadata,
                Matchers.hasEntry(Matchers.equalTo("externals"), Matchers.instanceOf(List.class)));
        final List<Object> xmetadata = (List<Object>) metadata.get("externals");

        Assert.assertThat(xmetadata,
                Matchers.allOf(Matchers.iterableWithSize(1),
                        Matchers.contains(Matchers.instanceOf(Map.class))));
        final Map<String, Object> emetadata = (Map<String, Object>) xmetadata.get(0);

        Assert.assertThat(emetadata,
                Matchers.allOf(Matchers.aMapWithSize(3),
                        Matchers.hasEntry("name", ENTRY.getName()),
                        Matchers.hasKey("checksum"),
                        Matchers.hasEntry("softlink", (Object) true)));
    }

    @Test
    public void testRecordSystemProperty() throws Exception {
        initContext();
        final ExportMigrationSystemPropertyReferencedEntryImpl ENTRY =
                new ExportMigrationSystemPropertyReferencedEntryImpl(CONTEXT,
                        PROPERTY,
                        FILE_PATH.toString());

        Assert.assertThat(XREPORT.recordSystemProperty(ENTRY), Matchers.sameInstance(XREPORT));

        final Map<String, Object> metadata = XREPORT.getMetadata();

        Assert.assertThat(metadata,
                Matchers.hasEntry(Matchers.equalTo("system.properties"),
                        Matchers.instanceOf(List.class)));
        final List<Object> smetadata = (List<Object>) metadata.get("system.properties");

        Assert.assertThat(smetadata,
                Matchers.allOf(Matchers.iterableWithSize(1),
                        Matchers.contains(Matchers.instanceOf(Map.class))));
        final Map<String, Object> emetadata = (Map<String, Object>) smetadata.get(0);

        Assert.assertThat(emetadata,
                Matchers.allOf(Matchers.aMapWithSize(2),
                        Matchers.hasEntry("property", PROPERTY),
                        Matchers.hasEntry("reference", FILE_PATH.toString())));
    }

    @Test
    public void testRecordJavaProperty() throws Exception {
        initContext();
        final Path PROPERTIES_PATH = DDF_HOME.resolve("file.properties");
        final ExportMigrationJavaPropertyReferencedEntryImpl ENTRY =
                new ExportMigrationJavaPropertyReferencedEntryImpl(CONTEXT,
                        PROPERTIES_PATH,
                        PROPERTY,
                        FILE_PATH.toString());

        Assert.assertThat(XREPORT.recordJavaProperty(ENTRY), Matchers.sameInstance(XREPORT));

        final Map<String, Object> metadata = XREPORT.getMetadata();

        Assert.assertThat(metadata,
                Matchers.hasEntry(Matchers.equalTo("java.properties"),
                        Matchers.instanceOf(List.class)));
        final List<Object> jmetadata = (List<Object>) metadata.get("java.properties");

        Assert.assertThat(jmetadata,
                Matchers.allOf(Matchers.iterableWithSize(1),
                        Matchers.contains(Matchers.instanceOf(Map.class))));
        final Map<String, Object> emetadata = (Map<String, Object>) jmetadata.get(0);

        Assert.assertThat(emetadata,
                Matchers.allOf(Matchers.aMapWithSize(3),
                        Matchers.hasEntry("property", PROPERTY),
                        Matchers.hasEntry("reference", FILE_PATH.toString()),
                        Matchers.hasEntry("name", PROPERTIES_PATH.toString())));
    }

    @Test
    public void testGetMetadataWhenNothingRegistered() throws Exception {
        final Map<String, Object> metadata = XREPORT.getMetadata();

        Assert.assertThat(metadata, Matchers.aMapWithSize(4));
        Assert.assertThat(metadata, Matchers.hasEntry("version", MIGRATABLE.getVersion()));
        Assert.assertThat(metadata, Matchers.hasEntry("title", MIGRATABLE.getTitle()));
        Assert.assertThat(metadata, Matchers.hasEntry("description", MIGRATABLE.getDescription()));
        Assert.assertThat(metadata,
                Matchers.hasEntry("organization", MIGRATABLE.getOrganization()));
    }
}
