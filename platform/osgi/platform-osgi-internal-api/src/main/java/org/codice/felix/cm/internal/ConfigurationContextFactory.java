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

import org.osgi.service.cm.Configuration;

/**
 * Provides a way to transform {@link Configuration}s into {@link ConfigurationContext} objects for
 * consistent processing despite changing Felix internals.
 */
public interface ConfigurationContextFactory {

  /**
   * Transform the {@link Configuration} into a {@link ConfigurationContext}.
   *
   * @param configuration the {@link Configuration} to convert.
   * @return a {@link ConfigurationContext} safe for internal config admin usage.
   */
  ConfigurationContext createContext(Configuration configuration);
}
