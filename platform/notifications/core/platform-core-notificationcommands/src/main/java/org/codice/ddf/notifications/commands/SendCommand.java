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
package org.codice.ddf.notifications.commands;

import java.util.UUID;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.codice.ddf.notifications.Notification;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Command(scope = "notifications", name = "send", description = "Send notification(s).")
public class SendCommand implements Action {

  public static final String SERVICE_PID = "org.osgi.service.event.EventAdmin";

  private static final Logger LOGGER = LoggerFactory.getLogger(ListCommand.class);

  private static final int DEFAULT_NUMBER_OF_NOTIFICATIONS = 1;

  private static final String DEFAULT_NOTIFICATION_MESSAGE = "Default notification message";

  private static final String DEFAULT_NOTIFICATION_APPLICATION = "console-send-cmd";

  private static final String DEFAULT_NOTIFICATION_TITLE = "Default Title";

  private static final int DEFAULT_WAIT_TIME_BETWEEN_NOTIFICATIONS = 0;

  @Reference BundleContext bundleContext;

  @Argument(
    name = "User ID",
    description = "User ID to send notifications to. ",
    index = 0,
    multiValued = false,
    required = true
  )
  String userId = null;

  @Argument(
    name = "NUMBER_OF_ITEMS",
    description = "Number of notifications to send.",
    index = 1,
    multiValued = false,
    required = false
  )
  int numberOfNotifications = DEFAULT_NUMBER_OF_NOTIFICATIONS;

  @Option(
    name = "Application",
    required = false,
    aliases = {"-a"},
    multiValued = false,
    description = "The name of application that sent the notification."
  )
  String application = DEFAULT_NOTIFICATION_APPLICATION;

  @Option(
    name = "Title",
    required = false,
    aliases = {"-t"},
    multiValued = false,
    description = "The title of the notification."
  )
  String title = DEFAULT_NOTIFICATION_TITLE;

  @Option(
    name = "Message",
    required = false,
    aliases = {"-m"},
    multiValued = false,
    description = "The detailed message to be sent in the notification."
  )
  String message = DEFAULT_NOTIFICATION_MESSAGE;

  @Option(
    name = "Wait Time",
    required = false,
    aliases = {"-w"},
    multiValued = false,
    description =
        "FOR TESTING ONLY: The amount of seconds to wait between sending each notification."
  )
  int waitTime = DEFAULT_WAIT_TIME_BETWEEN_NOTIFICATIONS;

  @Override
  public Object execute() throws Exception {

    for (int i = 0; i < numberOfNotifications; i++) {
      sendNotification();
      if (waitTime > 0) {
        try {
          Thread.sleep(waitTime * 1000);
        } catch (InterruptedException e) {

        }
      }
    }

    return null;
  }

  private void sendNotification() throws Exception {
    Long sysTimeMillis = System.currentTimeMillis();
    String id = UUID.randomUUID().toString().replaceAll("-", "");
    String sessionId = "mockSessionId";
    Notification notification =
        new Notification(id, sessionId, application, title, message, sysTimeMillis, userId);

    notification.put("status", "Started");
    notification.put("bytes", "12345");

    Event event = new Event(Notification.NOTIFICATION_TOPIC_DOWNLOADS, notification);

    // Get OSGi Event Admin service
    EventAdmin eventAdmin;
    @SuppressWarnings("rawtypes")
    ServiceReference[] serviceReferences = bundleContext.getServiceReferences(SERVICE_PID, null);

    if (serviceReferences == null || serviceReferences.length != 1) {
      LOGGER.debug("Found no service references for {}", SERVICE_PID);
    } else {
      LOGGER.debug("Found " + serviceReferences.length + " service references for " + SERVICE_PID);

      eventAdmin = (EventAdmin) bundleContext.getService(serviceReferences[0]);
      if (eventAdmin != null) {
        eventAdmin.postEvent(event);
      }
    }
  }
}
