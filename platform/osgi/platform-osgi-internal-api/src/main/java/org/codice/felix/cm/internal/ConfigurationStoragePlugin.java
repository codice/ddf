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
import java.util.Dictionary;
import java.util.Enumeration;

/**
 * The following interface is an exact copy of the PersistenceManager contract offered by Apache
 * Felix. Because they ship their interfaces with their implementation, we cannot use their
 * interface in our API because we can't depend on implementations.
 *
 * <p>This is a stop gap measure until the issue can be addressed. Refer to the Felix interface
 * PersistenceManager for appropriate documentation.
 *
 * <p>Unlike the Felix version of the Persistence Manager, ours extends {@link
 * ConfigurationInitializable}.
 *
 * <p>There can only be one {@link ConfigurationStoragePlugin} in use at a time. The one that gets
 * used for storing configuration data is whichever one was most recently registered. See the
 * important assumption in {@link ConfigurationInitializable} for how swapping back and forth
 * between two instances of {@link ConfigurationStoragePlugin} should handle remote state.
 */
public interface ConfigurationStoragePlugin extends ConfigurationInitializable {

  boolean exists(String pid);

  Dictionary load(String pid) throws IOException;

  Enumeration getDictionaries() throws IOException;

  void store(String pid, Dictionary properties) throws IOException;

  void delete(String pid) throws IOException;
}
