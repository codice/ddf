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
package ddf.catalog.cache.impl;

import com.hazelcast.core.MapLoader;
import com.hazelcast.core.MapStoreFactory;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is an implementation of the Hazelcast @MapStoreFactory, providing a way to dynamically
 * create a @FileSystemPersistenceProvider for persisting cached objects to disk. This class is
 * specified in the Hazelcast XML config file (platform-hazelcast.xml) in the <map-store> node.
 */
public class FileSystemMapStoreFactory implements MapStoreFactory<String, Object> {

  private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemMapStoreFactory.class);

  private static final String PERSISTENCE_PATH_KEY = "storage";

  @Override
  public MapLoader<String, Object> newMapStore(String mapName, Properties properties) {
    String persistencePath = properties.getProperty(PERSISTENCE_PATH_KEY);
    LOGGER.trace(
        "INSIDE: newMapStore(). Creating new {} for map {} stored in {}",
        FileSystemPersistenceProvider.class.getName(),
        mapName,
        persistencePath);
    return new FileSystemPersistenceProvider(mapName, persistencePath);
  }
}
