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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import org.codice.ddf.commands.catalog.facade.CatalogFacade;
import org.codice.ddf.commands.catalog.facade.Framework;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import ddf.catalog.CatalogFramework;
import ddf.catalog.transform.InputTransformer;

/**
 * Tests the {@link IngestCommand} output.
 */
public class TestIngestCommand extends TestAbstractCommand {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    ConsoleOutput consoleOutput;

    IngestCommand command;

    CatalogFramework catalogFramework;

    @Before
    public void setup() throws Exception {
        consoleOutput = new ConsoleOutput();
        consoleOutput.interceptSystemOut();
        catalogFramework = givenCatalogFramework(getResultList("id1", "id2"));

        command = new IngestCommand() {
            @Override
            protected CatalogFacade getCatalog() throws InterruptedException {
                return new Framework(catalogFramework);
            }

            @Override
            protected Object doExecute() throws Exception {
                return executeWithSubject();
            }

            public BundleContext getBundleContext() {
                BundleContext bundleContext = mock(BundleContext.class);
                try {
                    when(bundleContext.getServiceReferences(anyString(), anyString())).thenReturn(
                            new ServiceReference[] {mock(ServiceReference.class)});
                    InputTransformer inputTransformer = mock(InputTransformer.class);
                    when(bundleContext.getService(anyObject())).thenReturn(inputTransformer);
                } catch (InvalidSyntaxException e) {
                    //ignore
                }
                return bundleContext;
            }
        };
        command.filePath = testFolder.getRoot()
                .getAbsolutePath();
    }

    /**
     * Test empty folder
     *
     * @throws Exception
     */
    @Test
    public void testNoFiles() throws Exception {

        // when
        command.doExecute();

        // cleanup
        consoleOutput.resetSystemOut();

        // then
        try {
            String expectedIngested = "0 file(s) ingested";
            assertThat(consoleOutput.getOutput(), containsString(expectedIngested));

        } finally {
            consoleOutput.closeBuffer();
        }
    }

    /**
     * Check expected output and ingested,failed counts
     *
     * @throws Exception
     */
    @Test
    public void testExpectedCounts() throws Exception {

        testFolder.newFile("somefile1.txt");
        testFolder.newFile("somefile2.jpg");
        testFolder.newFile("somefile3.txt");
        testFolder.newFile("somefile4.jpg");
        testFolder.newFile("somefile5.txt");

        // when
        command.doExecute();

        // cleanup
        consoleOutput.resetSystemOut();

        // then
        try {
            String expectedIngested = "0 file(s) ingested";
            String expectedFailed = "5 file(s) failed";
            assertThat(consoleOutput.getOutput(), containsString(expectedIngested));
            assertThat(consoleOutput.getOutput(), containsString(expectedFailed));

        } finally {
            consoleOutput.closeBuffer();
        }
    }

    /**
     * Check expected output and ingested,ignored,failed counts for ignore command
     *
     * @throws Exception
     */
    @Test
    public void testExpectedCountsWithIgnore() throws Exception {

        testFolder.newFile("somefile1.txt");
        testFolder.newFile("somefile2.jpg");
        testFolder.newFile("somefile3.txt");
        testFolder.newFile("somefile4.jpg");
        testFolder.newFile("somefile5.txt");

        ArrayList<String> ignoreList = new ArrayList<>();
        ignoreList.add(".txt");
        command.ignoreList = ignoreList;

        // when
        command.doExecute();

        // cleanup
        consoleOutput.resetSystemOut();

        // then
        try {
            String expectedIngested = "0 file(s) ingested";
            String expectedIgnored = "3 file(s) ignored";
            String expectedFailed = "2 file(s) failed";
            assertThat(consoleOutput.getOutput(), containsString(expectedIngested));
            assertThat(consoleOutput.getOutput(), containsString(expectedFailed));
            assertThat(consoleOutput.getOutput(), containsString(expectedIgnored));

        } finally {
            consoleOutput.closeBuffer();
        }
    }

    /**
     * Check expected output for hidden files
     *
     * @throws Exception
     */
    @Test
    @Ignore
    //TODO: Use a conditional ignore from JUnit to skip this test on Windows
    public void testIgnoreHiddenFiles() throws Exception {

        testFolder.newFile(".somefile1");
        testFolder.newFile(".somefile2");
        testFolder.newFile(".somefile3");
        testFolder.newFile(".somefile4");
        testFolder.newFile("somefile5");

        // when
        command.doExecute();

        // cleanup
        consoleOutput.resetSystemOut();

        // then
        try {
            String expectedIngested = "0 file(s) ingested";
            String expectedFailed = "1 file(s) failed";

            String firstOutput = consoleOutput.getOutput();
            String secondOutput = consoleOutput.getOutput();

            assertThat(firstOutput, containsString(expectedIngested));
            assertThat(secondOutput, containsString(expectedFailed));
            assertFalse(consoleOutput.getOutput()
                    .contains("ignored"));

        } finally {
            consoleOutput.closeBuffer();
        }
    }

    @Test
    public void testIncludeContentNonZipFile() throws Exception {

        command.includeContent = true;

        // when
        command.doExecute();

        // cleanup
        consoleOutput.resetSystemOut();

        // then
        try {
            String expectedMessage = "must be a zip file";
            assertThat(consoleOutput.getOutput(), containsString(expectedMessage));
        } finally {
            consoleOutput.closeBuffer();
        }
    }
}
