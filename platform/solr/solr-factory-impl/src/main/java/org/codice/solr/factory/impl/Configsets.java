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
package org.codice.solr.factory.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Configsets {

  private static final Logger LOGGER = LoggerFactory.getLogger(Configsets.class);

  protected static final List<String> SOLR_CONFIG_FILES =
      Collections.unmodifiableList(
          Arrays.asList(
              "dictionary.txt",
              "protwords.txt",
              "schema.xml",
              "solr.xml",
              "solrconfig.xml",
              "stopwords.txt",
              "stopwords_en.txt",
              "synonyms.txt"));

  private String configsetsPath;

  private Path defaultPath;

  public Configsets() {
    this(Paths.get(System.getProperty("ddf.home", ""), "etc/solr/configsets"));
  }

  public Configsets(Path configsets) {
    configsetsPath = configsets.toAbsolutePath().toString();
    defaultPath = createDefaultConfigset(configsets);
  }

  private Path createDefaultConfigset(Path configsets) {
    Path defaultPath = configsets.resolve("default/conf");
    File defaultConfFolder = defaultPath.toFile();
    if (!defaultConfFolder.exists()) {
      boolean directoriesMade = defaultConfFolder.mkdirs();
      LOGGER.debug("Solr Config directories made?  {}", directoriesMade);
      for (String configFile : SOLR_CONFIG_FILES) {
        File currentFile = new File(defaultConfFolder, configFile);
        if (!currentFile.exists()) {
          try (InputStream configStream =
                  Configsets.class.getClassLoader().getResourceAsStream("solr/conf/" + configFile);
              FileOutputStream outputStream = new FileOutputStream(currentFile)) {
            long byteCount = IOUtils.copyLarge(configStream, outputStream);
            LOGGER.debug("Wrote out {} bytes for [{}].", byteCount, currentFile);
          } catch (IOException e) {
            LOGGER.warn("Unable to copy Solr configuration files", e);
          }
        }
      }
    }
    return defaultPath;
  }

  public Path get(String collection) {
    Path collectionPath = Paths.get(configsetsPath, collection, "conf");
    if (collectionPath.toFile().exists()) {
      return collectionPath;
    } else {
      LOGGER.debug("No configset for collection [{}]. Using default configset instead", collection);
      return defaultPath;
    }
  }
}
