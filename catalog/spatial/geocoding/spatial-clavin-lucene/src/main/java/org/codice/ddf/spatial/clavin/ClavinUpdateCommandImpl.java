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
 **/

package org.codice.ddf.spatial.clavin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bericotech.clavin.index.IndexDirectoryBuilder;

public class ClavinUpdateCommandImpl {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClavinUpdateCommandImpl.class);

    private String clavinIndexLocation;

    public void setClavinIndexLocation(final String indexLocation) {
        this.clavinIndexLocation = indexLocation;
    }

    public void createIndex(String resource) {
        LOGGER.debug("creating clavin index at location: " + clavinIndexLocation);
        try {
            File directoryFile = new File(clavinIndexLocation);
            LOGGER.info("clavin index: " + directoryFile.getAbsolutePath());
            Directory directory = FSDirectory.open(directoryFile);
            if (!DirectoryReader.indexExists(directory)) {
                List<File> gazeteerFiles = new ArrayList<>();

                gazeteerFiles.add(new File(resource));

                Constructor[] ctors = IndexDirectoryBuilder.class.getDeclaredConstructors();
                ctors[0].setAccessible(true);
                IndexDirectoryBuilder idb = (IndexDirectoryBuilder) ctors[0].newInstance(false);
                idb.buildIndex(directoryFile, gazeteerFiles, null);
            }
        } catch (IOException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(
                    "Failed to create clavin index at: " + clavinIndexLocation, e);
        }
    }
}
