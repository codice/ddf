/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package org.codice.ddf.spatial.ogc.catalog.common;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runnable task to cache and update the availability based on the {@link AvailabilityCommand}.
 * NOTE: It is ideal to run this on a short interval (1 second) to maintain an accurate status of
 * the availability.
 * 
 * @author kcwire
 * 
 */
public class AvailabilityTask implements Runnable {

    public static final int NO_DELAY = 0;

    public static final int ONE_SECOND = 1;

    private boolean isAvailable = false;

    private AtomicLong lastAvailableTimestamp = new AtomicLong(0);

    private long interval;

    private AvailabilityCommand availabilityCommand;

    private String sourceId;

    private static final Logger LOGGER = LoggerFactory.getLogger(AvailabilityTask.class);

    /**
     * Default Constructor
     * 
     * @param interval
     *            - in millis
     * @param command
     *            - the command to execute
     * @param id
     *            - the unique id
     */
    public AvailabilityTask(long interval, AvailabilityCommand command, String id) {
        this.interval = interval;
        this.availabilityCommand = command;
        this.sourceId = id;
    }

    /**
     * Determines if the interval has elapsed before executing the {@link AvailabilityCommand}. It
     * is ideal this task is run on short intervals to check if the interval has elapsed.
     */
    @Override
    public void run() {
        if ((System.currentTimeMillis() - lastAvailableTimestamp.get()) >= interval) {
            isAvailable = availabilityCommand.isAvailable();
            LOGGER.debug("Source: {} -> isAvailable = {} ", sourceId, isAvailable);
            updateLastAvailableTimestamp(System.currentTimeMillis());
        }
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    /**
     * Update the timestamp based on last successful response. This timestamp is used to determine
     * if the interval has elapsed.
     * 
     * @param timestamp
     *            - timestamp in millis
     */
    public void updateLastAvailableTimestamp(long timestamp) {
        LOGGER.debug("Updating Availability poll interval timestamp for {}", sourceId);
        lastAvailableTimestamp.compareAndSet(lastAvailableTimestamp.get(), timestamp);
    }

    /**
     * Sets the interval. This interval is used to determine if the Command should be executed.
     * 
     * @param interval
     *            - interval in millis
     */
    public void setInterval(long interval) {
        this.interval = interval;
    }

}
