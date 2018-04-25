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
package org.codice.ddf.security.boot;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.karaf.system.SystemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecureBoot {

  private static final Logger LOGGER = LoggerFactory.getLogger(SecureBoot.class);

  private static final String DDF_HOME_SYS_PROP = "ddf.home";

  private static final String USER_HOME_SYS_PROP = "user.home";

  private static final int INSECURE_BOOT_EXIT_CODE = 15;

  private static final String NOW = "0";

  private final SystemService systemService;

  private Path ddfHome;

  private Path userHome;

  private boolean shuttingDown = false;

  public SecureBoot(SystemService systemService) {
    this.systemService = systemService;
    try {
      ddfHome = getDdfHome();
      userHome = getUserHome();
    } catch (IOException e) {
      String message =
          "Unable to determine if your installation ["
              + ddfHome
              + "] is located in an insecure directory. See log for details. Shutting down...";
      System.err.println("\n" + message + "\n");
      LOGGER.error(message, e);
      shutdown();
    }
  }

  public void init() {
    if (isInsecureInstallation()) {
      String message =
          "ERROR: Your installation ["
              + ddfHome
              + "] is located in an insecure directory. Installations cannot be located in ["
              + userHome
              + "]. Shutting down...";

      System.err.println("\n" + message + "\n");
      LOGGER.error(message);
      shutdown();
    }
  }

  private boolean isInsecureInstallation() {
    return !shuttingDown && securityManagerEnabled() && isInstalledInUserHome();
  }

  private Path getDdfHome() throws IOException {
    return Paths.get(System.getProperty(DDF_HOME_SYS_PROP)).toRealPath();
  }

  private Path getUserHome() throws IOException {
    return Paths.get(System.getProperty(USER_HOME_SYS_PROP)).toRealPath();
  }

  private boolean isInstalledInUserHome() {
    return ddfHome.startsWith(userHome);
  }

  private void shutdown() {
    try {
      shuttingDown = true;
      systemService.halt(NOW);
    } catch (Exception e) {
      systemExit(e);
    }
  }

  @VisibleForTesting
  boolean securityManagerEnabled() {
    return System.getSecurityManager() != null;
  }

  @VisibleForTesting
  void systemExit(Exception e) {
    String message =
        "ERROR: Exception encountered while shutting system down via SystemService. Terminating JVM...";
    System.err.println("\n" + message + "\n");
    LOGGER.error(message, e);
    System.exit(INSECURE_BOOT_EXIT_CODE);
  }
}
