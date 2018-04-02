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

import java.util.Set;

/**
 * Base configuration contract that supports receiving a snapshot of all configurations.
 *
 * <p>If this configuration plugin writes to external services, it is assumed that implementations
 * properly re-initialize the service so configuration drift does not occur as plugins are turned on
 * and off repeatedly. This also includes cleaning up the service's last impression of this system's
 * configuration.
 */
public interface ConfigurationInitializable {

  /**
   * Provide an initialization hook for synchronous resource allocation <b>after</b> a plugin is
   * available to be called, unlike an OSGi life-cycle {@code init()} method, which is called prior
   * to the service becoming available.
   *
   * @param state the current configuration state of the system.
   */
  void initialize(Set<ConfigurationContext> state);
}
