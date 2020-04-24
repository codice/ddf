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
package org.codice.ddf.security.command.listener;

import ddf.security.audit.SecurityLogger;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

public class CommandAuditer implements EventHandler {

  private SecurityLogger securityLogger;

  @Override
  public void handleEvent(Event event) {
    if (event.getTopic().equals("org/apache/karaf/shell/console/EXECUTING")) {
      String command = (String) event.getProperty("command");
      writeAudit(command);
    }
  }

  void writeAudit(String command) {
    securityLogger.audit("Karaf Shell command executed: {}", command);
  }

  public void setSecurityLogger(SecurityLogger securityLogger) {
    this.securityLogger = securityLogger;
  }
}
