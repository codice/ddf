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
package org.codice.ddf.configuration.admin;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.apache.commons.lang.Validate.notNull;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

import javax.validation.constraints.NotNull;

import org.apache.commons.io.FileUtils;
import org.codice.ddf.configuration.status.ConfigurationFileException;
import org.codice.ddf.configuration.status.ConfigurationStatusService;
import org.codice.ddf.configuration.status.MigrationException;
import org.codice.ddf.configuration.status.MigrationWarning;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class used to migrate {@link ConfigurationAdmin} configurations. This includes importing
 * configuration files from a configuration directory and creating {@link Configuration} objects
 * for those and exporting {@link Configuration} objects to configuration files.
 */
public class ConfigurationAdminMigration implements ChangeListener, ConfigurationStatusService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationAdminMigration.class);

    private static final String FILE_FILTER = "*.config";

    private static final String FILTER = "(&(!(service.pid=jmx*))(!(service.pid=org.apache*))(!(service.pid=org.ops4j*)))";

    private String configurationFileExtension;

    private DirectoryStream<Path> configDirectoryStream;

    private Path processedDirectory;

    private Path failedDirectory;

    private ConfigurationFileFactory configurationFileFactory;

    private ConfigurationFilesPoller poller;

    private ConfigurationAdmin configurationAdmin;

    /**
     * Constructor.
     *
     * @param configDirectoryStream    reference to a {@link DirectoryStream} that will return the
     *                                 configuration files to read in upon startup
     * @param processedDirectory       directory where configuration files will be moved to after
     *                                 being successfully processed
     * @param failedDirectory          directory where configuration files will be moved when they
     *                                 failed to be processed
     * @param configurationFileFactory factory object used to create {@link ConfigurationFile}
     *                                 instances based on their type
     * @param poller                   object to register with to know when new configuration
     *                                 files have been created
     * @throws IllegalArgumentException thrown if any of the arguments is invalid
     */
    public ConfigurationAdminMigration(@NotNull DirectoryStream configDirectoryStream,
            @NotNull Path processedDirectory, @NotNull Path failedDirectory,
            @NotNull ConfigurationFileFactory configurationFileFactory,
            @NotNull ConfigurationFilesPoller poller,
            @NotNull ConfigurationAdmin configurationAdmin,
            @NotNull String configurationFileExtension) {

        notNull(configDirectoryStream, "Config directory stream cannot be null");
        notNull(processedDirectory, "Processed directory cannot be null");
        notNull(failedDirectory, "Failed directory cannot be null");
        notNull(configurationFileFactory, "Configuration file factory cannot be null");
        notNull(poller, "Directory poller cannot be null");
        notNull(configurationAdmin, "ConfigurationAdmin cannot be null");
        notNull(configurationFileExtension, "ConfigFileExtension cannot be null");

        this.configDirectoryStream = configDirectoryStream;
        this.processedDirectory = processedDirectory;
        LOGGER.debug("Processed Directory Path: [{}]", this.processedDirectory);
        this.failedDirectory = failedDirectory;
        LOGGER.debug("Failed Directory Path: [{}]", this.failedDirectory);
        this.configurationFileFactory = configurationFileFactory;
        this.poller = poller;
        this.configurationAdmin = configurationAdmin;
        this.configurationFileExtension = configurationFileExtension;
    }

    /**
     * Loads all the configuration files located in the configuration directory specified in the
     * constructor. Files that have been successfully processed will be moved to the
     * {@code processedDirectory} and files that failed to be processed will be moved to the
     * {@code failedDirectory}. It will also register for file creation events which will
     * be handled by the {@link #notify(Path)} method.
     */
    public void init() throws IOException {
        createDirectory(processedDirectory);
        createDirectory(failedDirectory);

        Collection<ConfigurationFile> configFiles = getConfigurationFiles();

        for (ConfigurationFile configFile : configFiles) {
            try {
                configFile.createConfig();
                moveConfigurationFile(configFile.getConfigFilePath(), processedDirectory);
            } catch (ConfigurationFileException e) {
                moveConfigurationFile(configFile.getConfigFilePath(), failedDirectory);
            }
        }

        LOGGER.debug("Registering with [{}] for directory changes.", poller.getClass().getName());
        poller.register(this);
    }

    @Override
    public void notify(Path file) {
        Path fileInFailedDirectory = failedDirectory.resolve(file.getFileName());
        boolean result = deleteFileFromFailedDirectory(fileInFailedDirectory);
        LOGGER.debug("Deleted file [{}]: {}", fileInFailedDirectory.toString(), result);

        try {
            ConfigurationFile configFile = configurationFileFactory.createConfigurationFile(file);
            configFile.createConfig();
            moveConfigurationFile(file, processedDirectory);
        } catch (ConfigurationFileException | RuntimeException e) {
            moveConfigurationFile(file, failedDirectory);
        }
    }

    public Collection<MigrationWarning> getFailedConfigurationFiles() throws IOException {
        Collection<MigrationWarning> migrationWarnings = new ArrayList<>();
        try (DirectoryStream<Path> stream = getFailedDirectoryStream()) {
            for (Path path : stream) {
                migrationWarnings.add(new MigrationWarning(path.getFileName().toString()));
                LOGGER.debug("Adding [{}] to the failed imports list.", path.toString());
            }
        }
        return migrationWarnings;
    }

    public void export(@NotNull Path exportDirectory) throws MigrationException, IOException {
        notNull(exportDirectory, "exportDirectory cannot be null");
        Path etcDirectory = createEtcDirectory(exportDirectory);

        try {
            Configuration[] configurations = configurationAdmin.listConfigurations(FILTER);
            if (configurations != null) {
                for (Configuration configuration : configurations) {
                    Path exportedFilePath = etcDirectory
                            .resolve(configuration.getPid() + configurationFileExtension);
                    try {
                        configurationFileFactory
                                .createConfigurationFile(configuration.getProperties())
                                .exportConfig(exportedFilePath.toString());
                    } catch (ConfigurationFileException e) {
                        LOGGER.error("Could not create configuration file {} for configuration {}.",
                                exportedFilePath, configuration.getPid());
                        throw new MigrationException("Failed to export configurations.", e);
                    } catch (IOException e) {
                        LOGGER.error("Could not export configuration {} to {}.",
                                configuration.getPid(), exportedFilePath);
                        throw new MigrationException("Failed to export configurations.", e);
                    }
                }
            }
        } catch (InvalidSyntaxException e) {
            LOGGER.error("Invalid filter string {}", FILTER, e);
            throw new MigrationException("Failed to export configurations.", e);
        } catch (IOException e) {
            LOGGER.error("There was an issue retrieving configurations from ConfigurationAdmin: {}",
                    e.getMessage());
            throw new MigrationException("Failed to export configurations.", e);
        }
    }

    private void moveFile(Path source, Path destination) throws IOException {
        Files.move(source, destination.resolve(source.getFileName()), REPLACE_EXISTING);
    }

    private DirectoryStream<Path> getFailedDirectoryStream() throws IOException {
        return Files.newDirectoryStream(failedDirectory, FILE_FILTER);
    }

    private boolean deleteFileFromFailedDirectory(Path file) {
        return FileUtils.deleteQuietly(file.toFile());
    }

    private Path createEtcDirectory(Path exportDirectory) throws IOException {
        Path etcDirectory = exportDirectory.resolve("etc");
        FileUtils.forceMkdir(etcDirectory.toFile());
        LOGGER.debug("Output directory {} created", etcDirectory.toString());
        return etcDirectory;
    }

    private void moveConfigurationFile(Path source, Path destination) {
        try {
            moveFile(source, destination);
        } catch (IOException e) {
            LOGGER.warn(String.format("Failed to move %s to %s directory", source.toString(),
                    destination.toString()), e);
        }
    }

    private Collection<ConfigurationFile> getConfigurationFiles() throws IOException {
        Collection<Path> files = listFiles();
        Collection<ConfigurationFile> configurationFiles = new ArrayList<>(files.size());

        for (Path file : files) {
            ConfigurationFile configFile;

            try {
                configFile = configurationFileFactory.createConfigurationFile(file);
                configurationFiles.add(configFile);
            } catch (ConfigurationFileException e) {
                LOGGER.error(e.getMessage(), e);
                moveConfigurationFile(file, failedDirectory);
            }
        }

        return configurationFiles;
    }

    private Collection<Path> listFiles() throws IOException {
        Collection<Path> fileNames = new ArrayList<>();
        try {
            for (Path path : configDirectoryStream) {
                fileNames.add(path);
            }
        } finally {
            configDirectoryStream.close();
            configDirectoryStream = null;
        }
        return fileNames;
    }

    private void createDirectory(Path path) throws IOException {
        FileUtils.forceMkdir(path.toFile());
        LOGGER.debug("{} directory created", path.toString());
    }
}
