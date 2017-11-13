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
package org.codice.ddf.admin.insecure.defaults.service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.codice.ddf.system.alerts.NoticePriority;
import org.codice.ddf.system.alerts.SystemNotice;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

public class UsersPropertiesCollector implements Runnable {
  private static final String FILE_FOUND_MSG =
      "A users.properties file was found with default values!";
  private static final String FOLLOW_DOC_MSG =
      "After the deletion of the default users, you will not be able to log back in to the system. Please follow the hardening guide in the documentation under Removing Default Users to avoid the system lock down.";
  private final DefaultUsersDeletionScheduler scheduler;
  private boolean usersPropertiesDeletion;
  private final EventAdmin eventAdmin;

  public UsersPropertiesCollector(EventAdmin eventAdmin, DefaultUsersDeletionScheduler scheduler) {
    this.eventAdmin = eventAdmin;
    this.scheduler = scheduler;
  }

  @Override
  public void run() {
    if (usersPropertiesDeletionOn()
        && DefaultUsersDeletionScheduler.getUsersPropertiesFilePath().toFile().exists()
        && scheduler.defaultUsersExist()) {
      boolean deletionScheduled = scheduler.scheduleDeletion();

      if (deletionScheduled) {
        eventAdmin.postEvent(
            new Event(
                SystemNotice.SYSTEM_NOTICE_BASE_TOPIC + "userProperties",
                createUsersPropertiesNotice().getProperties()));
      }
    } else {
      scheduler.deleteScheduledDeletions();
    }
  }

  public SystemNotice createUsersPropertiesNotice() {
    Instant firstInstall = scheduler.installationDate();
    Set<String> details = new HashSet<>();
    String timestampTitle = "";
    String timestampDetail = "";

    if (firstInstall != null) {
      Instant timestamp = firstInstall.plus(Duration.ofDays(3));

      DateTimeFormatter formatter =
          DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
              .withLocale(Locale.getDefault())
              .withZone(ZoneId.systemDefault());

      String timestampString = formatter.format(timestamp);
      timestampTitle = String.format("before %s ", timestampString);
      timestampDetail = String.format(" on %s ", timestampString);
    }

    String title =
        String.format(
            "USERS.PROPERTIES FILE FOUND WITH DEFAULT USERS! Follow the instructions below %sto prevent the system from locking down.",
            timestampTitle);

    details.add(
        String.format(
            "%s They will be automatically deleted%s. %s",
            FILE_FOUND_MSG, timestampDetail, FOLLOW_DOC_MSG));

    return new SystemNotice(this.getClass().getName(), NoticePriority.CRITICAL, title, details);
  }

  public boolean usersPropertiesDeletionOn() {
    return usersPropertiesDeletion;
  }

  public void setUsersPropertiesDeletion(boolean usersPropertiesDeletion) {
    this.usersPropertiesDeletion = usersPropertiesDeletion;
  }
}
