package ddf.catalog.backup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.google.common.collect.Lists;

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

public class PeriodicDrain<T> {

    List<T> items = new ArrayList<>();

    private ExecutorService executor;

    private ScheduledExecutorService scheduledExecutor;

    private Consumer<List<T>> task;

    private int flushOnCount;

    private long period;

    private TimeUnit timeUnit;

    public PeriodicDrain(int flushOnCount, long period, TimeUnit timeUnit) {
        if (flushOnCount < 1) {
            throw new IllegalArgumentException("Parameter flushOnCount must be greater than zero");
        }
        if (period < 1) {
            throw new IllegalArgumentException("Parameter period must be greater than zero ");
        }
        this.flushOnCount = flushOnCount;
        this.period = period;
        this.timeUnit = timeUnit;
        this.executor = Executors.newSingleThreadExecutor();
        this.scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    }

    private synchronized void drain() {
        for (List<T> batch : Lists.partition(items, flushOnCount)) {
            final List<T>  batchCopy = new CopyOnWriteArrayList<T>(batch);
            executor.execute(() -> getTask().accept(batchCopy));
        }
        items.clear();
    }

    public synchronized void addAll(Collection<T> newItems) {
        items.addAll(newItems);
        if (isBigEnough()) {
            drain();
        }
    }

    private boolean isBigEnough() {
        return items.size() >= flushOnCount;
    }

    private void startPolling() {
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
        shutdownExecutor();
        shutdownScheduledExecutor();
    }

    public Consumer<List<T>> getTask() {
        if (task == null) {
            throw new RuntimeException("Task cannot be null. Call setTask first.");
        }
        return task;
    }

    public void setTask(Consumer<List<T>> task) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null.");
        }
        this.task = task;
        startPolling();
    }
}

