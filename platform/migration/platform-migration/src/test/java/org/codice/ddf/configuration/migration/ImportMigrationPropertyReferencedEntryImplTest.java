/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.configuration.migration;

import com.github.npathai.hamcrestopt.OptionalMatchers;
import com.google.common.collect.ImmutableMap;
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
import org.codice.ddf.test.common.matchers.ThrowableMatchers;
import org.codice.ddf.util.function.BiThrowingConsumer;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mockito;

public class ImportMigrationPropertyReferencedEntryImplTest extends AbstractMigrationSupport {

  private static final String MIGRATABLE_NAME = "where/some/dir/test.txt";

  private static final Path MIGRATABLE_PATH =
      Paths.get(FilenameUtils.separatorsToSystem(MIGRATABLE_NAME));

  private static final String MIGRATABLE_PROPERTY = "test.property";

  private final MigrationReportImpl report =
      new MigrationReportImpl(MigrationOperation.IMPORT, Optional.empty());

  private final Map<String, Object> metadata = new HashMap<>();

  private final ImportMigrationEntryImpl referencedEntry =
      Mockito.mock(ImportMigrationEntryImpl.class);

  private ImportMigrationContextImpl context;

  private ImportMigrationPropertyReferencedEntryImpl entry;

  @Before
  public void setup() throws Exception {
    metadata.put(MigrationEntryImpl.METADATA_REFERENCE, MIGRATABLE_NAME);
    metadata.put(MigrationEntryImpl.METADATA_PROPERTY, MIGRATABLE_PROPERTY);

    context = Mockito.mock(ImportMigrationContextImpl.class);

    Mockito.when(context.getPathUtils()).thenReturn(new PathUtils());
    Mockito.when(context.getReport()).thenReturn(report);
    Mockito.when(context.getId()).thenReturn(MIGRATABLE_ID);
    Mockito.when(context.getOptionalEntry(MIGRATABLE_PATH))
        .thenReturn(Optional.of(referencedEntry));

    entry =
        Mockito.mock(
            ImportMigrationPropertyReferencedEntryImpl.class,
            Mockito.withSettings()
                .useConstructor(context, metadata)
                .defaultAnswer(Answers.CALLS_REAL_METHODS));
  }

  @Test
  public void testConstructor() throws Exception {
    Assert.assertThat(entry.getName(), Matchers.equalTo(MIGRATABLE_NAME));
    Assert.assertThat(entry.getPath(), Matchers.equalTo(MIGRATABLE_PATH));
    Assert.assertThat(entry.getContext(), Matchers.sameInstance(context));
    Assert.assertThat(entry.getReport(), Matchers.sameInstance(report));
    Assert.assertThat(entry.getProperty(), Matchers.equalTo(MIGRATABLE_PROPERTY));
    Assert.assertThat(entry.getReferencedEntry(), Matchers.sameInstance(referencedEntry));
  }

  @Test
  public void testConstructorWhenReferenceMetadataIsMissing() throws Exception {
    metadata.remove(MigrationEntryImpl.METADATA_REFERENCE);

    // Mockito will throw its own wrapper exception below, so we must go to the initial cause to get
    // the truth
    thrown.expect(
        ThrowableMatchers.hasInitialCauseMatching(Matchers.instanceOf(MigrationException.class)));
    thrown.expect(
        ThrowableMatchers.hasInitialCauseMessageMatching(
            Matchers.matchesPattern(
                ".*invalid metadata.*\\[" + MigrationEntryImpl.METADATA_REFERENCE + "\\].*")));

    Mockito.mock(
        ImportMigrationPropertyReferencedEntryImpl.class,
        Mockito.withSettings()
            .useConstructor(context, metadata)
            .defaultAnswer(Answers.CALLS_REAL_METHODS));
  }

  @Test
  public void testConstructorWhenPropertyMetadataIsMissing() throws Exception {
    metadata.remove(MigrationEntryImpl.METADATA_PROPERTY);

    // Mockito will throw its own wrapper exception below, so we must go to the initial cause to get
    // the truth
    thrown.expect(
        ThrowableMatchers.hasInitialCauseMatching(Matchers.instanceOf(MigrationException.class)));
    thrown.expect(
        ThrowableMatchers.hasInitialCauseMessageMatching(
            Matchers.matchesPattern(
                ".*invalid metadata.*\\[" + MigrationEntryImpl.METADATA_PROPERTY + "\\].*")));

    Mockito.mock(
        ImportMigrationPropertyReferencedEntryImpl.class,
        Mockito.withSettings()
            .useConstructor(context, metadata)
            .defaultAnswer(Answers.CALLS_REAL_METHODS));
  }

