/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.cache.impl;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.MapLoader;
import com.hazelcast.core.MapStoreFactory;

/**
 * This class is an implementation of the Hazelcast @MapStoreFactory, providing
 * a way to dynamically create a @FileSystemPersistenceProvider for persisting
 * cached objects to disk. This class is specified in the Hazelcast XML config
 * file (platform-hazelcast.xml) in the <map-store> node. 
 *
 */
public class FileSystemMapStoreFactory implements MapStoreFactory<String, Object> {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemMapStoreFactory.class);

    @Override
    public MapLoader<String, Object> newMapStore(String mapName, Properties properties) {
        LOGGER.info("INSIDE: newMapStore()");
        return new FileSystemPersistenceProvider(mapName);
    }

}
