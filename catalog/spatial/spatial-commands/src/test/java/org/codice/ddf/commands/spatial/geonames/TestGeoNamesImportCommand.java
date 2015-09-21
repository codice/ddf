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

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.codice.ddf.spatial.geocoding.GeoEntry;
import org.codice.ddf.spatial.geocoding.GeoEntryExtractor;
import org.codice.ddf.spatial.geocoding.GeoEntryIndexer;
import org.codice.ddf.spatial.geocoding.GeoEntryIndexingException;
import org.codice.ddf.spatial.geocoding.GeoNamesRemoteDownloader;
import org.codice.ddf.spatial.geocoding.ProgressCallback;
import org.junit.Before;
import org.junit.Test;

public class TestGeoNamesImportCommand {

    private static final String VALID_ZIP =
            "AD.zip";

    private static final String INVALID_URL =
            "bad.com";

    private static final String VALID_URL =
            "http://download.geonames.org/export/dump/"
                    + VALID_ZIP;

    private ConsoleInterceptor consoleInterceptor;
    private GeoNamesRemoteDownloader geoNamesRemoteDownloader;
    private GeoNamesImportCommand geoNamesImportCommand;

    @Before
    public void setUp() {
        consoleInterceptor = new ConsoleInterceptor();
        consoleInterceptor.interceptSystemOut();
        geoNamesRemoteDownloader = new GeoNamesRemoteDownloader();
        geoNamesRemoteDownloader.setIndexLocation(new File(".").getAbsolutePath());
        geoNamesImportCommand = new GeoNamesImportCommand();

        final GeoEntryExtractor geoEntryExtractor = spy(new GeoEntryExtractor() {
            @Override
            public List<GeoEntry> getGeoEntries(final String resource,
                    final ProgressCallback progressCallback) {
                return null;
            }

            @Override
            public void getGeoEntriesStreaming(final String resource,
                    final ExtractionCallback extractionCallback) {
                extractionCallback.updateProgress(50);
                assertThat(consoleInterceptor.getOutput(), containsString("50%"));
                extractionCallback.updateProgress(100);
                assertThat(consoleInterceptor.getOutput(), containsString("100%"));
            }

        });
        geoNamesImportCommand.setGeoEntryExtractor(geoEntryExtractor);

        final GeoEntryIndexer geoEntryIndexer = spy(new GeoEntryIndexer() {
            @Override
            public void updateIndex(final List<GeoEntry> newEntries, final boolean create,
                    final ProgressCallback progressCallback) { }

            @Override
            public void updateIndex(final String resource,
                    final GeoEntryExtractor geoEntryExtractor, final boolean create,
                    final ProgressCallback progressCallback) {
                final GeoEntryExtractor.ExtractionCallback extractionCallback = new GeoEntryExtractor.ExtractionCallback() {
                    @Override
                    public void extracted(final GeoEntry newEntry) { }

                    @Override
                    public void updateProgress(final int progress) {
                        progressCallback.updateProgress(progress);
                    }
                };
                geoEntryExtractor.getGeoEntriesStreaming(resource, extractionCallback);
            }
        });

        geoNamesImportCommand.setGeoEntryIndexer(geoEntryIndexer);
        geoNamesImportCommand.setGeoNamesRemoteDownloader(geoNamesRemoteDownloader);
    }

    @Test
    public void testImportValidUrl() throws IOException {

        geoNamesImportCommand.setUrl(VALID_URL);
        geoNamesImportCommand.doExecute();
        assertThat(consoleInterceptor.getOutput(),
                containsString("Download completed successfully."));
        consoleInterceptor.resetSystemOut();
        consoleInterceptor.closeBuffer();
    }

    @Test
    public void testImportInvalidUrl() throws IOException {

        geoNamesImportCommand.setUrl(INVALID_URL);
        geoNamesImportCommand.doExecute();
        assertThat(consoleInterceptor.getOutput(),
                containsString("GeoNamesRemoteDownloadException"));
        consoleInterceptor.resetSystemOut();
        consoleInterceptor.closeBuffer();

    }

    @Test
    public void testExceptionDuringIndexing() throws IOException {

        geoNamesImportCommand.setUrl(VALID_URL);

        final String errorText = "Indexing error text";

        final GeoEntryExtractor geoEntryExtractor = mock(GeoEntryExtractor.class);
        geoNamesImportCommand.setGeoEntryExtractor(geoEntryExtractor);

        final GeoEntryIndexer geoEntryIndexer = mock(GeoEntryIndexer.class);
        final GeoEntryIndexingException geoEntryIndexingException =
                new GeoEntryIndexingException(errorText);
        doThrow(geoEntryIndexingException).when(geoEntryIndexer).updateIndex(anyString(),
                any(GeoEntryExtractor.class), anyBoolean(), any(ProgressCallback.class));
        geoNamesImportCommand.setGeoEntryIndexer(geoEntryIndexer);

        geoNamesImportCommand.doExecute();
        assertThat(consoleInterceptor.getOutput(), containsString(errorText));

        consoleInterceptor.resetSystemOut();
        consoleInterceptor.closeBuffer();

        geoNamesRemoteDownloader.deleteDownloadedFile(new File("").getAbsolutePath() + "/" + VALID_ZIP);
    }

}
