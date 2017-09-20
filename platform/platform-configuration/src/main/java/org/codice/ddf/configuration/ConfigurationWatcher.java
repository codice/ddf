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
package org.codice.ddf.configuration;

import java.util.Map;

/**
 * This interface is used to specify a source as a watcher of updates to the DDF system
 * configuration settings. Whenever the source is configured, or updates are made to the DDF System
 * Settings, the source will receive the entire list of the most current DDF system settings.
 *
 * <p>It is up to the ConfigurationWatcher to determine which DDF system settings are of interest,
 * if their values have changed, and how to react to their values.
 *
 * @deprecated As of 2.8.0, replaced by using system properties. See {@link
 *     org.codice.ddf.configuration.SystemBaseUrl} and {@link
 *     org.codice.ddf.configuration.SystemInfo}
 */
public interface ConfigurationWatcher {

  /**
   * Invoked by the ConfigurationManager when the DDF System Settings are modified. The Map of
   * configuration properties contains the entire list of system settings, not just the ones that
   * have changed.
   *
   * @param configuration the entire list of DDF system settings
   * @deprecated As of 2.8.0, replaced by using system properties. See {@link
   *     org.codice.ddf.configuration.SystemBaseUrl} and {@link
   *     org.codice.ddf.configuration.SystemInfo}
   */
  public void configurationUpdateCallback(Map<String, String> configuration);
}
