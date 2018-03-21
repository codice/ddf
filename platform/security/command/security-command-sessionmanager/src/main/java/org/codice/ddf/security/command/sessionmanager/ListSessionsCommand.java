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
package org.codice.ddf.security.command.sessionmanager;

import java.io.PrintStream;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.codice.ddf.security.handler.api.SessionHandler;

@Service
@Command(scope = "security", name = "listsessions", description = "Lists all active SAML sessions.")
public class ListSessionsCommand implements Action {

  @Reference private SessionHandler sessionHandler;

  @SuppressWarnings("squid:S106" /* System.out for Karaf console */)
  @Override
  public Object execute() throws Exception {
    final Map<String, Set<String>> activeSessions = sessionHandler.getActiveSessions();
    PrintStream console = System.out;

    console.println();
    for (Entry<String, Set<String>> row : activeSessions.entrySet()) {
      console.printf("%s - active SPs: %s%n", row.getKey(), row.getValue());
    }
    console.println();
    return null;
  }
}
