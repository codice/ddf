/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */

package org.codice.ddf.commands.spatial.geonames;

import static org.codice.ddf.spatial.geocoding.GeoEntryExtractor.ExtractionCallback;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codice.ddf.spatial.geocoding.GeoEntry;
import org.codice.ddf.spatial.geocoding.GeoEntryExtractionException;
import org.codice.ddf.spatial.geocoding.GeoEntryExtractor;
import org.codice.ddf.spatial.geocoding.GeoEntryIndexer;
import org.codice.ddf.spatial.geocoding.GeoEntryIndexingException;
import org.codice.ddf.spatial.geocoding.GeoNamesRemoteDownloadException;
import org.codice.ddf.spatial.geocoding.ProgressCallback;
import org.junit.Before;
import org.junit.Test;

public class TestGeoNamesUpdateCommand {
    private ConsoleInterceptor consoleInterceptor;
    private GeoNamesUpdateCommand geoNamesUpdateCommand;

    @Before
    public void setUp() {
        consoleInterceptor = new ConsoleInterceptor();
        consoleInterceptor.interceptSystemOut();
        geoNamesUpdateCommand = new GeoNamesUpdateCommand();
    }

    @Test
    public void testProgressOutput() throws IOException {
        final GeoEntryExtractor geoEntryExtractor = spy(new GeoEntryExtractor() {
            @Override
            public List<GeoEntry> getGeoEntries(final String resource,
                    final ProgressCallback progressCallback) {
                return null;
            }

            @Override
            public void pushGeoEntriesToExtractionCallback(final String resource,
                    final ExtractionCallback extractionCallback) {
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

        final GeoEntryExtractor geoEntryUrlExtractor = spy(new GeoEntryExtractor() {
            @Override
            public List<GeoEntry> getGeoEntries(final String resource,
                    final ProgressCallback progressCallback) {
                return null;
            }

            @Override
            public void pushGeoEntriesToExtractionCallback(final String resource,
                    final ExtractionCallback extractionCallback) {
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

        final GeoEntryIndexer geoEntryIndexer = spy(new GeoEntryIndexer() {
            @Override
            public void updateIndex(final List<GeoEntry> newEntries, final boolean create,
                    final ProgressCallback progressCallback) { }

            @Override
            public void updateIndex(final String resource,
                    final GeoEntryExtractor geoEntryExtractor, final boolean create,
                    final ProgressCallback progressCallback) throws
                    GeoNamesRemoteDownloadException, GeoEntryIndexingException, GeoEntryExtractionException {
                final ExtractionCallback extractionCallback = new ExtractionCallback() {
                    @Override
                    public void extracted(final GeoEntry newEntry) { }

                    @Override
                    public void updateProgress(final int progress) {
                        progressCallback.updateProgress(progress);
                    }
                };
                geoEntryExtractor.pushGeoEntriesToExtractionCallback(resource, extractionCallback);
            }
        });

        List<GeoEntryExtractor> geoEntryExtractors = new ArrayList<GeoEntryExtractor>();
        geoEntryExtractors.add(geoEntryExtractor);
        geoEntryExtractors.add(geoEntryUrlExtractor);

        geoNamesUpdateCommand.setGeoEntryExtractor(geoEntryExtractor);
        geoNamesUpdateCommand.setGeoEntryIndexer(geoEntryIndexer);

        geoNamesUpdateCommand.setResource("test");
        geoNamesUpdateCommand.doExecute();

        consoleInterceptor.resetSystemOut();
        consoleInterceptor.closeBuffer();
    }

    @Test
    public void testExceptionDuringExtraction() throws IOException, GeoNamesRemoteDownloadException, GeoEntryExtractionException, GeoEntryIndexingException {
        final String errorText = "Extraction error text";
        final GeoEntryExtractor geoEntryExtractor = mock(GeoEntryExtractor.class);
        final GeoEntryExtractionException geoEntryExtractionException =
                new GeoEntryExtractionException(errorText);

        doThrow(geoEntryExtractionException).when(geoEntryExtractor)
                .pushGeoEntriesToExtractionCallback(anyString(), any(ExtractionCallback.class));

        final GeoEntryIndexer geoEntryIndexer = new GeoEntryIndexer() {
            @Override
            public void updateIndex(final List<GeoEntry> newEntries, final boolean create,
                    final ProgressCallback progressCallback) { }

            @Override
            public void updateIndex(final String resource,
                    final GeoEntryExtractor geoEntryExtractor, final boolean create,
                    final ProgressCallback progressCallback) throws  GeoNamesRemoteDownloadException, GeoEntryExtractionException, GeoEntryIndexingException{
                geoEntryExtractor.pushGeoEntriesToExtractionCallback(resource,
                        mock(ExtractionCallback.class));
            }
        };

        geoNamesUpdateCommand.setGeoEntryIndexer(geoEntryIndexer);
        geoNamesUpdateCommand.setGeoEntryExtractor(geoEntryExtractor);
        geoNamesUpdateCommand.setResource("temp.txt");

        geoNamesUpdateCommand.doExecute();
        assertThat(consoleInterceptor.getOutput(), containsString(errorText));

        consoleInterceptor.resetSystemOut();
        consoleInterceptor.closeBuffer();
    }

    @Test
    public void testExceptionDuringIndexing() throws  GeoNamesRemoteDownloadException, GeoEntryExtractionException, GeoEntryIndexingException {
        final String errorText = "Indexing error text";
        final GeoEntryExtractor geoEntryExtractor = mock(GeoEntryExtractor.class);

        final GeoEntryIndexer geoEntryIndexer = mock(GeoEntryIndexer.class);
        final GeoEntryIndexingException geoEntryIndexingException =
                new GeoEntryIndexingException(errorText);
        doThrow(geoEntryIndexingException).when(geoEntryIndexer).updateIndex(anyString(),
                any(GeoEntryExtractor.class), anyBoolean(), any(ProgressCallback.class));


        geoNamesUpdateCommand.setGeoEntryIndexer(geoEntryIndexer);
        geoNamesUpdateCommand.setGeoEntryExtractor(geoEntryExtractor);
        geoNamesUpdateCommand.setResource("temp");
        geoNamesUpdateCommand.doExecute();
        assertThat(consoleInterceptor.getOutput(), containsString(errorText));

        consoleInterceptor.resetSystemOut();
    }
}