  @Test
  public void testConstructorWhenReferencedEntryIsNotFound() throws Exception {
    Mockito.when(context.getOptionalEntry(MIGRATABLE_PATH)).thenReturn(Optional.empty());

    // Mockito will throw its own wrapper exception below, so we must go to the initial cause to get
    // the truth
    thrown.expect(
        ThrowableMatchers.hasInitialCauseMatching(Matchers.instanceOf(MigrationException.class)));
    thrown.expect(
        ThrowableMatchers.hasInitialCauseMessageMatching(
            Matchers.matchesPattern(
                ".*invalid metadata.*path \\[" + MIGRATABLE_NAME + "\\] is missing.*")));

    Mockito.mock(
        ImportMigrationPropertyReferencedEntryImpl.class,
        Mockito.withSettings()
            .useConstructor(context, metadata)
            .defaultAnswer(Answers.CALLS_REAL_METHODS));
  }

  @Test
  public void testGetLastModifiedTime() throws Exception {
    final long time = 123234L;

    Mockito.when(referencedEntry.getLastModifiedTime()).thenReturn(time);

    Assert.assertThat(entry.getLastModifiedTime(), Matchers.equalTo(time));

    Mockito.verify(referencedEntry).getLastModifiedTime();
  }

  @Test
  public void testGetInputStream() throws Exception {
    final InputStream is = Mockito.mock(InputStream.class);

    Mockito.when(referencedEntry.getInputStream()).thenReturn(Optional.of(is));
    Mockito.doNothing().when(entry).verifyPropertyAfterCompletion();

    Assert.assertThat(entry.getInputStream(), OptionalMatchers.hasValue(Matchers.sameInstance(is)));

    Mockito.verify(referencedEntry).getInputStream();
    Mockito.verify(entry).verifyPropertyAfterCompletion();
  }

  @Test
  public void testGetInputStreamWhenAlreadyCalled() throws Exception {
    final InputStream is = Mockito.mock(InputStream.class);

    Mockito.when(referencedEntry.getInputStream()).thenReturn(Optional.of(is));
    Mockito.doNothing().when(entry).verifyPropertyAfterCompletion();

    entry.getInputStream();

    Assert.assertThat(entry.getInputStream(), OptionalMatchers.hasValue(Matchers.sameInstance(is)));

    Mockito.verify(referencedEntry, Mockito.times(2)).getInputStream();
    Mockito.verify(entry).verifyPropertyAfterCompletion();
  }

  @Test
  public void testRestoreWhenRequired() throws Exception {
    final boolean required = true;

    Mockito.when(referencedEntry.restore(required)).thenReturn(true);
    Mockito.doNothing().when(entry).verifyPropertyAfterCompletion();

    Assert.assertThat(entry.restore(required), Matchers.equalTo(true));

    Mockito.verify(referencedEntry).restore(required);
    Mockito.verify(entry).verifyPropertyAfterCompletion();
  }

  @Test
  public void testRestoreWithFilterWhenRequiredAndMatching() throws Exception {
    final boolean required = true;

    Mockito.when(referencedEntry.restore(Mockito.eq(required), Mockito.any())).thenReturn(true);
    Mockito.doNothing().when(entry).verifyPropertyAfterCompletion();

    Assert.assertThat(entry.restore(required, p -> true), Matchers.equalTo(true));

    Mockito.verify(referencedEntry).restore(Mockito.eq(required), Mockito.any());
    Mockito.verify(entry).verifyPropertyAfterCompletion();
  }

  @Test
  public void testRestoreWithFilterWhenRequiredAndNotMatching() throws Exception {
    final boolean required = true;

    Mockito.when(referencedEntry.restore(Mockito.eq(required), Mockito.any())).thenReturn(false);
    Mockito.doNothing().when(entry).verifyPropertyAfterCompletion();

    Assert.assertThat(entry.restore(required, p -> false), Matchers.equalTo(false));

    Mockito.verify(referencedEntry).restore(Mockito.eq(required), Mockito.any());
    Mockito.verify(entry, Mockito.never()).verifyPropertyAfterCompletion();
  }

