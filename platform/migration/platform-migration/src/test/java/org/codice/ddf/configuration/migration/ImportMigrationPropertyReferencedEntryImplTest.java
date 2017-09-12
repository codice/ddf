package org.codice.ddf.configuration.migration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.FilenameUtils;
import org.codice.ddf.migration.ImportMigrationEntry;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationOperation;
import org.codice.ddf.migration.MigrationReport;
import org.codice.ddf.test.util.ThrowableMatchers;
import org.codice.ddf.util.function.EBiConsumer;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mockito;

import com.github.npathai.hamcrestopt.OptionalMatchers;
import com.google.common.collect.ImmutableMap;

public class ImportMigrationPropertyReferencedEntryImplTest extends AbstractMigrationTest {
    private static final String MIGRATABLE_NAME = "where/some/dir/test.txt";

    private static final Path MIGRATABLE_PATH = Paths.get(FilenameUtils.separatorsToSystem(
            MIGRATABLE_NAME));

    private static final String MIGRATABLE_PROPERTY = "test.property";

    private final MigrationReportImpl REPORT = new MigrationReportImpl(MigrationOperation.IMPORT,
            Optional.empty());

    private final Map<String, Object> METADATA = new HashMap<>();

    private ImportMigrationContextImpl CONTEXT;

    private ImportMigrationEntryImpl REFERENCED_ENTRY =
            Mockito.mock(ImportMigrationEntryImpl.class);

    private ImportMigrationPropertyReferencedEntryImpl ENTRY;

