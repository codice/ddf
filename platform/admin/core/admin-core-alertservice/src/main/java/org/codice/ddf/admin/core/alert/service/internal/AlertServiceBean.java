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
package org.codice.ddf.admin.core.alert.service.internal;

import static org.apache.commons.lang.Validate.notNull;
import static org.codice.ddf.admin.core.alert.service.data.internal.Alert.ALERT_EVENT_ALERT_PROPERTY;
import static org.codice.ddf.admin.core.alert.service.data.internal.Alert.ALERT_EVENT_TOPIC_VALUE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.codice.ddf.admin.core.alert.service.data.internal.Alert;
import org.codice.ddf.admin.insecure.defaults.service.InsecureDefaultsService;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AlertServiceBean} allows {@link Alert}s to be retrieved/dismissed by an MBean endpoint and
 * handles {@link Alert} {@link Event}s pushed to the {@link org.osgi.service.event.EventAdmin}.
 */
public class AlertServiceBean implements AlertServiceBeanMBean, EventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlertServiceBean.class);

    private static final String CLASS_NAME = AlertServiceBean.class.getName();

    protected static final String INSECURE_DEFAULTS_ALERT_TITLE =
            "The system is insecure because default configuration values are in use.";

    private final MBeanServer mBeanServer;

    private final InsecureDefaultsService insecureDefaultsService = new InsecureDefaultsService();

    private ObjectName objectName;

    private Map<String, Alert> addedAlerts = new HashMap<>();

    public AlertServiceBean(MBeanServer mBeanServer) throws MalformedObjectNameException {
        notNull(mBeanServer, "mBeanServer may not be null");
        this.mBeanServer = mBeanServer;
        objectName = new ObjectName(OBJECT_NAME);
    }

    public void init() {
        registerMBean();
        LOGGER.debug("Registered [{}] MBean under object name: [{}].",
                CLASS_NAME,
                objectName.toString());
    }

    public void destroy() {
        if (objectName != null && mBeanServer != null) {
            unregisterMBean();
            LOGGER.debug("Unregistered [{}] MBean.", CLASS_NAME);
        }
    }

    @Override
    @Nonnull
    public List<Map<String, Object>> getAlerts() {
        List<Alert> currentAlerts = new ArrayList<>();

        // add InsecureDefaultsService alerts
        List<String> insecureDefaultsAlertDetails = insecureDefaultsService.validate()
                .stream()
                .map(org.codice.ddf.admin.insecure.defaults.service.Alert::getMessage)
                .collect(Collectors.toList());
        if (!insecureDefaultsAlertDetails.isEmpty()) {
            final Alert insecureDefaultsAlert = new Alert(Alert.Level.ERROR,
                    INSECURE_DEFAULTS_ALERT_TITLE,
                    insecureDefaultsAlertDetails);
            currentAlerts.add(insecureDefaultsAlert);
        }

        // add alerts from the EventService
        currentAlerts.addAll(addedAlerts.values());

        return currentAlerts.stream()
                .map(AlertServiceBean::toMap)
                .collect(Collectors.toList());
    }

    @Override
    @Nonnull
    public Boolean dismissAlert(@Nonnull String key) {
        final Alert dismissedAlert = addedAlerts.remove(key);
        if (dismissedAlert == null) {
            LOGGER.debug("Failed to dismiss alert with key {}", key);
            return false;
        } else {
            LOGGER.debug("Dismissed alert with title=\"{}\" and key=\"{}\"",
                    dismissedAlert.getTitle(),
                    key);
            return true;
        }
    }

    /**
     * Adds an {@link Alert} to the collection of current {@link Alert}s. The even must have topic
     * {@link Alert#ALERT_EVENT_TOPIC_VALUE} and
     * event.getProperty({@link Alert#ALERT_EVENT_ALERT_PROPERTY}) is the {@link Alert} to add.
     * <p>
     * Already-added {@link Alert}s with the same key as the {@link Alert} in the event will be
     * overridden. For example, an application may overwrite an existing Alert with the timestamp of
     * when an issue most-recently occurred by specifying the key of the {@link Alert} to overwrite.
     *
     * @param event
     */
    @Override
    public void handleEvent(Event event) {
        if (event != null && ALERT_EVENT_TOPIC_VALUE.equals(event.getTopic())) {
            final Object alertObject = event.getProperty(ALERT_EVENT_ALERT_PROPERTY);
            if (alertObject instanceof Alert) {
                final Alert alert = (Alert) alertObject;
                String key = alert.getKey();

                // added {@link Alert}s must have a key because they must be dismissable
                if (key == null) {
                    key = UUID.randomUUID()
                            .toString()
                            .replaceAll("-", "");
                    alert.setKey(key);
                }

                addedAlerts.put(key, alert);
            }
        }
    }

    private void registerMBean() {
        if (!mBeanServer.isRegistered(objectName)) {
            try {
                mBeanServer.registerMBean(this, objectName);
            } catch (InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException e) {
                String message = String.format(
                        "Unable to register [%s] MBean under object name: [%s].",
                        CLASS_NAME,
                        objectName.toString());
                LOGGER.debug(message, e);
                throw new RuntimeException(message, e);
            }
        } else {
            LOGGER.debug("[{}] MBean is already registered under object name: [{}].",
                    CLASS_NAME,
                    objectName.toString());
        }
    }

    private void unregisterMBean() {
        try {
            if (mBeanServer.isRegistered(objectName)) {
                mBeanServer.unregisterMBean(objectName);
            } else {
                LOGGER.debug(
                        "Unable to unregister [{}] MBean under object name: [{}]. It is not registered.",
                        CLASS_NAME,
                        objectName.toString());
            }
        } catch (MBeanRegistrationException | InstanceNotFoundException e) {
            String message = String.format(
                    "Unable to unregister [%s] MBean under object name: [%s].",
                    CLASS_NAME,
                    objectName.toString());
            LOGGER.debug(message, e);
            throw new RuntimeException(message, e);
        }
    }

    /**
     * Converts an {@link Alert} to a map that callers of the {@link #getAlerts} method can
     * understand
     */
    private static Map<String, Object> toMap(Alert alert) {
        Map<String, Object> alertMap = new HashMap<>();

        // Alert key may be null
        final String key = alert.getKey();
        if (key != null) {
            alertMap.put("key", alert.getKey());
        }

        alertMap.put("level", getLevelString(alert));
        alertMap.put("title", alert.getTitle());
        alertMap.put("details", alert.getDetails());
        return alertMap;
    }

    /**
     * Maps the enums to existing Admin Console styles
     */
    private static String getLevelString(Alert alert) {
        switch (alert.getLevel()) {
        case INFO:
            return "info";
        case WARN:
            return "warning";
        default:
            return "danger";
        }
    }
}