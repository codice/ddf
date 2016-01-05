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
package ddf.common.test;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.hamcrest.Matcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class used to wait for a certain condition to be {@code true} or a timeout to expire. The
 * condition is checked at regular interval. Both the timeout and polling interval can be
 * configured using the {@link #within(long, TimeUnit)} and {@link #checkEvery(long, TimeUnit)}
 * methods respectively.
 * <p/>
 * Usage examples (using Java 8 lambda expressions):
 * <pre>
 *     expect("Query response contains 5 elements").until(() -&gt; runQuery(), hasSize(5));
 *     expect("Configuration object exists").
 *         within(30, SECONDS).
 *         checkEvery(1, SECONDS).
 *         until(() -&gt; configAdmin.getConfiguration(pid, null) != null);
 * </pre>
 */
public class WaitCondition {
    private static final Logger LOGGER = LoggerFactory.getLogger(WaitCondition.class);

    private static final long DEFAULT_TIMEOUT = SECONDS.toMillis(5);

    private static final long DEFAULT_POLLING_INTERVAL = MILLISECONDS.toMillis(500);

    private String description;

    private long timeoutMs;

    private long pollingIntervalMs;

    private WaitCondition(String description) {
        this.description = description;
        this.timeoutMs = DEFAULT_TIMEOUT;
        this.pollingIntervalMs = DEFAULT_POLLING_INTERVAL;
    }

    /**
     * Creates a instance of this class using the default timeout (5s) and polling interval (500ms).
     *
     * @param description short description of what the wait condition is expecting
     * @return new {@link WaitCondition} instance
     */
    public static WaitCondition expect(String description) {
        return new WaitCondition(description);
    }

    /**
     * Sets the timeout on this {@link WaitCondition}.
     *
     * @param timeout timeout to use
     * @param unit    timeout unit
     * @return the current {@link WaitCondition} object
     */
    public WaitCondition within(long timeout, TimeUnit unit) {
        this.timeoutMs = unit.toMillis(timeout);
        return this;
    }

    /**
     * Sets the polling interval this {@link WaitCondition} will use to re-evaluate the wait
     * condition.
     *
     * @param pollingInterval polling interval to use
     * @param unit            polling interval unit
     * @return the current {@link WaitCondition} object
     */
    public WaitCondition checkEvery(long pollingInterval, TimeUnit unit) {
        this.pollingIntervalMs = unit.toMillis(pollingInterval);
        return this;
    }

    /**
     * Waits until the value returned by the {@link Callable} satisfies the {@link Matcher}'s
     * condition, or the timeout expires. The wait condition will be re-evaluated every polling
     * interval. If the timeout expires or an exception occurs while waiting, the current test
     * will be failed.
     *
     * @param <T>                   type of the value returned by the {@link Callable}
     * @param currentValueRetriever object to call to retrieve the value to wait on
     * @param matchCondition        matcher used to determine whether the condition has been
     *                              met or not
     */
    public <T> void until(Callable<T> currentValueRetriever, Matcher<T> matchCondition) {
        long timeoutLimit = System.currentTimeMillis() + timeoutMs;

        try {
            while (!matchCondition.matches(currentValueRetriever.call())) {
                Thread.sleep(pollingIntervalMs);

                if (System.currentTimeMillis() > timeoutLimit) {
                    fail(String.format("%s: expectation not met after %d seconds", description,
                            TimeUnit.MILLISECONDS.toSeconds(timeoutMs)));
                }
            }
        } catch (Exception e) {
            String message = String
                    .format("%s: Unexpected exception [%s] caught while waiting for expectation to be met",
                            description, e);
            LOGGER.error(message);
            fail(message);
        }
    }

    /**
     * Waits until the value returned by the condition is {@code true}, or the timeout expires.
     * The wait condition will be re-evaluated every polling interval. If the timeout expires or
     * an exception occurs while waiting, the current test will be failed.
     *
     * @param condition object that indicates whether the wait condition has been met or not
     */
    public void until(Callable<Boolean> condition) {
        until(condition, is(true));
    }
}