    @Before
    public void before() throws Exception {
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

        ENTRY = Mockito.mock(ImportMigrationPropertyReferencedEntryImpl.class,
                Mockito.withSettings()
                        .useConstructor(CONTEXT, METADATA)
                        .defaultAnswer(Answers.CALLS_REAL_METHODS));
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
    public void testConstructorWhenReferenceMetadataIsMissing() throws Exception {
        METADATA.remove(MigrationEntryImpl.METADATA_REFERENCE);

        // Mockito will throw its own wrapper exception below, so we must go to the initial cause to get the truths
        thrown.expect(ThrowableMatchers.hasInitialCauseMatching(Matchers.instanceOf(
                MigrationException.class)));
        thrown.expect(ThrowableMatchers.hasInitialCauseMessageMatching(Matchers.matchesPattern(
                ".*invalid metadata.*\\[" + MigrationEntryImpl.METADATA_REFERENCE + "\\].*")));

        Mockito.mock(ImportMigrationPropertyReferencedEntryImpl.class,
                Mockito.withSettings()
                        .useConstructor(CONTEXT, METADATA)
                        .defaultAnswer(Answers.CALLS_REAL_METHODS));
    }

    @Test
    public void testConstructorWhenPropertyMetadataIsMissing() throws Exception {
        METADATA.remove(MigrationEntryImpl.METADATA_PROPERTY);

        // Mockito will throw its own wrapper exception below, so we must go to the initial cause to get the truths
        thrown.expect(ThrowableMatchers.hasInitialCauseMatching(Matchers.instanceOf(
                MigrationException.class)));
        thrown.expect(ThrowableMatchers.hasInitialCauseMessageMatching(Matchers.matchesPattern(
                ".*invalid metadata.*\\[" + MigrationEntryImpl.METADATA_PROPERTY + "\\].*")));

        Mockito.mock(ImportMigrationPropertyReferencedEntryImpl.class,
                Mockito.withSettings()
                        .useConstructor(CONTEXT, METADATA)
                        .defaultAnswer(Answers.CALLS_REAL_METHODS));
    }

    @Test
    public void testConstructorWhenReferencedEntryIsNotFound() throws Exception {
        Mockito.when(CONTEXT.getOptionalEntry(MIGRATABLE_PATH))
                .thenReturn(Optional.empty());

        // Mockito will throw its own wrapper exception below, so we must go to the initial cause to get the truths
        thrown.expect(ThrowableMatchers.hasInitialCauseMatching(Matchers.instanceOf(
                MigrationException.class)));
        thrown.expect(ThrowableMatchers.hasInitialCauseMessageMatching(Matchers.matchesPattern(
                ".*invalid metadata.*path \\[" + MIGRATABLE_NAME + "\\] is missing.*")));

        Mockito.mock(ImportMigrationPropertyReferencedEntryImpl.class,
                Mockito.withSettings()
                        .useConstructor(CONTEXT, METADATA)
                        .defaultAnswer(Answers.CALLS_REAL_METHODS));
    }

    @Test
    public void testGetLastModifiedTime() throws Exception {
        final long TIME = 123234L;

        Mockito.when(REFERENCED_ENTRY.getLastModifiedTime())
                .thenReturn(TIME);

        Assert.assertThat(ENTRY.getLastModifiedTime(), Matchers.equalTo(TIME));

        Mockito.verify(REFERENCED_ENTRY)
                .getLastModifiedTime();
    }

    @Test
    public void testGetInputStream() throws Exception {
        final InputStream IS = Mockito.mock(InputStream.class);

        Mockito.when(REFERENCED_ENTRY.getInputStream())
                .thenReturn(Optional.of(IS));
        Mockito.doNothing()
                .when(ENTRY)
                .verifyPropertyAfterCompletion();

        Assert.assertThat(ENTRY.getInputStream(),
                OptionalMatchers.hasValue(Matchers.sameInstance(IS)));

        Mockito.verify(REFERENCED_ENTRY)
                .getInputStream();
        Mockito.verify(ENTRY)
                .verifyPropertyAfterCompletion();
    }

    @Test
    public void testGetInputStreamWhenAlreadyCalled() throws Exception {
        final InputStream IS = Mockito.mock(InputStream.class);

        Mockito.when(REFERENCED_ENTRY.getInputStream())
                .thenReturn(Optional.of(IS));
        Mockito.doNothing()
                .when(ENTRY)
                .verifyPropertyAfterCompletion();

        ENTRY.getInputStream();

        Assert.assertThat(ENTRY.getInputStream(),
                OptionalMatchers.hasValue(Matchers.sameInstance(IS)));

        Mockito.verify(REFERENCED_ENTRY, Mockito.times(2))
                .getInputStream();
        Mockito.verify(ENTRY)
                .verifyPropertyAfterCompletion();
    }

    @Test
    public void testRestore() throws Exception {
        final boolean REQUIRED = true;

        Mockito.when(REFERENCED_ENTRY.restore(REQUIRED))
                .thenReturn(true);
        Mockito.doNothing()
                .when(ENTRY)
                .verifyPropertyAfterCompletion();

        Assert.assertThat(ENTRY.restore(), Matchers.equalTo(true));

        Mockito.verify(REFERENCED_ENTRY)
                .restore(REQUIRED);
        Mockito.verify(ENTRY)
                .verifyPropertyAfterCompletion();
    }

    @Test
    public void testRestoreWhenRequired() throws Exception {
        final boolean REQUIRED = true;

        Mockito.when(REFERENCED_ENTRY.restore(REQUIRED))
                .thenReturn(true);
        Mockito.doNothing()
                .when(ENTRY)
                .verifyPropertyAfterCompletion();

        Assert.assertThat(ENTRY.restore(REQUIRED), Matchers.equalTo(true));

        Mockito.verify(REFERENCED_ENTRY)
                .restore(REQUIRED);
        Mockito.verify(ENTRY)
                .verifyPropertyAfterCompletion();
    }

    @Test
    public void testRestoreWhenOptional() throws Exception {
        final boolean REQUIRED = false;

        Mockito.when(REFERENCED_ENTRY.restore(REQUIRED))
                .thenReturn(true);
        Mockito.doNothing()
                .when(ENTRY)
                .verifyPropertyAfterCompletion();

        Assert.assertThat(ENTRY.restore(REQUIRED), Matchers.equalTo(true));

        Mockito.verify(REFERENCED_ENTRY)
                .restore(REQUIRED);
        Mockito.verify(ENTRY)
                .verifyPropertyAfterCompletion();
    }

    @Test
    public void testRestoreWhenFailed() throws Exception {
        final boolean REQUIRED = true;

        Mockito.when(REFERENCED_ENTRY.restore(REQUIRED))
                .thenReturn(false);
        Mockito.doNothing()
                .when(ENTRY)
                .verifyPropertyAfterCompletion();

        Assert.assertThat(ENTRY.restore(REQUIRED), Matchers.equalTo(false));

        Mockito.verify(REFERENCED_ENTRY)
                .restore(REQUIRED);
        Mockito.verify(ENTRY, Mockito.never())
                .verifyPropertyAfterCompletion();
    }

    @Test
    public void testRestoreWhenAlreadyCalled() throws Exception {
        final boolean REQUIRED = true;

        Mockito.when(REFERENCED_ENTRY.restore(REQUIRED))
                .thenReturn(true);
        Mockito.doNothing()
                .when(ENTRY)
                .verifyPropertyAfterCompletion();

        ENTRY.restore(REQUIRED);

        Assert.assertThat(ENTRY.restore(REQUIRED), Matchers.equalTo(true));

        Mockito.verify(REFERENCED_ENTRY)
                .restore(REQUIRED);
        Mockito.verify(ENTRY)
                .verifyPropertyAfterCompletion();
    }

    @Test
    public void testRestoreWithConsumer() throws Exception {
        final EBiConsumer<MigrationReport, Optional<InputStream>, IOException> CONSUMER =
                Mockito.mock(EBiConsumer.class);

        Mockito.when(REFERENCED_ENTRY.restore(CONSUMER))
                .thenReturn(true);
        Mockito.doNothing()
                .when(ENTRY)
                .verifyPropertyAfterCompletion();

        Assert.assertThat(ENTRY.restore(CONSUMER), Matchers.equalTo(true));

        Mockito.verify(REFERENCED_ENTRY)
                .restore(CONSUMER);
        Mockito.verify(ENTRY)
                .verifyPropertyAfterCompletion();
    }

    @Test
    public void testRestoreWithConsumerWhenFailed() throws Exception {
        final EBiConsumer<MigrationReport, Optional<InputStream>, IOException> CONSUMER =
                Mockito.mock(EBiConsumer.class);

        Mockito.when(REFERENCED_ENTRY.restore(CONSUMER))
                .thenReturn(false);
        Mockito.doNothing()
                .when(ENTRY)
                .verifyPropertyAfterCompletion();

        Assert.assertThat(ENTRY.restore(CONSUMER), Matchers.equalTo(false));

        Mockito.verify(REFERENCED_ENTRY)
                .restore(CONSUMER);
        Mockito.verify(ENTRY, Mockito.never())
                .verifyPropertyAfterCompletion();
    }

    @Test
    public void testRestoreWithNullConsumer() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null consumer"));

        ENTRY.restore(null);

        Mockito.verify(ENTRY, Mockito.never())
                .verifyPropertyAfterCompletion();
    }

