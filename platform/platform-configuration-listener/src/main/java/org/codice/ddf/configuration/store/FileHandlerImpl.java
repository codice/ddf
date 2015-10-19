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

import static org.apache.commons.lang.Validate.notNull;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileLock;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.validation.constraints.NotNull;

import org.codice.ddf.platform.util.ConfigurationPropertiesComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class used to manage configuration property files.
 */
public class FileHandlerImpl implements FileHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileHandlerImpl.class);

    private final ConfigurationPropertiesComparator propertiesComparator = new ConfigurationPropertiesComparator();

    private final ReadWriteLock readWriteLock;

    private final Lock readLock;

    private final Lock writeLock;

    private final Map<String, Dictionary<String, Object>> propertiesCache = new HashMap<>();

    private final ConfigurationFilesPoller configurationFilesPoller;

    private final PersistenceStrategy persistenceStrategy;

    private final ConfigurationFileDirectory configurationFileDirectory;

    /**
     * Constructor.
     *
     * @param configurationFileDirectory object to use to access the configuration file directory
     * @param configurationFilesPoller   object to use to monitor file changes
     * @param persistenceStrategy        object to use to persist files to disk
     * @throws IllegalArgumentException thrown if any of the arguments is invalid
     */
    public FileHandlerImpl(@NotNull ConfigurationFileDirectory configurationFileDirectory,
            @NotNull ConfigurationFilesPoller configurationFilesPoller,
            @NotNull PersistenceStrategy persistenceStrategy) {
        notNull(configurationFileDirectory, "ConfigurationFileDirectory cannot be null");
        notNull(configurationFilesPoller, "ConfigurationFilesPoller cannot be null");
        notNull(persistenceStrategy, "PersistenceStrategy cannot be null");

        this.configurationFileDirectory = configurationFileDirectory;
        this.configurationFilesPoller = configurationFilesPoller;
        this.persistenceStrategy = persistenceStrategy;
        this.readWriteLock = createLock();
        this.readLock = readWriteLock.readLock();
        this.writeLock = readWriteLock.writeLock();
    }

    @Override
    public Collection<String> getConfigurationPids() throws ConfigurationFileException {
        return configurationFileDirectory.listFiles();
    }

    @Override
    public Dictionary<String, Object> read(String pid) {
        notNull(pid, "Configuration persistence ID cannot be null");

        readLock.lock();

        try (InputStream inputStream = configurationFileDirectory.createFileInputStream(pid)) {
            Dictionary<String, Object> properties = persistenceStrategy.read(inputStream);
            propertiesCache.put(pid, properties);
            return properties;
        } catch (IOException e) {
            LOGGER.error("Unable to read configuration file for pid {}", pid, e);
            throw new ConfigurationFileException("Unable to read configuration for pid " + pid, e);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void write(String pid, Dictionary<String, Object> properties) {
        notNull(pid, "Configuration persistence ID cannot be null");
        notNull(properties, "Properties map cannot be null");

        writeLock.lock();

        try {
            if (!propertiesCache.containsKey(pid)) {
                LOGGER.debug("Did not find custom configuration file for pid [{}]. "
                        + "The configuration will not be written.", pid);
                return;
            }

            if (doesPropertyNeedToBeWrittenToFile(pid, properties)) {
                writeFile(pid, properties);
                propertiesCache.put(pid, properties);
            } else {
                LOGGER.debug(
                        "Cached properties for configuration pid [{}] are equal to the incoming properties. "
                                + "Not updating configuration file.", pid);
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void delete(String pid) {
        notNull(pid, "Configuration persistence ID cannot be null");

        writeLock.lock();

        try {

            if (configurationFileDirectory.delete(pid)) {
                LOGGER.warn("Tried to delete configuration file for PID {} but it did not exist.",
                        pid);
            }

            propertiesCache.put(pid, null);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void registerForChanges(ChangeListener listener) {
        notNull(listener, "Change listener cannot be null");

        try {
            configurationFilesPoller.register(listener);
        } catch (IOException e) {
            throw new ConfigurationFileException("Unable to register listener for changes");
        }
    }

    ReadWriteLock createLock() {
        return new ReentrantReadWriteLock();
    }

    private void writeFile(String pid, Dictionary<String, Object> properties) {

        try (FileOutputStream out = configurationFileDirectory.createFileOutputStream(pid);
                FileLock fileLock = out.getChannel().tryLock()) {

            if (fileLock == null) {
                LOGGER.error("Failed to update configuration file for PID {}: "
                        + "file locked by another application", pid);
                throw new ConfigurationFileException("Unable to obtain file lock on " + pid);
            }

            persistenceStrategy.write(out, properties);
        } catch (IOException e) {
            throw new ConfigurationFileException("Unable to write to " + pid);
        }
    }

    private boolean doesPropertyNeedToBeWrittenToFile(String pid,
            Dictionary<String, Object> properties) {

        return !propertiesComparator.equal(propertiesCache.get(pid), properties);
    }
}
