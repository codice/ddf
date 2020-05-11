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

import static org.codice.ddf.spatial.geocoding.GeoEntryExtractor.ExtractionCallback;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import org.codice.ddf.spatial.geocoding.FeatureExtractionException;
import org.codice.ddf.spatial.geocoding.FeatureExtractor;
import org.codice.ddf.spatial.geocoding.FeatureIndexer;
import org.codice.ddf.spatial.geocoding.FeatureIndexingException;
import org.codice.ddf.spatial.geocoding.GeoEntry;
import org.codice.ddf.spatial.geocoding.GeoEntryExtractionException;
import org.codice.ddf.spatial.geocoding.GeoEntryExtractor;
import org.codice.ddf.spatial.geocoding.GeoEntryIndexer;
import org.codice.ddf.spatial.geocoding.GeoEntryIndexingException;
import org.codice.ddf.spatial.geocoding.GeoNamesRemoteDownloadException;
import org.codice.ddf.spatial.geocoding.ProgressCallback;
import org.junit.Before;
import org.junit.Test;

public class GazetteerUpdateCommandTest {
  private ConsoleInterceptor consoleInterceptor;

  private GazetteerUpdateCommand gazetteerUpdateCommand;

  @Before
  public void setUp() {
    consoleInterceptor = new ConsoleInterceptor();
    consoleInterceptor.interceptSystemOut();
    gazetteerUpdateCommand = new GazetteerUpdateCommand();
  }

  @Test
  public void testProgressOutput() throws Exception {
    final GeoEntryExtractor geoEntryExtractor =
        spy(
            new GeoEntryExtractor() {
              @Override
              public List<GeoEntry> getGeoEntries(
                  final String resource, final ProgressCallback progressCallback) {
                return null;
              }

              @Override
              public void pushGeoEntriesToExtractionCallback(
                  final String resource, final ExtractionCallback extractionCallback) {
                extractionCallback.updateProgress(50);
                assertThat(consoleInterceptor.getOutput(), containsString("50%"));
                extractionCallback.updateProgress(100);
                assertThat(consoleInterceptor.getOutput(), containsString("100%"));
              }

              @Override
              public void setUrl(String url) {
                return;
              }
            });

    final GeoEntryExtractor geoEntryUrlExtractor =
        spy(
            new GeoEntryExtractor() {
              @Override
              public List<GeoEntry> getGeoEntries(
                  final String resource, final ProgressCallback progressCallback) {
                return null;
              }

              @Override
              public void pushGeoEntriesToExtractionCallback(
                  final String resource, final ExtractionCallback extractionCallback) {
                extractionCallback.updateProgress(50);
                assertThat(consoleInterceptor.getOutput(), containsString("50%"));
                extractionCallback.updateProgress(100);
                assertThat(consoleInterceptor.getOutput(), containsString("100%"));
              }

              @Override
              public void setUrl(String url) {
                return;
              }
            });

    final GeoEntryIndexer geoEntryIndexer =
        spy(
            new GeoEntryIndexer() {
              @Override
              public void updateIndex(
                  final List<GeoEntry> newEntries,
                  final boolean create,
                  final ProgressCallback progressCallback,
                  final String entrySource) {
                /* stub */
              }

              @Override
              public void updateIndex(
                  final String resource,
                  final GeoEntryExtractor geoEntryExtractor,
                  final boolean create,
                  final ProgressCallback progressCallback)
                  throws GeoNamesRemoteDownloadException, GeoEntryIndexingException,
                      GeoEntryExtractionException {
                final ExtractionCallback extractionCallback =
                    new ExtractionCallback() {
                      @Override
                      public void extracted(final GeoEntry newEntry) {
                        // Not used in test
                      }

                      @Override
                      public void updateProgress(final int progress) {
                        progressCallback.updateProgress(progress);
                      }
                    };
                geoEntryExtractor.pushGeoEntriesToExtractionCallback(resource, extractionCallback);
              }
            });

    List<GeoEntryExtractor> geoEntryExtractors = new ArrayList<>();
    geoEntryExtractors.add(geoEntryExtractor);
    geoEntryExtractors.add(geoEntryUrlExtractor);

    gazetteerUpdateCommand.setGeoEntryExtractor(geoEntryExtractor);
    gazetteerUpdateCommand.setGeoEntryIndexer(geoEntryIndexer);

    gazetteerUpdateCommand.setResource("test");
    gazetteerUpdateCommand.executeWithSubject();

    consoleInterceptor.resetSystemOut();
    consoleInterceptor.closeBuffer();
  }

