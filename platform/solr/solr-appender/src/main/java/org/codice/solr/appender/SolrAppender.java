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
package org.codice.solr.appender;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentItem;
import org.codice.ddf.persistence.PersistentStore;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrAppender implements EventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolrAppender.class);

    private static final String BATCH_SIZE_PROPERTY = "org.codice.solr.appender.batchSize";

    private static final String PERIOD_PROPERTY = "org.codice.solr.appender.period";

    private static final int DEFAULT_BATCH_SIZE = 100;

    private static final int DEFAULT_PERIOD_IN_SECONDS = 10;

    private int batchSize = DEFAULT_BATCH_SIZE;

    private int period = DEFAULT_PERIOD_IN_SECONDS;

    private PersistentStore persistentStore;

    private ConcurrentLinkedQueue<PersistentItem> queue = new ConcurrentLinkedQueue<>();

    private ScheduledExecutorService executorService;

    private ScheduledFuture scheduledFuture;

    public SolrAppender(PersistentStore persistentStore, ScheduledExecutorService executorService) {
        this.persistentStore = persistentStore;
        this.executorService = executorService;
        batchSize = Integer.parseInt(System.getProperty(BATCH_SIZE_PROPERTY, "100"));
        period = Integer.parseInt(System.getProperty(PERIOD_PROPERTY, "10"));
    }

    @Override
    public void handleEvent(Event event) throws IllegalArgumentException {
        LOGGER.debug("Received decanter collection event on topic {}", event.getTopic());

        PersistentItem item = new PersistentItem();
        if (event.getProperty("id") == null) {
            item.addIdProperty(UUID.randomUUID()
                    .toString());
        }

        for (String key : event.getPropertyNames()) {
            item.addProperty(key, event.getProperty(key));
        }
        queue.add(item);
        if (queue.size() > batchSize) {
            flushItems();
        }

    }

    public void init() {
        scheduledFuture = executorService.scheduleAtFixedRate(this::flushItems,
                5,
                period,
                TimeUnit.SECONDS);
    }

    public void destroy() {
        executorService.shutdown();
    }

    synchronized void flushItems() {
        if (queue.isEmpty()) {
            return;
        }

        try {
            Collection<Map<String, Object>> items = new ArrayList<>();
            items.addAll(queue);
            queue.removeAll(items);
            LOGGER.debug("Flushing {} items to decanter core", items.size());
            persistentStore.add("decanter", items);
        } catch (PersistenceException e) {
            LOGGER.error("Failed to persist items to decanter core", e);
        }
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        if (this.period != period) {
            this.period = period;
            scheduledFuture.cancel(false);
            init();
        }
    }
}
