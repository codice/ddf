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
package ddf.catalog.util.impl;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.source.Source;

/**
 * The poller to check the availability of all configured sources. This class is instantiated by the
 * CatalogFramework's blueprint and is scheduled by the {@link SourcePoller} to execute at a
 * configured rate, i.e., the polling interval.
 * <p>
 * This class maintains a list of all of the sources to be polled for their availability. Sources
 * are added to this list when they come online and when they are deleted. A cached map is
 * maintained of all the sources and their last availability states.
 */
public class SourcePollerRunner implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SourcePollerRunner.class);

    private List<Source> sources;

    private Map<SourceKey, CachedSource> cachedSources =
            new ConcurrentSkipListMap<>(Comparator.comparing(SourceKey::getId,
                    Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(SourceKey::getTitle,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(SourceKey::getVersion,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(SourceKey::getDescription,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(SourceKey::getOrganization,
                            Comparator.nullsLast(Comparator.naturalOrder())));

    private ExecutorService pool;

    private Map<CachedSource, Lock> sourceStatusThreadLocks = new ConcurrentHashMap<>();

    /**
     * Creates an empty list of {@link Source} sources to be polled for availability. This
     * constructor is invoked by the CatalogFramework's blueprint.
     */
    public SourcePollerRunner() {

        LOGGER.debug("Creating source poller runner.");
        sources = new CopyOnWriteArrayList<>();
    }

    /**
     * Checks the availability of each source in the list of sources to be polled.
     */
    @Override
    public void run() {

        LOGGER.trace("RUNNER checking source statuses");

        for (Source source : sources) {

            if (source != null) {

                checkStatus(source);

            }

        }

    }

    /**
     * Checks if the specified source is available, updating the internally maintained map of
     * sources and their status. Lock ensures only one status thread is running per source.
     *
     * @param source the source to check if it is available
     */
    private void checkStatus(final Source source) {

        if (pool == null) {
            pool = Executors.newCachedThreadPool();

        }
        final Runnable statusRunner = () -> {
            final CachedSource cachedSource = cachedSources.get(getSourceKey(source));
            if (cachedSource != null) {
                Lock sourceStatusThreadLock = sourceStatusThreadLocks.get(cachedSource);

                if (sourceStatusThreadLock.tryLock()) {
                    try {
                        LOGGER.debug("Acquired lock for Source [{}] with id [{}]",
                                source,
                                getSourceKey(source).getId());
                        cachedSource.checkStatus();
                    } finally {
                        // release the lock acquired initially
                        sourceStatusThreadLock.unlock();
                        LOGGER.debug("Released lock for Source [{}] with id [{}]",
                                source,
                                getSourceKey(source).getId());
                    }
                } else {
                    LOGGER.debug("Unable to get lock for Source [{}] with id [{}]."
                            + "  A status thread is already running.", source, getSourceKey(source).getId());
                }
            }
        };
        pool.execute(statusRunner);
    }

    /**
     * Adds the {@link Source} instance to the list and sets its current status to UNCHECKED,
     * indicating it will checked at the next polling interval.
     *
     * @param source the source to add to the list
     */
    public void bind(Source source) {

        LOGGER.debug("Binding source: {}", source);
        if (source != null && !sources.contains(source)) {
            LOGGER.debug("Marking new source {} as UNCHECKED.", source);
            sources.add(source);
            CachedSource cachedSource = new CachedSource(source);
            cachedSources.put(getSourceKey(source), cachedSource);
            sourceStatusThreadLocks.put(cachedSource, new ReentrantLock());
            checkStatus(source);

        }
    }

    /**
     * Removes the {@link Source} instance from the list of references so that its availability is
     * no longer polled.
     *
     * @param source the source to remove from the list
     */
    public void unbind(Source source) {
        LOGGER.debug("Unbinding source [{}]", source);
        if (source != null) {
            sources.remove(source);
            CachedSource cachedSource = cachedSources.remove(getSourceKey(source));
            if (cachedSource != null) {
                sourceStatusThreadLocks.remove(cachedSource);
            }
        }
    }

    /**
     * Retrieves a {@link CachedSource} which contains cached values from the
     * specified {@link Source}. Returns a {@link Source} with values from the
     * last polling interval. If the {@link Source} is not known, null is returned.
     *
     * @param source the source to get the {@link CachedSource} for
     * @return a {@link CachedSource} which contains cached values
     */
    public CachedSource getCachedSource(Source source) {
        return cachedSources.get(getSourceKey(source));
    }

    /**
     * Calls the @link ExecutorService to shutdown immediately
     */
    public void shutdown() {
        LOGGER.trace("Shutting down status threads");
        if (pool != null) {
            pool.shutdownNow();
        }
        LOGGER.trace("Status threads shut down");
    }

    private SourceKey getSourceKey(Source source) {
        return new SourceKey(source);
    }

    private static class SourceKey {
        private final Source source;

        private String id;

        private String title;

        private String version;

        private String description;

        private String organization;

        public SourceKey(Source source) {
            this.source = source;
            this.id = source.getId();
            this.title = source.getTitle();
            this.version = source.getVersion();
            this.description = source.getDescription();
            this.organization = source.getOrganization();
        }

        public String getId() {
            try {
                id = source.getId();
            } catch (Exception e) {
                LOGGER.debug("Proxy source destroyed before cleanup", e);
            }
            return id;
        }

        public String getTitle() {
            try {
                title = source.getTitle();
            } catch (Exception e) {
                LOGGER.debug("Proxy source destroyed before cleanup", e);
            }
            return title;
        }

        public String getVersion() {
            try {
                version = source.getVersion();
            } catch (Exception e) {
                LOGGER.debug("Proxy source destroyed before cleanup", e);
            }
            return version;
        }

        public String getDescription() {
            try {
                description = source.getDescription();
            } catch (Exception e) {
                LOGGER.debug("Proxy source destroyed before cleanup", e);
            }
            return description;
        }

        public String getOrganization() {
            try {
                organization = source.getOrganization();
            } catch (Exception e) {
                LOGGER.debug("Proxy source destroyed before cleanup", e);
            }
            return organization;
        }
    }
}
