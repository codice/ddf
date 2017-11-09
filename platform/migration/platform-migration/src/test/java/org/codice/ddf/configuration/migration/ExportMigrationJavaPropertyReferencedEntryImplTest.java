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
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationWarning;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ExportMigrationJavaPropertyReferencedEntryImplTest extends AbstractMigrationSupport {
  private static final String[] DIRS = new String[] {"path", "path2"};

  private static final String FILENAME = "file.ext";

  private static final String PROPERTIES_FILENAME = "file.properties";

  private static final String UNIX_NAME = "path/path2/" + FILENAME;

  private static final Path FILE_PATH = Paths.get(FilenameUtils.separatorsToSystem(UNIX_NAME));

  private static final String PROPERTY = "property";

  private static final String MIGRATABLE_ID = "test-migratable";

  private final ExportMigrationReportImpl report = new ExportMigrationReportImpl();

  private final ExportMigrationContextImpl context = Mockito.mock(ExportMigrationContextImpl.class);

  private Path absoluteFilePath;

  private Path propertiesPath;

  private ExportMigrationJavaPropertyReferencedEntryImpl entry;

  @Before
  public void setup() throws Exception {
    final Path path = createFile(createDirectory(DIRS), FILENAME);

    propertiesPath = createFile(path.getParent(), PROPERTIES_FILENAME);
    absoluteFilePath = ddfHome.resolve(UNIX_NAME).toRealPath(LinkOption.NOFOLLOW_LINKS);

    Mockito.when(context.getPathUtils()).thenReturn(new PathUtils());
    Mockito.when(context.getReport()).thenReturn(report);
    Mockito.when(context.getId()).thenReturn(MIGRATABLE_ID);

    entry =
        new ExportMigrationJavaPropertyReferencedEntryImpl(
            context, propertiesPath, PROPERTY, UNIX_NAME);
  }

  @Test
  public void testConstructor() throws Exception {
    Assert.assertThat(entry.getContext(), Matchers.sameInstance(context));
    Assert.assertThat(entry.getPath(), Matchers.equalTo(FILE_PATH));
    Assert.assertThat(entry.getAbsolutePath(), Matchers.equalTo(absoluteFilePath));
    Assert.assertThat(entry.getFile(), Matchers.equalTo(absoluteFilePath.toFile()));
    Assert.assertThat(entry.getName(), Matchers.equalTo(UNIX_NAME));
    Assert.assertThat(entry.getProperty(), Matchers.equalTo(PROPERTY));
    Assert.assertThat(entry.getPropertiesPath(), Matchers.equalTo(propertiesPath));
  }

  @Test
  public void testConstructorWithNullContext() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("null context");

    new ExportMigrationJavaPropertyReferencedEntryImpl(null, propertiesPath, PROPERTY, UNIX_NAME);
  }

  @Test
  public void testConstructorWithNullPropertyPath() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("null properties path");

    new ExportMigrationJavaPropertyReferencedEntryImpl(context, null, PROPERTY, UNIX_NAME);
  }

  @Test
  public void testConstructorWithNullProperty() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("null property");

    new ExportMigrationJavaPropertyReferencedEntryImpl(context, propertiesPath, null, UNIX_NAME);
  }

  @Test
  public void testConstructorWithNullPathname() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("null pathname");

    new ExportMigrationJavaPropertyReferencedEntryImpl(context, propertiesPath, PROPERTY, null);
  }

  @Test
  public void testGetPropertiesPath() throws Exception {
    Assert.assertThat(entry.getPropertiesPath(), Matchers.equalTo(propertiesPath));
  }

  @Test
  public void testRecordEntry() throws Exception {
    final ExportMigrationReportImpl report = Mockito.mock(ExportMigrationReportImpl.class);

    Mockito.when(context.getReport()).thenReturn(report);
    Mockito.when(report.recordJavaProperty(Mockito.any())).thenReturn(report);

    entry.recordEntry();

    Mockito.verify(report).recordJavaProperty(Mockito.same(entry));
  }

  @Test
  public void testToDebugString() throws Exception {
    final String debug = entry.toDebugString();

    Assert.assertThat(debug, Matchers.containsString("Java property"));
    Assert.assertThat(debug, Matchers.containsString("[" + PROPERTY + "]"));
    Assert.assertThat(debug, Matchers.containsString("[" + propertiesPath + "]"));
    Assert.assertThat(debug, Matchers.containsString("[" + FILE_PATH + "]"));
  }

  @Test
  public void testNewWarning() throws Exception {
    final String reason = "test reason";
    final MigrationWarning warning = entry.newWarning(reason);

    Assert.assertThat(warning.getMessage(), Matchers.containsString("[" + PROPERTY + "]"));
    Assert.assertThat(warning.getMessage(), Matchers.containsString("[" + propertiesPath + "]"));
    Assert.assertThat(warning.getMessage(), Matchers.containsString("[" + FILE_PATH + "]"));
    Assert.assertThat(warning.getMessage(), Matchers.containsString(reason));
  }

  @Test
  public void testNewError() throws Exception {
    final String reason = "test reason";
    final IllegalArgumentException cause = new IllegalArgumentException("test cause");
    final MigrationException error = entry.newError(reason, cause);

    Assert.assertThat(error.getMessage(), Matchers.containsString("[" + PROPERTY + "]"));
    Assert.assertThat(error.getMessage(), Matchers.containsString("[" + propertiesPath + "]"));
    Assert.assertThat(error.getMessage(), Matchers.containsString("[" + FILE_PATH + "]"));
    Assert.assertThat(error.getMessage(), Matchers.containsString(reason));
    Assert.assertThat(error.getCause(), Matchers.sameInstance(cause));
  }

  @Test
  public void testCompareToWhenEquals() throws Exception {
    final ExportMigrationJavaPropertyReferencedEntryImpl entry2 =
        new ExportMigrationJavaPropertyReferencedEntryImpl(
            context, propertiesPath, PROPERTY, UNIX_NAME);

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
    final ExportMigrationJavaPropertyReferencedEntryImpl entry2 =
        new ExportMigrationJavaPropertyReferencedEntryImpl(
            context, propertiesPath, PROPERTY, UNIX_NAME + '2');

    Assert.assertThat(entry.compareTo(entry2), Matchers.lessThan(0));
  }

  @Test
  public void testCompareToWithLesserPropertyPath() throws Exception {
    final ExportMigrationJavaPropertyReferencedEntryImpl entry2 =
        new ExportMigrationJavaPropertyReferencedEntryImpl(
            context, Paths.get(propertiesPath.toString() + '2'), PROPERTY, UNIX_NAME);

    Assert.assertThat(entry.compareTo(entry2), Matchers.lessThan(0));
  }

  @Test
  public void testCompareToWithGreaterPropertyPath() throws Exception {
    final ExportMigrationJavaPropertyReferencedEntryImpl entry2 =
        new ExportMigrationJavaPropertyReferencedEntryImpl(
            context, Paths.get('a' + propertiesPath.toString()), PROPERTY, UNIX_NAME);

    Assert.assertThat(entry.compareTo(entry2), Matchers.greaterThan(0));
  }

  @Test
  public void testEqualsWhenEquals() throws Exception {
    final ExportMigrationJavaPropertyReferencedEntryImpl entry2 =
        new ExportMigrationJavaPropertyReferencedEntryImpl(
            context, propertiesPath, PROPERTY, UNIX_NAME);

    Assert.assertThat(entry.equals(entry2), Matchers.equalTo(true));
  }

  @Test
  public void testEqualsWhenIdentical() throws Exception {
    Assert.assertThat(entry.equals(entry), Matchers.equalTo(true));
  }

  @SuppressWarnings("PMD.EqualsNull" /* purposely testing equals() when called with null */)
  @Test
  public void testEqualsWhenNull() throws Exception {
    Assert.assertThat(entry.equals(null), Matchers.equalTo(false));
  }

  @Test
  public void testEqualsWhenPropertyPathsAreDifferent() throws Exception {
    final ExportMigrationJavaPropertyReferencedEntryImpl entry2 =
        new ExportMigrationJavaPropertyReferencedEntryImpl(
            context, propertiesPath.getParent(), PROPERTY, UNIX_NAME);

    Assert.assertThat(entry.equals(entry2), Matchers.equalTo(false));
  }

  @Test
  public void testHashCodeWhenEquals() throws Exception {
    final ExportMigrationJavaPropertyReferencedEntryImpl entry2 =
        new ExportMigrationJavaPropertyReferencedEntryImpl(
            context, propertiesPath, PROPERTY, UNIX_NAME);

    Assert.assertThat(entry.hashCode(), Matchers.equalTo(entry2.hashCode()));
  }

  @Test
  public void testHashCodeWhenDifferent() throws Exception {
    final ExportMigrationJavaPropertyReferencedEntryImpl entry2 =
        new ExportMigrationJavaPropertyReferencedEntryImpl(
            context, propertiesPath.getParent(), PROPERTY, UNIX_NAME);

    Assert.assertThat(entry.hashCode(), Matchers.not(Matchers.equalTo(entry2.hashCode())));
  }
}
