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
package org.codice.ddf.security.common;

import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FailedLoginDelayer {
  private static final Logger LOGGER = LoggerFactory.getLogger(FailedLoginDelayer.class);

  private static final int TIMEOUT = 2;

  public void delay(String username) {
    try {
      LOGGER.debug("Failed login for {}; sleeping for {} seconds\n", username, TIMEOUT);
      TimeUnit.SECONDS.sleep(TIMEOUT);
    } catch (InterruptedException e1) {
      LOGGER.debug("Error sleeping for failed login attempt");

      Thread.currentThread().interrupt();
    }
  }
}