  @Test
  public void testRestoreWhenOptional() throws Exception {
    final boolean required = false;

    Mockito.when(referencedEntry.restore(required)).thenReturn(true);
    Mockito.doNothing().when(entry).verifyPropertyAfterCompletion();

    Assert.assertThat(entry.restore(required), Matchers.equalTo(true));

    Mockito.verify(referencedEntry).restore(required);
    Mockito.verify(entry).verifyPropertyAfterCompletion();
  }

  @Test
  public void testRestoreWithFilterWhenOptionalAndMatching() throws Exception {
    final boolean required = false;

    Mockito.when(referencedEntry.restore(Mockito.eq(required), Mockito.any())).thenReturn(true);
    Mockito.doNothing().when(entry).verifyPropertyAfterCompletion();

    Assert.assertThat(entry.restore(required, p -> true), Matchers.equalTo(true));

    Mockito.verify(referencedEntry).restore(Mockito.eq(required), Mockito.any());
    Mockito.verify(entry).verifyPropertyAfterCompletion();
  }

  @Test
  public void testRestoreWithFilterWhenOptionalAndNotMatching() throws Exception {
    final boolean required = false;

    Mockito.when(referencedEntry.restore(Mockito.eq(required), Mockito.any())).thenReturn(true);
    Mockito.doNothing().when(entry).verifyPropertyAfterCompletion();

    Assert.assertThat(entry.restore(required, p -> false), Matchers.equalTo(true));

    Mockito.verify(referencedEntry).restore(Mockito.eq(required), Mockito.any());
    Mockito.verify(entry, Mockito.never()).verifyPropertyAfterCompletion();
  }

  @Test
  public void testRestoreWhenRequiredAndFailed() throws Exception {
    final boolean required = true;

    Mockito.when(referencedEntry.restore(required)).thenReturn(false);
    Mockito.doNothing().when(entry).verifyPropertyAfterCompletion();

    Assert.assertThat(entry.restore(required), Matchers.equalTo(false));

    Mockito.verify(referencedEntry).restore(required);
    Mockito.verify(entry, Mockito.never()).verifyPropertyAfterCompletion();
  }

  @Test
  public void testRestoreWithFilterWhenRequiredAndFailedAndMatching() throws Exception {
    final boolean required = true;

    Mockito.when(referencedEntry.restore(Mockito.eq(required), Mockito.any())).thenReturn(false);
    Mockito.doNothing().when(entry).verifyPropertyAfterCompletion();

    Assert.assertThat(entry.restore(required, p -> true), Matchers.equalTo(false));

    Mockito.verify(referencedEntry).restore(Mockito.eq(required), Mockito.any());
    Mockito.verify(entry, Mockito.never()).verifyPropertyAfterCompletion();
  }

  @Test
  public void testRestoreWithFilterWhenRequiredAndFailedAndNotMatching() throws Exception {
    final boolean required = true;

    Mockito.when(referencedEntry.restore(Mockito.eq(required), Mockito.any())).thenReturn(false);
    Mockito.doNothing().when(entry).verifyPropertyAfterCompletion();

    Assert.assertThat(entry.restore(required, p -> false), Matchers.equalTo(false));

    Mockito.verify(referencedEntry).restore(Mockito.eq(required), Mockito.any());
    Mockito.verify(entry, Mockito.never()).verifyPropertyAfterCompletion();
  }

  @Test
  public void testRestoreWhenOptionalAndFailed() throws Exception {
    final boolean required = false;

    Mockito.when(referencedEntry.restore(required)).thenReturn(false);
    Mockito.doNothing().when(entry).verifyPropertyAfterCompletion();

    Assert.assertThat(entry.restore(required), Matchers.equalTo(false));

    Mockito.verify(referencedEntry).restore(required);
    Mockito.verify(entry, Mockito.never()).verifyPropertyAfterCompletion();
  }

  @Test
  public void testRestoreWithFilterWhenOptionalAndFailedAndMatching() throws Exception {
    final boolean required = false;

    Mockito.when(referencedEntry.restore(Mockito.eq(required), Mockito.any())).thenReturn(false);
    Mockito.doNothing().when(entry).verifyPropertyAfterCompletion();

    Assert.assertThat(entry.restore(required, p -> true), Matchers.equalTo(false));

    Mockito.verify(referencedEntry).restore(Mockito.eq(required), Mockito.any());
    Mockito.verify(entry, Mockito.never()).verifyPropertyAfterCompletion();
  }

