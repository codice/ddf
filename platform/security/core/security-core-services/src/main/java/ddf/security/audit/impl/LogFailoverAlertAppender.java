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
package ddf.security.audit.impl;

import static org.apache.commons.lang.Validate.notNull;

import java.util.Arrays;
import java.util.HashSet;
import org.codice.ddf.system.alerts.NoticePriority;
import org.codice.ddf.system.alerts.SystemNotice;
import org.joda.time.DateTime;
import org.ops4j.pax.logging.spi.PaxAppender;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;

public final class LogFailoverAlertAppender implements PaxAppender {

  private final EventAdmin eventAdmin;

  private DateTime lastUpdated;

  private HashSet<String> details =
      new HashSet<>(
          Arrays.asList(
              "Writing to the backup audit log has failed. This could occur if there are insufficient permissions to write to the log directory, a full disk, or the primary or backup log configurations are incorrect."));

  public LogFailoverAlertAppender(EventAdmin eventAdmin) {
    notNull(eventAdmin, "eventAdmin may not be null");
    this.eventAdmin = eventAdmin;
  }

  @Override
  public void doAppend(PaxLoggingEvent event) {
    if (lastUpdated == null) {
      lastUpdated = DateTime.now();
    } else if (DateTime.now().isBefore(lastUpdated.plusSeconds(5))) {
      // don't spam the admin console when multiple log messages come in at once
      return;
    }

    SystemNotice notice =
        new SystemNotice(
            LogFailoverAlertAppender.class.toString(),
            NoticePriority.CRITICAL,
            "Audit Logging Failure",
            details);
    eventAdmin.postEvent(
        new Event(SystemNotice.SYSTEM_NOTICE_BASE_TOPIC.concat("audit"), notice.getProperties()));
    lastUpdated = DateTime.now();
  }
}
