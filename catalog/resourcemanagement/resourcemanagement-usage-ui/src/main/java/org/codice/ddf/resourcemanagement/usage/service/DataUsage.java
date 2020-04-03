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
package org.codice.ddf.resourcemanagement.usage.service;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.collections.MapUtils;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentItem;
import org.codice.ddf.persistence.PersistentStore;
import org.codice.ddf.persistence.attributes.AttributesStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataUsage extends RouteBuilder implements DataUsageMBean {

  private static final Logger LOGGER = LoggerFactory.getLogger(DataUsage.class);

  // 23:30 Daily
  public static final String DEFAULT_CRON_TIME = "0+30+23+*+*+?";

  public static final String CRON_TIME_ID_KEY = "data-usage-cron-key";

  public static final String CRON_TIME_KEY = "cron_time";

  public static final String ID = "id";

  public static final String TXT_PREFIX = "_txt";

  private static final String PREFERENCES_TYPE = "preferences";

  private static final String LNG_PREFIX = "_lng";

  private AttributesStore attributesStore;

  private PersistentStore persistentStore;

  private ObjectName objectName;

  private MBeanServer mBeanServer;

  private String cronTime;

  private ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

  public DataUsage(AttributesStore attributesStore, PersistentStore persistentStore) {
    this.attributesStore = attributesStore;
    this.persistentStore = persistentStore;
    registerMbean();
  }

  public void init() {
    this.cronTime = getPersistentCronTime();
    LOGGER.debug("Set Cron Time as : {}", this.cronTime);
  }

  private String getPersistentCronTime() {
    try {
      List<Map<String, Object>> mapList;
      try {
        readWriteLock.readLock().lock();
        mapList = persistentStore.get(PREFERENCES_TYPE);
      } finally {
        readWriteLock.readLock().unlock();
      }
      if (mapList == null) {
        throw new PersistenceException("Unable to get Cron Time.");
      }
      for (Map<String, Object> preference : mapList) {
        String id = (String) preference.get(ID + TXT_PREFIX);
        if (CRON_TIME_ID_KEY.equals(id)) {
          return (String) preference.get(CRON_TIME_KEY + TXT_PREFIX);
        }
      }
    } catch (PersistenceException e) {
      LOGGER.error("Unable to get Cron Time from Persistent Store.", e);
    }
    return DEFAULT_CRON_TIME;
  }

  private void setPersistentCronTime() {
    PersistentItem persistentItem = new PersistentItem();
    persistentItem.addIdProperty(CRON_TIME_ID_KEY);
    persistentItem.addProperty(CRON_TIME_KEY, this.cronTime);

    try {
      readWriteLock.readLock().lock();
      persistentStore.add(PREFERENCES_TYPE, persistentItem);

    } catch (PersistenceException e) {
      LOGGER.error("Error adding Cron Time to Persistent Store.", e);
    } finally {
      readWriteLock.readLock().unlock();
    }
  }

  private void registerMbean() {
    try {
      objectName = new ObjectName(DataUsage.class.getName() + ":service=datausage");
      mBeanServer = ManagementFactory.getPlatformMBeanServer();
    } catch (MalformedObjectNameException e) {
      LOGGER.error("Unable to create Data Usage Configuration MBean.", e);
    }
    if (mBeanServer == null) {
      return;
    }
    try {
      try {
        mBeanServer.registerMBean(this, objectName);
        LOGGER.info("Registered Data Usage Configuration MBean under object name: {}", objectName);
      } catch (InstanceAlreadyExistsException e) {
        // Try to remove and re-register
        mBeanServer.unregisterMBean(objectName);
        mBeanServer.registerMBean(this, objectName);
        LOGGER.info("Re-registered Data Usage Configuration MBean");
      }
    } catch (MBeanRegistrationException
        | InstanceNotFoundException
        | InstanceAlreadyExistsException
        | NotCompliantMBeanException e) {
      LOGGER.error("Could not register MBean [{}].", objectName.toString(), e);
    }
  }

  @Override
  public Map<String, List<Long>> userMap() {
    Map<String, List<Long>> dataUsageMap = new HashMap<>();
    try {

      attributesStore.getAllUsers().stream()
          .filter(MapUtils::isNotEmpty)
          .forEach(
              stringObjectMap -> {
                if (MapUtils.isNotEmpty(stringObjectMap)) {
                  dataUsageMap.put(
                      (String) stringObjectMap.get(AttributesStore.USER_KEY + TXT_PREFIX),
                      Arrays.asList(
                          (Long) stringObjectMap.get(AttributesStore.DATA_USAGE_KEY + LNG_PREFIX),
                          (Long)
                              stringObjectMap.get(
                                  AttributesStore.DATA_USAGE_LIMIT_KEY + LNG_PREFIX)));
                }
              });

    } catch (PersistenceException e) {
      LOGGER.debug("Unable to get persistent users from the AttributesStore.", e);
    }
    return dataUsageMap;
  }

  @Override
  public void updateUserDataLimit(Map<String, Long> userMap) {
    LOGGER.debug("Updating user data limit : {}", userMap);

    for (Map.Entry<String, Long> entry : userMap.entrySet()) {
      long dataLimit = entry.getValue();
      String username = entry.getKey();
      try {
        attributesStore.setDataLimit(username, dataLimit);
      } catch (PersistenceException e) {
        LOGGER.warn("Unable to update properties for {}.", username, e);
      }
    }
  }

  @Override
  public void updateCronTime(String cronTime) {
    this.cronTime = parseCronTime(cronTime);
    setPersistentCronTime();
    try {
      configure();
    } catch (Exception e) {
      LOGGER.warn("Unable to update Cron Time.");
    }
  }

  private String parseCronTime(String cronTime) {
    String result;
    if (cronTime.contains(":")) {
      String[] time = cronTime.split(":");
      result = "0+" + time[1] + "+" + time[0] + "+*+*+?";
      LOGGER.debug("Setting new cron time : {}.", result);
    } else {
      result = DEFAULT_CRON_TIME;
      LOGGER.warn(
          "Unable to parse cron time from : {}.  Using Default Cron Time : {}.", cronTime, result);
    }
    return result;
  }

  @Override
  public String cronTime() {
    String[] cronSplit = cronTime.split("\\+");
    String time = cronSplit[2] + ":" + cronSplit[1];
    return time;
  }

  @Override
  public void configure() {
    from("quartz://dataUsage/dataUsageResetTimer?cron=" + cronTime + "&stateful=true")
        .errorHandler(
            loggingErrorHandler(DataUsage.class.getCanonicalName()).level(LoggingLevel.ERROR))
        .process(
            (Exchange exchange) -> {
              LOGGER.info(
                  "Resetting Data Usages for all users in {}.",
                  AttributesStore.class.getCanonicalName());
              attributesStore.resetUserDataUsages();
            });
  }

  public void destroy() {
    try {
      if (objectName != null && mBeanServer != null) {
        mBeanServer.unregisterMBean(objectName);
      }
    } catch (Exception e) {
      LOGGER.warn("Exception unregistering MBean: ", e);
    }
  }
}
