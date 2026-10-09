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
package org.codice.ddf.catalog.content.monitor;

import static ddf.catalog.Constants.CDM_LOGGER_NAME;

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
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.codice.ddf.configuration.AbsolutePathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serializes Java objects to disk, one file per key, under a named directory in DDF_HOME/data.
 *
 * <p>NOTE: The usage of object serialization/deserialization may trigger static analysis warnings.
 * This usage is acceptable as the read/write directory is not configurable and lives under
 * DDF_HOME.
 */
public class FileSystemPersistenceProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(CDM_LOGGER_NAME);

  private static final String PERSISTED_FILE_SUFFIX = ".ser";

  private static final String PERSISTED_FILE_SUFFIX_REGEX = "\\.ser";

  private String mapName = "default";

  private FilenameFilter filter;

  public FileSystemPersistenceProvider() {}

  public FileSystemPersistenceProvider(String mapName) {
    LOGGER.trace("INSIDE: FileSystemPersistenceProvider constructor,  mapName = {}", mapName);
    this.mapName = mapName;
    File dir = new File(getPersistencePath());
    if (!dir.exists()) {
      if (!dir.mkdir()) {
        LOGGER.debug("Unable to create directory: {}", dir.getAbsolutePath());
      }
    }
  }

  /**
   * Retrieve root directory of all persisted objects. The path is relative to containing bundle,
   * i.e., DDF install directory.
   *
   * @return the path to root directory where serialized objects will be persisted
   */
  String getPersistencePath() {
    return new AbsolutePathResolver("data").getPath();
  }

  /**
   * Path to where persisted objects for this map will be stored to disk.
   *
   * @return
   */
  String getMapStorePath() {
    return Paths.get(getPersistencePath(), mapName).toString() + File.separator;
  }

  public void store(String key, Object value) {
    File dir = new File(getMapStorePath());
    if (!dir.exists() && !dir.mkdir()) {
      LOGGER.debug("Unable to create directory: {}", dir.getAbsolutePath());
    }
    LOGGER.trace("Entering: store - key: {}", key);
    try (OutputStream file = new FileOutputStream(getMapStorePath() + key + PERSISTED_FILE_SUFFIX);
        OutputStream buffer = new BufferedOutputStream(file);
        ObjectOutputStream output = new ObjectOutputStream(buffer)) {
      output.writeObject(value);
    } catch (IOException e) {
      LOGGER.debug("IOException storing value in cache with key = " + key, e);
    }
  }

  public void storeAll(Map<String, Object> keyValueMap) {
    for (Map.Entry<String, Object> entry : keyValueMap.entrySet()) {
      store(entry.getKey(), entry.getValue());
    }
  }

  public void delete(String key) {
    File file = new File(getMapStorePath() + key + PERSISTED_FILE_SUFFIX);
    if (file.exists()) {
      if (!file.delete()) {
        LOGGER.debug("File was unable to be deleted: {}", file.getAbsolutePath());
      }
    }
  }

  public void deleteAll(Collection<String> keys) {
    for (String key : keys) {
      delete(key);
    }
  }

  public Object load(String key) {
    // Not implemented. Callers read from disk via loadFromPersistence or loadAll.
    return null;
  }

  Object loadFromPersistence(String key) {
    File file = new File(getMapStorePath() + key + PERSISTED_FILE_SUFFIX);
    if (!file.exists()) {
      return null;
    }

    try (InputStream inputStream =
        new FileInputStream(getMapStorePath() + key + PERSISTED_FILE_SUFFIX)) {
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
      filter =
          new FilenameFilter() {
            @Override
            public boolean accept(File file, String name) {
              return name.toLowerCase().endsWith(PERSISTED_FILE_SUFFIX);
            }
          };
    }
    return filter;
  }

  public Set<String> loadAllKeys() {
    Set<String> keys = new HashSet<String>();

    File[] files = new File(getMapStorePath()).listFiles(getFilenameFilter());
    if (files == null) {
      return keys;
    }

    for (File file : files) {
      keys.add(file.getName().replaceFirst(PERSISTED_FILE_SUFFIX_REGEX, ""));
    }
    return keys;
  }

  @Override
  public String toString() {
    return getMapStorePath();
  }
}
