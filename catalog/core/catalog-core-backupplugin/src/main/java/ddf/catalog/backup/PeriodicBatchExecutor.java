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
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class PeriodicBatchExecutor<T> {

    public static final int TIMEOUT = 10;

    private static final Logger LOGGER = LoggerFactory.getLogger(PeriodicBatchExecutor.class);

    final List<T> items;

    private final ExecutorService executor;

    private final ScheduledExecutorService scheduledExecutor;

    private final BlockingQueue<Future> futures;

    private final int chunkSize;

    private final long period;

    private final TimeUnit timeUnit;

    private Consumer<List<T>> task;

    public PeriodicBatchExecutor(int chunkSize, long period, TimeUnit timeUnit) {

        Validate.isTrue(chunkSize > 0, "Parameter chunkSize must be greater than zero");
        Validate.isTrue(period > 0, "Parameter period must be greater than zero ");
        this.chunkSize = chunkSize;
        this.period = period;
        this.timeUnit = timeUnit;
        this.executor = Executors.newSingleThreadExecutor();
        this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
        this.futures = new LinkedBlockingQueue<>();
        this.items = new ArrayList<>();
    }

    public synchronized void addAll(List<T> newItems) {
        getItems().addAll(newItems);
        if (isTankFull()) {
            drain();
        }
    }

    public List<T> getItems() {
        return items;
    }

    public void shutdown() {
        drain();
        Stream.of(executor, scheduledExecutor)
                .forEach(executor -> {
                    executor.shutdown();
                    if (!executor.isShutdown()) {
                        try {
                            executor.awaitTermination(TIMEOUT, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            throw new IllegalStateException(e);
                        }
                        final List<Runnable> failures = executor.shutdownNow();
                        final int failedCount = failures.size();
                        if (failedCount > 0) {
                            LOGGER.debug("Failed to execute {} tasks", failedCount);
                        }
                    }
                });
    }

    public Consumer<List<T>> getTask() {
        Validate.notNull(task, "The executor's task has not been set");
        return task;
    }

    public void setTask(Consumer<List<T>> command) {
        Validate.notNull(command, "Command object cannot be null");
        this.task = command;
        startSchedule();
    }

    synchronized void drain() {
        for (List<T> batch : getBatches()) {
            getFutures().add(executor.submit(() -> getTask().accept(batch)));
        }
    }

    List<List<T>> getBatches() {
        return Lists.partition(getItems(), chunkSize);
    }

    BlockingQueue<Future> getFutures() {
        return futures;
    }

    private boolean isTankFull() {
        return getItems().size() >= chunkSize;
    }

    private void startSchedule() {
        scheduledExecutor.scheduleAtFixedRate(this::drain, period, period, timeUnit);
    }
}
