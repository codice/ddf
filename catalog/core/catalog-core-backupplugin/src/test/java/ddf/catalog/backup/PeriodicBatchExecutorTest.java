/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/

package ddf.catalog.backup;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.Test;

public class PeriodicBatchExecutorTest {

    @Test
    public void executeWhenTankIsFull() throws InterruptedException, ExecutionException {

        int batchSize = 2;
        PeriodicBatchExecutor<Integer> executor = new PeriodicBatchExecutor<>(batchSize,
                Long.MAX_VALUE,
                TimeUnit.SECONDS);

        verifyResults(runTest(executor, getSampleItems(), batchSize), getSampleItems(), batchSize);
        assertThat(executor.getFutures(), is(empty()));

    }

    private List<Integer> getSampleItems() {
        List<Integer> sampleItems = new ArrayList<>();
        sampleItems.add(42);
        sampleItems.add(64);
        sampleItems.add(128);

        return sampleItems;
    }

    @Test
    public void executePeriodically() {
        int batchSize = Integer.MAX_VALUE;
        PeriodicBatchExecutor<Integer> executor = new PeriodicBatchExecutor<>(batchSize,
                1,
                TimeUnit.MILLISECONDS);

        verifyResults(runTest(executor, getSampleItems(), batchSize), getSampleItems(), batchSize);
    }

    @Test(expected = java.util.concurrent.RejectedExecutionException.class)
    public void shutdown() {
        int batchSize = 2;
        PeriodicBatchExecutor<Integer> executor = new PeriodicBatchExecutor<>(batchSize,
                Long.MAX_VALUE,
                TimeUnit.SECONDS);

        List<List<Integer>> batches = runTest(executor, getSampleItems(), batchSize);
        executor.shutdown();

        //Should throw exception.
        executor.addAll(getSampleItems());

    }

    private List<List<Integer>> runTest(PeriodicBatchExecutor<Integer> executor,
            List<Integer> inputItems, int batchSize) {

        int expectedNumberOfBatches = getExpectedNumberOfBatches(inputItems, batchSize);

        List<List<Integer>> batches = new ArrayList<>();
        executor.execute(batches::add);

        executor.addAll(inputItems);

        //Wait for all tasks to be submitted and each task to finish.
        BlockingQueue<Future> futures = executor.getFutures();

        for (int i = 0; i < expectedNumberOfBatches; ++i) {
            try {
                futures.take()
                        .get();
            } catch (ExecutionException | InterruptedException e) {
                fail();
            }
        }

        return batches;
    }

    private void verifyResults(List<List<Integer>> outputBatches, List<Integer> inputItems,
            int batchSize) {

        //VERIFY BATCH SIZES
        // Hamcrest containsInAnyOrder() ensures one collection does not have too many or too few elements.
        // Hamcrest is picky here. Left-hand-side must be Collection of Integers and right-hand-side must be
        // primitive array of Integers. Also, do not mix Integer and int.
        List<Integer> actualBatchSizes = sizeOfSublists(outputBatches);
        List<Integer> expectedBatchSizes = expectedBatchSizes(inputItems, batchSize);
        Integer[] expectedBatchSizesArray =
                expectedBatchSizes.toArray(new Integer[expectedBatchSizes.size()]);
        assertThat(actualBatchSizes, containsInAnyOrder(expectedBatchSizesArray));

        //VERIFY OUTPUT MATCHES INPUT
        Integer[] inputArray = inputItems.toArray(new Integer[inputItems.size()]);
        List<Integer> outputItems = flatten(outputBatches);
        assertThat(outputItems, containsInAnyOrder(inputArray));
    }

    private <T> List<T> flatten(List<List<T>> lists) {
        return lists.stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private <T> int getExpectedNumberOfBatches(List<T> inputItems, int batchSize) {

        int itemCount = inputItems.size();
        int batchCount = itemCount / batchSize;
        if (itemCount % batchSize != 0) {
            batchCount++;
        }
        return batchCount;
    }

    private <T> List<Integer> expectedBatchSizes(List<T> input, int batchSize) {

        return sizeOfSublists(com.google.common.collect.Lists.partition(input, batchSize));

    }

    private <T> List<Integer> sizeOfSublists(List<List<T>> lists) {

        return lists.stream()
                .map(List::size)
                .collect(Collectors.toList());
    }

}
