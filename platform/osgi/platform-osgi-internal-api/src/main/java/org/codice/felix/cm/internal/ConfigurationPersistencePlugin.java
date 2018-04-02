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
package org.codice.felix.cm.internal;

import java.io.IOException;

/**
 * Provides synchronous interceptors for working with configuration data prior to writing or
 * deleting, and can influence the results of the config operations.
 *
 * <p>Interacting with a {@link Configuration} object at the {@link ConfigurationPersistencePlugin}
 * (or similar) level has restrictions and side effects:
 *
 * <ul>
 *   <li>The configuration may not be created yet
 *   <li>The configuration's properties might be stale
 * </ul>
 *
 * Implementations of these plugins should <b>never</b> attempt to make calls back into {@link
 * org.osgi.service.cm.ConfigurationAdmin} but instead should utilize an API-provided {@link
 * ConfigurationContext} to perform allowed configuration operations.
 */
public interface ConfigurationPersistencePlugin extends ConfigurationInitializable {

  /**
   * Given the {@link ConfigurationContext} of a configuration about to be written, perform
   * processing <b>before</b> the results get stored.
   *
   * @param context an object of relevant info that is safe to access during a config operation.
   * @throws IOException to abort the configuration update operation and indicate to the user that
   *     it failed.
   * @throws IllegalStateException if configuration data disappeared or felix internal data was
   *     otherwise corrupt.
   */
  void handleStore(ConfigurationContext context) throws IOException;

  /**
   * Given the pid of a configuration about to be deleted, perform processing <b>before</b> the
   * configuration is deleted.
   *
   * @param pid the service pid identifying the config object that will be deleted.
   * @throws IOException to abort the configuration delete operation and indicate to the user that
   *     it failed.
   */
  void handleDelete(String pid) throws IOException;
}
