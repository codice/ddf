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

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InputTransformerIds {

  private static final Logger LOGGER = LoggerFactory.getLogger(InputTransformerIds.class);

  private Path transformerFolder;

  public InputTransformerIds() {
    this(Paths.get("etc", "transformers"));
  }

  /** @param path path of directory containing {@link InputTransformerIds} json files */
  public InputTransformerIds(Path path) {
    transformerFolder = path;
  }

  public Set<String> getIds() {
    File[] transformerFiles = transformerFolder.toFile().listFiles();
    if (transformerFiles == null || transformerFiles.length == 0) {
      LOGGER.debug("No Input Transformer json files found in {}", transformerFolder);
      return Collections.emptySet();
    }

    Set<String> allTransformerIds = new HashSet<>();
    Gson gson = new Gson();
    boolean malformed = false;

    for (File transformerFile : transformerFiles) {
      if (!FilenameUtils.getExtension(transformerFile.toString()).equalsIgnoreCase("json")) {
        LOGGER.debug("Skipping non-json file {}", transformerFile);
        continue;
      }

      try (FileReader reader = new FileReader(transformerFile)) {
        List<String> filesTransformerIds =
            gson.fromJson(reader, new TypeToken<List<String>>() {}.getType());

        if (filesTransformerIds != null) {
          allTransformerIds.addAll(filesTransformerIds);
        }
      } catch (IOException e) {
        LOGGER.debug(
            "Error reading InputTransformer ids from file {}. The ids returned may not be the full set described in {}.",
            transformerFile,
            transformerFolder);
      } catch (JsonSyntaxException e) {
        LOGGER.error(
            "Malformed JSON file {}. Fix or remove the file before continuing. See debug logs for more info.",
            transformerFile);
        LOGGER.debug("Malformed InputTransformer JSON file {}", transformerFile, e);
        malformed = true;
      }
    }

    if (malformed) {
      throw new IllegalStateException("One or more InputTransformer JSON files were malformed.");
    }

    return ImmutableSet.copyOf(allTransformerIds);
  }

  public String getTransformerPath() {
    return transformerFolder.toString();
  }
}
