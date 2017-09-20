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
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.configuration.AbsolutePathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hazelcast persistence provider implementation of @MapLoader and @MapStore to serialize and
 * persist Java objects stored in Hazelcast cache to disk.
 */
public class FileSystemPersistenceProvider
    implements MapLoader<String, Object>, MapStore<String, Object> {

  private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemPersistenceProvider.class);

  private static final String SER = ".ser";

  private static final String SER_REGEX = "\\.ser";

  private String mapName = "default";

  FileSystemPersistenceProvider(String mapName) {
    LOGGER.trace("INSIDE: FileSystemPersistenceProvider constructor,  mapName = {}", mapName);
    this.mapName = mapName;
    File dir = new File(getPersistencePath());
    if (!dir.exists()) {
      boolean success = dir.mkdir();
      if (!success) {
        LOGGER.info("Could not make directory: {}", dir.getAbsolutePath());
      }
    }
  }

  /**
   * Retrieve root directory of all persisted Hazelcast objects for this cache. The path is relative
   * to containing bundle, i.e., DDF install directory.
   *
   * @return the path to root directory where serialized objects will be persisted
   */
  String getPersistencePath() {
    return new AbsolutePathResolver("data").getPath();
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
    if (!dir.exists() && !dir.mkdir()) {
      LOGGER.debug("Unable to create directory: {}", dir.getAbsolutePath());
    }
    LOGGER.trace("Entering: store - key: {}", key);
    try (OutputStream file = new FileOutputStream(getMapStoreFile(key));
        OutputStream buffer = new BufferedOutputStream(file);
        ObjectOutput output = new ObjectOutputStream(buffer)) {
      LOGGER.debug("file name: {}{}{}", getMapStorePath(), key, SER);
      output.writeObject(value);
    } catch (IOException e) {
      LOGGER.debug("IOException storing value in cache with key = " + key, e);
    }
    LOGGER.trace("Exiting: store");
  }

  @Override
  public void storeAll(Map<String, Object> keyValueMap) {
    for (Map.Entry<String, Object> entry : keyValueMap.entrySet()) {
      store(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public void delete(String key) {
    File file = getMapStoreFile(key);
    if (file.exists()) {
      boolean success = file.delete();
      if (!success) {
        LOGGER.info("Could not delete file {}", file.getAbsolutePath());
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
    File file = getMapStoreFile(key);
    if (!file.exists()) {
      return null;
    }
    InputStream inputStream = null;
    try {
      inputStream = new FileInputStream(getMapStorePath() + key + SER);
      InputStream buffer = new BufferedInputStream(inputStream);

      try (ObjectInput input = new ObjectInputStream(buffer)) {
        return input.readObject();
      }
    } catch (IOException e) {
      LOGGER.info("Unable to read object.", e);
    } catch (ClassNotFoundException e) {
      LOGGER.info("Class for object being read from stream does not exist.", e);
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
    FilenameFilter filter =
        new FilenameFilter() {
          @Override
          public boolean accept(File file, String name) {
            return name.toLowerCase().endsWith(SER);
          }
        };
    return filter;
  }

  @Override
  public Set<String> loadAllKeys() {
    Set<String> keys = new HashSet<String>();
    LOGGER.debug("Entering loadAllKeys");

    File[] files = new File(getMapStorePath()).listFiles(getFilenameFilter());
    if (files == null) {
      return keys;
    }

    for (File file : files) {
      keys.add(file.getName().replaceFirst(SER_REGEX, ""));
    }

    LOGGER.debug("Leaving loadAllKeys");

    return keys;
  }

  public void clear() {
    File[] files = new File(getMapStorePath()).listFiles(getFilenameFilter());
    if (null != files) {
      for (File file : files) {
        boolean success = file.delete();
        if (!success) {
          LOGGER.info("Could not delete file {}", file.getAbsolutePath());
        }
      }
    }
  }

  private File getMapStoreFile(String key) {
    return new File(getMapStorePath() + key + SER);
  }
}
