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
package org.codice.ddf.commands.spatial.gazetteer;

import java.io.PrintStream;
import java.util.Locale;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.codice.ddf.commands.catalog.SubjectCommands;
import org.codice.ddf.spatial.geocoding.FeatureExtractionException;
import org.codice.ddf.spatial.geocoding.FeatureExtractor;
import org.codice.ddf.spatial.geocoding.FeatureIndexer;
import org.codice.ddf.spatial.geocoding.FeatureIndexingException;
import org.codice.ddf.spatial.geocoding.GeoEntryExtractionException;
import org.codice.ddf.spatial.geocoding.GeoEntryExtractor;
import org.codice.ddf.spatial.geocoding.GeoEntryIndexer;
import org.codice.ddf.spatial.geocoding.GeoEntryIndexingException;
import org.codice.ddf.spatial.geocoding.GeoNamesRemoteDownloadException;
import org.codice.ddf.spatial.geocoding.ProgressCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Command(
    scope = "gazetteer",
    name = "update",
    description = "Updates the gazetteer entries from a resource")
public final class GazetteerUpdateCommand extends SubjectCommands {

  private static final Logger LOGGER = LoggerFactory.getLogger(GazetteerUpdateCommand.class);

  @Argument(
      index = 0,
      name = "resource",
      description =
          "The resource whose contents will be added to the index. The resource can be a local file path to a .txt, .zip, or .geo.json GeoNames data file; a URL to a .txt or .zip GeoNames data file; or a keyword to automatically process a GeoNames file from from http://download.geonames.org/export/dump. See the documentation for more details.",
      required = true)
  private String resource = null;

  @Option(
      name = "-c",
      aliases = "--create",
      description = "Create a new index, overwriting any existing index at the destination.")
  private boolean create;

  @Reference private GeoEntryIndexer geoEntryIndexer;

  @Reference private GeoEntryExtractor geoEntryExtractor;

  @Reference private FeatureIndexer featureIndexer;

  @Reference private FeatureExtractor featureExtractor;

  public void setGeoEntryExtractor(final GeoEntryExtractor geoEntryExtractor) {
    this.geoEntryExtractor = geoEntryExtractor;
  }

  public void setGeoEntryIndexer(final GeoEntryIndexer geoEntryIndexer) {
    this.geoEntryIndexer = geoEntryIndexer;
  }

  public void setFeatureIndexer(FeatureIndexer featureIndexer) {
    this.featureIndexer = featureIndexer;
  }

  public void setFeatureExtractor(FeatureExtractor featureExtractor) {
    this.featureExtractor = featureExtractor;
  }

  public void setResource(String resource) {
    this.resource = resource;
  }

  @Override
  protected Object executeWithSubject() throws Exception {
    final PrintStream console = System.out;

    final ProgressCallback progressCallback =
        progress -> {
          console.printf("\r%d%%", progress);
          console.flush();
        };

    final FeatureIndexer.IndexCallback featureIndexCallback =
        count -> {
          console.printf("\r%d features indexed", count);
          console.flush();
        };

    console.println("Updating...");

    try {
      if (isResourceGeoJSON()) {
        featureIndexer.updateIndex(resource, featureExtractor, create, featureIndexCallback);
      } else {
        geoEntryIndexer.updateIndex(resource, geoEntryExtractor, create, progressCallback);
      }

      console.println("\nDone.");
    } catch (GeoEntryExtractionException | FeatureExtractionException e) {
      LOGGER.info("Error extracting data from resource {}", resource, e);
      console.printf(
          "Could not extract data from resource %s.%n Message: %s%n Check the logs for more details.%n",
          resource, e.getMessage());
    } catch (GeoEntryIndexingException | FeatureIndexingException e) {
      LOGGER.info("Error indexing data", e);
      console.printf(
          "Could not index the  data.%n Message: %s%n Check the logs for more details.%n",
          e.getMessage());
    } catch (GeoNamesRemoteDownloadException e) {
      LOGGER.info("Error downloading resource from remote source {}", resource, e);
      console.printf(
          "Could not download the GeoNames file %s.%n  Message: %s%n Check the logs for more details.%n",
          resource, e.getMessage());
    }

    return null;
  }

  private boolean isResourceGeoJSON() {
    String path = resource.toLowerCase(Locale.getDefault());
    return path.endsWith(".geojson") || path.endsWith(".geo.json");
  }
}
