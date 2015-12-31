/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.notifications.impl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.core.MapLoader;
import com.hazelcast.core.MapStore;

/**
 * Hazelcast persistence provider implementation of @MapLoader and @MapStore to serialize and
 * persist Java objects stored in Hazelcast cache to disk.
 *
 */
public class FileSystemPersistenceProvider
        implements MapLoader<String, Object>, MapStore<String, Object> {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(FileSystemPersistenceProvider.class);

    private static final String PERSISTED_FILE_SUFFIX = ".ser";

    private static final String PERSISTED_FILE_SUFFIX_REGEX = "\\.ser";

    private String mapName = "default";

    private FilenameFilter filter;

    public FileSystemPersistenceProvider() {
    }

    public FileSystemPersistenceProvider(String mapName) {
        LOGGER.debug("INSIDE: FileSystemPersistenceProvider constructor,  mapName = {}", mapName);
        this.mapName = mapName;
        File dir = new File(getPersistencePath());
        if (!dir.exists()) {
            if (!dir.mkdir()) {
                LOGGER.warn("Unable to create directory: {}", dir.getAbsolutePath());
            }
        }
    }

    /**
     * Retrieve root directory of all persisted Hazelcast objects for this cache. The path is
     * relative to containing bundle, i.e., DDF install directory.
     *
     * @return the path to root directory where serialized objects will be persisted
     */
    String getPersistencePath() {
        return "data/";
    }

    /**
     * Path to where persisted Hazelcast objects will be stored to disk.
     *
     * @return
     */
    String getMapStorePath() {
        return getPersistencePath() + mapName + "/";
    }

    @Override
    public void store(String key, Object value) {
        OutputStream file = null;
        ObjectOutputStream output = null;
        try {
            File dir = new File(getMapStorePath());
            if (!dir.exists()) {
                if (!dir.mkdir()) {
                    LOGGER.warn("Unable to create directory: {}", dir.getAbsolutePath());
                }
            }
            file = new FileOutputStream(getMapStorePath() + key + PERSISTED_FILE_SUFFIX);
            OutputStream buffer = new BufferedOutputStream(file);
            output = new ObjectOutputStream(buffer);
            output.writeObject(value);
        } catch (IOException e) {
            LOGGER.info("IOException storing value in cache with key = " + key, e);
        } finally {
            IOUtils.closeQuietly(output);
            IOUtils.closeQuietly(file);
        }
    }

    @Override
    public void storeAll(Map<String, Object> keyValueMap) {
        for (Map.Entry<String, Object> entry : keyValueMap.entrySet()) {
            store(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void delete(String key) {
        File file = new File(getMapStorePath() + key + PERSISTED_FILE_SUFFIX);
        if (file.exists()) {
            if (!file.delete()) {
                LOGGER.warn("File was unable to be deleted: {}", file.getAbsolutePath());
            }
        }
    }

    @Override
    public void deleteAll(Collection<String> keys) {
        for (String key : keys) {
            delete(key);
        }
    }

    @Override
    public Object load(String key) {
        // Not implemented because the Hazelcast data grid is all in cache,
        // so never have something persisted that is
        // not in memory and want to avoid a performance hit on the file system
        return null;
    }

    Object loadFromPersistence(String key) {
        File file = new File(getMapStorePath() + key + PERSISTED_FILE_SUFFIX);
        if (!file.exists()) {
            return null;
        }
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(getMapStorePath() + key + PERSISTED_FILE_SUFFIX);
            InputStream buffer = new BufferedInputStream(inputStream);
            ObjectInput input = new ObjectInputStream(buffer);
            return (Object) input.readObject();
        } catch (IOException e) {
            LOGGER.debug("IOException", e);
        } catch (ClassNotFoundException e) {
            LOGGER.debug("ClassNotFoundException", e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
        return null;
    }

    @Override
    public Map<String, Object> loadAll(Collection<String> keys) {
        Map<String, Object> values = new HashMap<String, Object>();

        for (String key : keys) {
            Object obj = loadFromPersistence(key);
            if (obj != null) {
                values.put(key, obj);
            }
        }
        return values;
    }

    private FilenameFilter getFilenameFilter() {
        if (filter == null) {
            filter = new FilenameFilter() {
                @Override
                public boolean accept(File file, String name) {
                    return name.toLowerCase()
                            .endsWith(PERSISTED_FILE_SUFFIX);
                }
            };
        }
        return filter;
    }

    @Override
    public Set<String> loadAllKeys() {
        Set<String> keys = new HashSet<String>();

        File[] files = new File(getMapStorePath()).listFiles(getFilenameFilter());
        if (files == null) {
            return keys;
        }

        for (File file : files) {
            keys.add(file.getName()
                    .replaceFirst(PERSISTED_FILE_SUFFIX_REGEX, ""));
        }
        return keys;
    }

    public void clear() {
        File[] files = new File(getMapStorePath()).listFiles(getFilenameFilter());
        if (files != null) {
            for (File file : files) {
                if (!file.delete()) {
                    LOGGER.warn("File was unable to be deleted: {}", file.getAbsolutePath());
                }
            }
        }
    }
}
