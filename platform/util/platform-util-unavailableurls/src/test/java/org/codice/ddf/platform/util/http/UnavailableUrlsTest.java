/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.platform.util.http;

import static org.codice.ddf.platform.util.http.UnavailableUrls.INITIAL_TIMEOUT_SECONDS_PROPERTY;
import static org.codice.ddf.platform.util.http.UnavailableUrls.MAX_TIMEOUT_SECONDS_PROPERTY;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsArrayContaining.hasItemInArray;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;

import java.net.HttpURLConnection;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class UnavailableUrlsTest {

  private static final String MAX_TIMEOUT_SECONDS = "300";

  private static final String INITIAL_TIMEOUT_SECONDS = "10";

  @Rule
  public final ProvideSystemProperty initialTimeout =
      new ProvideSystemProperty(INITIAL_TIMEOUT_SECONDS_PROPERTY, INITIAL_TIMEOUT_SECONDS);

  @Rule
  public final ProvideSystemProperty maxTimout =
      new ProvideSystemProperty(MAX_TIMEOUT_SECONDS_PROPERTY, MAX_TIMEOUT_SECONDS);

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
  private boolean isExponentialBackoff(Long[] list) {
    if (list.length < 2) {
      return false;
    }

    double base = list[1] / (double) list[0];
    Long max = list[list.length - 1];

    if (!(base > 1)) {
      return false;
    }

    for (int i = 1; i < list.length - 1; i++) {
      double changeOfBase = Math.log(list[i]) / Math.log(base);
      if (changeOfBase < i && !list[i].equals(max)) {
        return false;
      }
    }

    return true;
  }

  @Test
  public void testIsExponentialBackoff() {
    assertThat(isExponentialBackoff(new Long[] {1L, 1L, 1L, 1L, 1L}), is(false)); // constant
    assertThat(isExponentialBackoff(new Long[] {1L, 2L, 3L, 4L, 5L, 6L}), is(false)); // linear
    assertThat(isExponentialBackoff(new Long[] {1L, 4L, 9L, 16L, 25L}), is(false)); // quadratic
    assertThat(isExponentialBackoff(new Long[] {1L, 8L, 27L, 64L, 125L}), is(false)); // cubic
    assertThat(
        isExponentialBackoff(new Long[] {1L, 2L, 4L, 8L, 16L, 32L}), is(true)); // exponential 2^n
    assertThat(
        isExponentialBackoff(new Long[] {4L, 8L, 16L, 32L, 64L, 128L, 256L}),
        is(true)); // exponential 2^n
    assertThat(
        isExponentialBackoff(new Long[] {1L, 3L, 9L, 81L, 243L, 729L}),
        is(true)); // exponential 3^n
    assertThat(
        isExponentialBackoff(new Long[] {3L, 9L, 81L, 243L, 729L, 2187L}),
        is(true)); // exponential 3^n
    assertThat(
        isExponentialBackoff(new Long[] {1L, 2L, 6L, 24L, 120L, 720L, 5040L}),
        is(true)); // factorial
  }

  @Test
  public void testExponentialBackoff() {

    // Number of times to retry ping, should be big enough to reach max
    // timeout and plateau.
    int retries = 25;
    Long backoffs[] = new Long[retries];

    SchedulerCapture sc;
    UnavailableUrls set = initSet(404);

    set.add("hello");

    for (int i = 0; i < retries; i++) {
      sc = capture();
      backoffs[i] = sc.timeout;
      sc.runnable.run();
    }

    assertThat(isExponentialBackoff(backoffs), is(true));
    assertThat(
        "List should not contain a value greater than the maximum cutoff value",
        backoffs,
        not(hasItemInArray(greaterThan(getMaxTimoutSeconds()))));
  }

  @Test
  public void testUnsetSysProperties() {
    System.getProperties().remove(INITIAL_TIMEOUT_SECONDS_PROPERTY);
    System.getProperties().remove(MAX_TIMEOUT_SECONDS_PROPERTY);
    UnavailableUrls unavailableUrls = new UnavailableUrls();
    assertThat(
        "Failed to find a max retry interval",
        unavailableUrls.getMaxRetryInterval(),
        greaterThan(0));
    assertThat(
        "Failed to find a initial retry interval",
        unavailableUrls.getInitialRetryInterval(),
        greaterThan(0));
  }

  private Long getMaxTimoutSeconds() {
    return Long.parseLong(MAX_TIMEOUT_SECONDS);
  }

  private class SchedulerCapture {
    public final Runnable runnable;

    public final long timeout;

    public final TimeUnit unit;

    SchedulerCapture(Runnable r, Long to, TimeUnit tu) {
      this.runnable = r;
      this.timeout = to;
      this.unit = tu;
    }
  }
}
