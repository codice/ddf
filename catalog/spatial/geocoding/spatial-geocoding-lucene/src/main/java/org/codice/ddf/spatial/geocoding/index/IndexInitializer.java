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
package org.codice.ddf.spatial.geocoding.index;

import java.io.File;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import org.codice.ddf.spatial.geocoding.GeoEntryExtractionException;
import org.codice.ddf.spatial.geocoding.GeoEntryExtractor;
import org.codice.ddf.spatial.geocoding.GeoEntryIndexer;
import org.codice.ddf.spatial.geocoding.GeoEntryIndexingException;
import org.codice.ddf.spatial.geocoding.GeoNamesRemoteDownloadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexInitializer {

  private static final Logger LOGGER = LoggerFactory.getLogger(IndexInitializer.class);

  private String defaultGeonamesDataPath;

  private String indexLocationPath;

  private GeoEntryExtractor extractor;

  private GeoEntryIndexer indexer;

  private ExecutorService executor;

  public void init() {
    File defaultGeonamesDataFile = Paths.get(defaultGeonamesDataPath).toFile();
    File indexLocationDir = Paths.get(indexLocationPath).toFile();
    if (!defaultGeonamesDataFile.exists()) {
      LOGGER.warn(
          "Could not locate default geonames data file at {}. Check that your distribution is no corrupted");
      return;
    }

    if (indexLocationDir.exists() && indexLocationDir.list().length > 0) {
      LOGGER.info("Geoname index already exists. Not indexing default data set.");
      return;
    }
    executor.submit(
        () -> {
          try {
            LOGGER.info("Indexing default geoname data at {}.", defaultGeonamesDataPath);

            indexer.updateIndex(
                defaultGeonamesDataFile.getAbsolutePath(),
                extractor,
                true,
                progress -> {
                  if (progress >= 100) {
                    LOGGER.info("Default geoname data indexed successfully");
                  }
                });
          } catch (GeoEntryIndexingException
              | GeoEntryExtractionException
              | GeoNamesRemoteDownloadException e) {
            LOGGER.error(
                "Failed to index default geonames data. Try using the geonames:update command to generate the index manually.",
                e);
          }
        });

    executor.shutdown();
  }

  public void destroy() {
    executor.shutdownNow();
  }

  public void setDefaultGeonamesDataPath(String defaultGeonamesDataPath) {
    this.defaultGeonamesDataPath = defaultGeonamesDataPath;
  }

  public void setExtractor(GeoEntryExtractor extractor) {
    this.extractor = extractor;
  }

  public void setIndexLocationPath(String indexLocationPath) {
    this.indexLocationPath = indexLocationPath;
  }

  public void setIndexer(GeoEntryIndexer indexer) {
    this.indexer = indexer;
  }

  public void setExecutor(ExecutorService executor) {
    this.executor = executor;
  }
}
