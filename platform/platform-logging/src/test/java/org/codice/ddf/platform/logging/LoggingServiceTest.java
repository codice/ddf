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
package org.codice.ddf.platform.logging;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.management.MBeanServer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.ops4j.pax.logging.spi.PaxLevel;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;
import org.osgi.service.log.LogLevel;

@RunWith(MockitoJUnitRunner.class)
public class LoggingServiceTest {

  private static final String BUNDLE_NAME_KEY = "bundle.name";

  private static final String BUNDLE_VERSION_KEY = "bundle.version";

  private static final int MAX_LOG_EVENTS_LIMIT = 5000;

  @Mock private MBeanServer mockMBeanServer;

  @Test
  public void testRetrieveLogEvents() throws Exception {
    // Setup
    List<PaxLoggingEvent> mockPaxLoggingEvents =
        getMockPaxLoggingEventsTimestampOrderSmallestToLargest(3);
    List<LogEvent> expectedLogEvents = getExpectedLogEvents(mockPaxLoggingEvents);
    LoggingService loggingServiceBean = getLoggingService();
    appendLogs(loggingServiceBean, mockPaxLoggingEvents);

    // Perform Test
    List<LogEvent> actualLogEvents = loggingServiceBean.retrieveLogEvents();

    // Verify
    assertThat(
        actualLogEvents,
        contains(expectedLogEvents.toArray(new LogEvent[expectedLogEvents.size()])));
  }

  @Test
  public void testResizeMaxLogEventsKeepsAppropriateLogEvents() throws Exception {
    // Setup
    List<PaxLoggingEvent> mockPaxLoggingEvents =
        getMockPaxLoggingEventsTimestampOrderSmallestToLargest(3);
    LogEvent expectedLogEventAfterResize = getExpectedLogEvent(mockPaxLoggingEvents.get(2));
    LoggingService loggingServiceBean = getLoggingService();
    appendLogs(loggingServiceBean, mockPaxLoggingEvents);

    // Perform Test
    loggingServiceBean.setMaxLogEvents(1);

    // Verify
    List<LogEvent> actualLogEvents = loggingServiceBean.retrieveLogEvents();
    assertThat(actualLogEvents, contains(expectedLogEventAfterResize));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetMaxLogEventsTo0() throws Exception {
    // Setup
    LoggingService loggingServiceBean = getLoggingService();

    // Perform Test
    loggingServiceBean.setMaxLogEvents(0);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetMaxLogEventsToExceedLimit() throws Exception {
    // Setup
    LoggingService loggingServiceBean = getLoggingService();

    // Perform Test
    loggingServiceBean.setMaxLogEvents(MAX_LOG_EVENTS_LIMIT + 1);
  }

  /** Verify oldest log events are evicted when queue is full. */
  @Test
  public void testDoAppendWhenLoggingQueueIsFull() throws Exception {
    // Setup
    List<PaxLoggingEvent> mockPaxLoggingEvents =
        getMockPaxLoggingEventsTimestampOrderSmallestToLargest(6);
    List<LogEvent> expectedLogEvents =
        getExpectedLogEvents(mockPaxLoggingEvents).subList(3, mockPaxLoggingEvents.size());
    LoggingService loggingServiceBean = getLoggingService();
    loggingServiceBean.setMaxLogEvents(3);
    appendLogs(loggingServiceBean, mockPaxLoggingEvents.subList(0, 3));

    // Perform Test
    appendLogs(loggingServiceBean, mockPaxLoggingEvents.subList(3, mockPaxLoggingEvents.size()));

    // Verify
    List<LogEvent> actualLogEvents = loggingServiceBean.retrieveLogEvents();
    assertThat(
        actualLogEvents,
        contains(expectedLogEvents.toArray(new LogEvent[expectedLogEvents.size()])));
  }

  @Test
  public void testDestroy() throws Exception {
    LoggingService loggingServiceBean = getLoggingService();
    loggingServiceBean.destroy();
  }

  private PaxLevel getMockPaxLevel(String level) {
    PaxLevel mockPaxLevel =
        new PaxLevel() {
          @Override
          public String toString() {
            return level;
          }

          @Override
          public boolean isGreaterOrEqual(PaxLevel r) {
            return false;
          }

          @Override
          public LogLevel toLevel() {
            switch (level) {
              case "ERROR":
                return LogLevel.ERROR;
              case "WARN":
                return LogLevel.WARN;
              case "INFO":
                return LogLevel.INFO;
              case "DEBUG":
                return LogLevel.DEBUG;
              case "TRACE":
                return LogLevel.TRACE;
              default:
                return null;
            }
          }

          @Override
          public int getSyslogEquivalent() {
            return 0;
          }
        };

    return mockPaxLevel;
  }

  private List<PaxLoggingEvent> getMockPaxLoggingEventsTimestampOrderSmallestToLargest(
      int numberOfLoggingEvents) {
    String baseMessage = "message ";
    String baseBundleName = "my-bundle-name-";

    List<PaxLoggingEvent> mockPaxLoggingEvents = new ArrayList<>(3);

    for (int i = 0; i < numberOfLoggingEvents; i++) {
      PaxLoggingEvent mockPaxLoggingEvent =
          getMockPaxLoggingEvent(i, "INFO", baseMessage + i, baseBundleName + i, "1.2.3");
      mockPaxLoggingEvents.add(mockPaxLoggingEvent);
    }

    return mockPaxLoggingEvents;
  }

  private List<LogEvent> getExpectedLogEvents(List<PaxLoggingEvent> mockPaxLoggingEvents) {
    List<LogEvent> expectedLogEvents = new ArrayList<>(mockPaxLoggingEvents.size());

    for (PaxLoggingEvent mockPaxLoggingEvent : mockPaxLoggingEvents) {
      expectedLogEvents.add(getExpectedLogEvent(mockPaxLoggingEvent));
    }

    return expectedLogEvents;
  }

  private LogEvent getExpectedLogEvent(PaxLoggingEvent mockPaxLoggingEvent) {
    return new LogEvent(mockPaxLoggingEvent);
  }

  private Map<String, Object> getLoggingProperties(String bundleName, String bundleVersion) {
    Map<String, Object> properties = new HashMap<>();
    properties.put(BUNDLE_NAME_KEY, bundleName);
    properties.put(BUNDLE_VERSION_KEY, bundleVersion);
    return properties;
  }

  private PaxLoggingEvent getMockPaxLoggingEvent(
      long timestamp, String level, String message, String bundleName, String bundleVersion) {
    PaxLoggingEvent mockPaxLoggingEvent = mock(PaxLoggingEvent.class);
    when(mockPaxLoggingEvent.getTimeStamp()).thenReturn(timestamp);
    when(mockPaxLoggingEvent.getLevel()).thenReturn(getMockPaxLevel(level));
    when(mockPaxLoggingEvent.getMessage()).thenReturn(message);
    when(mockPaxLoggingEvent.getProperties())
        .thenReturn(getLoggingProperties(bundleName, bundleVersion));
    return mockPaxLoggingEvent;
  }

  private void appendLogs(
      LoggingService loggingServiceBean, List<PaxLoggingEvent> mockPaxLoggingEvents) {
    for (PaxLoggingEvent mockPaxLoggingEvent : mockPaxLoggingEvents) {
      loggingServiceBean.doAppend(mockPaxLoggingEvent);
    }
  }

  private LoggingService getLoggingService() throws Exception {
    LoggingService loggingServiceBean = new LoggingService(mockMBeanServer);
    loggingServiceBean.init();
    return loggingServiceBean;
  }
}
