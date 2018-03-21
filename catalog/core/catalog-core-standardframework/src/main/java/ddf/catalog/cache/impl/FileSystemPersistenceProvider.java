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
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hazelcast persistence provider implementation of @MapLoader and @MapStore to serialize and
 * persist Java objects stored in Hazelcast cache to disk.
 */
public class FileSystemPersistenceProvider
    implements MapLoader<String, Object>, MapStore<String, Object> {

  private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemPersistenceProvider.class);

  private static final String EXT = ".ser";

  private static final String EXT_REGEX = "\\.ser";

  private static final String EXT_PATH_MATCH = "glob:**.ser";
  private File persistencePath;

  FileSystemPersistenceProvider(String mapName, String persistencePath) {
    LOGGER.trace("INSIDE: FileSystemPersistenceProvider constructor,  mapName = {}", mapName);
    this.persistencePath = new File(persistencePath);
    initializePersistencePath();
  }

  @Override
  public void store(String key, Object value) {
    LOGGER.trace("Entering: store - key: {}", key);
    try (ObjectOutput output =
        new ObjectOutputStream(
            new BufferedOutputStream(new FileOutputStream(getMapStoreFile(key))))) {
      LOGGER.debug("file name: {}/{}{}", persistencePath, key, EXT);
      output.writeObject(value);
    } catch (IOException e) {
      LOGGER.debug("IOException storing value in cache with key = " + key, e);
    }
    LOGGER.trace("Exiting: store");
  }

  @Override
  public void storeAll(Map<String, Object> keyValueMap) {
    keyValueMap.forEach(this::store);
  }

  @Override
  public void delete(String key) {
    FileUtils.deleteQuietly(getMapStoreFile(key));
  }

  @Override
  public void deleteAll(Collection<String> keys) {
    keys.forEach(this::delete);
  }

  @Override
  public Object load(String key) {
    // Not implemented because the Hazelcast data grid is all in cache,
    // so never have something persisted that is
    // not in memory and want to avoid a performance hit on the file system
    return null;
  }

  @Override
  public Map<String, Object> loadAll(Collection<String> keys) {
    Map<String, Object> values = new HashMap<>();

    keys.forEach(
        key -> {
          Object obj = loadFromPersistence(key);
          if (obj != null) {
            values.put(key, obj);
          }
        });
    return values;
  }

  @Override
  public Set<String> loadAllKeys() {
    Set<String> keys = new HashSet<>();
    LOGGER.trace("Entering loadAllKeys");

    try (Stream<Path> stream = Files.list(persistencePath.toPath())) {
      keys =
          stream
              .filter(getPathMatcher()::matches)
              .map(Path::getFileName)
              .map(Path::toString)
              .map(fileName -> fileName.replaceFirst(EXT_REGEX, ""))
              .collect(Collectors.toSet());
    } catch (IOException e) {
      LOGGER.warn("Unable to read files at {}", persistencePath);
    }

    LOGGER.trace("Leaving loadAllKeys");

    return keys;
  }

  public void clear() {
    try (Stream<Path> stream = Files.list(persistencePath.toPath())) {
      stream.filter(getPathMatcher()::matches).map(Path::toFile).forEach(FileUtils::deleteQuietly);
    } catch (IOException e) {
      LOGGER.warn("Unable to delete files at {}", persistencePath.getAbsolutePath());
    }
  }

  public File getPersistencePath() {
    return persistencePath;
  }

  private Object loadFromPersistence(String key) {
    File file = getMapStoreFile(key);

    if (!file.exists()) {
      return null;
    }

    try (ObjectInput input =
        new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)))) {
      return input.readObject();
    } catch (IOException e) {
      LOGGER.info("Unable to read object.", e);
    } catch (ClassNotFoundException e) {
      LOGGER.info("Class for object being read from stream does not exist.", e);
      FileUtils.deleteQuietly(file);
    }

    return null;
  }

  private File getMapStoreFile(String key) {
    return persistencePath.toPath().resolve(key + EXT).toFile();
  }

  private PathMatcher getPathMatcher() {
    return persistencePath.toPath().getFileSystem().getPathMatcher(EXT_PATH_MATCH);
  }

  private void initializePersistencePath() {
    if (!persistencePath.exists()) {
      try {
        FileUtils.forceMkdir(persistencePath);
      } catch (IOException e) {
        LOGGER.warn("Could not make directory: {}", persistencePath.getAbsolutePath());
      }
    }
  }
}
