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
import org.codice.ddf.spatial.geocoding.ProgressCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "geonames", name = "update",
        description = "Adds new entries to an existing local GeoNames index. Attempting to " +
                "add entries when no index exists is an error.")
public final class GeoNamesUpdateCommand extends OsgiCommandSupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(GeoNamesUpdateCommand.class);

    @Argument(index = 0, name = "resource",
            description = "The resource whose contents you wish to insert into the index.",
            required = true)
    private String resource = null;

    @Option(name = "-c", aliases = "--create",
            description = "Create a new index, overwriting any existing index at the destination.")
    private boolean create;

    private GeoEntryExtractor geoEntryExtractor;
    private GeoEntryIndexer geoEntryIndexer;

    public void setGeoEntryExtractor(final GeoEntryExtractor geoEntryExtractor) {
        this.geoEntryExtractor = geoEntryExtractor;
    }

    public void setGeoEntryIndexer(final GeoEntryIndexer geoEntryIndexer) {
        this.geoEntryIndexer = geoEntryIndexer;
    }

    @Override
    protected Object doExecute() {
        final PrintStream console = System.out;

        final ProgressCallback progressCallback = new ProgressCallback() {
            @Override
            public void updateProgress(final int progress) {
                console.printf("\r%d%%", progress);
                console.flush();
            }
        };

        console.println("Updating...");

        try {
            geoEntryIndexer.updateIndex(resource, geoEntryExtractor, create, progressCallback);
            console.println("\nDone.");
        } catch (GeoEntryExtractionException e) {
            LOGGER.error("Error extracting GeoNames data from resource {}", resource, e);
            console.printf("Could not extract GeoNames data from resource %s.\n" + "Message: %s\n"
                    + "Check the logs for more details.\n", resource, e.getMessage());
        } catch (GeoEntryIndexingException e) {
            LOGGER.error("Error indexing GeoNames data", e);
            console.printf("Could not index the GeoNames data.\n" + "Message: %s\n"
                    + "Check the logs for more details.\n", e.getMessage());
        }

        return null;
    }
}
