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

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import ddf.catalog.data.Result;
import ddf.catalog.data.impl.ResultImpl;

@RunWith(MockitoJUnitRunner.class)
public class MigrationTaskManagerTest {

    /**
     * The following values are arbitrary to some degree but must remain consistent
     * throughout the test.
     * <p>
     * TEST_GROUP_COUNT: Independent of the other values and strictly for file naming
     * TEST_FILE_SIZE: Number of metacards to be written per file
     * TEST_RESULTS_LIST_SIZE: Size of the list we're passing as if it came from a catalog query.
     * Note that the math yields 3 total files (i.e. 7/3 = 2 full files + 1 for remainder).
     * This is an important relationship for the integrity of the test.
     * TEST_FUTURES_LIST_SIZE: Independent, arbitrary number of futures to populate the list with.
     */

    private static final long TEST_GROUP_COUNT = 12;

    private static final int TEST_FILE_SIZE = 3;

    private static final int TEST_RESULTS_LIST_SIZE = 7;

    private static final int TEST_FUTURES_LIST_SIZE = 4;

    @Captor
    private ArgumentCaptor<File> argFileCaptor;

    @Captor
    private ArgumentCaptor<List<Result>> argResultCaptor;

    @Mock
    private MigrationFileWriter mockFileWriter;

    @Mock
    private ListeningExecutorService mockExecutor;

    private MigrationTaskManager taskManager;

    private CatalogMigratableConfig config;

    /**
     * For testing, the export path can be arbitrary. Our {@link CatalogMigratableConfig}
     * object is real and initilized to defaults. The {@link MigrationTaskManager} is the class
     * under test and is loaded with a REAL list of MOCK future objects, otherwise we will get
     * a null pointer exception in the foreach loop of exportFinish().
     * <p>
     * The exception to this rule is the final test method where we examine exportMetacardQuery()
     * and absolutely require the whole list to be a MOCK.
     *
     * @throws Exception
     */
    @Before
    public void setup() throws Exception {
        config = new CatalogMigratableConfig();
        config.setExportPath(Paths.get("folderA", "folderB", "testFile"));
        taskManager = new MigrationTaskManager(config, mockFileWriter, () -> mockExecutor);
    }

    @Test
    public void testExportFinishedGracefulShutdown() throws Exception {
        when(mockExecutor.awaitTermination(1L, TimeUnit.MINUTES)).thenReturn(true);

        taskManager.close();

        verify(mockExecutor).shutdown();
        verify(mockExecutor, never()).shutdownNow();
        verify(mockExecutor).awaitTermination(anyLong(), any(TimeUnit.class));
    }

    @Test
    public void testExportFinishedInterruptedShutdown() throws Exception {
        when(mockExecutor.awaitTermination(1L,
                TimeUnit.MINUTES)).thenThrow(InterruptedException.class);

        taskManager.close();

        verify(mockExecutor).shutdown();
        verify(mockExecutor).shutdownNow();
    }

    @Test
    public void testExportFinishedTimeoutShutdown() throws Exception {
        when(mockExecutor.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(false);

        taskManager.close();

        verify(mockExecutor).shutdown();
        verify(mockExecutor).shutdownNow();
    }

    @Test
    public void testExportMetacardQuerySubmit() throws Exception {
        final int expectedCallsToWriteMetacards = 3;
        List<File> testFiles = generateTestFiles();
        List<Result> testResults = generateProvidedResults();
        List<List<Result>> expectedResults = generateExpectedResults(testResults);

        when(mockExecutor.submit(any(Callable.class))).thenAnswer(invocationOnMock -> {
            Callable<Void> arg = (Callable<Void>) invocationOnMock.getArguments()[0];
            arg.call();
            return mock(ListenableFuture.class);
        });

        config.setExportCardsPerFile(TEST_FILE_SIZE);
        taskManager.exportMetacardQuery(testResults, TEST_GROUP_COUNT);

        verify(mockFileWriter,
                times(expectedCallsToWriteMetacards)).writeMetacards(argFileCaptor.capture(),
                argResultCaptor.capture());

        List<File> capturedFiles = argFileCaptor.getAllValues();
        assertThat(testFiles, containsInAnyOrder(capturedFiles.toArray()));

        try {
            List<List<Result>> actualResults = argResultCaptor.getAllValues();
            for (int i = 0; i < expectedResults.size(); i++) {
                assertThat(expectedResults.get(i),
                        containsInAnyOrder(actualResults.get(i)
                                .toArray()));
            }
        } catch (NullPointerException e) {
            fail("Passed arguments do not match as expected.");
        }

        int fileCount = TEST_RESULTS_LIST_SIZE / TEST_FILE_SIZE;
        if (TEST_FUTURES_LIST_SIZE % TEST_FILE_SIZE > 0) {
            fileCount++;
        }

        verify(mockExecutor, times(fileCount)).submit(any(Callable.class));
    }

    @Test
    public void testExportMetacardQueryEmptyList() throws Exception {
        List<Result> testResults = new ArrayList<>();
        config.setExportCardsPerFile(TEST_FILE_SIZE);
        taskManager.exportMetacardQuery(testResults, TEST_GROUP_COUNT);
        verify(mockExecutor, never()).submit(any(Callable.class));
    }

    private List<File> generateTestFiles() {
        List<File> testFiles = new ArrayList<>();
        testFiles.add(config.getExportPath()
                .resolve("catalogExport_12_0")
                .toFile());
        testFiles.add(config.getExportPath()
                .resolve("catalogExport_12_3")
                .toFile());
        testFiles.add(config.getExportPath()
                .resolve("catalogExport_12_6")
                .toFile());
        return testFiles;
    }

    /**
     * We need to generate the original list of results. We feed this to exportMetacardQuery() in
     * the {@link MigrationTaskManager}.
     *
     * @return Our list of results.
     */
    private List<Result> generateProvidedResults() {
        List<Result> testResults = new ArrayList<>();
        for (int i = 0; i < TEST_RESULTS_LIST_SIZE; i++) {
            testResults.add(new ResultImpl());
        }
        return testResults;
    }

    /**
     * We need to generate the partitioned list that we expect. This uses sublists.
     *
     * @return Our list of lists.
     */
    private List<List<Result>> generateExpectedResults(List<Result> originalResults) {
        List<List<Result>> expectedResults = new ArrayList<>();
        for (int i = 0; i < originalResults.size(); i += TEST_FILE_SIZE) {
            expectedResults.add(originalResults.subList(i,
                    Math.min((i + TEST_FILE_SIZE), originalResults.size())));
        }
        return expectedResults;
    }
}
