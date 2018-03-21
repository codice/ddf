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

import ddf.security.service.SecurityServiceException;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import org.codice.ddf.security.common.Security;
import org.codice.ddf.spatial.geocoding.GeoEntryExtractionException;
import org.codice.ddf.spatial.geocoding.GeoEntryExtractor;
import org.codice.ddf.spatial.geocoding.GeoEntryIndexer;
import org.codice.ddf.spatial.geocoding.GeoEntryIndexingException;
import org.codice.ddf.spatial.geocoding.GeoNamesRemoteDownloadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexInitializer {

  private static final Logger LOGGER = LoggerFactory.getLogger(IndexInitializer.class);

  private String defaultGeoNamesDataPath;

  private GeoEntryExtractor extractor;

  private GeoEntryIndexer indexer;

  private ExecutorService executor;

  private final Security security;

  public IndexInitializer() {
    security = Security.getInstance();
  }

  public void init() {
    File defaultGeoNamesDataFile = Paths.get(defaultGeoNamesDataPath).toFile();
    if (!defaultGeoNamesDataFile.exists()) {
      LOGGER.warn(
          "Could not locate default GeoNames data file at {}. Check that your distribution is not corrupted.",
          defaultGeoNamesDataPath);
      return;
    }
    submitUpdateIndexToExecutorService(defaultGeoNamesDataFile);
    executor.shutdown();
  }

  private void submitUpdateIndexToExecutorService(File defaultGeoNamesDataFile) {
    executor.submit(
        () -> {
          LOGGER.info("Indexing default GeoNames data at {}.", defaultGeoNamesDataPath);
          security.runAsAdmin(
              () -> {
                try {
                  security.runWithSubjectOrElevate(() -> updateIndex(defaultGeoNamesDataFile));
                } catch (SecurityServiceException | InvocationTargetException e) {
                  LOGGER.debug("Unable to update Gazetteer index.", e);
                }
                return null;
              });
        });
  }

  private Object updateIndex(File defaultGeoNamesDataFile) {
    try {
      indexer.updateIndex(
          defaultGeoNamesDataFile.getAbsolutePath(),
          extractor,
          true,
          progress -> {
            if (progress >= 100) {
              LOGGER.info("Default GeoNames data indexed successfully");
            }
          });
    } catch (GeoEntryIndexingException
        | GeoEntryExtractionException
        | GeoNamesRemoteDownloadException e) {
      LOGGER.debug("Could not update index.", e);
    }
    return null;
  }

  public void destroy() {
    executor.shutdownNow();
  }

  public void setDefaultGeoNamesDataPath(String defaultGeonamesDataPath) {
    this.defaultGeoNamesDataPath = defaultGeonamesDataPath;
  }

  public void setExtractor(GeoEntryExtractor extractor) {
    this.extractor = extractor;
  }

  public void setIndexer(GeoEntryIndexer indexer) {
    this.indexer = indexer;
  }

  public void setExecutor(ExecutorService executor) {
    this.executor = executor;
  }
}
