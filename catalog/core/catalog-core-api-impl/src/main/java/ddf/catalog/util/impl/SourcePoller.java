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
 */
package ddf.catalog.util.impl;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.catalog.CatalogFramework;
import ddf.catalog.source.Source;

/**
 * The SourcePoller is the scheduler of the task to poll all configured sources at a fixed interval
 * to determine their availability. It is created by the CatalogFramework's blueprint.
 *
 * An isAvailable() method is included in this class so that the caller, nominally the
 * CatalogFramework, can retrieve the cached availability of a specific source, or have it polled on
 * demand if there is no availability status cached.
 *
 */
public class SourcePoller {

    private static final int INTERVAL = 60;

    private static final int INITIAL_DELAY = 0;

    private static final XLogger LOGGER = new XLogger(LoggerFactory.getLogger(SourcePoller.class));

    private ScheduledExecutorService scheduler;

    private ScheduledFuture<?> handle;

    private SourcePollerRunner runner;

    /**
     * Constructor to schedule the SourcePollerRunner to execute immediately and at a fixed
     * interval, currently set at every 60 seconds. This constructor is invoked by the
     * CatalogFramework's blueprint.
     *
     * @param incomingRunner
     *            the SourcePollerRunner to use for polling
     */
    public SourcePoller(SourcePollerRunner incomingRunner) {

        this.runner = incomingRunner;

        scheduler = Executors.newSingleThreadScheduledExecutor();

        handle = scheduler.scheduleAtFixedRate(runner, INITIAL_DELAY, INTERVAL, TimeUnit.SECONDS);

    }

    /**
     * Retrieves a {@link CachedSource} which contains cached values from the 
     * specified {@link Source}. Returns a {@link Source} with values from the 
     * last polling interval. If the {@link Source} is not known, null is returned.
     *
     * @param source
     *            the source to get the {@link CachedSource} for
     *
     * @return a {@link CachedSource} which contains cached values
     */
    public CachedSource getCachedSource(Source source) {
        return runner.getCachedSource(source);
    }

    /**
     * Cancels the {@link SourcePollerRunner} thread that had been previously scheduled to run at
     * specific intervals. Invoked by the CatalogFramework's blueprint when the framework is
     * unregistered/uninstalled.
     *
     * @param framework
     *            unused, but required by blueprint
     * @param properties
     *            unused, but required by blueprint
     */
    public void cancel(CatalogFramework framework, Map properties) {

        LOGGER.info("Cancelling scheduled polling.");

        runner.shutdown();

        handle.cancel(true);

        scheduler.shutdownNow();

    }

    /**
     * Start method for this poller, invoked by the CatalogFramework's blueprint when the framework
     * is registered/installed. No logic is executed except for logging the framework name.
     *
     * @param framework
     *            the catalog framework being started
     * @param properties
     *            unused, but required by blueprint
     */
    public void start(CatalogFramework framework, Map properties) {
        LOGGER.debug("Framework started for [{}]", framework);
    }

}
