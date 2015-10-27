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
package org.codice.ddf.configuration.store;

import static org.apache.commons.lang.Validate.isTrue;
import static org.apache.commons.lang.Validate.notNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that provides utility methods to access the configuration files in the configuration
 * directory.
 * <p/>
 * Note: Since this class is meant to only be used by {@link FileHandlerImpl} and is package
 * private, it assumes that all input validation has already been performed.
 */
public class ConfigurationFileDirectory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationFileDirectory.class);

    private Path configurationDirectory;

    private String fileExtension;

    private ConfigurationFileFactory configurationFileFactory;

    /**
     * Constructor.
     *
     * @param configurationDirectory
     *            directory that contains the configuration files. The directory must exist and be
     *            readable and writable.
     * @param fileExtension
     *            configuration files extension with preceding period, e.g., ".cfg"
     * @throws IllegalArgumentException
     *             thrown if any of the arguments is invalid
     */
    public ConfigurationFileDirectory(@NotNull Path configurationDirectory,
            @NotNull @Min(2) String fileExtension, ConfigurationFileFactory configurationFileFactory) {
        notNull(configurationDirectory, "Configuration directory cannot be null");
        notNull(fileExtension, "File extension is required");
        isTrue(fileExtension.length() >= 2, "Invalid file extension: ", fileExtension);
        isTrue(configurationDirectory.toFile().exists()
                && configurationDirectory.toFile().canRead()
                && configurationDirectory.toFile().canWrite(),
                "Directory does not exist or is not readable/writable: ", configurationDirectory);

        this.configurationDirectory = configurationDirectory;
        this.fileExtension = fileExtension;
        this.configurationFileFactory = configurationFileFactory;
    }

    public void init() {
        Collection<ConfigurationFile> configFiles = null;
        try {
            configFiles = getConfigurationFiles();
        } catch (IOException e) {
            LOGGER.error("ERROR", e);
            return;
        }

        for (ConfigurationFile configFile : configFiles) {
            configFile.createConfig();
        }
    }

    public Path getDirectoryPath() {
        return this.configurationDirectory;
    }

    /**
     * Gets the list of configuration file PIDs in the configuration directory.
     *
     * @return list of configuration file PIDs
     * @throws IOException
     * @throws FileNotFoundException
     */
    private Collection<ConfigurationFile> getConfigurationFiles() throws FileNotFoundException,
        IOException {
        Collection<Path> files = listFiles();
        Collection<ConfigurationFile> configurationFiles = new ArrayList<>(files.size());
        for (Path file : files) {
            ConfigurationFile configFile = configurationFileFactory.createConfigurationFile(file);
            configurationFiles.add(configFile);

        }
        return configurationFiles;
    }

    private Collection<Path> listFiles() throws IOException {
        Collection<Path> fileNames = new ArrayList<>();
        if (Files.isDirectory(configurationDirectory)) {
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(
                    configurationDirectory, "*" + fileExtension)) {
                for (Path path : directoryStream) {
                    fileNames.add(path);
                }
            } catch (IOException e) {
                throw new IOException("Unable to list files with extension " + fileExtension
                        + " in directory " + configurationDirectory, e);
            }
        }
        return fileNames;
    }
}
