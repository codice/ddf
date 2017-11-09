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

import java.nio.file.Paths;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.migration.MigrationReport;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class MigrationEntryImplTest extends AbstractMigrationSupport {
  private static final String ENTRY_NAME = Paths.get("path/path2/file.ext").toString();

  private static final String MIGRATABLE_ID = "test-migratable";

  private final MigrationContextImpl context = Mockito.mock(MigrationContextImpl.class);

  private final MigrationEntryImpl entry =
      Mockito.mock(MigrationEntryImpl.class, Mockito.CALLS_REAL_METHODS);

  private final ExportMigrationContextImpl context2 =
      Mockito.mock(ExportMigrationContextImpl.class);

  private final MigrationEntryImpl entry2 =
      Mockito.mock(MigrationEntryImpl.class, Mockito.CALLS_REAL_METHODS);

  @Before
  public void setup() throws Exception {
    Mockito.when(entry.getName()).thenReturn(ENTRY_NAME);
    Mockito.when(entry.getContext()).thenReturn(context);
    Mockito.when(context.getId()).thenReturn(MIGRATABLE_ID);

    Mockito.when(entry2.getName()).thenReturn(ENTRY_NAME);
    Mockito.when(entry2.getContext()).thenReturn(context2);
    Mockito.when(context2.getId()).thenReturn(MIGRATABLE_ID);
  }

  @Test
  public void testGetReport() {
    final MigrationReport report = Mockito.mock(MigrationReport.class);

    Mockito.when(context.getReport()).thenReturn(report);

    Assert.assertThat(entry.getReport(), Matchers.sameInstance(report));
  }

  @Test
  public void testGetId() {
    Assert.assertThat(entry.getId(), Matchers.equalTo(MIGRATABLE_ID));
  }

  // cannot test equals() or hashCode() on mocks, will test them via the ExportMigrationEntryImpl

  @Test
  public void testCompareToWhenEquals() throws Exception {
    Assert.assertThat(entry.compareTo(entry2), Matchers.equalTo(0));
  }

  @Test
  @SuppressWarnings("SelfComparison")
  public void testCompareToWhenIdentical() throws Exception {
    Assert.assertThat(entry.compareTo(entry), Matchers.equalTo(0));
  }

  @Test
  public void testCompareToWithLesserName() throws Exception {
    Mockito.when(entry2.getName()).thenReturn(ENTRY_NAME + '2');

    Assert.assertThat(entry.compareTo(entry2), Matchers.lessThan(0));
  }

  @Test
  public void testCompareToWithGreaterName() throws Exception {
    Mockito.when(entry2.getName())
        .thenReturn(StringUtils.right(ENTRY_NAME, ENTRY_NAME.length() - 1));

    Assert.assertThat(entry.compareTo(entry2), Matchers.greaterThan(0));
  }

  @Test
  public void testCompareToWithLesserId() throws Exception {
    Mockito.when(context2.getId()).thenReturn(MIGRATABLE_ID + '2');

    Assert.assertThat(entry.compareTo(entry2), Matchers.lessThan(0));
  }

  @Test
  public void testCompareToWithGreaterId() throws Exception {
    Mockito.when(context2.getId())
        .thenReturn('a' + StringUtils.right(MIGRATABLE_ID, MIGRATABLE_ID.length() - 1));

    Assert.assertThat(entry.compareTo(entry2), Matchers.greaterThan(0));
  }

  @Test
  public void testCompareToWhenIdIsNull() throws Exception {
    Mockito.when(context.getId()).thenReturn(null);

    Assert.assertThat(entry.compareTo(entry2), Matchers.lessThan(0));
  }

  @Test
  public void testCompareToWhenOtherIdIsNull() throws Exception {
    Mockito.when(context2.getId()).thenReturn(null);

    Assert.assertThat(entry.compareTo(entry2), Matchers.greaterThan(0));
  }

  @Test
  public void testCompareToWhenBothIdsAreNull() throws Exception {
    Mockito.when(context.getId()).thenReturn(null);
    Mockito.when(context2.getId()).thenReturn(null);

    Assert.assertThat(entry.compareTo(entry2), Matchers.equalTo(0));
  }

  @Test
  public void testCompareToWithOtherClass() throws Exception {
    final ExportMigrationEntryImpl entry2 = Mockito.mock(ExportMigrationEntryImpl.class);

    Mockito.when(entry2.getName()).thenReturn(ENTRY_NAME);
    Mockito.when(entry2.getContext()).thenReturn(context2);
    Mockito.when(context2.getId()).thenReturn(MIGRATABLE_ID);

    Assert.assertThat(entry.compareTo(entry2), Matchers.greaterThan(0));
  }

  @Test
  public void testCompareToWithNull() throws Exception {
    Assert.assertThat(entry.compareTo(null), Matchers.greaterThan(0));
  }

  @Test
  public void testToStringWhenEquals() throws Exception {
    Assert.assertThat(entry.toString(), Matchers.equalTo(entry2.toString()));
  }

  @Test
  public void testToStringWhenDifferent() throws Exception {
    Mockito.when(entry2.getName()).thenReturn(ENTRY_NAME + '2');

    Assert.assertThat(entry.toString(), Matchers.not(Matchers.equalTo(entry2.toString())));
  }
}
