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

public class ImportMigrationSystemPropertyReferencedEntryImplTest extends AbstractMigrationSupport {
  private static final String MIGRATABLE_NAME = "where/some/dir/test.txt";

  private static final Path MIGRATABLE_PATH =
      Paths.get(FilenameUtils.separatorsToSystem(MIGRATABLE_NAME));

  private static final String MIGRATABLE_PROPERTY = "test.property";

  private final MigrationReportImpl report =
      Mockito.mock(
          MigrationReportImpl.class,
          Mockito.withSettings()
              .useConstructor(MigrationOperation.IMPORT, Optional.empty())
              .defaultAnswer(Mockito.CALLS_REAL_METHODS));

  private final Map<String, Object> metadata = new HashMap<>();

  private final ImportMigrationEntryImpl referencedEntry =
      Mockito.mock(ImportMigrationEntryImpl.class);

  private ImportMigrationContextImpl context;

  private ImportMigrationSystemPropertyReferencedEntryImpl entry;

  @Before
  public void setup() throws Exception {
    System.setProperty(MIGRATABLE_PROPERTY, createFile(MIGRATABLE_PATH).toString());

    metadata.put(MigrationEntryImpl.METADATA_REFERENCE, MIGRATABLE_NAME);
    metadata.put(MigrationEntryImpl.METADATA_PROPERTY, MIGRATABLE_PROPERTY);

    context = Mockito.mock(ImportMigrationContextImpl.class);

    Mockito.when(context.getPathUtils()).thenReturn(new PathUtils());
    Mockito.when(context.getReport()).thenReturn(report);
    Mockito.when(context.getId()).thenReturn(MIGRATABLE_ID);
    Mockito.when(context.getOptionalEntry(MIGRATABLE_PATH))
        .thenReturn(Optional.of(referencedEntry));
    Mockito.doAnswer(AdditionalAnswers.<Consumer<MigrationReport>>answerVoid(c -> c.accept(report)))
        .when(report)
        .doAfterCompletion(Mockito.any());

    entry = new ImportMigrationSystemPropertyReferencedEntryImpl(context, metadata);
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
  public void testVerifyPropertyAfterCompletionWhenPropertyIsStillDefined() throws Exception {
    entry.verifyPropertyAfterCompletion();

    Assert.assertThat(report.wasSuccessful(), Matchers.equalTo(true));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(false));

    Mockito.verify(report).doAfterCompletion(Mockito.notNull());
  }

  @Test
  public void testVerifyPropertyAfterCompletionWhenPropertyIsNotDefined() throws Exception {
    System.getProperties().remove(MIGRATABLE_PROPERTY);

    entry.verifyPropertyAfterCompletion();

    Assert.assertThat(report.wasSuccessful(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(true));

    thrown.expect(MigrationException.class);
    thrown.expectMessage(
        Matchers.matchesPattern(
            ".*system property \\[" + MIGRATABLE_PROPERTY + "\\].* no longer defined.*"));

    Mockito.verify(report).doAfterCompletion(Mockito.notNull());

    report.verifyCompletion(); // to trigger the exception
  }

  @Test
  public void testVerifyPropertyAfterCompletionWhenPropertyIsBlank() throws Exception {
    System.setProperty(MIGRATABLE_PROPERTY, "");

    entry.verifyPropertyAfterCompletion();

    Assert.assertThat(report.wasSuccessful(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(true));

    thrown.expect(MigrationException.class);
    thrown.expectMessage(
        Matchers.matchesPattern(
            ".*system property \\[" + MIGRATABLE_PROPERTY + "\\].* is now empty.*"));

    Mockito.verify(report).doAfterCompletion(Mockito.notNull());

    report.verifyCompletion(); // to trigger the exception
  }

  @Test
  public void testVerifyPropertyAfterCompletionWhenReferencedFileIsDifferent() throws Exception {
    System.setProperty(MIGRATABLE_PROPERTY, createFile(MIGRATABLE_PATH + "2").toString());

    entry.verifyPropertyAfterCompletion();

    Assert.assertThat(report.wasSuccessful(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(true));

    thrown.expect(MigrationException.class);
    thrown.expectMessage(
        Matchers.matchesPattern(
            ".*system property \\[" + MIGRATABLE_PROPERTY + "\\].* is now set to \\[.*2\\].*"));

    Mockito.verify(report).doAfterCompletion(Mockito.notNull());

    report.verifyCompletion(); // to trigger the exception
  }

  @Test
  public void testVerifyPropertyAfterCompletionWhenNewReferencedFileDoesNotExist()
      throws Exception {
    System.setProperty(MIGRATABLE_PROPERTY, MIGRATABLE_PATH + "2");

    entry.verifyPropertyAfterCompletion();

    Assert.assertThat(report.wasSuccessful(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(true));

    thrown.expect(MigrationException.class);
    thrown.expectMessage(
        Matchers.matchesPattern(
            ".*system property \\[" + MIGRATABLE_PROPERTY + "\\].* is now set to \\[.*2\\]; .*"));
    thrown.expectCause(Matchers.instanceOf(IOException.class));

    Mockito.verify(report).doAfterCompletion(Mockito.notNull());

    report.verifyCompletion(); // to trigger the exception
  }

  @Test
  public void testVerifyPropertyAfterCompletionWhenOriginalReferencedFileDoesNotExist()
      throws Exception {
    ddfHome.resolve(MIGRATABLE_PATH).toFile().delete();

    System.setProperty(MIGRATABLE_PROPERTY, createFile(MIGRATABLE_PATH + "2").toString());

    entry.verifyPropertyAfterCompletion();

    Assert.assertThat(report.wasSuccessful(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(true));

    thrown.expect(MigrationException.class);
    thrown.expectMessage(
        Matchers.matchesPattern(
            ".*system property \\[" + MIGRATABLE_PROPERTY + "\\].* is now set to \\[.*2\\]; .*"));
    thrown.expectCause(Matchers.instanceOf(IOException.class));

    Mockito.verify(report).doAfterCompletion(Mockito.notNull());

    report.verifyCompletion(); // to trigger the exception
  }
}