  @Test
  public void testExceptionDuringExtraction() throws Exception {
    final String errorText = "Extraction error text";
    final GeoEntryExtractor geoEntryExtractor = mock(GeoEntryExtractor.class);
    final GeoEntryExtractionException geoEntryExtractionException =
        new GeoEntryExtractionException(errorText);

    doThrow(geoEntryExtractionException)
        .when(geoEntryExtractor)
        .pushGeoEntriesToExtractionCallback(anyString(), any(ExtractionCallback.class));

    final GeoEntryIndexer geoEntryIndexer =
        new GeoEntryIndexer() {
          @Override
          public void updateIndex(
              final List<GeoEntry> newEntries,
              final boolean create,
              final ProgressCallback progressCallback,
              final String entrySource) {
            /* stub */
          }

          @Override
          public void updateIndex(
              final String resource,
              final GeoEntryExtractor geoEntryExtractor,
              final boolean create,
              final ProgressCallback progressCallback)
              throws GeoNamesRemoteDownloadException, GeoEntryExtractionException,
                  GeoEntryIndexingException {
            geoEntryExtractor.pushGeoEntriesToExtractionCallback(
                resource, mock(ExtractionCallback.class));
          }
        };

    gazetteerUpdateCommand.setGeoEntryIndexer(geoEntryIndexer);
    gazetteerUpdateCommand.setGeoEntryExtractor(geoEntryExtractor);
    gazetteerUpdateCommand.setResource("temp.txt");

    gazetteerUpdateCommand.executeWithSubject();
    assertThat(consoleInterceptor.getOutput(), containsString(errorText));

    consoleInterceptor.resetSystemOut();
    consoleInterceptor.closeBuffer();
  }

  @Test
  public void testExceptionDuringIndexing() throws Exception {
    final String errorText = "Indexing error text";
    final GeoEntryIndexer geoEntryIndexer = mock(GeoEntryIndexer.class);
    final GeoEntryIndexingException geoEntryIndexingException =
        new GeoEntryIndexingException(errorText);
    doThrow(geoEntryIndexingException)
        .when(geoEntryIndexer)
        .updateIndex(
            anyString(), any(GeoEntryExtractor.class), anyBoolean(), any(ProgressCallback.class));

    gazetteerUpdateCommand.setGeoEntryIndexer(geoEntryIndexer);
    gazetteerUpdateCommand.setResource("temp");
    gazetteerUpdateCommand.executeWithSubject();

    assertThat(consoleInterceptor.getOutput(), containsString(errorText));

    consoleInterceptor.resetSystemOut();
  }

  @Test
  public void testFeatureIndexing() throws Exception {
    String resource = "example.geojson";
    final FeatureExtractor featureExtractor =
        spy(
            new FeatureExtractor() {
              @Override
              public void pushFeaturesToExtractionCallback(
                  String resource, ExtractionCallback extractionCallback)
                  throws FeatureExtractionException {
                /* stub */
              }
            });

    final FeatureIndexer featureIndexer =
        spy(
            new FeatureIndexer() {
              @Override
              public void updateIndex(
                  String resource,
                  FeatureExtractor featureExtractor,
                  boolean create,
                  IndexCallback callback)
                  throws FeatureExtractionException, FeatureIndexingException {
                /* stub */
              }
            });

    gazetteerUpdateCommand.setResource(resource);
    gazetteerUpdateCommand.setFeatureExtractor(featureExtractor);
    gazetteerUpdateCommand.setFeatureIndexer(featureIndexer);
    gazetteerUpdateCommand.executeWithSubject();
    verify(featureIndexer, times(1))
        .updateIndex(
            eq(resource), eq(featureExtractor), eq(false), any(FeatureIndexer.IndexCallback.class));
  }
}
