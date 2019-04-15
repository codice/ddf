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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.codec.digest.DigestUtils;
import org.codice.ddf.configuration.AbsolutePathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonPersistantStore implements ObjectPersistentStore {

  private static final Logger LOGGER = LoggerFactory.getLogger(JsonPersistantStore.class);

  private static final String PERSISTED_FILE_SUFFIX = ".json";

  private Gson gson =
      new GsonBuilder()
          .registerTypeAdapter(new TypeToken<File>() {}.getType(), new FileTypeAdapter())
          .create();

  private final String mapName;

  public JsonPersistantStore() {
    mapName = getClass().getSimpleName();
  }

  public JsonPersistantStore(String mapName) {
    this.mapName = mapName;
  }

  private String getShaFor(String key) {
    return DigestUtils.sha1Hex(key);
  }

  private Path getPath() {
    return Paths.get(getPersistencePath(), mapName);
  }

  private String getPersistencePath() {
    return new AbsolutePathResolver("data").getPath();
  }

  @Override
  public void store(String key, Object toStore) {
    File dir = getPath().toFile();
    if (!dir.exists() && !dir.mkdir()) {
      LOGGER.debug("Unable to create directory: {}", dir.getAbsolutePath());
    }
    String shaKey = getShaFor(key);
    try (OutputStream file =
            new FileOutputStream(getPath().resolve(shaKey + PERSISTED_FILE_SUFFIX).toFile());
        OutputStream buffer = new BufferedOutputStream(file);
        OutputStreamWriter output = new OutputStreamWriter(buffer)) {
      gson.toJson(toStore, output);
    } catch (IOException | JsonIOException e) {
      LOGGER.debug("IOException storing value in cache with key = " + key, e);
    }
  }

  @Override
  public <T> T load(String key, Class<T> objectClass) {
    String shaKey = getShaFor(key);
    File file = getPath().resolve(shaKey + PERSISTED_FILE_SUFFIX).toFile();
    if (!file.exists()) {
      return null;
    }

    try (Reader fileReader = new FileReader(file)) {
      return gson.fromJson(fileReader, objectClass);

    } catch (IOException | JsonIOException e) {
      LOGGER.debug("IOException", e);
    } catch (JsonSyntaxException f) {
      LOGGER.debug("JsonSyntaxException", f);
    }
    return null;
  }
}
