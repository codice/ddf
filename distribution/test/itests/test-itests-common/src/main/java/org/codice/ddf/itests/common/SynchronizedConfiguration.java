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

import static org.junit.Assert.fail;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.Callable;
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

    long timeoutLimit = System.currentTimeMillis() + CONFIG_UPDATE_MAX_WAIT_MILLIS;
    boolean retried = false;
    while (true) {
      if (configCallable.call()) {
        break;
      } else {
        // this is a hack to retry the configuration since it sometimes fails
        // if it still keeps failing then we should remove this
        if (!retried
            && System.currentTimeMillis() > timeoutLimit - CONFIG_UPDATE_MAX_WAIT_MILLIS / 2) {
          config.update(new Hashtable<>(configProps));
          retried = true;
        }
        if (System.currentTimeMillis() > timeoutLimit) {
          fail(String.format("Timed out waiting for configuration change for %s", pid));
        } else {
          Thread.sleep(LOOP_SLEEP_MILLIS);
        }
      }
    }

    return oldProps;
  }
}
