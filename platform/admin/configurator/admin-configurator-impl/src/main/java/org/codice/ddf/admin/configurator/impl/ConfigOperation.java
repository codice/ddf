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
import java.nio.file.Path;
import java.util.Map;

import org.codice.ddf.admin.configurator.ConfiguratorException;
import org.codice.ddf.admin.configurator.Operation;
import org.codice.ddf.admin.configurator.Result;
import org.codice.ddf.internal.admin.configurator.actions.ConfigActions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transactional handler for persisting config file changes.
 * <p>
 * <b> This code is experimental. While this class is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
public abstract class ConfigOperation implements Operation<Void> {
    public static class Actions implements ConfigActions {
        @Override
        public Operation<Void> create(Path configFile, Map<String, String> configs)
                throws ConfiguratorException {
            throw new UnsupportedOperationException();
        }

        @Override
        public ConfigOperation delete(Path propFile) throws ConfiguratorException {
            validateConfigPath(propFile);
            return new ConfigOperation.DeleteHandler(propFile);
        }

        @Override
        public Operation<Void> update(Path configFile, Map<String, String> configs,
                boolean keepIfNotPresent) throws ConfiguratorException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<String, String> getProperties(Path propFile) throws ConfiguratorException {
            throw new UnsupportedOperationException();
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigOperation.class);

    final File configFile;

    private ConfigOperation(Path configFilePath) {
        this.configFile = configFilePath.toFile();
    }

    /**
     * Transactional handler for deleting config files
     */
    private static class DeleteHandler extends ConfigOperation {
        private DeleteHandler(Path configFile) {
            super(configFile);
        }

        @Override
        public Result<Void> commit() throws ConfiguratorException {
            boolean deleted = configFile.delete();
            if (!deleted) {
                LOGGER.debug("Problem deleting properties file {}", configFile);
            }
            return ResultImpl.pass();
        }

        @Override
        public Result<Void> rollback() throws ConfiguratorException {
            return ResultImpl.rollback();
        }
    }
}
