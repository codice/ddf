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
package ddf.camel.component.catalog.content;

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
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NOTE: The usage of object serialization/deserialization may trigger static analysis warnings.
 * This usage is acceptable as the read/write directory is not configurable and lives under
 * DDF_HOME.
 */
public class FileSystemDataAccessObject {

  private static final Logger LOGGER = LoggerFactory.getLogger(FileSystemDataAccessObject.class);

  public void store(String storePath, String suffix, String key, Object value) {
    File dir = new File(storePath);
    if (!dir.exists() && !dir.mkdir()) {
      LOGGER.debug("Unable to create directory: {}", dir.getAbsolutePath());
    }
    try (OutputStream file = new FileOutputStream(storePath + key + suffix);
        OutputStream buffer = new BufferedOutputStream(file);
        ObjectOutputStream output = new ObjectOutputStream(buffer)) {
      output.writeObject(value);
    } catch (IOException e) {
      LOGGER.debug("IOException storing value in cache with key = " + key, e);
    }
  }

  public Object loadFromPersistence(String storePath, String suffix, String key) {
    String pathString = storePath + key + suffix;
    File file = new File(pathString);
    if (!file.exists()) {
      return null;
    }

    try (InputStream inputStream = new FileInputStream(pathString)) {
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

  public Set<String> loadAllKeys(
      String storePath, String suffixRegex, FilenameFilter filenameFilter) {
    Set<String> keys = new HashSet<String>();

    File[] files = new File(storePath).listFiles(filenameFilter);
    if (files == null) {
      return keys;
    }

    for (File file : files) {
      keys.add(file.getName().replaceFirst(suffixRegex, ""));
    }
    return keys;
  }

  public void clear(String storePath, FilenameFilter filenameFilter) {
    File[] files = new File(storePath).listFiles(filenameFilter);
    if (files != null) {
      for (File file : files) {
        if (!file.delete()) {
          LOGGER.debug("File was unable to be deleted: {}", file.getAbsolutePath());
        }
      }
    }
  }

  public FilenameFilter getFilenameFilter(String suffix) {
    return (file, name) -> name.toLowerCase(Locale.getDefault()).endsWith(suffix);
  }
}
