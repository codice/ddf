package org.codice.ddf.configuration.migration;

import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FilenameUtils;
import org.codice.ddf.migration.ExportPathMigrationException;
import org.codice.ddf.migration.ExportPathMigrationWarning;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ExportMigrationSystemPropertyReferencedEntryImplTest extends AbstractMigrationTest {
    private static final String[] DIRS = new String[] {"path", "path2"};

    private static final String FILENAME = "file.ext";

    private static final String UNIX_NAME = "path/path2/" + FILENAME;

    private static final Path FILE_PATH = Paths.get(FilenameUtils.separatorsToSystem(UNIX_NAME));

    private static final String PROPERTY = "property";

    private static final String MIGRATABLE_ID = "test-migratable";

    private final ExportMigrationReportImpl REPORT = new ExportMigrationReportImpl();

    private final ExportMigrationContextImpl CONTEXT =
            Mockito.mock(ExportMigrationContextImpl.class);

    private Path ABSOLUTE_FILE_PATH;

    private PathUtils PATH_UTILS;

    private ExportMigrationSystemPropertyReferencedEntryImpl ENTRY;

    @Before
    public void before() throws Exception {
        createFile(createDirectory(DIRS), FILENAME);
        PATH_UTILS = new PathUtils();
        Mockito.when(CONTEXT.getPathUtils())
                .thenReturn(PATH_UTILS);
        Mockito.when(CONTEXT.getReport())
                .thenReturn(REPORT);
        Mockito.when(CONTEXT.getId())
                .thenReturn(MIGRATABLE_ID);
        ENTRY = new ExportMigrationSystemPropertyReferencedEntryImpl(CONTEXT, PROPERTY, UNIX_NAME);
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
    }

    @Test
    public void testConstructorWithNullContext() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("null context");

        new ExportMigrationSystemPropertyReferencedEntryImpl(null, PROPERTY, UNIX_NAME);
    }

    @Test
    public void testConstructorWithNullProperty() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("null property");

        new ExportMigrationSystemPropertyReferencedEntryImpl(CONTEXT, null, UNIX_NAME);
    }

    @Test
    public void testConstructorWithNullPathname() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("null pathname");

        new ExportMigrationSystemPropertyReferencedEntryImpl(CONTEXT, PROPERTY, null);
    }

    @Test
    public void testRecordEntry() throws Exception {
        final ExportMigrationReportImpl REPORT = Mockito.mock(ExportMigrationReportImpl.class);

        Mockito.when(CONTEXT.getReport())
                .thenReturn(REPORT);
        Mockito.when(REPORT.recordSystemProperty(Mockito.any()))
                .thenReturn(REPORT);

        ENTRY.recordEntry();

        Mockito.verify(REPORT)
                .recordSystemProperty(Mockito.same(ENTRY));
    }

    @Test
    public void testToDebugString() throws Exception {
        final String debug = ENTRY.toDebugString();

        Assert.assertThat(debug, Matchers.containsString("system property"));
        Assert.assertThat(debug, Matchers.containsString("[" + PROPERTY + "]"));
        Assert.assertThat(debug, Matchers.containsString("[" + UNIX_NAME + "]"));
    }

    @Test
    public void testNewWarning() throws Exception {
        final String REASON = "test reason";
        final ExportPathMigrationWarning warning = ENTRY.newWarning(REASON);

        Assert.assertThat(warning.getMessage(), Matchers.containsString("[" + PROPERTY + "]"));
        Assert.assertThat(warning.getMessage(), Matchers.containsString("[" + UNIX_NAME + "]"));
        Assert.assertThat(warning.getMessage(), Matchers.containsString(REASON));
    }

    @Test
    public void testNewError() throws Exception {
        final String REASON = "test reason";
        final IllegalArgumentException CAUSE = new IllegalArgumentException("test cause");
        final ExportPathMigrationException error = ENTRY.newError(REASON, CAUSE);

        Assert.assertThat(error.getMessage(), Matchers.containsString("[" + PROPERTY + "]"));
        Assert.assertThat(error.getMessage(), Matchers.containsString("[" + UNIX_NAME + "]"));
        Assert.assertThat(error.getMessage(), Matchers.containsString(REASON));
        Assert.assertThat(error.getCause(), Matchers.sameInstance(CAUSE));
    }

    @Test
    public void testEqualsWhenEquals() throws Exception {
        final ExportMigrationSystemPropertyReferencedEntryImpl ENTRY2 =
                new ExportMigrationSystemPropertyReferencedEntryImpl(CONTEXT, PROPERTY, UNIX_NAME);

        Assert.assertThat(ENTRY.equals(ENTRY2), Matchers.equalTo(true));
    }

    @Test
    public void testEqualsWhenIdentical() throws Exception {
        Assert.assertThat(ENTRY.equals(ENTRY), Matchers.equalTo(true));
    }

    @Test
    public void testEqualsWhenNull() throws Exception {
        Assert.assertThat(ENTRY.equals(null), Matchers.equalTo(false));
    }

    @Test
    public void testEqualsWhenPropertiesAreDifferent() throws Exception {
        final ExportMigrationSystemPropertyReferencedEntryImpl ENTRY2 =
                new ExportMigrationSystemPropertyReferencedEntryImpl(CONTEXT, PROPERTY + '2', UNIX_NAME);

        Assert.assertThat(ENTRY.equals(ENTRY2), Matchers.equalTo(false));
    }

    @Test
    public void testHashCodeWhenEquals() throws Exception {
        final ExportMigrationSystemPropertyReferencedEntryImpl ENTRY2 =
                new ExportMigrationSystemPropertyReferencedEntryImpl(CONTEXT, PROPERTY, UNIX_NAME);

        Assert.assertThat(ENTRY.hashCode(), Matchers.equalTo(ENTRY2.hashCode()));
    }

    @Test
    public void testHashCodeWhenDifferent() throws Exception {
        final ExportMigrationSystemPropertyReferencedEntryImpl ENTRY2 =
                new ExportMigrationSystemPropertyReferencedEntryImpl(CONTEXT, PROPERTY + '2', UNIX_NAME);

        Assert.assertThat(ENTRY.hashCode(), Matchers.not(Matchers.equalTo(ENTRY2.hashCode())));
    }
}
