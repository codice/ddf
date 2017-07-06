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
package org.codice.ddf.platform.logging;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.ops4j.pax.logging.PaxLogger.LEVEL_DEBUG;
import static org.ops4j.pax.logging.PaxLogger.LEVEL_ERROR;
import static org.ops4j.pax.logging.PaxLogger.LEVEL_INFO;
import static org.ops4j.pax.logging.PaxLogger.LEVEL_TRACE;
import static org.ops4j.pax.logging.PaxLogger.LEVEL_WARNING;

import java.util.Properties;

import org.codice.ddf.platform.logging.LogEvent.Level;
import org.junit.Test;
import org.ops4j.pax.logging.spi.PaxLevel;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;

public class LogEventTest {

    private static final String ERROR_LEVEL = "ERROR";

    private static final String WARN_LEVEL = "WARN";

    private static final String INFO_LEVEL = "INFO";

    private static final String DEBUG_LEVEL = "DEBUG";

    private static final String TRACE_LEVEL = "TRACE";

    private static final String BUNDLE_VERSION = "1.2.3";

    private static final String MESSAGE_1 = "message 1";

    private static final String MESSAGE_2 = "message 2";

    private static final String BUNDLE_NAME_1 = "my-bundle-name-1";

    private static final String BUNDLE_NAME_2 = "my-bundle-name-2";

    private static final String BUNDLE_NAME_KEY = "bundle.name";

    private static final String BUNDLE_VERSION_KEY = "bundle.version";

    @Test
    public void testEqualsLogEventsSameFieldValues() {
        LogEvent logEvent = new LogEvent(getMockPaxLoggingEvent(1L, INFO_LEVEL, MESSAGE_1,
                BUNDLE_NAME_1, BUNDLE_VERSION));
        LogEvent anotherLogEvent = new LogEvent(getMockPaxLoggingEvent(1L, INFO_LEVEL, MESSAGE_1,
                BUNDLE_NAME_1, BUNDLE_VERSION));
        assertThat(logEvent, equalTo(anotherLogEvent));
    }

    @Test
    public void testEqualsLogEventsDifferentReferences() {
        LogEvent logEvent = new LogEvent(getMockPaxLoggingEvent(1L, INFO_LEVEL, MESSAGE_1,
                BUNDLE_NAME_1, BUNDLE_VERSION));
        LogEvent anotherLogEvent = new LogEvent(getMockPaxLoggingEvent(2L, ERROR_LEVEL, MESSAGE_2,
                BUNDLE_NAME_2, BUNDLE_VERSION));
        assertThat(logEvent, not(equalTo(anotherLogEvent)));
    }

    @Test
    public void testEqualsLogEventsSameReference() {
        LogEvent logEvent = new LogEvent(getMockPaxLoggingEvent(1L, TRACE_LEVEL, MESSAGE_1,
                BUNDLE_NAME_1, BUNDLE_VERSION));
        assertThat(logEvent, equalTo(logEvent));
    }

    @Test
    public void testEqualsOtherLogEventNotInstanceOfLogEvent() {
        LogEvent logEvent = new LogEvent(getMockPaxLoggingEvent(1L, DEBUG_LEVEL, MESSAGE_1,
                BUNDLE_NAME_1, BUNDLE_VERSION));
        String anotherLogEvent = "logEvent";
        assertThat(logEvent, not(equalTo(anotherLogEvent)));
    }

    @Test
    public void testHashCode() {
        LogEvent logEvent = new LogEvent(getMockPaxLoggingEvent(1L, WARN_LEVEL, MESSAGE_1,
                BUNDLE_NAME_1, BUNDLE_VERSION));
        LogEvent anotherLogEvent = new LogEvent(getMockPaxLoggingEvent(1L, WARN_LEVEL, MESSAGE_1,
                BUNDLE_NAME_1, BUNDLE_VERSION));
        assertThat(logEvent, equalTo(anotherLogEvent));
        assertThat(anotherLogEvent, equalTo(logEvent));
        assertThat(logEvent.hashCode(), equalTo(anotherLogEvent.hashCode()));
    }

    @Test
    public void testGetLogLevel() {
        LogEvent logEvent = new LogEvent(getMockPaxLoggingEvent(1L, INFO_LEVEL, MESSAGE_1,
                BUNDLE_NAME_1, BUNDLE_VERSION));
        Level level = logEvent.getLevel();
        assertThat(level, equalTo(Level.INFO));
    }

    private void addBundleNameProperty(Properties properties, String bundleName) {
        properties.put(BUNDLE_NAME_KEY, bundleName);
    }

    private void addBundleVersionProperty(Properties properties, String bundleVersion) {
        properties.put(BUNDLE_VERSION_KEY, bundleVersion);
    }

    private PaxLevel getMockPaxLevel(String level) {
        PaxLevel mockPaxLevel = new PaxLevel() {
            @Override
            public String toString() {
                return level;
            }

            @Override
            public boolean isGreaterOrEqual(PaxLevel r) {
                return false;
            }

            @Override
            public int toInt() {
                switch (level) {
                case ERROR_LEVEL:
                    return LEVEL_ERROR;
                case WARN_LEVEL:
                    return LEVEL_WARNING;
                case INFO_LEVEL:
                    return LEVEL_INFO;
                case DEBUG_LEVEL:
                    return LEVEL_DEBUG;
                case TRACE_LEVEL:
                    return LEVEL_TRACE;
                default:
                    return -1;
                }
            }

            @Override
            public int getSyslogEquivalent() {
                return 0;
            }
        };

        return mockPaxLevel;
    }

    private Properties getLoggingProperties(String bundleName, String bundleVersion) {
        Properties properties = new Properties();
        addBundleNameProperty(properties, bundleName);
        addBundleVersionProperty(properties, bundleVersion);
        return properties;
    }

    private PaxLoggingEvent getMockPaxLoggingEvent(long timestamp, String level, String message,
            String bundleName, String bundleVersion) {
        PaxLoggingEvent mockPaxLoggingEvent = mock(PaxLoggingEvent.class);
        when(mockPaxLoggingEvent.getTimeStamp()).thenReturn(timestamp);
        when(mockPaxLoggingEvent.getLevel()).thenReturn(getMockPaxLevel(level));
        when(mockPaxLoggingEvent.getMessage()).thenReturn(message);
        when(mockPaxLoggingEvent.getProperties()).thenReturn(
                getLoggingProperties(bundleName, bundleVersion));
        return mockPaxLoggingEvent;
    }
}

