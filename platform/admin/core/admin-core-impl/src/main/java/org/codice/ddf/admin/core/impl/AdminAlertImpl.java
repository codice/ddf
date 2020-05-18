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
package org.codice.ddf.admin.core.impl;

import com.google.common.html.HtmlEscapers;
import ddf.security.SubjectOperations;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.management.NotCompliantMBeanException;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.codice.ddf.admin.core.api.jmx.AdminAlertMBean;
import org.codice.ddf.log.sanitizer.LogSanitizer;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentItem;
import org.codice.ddf.persistence.PersistentStore;
import org.codice.ddf.system.alerts.Alert;
import org.codice.ddf.system.alerts.NoticePriority;
import org.codice.ddf.system.alerts.SystemNotice;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AdminAlertImpl extends BasicMBean implements AdminAlertMBean {

  private static final Logger LOGGER = LoggerFactory.getLogger(AdminAlertImpl.class);

  private final String activeAlertQuery;

  private final PersistentStore persistentStore;

  private final EventAdmin eventAdmin;

  private SubjectOperations subjectOperations;

  public AdminAlertImpl(PersistentStore persistentStore, EventAdmin eventAdmin)
      throws NotCompliantMBeanException {
    super(AdminAlertMBean.class, AdminAlertMBean.OBJECT_NAME);
    this.persistentStore = persistentStore;
    this.eventAdmin = eventAdmin;
    activeAlertQuery = String.format("status='%s'", Alert.ALERT_ACTIVE_STATUS);
  }

  @Override
  public List<Map<String, Object>> getAlerts() {
    List<Map<String, Object>> alerts = new ArrayList<>();
    try {
      List<Map<String, Object>> results = persistentStore.get("alerts", activeAlertQuery);
      if (!results.isEmpty()) {
        for (Map<String, Object> item : results) {
          item = PersistentItem.stripSuffixes(item);
          // this check and type change is needed because of how the persistent store handles
          // strings/sets of strings. Single value sets will come back as strings.
          if (item.get(SystemNotice.SYSTEM_NOTICE_DETAILS_KEY) instanceof String) {
            item.put(
                SystemNotice.SYSTEM_NOTICE_DETAILS_KEY,
                Collections.singleton(item.get(SystemNotice.SYSTEM_NOTICE_DETAILS_KEY)));
          }
          escapeStrings(item);
          alerts.add(item);
        }
      }
    } catch (PersistenceException pe) {
      LOGGER.debug("Error retrieving system alert.", pe);

      return Collections.singletonList(
          new Alert(
                  "unable_to_retrieve_alerts",
                  NoticePriority.CRITICAL,
                  "Persistent Storage Not Responding. Could Not Retrieve Alerts.",
                  Collections.singleton(
                      "Critical alerts may be present, but not displayed because the persistent storage is not responding."))
              .getProperties());
    }
    alerts.sort(
        (map1, map2) ->
            new Alert(map1).getPriority().value() >= new Alert(map2).getPriority().value()
                ? -1
                : 1);
    return alerts;
  }

  private void escapeStrings(Map<String, Object> map) {
    for (Map.Entry<String, Object> entry : map.entrySet()) {
      if (entry.getValue() instanceof String) {
        entry.setValue(HtmlEscapers.htmlEscaper().escape((String) entry.getValue()));
      }
    }
    Set<String> details =
        (Set<String>) map.getOrDefault(Alert.SYSTEM_NOTICE_DETAILS_KEY, new HashSet<String>());
    Set<String> escapedStrings = new HashSet<>();
    for (String detail : details) {
      escapedStrings.add(HtmlEscapers.htmlEscaper().escape(detail));
    }
    map.put(Alert.SYSTEM_NOTICE_DETAILS_KEY, escapedStrings);
  }

  @Override
  public void dismissAlert(String id) {
    if (StringUtils.isEmpty(id)) {
      LOGGER.debug("Cannot dismiss alert without an id");
      return;
    }

    if (subjectOperations == null) {
      throw new IllegalStateException("Unable to resolve user's name.");
    }

    String subjectName = subjectOperations.getName(SecurityUtils.getSubject());
    if (subjectName == null) {
      LOGGER.debug("No Subject Name! Could not dismiss alert {}", LogSanitizer.sanitize(id));
      return;
    }

    Map<String, String> props = new HashMap<>();
    props.put(Alert.SYSTEM_NOTICE_ID_KEY, id);
    props.put(Alert.ALERT_DISMISSED_BY, subjectName);
    eventAdmin.postEvent(new Event(Alert.ALERT_DISMISS_TOPIC, props));
  }

  public void setSubjectOperations(SubjectOperations subjectOperations) {
    this.subjectOperations = subjectOperations;
  }
}