  @Test
  public void testRestoreWithFilterWhenOptionalAndFailedAndNotMatching() throws Exception {
    final boolean required = false;

    Mockito.when(referencedEntry.restore(Mockito.eq(required), Mockito.any())).thenReturn(false);
    Mockito.doNothing().when(entry).verifyPropertyAfterCompletion();

    Assert.assertThat(entry.restore(required, p -> false), Matchers.equalTo(false));

    Mockito.verify(referencedEntry).restore(Mockito.eq(required), Mockito.any());
    Mockito.verify(entry, Mockito.never()).verifyPropertyAfterCompletion();
  }

  @Test
  public void testRestoreWhenAlreadyCalled() throws Exception {
    final boolean required = true;

    Mockito.when(referencedEntry.restore(required)).thenReturn(true);
    Mockito.doNothing().when(entry).verifyPropertyAfterCompletion();

    entry.restore(required);

    Assert.assertThat(entry.restore(required), Matchers.equalTo(true));

    Mockito.verify(referencedEntry).restore(required);
    Mockito.verify(entry).verifyPropertyAfterCompletion();
  }

  @Test
  public void testRestoreWithFilterWhenAlreadyCalledAndMatching() throws Exception {
    final boolean required = true;

    Mockito.when(referencedEntry.restore(required)).thenReturn(true);
    Mockito.when(referencedEntry.restore(Mockito.eq(required), Mockito.any())).thenReturn(false);
    Mockito.doNothing().when(entry).verifyPropertyAfterCompletion();

    entry.restore(required);

    Assert.assertThat(entry.restore(required, p -> true), Matchers.equalTo(true));

    Mockito.verify(referencedEntry).restore(required);
    Mockito.verify(entry).verifyPropertyAfterCompletion();
    Mockito.verify(referencedEntry, Mockito.never()).restore(Mockito.eq(required), Mockito.any());
  }

  @Test
  public void testRestoreWithFilterWhenAlreadyCalledAndNotMatching() throws Exception {
    final boolean required = true;

    Mockito.when(referencedEntry.restore(required)).thenReturn(false);
    Mockito.when(referencedEntry.restore(Mockito.eq(required), Mockito.any())).thenReturn(false);
    Mockito.doNothing().when(entry).verifyPropertyAfterCompletion();

    entry.restore(required);

    Assert.assertThat(entry.restore(required, p -> false), Matchers.equalTo(false));

    Mockito.verify(referencedEntry).restore(required);
    Mockito.verify(entry, Mockito.never()).verifyPropertyAfterCompletion();
    Mockito.verify(referencedEntry, Mockito.never()).restore(Mockito.eq(required), Mockito.any());
  }

  @Test
  public void testRestoreWithFilterWhenFilterIsNull() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null path filter"));

