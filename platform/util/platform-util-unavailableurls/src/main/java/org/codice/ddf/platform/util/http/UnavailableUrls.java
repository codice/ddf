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
package org.codice.ddf.platform.util.http;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class to contain a set of unavailable URL's. They will be periodically checked
 * to determine if the URL has become available. If the URL is reachable, this implies
 * the web service is available, and the URL will be removed from the set.
 * <p>
 * NOTE: to avoid excessive network traffic, the period to wait before checking for a
 * URL increases exponentially.
 */
public class UnavailableUrls {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnavailableUrls.class);

    // scheduler the URL checks.
    // NOTE: every instance of UnavailableUrls will get the same scheduler and it's single
    // threaded, so pings are queued up and happen in serial. However, if this class gets loaded
    // from multiple class loaders, there would be a scheduler/thread for each class loader.
    private static final ScheduledExecutorService SCHEDULER = Executors
            .newSingleThreadScheduledExecutor();

    // time to give the ping request before we stop trying
    private static final long PING_TIMEOUT_SECONDS = 10;

    // initial timeout for checking a URL
    private static final long INITIAL_TIMEOUT_SECONDS = 3;

    // maximum timeout for checking a URL
    private static final long MAX_TIMEOUT_SECONDS = TimeUnit.HOURS.toSeconds(7);

    private final ConcurrentHashMap<String, UrlChecker> checkers = new ConcurrentHashMap<>();

    ScheduledExecutorService getScheduler() {
        return SCHEDULER;
    }

    /**
     * Ping a url with an HTTP get request.
     * NOTE: all pings have a timeout of PING_TIMEOUT_MS.
     *
     * @param url - url to try and get
     * @return HTTP status code of making the GET request
     */
    int ping(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(PING_TIMEOUT_SECONDS));
        connection.connect();
        return connection.getResponseCode();
    }

    /**
     * Add a URL to the set of unavailable URL's.
     * NOTE: the URL will automatically be removed when it can be reached.
     *
     * @param url - no validation that the string is a valid URL
     */
    public void add(String url) {
        if (!checkers.containsKey(url)) {
            checkers.put(url, new UrlChecker(url));
        }
    }

    /**
     * Does the set contain a specified URL.
     *
     * @param url
     * @return the truth
     */
    public boolean contains(String url) {
        return checkers.containsKey(url);
    }

    /**
     * Periodically checks if a URL is available.
     * <p>
     * NOTE: the exponential backoff calculations could be refactored and generalized into
     * a generic backoff class; this works fine for now.
     */
    private class UrlChecker implements Runnable {

        private final String url;

        private long timeout = INITIAL_TIMEOUT_SECONDS;

        /**
         * Let me check that URL for you!
         *
         * @param url - also no validation
         */
        UrlChecker(String url) {
            this.url = url;

            // whenever a checker is constructed, it auto schedules it's first URL check
            schedule();
        }

        /**
         * Computes the next timeout, doesn't exceed MAX_TIMEOUT_SECONDS
         */
        private void backoff() {
            timeout = Math.min(timeout * INITIAL_TIMEOUT_SECONDS, MAX_TIMEOUT_SECONDS);
        }

        private void schedule() {
            getScheduler().schedule(this, timeout, TimeUnit.SECONDS);
            backoff(); // the only reason to schedule is because of a failed ping
        }

        /**
         * Ping the URL.
         * If successful, remove from set and stop checking.
         * Else reschedule for another ping.
         */
        public void run() {
            try {
                // assume that an HTTP OK is a successful ping
                if (ping(url) == HttpURLConnection.HTTP_OK) {
                    checkers.remove(url);
                    return;
                }
            } catch (Exception e) {
                LOGGER.debug(
                        "Exception occurred while trying to ping URL {}. {}",
                        url, e.getMessage());
            }

            LOGGER.debug("Couldn't find URL. Checking again in {} seconds.", timeout);
            schedule();
        }
    }
}

