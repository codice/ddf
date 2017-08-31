package org.codice.ddf.configuration.migration;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.commons.io.FilenameUtils;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationOperation;
import org.codice.ddf.migration.MigrationReport;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.Mockito;

public class ImportMigrationSystemPropertyReferencedEntryImplTest extends AbstractMigrationTest {
    private static final String MIGRATABLE_NAME = "where/some/dir/test.txt";

    private static final Path MIGRATABLE_PATH = Paths.get(FilenameUtils.separatorsToSystem(
            MIGRATABLE_NAME));

    private static final String MIGRATABLE_PROPERTY = "test.property";

    private final MigrationReportImpl REPORT = Mockito.mock(MigrationReportImpl.class,
            Mockito.withSettings()
                    .useConstructor(MigrationOperation.IMPORT, Optional.empty())
                    .defaultAnswer(Mockito.CALLS_REAL_METHODS));

    private final Map<String, Object> METADATA = new HashMap<>();

    private final ImportMigrationEntryImpl REFERENCED_ENTRY =
            Mockito.mock(ImportMigrationEntryImpl.class);

    private ImportMigrationContextImpl CONTEXT;

    private ImportMigrationSystemPropertyReferencedEntryImpl ENTRY;

    @Before
    public void setup() throws Exception {
        System.setProperty(MIGRATABLE_PROPERTY, createFile(MIGRATABLE_PATH).toString());

        METADATA.put(MigrationEntryImpl.METADATA_REFERENCE, MIGRATABLE_NAME);
        METADATA.put(MigrationEntryImpl.METADATA_PROPERTY, MIGRATABLE_PROPERTY);

        CONTEXT = Mockito.mock(ImportMigrationContextImpl.class);

        Mockito.when(CONTEXT.getPathUtils())
                .thenReturn(new PathUtils());
        Mockito.when(CONTEXT.getReport())
                .thenReturn(REPORT);
        Mockito.when(CONTEXT.getId())
                .thenReturn(MIGRATABLE_ID);
        Mockito.when(CONTEXT.getOptionalEntry(MIGRATABLE_PATH))
                .thenReturn(Optional.of(REFERENCED_ENTRY));
        Mockito.doAnswer(AdditionalAnswers.<Consumer<MigrationReport>>answerVoid(c -> c.accept(
                REPORT)))
                .when(REPORT)
                .doAfterCompletion(Mockito.any());

        ENTRY = new ImportMigrationSystemPropertyReferencedEntryImpl(CONTEXT, METADATA);
    }

    @Test
    public void testConstructor() throws Exception {
        Assert.assertThat(ENTRY.getName(), Matchers.equalTo(MIGRATABLE_NAME));
        Assert.assertThat(ENTRY.getPath(), Matchers.equalTo(MIGRATABLE_PATH));
        Assert.assertThat(ENTRY.getContext(), Matchers.sameInstance(CONTEXT));
        Assert.assertThat(ENTRY.getReport(), Matchers.sameInstance(REPORT));
        Assert.assertThat(ENTRY.getProperty(), Matchers.equalTo(MIGRATABLE_PROPERTY));
        Assert.assertThat(ENTRY.getReferencedEntry(), Matchers.sameInstance(REFERENCED_ENTRY));
    }

    @Test
    public void testVerifyPropertyAfterCompletionWhenPropertyIsStillDefined() throws Exception {
        ENTRY.verifyPropertyAfterCompletion();

        Assert.assertThat(REPORT.wasSuccessful(), Matchers.equalTo(true));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(false));

        Mockito.verify(REPORT)
                .doAfterCompletion(Mockito.notNull());
    }

    @Test
    public void testVerifyPropertyAfterCompletionWhenPropertyIsNotDefined() throws Exception {
        System.getProperties()
                .remove(MIGRATABLE_PROPERTY);

        ENTRY.verifyPropertyAfterCompletion();

        Assert.assertThat(REPORT.wasSuccessful(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(true));

        thrown.expect(MigrationException.class);
        thrown.expectMessage(Matchers.matchesPattern(
                ".*system property \\[" + MIGRATABLE_PROPERTY + "\\].* no longer defined.*"));

        Mockito.verify(REPORT)
                .doAfterCompletion(Mockito.notNull());

        REPORT.verifyCompletion(); // to trigger the exception
    }

    @Test
    public void testVerifyPropertyAfterCompletionWhenPropertyIsBlank() throws Exception {
        System.setProperty(MIGRATABLE_PROPERTY, "");

        ENTRY.verifyPropertyAfterCompletion();

        Assert.assertThat(REPORT.wasSuccessful(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(true));

        thrown.expect(MigrationException.class);
        thrown.expectMessage(Matchers.matchesPattern(
                ".*system property \\[" + MIGRATABLE_PROPERTY + "\\].* is now empty.*"));

        Mockito.verify(REPORT)
                .doAfterCompletion(Mockito.notNull());

        REPORT.verifyCompletion(); // to trigger the exception
    }

    @Test
    public void testVerifyPropertyAfterCompletionWhenReferencedFileIsDifferent() throws Exception {
        System.setProperty(MIGRATABLE_PROPERTY, createFile(MIGRATABLE_PATH + "2").toString());

        ENTRY.verifyPropertyAfterCompletion();

        Assert.assertThat(REPORT.wasSuccessful(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(true));

        thrown.expect(MigrationException.class);
        thrown.expectMessage(Matchers.matchesPattern(
                ".*system property \\[" + MIGRATABLE_PROPERTY + "\\].* is now set to \\[.*2\\].*"));

        Mockito.verify(REPORT)
                .doAfterCompletion(Mockito.notNull());

        REPORT.verifyCompletion(); // to trigger the exception
    }

    @Test
    public void testVerifyPropertyAfterCompletionWhenNewReferencedFileDoesNotExist()
            throws Exception {
        System.setProperty(MIGRATABLE_PROPERTY, MIGRATABLE_PATH + "2");

        ENTRY.verifyPropertyAfterCompletion();

        Assert.assertThat(REPORT.wasSuccessful(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(true));

        thrown.expect(MigrationException.class);
        thrown.expectMessage(Matchers.matchesPattern(".*system property \\[" + MIGRATABLE_PROPERTY
                + "\\].* is now set to \\[.*2\\]; .*"));
        thrown.expectCause(Matchers.instanceOf(IOException.class));

        Mockito.verify(REPORT)
                .doAfterCompletion(Mockito.notNull());

        REPORT.verifyCompletion(); // to trigger the exception
    }

    @Test
    public void testVerifyPropertyAfterCompletionWhenOriginalReferencedFileDoesNotExist()
            throws Exception {
        DDF_HOME.resolve(MIGRATABLE_PATH)
                .toFile()
                .delete();

        System.setProperty(MIGRATABLE_PROPERTY, createFile(MIGRATABLE_PATH + "2").toString());

        ENTRY.verifyPropertyAfterCompletion();

        Assert.assertThat(REPORT.wasSuccessful(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasWarnings(), Matchers.equalTo(false));
        Assert.assertThat(REPORT.hasErrors(), Matchers.equalTo(true));

        thrown.expect(MigrationException.class);
        thrown.expectMessage(Matchers.matchesPattern(".*system property \\[" + MIGRATABLE_PROPERTY
                + "\\].* is now set to \\[.*2\\]; .*"));
        thrown.expectCause(Matchers.instanceOf(IOException.class));

        Mockito.verify(REPORT)
                .doAfterCompletion(Mockito.notNull());

        REPORT.verifyCompletion(); // to trigger the exception
    }
}
