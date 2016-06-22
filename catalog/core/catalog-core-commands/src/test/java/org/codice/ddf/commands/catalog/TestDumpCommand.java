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
package org.codice.ddf.commands.catalog;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URI;
import java.util.List;

import org.codice.ddf.commands.catalog.facade.CatalogFacade;
import org.codice.ddf.commands.catalog.facade.Framework;
import org.fusesource.jansi.Ansi;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.common.io.ByteSource;
import com.google.common.io.Files;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.impl.ResourceImpl;

/**
 * Tests the {@link DumpCommand} output.
 */
public class TestDumpCommand extends TestAbstractCommand {

    private static final String CONTENT_FILENAME = "content.txt";

    private static final String CONTENT_PATH = TestDumpCommand.class.getResource(
            "/" + CONTENT_FILENAME)
            .getPath();

    static final String DEFAULT_CONSOLE_COLOR = Ansi.ansi()
            .reset()
            .toString();

    static final String RED_CONSOLE_COLOR = Ansi.ansi()
            .fg(Ansi.Color.RED)
            .toString();

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    /**
     * Check for bad output directory.
     *
     * @throws Exception
     */
    @Test
    public void testNonExistentOutputDirectory() throws Exception {

        ConsoleOutput consoleOutput = new ConsoleOutput();
        consoleOutput.interceptSystemOut();

        // given
        DumpCommand command = new DumpCommand() {
            @Override
            protected Object doExecute() throws Exception {
                return executeWithSubject();
            }
        };
        command.dirPath = "nosuchdirectoryanywherehereman";

        // when
        command.doExecute();

        // cleanup
        consoleOutput.resetSystemOut();

        // then
        try {
            String message = "Directory [nosuchdirectoryanywherehereman/] must exist.";
            String expectedPrintOut = RED_CONSOLE_COLOR + message + DEFAULT_CONSOLE_COLOR;
            assertThat(consoleOutput.getOutput(), startsWith(expectedPrintOut));

        } finally {
            consoleOutput.closeBuffer();
        }
    }

    /**
     * Check for output directory is really a file.
     *
     * @throws Exception
     */
    @Test
    public void testOutputDirectoryIsFile() throws Exception {

        ConsoleOutput consoleOutput = new ConsoleOutput();
        consoleOutput.interceptSystemOut();

        // given
        DumpCommand command = new DumpCommand() {
            @Override
            protected Object doExecute() throws Exception {
                return executeWithSubject();
            }
        };
        File testFile = testFolder.newFile("somefile.txt");
        String testFilePath = testFile.getAbsolutePath();
        command.dirPath = testFilePath;

        // when
        command.doExecute();

        // cleanup
        consoleOutput.resetSystemOut();

        // then
        try {
            String message = "Path [" + testFilePath + "] must be a directory.";
            String expectedPrintOut = RED_CONSOLE_COLOR + message + DEFAULT_CONSOLE_COLOR;
            assertThat(consoleOutput.getOutput(), startsWith(expectedPrintOut));

        } finally {
            testFile.delete();
            consoleOutput.closeBuffer();
        }
    }

    /**
     * Check for normal operation
     *
     * @throws Exception
     */
    @Test
    public void testNormalOperation() throws Exception {

        ConsoleOutput consoleOutput = new ConsoleOutput();
        consoleOutput.interceptSystemOut();

        // given
        final CatalogFramework catalogFramework = givenCatalogFramework(getResultList("id1",
                "id2"));
        DumpCommand command = new DumpCommand() {
            @Override
            protected CatalogFacade getCatalog() throws InterruptedException {
                return new Framework(catalogFramework);
            }

            @Override
            protected FilterBuilder getFilterBuilder() throws InterruptedException {
                return new GeotoolsFilterBuilder();
            }

            @Override
            protected Object doExecute() throws Exception {
                return executeWithSubject();
            }
        };
        File outputDirectory = testFolder.newFolder("somedirectory");
        String outputDirectoryPath = outputDirectory.getAbsolutePath();
        command.dirPath = outputDirectoryPath;

        // when
        command.doExecute();

        // cleanup
        consoleOutput.resetSystemOut();

        // then
        try {
            assertThat(consoleOutput.getOutput(), containsString(" 2 file(s) dumped in "));
        } finally {
            consoleOutput.closeBuffer();
        }
    }

    /**
     * Check for normal operation without any files
     *
     * @throws Exception
     */
    @Test
    public void testNormalOperationNoFiles() throws Exception {

        ConsoleOutput consoleOutput = new ConsoleOutput();
        consoleOutput.interceptSystemOut();

        // given
        final CatalogFramework catalogFramework = givenCatalogFramework(getEmptyResultList());
        DumpCommand command = new DumpCommand() {
            @Override
            protected CatalogFacade getCatalog() throws InterruptedException {
                return new Framework(catalogFramework);
            }

            @Override
            protected FilterBuilder getFilterBuilder() throws InterruptedException {
                return new GeotoolsFilterBuilder();
            }

            @Override
            protected Object doExecute() throws Exception {
                return executeWithSubject();
            }
        };
        File outputDirectory = testFolder.newFolder("somedirectory");
        String outputDirectoryPath = outputDirectory.getAbsolutePath();
        command.dirPath = outputDirectoryPath;

        // when
        command.doExecute();

        // cleanup
        consoleOutput.resetSystemOut();

        // then
        try {
            String expectedPrintOut = " 0 file(s) dumped in ";
            assertThat(consoleOutput.getOutput(), startsWith(expectedPrintOut));

        } finally {
            consoleOutput.closeBuffer();
        }
    }

    /**
     * Check for normal operation when there is local content associated with metacards
     *
     * @throws Exception
     */
    @Test
    public void testNormalOperationWithContent() throws Exception {

        ResourceResponse resourceResponse = mock(ResourceResponse.class);
        File file = new File(CONTENT_PATH);
        ByteSource byteSource = Files.asByteSource(file);

        ConsoleOutput consoleOutput = new ConsoleOutput();
        consoleOutput.interceptSystemOut();

        // given
        List<Result> resultList = getResultList("id1", "id2");
        MetacardImpl metacard1 = new MetacardImpl(resultList.get(0)
                .getMetacard());
        MetacardImpl metacard2 = new MetacardImpl(resultList.get(1)
                .getMetacard());
        metacard1.setResourceURI(new URI("content:" + metacard1.getId()));

        metacard2.setResourceURI(new URI("content:" + metacard2.getId() + "#preview"));

        Resource resource = new ResourceImpl(byteSource.openStream(), CONTENT_FILENAME);

        when(resourceResponse.getResource()).thenReturn(resource);

        // given
        final CatalogFramework catalogFramework = givenCatalogFramework(resultList);

        DumpCommand command = new DumpCommand() {
            @Override
            protected CatalogFacade getCatalog() throws InterruptedException {
                return new Framework(catalogFramework);
            }

            @Override
            protected FilterBuilder getFilterBuilder() throws InterruptedException {
                return new GeotoolsFilterBuilder();
            }

            @Override
            protected Object doExecute() throws Exception {
                return executeWithSubject();
            }
        };
        File outputDirectory = testFolder.newFolder("somedirectory");
        String outputDirectoryPath = outputDirectory.getAbsolutePath();
        command.dirPath = outputDirectoryPath;

        // when
        command.doExecute();

        // cleanup
        consoleOutput.resetSystemOut();

        // then
        try {
            assertThat(consoleOutput.getOutput(), containsString(" 2 file(s) dumped in "));
        } finally {
            consoleOutput.closeBuffer();
        }
    }

}
