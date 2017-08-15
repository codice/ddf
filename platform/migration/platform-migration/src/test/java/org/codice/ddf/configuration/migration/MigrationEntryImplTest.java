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
package org.codice.ddf.configuration.migration;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.migration.MigrationReport;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class MigrationEntryImplTest extends AbstractMigrationTest {
    private static final String ENTRY_NAME = Paths.get("path/path2/file.ext")
            .toString();

    private static final Path FILE_PATH = Paths.get(ENTRY_NAME);

    private static final String MIGRATABLE_ID = "test-migratable";

    private static final String MIGRATABLE_FQN = MIGRATABLE_ID + File.separatorChar + ENTRY_NAME;

    private final MigrationContextImpl CONTEXT = Mockito.mock(MigrationContextImpl.class);

    private final MigrationEntryImpl ENTRY = Mockito.mock(MigrationEntryImpl.class,
            Mockito.CALLS_REAL_METHODS);

    private final ExportMigrationContextImpl CONTEXT2 =
            Mockito.mock(ExportMigrationContextImpl.class);

    private final MigrationEntryImpl ENTRY2 = Mockito.mock(MigrationEntryImpl.class,
            Mockito.CALLS_REAL_METHODS);

    @Before
    public void before() throws Exception {
        Mockito.when(ENTRY.getName())
                .thenReturn(ENTRY_NAME);
        Mockito.when(ENTRY.getContext())
                .thenReturn(CONTEXT);
        Mockito.when(CONTEXT.getId())
                .thenReturn(MIGRATABLE_ID);

        Mockito.when(ENTRY2.getName())
                .thenReturn(ENTRY_NAME);
        Mockito.when(ENTRY2.getContext())
                .thenReturn(CONTEXT2);
        Mockito.when(CONTEXT2.getId())
                .thenReturn(MIGRATABLE_ID);
    }

    @Test
    public void testGetReport() {
        final MigrationReport REPORT = Mockito.mock(MigrationReport.class);

        Mockito.when(CONTEXT.getReport())
                .thenReturn(REPORT);

        Assert.assertThat(ENTRY.getReport(), Matchers.sameInstance(REPORT));
    }

    @Test
    public void testGetId() {
        Assert.assertThat(ENTRY.getId(), Matchers.equalTo(MIGRATABLE_ID));
    }

    // cannot test equals() or hashCode() on mocks, will test them via the ExportMigrationEntryImpl

    @Test
    public void testCompareToWhenEquals() throws Exception {
        Assert.assertThat(ENTRY.compareTo(ENTRY2), Matchers.equalTo(0));
    }

    @Test
    public void testCompareToWhenIdentical() throws Exception {
        Assert.assertThat(ENTRY.compareTo(ENTRY), Matchers.equalTo(0));
    }

    @Test
    public void testCompareToWithLesserName() throws Exception {
        Mockito.when(ENTRY2.getName())
                .thenReturn(ENTRY_NAME + '2');

        Assert.assertThat(ENTRY.compareTo(ENTRY2), Matchers.lessThan(0));
    }

    @Test
    public void testCompareToWithGreaterName() throws Exception {
        Mockito.when(ENTRY2.getName())
                .thenReturn(StringUtils.right(ENTRY_NAME, ENTRY_NAME.length() - 1));

        Assert.assertThat(ENTRY.compareTo(ENTRY2), Matchers.greaterThan(0));
    }

    @Test
    public void testCompareToWithLesserId() throws Exception {
        Mockito.when(CONTEXT2.getId())
                .thenReturn(MIGRATABLE_ID + '2');

        Assert.assertThat(ENTRY.compareTo(ENTRY2), Matchers.lessThan(0));
    }

    @Test
    public void testCompareToWithGreaterId() throws Exception {
        Mockito.when(CONTEXT2.getId())
                .thenReturn('a' + StringUtils.right(MIGRATABLE_ID, MIGRATABLE_ID.length() - 1));

        Assert.assertThat(ENTRY.compareTo(ENTRY2), Matchers.greaterThan(0));
    }

    @Test
    public void testCompareToWhenIdIsNull() throws Exception {
        Mockito.when(CONTEXT.getId())
                .thenReturn(null);

        Assert.assertThat(ENTRY.compareTo(ENTRY2), Matchers.lessThan(0));
    }

    @Test
    public void testCompareToWhenOtherIdIsNull() throws Exception {
        Mockito.when(CONTEXT2.getId())
                .thenReturn(null);

        Assert.assertThat(ENTRY.compareTo(ENTRY2), Matchers.greaterThan(0));
    }

    @Test
    public void testCompareToWhenBothIdsAreNull() throws Exception {
        Mockito.when(CONTEXT.getId())
                .thenReturn(null);
        Mockito.when(CONTEXT2.getId())
                .thenReturn(null);

        Assert.assertThat(ENTRY.compareTo(ENTRY2), Matchers.equalTo(0));
    }

    @Test
    public void testCompareToWithOtherClass() throws Exception {
        final ExportMigrationEntryImpl ENTRY2 = Mockito.mock(ExportMigrationEntryImpl.class);

        Mockito.when(ENTRY2.getName())
                .thenReturn(ENTRY_NAME);
        Mockito.when(ENTRY2.getContext())
                .thenReturn(CONTEXT2);
        Mockito.when(CONTEXT2.getId())
                .thenReturn(MIGRATABLE_ID);

        Assert.assertThat(ENTRY.compareTo(ENTRY2), Matchers.greaterThan(0));
    }

    @Test
    public void testCompareToWithNull() throws Exception {
        Assert.assertThat(ENTRY.compareTo(null), Matchers.greaterThan(0));
    }

    @Test
    public void testToStringWhenEquals() throws Exception {
        Assert.assertThat(ENTRY.toString(), Matchers.equalTo(ENTRY2.toString()));
    }

    @Test
    public void testToStringWhenDifferent() throws Exception {
        Mockito.when(ENTRY2.getName())
                .thenReturn(ENTRY_NAME + '2');

        Assert.assertThat(ENTRY.toString(), Matchers.not(Matchers.equalTo(ENTRY2.toString())));
    }
}
