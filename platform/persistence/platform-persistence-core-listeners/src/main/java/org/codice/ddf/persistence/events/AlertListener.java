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
package org.codice.ddf.persistence.events;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentItem;
import org.codice.ddf.persistence.PersistentStore;
import org.codice.ddf.system.alerts.Alert;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AlertListener is a kind of Decanter alerter. It listens for alerts on the decanter/alert/* topic
 * and persists the alerts to solr. Alerts from the same host and source are merged into one solr
 * entry.
 */
public class AlertListener implements EventHandler {

  private static final String CORE_NAME = "alerts";

  private static final Logger LOGGER = LoggerFactory.getLogger(AlertListener.class);

  private PersistentStore persistentStore;

  private String hostName;

  public AlertListener(PersistentStore persistentStore) {
    this.persistentStore = persistentStore;
  }

  public void init() throws UnknownHostException {
    this.hostName = InetAddress.getLocalHost().getHostName();
  }

  /**
   * Processes alert events and stores them in the persistent store. Because this handler 'squashes'
   * alerts into one entry it needs to be synchronized to be thread safe.
   *
   * @param event The osgi alert event
   * @throws IllegalArgumentException
   */
  @Override
  public synchronized void handleEvent(Event event) throws IllegalArgumentException {
    if (Alert.ALERT_DISMISS_TOPIC.equals(event.getTopic())) {
      dismissAlert(event);
      return;
    }
    // need source to lookup previous alerts
    if (event.getProperty(Alert.SYSTEM_NOTICE_SOURCE_KEY) == null) {
      return;
    }
    LOGGER.debug("Received alert event on topic {}", event.getTopic());

    Alert alert = getAlertFromEvent(event);
    Alert existingAlert = getAlertBySource(alert.getSource());
    if (existingAlert != null) {
      Map<String, Object> properties = alert.getProperties();
      properties.put(Alert.SYSTEM_NOTICE_ID_KEY, existingAlert.getId());
      properties.put(Alert.ALERT_COUNT, existingAlert.getCount() + 1);
      properties.put(Alert.SYSTEM_NOTICE_TIME_KEY, existingAlert.getTime());
      alert = new Alert(properties);
    }

    addAlertToStore(alert);
  }

  private void addAlertToStore(Alert alert) {
    PersistentItem item = new PersistentItem();
    alert.getProperties().forEach((name, value) -> item.addProperty(name, value));
    try {
      persistentStore.add(CORE_NAME, item);
    } catch (PersistenceException e) {
      LOGGER.error("Failed to persist alert.");
    }
  }

  private Alert getAlertFromEvent(Event event) {
    Map<String, Object> properties = new HashMap<>();
    for (String name : event.getPropertyNames()) {
      properties.put(name, event.getProperty(name));
    }
    return new Alert(properties);
  }

  // returns null if no results found
  private Alert getAlertBySource(String source) {
    return getSingleAlertFromStore(
        String.format(
            "%s = '%s' AND %s = '%s' AND %s = '%s'",
            Alert.SYSTEM_NOTICE_SOURCE_KEY,
            source,
            Alert.ALERT_STATUS,
            Alert.ALERT_ACTIVE_STATUS,
            Alert.SYSTEM_NOTICE_HOST_NAME_KEY,
            hostName));
  }

  // returns null if no results found
  private Alert getAlertById(String id) {
    return getSingleAlertFromStore(String.format("'id' = '%s'", id));
  }

  // returns null if no results found
  private Alert getSingleAlertFromStore(String cql) {
    List<Alert> alerts = getAlertFromStore(cql);
    if (!alerts.isEmpty()) {
      if (alerts.size() > 1) {
        LOGGER.warn("Found {} store entries when only one was expected", alerts.size());
      }
      return alerts.get(0);
    }
    return null;
  }

  private List<Alert> getAlertFromStore(String cql) {
    List<Alert> alerts = new ArrayList<>();
    try {
      alerts =
          persistentStore.get(CORE_NAME, cql).stream()
              .map(item -> new Alert(PersistentItem.stripSuffixes(item)))
              .collect(Collectors.toList());

    } catch (PersistenceException pe) {
      LOGGER.error("Error retrieving system alert.", pe);
    }
    return alerts;
  }

  private void dismissAlert(Event dismissEvent) {
    String id = (String) dismissEvent.getProperty(Alert.SYSTEM_NOTICE_ID_KEY);
    if (id == null) {
      return;
    }

    Alert alert = getAlertById(id);
    if (alert == null) {
      LOGGER.debug("Could not find alert {} for dismissal.", id);
      return;
    }

    String dismissedBy = (String) dismissEvent.getProperty(Alert.ALERT_DISMISSED_BY);
    if (dismissedBy == null) {
      LOGGER.debug(
          "Could not dismiss alert {} because the {} property was not provided.",
          id,
          Alert.ALERT_DISMISSED_BY);
      return;
    }

    addAlertToStore(alert.getDismissedAlert(dismissedBy));
  }
}
