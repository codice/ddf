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
package org.codice.ddf.admin.configurator.impl;

import static org.codice.ddf.admin.configurator.impl.ConfigValidator.validateConfigPath;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import org.codice.ddf.admin.configurator.ConfiguratorException;
import org.codice.ddf.admin.configurator.Operation;
import org.codice.ddf.admin.configurator.Result;
import org.codice.ddf.internal.admin.configurator.actions.ConfigActions;
import org.codice.ddf.platform.io.internal.PersistenceStrategy;
import org.codice.ddf.platform.util.SortedServiceList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

/**
 * Transactional handler for persisting config file changes.
 * <p>
 * <b> This code is experimental. While this class is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
public abstract class ConfigOperation implements Operation<Void> {
    public static class Actions implements ConfigActions {
        private final SortedServiceList<PersistenceStrategy> strategies;

        /**
         * {@link SortedServiceList} implements {@link java.util.List} using generic parameter
         * <code>&lt;T&gt;</code> and not a concrete type, so the ctor cannot be matched by the
         * blueprint container if we did <code>SortedServiceList&lt;PersistenceStrategy&gt;</code>
         * <p>
         * See https://issues.apache.org/jira/browse/ARIES-960
         */
        public Actions(SortedServiceList strategies) {
            this.strategies = strategies;
        }

        @Override
        public Operation<Void> create(Path configPath, Dictionary<String, Object> configs)
                throws ConfiguratorException {
            validateConfigPath(configPath);
            return new ConfigOperation.CreateHandler(configPath,
                    configs,
                    getAppropriateStrategy(configPath, strategies));
        }

        @Override
        public ConfigOperation delete(Path configPath) throws ConfiguratorException {
            validateConfigPath(configPath);
            return new ConfigOperation.DeleteHandler(configPath,
                    getAppropriateStrategy(configPath, strategies));
        }

        @Override
        public Operation<Void> update(Path configPath, Dictionary<String, Object> configs,
                boolean keepIfNotPresent) throws ConfiguratorException {
            validateConfigPath(configPath);
            return new ConfigOperation.UpdateHandler(configPath,
                    configs,
                    keepIfNotPresent,
                    getAppropriateStrategy(configPath, strategies));
        }

        @Override
        public Dictionary<String, Object> getProperties(Path configPath)
                throws ConfiguratorException {
            validateConfigPath(configPath);
            return readConfig(configPath.toFile(), getAppropriateStrategy(configPath, strategies));
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigOperation.class);

    final File configFile;

    final Dictionary<String, Object> configs;

    final PersistenceStrategy strategy;

    private ConfigOperation(Path configFilePath, Dictionary<String, Object> configs,
            PersistenceStrategy strategy) {
        this.configFile = configFilePath.toFile();
        this.configs = configs;
        this.strategy = strategy;
    }

    private static PersistenceStrategy getAppropriateStrategy(Path path,
            SortedServiceList<PersistenceStrategy> strategies) {

        String ext = Files.getFileExtension(path.toString());
        if (ext == null || ext.isEmpty()) {
            throw new ConfiguratorException("Provided path has invalid file extension");
        }

        return strategies.stream()
                .filter(s -> s.getExtension()
                        .equals(ext))
                .findFirst()
                .orElseThrow(() -> new ConfiguratorException(
                        "No suitable strategy to persist config"));
    }

    private static Dictionary<String, Object> readConfig(File configFile,
            PersistenceStrategy strategy) {
        try (InputStream inputStream = new FileInputStream(configFile)) {
            return strategy.read(inputStream);
        } catch (IOException e) {
            throw new ConfiguratorException("Problem reading config: ", e);
        }
    }

    private static void createConfig(File configFile, PersistenceStrategy strategy,
            Dictionary<String, Object> configs) throws IOException {
        boolean created = configFile.createNewFile();
        if (!created) {
            LOGGER.debug("Config file already exists: {}", configFile.getAbsolutePath());
        }
        try (OutputStream outputStream = new FileOutputStream(configFile)) {
            strategy.write(outputStream, configs);
        }
    }

    /**
     * Transactional handler for creating config files
     */
    private static class CreateHandler extends ConfigOperation {
        private CreateHandler(Path configFile, Dictionary<String, Object> configs,
                PersistenceStrategy strategy) {
            super(configFile, configs, strategy);
        }

        @Override
        public Result<Void> commit() throws ConfiguratorException {
            try {
                createConfig(configFile, strategy, configs);
                return ResultImpl.pass();
            } catch (IOException e) {
                return ResultImpl.fail(new ConfiguratorException("Error creating config: ", e));
            }
        }

        /**
         * If a config was created on top of an already existing file, a successful rollback would
         * cause the file to be deleted, which was NOT precisely the state the system was in before.
         * The caller is responsible for invoking the appropriate operation knowing full well the
         * rollback consequences.
         */
        @Override
        public Result<Void> rollback() throws ConfiguratorException {
            boolean deleted = configFile.delete();
            if (!deleted) {
                LOGGER.debug("Problem deleting properties file {}", configFile);
                return ResultImpl.rollbackFail(new ConfiguratorException(
                        "Problem deleting properties file " + configFile));
            }
            return ResultImpl.rollback();
        }

        @Override
        public String toString() {
            return "Creating config " + configFile.getAbsolutePath();
        }
    }

    /**
     * Transactional handler for deleting config files
     */
    private static class DeleteHandler extends ConfigOperation {
        private Dictionary<String, Object> originalConfigs;

        private DeleteHandler(Path configFile, PersistenceStrategy strategy) {
            super(configFile, null, strategy);
            this.originalConfigs = new Hashtable<>();
        }

        @Override
        public Result<Void> commit() throws ConfiguratorException {
            originalConfigs = readConfig(configFile, strategy);
            boolean deleted = configFile.delete();
            if (!deleted) {
                LOGGER.debug("Problem deleting properties file {}", configFile);
                return ResultImpl.fail(new ConfiguratorException(
                        "Problem deleting properties file " + configFile));
            }
            return ResultImpl.pass();
        }

        @Override
        public Result<Void> rollback() throws ConfiguratorException {
            try {
                createConfig(configFile, strategy, originalConfigs);
                return ResultImpl.rollback();
            } catch (IOException e) {
                return ResultImpl.rollbackFail(new ConfiguratorException("Error creating config: ",
                        e));
            }
        }

        @Override
        public String toString() {
            return "Deleting config " + configFile.getAbsolutePath();
        }
    }

    /**
     * Transactional handler for updating config files
     */
    private static class UpdateHandler extends ConfigOperation {
        private final boolean keepIfNotPresent;

        private Dictionary<String, Object> originalConfigs;

        private UpdateHandler(Path configFile, Dictionary<String, Object> configs,
                boolean keepIfNotPresent, PersistenceStrategy strategy) {
            super(configFile, configs, strategy);
            this.keepIfNotPresent = keepIfNotPresent;
            this.originalConfigs = new Hashtable<>();
        }

        @Override
        public Result<Void> commit() throws ConfiguratorException {
            Dictionary<String, Object> configsToWrite = new Hashtable<>();
            originalConfigs = readConfig(configFile, strategy);

            if (keepIfNotPresent) {
                putAll(originalConfigs, configsToWrite);
            }
            putAll(configs, configsToWrite);

            try {
                FileOutputStream outputStream = new FileOutputStream(configFile);
                strategy.write(outputStream, configsToWrite);
                return ResultImpl.pass();
            } catch (IOException e) {
                return ResultImpl.fail(new ConfiguratorException("Error writing config: ", e));
            }
        }

        @Override
        public Result<Void> rollback() throws ConfiguratorException {
            try {
                FileOutputStream outputStream = new FileOutputStream(configFile);
                strategy.write(outputStream, originalConfigs);
                return ResultImpl.rollback();
            } catch (IOException e) {
                return ResultImpl.rollbackFail(new ConfiguratorException("Error writing config: ",
                        e));
            }
        }

        @Override
        public String toString() {
            return "Updating config " + configFile.getAbsolutePath();
        }

        private static void putAll(Dictionary<String, Object> source,
                Dictionary<String, Object> target) {
            Enumeration keys = source.keys();
            while (keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                target.put(key, source.get(key));
            }
        }
    }
}