    entry.restore(true, null);
  }

  @Test
  public void testRestoreWithConsumer() throws Exception {
    final BiThrowingConsumer<MigrationReport, Optional<InputStream>, IOException> consumer =
        Mockito.mock(BiThrowingConsumer.class);

    Mockito.when(referencedEntry.restore(consumer)).thenReturn(true);
    Mockito.doNothing().when(entry).verifyPropertyAfterCompletion();

    Assert.assertThat(entry.restore(consumer), Matchers.equalTo(true));

    Mockito.verify(referencedEntry).restore(consumer);
    Mockito.verify(entry).verifyPropertyAfterCompletion();
  }

  @Test
  public void testRestoreWithConsumerWhenFailed() throws Exception {
    final BiThrowingConsumer<MigrationReport, Optional<InputStream>, IOException> consumer =
        Mockito.mock(BiThrowingConsumer.class);

    Mockito.when(referencedEntry.restore(consumer)).thenReturn(false);
    Mockito.doNothing().when(entry).verifyPropertyAfterCompletion();

    Assert.assertThat(entry.restore(consumer), Matchers.equalTo(false));

    Mockito.verify(referencedEntry).restore(consumer);
    Mockito.verify(entry, Mockito.never()).verifyPropertyAfterCompletion();
  }

  @Test
  public void testRestoreWithNullConsumer() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null consumer"));

    entry.restore((BiThrowingConsumer<MigrationReport, Optional<InputStream>, IOException>) null);

    Mockito.verify(entry, Mockito.never()).verifyPropertyAfterCompletion();
  }

  @Test
  public void testGetPropertyReferencedEntry() throws Exception {
    final ImportMigrationEntry propertyEntry = Mockito.mock(ImportMigrationEntry.class);
    final String propertyName = "test.property";

    Mockito.when(referencedEntry.getPropertyReferencedEntry(propertyName))
        .thenReturn(Optional.of(propertyEntry));

    Assert.assertThat(
        entry.getPropertyReferencedEntry(propertyName),
        OptionalMatchers.hasValue(Matchers.sameInstance(propertyEntry)));

    Mockito.verify(referencedEntry).getPropertyReferencedEntry(propertyName);
  }

  // cannot test equals() or hashcode() from a mocked abstract class with Mockito so they will be
  // tested in ImportMigrationJavaPropertyReferencedEntryImplTest

  @Test
  public void testCompareToWhenEquals() throws Exception {
    final ImportMigrationPropertyReferencedEntryImpl entry2 =
        Mockito.mock(
            ImportMigrationPropertyReferencedEntryImpl.class,
            Mockito.withSettings()
                .useConstructor(context, metadata)
                .defaultAnswer(Answers.CALLS_REAL_METHODS));

    Assert.assertThat(entry.compareTo(entry2), Matchers.equalTo(0));
  }

  @Test
  @SuppressWarnings("SelfComparison")
  public void testCompareToWhenIdentical() throws Exception {
    Assert.assertThat(entry.compareTo(entry), Matchers.equalTo(0));
  }

  @Test
  public void testCompareToWhenNameDifferent() throws Exception {
    final String migratableName2 = "where/some/dir/test2.txt";
    final Path migratablePath2 = Paths.get(FilenameUtils.separatorsToSystem(migratableName2));
    final Map<String, Object> metadata2 =
        ImmutableMap.of(
            MigrationEntryImpl.METADATA_REFERENCE,
            migratableName2,
            MigrationEntryImpl.METADATA_PROPERTY,
            MIGRATABLE_PROPERTY);

    Mockito.when(context.getOptionalEntry(migratablePath2))
        .thenReturn(Optional.of(referencedEntry));

    final ImportMigrationPropertyReferencedEntryImpl entry2 =
        Mockito.mock(
            ImportMigrationPropertyReferencedEntryImpl.class,
            Mockito.withSettings()
                .useConstructor(context, metadata2)
                .defaultAnswer(Answers.CALLS_REAL_METHODS));

    Assert.assertThat(entry.compareTo(entry2), Matchers.not(Matchers.equalTo(0)));
  }

  @Test
  public void testCompareToWhenPropertyLess() throws Exception {
    final Map<String, Object> metadata2 =
        ImmutableMap.of(
            MigrationEntryImpl.METADATA_REFERENCE,
            MIGRATABLE_NAME,
            MigrationEntryImpl.METADATA_PROPERTY,
            MIGRATABLE_PROPERTY + 'a');

    final ImportMigrationPropertyReferencedEntryImpl entry2 =
        Mockito.mock(
            ImportMigrationPropertyReferencedEntryImpl.class,
            Mockito.withSettings()
                .useConstructor(context, metadata2)
                .defaultAnswer(Answers.CALLS_REAL_METHODS));

    Assert.assertThat(entry.compareTo(entry2), Matchers.lessThan(0));
  }

  @Test
  public void testCompareToWhenPropertyGreater() throws Exception {
    final Map<String, Object> metadata2 =
        ImmutableMap.of(
            MigrationEntryImpl.METADATA_REFERENCE,
            MIGRATABLE_NAME,
            MigrationEntryImpl.METADATA_PROPERTY,
            'a' + MIGRATABLE_PROPERTY);

    final ImportMigrationPropertyReferencedEntryImpl entry2 =
        Mockito.mock(
            ImportMigrationPropertyReferencedEntryImpl.class,
            Mockito.withSettings()
                .useConstructor(context, metadata2)
                .defaultAnswer(Answers.CALLS_REAL_METHODS));

    Assert.assertThat(entry.compareTo(entry2), Matchers.greaterThan(0));
  }

  @Test
  public void testIsFile() throws Exception {
    Mockito.when(referencedEntry.isFile()).thenReturn(true);

    Assert.assertThat(entry.isFile(), Matchers.equalTo(true));

    Mockito.verify(referencedEntry).isFile();
  }

  @Test
  public void testIsDirectory() throws Exception {
    Mockito.when(referencedEntry.isDirectory()).thenReturn(false);

    Assert.assertThat(entry.isDirectory(), Matchers.equalTo(false));

    Mockito.verify(referencedEntry).isDirectory();
  }
}
