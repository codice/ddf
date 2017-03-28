/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.security.common.audit;

import static org.apache.commons.lang.Validate.notNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.codice.ddf.admin.core.alert.service.data.internal.Alert;
import org.ops4j.pax.logging.spi.PaxAppender;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

/**
 * The {@link AdminAlertAppender} is configured as a backup logger to the audit logger. The
 * {@link AdminAlertAppender} will send an {@link Alert} {@link Event} on each failed logging event
 * that will be handled by the
 * {@link org.codice.ddf.admin.core.alert.service.internal.AlertServiceBean}.
 */
public final class AdminAlertAppender implements PaxAppender {

    private final EventAdmin eventAdmin;

    public AdminAlertAppender(EventAdmin eventAdmin) {
        notNull(eventAdmin, "eventAdmin may not be null");
        this.eventAdmin = eventAdmin;
    }

    @Override
    public void doAppend(PaxLoggingEvent event) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(Alert.ALERT_EVENT_ALERT_PROPERTY,
                new Alert(this.getClass()
                        .getCanonicalName(),
                        Alert.Level.ERROR,
                        "Audit logging has failed.",
                        new ArrayList<>()));
        eventAdmin.postEvent(new Event(Alert.ALERT_EVENT_TOPIC_VALUE, properties));
    }
}