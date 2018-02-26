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
package org.codice.ddf.migration;

import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.stream.Stream;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class ExportMigrationContextTest {
  private static final String PROPERTY_NAME = "test.property";

  private final MigrationReport report = Mockito.mock(MigrationReport.class);

  // This will create a mock of the interface to test method delegation later
  private final ExportMigrationContext context =
      Mockito.mock(ExportMigrationContext.class, Mockito.CALLS_REAL_METHODS);

  private static Answer<Optional<ExportMigrationEntry>> verifyValidatorWith(
      MigrationReport report, String value, Matcher<Boolean> matcher) {
    return new Answer<Optional<ExportMigrationEntry>>() {
      @Override
      public Optional<ExportMigrationEntry> answer(InvocationOnMock invocation) throws Throwable {
        final BiPredicate<MigrationReport, String> validator =
            (BiPredicate<MigrationReport, String>) invocation.getArgument(1);

        Assert.assertThat(validator.test(report, value), matcher);
        return Optional.empty();
      }
    };
  }

  @Test
  public void testGetSystemPropertyReferencedEntry() throws Exception {
    Mockito.when(context.getSystemPropertyReferencedEntry(Mockito.eq(PROPERTY_NAME), Mockito.any()))
        .thenReturn(Optional.empty());

    context.getSystemPropertyReferencedEntry(PROPERTY_NAME);

    Mockito.verify(context)
        .getSystemPropertyReferencedEntry(Mockito.eq(PROPERTY_NAME), Mockito.notNull());
  }

  @Test
  public void testGetSystemPropertyReferencedEntryValidator() throws Exception {
    Mockito.when(context.getSystemPropertyReferencedEntry(Mockito.eq(PROPERTY_NAME), Mockito.any()))
        .thenAnswer(verifyValidatorWith(report, "etc/security/test/txt", Matchers.equalTo(true)));

    context.getSystemPropertyReferencedEntry(PROPERTY_NAME);

    Mockito.verify(context)
        .getSystemPropertyReferencedEntry(Mockito.eq(PROPERTY_NAME), Mockito.any());
  }

  @Test
  public void testGetSystemPropertyReferencedEntryValidatorWithNullValue() throws Exception {
    Mockito.when(context.getSystemPropertyReferencedEntry(Mockito.eq(PROPERTY_NAME), Mockito.any()))
        .thenAnswer(verifyValidatorWith(report, null, Matchers.equalTo(true)));

    context.getSystemPropertyReferencedEntry(PROPERTY_NAME);

    Mockito.verify(context)
        .getSystemPropertyReferencedEntry(Mockito.eq(PROPERTY_NAME), Mockito.any());
  }

  @Test
  public void testGetSystemPropertyReferencedEntryValidatorWithNullReport() throws Exception {
    Mockito.when(context.getSystemPropertyReferencedEntry(Mockito.eq(PROPERTY_NAME), Mockito.any()))
        .thenAnswer(verifyValidatorWith(null, "etc/security/test/txt", Matchers.equalTo(true)));

    context.getSystemPropertyReferencedEntry(PROPERTY_NAME);

    Mockito.verify(context)
        .getSystemPropertyReferencedEntry(Mockito.eq(PROPERTY_NAME), Mockito.any());
  }

  @Test
  public void testGetSystemPropertyReferencedEntryValidatorWithNullReportAndNullValue()
      throws Exception {
    Mockito.when(context.getSystemPropertyReferencedEntry(Mockito.eq(PROPERTY_NAME), Mockito.any()))
        .thenAnswer(verifyValidatorWith(null, null, Matchers.equalTo(true)));

    context.getSystemPropertyReferencedEntry(PROPERTY_NAME);

    Mockito.verify(context)
        .getSystemPropertyReferencedEntry(Mockito.eq(PROPERTY_NAME), Mockito.any());
  }

  @Test
  public void testEntries() throws Exception {
    final Path path = Mockito.mock(Path.class);
    final Stream stream = Mockito.mock(Stream.class);

    Mockito.when(context.entries(Mockito.any(), Mockito.anyBoolean())).thenReturn(stream);

    Assert.assertThat(context.entries(path), Matchers.sameInstance(stream));

    Mockito.verify(context).entries(path, true);
  }

  @Test
  public void testEntriesWithFilter() throws Exception {
    final PathMatcher filter = Mockito.mock(PathMatcher.class);
    final Path path = Mockito.mock(Path.class);
    final Stream stream = Mockito.mock(Stream.class);

    Mockito.when(context.entries(Mockito.any(), Mockito.anyBoolean(), Mockito.any()))
        .thenReturn(stream);

    Assert.assertThat(context.entries(path, filter), Matchers.sameInstance(stream));

    Mockito.verify(context).entries(path, true, filter);
  }
}
