/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */

package org.codice.ddf.catalog.content.plugin.clavin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bericotech.clavin.ClavinException;
import com.bericotech.clavin.GeoParser;
import com.bericotech.clavin.GeoParserFactory;
import com.bericotech.clavin.index.IndexDirectoryBuilder;
import com.bericotech.clavin.resolver.ResolvedLocation;

public class ClavinWrapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClavinWrapper.class);

    private String indexLocation;

    private GeoParser geoParser;

    public void setIndexLocation(final String indexLocation) {
        if (!indexLocation.equals(this.indexLocation)) {
            geoParser = null;
        }
        this.indexLocation = indexLocation;
    }

    private IndexDirectoryBuilder indexDirectoryBuilder;

    /**
     * Get a Geoparser instance.
     *
     * @return Geoparser instance.
     * @throws ClavinException
     */
    public GeoParser getGeoParser() throws ClavinException {
        geoParser = geoParser == null ? createGeoParser() : geoParser;
        return geoParser;
    }

    /**
     * Get an IndexDirectoryBuilder instance.
     *
     * @return IndexDirectoryBuilder instance.
     */
    public IndexDirectoryBuilder getIndexDirectoryBuilder() {
        indexDirectoryBuilder = indexDirectoryBuilder == null ?
                createIndexDirectoryBuilder() :
                indexDirectoryBuilder;
        return indexDirectoryBuilder;
    }

    /**
     * Test if a file points to a lucene index.
     * <p>
     *
     * @param directoryFile File to test.
     * @return true if directory File is a lucene index, false otherwise.
     */
    public boolean indexExists(File directoryFile) {
        try (Directory directory = FSDirectory.open(directoryFile)) {
            return DirectoryReader.indexExists(directory);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to test for lucene index.", e);
        }
    }

    /**
     * Create a lucene index from the resource file.
     * <p>
     *
     * @param resource File to build index from.
     * @throws IOException if problem building index.
     */
    public void createIndex(File resource) throws IOException, ClavinException {
        File directoryFile = new File(indexLocation);
        if (!indexExists(directoryFile)) {
            List<File> gazeteerFiles = new ArrayList<>();
            gazeteerFiles.add(resource);

            LOGGER.info("Creating clavin index: " + directoryFile.getAbsolutePath());

            getIndexDirectoryBuilder().buildIndex(directoryFile, gazeteerFiles, null);

            LOGGER.info("Done creating clavin index: " + directoryFile.getAbsolutePath());
        }
    }

    /**
     * @param text
     * @return
     * @throws Exception
     */
    public List<ResolvedLocation> parse(String text) throws Exception {
        GeoParser parser = getGeoParser();
        List<ResolvedLocation> resolvedLocationList;
        if (StringUtils.isBlank(text)) {
            resolvedLocationList = Collections.emptyList();
        } else {
            resolvedLocationList = parser.parse(text);
        }
        return resolvedLocationList;
    }

    private GeoParser createGeoParser() throws ClavinException {
        return GeoParserFactory.getDefault(indexLocation, true);
    }

    private IndexDirectoryBuilder createIndexDirectoryBuilder() {
        IndexDirectoryBuilder indexDirectoryBuilderInstance = null;
        try {
            Constructor[] ctors = IndexDirectoryBuilder.class.getDeclaredConstructors();
            ctors[0].setAccessible(true);
            indexDirectoryBuilderInstance = (IndexDirectoryBuilder) ctors[0].newInstance(false);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Failed to get IndexDirectoryBuilder instance.", e);
        }
        return indexDirectoryBuilderInstance;
    }

}
