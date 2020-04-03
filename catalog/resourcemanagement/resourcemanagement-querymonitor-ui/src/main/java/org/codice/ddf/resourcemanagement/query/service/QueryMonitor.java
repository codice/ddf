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
package org.codice.ddf.resourcemanagement.query.service;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.codice.ddf.resourcemanagement.query.plugin.ActiveSearch;
import org.codice.ddf.resourcemanagement.query.plugin.QueryMonitorPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryMonitor implements QueryMonitorMBean {

  private static final Logger LOGGER = LoggerFactory.getLogger(QueryMonitor.class);

  public static final String USER = "user";

  public static final String SOURCE_ID = "sourceId";

  public static final String QUERY = "query";

  public static final String DATE_STARTED = "dateStarted";

  public static final String UUID_PROPERTY = "uuid";

  private ObjectName objectName;

  private MBeanServer mBeanServer;

  private QueryMonitorPlugin queryMonitorPlugin;

  private static final String DATE_FORMAT = "dd MMM yyyy hh:mm:ss";

  private static final FastDateFormat FAST_DATE_FORMAT = FastDateFormat.getInstance(DATE_FORMAT);

  public QueryMonitor(QueryMonitorPlugin queryMonitorPlugin) {
    this.queryMonitorPlugin = queryMonitorPlugin;
    registerMbean();
  }

  private void registerMbean() {
    try {
      objectName = new ObjectName(QueryMonitor.class.getName() + ":service=querymonitor");
      mBeanServer = ManagementFactory.getPlatformMBeanServer();
    } catch (MalformedObjectNameException e) {
      LOGGER.info("Unable to create Query Monitor Configuration MBean.", e);
    }
    if (mBeanServer == null) {
      return;
    }
    try {
      try {
        mBeanServer.registerMBean(this, objectName);
        LOGGER.debug(
            "Registered Query Monitor Configuration MBean under object name: {}", objectName);
      } catch (InstanceAlreadyExistsException e) {
        // Try to remove and re-register
        mBeanServer.unregisterMBean(objectName);
        mBeanServer.registerMBean(this, objectName);
        LOGGER.debug("Re-registered Query Monitor Configuration MBean");
      }
    } catch (MBeanRegistrationException
        | InstanceNotFoundException
        | InstanceAlreadyExistsException
        | NotCompliantMBeanException e) {
      LOGGER.info("Could not register MBean [{}].", objectName.toString(), e);
    }
  }

  private static final Function<ActiveSearch, Map<String, String>> ACTIVE_SEARCH_MAP_FUNCTION =
      (activeSearch) -> {
        Map<String, String> map = new HashMap<>();
        map.put(USER, activeSearch.getClientInfo());
        map.put(SOURCE_ID, activeSearch.getSource().getId());
        map.put(QUERY, activeSearch.getCQL());

        Date date = activeSearch.getStartTime();
        map.put(DATE_STARTED, FAST_DATE_FORMAT.format(date));
        map.put(UUID_PROPERTY, activeSearch.getUniqueID().toString());
        return map;
      };

  @Override
  public List<Map<String, String>> activeSearches() {
    Map<UUID, ActiveSearch> map = queryMonitorPlugin.getActiveSearches();
    if (MapUtils.isNotEmpty(map)) {
      return queryMonitorPlugin.getActiveSearches().values().stream()
          .map(ACTIVE_SEARCH_MAP_FUNCTION)
          .collect(Collectors.toList());
    }
    return new ArrayList<>();
  }

  @Override
  public void cancelActiveSearch(String uuid) {
    UUID uniqueId = UUID.fromString(uuid);
    queryMonitorPlugin.removeActiveSearch(uniqueId);
  }
}
