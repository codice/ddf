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
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.codice.ddf.security.handler.api.SessionHandler;

@Service
@Command(scope = "security", name = "endsession", description = "Deletes an active SAML session.")
public class EndSessionCommand implements Action {
  @Reference private SessionHandler sessionHandler;

  @Argument(
      name = "subjectName",
      description = "The name of the subject attached to the SAML session to be deleted.",
      required = true)
  private String subjectName;

  @Override
  @SuppressWarnings("squid:S106" /* Output to Karaf console */)
  public Object execute() throws Exception {
    sessionHandler.invalidateSession(subjectName);
    PrintStream console = System.out;

    console.printf("Asynchronous backchannel logout request sent for subject [%s]%n", subjectName);
    return null;
  }
}
