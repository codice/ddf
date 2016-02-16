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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.codice.ddf.migration.MigrationException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
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

    @Mock
    private MigrationFileWriter mockFileWriter;

    @Mock
    private ListeningExecutorService mockExecutor;

    private FutureCallback<Void> mockFutureCallback;

    @Mock
    private ListenableFuture<Void> mockFutureInstance;

    @Mock
    private AtomicReference<Throwable> mockAtomicRef;

    @Mock
    private Throwable mockThrowable;

    private InspectableMigrationTaskManager taskManager;

    private CatalogMigratableConfig config;

    private Path exportPath;

    /**
     * For testing, the export {@link Path} can be arbitrary. Our {@link CatalogMigratableConfig}
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
        exportPath = Paths.get("folderA", "folderB", "testFile");
        config = new CatalogMigratableConfig();
        config.setExportPath(exportPath);
        taskManager = new InspectableMigrationTaskManager();
        mockFutureCallback = null;
    }

    @Test
    public void testExportSetupInitGetsCalled() throws Exception {
        verify(mockFileWriter).init(exportPath);
    }

    @Test
    public void testExportFinishedGracefulShutdown() throws Exception {
        when(mockExecutor.awaitTermination(1L, TimeUnit.MINUTES)).thenReturn(true);

        taskManager.exportFinish();

        verify(mockExecutor).shutdown();
        verify(mockExecutor, never()).shutdownNow();
        verify(mockExecutor).awaitTermination(anyLong(), any(TimeUnit.class));
    }

    @Test
    public void testExportFinishedInterruptedShutdown() throws Exception {
        when(mockExecutor.awaitTermination(1L,
                TimeUnit.MINUTES)).thenThrow(InterruptedException.class);

        taskManager.exportFinish();

        verify(mockExecutor).shutdown();
        verify(mockExecutor).shutdownNow();
    }

    @Test
    public void testExportFinishedTimeoutShutdown() throws Exception {
        when(mockExecutor.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(false);

        taskManager.exportFinish();

        verify(mockExecutor).shutdown();
        verify(mockExecutor).shutdownNow();
    }

    @Test(expected = MigrationException.class)
    public void testExportFinishedErrorOccurred() throws Exception {
        try {
            taskManager.getAtomicReference()
                    .set(mockThrowable);
            taskManager.exportFinish();
        } catch (MigrationException e) {
            assertThat(e.getCause(), is(mockThrowable));
            throw e;
        }
    }

    @Test(expected = MigrationException.class)
    public void testExportMetacardQueryWithError() throws Exception {
        List<Result> testResults = new ArrayList<>();
        for (int i = 0; i < TEST_RESULTS_LIST_SIZE; i++) {
            testResults.add(new ResultImpl());
        }

        when(mockExecutor.submit(any(CatalogWriterCallable.class))).thenReturn(mockFutureInstance);

        taskManager.getAtomicReference()
                .set(mockThrowable);
        taskManager.exportMetacardQuery(testResults, TEST_GROUP_COUNT);
    }

    @Test
    public void testExportMetacardQuerySubmit() throws Exception {
        List<Result> testResults = new ArrayList<>();
        for (int i = 0; i < TEST_RESULTS_LIST_SIZE; i++) {
            testResults.add(new ResultImpl());
        }

        config.setExportCardsPerFile(TEST_FILE_SIZE);

        List<CatalogWriterCallable> writerCallableList = new ArrayList<>();

        when(mockExecutor.submit(any(CatalogWriterCallable.class))).thenReturn(mockFutureInstance);

        for (int i = 0; i < testResults.size(); i += config.getExportCardsPerFile()) {
            File expectedFile = config.getExportPath()
                    .resolve(taskManager.makeFileName(TEST_GROUP_COUNT, i))
                    .toFile();
            List<Result> expectedResults = testResults.subList(i,
                    Math.min((i + config.getExportCardsPerFile()), testResults.size()));
            CatalogWriterCallable writerCallable = new CatalogWriterCallable(expectedFile,
                    expectedResults,
                    mockFileWriter);
            writerCallableList.add(writerCallable);
        }

        taskManager.exportMetacardQuery(testResults, TEST_GROUP_COUNT);

        assertThat(taskManager.getCallables(), containsInAnyOrder(writerCallableList.toArray()));

        int fileCount = TEST_RESULTS_LIST_SIZE / TEST_FILE_SIZE;
        if (TEST_FUTURES_LIST_SIZE % TEST_FILE_SIZE > 0) {
            fileCount++;
        }

        verify(mockExecutor, times(fileCount)).submit(any(CatalogWriterCallable.class));
    }

    @Test
    public void testExportMetacardQueryEmptyList() throws Exception {
        List<Result> testResults = new ArrayList<>();
        config.setExportCardsPerFile(TEST_FILE_SIZE);
        taskManager.exportMetacardQuery(testResults, TEST_GROUP_COUNT);
        verify(mockExecutor, never()).submit(any(CatalogWriterCallable.class));
    }

    @Test
    public void testExportMetacardQueryRegistersCallback() throws Exception {
        when(mockExecutor.submit(any(CatalogWriterCallable.class))).thenReturn(mockFutureInstance);

        List<Result> testResults = new ArrayList<>();
        testResults.add(new ResultImpl(new MetacardImpl()));
        config.setExportCardsPerFile(TEST_FILE_SIZE);
        taskManager.exportMetacardQuery(testResults, TEST_GROUP_COUNT);

        assertThat(mockFutureCallback, notNullValue());
    }

    @Test(expected = MigrationException.class)
    public void testCtorIfFileWriterInitFails() throws Exception {
        doThrow(MigrationException.class).when(mockFileWriter)
                .init(any(Path.class));
        MigrationTaskManager tempManager = new MigrationTaskManager(new CatalogMigratableConfig(),
                mockFileWriter,
                mockExecutor);
    }

    /**
     * Extension of {@link MigrationTaskManager} to provide exposed test data.
     * <p>
     * This class is not for production use and cannot be inherited.
     */
    private final class InspectableMigrationTaskManager extends MigrationTaskManager {
        private List<CatalogWriterCallable> callables;

        public InspectableMigrationTaskManager() {
            super(MigrationTaskManagerTest.this.config,
                    MigrationTaskManagerTest.this.mockFileWriter,
                    MigrationTaskManagerTest.this.mockExecutor);
            callables = new ArrayList<>();
        }

        public List<CatalogWriterCallable> getCallables() {
            return callables;
        }

        @Override
        AtomicReference<Throwable> createAtomicReference() {
            return mockAtomicRef;
        }

        @Override
        FutureCallback<Void> createFutureCallback() {
            mockFutureCallback = mock(FutureCallback.class);
            return mockFutureCallback;
        }

        @Override
        CatalogWriterCallable createWriterCallable(final File exportFile,
                final List<Result> fileResults, final MigrationFileWriter fileWriter) {
            CatalogWriterCallable callable = new CatalogWriterCallable(exportFile,
                    fileResults,
                    fileWriter);
            callables.add(callable);
            return callable;
        }
    }
}
