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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class PeriodicDrainExecutor<T> {

    List<T> items = new CopyOnWriteArrayList<>();

    private ExecutorService executor;

    private ScheduledExecutorService scheduledExecutor;

    private Consumer<List<T>> task;

    private int chunkSize;

    private long period;

    private TimeUnit timeUnit;

    private BlockingQueue<Future<?>> futures;

    public PeriodicDrainExecutor(int chunkSize, long period, TimeUnit timeUnit) {
        if (chunkSize < 1) {
            throw new IllegalArgumentException("Parameter chunkSize must be greater than zero");
        }
        if (period < 1) {
            throw new IllegalArgumentException("Parameter period must be greater than zero ");
        }
        this.chunkSize = chunkSize;
        this.period = period;
        this.timeUnit = timeUnit;
        this.executor = Executors.newSingleThreadExecutor();
        this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        this.futures = new LinkedBlockingQueue();
    }

    private synchronized void drain() {
        for (List<T> batch : getBatches()) {
            //            final List<T> batchCopy = new CopyOnWriteArrayList<T>(batch);
            futures.offer(executor.submit(() -> getTask().accept(batch)));
        }
    }

    public synchronized void addAll(Collection<T> newItems) {
        items.addAll(newItems);
        if (isTankFull()) {
            drain();
        }
    }

    private boolean isTankFull() {
        return items.size() >= chunkSize;
    }

    private void startSchedule() {
        scheduledExecutor.scheduleAtFixedRate(this::drain, period, period, timeUnit);
    }

    private void shutdownExecutor() {
        if (executor != null) {
            executor.shutdown();
        }
    }

    private void shutdownScheduledExecutor() {
        if (scheduledExecutor != null) {
            scheduledExecutor.shutdown();
        }
    }

    public void shutdown() {
        drain();
        shutdownExecutor();
        shutdownScheduledExecutor();
    }

    public Consumer<List<T>> getTask() {
        return Objects.requireNonNull(task);
    }

    public void execute(Consumer<List<T>> command) {

        this.task = Objects.requireNonNull(command);
        startSchedule();
    }

    private List<List<T>> getBatches() {
        List<List<T>> partitions = new ArrayList<>();
        List<T> itemsCopy = new ArrayList<T>(items);

        for (int index = 0; index < itemsCopy.size(); index += chunkSize) {
            int end = Math.min(itemsCopy.size(), index + chunkSize);

            List<T> batch = itemsCopy.subList(index, end);
            partitions.add(batch);
            items.removeAll(batch);
        }
        return partitions;
    }

    public BlockingQueue getFutures() {
        return futures;
    }
}
