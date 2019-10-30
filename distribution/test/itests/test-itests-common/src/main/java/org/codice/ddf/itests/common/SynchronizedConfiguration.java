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
package org.codice.ddf.itests.common;

import static org.awaitility.Awaitility.await;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class SynchronizedConfiguration {

  private static final long CONFIG_UPDATE_MAX_WAIT_MILLIS =
      AbstractIntegrationTest.GENERIC_TIMEOUT_MILLISECONDS;

  private static final int LOOP_SLEEP_MILLIS = 5;

  private final String pid;

  private final String location;

  private final Map<String, Object> configProps;

  private final Callable<Boolean> configCallable;

  private ConfigurationAdmin configAdmin;

  public SynchronizedConfiguration(
      String pid,
      String location,
      Map<String, Object> configProps,
      Callable<Boolean> configCallable,
      ConfigurationAdmin configAdmin) {
    this.pid = pid;
    this.location = location;
    this.configProps = configProps;
    this.configCallable = configCallable;
    this.configAdmin = configAdmin;
  }

  public final Dictionary<String, Object> updateConfig() throws Exception {
    Configuration config = configAdmin.getConfiguration(pid, location);
    Dictionary<String, Object> oldProps = config.getProperties();
    config.update(new Hashtable<>(configProps));

    await("Waiting for configuration change")
        .atMost(CONFIG_UPDATE_MAX_WAIT_MILLIS, TimeUnit.MILLISECONDS)
        .pollDelay(LOOP_SLEEP_MILLIS, TimeUnit.MILLISECONDS)
        .until(configCallable);

    return oldProps;
  }
}
