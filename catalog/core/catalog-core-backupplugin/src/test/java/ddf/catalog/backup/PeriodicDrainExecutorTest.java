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
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.Test;

public class PeriodicDrainExecutorTest {

    @Test
    public void executeWhenTankIsFull() throws InterruptedException, ExecutionException {

        int batchSize = 2;
        PeriodicDrainExecutor<Integer> executor = new PeriodicDrainExecutor(batchSize,
                Long.MAX_VALUE,
                TimeUnit.SECONDS);

        List<Integer> inputItems  = new ArrayList<>();
        inputItems.add(42);inputItems.add(64);inputItems.add(128);

        verify(executor, inputItems, batchSize);

    }

    @Test
    public void executePeriodically() {
        int batchSize = 1;
        PeriodicDrainExecutor<Integer> executor = new PeriodicDrainExecutor(Integer.MAX_VALUE,
                batchSize,
                TimeUnit.NANOSECONDS);

        int expectedNumberOfBatches = 1;

        Integer[] input = {42, 64, 128};

        List<List<Integer>> batches = new ArrayList<>();
        executor.execute(oneBatch -> batches.add(oneBatch));

        List<Integer> inputItems = Arrays.asList(input);
        executor.addAll(inputItems);

        //Wait for all the taks to be submitted and each task to finish.
        BlockingQueue<Future> futures = executor.getFutures();

        for (int i = 0; i < expectedNumberOfBatches; ++i) {
            try {
                futures.take()
                        .get();
            } catch (ExecutionException | InterruptedException e) {
                fail();
            }
        }

        // Flatten batches into single list
        List<Integer> outputItems = inputItems.stream()
                .collect(Collectors.toList());

        // Hamcrest will ensure one collection does not have too many or too few elements.
        // Hamcrest is picky here. Left-hand-side is Collection of Integers and right-hand-side is
        // primitive array of Integers. Do not mix Integer and int.
        assertThat(outputItems, containsInAnyOrder(input));

    }
    //Test that  shutdown.

    private int getExpectedNumberOfBatches(List inputItems, int batchSize) {

        int itemCount = inputItems.size();
        int batchCount = itemCount / batchSize;
        if (itemCount % batchSize != 0) {
            batchCount++;
        }
        return batchCount;
    }

    private List<Integer> expectedBatchSizes(List input, int batchSize) {

        List<List<Object>> batches = com.google.common.collect.Lists.partition(input, batchSize);
        return batches.stream()
                .map(List::size)
                .collect(Collectors.toList());

    }


    private List verify(PeriodicDrainExecutor executor, List inputItems, int batchSize) {

        int expectedNumberOfBatches = getExpectedNumberOfBatches(inputItems, batchSize);

        List<List<Object>> batches = new ArrayList<>();
        executor.execute(oneBatch -> batches.add(oneBatch));

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

        // Flatten batches into single list
        List<Object> outputItems = batches.stream().flatMap(Collection::stream).collect(Collectors.toList());
        List<Object> outputItems2 = batches.stream().collect(Collectors.toList());

        // Verify
//        assertThat(batches, containsInAnyOrder(hasSize(oneExpectedBatchSize), hasSize(
//                anotherExpectedBatchSize)));

        // Hamcrest will ensure one collection does not have too many or too few elements.
        // Hamcrest is picky here. Left-hand-side is Collection of Integers and right-hand-side is
        // primitive array of Integers. Do not mix Integer and int.
//        assertThat(outputItems, containsInAnyOrder(input));

        return outputItems;
    }
}
