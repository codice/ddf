package org.codice.ddf.configuration.migration;

import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FilenameUtils;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

public class ExportMigrationJavaPropertyReferencedEntryImplTest extends AbstractMigrationTest {
    private static final String[] DIRS = new String[] {"path", "path2"};

    private static final String FILENAME = "file.ext";

    private static final String PROPERTIES_FILENAME = "file.properties";

    private static final String UNIX_NAME = "path/path2/" + FILENAME;

    private static final Path FILE_PATH = Paths.get(FilenameUtils.separatorsToSystem(UNIX_NAME));

    private static final String PROPERTY = "property";

    private static final String MIGRATABLE_ID = "test-migratable";

    private final ExportMigrationReportImpl REPORT = new ExportMigrationReportImpl();

    private final ExportMigrationContextImpl CONTEXT =
            Mockito.mock(ExportMigrationContextImpl.class);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private Path ABSOLUTE_FILE_PATH;

    private Path PROPERTIES_PATH;

    private PathUtils PATH_UTILS;

    private ExportMigrationJavaPropertyReferencedEntryImpl ENTRY;

    @Before
    public void before() throws Exception {
        final Path path = createFile(createDirectory(DIRS), FILENAME);

        PROPERTIES_PATH = createFile(path.getParent(), PROPERTIES_FILENAME);
        PATH_UTILS = new PathUtils();
        Mockito.when(CONTEXT.getPathUtils())
                .thenReturn(PATH_UTILS);
        Mockito.when(CONTEXT.getReport())
                .thenReturn(REPORT);
        Mockito.when(CONTEXT.getId())
                .thenReturn(MIGRATABLE_ID);
        ENTRY = new ExportMigrationJavaPropertyReferencedEntryImpl(CONTEXT,
                PROPERTIES_PATH,
                PROPERTY,
                UNIX_NAME);
        ABSOLUTE_FILE_PATH = DDF_HOME.resolve(UNIX_NAME)
                .toRealPath(LinkOption.NOFOLLOW_LINKS);
    }

    @Test
    public void testConstructor() throws Exception {
        Assert.assertThat(ENTRY.getContext(), Matchers.sameInstance(CONTEXT));
        Assert.assertThat(ENTRY.getPath(), Matchers.equalTo(FILE_PATH));
        Assert.assertThat(ENTRY.getAbsolutePath(), Matchers.equalTo(ABSOLUTE_FILE_PATH));
        Assert.assertThat(ENTRY.getFile(), Matchers.equalTo(ABSOLUTE_FILE_PATH.toFile()));
        Assert.assertThat(ENTRY.getName(), Matchers.equalTo(UNIX_NAME));
        Assert.assertThat(ENTRY.getProperty(), Matchers.equalTo(PROPERTY));
        Assert.assertThat(ENTRY.getPropertiesPath(), Matchers.equalTo(PROPERTIES_PATH));
    }

    @Test
    public void testConstructorWithNullContext() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("null context");

        new ExportMigrationJavaPropertyReferencedEntryImpl(null,
                PROPERTIES_PATH,
                PROPERTY,
                UNIX_NAME);
    }

    @Test
    public void testConstructorWithNullPropertyPath() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("null properties path");

        new ExportMigrationJavaPropertyReferencedEntryImpl(CONTEXT, null, PROPERTY, UNIX_NAME);
    }

    @Test
    public void testConstructorWithNullProperty() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("null property");

        new ExportMigrationJavaPropertyReferencedEntryImpl(CONTEXT,
                PROPERTIES_PATH,
                null,
                UNIX_NAME);
    }

    @Test
    public void testConstructorWithNullPathname() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("null pathname");

        new ExportMigrationJavaPropertyReferencedEntryImpl(CONTEXT,
                PROPERTIES_PATH,
                PROPERTY,
                null);
    }

    @Test
    public void testGetPropertiesPath() throws Exception {
        Assert.assertThat(ENTRY.getPropertiesPath(), Matchers.equalTo(PROPERTIES_PATH));
    }

    @Test
    public void testRecordEntry() throws Exception {
        final ExportMigrationReportImpl REPORT = Mockito.mock(ExportMigrationReportImpl.class);

        Mockito.when(CONTEXT.getReport())
                .thenReturn(REPORT);
        Mockito.when(REPORT.recordJavaProperty(Mockito.any()))
                .thenReturn(REPORT);

        ENTRY.recordEntry();

        Mockito.verify(REPORT)
                .recordJavaProperty(Mockito.same(ENTRY));
    }

    @Test
    public void testToDebugString() throws Exception {
        final String debug = ENTRY.toDebugString();

        Assert.assertThat(debug, Matchers.containsString("Java property"));
        Assert.assertThat(debug, Matchers.containsString("[" + PROPERTY + "]"));
        Assert.assertThat(debug, Matchers.containsString("[" + PROPERTIES_PATH + "]"));
        Assert.assertThat(debug, Matchers.containsString("[" + UNIX_NAME + "]"));
    }

    //@Test
    //public void testRecordWarning() throws Exception {
    //    Mockito.when(REPORT.record(Mockito.any(MigrationMessage.class)))
    //            .thenReturn(REPORT);

        //Mockito.verify(REPORT).record(Mockito.ins)
    //}
}
