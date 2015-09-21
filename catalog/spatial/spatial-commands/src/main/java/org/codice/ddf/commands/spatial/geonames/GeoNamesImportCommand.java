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

import java.io.PrintStream;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.codice.ddf.spatial.geocoding.GeoEntryExtractionException;
import org.codice.ddf.spatial.geocoding.GeoEntryExtractor;
import org.codice.ddf.spatial.geocoding.GeoEntryIndexer;
import org.codice.ddf.spatial.geocoding.GeoEntryIndexingException;
import org.codice.ddf.spatial.geocoding.GeoNamesRemoteDownloadException;
import org.codice.ddf.spatial.geocoding.GeoNamesRemoteDownloader;
import org.codice.ddf.spatial.geocoding.ProgressCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "geonames", name = "import",
        description = "Adds new entries to the local GeoNames index from a specified URL.  "
                + "The GeoNames index must be created using -c, or the import "
                + "will result in an error.")
public class GeoNamesImportCommand extends OsgiCommandSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(GeoNamesUpdateCommand.class);

    @Argument(index = 0, name = "URL",
            description = "The URL of the resource whose contents you wish to download and insert into the index.",
            required = true)
    private String url = null;

    @Option(name = "-c", aliases = "--create",
            description = "Create a new index, overwriting any existing index at the destination.")
    private boolean create;

    private GeoEntryExtractor geoEntryExtractor;
    private GeoEntryIndexer geoEntryIndexer;
    private GeoNamesRemoteDownloader geoNamesRemoteDownloader = new GeoNamesRemoteDownloader();

    public void setGeoNamesRemoteDownloader(final GeoNamesRemoteDownloader geoNamesRemoteDownloader) {
        this.geoNamesRemoteDownloader = geoNamesRemoteDownloader;
    }

    public void setGeoEntryExtractor(final GeoEntryExtractor geoEntryExtractor) {
        this.geoEntryExtractor = geoEntryExtractor;
    }

    public void setGeoEntryIndexer(final GeoEntryIndexer geoEntryIndexer) {
        this.geoEntryIndexer = geoEntryIndexer;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    protected Object doExecute() {

        final PrintStream console = System.out;

        final ProgressCallback downloadCallback = new ProgressCallback() {
            @Override
            public void updateProgress(final int progress) {
                console.printf("\rDownloading : %d%%", progress);
                console.flush();
            }
        };

        final ProgressCallback progressCallback = new ProgressCallback() {
            @Override
            public void updateProgress(final int progress) {
                console.printf("\r%d%%", progress);
                console.flush();
            }
        };

        console.println("Importing from : " + url);
        String downloadPath = null;
        try {
            downloadPath = geoNamesRemoteDownloader.getResourceFromUrl(url, downloadCallback);
        } catch (GeoNamesRemoteDownloadException e) {
            LOGGER.error("Error downloading GeoNames zip file from resource {}", e);
            console.println("Error downloading GeoNames zip file from resource.\n" + e);
        }

        if(downloadPath != null) {

            try {
                console.println("\nDownload completed successfully.\nUpdating index... ");
                geoEntryIndexer
                        .updateIndex(downloadPath, geoEntryExtractor, create, progressCallback);
                console.println("\nDone.");
                geoNamesRemoteDownloader.deleteDownloadedFile(downloadPath);
            } catch(GeoEntryExtractionException e) {
                LOGGER.error("Error extracting GeoNames data from resource {}", downloadPath, e);
                console.printf("Could not extract GeoNames data from resource %s.\n" + "Message: %s\n"
                        + "Check the logs for more details.\n", downloadPath, e.getMessage());
            } catch(GeoEntryIndexingException e) {
                LOGGER.error("Error indexing GeoNames data", e);
                console.printf("Could not index the GeoNames data.\n" + "Message: %s\n"
                        + "Check the logs for more details.\n", e.getMessage());
            }
        }

        return null;
    }



}
