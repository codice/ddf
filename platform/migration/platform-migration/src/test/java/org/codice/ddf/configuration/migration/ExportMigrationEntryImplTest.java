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

import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FilenameUtils;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

public class ExportMigrationEntryImplTest extends AbstractMigrationTest {
    private static final String[] DIRS = new String[] {"path", "path2"};

    private static final String UNIX_NAME = "path/path2/file.ext";

    private static final String WINDOWS_NAME = "path\\path2\\file.ext";

    private static final String MIXED_NAME = "path\\path2/file.ext";

    private static final Path FILE_PATH = Paths.get(UNIX_NAME);

    private static final String MIGRATABLE_ID = "test-migratable";

    private final ExportMigrationReportImpl REPORT = Mockito.mock(ExportMigrationReportImpl.class);

    private final ZipOutputStream ZOS = Mockito.mock(ZipOutputStream.class);

    private final ExportMigrationContextImpl CONTEXT =
            Mockito.mock(ExportMigrationContextImpl.class);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private Path ABSOLUTE_FILE_PATH;

    private PathUtils PATH_UTILS;

    private ExportMigrationEntryImpl ENTRY;

    @Before
    public void before() throws Exception {
        createFile(createDirectory(DIRS), "file.ext");
        PATH_UTILS = new PathUtils();
        Mockito.when(CONTEXT.getPathUtils())
                .thenReturn(PATH_UTILS);
        Mockito.when(CONTEXT.getReport())
                .thenReturn(REPORT);
        Mockito.when(CONTEXT.getId())
                .thenReturn(MIGRATABLE_ID);
        ENTRY = new ExportMigrationEntryImpl(CONTEXT, FILE_PATH);
        ABSOLUTE_FILE_PATH = DDF_HOME.resolve(UNIX_NAME)
                .toRealPath();
    }

    @Test
    public void testConstructorWithRelativePath() throws Exception {
        Assert.assertThat(ENTRY.getContext(), Matchers.sameInstance(CONTEXT));
        Assert.assertThat(ENTRY.getPath(), Matchers.equalTo(FILE_PATH));
        Assert.assertThat(ENTRY.getAbsolutePath(), Matchers.equalTo(ABSOLUTE_FILE_PATH));
        Assert.assertThat(ENTRY.getFile(), Matchers.equalTo(ABSOLUTE_FILE_PATH.toFile()));
        Assert.assertThat(ENTRY.getName(), Matchers.equalTo(UNIX_NAME));
    }

    @Test
    public void testConstructorWithAbsolutePathUnderDDFHome() throws Exception {
        final ExportMigrationEntryImpl ENTRY = new ExportMigrationEntryImpl(CONTEXT,
                ABSOLUTE_FILE_PATH);

        Assert.assertThat(ENTRY.getContext(), Matchers.sameInstance(CONTEXT));
        Assert.assertThat(ENTRY.getPath(), Matchers.equalTo(FILE_PATH));
        Assert.assertThat(ENTRY.getAbsolutePath(), Matchers.equalTo(ABSOLUTE_FILE_PATH));
        Assert.assertThat(ENTRY.getFile(), Matchers.equalTo(ABSOLUTE_FILE_PATH.toFile()));
        Assert.assertThat(ENTRY.getName(), Matchers.equalTo(UNIX_NAME));
    }

    @Test
    public void testConstructorWithAbsolutePathNotUnderDDFHome() throws Exception {
        final Path ABSOLUTE_FILE_PATH = createFile(ROOT, "test.ext");
        final String ABSOLUTE_FILE_NAME =
                FilenameUtils.separatorsToUnix(ABSOLUTE_FILE_PATH.toString());

        final ExportMigrationEntryImpl ENTRY = new ExportMigrationEntryImpl(CONTEXT,
                ABSOLUTE_FILE_PATH);

        Assert.assertThat(ENTRY.getContext(), Matchers.sameInstance(CONTEXT));
        Assert.assertThat(ENTRY.getPath(), Matchers.equalTo(ABSOLUTE_FILE_PATH));
        Assert.assertThat(ENTRY.getAbsolutePath(), Matchers.equalTo(ABSOLUTE_FILE_PATH));
        Assert.assertThat(ENTRY.getFile(), Matchers.equalTo(ABSOLUTE_FILE_PATH.toFile()));
        Assert.assertThat(ENTRY.getName(), Matchers.equalTo(ABSOLUTE_FILE_NAME));
    }

    @Test
    public void testConstructorWithNullContext() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null context"));

        new ExportMigrationEntryImpl(null, FILE_PATH);
    }

    @Test
    public void testConstructorWithNullPath() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage(Matchers.containsString("null path"));

        new ExportMigrationEntryImpl(CONTEXT, null);
    }

    @Test
    public void testGetReport() throws Exception {
        Assert.assertThat(CONTEXT.getReport(), Matchers.sameInstance(REPORT));
    }

    //@Test
    public void testOutputStream() throws Exception {
        final OutputStream OS = Mockito.mock(OutputStream.class);

        Mockito.when(CONTEXT.getOutputStreamFor(Mockito.any())).thenReturn(OS);

        final OutputStream os = ENTRY.getOutputStream();

        Assert.assertThat(os, Matchers.sameInstance(OS));

        //Mockito.verify(CONTEXT).getOutputStreamFor()
    }
}
