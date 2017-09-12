package org.codice.ddf.configuration.migration;

import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FilenameUtils;
import org.codice.ddf.test.util.ThrowableMatchers;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mockito;

public class ExportMigrationPropertyReferencedEntryImplTest extends AbstractMigrationTest {
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

    private ExportMigrationPropertyReferencedEntryImpl ENTRY;

    @Before
    public void before() throws Exception {
        createFile(createDirectory(DIRS), FILENAME);
        ABSOLUTE_FILE_PATH = DDF_HOME.resolve(UNIX_NAME)
                .toRealPath(LinkOption.NOFOLLOW_LINKS);

        Mockito.when(CONTEXT.getPathUtils())
                .thenReturn(new PathUtils());
        Mockito.when(CONTEXT.getReport())
                .thenReturn(REPORT);
        Mockito.when(CONTEXT.getId())
                .thenReturn(MIGRATABLE_ID);

        ENTRY = Mockito.mock(ExportMigrationPropertyReferencedEntryImpl.class,
                Mockito.withSettings()
                        .useConstructor(CONTEXT, PROPERTY, UNIX_NAME)
                        .defaultAnswer(Answers.CALLS_REAL_METHODS));
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
        thrown.expect(ThrowableMatchers.hasInitialCauseMatching(Matchers.instanceOf(
                IllegalArgumentException.class)));
        thrown.expect(ThrowableMatchers.hasInitialCauseMessageMatching(Matchers.containsString(
                "null context")));

        Mockito.mock(ExportMigrationPropertyReferencedEntryImpl.class,
                Mockito.withSettings()
                        .useConstructor(null, PROPERTY, UNIX_NAME)
                        .defaultAnswer(Answers.CALLS_REAL_METHODS));
    }

    @Test
    public void testConstructorWithNullProperty() throws Exception {
        thrown.expect(ThrowableMatchers.hasInitialCauseMatching(Matchers.instanceOf(
                IllegalArgumentException.class)));
        thrown.expect(ThrowableMatchers.hasInitialCauseMessageMatching(Matchers.containsString(
                "null property")));

        Mockito.mock(ExportMigrationPropertyReferencedEntryImpl.class,
                Mockito.withSettings()
                        .useConstructor(CONTEXT, null, UNIX_NAME)
                        .defaultAnswer(Answers.CALLS_REAL_METHODS));
    }

    @Test
    public void testConstructorWithNullPathname() throws Exception {
        thrown.expect(ThrowableMatchers.hasInitialCauseMatching(Matchers.instanceOf(
                IllegalArgumentException.class)));
        thrown.expect(ThrowableMatchers.hasInitialCauseMessageMatching(Matchers.containsString(
                "null pathname")));

        Mockito.mock(ExportMigrationPropertyReferencedEntryImpl.class,
                Mockito.withSettings()
                        .useConstructor(CONTEXT, PROPERTY, null)
                        .defaultAnswer(Answers.CALLS_REAL_METHODS));
    }

    @Test
    public void testGetProperty() throws Exception {
        Assert.assertThat(ENTRY.getProperty(), Matchers.equalTo(PROPERTY));
    }

    // cannot test equals() or hashCode() on mocks, will test them via the ExportMigrationSystemPropertyReferencedEntryImpl

    @Test
    public void testCompareToWhenEquals() throws Exception {
        final ExportMigrationPropertyReferencedEntryImpl ENTRY2 = Mockito.mock(
                ExportMigrationPropertyReferencedEntryImpl.class,
                Mockito.withSettings()
                        .useConstructor(CONTEXT, PROPERTY, UNIX_NAME)
                        .defaultAnswer(Answers.CALLS_REAL_METHODS));

        Assert.assertThat(ENTRY.compareTo(ENTRY2), Matchers.equalTo(0));
    }

    @Test
    public void testCompareToWhenIdentical() throws Exception {
        Assert.assertThat(ENTRY.compareTo(ENTRY), Matchers.equalTo(0));
    }

    @Test
    public void testCompareToWithNull() throws Exception {
        Assert.assertThat(ENTRY.compareTo(null), Matchers.greaterThan(0));
    }

    @Test
    public void testCompareToWhenSuperNotEqual() throws Exception {
        final ExportMigrationPropertyReferencedEntryImpl ENTRY2 = Mockito.mock(
                ExportMigrationPropertyReferencedEntryImpl.class,
                Mockito.withSettings()
                        .useConstructor(CONTEXT, PROPERTY, UNIX_NAME + '2')
                        .defaultAnswer(Answers.CALLS_REAL_METHODS));

        Assert.assertThat(ENTRY.compareTo(ENTRY2), Matchers.lessThan(0));
    }

    @Test
    public void testCompareToWithLesserProperty() throws Exception {
        final ExportMigrationPropertyReferencedEntryImpl ENTRY2 = Mockito.mock(
                ExportMigrationPropertyReferencedEntryImpl.class,
                Mockito.withSettings()
                        .useConstructor(CONTEXT, PROPERTY + '2', UNIX_NAME)
                        .defaultAnswer(Answers.CALLS_REAL_METHODS));

        Assert.assertThat(ENTRY.compareTo(ENTRY2), Matchers.lessThan(0));
    }

    @Test
    public void testCompareToWithGreaterProperty() throws Exception {
        final ExportMigrationPropertyReferencedEntryImpl ENTRY2 = Mockito.mock(
                ExportMigrationPropertyReferencedEntryImpl.class,
                Mockito.withSettings()
                        .useConstructor(CONTEXT, 'a' + PROPERTY, UNIX_NAME)
                        .defaultAnswer(Answers.CALLS_REAL_METHODS));

        Assert.assertThat(ENTRY.compareTo(ENTRY2), Matchers.greaterThan(0));
    }
}
