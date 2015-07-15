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
 **/

package org.codice.ddf.spatial.geo.extract;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.codice.ddf.spatial.geo.GeoEntry;
import org.codice.ddf.spatial.geo.GeoEntryCreator;
import org.codice.ddf.spatial.geo.GeoEntryExtractionException;
import org.codice.ddf.spatial.geo.GeoEntryExtractor;
import org.codice.ddf.spatial.geo.ProgressCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

public class GeoNamesFileExtractor implements GeoEntryExtractor {
    private static final Logger LOGGER = LoggerFactory.getLogger(GeoNamesFileExtractor.class);

    private GeoEntryCreator geoEntryCreator;

    public void setGeoEntryCreator(final GeoEntryCreator geoEntryCreator) {
        this.geoEntryCreator = geoEntryCreator;
    }

    @Override
    public List<GeoEntry> getGeoEntries(final String resource,
            final ProgressCallback progressCallback) {
        // It's probably safe to assume that there will be at least 1000 GeoNames entries in the
        // resource.
        final List<GeoEntry> geoEntryList = new ArrayList<>(1000);

        final ExtractionCallback extractionCallback = new ExtractionCallback() {
            @Override
            public void extracted(final GeoEntry newEntry) {
                geoEntryList.add(newEntry);
            }

            @Override
            public void updateProgress(final int progress) {
                if (progressCallback != null) {
                    progressCallback.updateProgress(progress);
                }
            }
        };

        getGeoEntriesStreaming(resource, extractionCallback);

        return geoEntryList;
    }

    @Override
    public void getGeoEntriesStreaming(final String resource,
            final ExtractionCallback extractionCallback) {
        if (extractionCallback == null) {
            throw new IllegalArgumentException("You must pass a non-null callback.");
        }

        final String inputTextFileLocation = getInputTextFileLocation(resource);

        try (final BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(inputTextFileLocation)));
            final LineNumberReader lineNumberReader = new LineNumberReader(
                new InputStreamReader(new FileInputStream(inputTextFileLocation)))) {
            // Use the number of lines in the file to track the extraction progress.
            lineNumberReader.skip(Long.MAX_VALUE);
            final int lineCount = lineNumberReader.getLineNumber() + 1;

            int progress = 0;
            int currentLine = 0;
            for (String line; (line = reader.readLine()) != null;) {
                extractionCallback.extracted(extractGeoEntry(line));

                if (currentLine == (int) (lineCount * (progress / 100.0f))) {
                    extractionCallback.updateProgress(progress);
                    progress += 5;
                }
                ++currentLine;
            }
            // Since we start counting the lines at 0, the progress callback won't be called in the
            // above loop when progress is 100. In any case, we need to give a progress update when
            // the work is complete.
            extractionCallback.updateProgress(100);
        } catch (ArrayIndexOutOfBoundsException e) {
            LOGGER.error("{} is not in the format expected by the GeoEntryCreator",
                    inputTextFileLocation, e);
            throw new GeoEntryExtractionException(inputTextFileLocation + " does not follow the " +
                    "expected GeoNames file format.", e);
        } catch (FileNotFoundException e) {
            LOGGER.error("Could not find the input file: {}", inputTextFileLocation, e);
            throw new GeoEntryExtractionException("Could not find " + inputTextFileLocation, e);
        } catch (IOException e) {
            LOGGER.error("An error occurred while reading {}", inputTextFileLocation, e);
            throw new GeoEntryExtractionException("An error occurred while reading " +
                    inputTextFileLocation, e);
        }
    }

    private String getInputTextFileLocation(final String fileLocation) {
        if (FilenameUtils.isExtension(fileLocation, "zip")) {
            // The GeoNames .zip files at http://download.geonames.org/export/dump each contain
            // a text file with the same name.
            final String baseName = FilenameUtils.getBaseName(fileLocation);
            final String textFileName = baseName + ".txt";
            try {
                unzipFile(fileLocation, textFileName);
                return FilenameUtils.getFullPath(fileLocation) + textFileName;
            } catch (ZipException e) {
                LOGGER.error("Error unzipping {} from {}", textFileName, fileLocation, e);
                throw new GeoEntryExtractionException("Error unzipping " + textFileName + " from " +
                    fileLocation, e);
            }
        } else if (FilenameUtils.isExtension(fileLocation, "txt")) {
            return fileLocation;
        } else {
            throw new GeoEntryExtractionException("Input must be a .txt or a .zip.");
        }
    }

    private void unzipFile(final String zipFileLocation, final String textFileName)
            throws ZipException {
        final ZipFile zipFile = new ZipFile(zipFileLocation);
        zipFile.extractFile(textFileName, FilenameUtils.getFullPath(zipFileLocation));
    }

    private GeoEntry extractGeoEntry(final String line) {
        return geoEntryCreator.createGeoEntry(line);
    }
}
