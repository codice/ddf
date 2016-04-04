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
package org.codice.ddf.catalog.migratable.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;

@PrepareForTest({MigrationFileWriter.class, File.class, FileUtils.class})
public class MigrationFileWriterTest {

    private static final int RESULT_COUNT = 5;

    private static final Path DDF_BASE_PATH = Paths.get("ddf");

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    private Path mockPath;

    private File mockFile;

    private FileOutputStream mockFileOutputStream;

    private BufferedOutputStream mockBufferedOutputStream;

    private ObjectOutputStream mockObjectOutputStream;

    private MigrationFileWriter fileWriter;

    @Before
    public void setup() throws Exception {
        fileWriter = new MigrationFileWriter();

        mockPath = mock(Path.class);
        mockFile = mock(File.class);
        mockFileOutputStream = mock(FileOutputStream.class);
        mockBufferedOutputStream = mock(BufferedOutputStream.class);
        mockObjectOutputStream = mock(ObjectOutputStream.class);

        when(mockPath.toFile()).thenReturn(mockFile);

        whenNew(FileOutputStream.class).withAnyArguments()
                .thenReturn(mockFileOutputStream);
        whenNew(BufferedOutputStream.class).withAnyArguments()
                .thenReturn(mockBufferedOutputStream);
    }

    @Test
    public void testWriteSuccess() throws Exception {
        whenNew(ObjectOutputStream.class).withAnyArguments()
                .thenReturn(mockObjectOutputStream);

        ArgumentCaptor<MetacardImpl> arg = ArgumentCaptor.forClass(MetacardImpl.class);
        final List<Result> results = loadList();
        when(mockFile.exists()).thenReturn(true);

        fileWriter.writeMetacards(mockFile, results);

        verify(mockObjectOutputStream, times(RESULT_COUNT)).writeObject(arg.capture());
        for (int i = 0; i < RESULT_COUNT; i++) {
            assertEquals(results.get(i)
                            .getMetacard()
                            .getId(),
                    arg.getAllValues()
                            .get(i)
                            .getId());
        }

        verify(mockObjectOutputStream, times(1)).flush();
    }

    @Test
    public void testWriteMakesFile() throws Exception {
        whenNew(ObjectOutputStream.class).withAnyArguments()
                .thenReturn(mockObjectOutputStream);

        final List<Result> results = loadList();
        when(mockFile.exists()).thenReturn(false);

        fileWriter.writeMetacards(mockFile, results);

        verify(mockFile).createNewFile();
    }

    @Test
    public void testStreamsClosed() throws Exception {
        whenNew(ObjectOutputStream.class).withAnyArguments()
                .thenReturn(mockObjectOutputStream);

        final List<Result> results = loadList();
        when(mockFile.exists()).thenReturn(false);

        fileWriter.writeMetacards(mockFile, results);

        verify(mockObjectOutputStream).close();
    }

    @Test(expected = IOException.class)
    public void testWriteMetacardsBadDirectory() throws Exception {
        whenNew(ObjectOutputStream.class).withAnyArguments()
                .thenReturn(mockObjectOutputStream);

        File uncreatedFile = DDF_BASE_PATH.resolve("notRealDirectory")
                .toFile();

        fileWriter.writeMetacards(uncreatedFile, Collections.emptyList());
    }

    @Test(expected = IOException.class)
    public void testWriteMetacardsObjectStreamFails() throws Exception {
        whenNew(ObjectOutputStream.class).withAnyArguments()
                .thenThrow(IOException.class);
        fileWriter = new MigrationFileWriter();
        fileWriter.writeMetacards(mockFile, Collections.emptyList());
    }

    @Test(expected = IOException.class)
    public void testWriteMetacardsWriteObjectFails() throws Exception {
        whenNew(ObjectOutputStream.class).withAnyArguments()
                .thenReturn(mockObjectOutputStream);
        doThrow(IOException.class).when(mockObjectOutputStream)
                .writeObject(anyObject());

        List<Result> results = new ArrayList<>();
        results.add(new ResultImpl(new MetacardImpl()));

        mockFile = testFolder.newFile("mockFile");
        fileWriter.writeMetacards(mockFile, results);
    }

    private List<Result> loadList() {
        final List<Result> results = new ArrayList<>();
        for (int i = 0; i < RESULT_COUNT; i++) {
            MetacardImpl metacard = new MetacardImpl();
            metacard.setId("id" + String.valueOf(i));
            results.add(new ResultImpl(metacard));
        }
        return results;
    }
}
