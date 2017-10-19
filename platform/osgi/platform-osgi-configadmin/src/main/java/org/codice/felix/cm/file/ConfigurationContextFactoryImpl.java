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
package org.codice.felix.cm.file;

import org.codice.felix.cm.internal.ConfigurationContext;
import org.codice.felix.cm.internal.ConfigurationContextFactory;
import org.osgi.service.cm.Configuration;

/**
 * Default factory implementation.
 *
 * <p><b>See FELIX-4005 & FELIX-4556. This class cannot utilize Java 8 language constructs due to
 * maven bundle plugin 2.3.7</b>
 */
public class ConfigurationContextFactoryImpl implements ConfigurationContextFactory {
  @Override
  public ConfigurationContext createContext(Configuration configuration) {
    if (configuration == null) {
      throw new IllegalArgumentException("Configuration cannot be null");
    }
    if (configuration.getProperties() == null) {
      throw new IllegalArgumentException("Properties cannot be null");
    }
    return new ConfigurationContextImpl(configuration);
  }
}
