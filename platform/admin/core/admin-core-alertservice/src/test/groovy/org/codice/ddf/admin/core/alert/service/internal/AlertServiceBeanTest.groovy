/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.admin.core.alert.service.internal

import org.codice.ddf.admin.core.alert.service.data.internal.Alert
import org.osgi.service.event.Event
import spock.lang.Specification

import javax.management.MalformedObjectNameException
import javax.management.ObjectName
import java.lang.management.ManagementFactory

class AlertServiceBeanTest extends Specification {

    final mbeanName = new ObjectName(AlertServiceBean.class.getName() + ":service=alert-service")

    def alertServiceBean

    void setup() throws Exception {
        // the keystore path must be set for the InsecureDefaultsService to use
        System.setProperty("javax.net.ssl.keyStore", "TestKeystorePath")
        alertServiceBean = new AlertServiceBean(ManagementFactory.getPlatformMBeanServer())
        assert !isRegistered()
        assert alertServiceBean.addedAlerts.isEmpty()
    }

    void cleanup() throws Exception {
        if (isRegistered()) {
            ManagementFactory.getPlatformMBeanServer()
                    .unregisterMBean(mbeanName)
        }
    }

    def 'test init'() throws Exception {
        when:
            alertServiceBean.init()

        then:
            isRegistered()
    }

    def 'test init when the bean has already been registered'() throws Exception {
        given:
            alertServiceBean.init()

        when:
            alertServiceBean.init()

        then:
            isRegistered()
    }

    def 'test destroy'() throws Exception {
        given:
            alertServiceBean.init()

        when:
            alertServiceBean.destroy()

        then:
            !isRegistered()
    }

    def 'test destroy when the bean has not already been registered'() throws Exception {
        when:
            alertServiceBean.destroy()

        then:
            !isRegistered()
    }

    private boolean isRegistered() throws MalformedObjectNameException {
        return ManagementFactory.getPlatformMBeanServer()
                .isRegistered(mbeanName)
    }

    def 'test getAlerts with the default InsecureDefaultsService alerts'() {
        when:
            final alerts = alertServiceBean.getAlerts()

        then:
            alerts.size() == 1
            verifyInsecureDefaultsServiceDefaultAlerts(alerts)
    }

    def 'test handling an invalid alert event'(event) {
        given:
            alertServiceBean.handleEvent(event)

        when:
            final alerts = alertServiceBean.getAlerts()

        then:
            alerts.size() == 1
            verifyInsecureDefaultsServiceDefaultAlerts(alerts)

        where:
            event << [new Event("myevent", [_: _]), new Event(Alert.ALERT_EVENT_TOPIC_VALUE, [_: _]), createAlertEvent(_), null]
    }

    def 'test adding an alert with a key'() {
        given:
            final key = _ as String
            final alert = new Alert(key, Alert.Level.ERROR, _ as String, [_])

        when:
            alertServiceBean.handleEvent(createAlertEvent(alert))

        then:
            alertServiceBean.addedAlerts.size() == 1

            final alerts = alertServiceBean.getAlerts()
            alerts.size() == 2
            verifyInsecureDefaultsServiceDefaultAlerts(alerts)

            final addedAlert = alerts[1]
            assertMappedAlert(addedAlert, alert)

    }

    def 'test adding an alert with the same key'() {
        given:
            final key = _ as String
            final alert1 = new Alert(key, Alert.Level.ERROR, _ as String, [_])
            alertServiceBean.handleEvent(createAlertEvent(alert1))
            final alert2 = new Alert(key, Alert.Level.INFO, _ as String, [_])

        when:
            alertServiceBean.handleEvent(createAlertEvent(alert2))

        then:
            alertServiceBean.addedAlerts.size() == 1

            final List<Map<String, Object>> alerts = alertServiceBean.getAlerts()
            alerts.size() == 2
            verifyInsecureDefaultsServiceDefaultAlerts(alerts)

            !alerts.contains(alert1)
            final addedAlert = alerts[1]
            assertMappedAlert(addedAlert, alert2)
    }

