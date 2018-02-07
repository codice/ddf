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

import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.io.FilenameUtils;
import org.codice.ddf.test.common.matchers.ThrowableMatchers;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mockito;

public class ExportMigrationPropertyReferencedEntryImplTest extends AbstractMigrationSupport {
  private static final String[] DIRS = new String[] {"path", "path2"};

  private static final String FILENAME = "file.ext";

  private static final String UNIX_NAME = "path/path2/" + FILENAME;

  private static final Path FILE_PATH = Paths.get(FilenameUtils.separatorsToSystem(UNIX_NAME));

  private static final String PROPERTY = "property";

  private static final String MIGRATABLE_ID = "test-migratable";

  private final ExportMigrationReportImpl report = new ExportMigrationReportImpl();

  private final ExportMigrationContextImpl context = Mockito.mock(ExportMigrationContextImpl.class);

  private Path absoluteFilePath;

  private ExportMigrationPropertyReferencedEntryImpl entry;

  @Before
  public void setup() throws Exception {
    createFile(createDirectory(DIRS), FILENAME);
    absoluteFilePath = ddfHome.resolve(UNIX_NAME).toRealPath(LinkOption.NOFOLLOW_LINKS);

    Mockito.when(context.getPathUtils()).thenReturn(new PathUtils());
    Mockito.when(context.getReport()).thenReturn(report);
    Mockito.when(context.getId()).thenReturn(MIGRATABLE_ID);

    entry =
        Mockito.mock(
            ExportMigrationPropertyReferencedEntryImpl.class,
            Mockito.withSettings()
                .useConstructor(context, PROPERTY, UNIX_NAME)
                .defaultAnswer(Answers.CALLS_REAL_METHODS));
  }

  @Test
  public void testConstructor() throws Exception {
    Assert.assertThat(entry.getContext(), Matchers.sameInstance(context));
    Assert.assertThat(entry.getPath(), Matchers.equalTo(FILE_PATH));
    Assert.assertThat(entry.getAbsolutePath(), Matchers.equalTo(absoluteFilePath));
    Assert.assertThat(entry.getFile(), Matchers.equalTo(absoluteFilePath.toFile()));
    Assert.assertThat(entry.getName(), Matchers.equalTo(UNIX_NAME));
    Assert.assertThat(entry.getProperty(), Matchers.equalTo(PROPERTY));
  }

  @Test
  public void testConstructorWithNullContext() throws Exception {
    thrown.expect(
        ThrowableMatchers.hasInitialCauseMatching(
            Matchers.instanceOf(IllegalArgumentException.class)));
    thrown.expect(
        ThrowableMatchers.hasInitialCauseMessageMatching(Matchers.containsString("null context")));

    Mockito.mock(
        ExportMigrationPropertyReferencedEntryImpl.class,
        Mockito.withSettings()
            .useConstructor(null, PROPERTY, UNIX_NAME)
            .defaultAnswer(Answers.CALLS_REAL_METHODS));
  }

  @Test
  public void testConstructorWithNullProperty() throws Exception {
    thrown.expect(
        ThrowableMatchers.hasInitialCauseMatching(
            Matchers.instanceOf(IllegalArgumentException.class)));
    thrown.expect(
        ThrowableMatchers.hasInitialCauseMessageMatching(Matchers.containsString("null property")));

    Mockito.mock(
        ExportMigrationPropertyReferencedEntryImpl.class,
        Mockito.withSettings()
            .useConstructor(context, null, UNIX_NAME)
            .defaultAnswer(Answers.CALLS_REAL_METHODS));
  }

  @Test
  public void testConstructorWithNullPathname() throws Exception {
    thrown.expect(
        ThrowableMatchers.hasInitialCauseMatching(
            Matchers.instanceOf(IllegalArgumentException.class)));
    thrown.expect(
        ThrowableMatchers.hasInitialCauseMessageMatching(Matchers.containsString("null pathname")));

    Mockito.mock(
        ExportMigrationPropertyReferencedEntryImpl.class,
        Mockito.withSettings()
            .useConstructor(context, PROPERTY, null)
            .defaultAnswer(Answers.CALLS_REAL_METHODS));
  }

  @Test
  public void testGetProperty() throws Exception {
    Assert.assertThat(entry.getProperty(), Matchers.equalTo(PROPERTY));
  }

  // cannot test equals() or hashCode() on mocks, will test them via the
  // ExportMigrationSystemPropertyReferencedEntryImpl

  @Test
  public void testCompareToWhenEquals() throws Exception {
    final ExportMigrationPropertyReferencedEntryImpl entry2 =
        Mockito.mock(
            ExportMigrationPropertyReferencedEntryImpl.class,
            Mockito.withSettings()
                .useConstructor(context, PROPERTY, UNIX_NAME)
                .defaultAnswer(Answers.CALLS_REAL_METHODS));

    Assert.assertThat(entry.compareTo(entry2), Matchers.equalTo(0));
  }

  @Test
  @SuppressWarnings("SelfComparison")
  public void testCompareToWhenIdentical() throws Exception {
    Assert.assertThat(entry.compareTo(entry), Matchers.equalTo(0));
  }

  @Test
  public void testCompareToWithNull() throws Exception {
    Assert.assertThat(entry.compareTo(null), Matchers.greaterThan(0));
  }

  @Test
  public void testCompareToWhenSuperNotEqual() throws Exception {
    final ExportMigrationPropertyReferencedEntryImpl entry2 =
        Mockito.mock(
            ExportMigrationPropertyReferencedEntryImpl.class,
            Mockito.withSettings()
                .useConstructor(context, PROPERTY, UNIX_NAME + '2')
                .defaultAnswer(Answers.CALLS_REAL_METHODS));

    Assert.assertThat(entry.compareTo(entry2), Matchers.lessThan(0));
  }

  @Test
  public void testCompareToWithLesserProperty() throws Exception {
    final ExportMigrationPropertyReferencedEntryImpl entry2 =
        Mockito.mock(
            ExportMigrationPropertyReferencedEntryImpl.class,
            Mockito.withSettings()
                .useConstructor(context, PROPERTY + '2', UNIX_NAME)
                .defaultAnswer(Answers.CALLS_REAL_METHODS));

    Assert.assertThat(entry.compareTo(entry2), Matchers.lessThan(0));
  }

  @Test
  public void testCompareToWithGreaterProperty() throws Exception {
    final ExportMigrationPropertyReferencedEntryImpl entry2 =
        Mockito.mock(
            ExportMigrationPropertyReferencedEntryImpl.class,
            Mockito.withSettings()
                .useConstructor(context, 'a' + PROPERTY, UNIX_NAME)
                .defaultAnswer(Answers.CALLS_REAL_METHODS));

    Assert.assertThat(entry.compareTo(entry2), Matchers.greaterThan(0));
  }
}
