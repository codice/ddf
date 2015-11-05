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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.net.HttpURLConnection;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class UnavailableUrlsTest {

    private ScheduledExecutorService scheduler;

    @Before
    public void setUp() throws Exception {
        scheduler = Mockito.mock(ScheduledExecutorService.class);
    }

    private UnavailableUrls initSet(int pingResponse) {
        return new UnavailableUrls() {
            @Override
            ScheduledExecutorService getScheduler() {
                return scheduler;
            }

            @Override
            int ping(String url) {
                return pingResponse;
            }
        };
    }

    @Test
    public void testRandomContains() {
        UnavailableUrls set = initSet(200);
        assertThat(set.contains("random"), is(false));
    }

    @Test
    public void testInitialContains() {
        UnavailableUrls set = initSet(200);
        set.add("hello");
        assertThat(set.contains("hello"), is(true));
    }

    private class SchedulerCapture {
        public final Runnable runnable;

        public final long timeout;

        public final TimeUnit unit;

        public SchedulerCapture(Runnable r, long to, TimeUnit tu) {
            this.runnable = r;
            this.timeout = to;
            this.unit = tu;
        }
    }

    // capture the call to the scheduler and return the parameters
    private SchedulerCapture capture() {

        ArgumentCaptor<Runnable> runnable = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Long> timeout = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<TimeUnit> unit = ArgumentCaptor.forClass(TimeUnit.class);

        Mockito.verify(scheduler).schedule(runnable.capture(), timeout.capture(), unit.capture());
        Mockito.reset(scheduler);

        return new SchedulerCapture(runnable.getValue(), timeout.getValue(), unit.getValue());
    }

    // test the contents of the set after a ping
    // statusCode - the http status code the ping should return
    // present - is the url suppose to be present?
    private void testPing(int statusCode, boolean present) {

        UnavailableUrls set = initSet(statusCode);

        set.add("hello");

        capture().runnable.run(); // force URL ping

        assertThat(set.contains("hello"), is(present));
    }

    @Test
    // if the ping returns ok, the url shouldn't be present
    public void testSuccessfulPing() {
        testPing(HttpURLConnection.HTTP_OK, false);
    }

    @Test
    // if the ping returns not ok, the url should still be present
    public void testUnsuccessfulPing() {
        testPing(HttpURLConnection.HTTP_NOT_FOUND, true);
    }

    /**
     * Check if a list of numbers grows exponentially or greater upto a max backoff.
     *
     * @param list - list of two or more positive integers
     * @return true if sequence is exponential, false otherwise
     */
    private boolean isExponentialBackoff(long[] list) {
        if (list.length < 2) {
            return false;
        }

        double base = list[1] / (double) list[0];
        long max = list[list.length - 1];

        if (!(base > 1)) {
            return false;
        }

        for (int i = 1; i < list.length - 1; i++) {
            double changeOfBase = Math.log(list[i]) / Math.log(base);
            if (changeOfBase < i && list[i] != max) {
                return false;
            }
        }

        return true;
    }

    @Test
    public void testIsExponentialBackoff() {
        assertThat(isExponentialBackoff(new long[] {1, 1, 1, 1, 1}), is(false)); // constant
        assertThat(isExponentialBackoff(new long[] {1, 2, 3, 4, 5, 6}), is(false)); // linear
        assertThat(isExponentialBackoff(new long[] {1, 4, 9, 16, 25}), is(false)); // quadratic
        assertThat(isExponentialBackoff(new long[] {1, 8, 27, 64, 125}), is(false)); // cubic
        assertThat(isExponentialBackoff(new long[] {1, 2, 4, 8, 16, 32}),
                is(true));  // exponential 2^n
        assertThat(isExponentialBackoff(new long[] {4, 8, 16, 32, 64, 128, 256}),
                is(true));  // exponential 2^n
        assertThat(isExponentialBackoff(new long[] {1, 3, 9, 81, 243, 729}),
                is(true));  // exponential 3^n
        assertThat(isExponentialBackoff(new long[] {3, 9, 81, 243, 729, 2187}),
                is(true));  // exponential 3^n
        assertThat(isExponentialBackoff(new long[] {1, 2, 6, 24, 120, 720, 5040}),
                is(true));  // factorial
    }

    @Test
    public void testExponentialBackoff() {

        // Number of times to retry ping, should be big enough to reach max
        // timeout and plateau.
        int retries = 25;
        long backoffs[] = new long[retries];

        SchedulerCapture sc;
        UnavailableUrls set = initSet(404);

        set.add("hello");

        for (int i = 0; i < retries; i++) {
            sc = capture();
            backoffs[i] = sc.timeout;
            sc.runnable.run();
        }

        assertThat(isExponentialBackoff(backoffs), is(true));
    }

}