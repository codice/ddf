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
package org.codice.ddf.system.alerts;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * Alert is the property map class used by DDF for alerting events. When events are received on
 * the Decanter topic 'decanter/alert/*' by the AlertListener they are converted to this class for
 * storage and dissemination. This class defines all the common keys used in alerts events that are not
 * included in the SystemNotice class and provides convenient helpers for accessing them.
 */
public class Alert extends SystemNotice {
    //topics
    public static final String ALERT_DISMISS_TOPIC = "ddf/alert/dismiss";

    //alert specific constants
    public static final String ALERT_EVENT_TYPE = "system-alert";

    /**
     * Key for the date/time when this alert was last updated
     */
    public static final String ALERT_LAST_UPDATED = "last-updated";

    /**
     * Key for the integer count indicating how many times this alert has occurred
     */
    public static final String ALERT_COUNT = "count";

    /**
     * Key for the status of this alert, either acitve or dismissed
     */
    public static final String ALERT_STATUS = "status";

    /**
     * Key for the data/time this alert was dismissed
     */
    public static final String ALERT_DISMISSED_TIME = "dismissed-time";

    /**
     * Key for the user who dismissed the alert
     */
    public static final String ALERT_DISMISSED_BY = "dismissed-by";

    public static final String ALERT_DISMISSED_STATUS = "dismissed";

    public static final String ALERT_ACTIVE_STATUS = "active";

    public Alert() {
        super();
    }

    public Alert(String source, NoticePriority priority, String title, Set<String> details) {
        super(source, priority, title, details);
    }

    public Alert(Map<String, Object> map) {
        super(map);
    }

    @Override
    protected void init() {
        super.init();
        properties.put(EVENT_TYPE_KEY, ALERT_EVENT_TYPE);
        setCount(1L);
        setLastUpdated(new Date());
        setStatus(ALERT_ACTIVE_STATUS);
    }

    public Date getLastUpdated() {
        return (Date) properties.get(ALERT_LAST_UPDATED);
    }

    public Long getCount() {
        return (Long) properties.get(ALERT_COUNT);
    }

    public String getStatus() {
        return (String) properties.get(ALERT_STATUS);
    }

    public Date getDismissedTime() {
        return (Date) properties.get(ALERT_DISMISSED_TIME);
    }

    public String getDismissedBy() {
        return (String) properties.get(ALERT_DISMISSED_BY);
    }

    private void setStatus(String status) {
        safePut(ALERT_STATUS, status);
    }

    private void setLastUpdated(Date time) {
        safePut(ALERT_LAST_UPDATED, time);
    }

    private void setCount(Long count) {
        safePut(ALERT_COUNT, count);
    }

    private void setDismissedTime(Date time) {
        safePut(ALERT_DISMISSED_TIME, time);
    }

    private void setDismissedBy(String dismissedBy) {
        safePut(ALERT_DISMISSED_BY, dismissedBy);
    }

    public Alert getDismissedAlert(String user) {
        Alert dismissedAlert = new Alert(properties);
        dismissedAlert.setStatus(Alert.ALERT_DISMISSED_STATUS);
        dismissedAlert.setDismissedTime(Date.from(Instant.now()));
        dismissedAlert.setDismissedBy(user);
        return dismissedAlert;
    }
}