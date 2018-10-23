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
package org.codice.ddf.sync.installer.impl;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.TimeUnit;
import org.apache.karaf.log.core.LogService;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.codice.ddf.sync.installer.api.SynchronizedInstaller;

@Command(
  scope = "system",
  name = "wait-for-ready",
  description = "Waits for the system to be in a ready state for operations."
)
@Service
public class WaitForReadyCommand implements Action {

  @Option(
    name = "-l",
    aliases = {"--log"},
    description = "Increases log level of wait information.",
    required = false,
    multiValued = false
  )
  boolean increaseLogLevel = false;

  @Option(
    name = "-wt",
    aliases = {"--wait-time"},
    description = "Amount of time to wait for the system to reach a ready state in seconds.",
    required = false,
    multiValued = false
  )
  Integer waitTime = null;

  @Reference SynchronizedInstaller syncInstaller;

  @Reference LogService logService;

  @Override
  public Object execute() throws Exception {
    try {
      AccessController.doPrivileged(
          (PrivilegedExceptionAction<Void>)
              () -> {
                if (increaseLogLevel) {
                  logService.setLevel("org.codice.ddf.sync.installer.impl", "TRACE");
                }
                if (waitTime == null) {
                  syncInstaller.waitForBootFinish();
                } else {
                  syncInstaller.waitForBootFinish(Math.max(0, TimeUnit.SECONDS.toMillis(waitTime)));
                }
                return null;
              });
    } catch (PrivilegedActionException e) {
      throw e.getException();
    }
    return null;
  }
}
