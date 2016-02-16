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
package org.codice.ddf.catalog.migratable.impl;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.codice.ddf.migration.MigrationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;

/**
 * Responsible for all low-level IO operations pertaining to migration.
 */
public class MigrationFileWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MigrationFileWriter.class);

    /**
     * Creates the directory for the catalog export if it doesn't exist.
     * <p>
     * If the directory does exist, work based on the assumption it was cleaned by the parent
     * process in the management logic of inport and export. There is no point to delegate
     * low level directory operations to each and every migratable when they can be abstracted
     * into the base logic.
     *
     * @param exportPath The path representing the directory to create.
     * @throws MigrationException thrown if the export directory doesn't exist and could not be created
     */
    public void init(Path exportPath) throws MigrationException {
        try {
            final File exportDir = exportPath.toFile();
            FileUtils.forceMkdir(exportDir);
        } catch (IOException e) {
            LOGGER.error("IO Exception during FileUtils.forceMkdir", e.getMessage(), e);
            throw new MigrationException(e.getMessage());
        }
    }

    /**
     * Writes the metacards in the provided results list to the provided file.
     *
     * @param exportFile The file to write to.
     * @param results    The metacards to write into the file.
     */
    public void writeMetacards(File exportFile, final List<Result> results) throws IOException {
        if (!exportFile.exists()) {
            boolean success = exportFile.createNewFile();
            if (!success) {
                LOGGER.warn("Was not able to create new file for metacard export");
            }
        }

        try (
                FileOutputStream fileStream = createFileStream(exportFile);
                BufferedOutputStream bufferedStream = createBufferedStream(fileStream);
                ObjectOutputStream objectStream = createObjectStream(bufferedStream);
        ) {
            for (final Result result : results) {
                Metacard metacard = new MetacardImpl(result.getMetacard());
                objectStream.writeObject(metacard);
            }
            bufferedStream.flush();
        }
    }

    ObjectOutputStream createObjectStream(OutputStream source) throws IOException {
        return new ObjectOutputStream(source);
    }

    BufferedOutputStream createBufferedStream(OutputStream source) throws IOException {
        return new BufferedOutputStream(source);
    }

    FileOutputStream createFileStream(File file) throws IOException {
        return new FileOutputStream(file);
    }
}
