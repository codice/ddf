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
package org.codice.ddf.catalog.plugin.metacard.backup.storage.filestorage;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.codice.ddf.catalog.plugin.metacard.backup.storage.internal.MetacardBackupException;
import org.codice.ddf.catalog.plugin.metacard.backup.storage.internal.MetacardBackupStorageProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetacardBackupFileStorage implements MetacardBackupStorageProvider {

    private static final String DESCRIBABLE_PROPERTIES_FILE = "/describable.properties";

    private static final String DESCRIPTION = "description";

    private static final String ORGANIZATION = "organization";

    private static final String VERSION = "version";

    private static final String TITLE = "name";

    private static final String ID = "id";

    private static final Logger LOGGER = LoggerFactory.getLogger(MetacardBackupFileStorage.class);

    private static final String OUTPUT_DIRECTORY_PROPERTY = "outputDirectory";

    private String id;

    private String outputDirectory;

    private static Properties describableProperties = new Properties();

    static {
        try (InputStream properties = MetacardBackupFileStorage.class.getResourceAsStream(
                DESCRIBABLE_PROPERTIES_FILE)) {
            describableProperties.load(properties);
        } catch (IOException e) {
            LOGGER.debug("Unable to load describable properties", e);
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getVersion() {
        return (String) describableProperties.get(VERSION);
    }

    @Override
    public String getTitle() {
        return (String) describableProperties.get(TITLE);
    }

    @Override
    public String getDescription() {
        return (String) describableProperties.get(DESCRIPTION);
    }

    @Override
    public String getOrganization() {
        return (String) describableProperties.get(ORGANIZATION);
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public void delete(String id) throws IOException, MetacardBackupException {
        if (StringUtils.isEmpty(outputDirectory)) {
            throw new MetacardBackupException(
                    "Unable to delete stored data; no output directory specified.");
        }

        deleteBackupIfPresent(id);
    }

    @Override
    public void store(String id, byte[] data) throws IOException, MetacardBackupException {
        if (StringUtils.isEmpty(outputDirectory)) {
            throw new MetacardBackupException("Unable to store data; no output directory specified.");
        }

        if (data == null) {
            throw new MetacardBackupException("No data to store");
        }

        Path metacardPath = getMetacardDirectory(id);
        if (metacardPath == null) {
            String message = String.format("Unable to create metacard path directory for %s", id);
            LOGGER.debug(message);
            throw new MetacardBackupException(message);
        }

        try {
            Path parent = metacardPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.createFile(metacardPath);
        } catch (IOException e) {
            LOGGER.trace("Unable to create empty backup file {}.  File may already exist.",
                    metacardPath,
                    e);
        }

        try (OutputStream outputStream = new FileOutputStream(metacardPath.toFile())) {
            IOUtils.write(data, outputStream);
        }
    }

    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public String getOutputDirectory() {
        return outputDirectory;
    }

    public void refresh(Map<String, Object> properties) {
        Object outputDirectory = properties.get(OUTPUT_DIRECTORY_PROPERTY);
        if (outputDirectory instanceof String && StringUtils.isNotBlank((String) outputDirectory)) {
            this.outputDirectory = (String) outputDirectory;
        }

        Object id = properties.get(ID);
        if (id instanceof String) {
            setId((String) id);
        }
    }

    private void deleteBackupIfPresent(String filename) throws MetacardBackupException {
        Path metacardPath = getMetacardDirectory(filename);
        if (metacardPath == null) {
            LOGGER.debug("Unable to delete backup for: {}", filename);
            throw new MetacardBackupException("Unable to delete backup");
        }

        try {
            Files.deleteIfExists(metacardPath);
            while (metacardPath.getParent() != null && !metacardPath.getParent()
                    .toString()
                    .equals(outputDirectory)) {
                metacardPath = metacardPath.getParent();
                if (isDirectoryEmpty(metacardPath)) {
                    FileUtils.deleteDirectory(metacardPath.toFile());
                }
            }
        } catch (IOException e) {
            LOGGER.debug("Unable to delete backup file {}", metacardPath, e);
            throw new MetacardBackupException("Unable to delete backup file", e);
        }
    }

    public Path getMetacardDirectory(String id) {
        if (StringUtils.isEmpty(id)) {
            return null;
        }

        if (id.length() < 6) {
            id = StringUtils.rightPad(id, 6, "0");
        }

        try {
            return Paths.get(outputDirectory, id.substring(0, 3), id.substring(3, 6), id);
        } catch (InvalidPathException e) {
            LOGGER.debug("Unable to create path from id {}", outputDirectory, e);
            return null;
        }
    }

    private boolean isDirectoryEmpty(Path dir) throws IOException {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(dir)) {
            return !dirStream.iterator()
                    .hasNext();
        } catch (IOException e) {
            LOGGER.debug("Unable to open directory stream for {}", dir.toString(), e);
            throw e;
        }
    }
}
