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
package org.codice.ddf.catalog.harvest.common;

import com.hazelcast.core.MapLoader;
import com.hazelcast.core.MapStore;
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.codice.ddf.configuration.AbsolutePathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hazelcast persistence provider implementation of @MapLoader and @MapStore to serialize and
 * persist Java objects stored in Hazelcast cache to disk.
 *
 * <p>NOTE: The usage of object serialization/deserialization may trigger static analysis warnings.
 * This usage is acceptable as the read/write directory is not configurable and lives under
 * DDF_HOME.
 */
public class FileSystemPersistenceProvider
    implements MapLoader<String, Object>, MapStore<String, Object> {

  private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemPersistenceProvider.class);

  private static final String PERSISTED_FILE_SUFFIX = ".ser";

  private static final String PERSISTED_FILE_SUFFIX_REGEX = "\\.ser";

  private final String persistencePath;

  private final String mapName;

  private FilenameFilter filter;

  public FileSystemPersistenceProvider(String mapName) {
    this(mapName, "data");
  }

  public FileSystemPersistenceProvider(String mapName, String persistencePath) {
    this.mapName = mapName;
    this.persistencePath = persistencePath;
    File dir = new File(getPersistencePath());
    if (!dir.exists() && !dir.mkdir()) {
      // TODO audit
      // TODO catch AccessControlException
      throw new IllegalArgumentException(
          String.format("Unable to create persistence directory for [%s]", dir.getAbsoluteFile()));
    }
  }

  /**
   * Retrieve root directory of all persisted Hazelcast objects for this cache. The path is relative
   * to containing bundle, i.e., DDF install directory.
   *
   * @return the path to root directory where serialized objects will be persisted
   */
  String getPersistencePath() {
    return new AbsolutePathResolver(persistencePath).getPath();
  }

  /**
   * Path to where persisted Hazelcast objects will be stored to disk.
   *
   * @return
   */
  String getMapStorePath() {
    return Paths.get(getPersistencePath(), mapName).toString() + File.separator;
  }

  @Override
  public void store(String key, Object value) {
    File dir = new File(getMapStorePath());

    if (new File(getPersistencePath()).canWrite()) {
      boolean created = dir.mkdirs();
      // TODO audit
      // TODO catch AccessControlException
      if (created || dir.exists()) {
        LOGGER.trace("Entering: store - key: {}", key);
        try (OutputStream file = new FileOutputStream(getPersistedFilePathForKey(key));
            // TODO audit
            // TODO catch AccessControlException
            OutputStream buffer = new BufferedOutputStream(file);
            ObjectOutputStream output = new ObjectOutputStream(buffer)) {
          output.writeObject(value);
        } catch (IOException e) {
          LOGGER.debug("IOException storing value in cache with key = {}", key, e);
          LOGGER.warn(
              "Error storing value in cache with key [{}]. Turn logging to debug for more info.",
              key);
          // TODO add details for a system administrator to recover from this condition
        }
      } else {
        LOGGER.debug("Unable to create directory: {}", dir.getAbsolutePath());
      }
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
    File file = new File(getPersistedFilePathForKey(key));
    if (file.exists()) {
      try {
        Files.delete(file.toPath());
        // TODO audit
        // TODO catch AccessControlException
      } catch (IOException e) {
        LOGGER.debug("File was unable to be deleted: {}", file.getAbsolutePath(), e);
        LOGGER.warn(
            "Unable to remove file [{}]. Turn logging to debug for more info.",
            file.getAbsolutePath());
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
    // so we will never have something persisted that is
    // not in memory and want to avoid a performance hit on the file system
    return null;
  }

  @Nullable
  public Object loadFromPersistence(String key) {
    File file = new File(getPersistedFilePathForKey(key));
    if (!file.exists()) {
      return null;
    }

    try (InputStream inputStream = new FileInputStream(getPersistedFilePathForKey(key))) {
      InputStream buffer = new BufferedInputStream(inputStream);
      try (ObjectInput input = new ObjectInputStream(buffer)) {
        return input.readObject();
      }
    } catch (IOException e) {
      LOGGER.debug("IOException", e);
    } catch (ClassNotFoundException e) {
      LOGGER.debug("ClassNotFoundException", e);
    }

    return null;
  }

  @Override
  public Map<String, Object> loadAll(Collection<String> keys) {
    Map<String, Object> values = new HashMap<>();

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
      filter = (file, name) -> name.toLowerCase().endsWith(PERSISTED_FILE_SUFFIX);
    }
    return filter;
  }

  @Override
  public Set<String> loadAllKeys() {
    Set<String> keys = new HashSet<>();

    File[] files = new File(getMapStorePath()).listFiles(getFilenameFilter());
    // TODO catch AccessControlException
    if (files == null) {
      return keys;
    }

    for (File file : files) {
      keys.add(file.getName().replaceFirst(PERSISTED_FILE_SUFFIX_REGEX, ""));
    }
    return keys;
  }

  private String getPersistedFilePathForKey(String key) {
    return getMapStorePath() + key + PERSISTED_FILE_SUFFIX;
  }

  public void clear() {
    File[] files = new File(getMapStorePath()).listFiles(getFilenameFilter());
    if (files != null) {
      for (File file : files) {
        try {
          Files.delete(file.toPath());
          // TODO audit
          // TODO catch AccessControlException
        } catch (IOException e) {
          LOGGER.debug("File was unable to be deleted: {}", file.getAbsolutePath(), e);
          LOGGER.warn(
              "Unable to delete file [{}]. Turn logging to debug for more info.",
              file.getAbsolutePath());
          // TODO add details for a system administrator to recover from this condition
        }
      }
    }
  }
}
