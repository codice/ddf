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

import ddf.security.common.audit.SecurityLogger;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

public class CommandAuditer implements EventHandler {
  @Override
  public void handleEvent(Event event) {
    if ("org/apache/karaf/shell/console/EXECUTING".equals(event.getTopic())) {
      String command = (String) event.getProperty("command");
      writeAudit(command);
    }
  }

  void writeAudit(String command) {
    SecurityLogger.audit("Karaf Shell command executed: {}", command);
  }
}
