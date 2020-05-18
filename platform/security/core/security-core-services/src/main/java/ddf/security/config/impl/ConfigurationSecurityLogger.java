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
package ddf.security.config.impl;

import ddf.security.audit.SecurityLogger;
import java.security.AccessController;
import java.security.PrivilegedAction;
import javax.security.auth.Subject;
import org.apache.shiro.util.ThreadContext;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.SynchronousConfigurationListener;

public class ConfigurationSecurityLogger implements SynchronousConfigurationListener {

  private SecurityLogger securityLogger;

  @Override
  public final void configurationEvent(ConfigurationEvent event) {
    AccessController.doPrivileged(
        (PrivilegedAction<Void>)
            () -> {
              String type = getType(event);

              // check if there is a subject associated with the configuration change
              if (ThreadContext.getSubject() != null
                  || Subject.getSubject(AccessController.getContext()) != null) {
                securityLogger.audit("Configuration {} for {}.", type, event.getPid());
              } else {
                // there was no subject change was caused by an update to the config file on the
                // filesystem
                securityLogger.auditWarn(
                    "Configuration {} via filesystem for {}.", type, event.getPid());
              }
              return null;
            });
  }

  private String getType(ConfigurationEvent event) {
    switch (event.getType()) {
      case 1:
        return "updated";
      case 2:
        return "deleted";
      case 3:
        return "location changed";
      default:
        return "unknown";
    }
  }

  public void setSecurityLogger(SecurityLogger securityLogger) {
    this.securityLogger = securityLogger;
  }
}
