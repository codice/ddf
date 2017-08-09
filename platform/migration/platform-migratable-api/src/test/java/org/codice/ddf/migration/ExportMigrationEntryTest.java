/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.migration;

import java.util.Optional;
import java.util.function.BiPredicate;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class ExportMigrationEntryTest {
    private static final String PROPERTY_NAME = "test.property";

    private final MigrationReport REPORT = Mockito.mock(MigrationReport.class);

    private final ExportMigrationEntry ENTRY = Mockito.mock(ExportMigrationEntry.class,
            Mockito.CALLS_REAL_METHODS);

    private static Answer<Optional<ExportMigrationEntry>> verifyValidatorWith(
            MigrationReport report, String value, Matcher<Boolean> matcher) {
        return new Answer<Optional<ExportMigrationEntry>>() {
            @Override
            public Optional<ExportMigrationEntry> answer(InvocationOnMock invocation)
                    throws Throwable {
                final BiPredicate<MigrationReport, String> validator =
                        (BiPredicate<MigrationReport, String>) invocation.getArgument(1);

                Assert.assertThat(validator.test(report, value), matcher);
                return Optional.empty();
            }
        };
    }

    @Test
    public void testGetPropertyReferencedEntry() throws Exception {
        Mockito.when(ENTRY.getPropertyReferencedEntry(Mockito.eq(PROPERTY_NAME),
                Mockito.any()))
                .thenReturn(Optional.empty());

        ENTRY.getPropertyReferencedEntry(PROPERTY_NAME);

        Mockito.verify(ENTRY)
                .getPropertyReferencedEntry(Mockito.eq(PROPERTY_NAME), Mockito.notNull());
    }

    @Test
    public void testGetPropertyReferencedEntryValidator() throws Exception {
        Mockito.when(ENTRY.getPropertyReferencedEntry(Mockito.eq(PROPERTY_NAME),
                Mockito.any()))
                .thenAnswer(verifyValidatorWith(REPORT,
                        "etc/security/test/txt",
                        Matchers.equalTo(true)));

        ENTRY.getPropertyReferencedEntry(PROPERTY_NAME);

        Mockito.verify(ENTRY)
                .getPropertyReferencedEntry(Mockito.eq(PROPERTY_NAME), Mockito.any());
    }

    @Test
    public void testGetPropertyReferencedEntryValidatorWithNullValue() throws Exception {
        Mockito.when(ENTRY.getPropertyReferencedEntry(Mockito.eq(PROPERTY_NAME),
                Mockito.any()))
                .thenAnswer(verifyValidatorWith(REPORT, null, Matchers.equalTo(true)));

        ENTRY.getPropertyReferencedEntry(PROPERTY_NAME);

        Mockito.verify(ENTRY)
                .getPropertyReferencedEntry(Mockito.eq(PROPERTY_NAME), Mockito.any());
    }

    @Test
    public void testGetPropertyReferencedEntryValidatoWithNulLReportr() throws Exception {
        Mockito.when(ENTRY.getPropertyReferencedEntry(Mockito.eq(PROPERTY_NAME),
                Mockito.any()))
                .thenAnswer(verifyValidatorWith(null,
                        "etc/security/test/txt",
                        Matchers.equalTo(true)));

        ENTRY.getPropertyReferencedEntry(PROPERTY_NAME);

        Mockito.verify(ENTRY)
                .getPropertyReferencedEntry(Mockito.eq(PROPERTY_NAME), Mockito.any());
    }

    @Test
    public void testGetPropertyReferencedEntryValidatorWithNullReportAndNullValue()
            throws Exception {
        Mockito.when(ENTRY.getPropertyReferencedEntry(Mockito.eq(PROPERTY_NAME),
                Mockito.any()))
                .thenAnswer(verifyValidatorWith(null, null, Matchers.equalTo(true)));

        ENTRY.getPropertyReferencedEntry(PROPERTY_NAME);

        Mockito.verify(ENTRY)
                .getPropertyReferencedEntry(Mockito.eq(PROPERTY_NAME), Mockito.any());
    }
}
