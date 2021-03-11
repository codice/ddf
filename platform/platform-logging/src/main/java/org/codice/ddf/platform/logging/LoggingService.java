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

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Lists;
import java.util.List;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import org.ops4j.pax.logging.spi.PaxAppender;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A custom {@link org.ops4j.pax.logging.spi.PaxAppender} which receives {@link
 * org.ops4j.pax.logging.spi.PaxLoggingEvent}s and is the Jolokia endpoint for the Logging UI in the
 * Admin Console.
 */
public class LoggingService implements PaxAppender, LoggingServiceMBean {

  private static final Logger LOGGER = LoggerFactory.getLogger(LoggingService.class);

  private static final String CLASS_NAME = LoggingService.class.getName();

  private static final String MBEAN_OBJECT_NAME = CLASS_NAME + ":service=logging-service";

  private static final int MAX_LOG_EVENTS_LIMIT = 5000;

  private static final int DEFAULT_LOG_EVENTS_LIMIT = 500;

  private final MBeanServer mBeanServer;

  private ObjectName objectName;

  private EvictingQueue<LogEvent> logEvents;

  private int maxLogEvents = DEFAULT_LOG_EVENTS_LIMIT;

  /**
   * Constructor
   *
   * @param mBeanServer object used to register this object as an MBean
   */
  public LoggingService(MBeanServer mBeanServer) {
    this.mBeanServer = mBeanServer;
  }

  public void init()
      throws MBeanRegistrationException, MalformedObjectNameException,
          InstanceAlreadyExistsException, InstanceNotFoundException, NotCompliantMBeanException {
    try {
      if (logEvents == null) {
        logEvents = EvictingQueue.create(DEFAULT_LOG_EVENTS_LIMIT);
      }
      objectName = new ObjectName(MBEAN_OBJECT_NAME);
      mBeanServer.registerMBean(this, objectName);
      LOGGER.debug("Registered [{}] MBean under object name: [{}].", CLASS_NAME, objectName);
    } catch (InstanceAlreadyExistsException e) {
      LOGGER.debug("[{}] already registered as an MBean. Re-registering.", CLASS_NAME);

      mBeanServer.unregisterMBean(objectName);
      mBeanServer.registerMBean(this, objectName);

      LOGGER.debug("Successfully re-registered [{}] as an MBean.", CLASS_NAME);
    }
  }

  public void destroy() {
    try {
      if (objectName != null && mBeanServer != null) {
        mBeanServer.unregisterMBean(objectName);
        LOGGER.debug("Unregistered Logging Service MBean");
      }
    } catch (InstanceNotFoundException | MBeanRegistrationException e) {
      LOGGER.info("Exception unregistering MBean [{}].", objectName, e);
    }
  }

  /**
   * Called each time a {@link org.ops4j.pax.logging.spi.PaxLoggingEvent} is created in the system
   */
  @Override
  public void doAppend(PaxLoggingEvent paxLoggingEvent) {
    add(new LogEvent(paxLoggingEvent));
  }

  @Override
  public synchronized List<LogEvent> retrieveLogEvents() {
    return Lists.newArrayList(logEvents);
  }

  /**
   * Sets the maximum number of {@link LogEvent}s to store
   *
   * @param newMaxLogEvents This number cannot be less than 0 or greater than {@code
   *     MAX_LOG_EVENTS_LIMIT}. In the event that this parameter is less than 0 or greater than
   *     {@code MAX_LOG_EVENTS_LIMIT}, the maximum log events stored will set set to {@code
   *     MAX_LOG_EVENTS_LIMIT}.
   */
  public synchronized void setMaxLogEvents(int newMaxLogEvents) {
    if (newMaxLogEvents <= 0 || newMaxLogEvents > MAX_LOG_EVENTS_LIMIT) {
      String message =
          String.format(
              "An invalid value of [%d] was entered for maximum log events to store. This "
                  + "value must be greater than 0 and must not exceed [%d]. Unable to reset maximum log"
                  + " events to store.",
              newMaxLogEvents, MAX_LOG_EVENTS_LIMIT);
      throw new IllegalArgumentException(message);
    }

    EvictingQueue<LogEvent> evictingQueue = createNewEvictingQueue(newMaxLogEvents);
    this.maxLogEvents = newMaxLogEvents;
    logEvents = evictingQueue;
  }

  public synchronized int getMaxLogEvents() {
    return maxLogEvents;
  }

  private synchronized void add(LogEvent logEvent) {
    logEvents.add(logEvent);
  }

  private EvictingQueue<LogEvent> createNewEvictingQueue(int newMaxLogEvents) {
    EvictingQueue<LogEvent> evictingQueue = EvictingQueue.create(newMaxLogEvents);
    evictingQueue.addAll(logEvents);
    return evictingQueue;
  }
}
