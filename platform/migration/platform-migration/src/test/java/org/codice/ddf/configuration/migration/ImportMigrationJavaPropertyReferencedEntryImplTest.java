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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.io.FileUtils;
import org.codice.ddf.migration.MigrationException;
import org.codice.ddf.migration.MigrationOperation;
import org.codice.ddf.migration.MigrationReport;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ImportMigrationJavaPropertyReferencedEntryImplTest extends AbstractMigrationSupport {

  public static final String PROPERTIES_PATH = "file.properties";

  public static final String PROPERTY_NAME = "property.name";

  public static final String REFERENCED_PATH = "file.txt";

  private static final Map<String, Object> METADATA_MAP =
      ImmutableMap.of(
          MigrationEntryImpl.METADATA_NAME,
          PROPERTIES_PATH,
          MigrationEntryImpl.METADATA_REFERENCE,
          REFERENCED_PATH,
          MigrationEntryImpl.METADATA_PROPERTY,
          PROPERTY_NAME);

  @Mock public ImportMigrationContextImpl mockContext;

  @Mock private ImportMigrationEntryImpl referencedEntry;

  public ImportMigrationJavaPropertyReferencedEntryImpl entry;

  public Path properties;

  public Path path;

  public MigrationReport report;

  @Before
  public void setup() throws Exception {
    properties = ddfHome.resolve(createFile(PROPERTIES_PATH)).toRealPath(LinkOption.NOFOLLOW_LINKS);
    FileUtils.writeStringToFile(
        properties.toFile(), PROPERTY_NAME + '=' + REFERENCED_PATH, Charsets.UTF_8, false);

    path = ddfHome.resolve(createFile(REFERENCED_PATH)).toRealPath(LinkOption.NOFOLLOW_LINKS);

    report =
        mock(
            MigrationReportImpl.class,
            Mockito.withSettings()
                .useConstructor(MigrationOperation.IMPORT, Optional.empty())
                .defaultAnswer(Mockito.CALLS_REAL_METHODS));

    when(mockContext.getPathUtils()).thenReturn(new PathUtils());
    when(mockContext.getReport()).thenReturn(report);
    when(mockContext.getOptionalEntry(any(Path.class))).thenReturn(Optional.of(referencedEntry));

    entry = new ImportMigrationJavaPropertyReferencedEntryImpl(mockContext, METADATA_MAP);
  }

  @Test
  public void getPropertiesPath() {
    assertThat(entry.getPropertiesPath().toString(), equalTo(PROPERTIES_PATH));
  }

  @Test
  public void hashCodeShouldBeEqual() {
    ImportMigrationJavaPropertyReferencedEntryImpl entry2 =
        new ImportMigrationJavaPropertyReferencedEntryImpl(mockContext, METADATA_MAP);
    assertThat(entry.hashCode(), equalTo(entry2.hashCode()));
  }

  @Test
  public void hashCodeShouldNotBeEqualWithDifferentPropertiesPath() {
    Map<String, Object> metadataMap =
        ImmutableMap.of(
            MigrationEntryImpl.METADATA_NAME,
            "Different properties path",
            MigrationEntryImpl.METADATA_REFERENCE,
            REFERENCED_PATH,
            MigrationEntryImpl.METADATA_PROPERTY,
            PROPERTY_NAME);
    ImportMigrationJavaPropertyReferencedEntryImpl entry2 =
        new ImportMigrationJavaPropertyReferencedEntryImpl(mockContext, metadataMap);
    assertThat(entry.hashCode(), not(equalTo(entry2.hashCode())));
  }

  @Test
  public void shouldBeEqual() {
    ImportMigrationJavaPropertyReferencedEntryImpl entry2 =
        new ImportMigrationJavaPropertyReferencedEntryImpl(mockContext, METADATA_MAP);
    assertThat("The entries are equal", entry.equals(entry2), is(true));
  }

  @Test
  public void testEqualWhenIdentical() {
    assertThat("The entries are equal", entry.equals(entry), is(true));
  }

  @SuppressWarnings("PMD.EqualsNull" /* purposely testing equals() when called with null */)
  @Test
  public void shouldNotBeEqualBecauseSuperIsNotEqual() {
    assertThat("The entries are not equal", entry.equals(null), is(false));
  }

  @Test
  public void testCompareToWhenEquals() throws Exception {
    final ImportMigrationJavaPropertyReferencedEntryImpl entry2 =
        new ImportMigrationJavaPropertyReferencedEntryImpl(mockContext, METADATA_MAP);

    Assert.assertThat(entry.compareTo(entry2), Matchers.equalTo(0));
  }

  @Test
  @SuppressWarnings("SelfComparison")
  public void testCompareToWhenIdentical() throws Exception {
    Assert.assertThat(entry.compareTo(entry), Matchers.equalTo(0));
  }

  @Test
  public void testCompareToWhenSuperIsDifferent() throws Exception {
    final Map<String, Object> metadataMap =
        ImmutableMap.of(
            MigrationEntryImpl.METADATA_NAME,
            PROPERTIES_PATH,
            MigrationEntryImpl.METADATA_REFERENCE,
            REFERENCED_PATH,
            MigrationEntryImpl.METADATA_PROPERTY,
            "Different" + PROPERTY_NAME);
    final ImportMigrationJavaPropertyReferencedEntryImpl entry2 =
        new ImportMigrationJavaPropertyReferencedEntryImpl(mockContext, metadataMap);

    Assert.assertThat(entry.compareTo(entry2), Matchers.not(Matchers.equalTo(0)));
  }

  @Test
  public void testCompareToWhenPropertiesPathLess() throws Exception {
    final Map<String, Object> metadataMap =
        ImmutableMap.of(
            MigrationEntryImpl.METADATA_NAME,
            PROPERTIES_PATH + 'a',
            MigrationEntryImpl.METADATA_REFERENCE,
            REFERENCED_PATH,
            MigrationEntryImpl.METADATA_PROPERTY,
            PROPERTY_NAME);
    final ImportMigrationJavaPropertyReferencedEntryImpl entry2 =
        new ImportMigrationJavaPropertyReferencedEntryImpl(mockContext, metadataMap);

    Assert.assertThat(entry.compareTo(entry2), Matchers.lessThan(0));
  }

  @Test
  public void testCompareToWhenPropertiesPathGreater() throws Exception {
    final Map<String, Object> metadataMap =
        ImmutableMap.of(
            MigrationEntryImpl.METADATA_NAME,
            'A' + PROPERTIES_PATH,
            MigrationEntryImpl.METADATA_REFERENCE,
            REFERENCED_PATH,
            MigrationEntryImpl.METADATA_PROPERTY,
            PROPERTY_NAME);
    final ImportMigrationJavaPropertyReferencedEntryImpl entry2 =
        new ImportMigrationJavaPropertyReferencedEntryImpl(mockContext, metadataMap);

    Assert.assertThat(entry.compareTo(entry2), Matchers.greaterThan(0));
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
    FileUtils.writeStringToFile(properties.toFile(), "prop=value", Charsets.UTF_8, false);

    entry.verifyPropertyAfterCompletion();

    Assert.assertThat(report.wasSuccessful(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(true));

    thrown.expect(MigrationException.class);
    thrown.expectMessage(
        Matchers.matchesPattern(
            ".*Java property \\[" + PROPERTY_NAME + "\\].* no longer defined.*"));

    Mockito.verify(report).doAfterCompletion(Mockito.notNull());

    report.verifyCompletion(); // to trigger the exception
  }

  @Test
  public void testVerifyPropertyAfterCompletionWhenPropertyIsBlank() throws Exception {
    FileUtils.writeStringToFile(properties.toFile(), PROPERTY_NAME + '=', Charsets.UTF_8, false);

    entry.verifyPropertyAfterCompletion();

    Assert.assertThat(report.wasSuccessful(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(true));

    thrown.expect(MigrationException.class);
    thrown.expectMessage(
        Matchers.matchesPattern(".*Java property \\[" + PROPERTY_NAME + "\\].* is now empty.*"));

    Mockito.verify(report).doAfterCompletion(Mockito.notNull());

    report.verifyCompletion(); // to trigger the exception
  }

  @Test
  public void testVerifyPropertyAfterCompletionWhenReferencedFileIsDifferent() throws Exception {
    FileUtils.writeStringToFile(
        properties.toFile(),
        PROPERTY_NAME + '=' + createFile(REFERENCED_PATH + "2").toString(),
        Charsets.UTF_8,
        false);

    entry.verifyPropertyAfterCompletion();

    Assert.assertThat(report.wasSuccessful(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(true));

    thrown.expect(MigrationException.class);
    thrown.expectMessage(
        Matchers.matchesPattern(
            ".*Java property \\[" + PROPERTY_NAME + "\\].* is now set to \\[.*2\\].*"));

    Mockito.verify(report).doAfterCompletion(Mockito.notNull());

    report.verifyCompletion(); // to trigger the exception
  }

  @Test
  public void testVerifyPropertyAfterCompletionWhenNewReferencedFileDoesNotExist()
      throws Exception {
    FileUtils.writeStringToFile(
        properties.toFile(), PROPERTY_NAME + '=' + REFERENCED_PATH + "2", Charsets.UTF_8, false);

    entry.verifyPropertyAfterCompletion();

    Assert.assertThat(report.wasSuccessful(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(true));

    thrown.expect(MigrationException.class);
    thrown.expectMessage(
        Matchers.matchesPattern(
            ".*Java property \\[" + PROPERTY_NAME + "\\].* is now set to \\[.*2\\]; .*"));
    thrown.expectCause(Matchers.instanceOf(IOException.class));

    Mockito.verify(report).doAfterCompletion(Mockito.notNull());

    report.verifyCompletion(); // to trigger the exception
  }

  @Test
  public void testVerifyPropertyAfterCompletionWhenOriginalReferencedFileDoesNotExist()
      throws Exception {
    path.toFile().delete();

    FileUtils.writeStringToFile(
        properties.toFile(), PROPERTY_NAME + '=' + REFERENCED_PATH + "2", Charsets.UTF_8, false);

    entry.verifyPropertyAfterCompletion();

    Assert.assertThat(report.wasSuccessful(), Matchers.equalTo(false));
    Assert.assertThat(report.hasWarnings(), Matchers.equalTo(false));
    Assert.assertThat(report.hasErrors(), Matchers.equalTo(true));

    thrown.expect(MigrationException.class);
    thrown.expectMessage(
        Matchers.matchesPattern(
            ".*Java property \\[" + PROPERTY_NAME + "\\].* is now set to \\[.*2\\]; .*"));
    thrown.expectCause(Matchers.instanceOf(IOException.class));

    Mockito.verify(report).doAfterCompletion(Mockito.notNull());

    report.verifyCompletion(); // to trigger the exception
  }
}
