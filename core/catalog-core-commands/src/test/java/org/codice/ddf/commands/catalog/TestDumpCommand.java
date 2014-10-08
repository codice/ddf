/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package org.codice.ddf.commands.catalog;

import ddf.catalog.CatalogFramework;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import org.codice.ddf.commands.catalog.facade.CatalogFacade;
import org.codice.ddf.commands.catalog.facade.Framework;
import org.fusesource.jansi.Ansi;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

/**
 * Tests the {@link DumpCommand} output.
 * 
 */
public class TestDumpCommand extends AbstractCommandTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    static final String DEFAULT_CONSOLE_COLOR = Ansi.ansi().reset().toString();

    static final String RED_CONSOLE_COLOR = Ansi.ansi().fg(Ansi.Color.RED).toString();

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
        DumpCommand command = new DumpCommand();
        command.dirPath = "nosuchdirectoryanywherehereman";

        // when
        command.doExecute();

        // cleanup
        consoleOutput.resetSystemOut();

        // then
        try {
            String message = "Directory [nosuchdirectoryanywherehereman] must exist.";
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
        DumpCommand command = new DumpCommand();
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
        final CatalogFramework catalogFramework = givenCatalogFramework(getResultList("id1", "id2"));
        DumpCommand command = new DumpCommand() {
            @Override
            protected CatalogFacade getCatalog() throws InterruptedException {
                return new Framework(catalogFramework);
            }

            @Override
            protected FilterBuilder getFilterBuilder() throws InterruptedException {
                return new GeotoolsFilterBuilder();
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

}