    def 'test adding an alert without a key'() {
        given:
            final alert = new Alert(Alert.Level.ERROR, _ as String, [_])

        when:
            alertServiceBean.handleEvent(createAlertEvent(alert))

        then:
            alertServiceBean.addedAlerts.size() == 1

            final alerts = alertServiceBean.getAlerts()
            alerts.size() == 2
            verifyInsecureDefaultsServiceDefaultAlerts(alerts)

            final addedAlert = alerts[1]
            assertMappedAlert(addedAlert, alert)
    }

    def 'test adding multiple mixed alerts'() {
        given:
            final alert1 = new Alert("1", Alert.Level.ERROR, _ as String, [_])
            final alert2 = new Alert(Alert.Level.INFO, _ as String, [_])
            final alert3 = new Alert("3", Alert.Level.ERROR, _ as String, [_])
            final alert4 = new Alert("4", Alert.Level.ERROR, _ as String, [_])
            final alert5 = new Alert(Alert.Level.WARN, _ as String, [_])
            final alert4Overwrite = new Alert("4", Alert.Level.WARN, _ as String, [_])

        when:
            alertServiceBean.handleEvent(createAlertEvent(alert1))
            alertServiceBean.handleEvent(new Event(Alert.ALERT_EVENT_TOPIC_VALUE, [_: _]))
            alertServiceBean.handleEvent(new Event(Alert.ALERT_EVENT_TOPIC_VALUE, [_: _]))
            alertServiceBean.handleEvent(createAlertEvent(alert2))
            alertServiceBean.handleEvent(createAlertEvent(alert3))
            alertServiceBean.handleEvent(createAlertEvent(alert4))
            alertServiceBean.handleEvent(createAlertEvent(alert5))
            alertServiceBean.handleEvent(createAlertEvent(alert4Overwrite))
            alertServiceBean.handleEvent(new Event("myevent", [_: _]))

        then:
            alertServiceBean.addedAlerts.size() == 5

            final List<Map<String, Object>> alerts = alertServiceBean.getAlerts()
            alerts.size() == 6
            verifyInsecureDefaultsServiceDefaultAlerts(alerts)
    }

    def 'test dismissing an alert'() {
        given:
            final key = "key"
            alertServiceBean.addedAlerts = [(key): Mock(Alert)]

        when:
            assert alertServiceBean.dismissAlert(key)

        then:
            alertServiceBean.addedAlerts.isEmpty()

            final alerts = alertServiceBean.getAlerts()
            alerts.size() == 1
            verifyInsecureDefaultsServiceDefaultAlerts(alerts)
    }

    def 'test dismissing an alert that wasn\'t already added'() {
        given:
            alertServiceBean.addedAlerts = [(_ as String): Mock(Alert)]

        expect:
            assert !alertServiceBean.dismissAlert("testKey")
    }

    private static void verifyInsecureDefaultsServiceDefaultAlerts(alerts) {
        final insecureDefaultsAlert = alerts.get(0)

        assert insecureDefaultsAlert.get("key") == null
        assert insecureDefaultsAlert.get("level") == "danger"
        assert insecureDefaultsAlert.get("title") == AlertServiceBean.INSECURE_DEFAULTS_ALERT_TITLE
        assert insecureDefaultsAlert.get("details").size() > 0
    }

    private static Event createAlertEvent(alert) {
        return new Event(Alert.ALERT_EVENT_TOPIC_VALUE, [(Alert.ALERT_EVENT_ALERT_PROPERTY): alert])
    }

    private static boolean assertMappedAlert(Map<String, Object> alertAsMap, Alert alert) {
        final String alertMapKey = alertAsMap.get("key")
        if (alert.getKey() == null) {
            // assert that the key was set to a random UUID
            assert alertMapKey != null
        } else {
            alertMapKey == alert.getKey()
        }
        assert alertAsMap.get("level") == AlertServiceBean.getLevelString(alert)
        assert alertAsMap.get("title") == alert.getTitle()
        assert alertAsMap.get("details") == alert.getDetails()

        return true
    }
}