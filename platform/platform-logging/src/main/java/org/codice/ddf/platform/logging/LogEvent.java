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

import static org.ops4j.pax.logging.PaxLogger.LEVEL_DEBUG;
import static org.ops4j.pax.logging.PaxLogger.LEVEL_ERROR;
import static org.ops4j.pax.logging.PaxLogger.LEVEL_INFO;
import static org.ops4j.pax.logging.PaxLogger.LEVEL_TRACE;
import static org.ops4j.pax.logging.PaxLogger.LEVEL_WARNING;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;

/**
 * Describes a log event in the system
 */
public class LogEvent {

    private static final String BUNDLE_NAME_KEY = "bundle.name";

    private static final String BUNDLE_VERSION_KEY = "bundle.version";

    private final long timestamp;

    private final Level level;

    private final String message;

    private final String bundleName;

    private final String bundleVersion;

    /**
     * Constructor
     * 
     * @param paxLoggingEvent
     *            the {@link org.ops4j.pax.logging.spi.PaxLoggingEvent} used to create this
     *            {@link LogEvent}
     */
    LogEvent(PaxLoggingEvent paxLoggingEvent) {
        this.timestamp = paxLoggingEvent.getTimeStamp();
        this.level = getLevel(paxLoggingEvent.getLevel().toInt());
        this.message = paxLoggingEvent.getMessage();
        this.bundleName = getBundleName(paxLoggingEvent);
        this.bundleVersion = getBundleVersion(paxLoggingEvent);
    }

    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Returns the log level
     * 
     * @return {@link Level} of this {@link LogEvent}
     */
    public Level getLevel() {
        return level;
    }

    /**
     * Returns the Message of this {@link LogEvent}
     * 
     * @return the message of this {@link LogEvent} (can be null)
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the name of the bundle that created this {@link LogEvent}
     * 
     * @return the name of the bundle that created this {@link LogEvent} (can be null)
     */
    public String getBundleName() {
        return bundleName;
    }

    /**
     * Returns the version of the bundle that created this {@link LogEvent}
     * 
     * @return the version of the bundle that created this {@link LogEvent} (can be null)
     */
    public String getBundleVersion() {
        return bundleVersion;
    }

    /**
     * Compares this {@link LogEvent} with the specified {@link java.lang.Object}
     */
    @Override
    public boolean equals(Object anotherLogEvent) {
        if (!(anotherLogEvent instanceof LogEvent)) {
            return false;
        }
        if (anotherLogEvent == this) {
            return true;
        }
        LogEvent rhs = (LogEvent) anotherLogEvent;
        return new EqualsBuilder().append(timestamp, rhs.getTimestamp())
                .append(level.getLevel(), rhs.getLevel().getLevel())
                .append(message, rhs.getMessage()).append(bundleName, rhs.getBundleName())
                .append(bundleVersion, rhs.getBundleVersion()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31).append(timestamp).append(level).append(message)
                .append(bundleName).append(bundleVersion).toHashCode();
    }

    private String getBundleName(PaxLoggingEvent paxLoggingEvent) {
        return (String) paxLoggingEvent.getProperties().get(BUNDLE_NAME_KEY);
    }

    private String getBundleVersion(PaxLoggingEvent paxLoggingEvent) {
        return (String) paxLoggingEvent.getProperties().get(BUNDLE_VERSION_KEY);
    }

    private Level getLevel(int level) {
        switch (level) {
        case LEVEL_ERROR:
            return Level.ERROR;
        case LEVEL_WARNING:
            return Level.WARN;
        case LEVEL_INFO:
            return Level.INFO;
        case LEVEL_DEBUG:
            return Level.DEBUG;
        case LEVEL_TRACE:
            return Level.TRACE;
        default:
            return Level.UNKNOWN;
        }
    }

    public enum Level {
        TRACE("TRACE"), 
        DEBUG("DEBUG"), 
        INFO("INFO"), 
        WARN("WARN"), 
        ERROR("ERROR"),
        UNKNOWN("UNKNOWN");

        private String level;

        Level(String l) {
            level = l;
        }

        public String getLevel() {
            return level;
        }
    }
}