    @Test
    public void testGetPropertyReferencedEntry() throws Exception {
        final ImportMigrationEntry PROPERTY_ENTRY = Mockito.mock(ImportMigrationEntry.class);
        final String PROPERTY_NAME = "test.property";

        Mockito.when(REFERENCED_ENTRY.getPropertyReferencedEntry(PROPERTY_NAME))
                .thenReturn(Optional.of(PROPERTY_ENTRY));

        Assert.assertThat(ENTRY.getPropertyReferencedEntry(PROPERTY_NAME),
                OptionalMatchers.hasValue(Matchers.sameInstance(PROPERTY_ENTRY)));

        Mockito.verify(REFERENCED_ENTRY)
                .getPropertyReferencedEntry(PROPERTY_NAME);
    }

    // cannot test equals() or hashcode() from a mocked abstract class with Mockito so they will be tested in ImportMigrationJavaPropertyReferencedEntryImplTest

    @Test
    public void testCompareToWhenEquals() throws Exception {
        final ImportMigrationPropertyReferencedEntryImpl ENTRY2 = Mockito.mock(
                ImportMigrationPropertyReferencedEntryImpl.class,
                Mockito.withSettings()
                        .useConstructor(CONTEXT, METADATA)
                        .defaultAnswer(Answers.CALLS_REAL_METHODS));

        Assert.assertThat(ENTRY.compareTo(ENTRY2), Matchers.equalTo(0));
    }

    @Test
    public void testCompareToWhenIdentical() throws Exception {
        Assert.assertThat(ENTRY.compareTo(ENTRY), Matchers.equalTo(0));
    }

    @Test
    public void testCompareToWhenNameDifferent() throws Exception {
        final String MIGRATABLE_NAME2 = "where/some/dir/test2.txt";
        final Path MIGRATABLE_PATH2 = Paths.get(FilenameUtils.separatorsToSystem(MIGRATABLE_NAME2));
        final Map<String, Object> METADATA2 = ImmutableMap.of(MigrationEntryImpl.METADATA_REFERENCE,
                MIGRATABLE_NAME2,
                MigrationEntryImpl.METADATA_PROPERTY,
                MIGRATABLE_PROPERTY);

        Mockito.when(CONTEXT.getOptionalEntry(MIGRATABLE_PATH2))
                .thenReturn(Optional.of(REFERENCED_ENTRY));

        final ImportMigrationPropertyReferencedEntryImpl ENTRY2 = Mockito.mock(
                ImportMigrationPropertyReferencedEntryImpl.class,
                Mockito.withSettings()
                        .useConstructor(CONTEXT, METADATA2)
                        .defaultAnswer(Answers.CALLS_REAL_METHODS));

        Assert.assertThat(ENTRY.compareTo(ENTRY2), Matchers.not(Matchers.equalTo(0)));
    }

    @Test
    public void testCompareToWhenPropertyLess() throws Exception {
        final Map<String, Object> METADATA2 = ImmutableMap.of(MigrationEntryImpl.METADATA_REFERENCE,
                MIGRATABLE_NAME,
                MigrationEntryImpl.METADATA_PROPERTY,
                MIGRATABLE_PROPERTY + 'a');
        final ImportMigrationPropertyReferencedEntryImpl ENTRY2 = Mockito.mock(
                ImportMigrationPropertyReferencedEntryImpl.class,
                Mockito.withSettings()
                        .useConstructor(CONTEXT, METADATA2)
                        .defaultAnswer(Answers.CALLS_REAL_METHODS));

        Assert.assertThat(ENTRY.compareTo(ENTRY2), Matchers.lessThan(0));
    }

    @Test
    public void testCompareToWhenPropertyGreater() throws Exception {
        final Map<String, Object> METADATA2 = ImmutableMap.of(MigrationEntryImpl.METADATA_REFERENCE,
                MIGRATABLE_NAME,
                MigrationEntryImpl.METADATA_PROPERTY,
                'a' + MIGRATABLE_PROPERTY);

        final ImportMigrationPropertyReferencedEntryImpl ENTRY2 = Mockito.mock(
                ImportMigrationPropertyReferencedEntryImpl.class,
                Mockito.withSettings()
                        .useConstructor(CONTEXT, METADATA2)
                        .defaultAnswer(Answers.CALLS_REAL_METHODS));

        Assert.assertThat(ENTRY.compareTo(ENTRY2), Matchers.greaterThan(0));
    }
}

