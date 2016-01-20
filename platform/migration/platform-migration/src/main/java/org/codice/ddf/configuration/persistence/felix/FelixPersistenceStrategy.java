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
package org.codice.ddf.configuration.persistence.felix;

import static org.apache.commons.lang.Validate.notNull;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;

import org.apache.felix.cm.file.ConfigurationHandler;
import org.codice.ddf.configuration.persistence.PersistenceStrategy;
import org.codice.ddf.configuration.status.ConfigurationFileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that persists configuration properties using the Felix' file format.
 */
public class FelixPersistenceStrategy implements PersistenceStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(FelixPersistenceStrategy.class);

    @Override
    @SuppressWarnings("unchecked")
    public Dictionary<String, Object> read(InputStream inputStream)
            throws ConfigurationFileException, IOException {
        notNull(inputStream, "InputStream cannot be null");

        final StringBuilder filteredOutput = new StringBuilder();

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        PropertyConverter propertyConverter = createPropertyConverter(filteredOutput);

        reader.lines()
                .forEach(propertyConverter);

        LOGGER.debug("Calling ConfigurationHandler with {}", filteredOutput.toString());

        Dictionary properties;

        try {
            properties =
                    ConfigurationHandler.read(new ByteArrayInputStream(filteredOutput.toString()
                            .getBytes(StandardCharsets.UTF_8)));
        } catch (RuntimeException e) {
            LOGGER.error("ConfigurationHandler failed to read configuration from file", e);
            throw new ConfigurationFileException("Failed to read configuration from file", e);
        }

        checkForInvalidProperties(propertyConverter.getPropertyNames(), properties);

        return properties;
    }

    @Override
    public void write(OutputStream outputStream, Dictionary<String, Object> properties)
            throws IOException {
        notNull(outputStream, "OutputStream cannot be null");
        notNull(properties, "Properties cannot be null");

        ConfigurationHandler.write(outputStream, properties);
    }

    PropertyConverter createPropertyConverter(StringBuilder filteredOutput) {
        return new PropertyConverter(filteredOutput);
    }

    /*
     * Checks that all properties found by the propertyConverter object have been returned
     * by the ConfigurationHandler class. If not, it means that ConfigurationHandler failed
     * to read in one of the values and that something is invalid in the file. This is needed
     * to work-around the problem with ConfigurationHandler not reporting parsing errors.
     */
    private void checkForInvalidProperties(Set<String> expectedPropertyName, Dictionary properties)
            throws ConfigurationFileException {
        if (properties.size() != expectedPropertyName.size()) {
            @SuppressWarnings("unchecked")
            Set<String> propertyNames = new HashSet<>(Collections.list(properties.keys()));

            LOGGER.error("Unable to convert all config file properties. One of [{}] is invalid",
                    expectedPropertyName.removeAll(propertyNames));
            throw new ConfigurationFileException("Unable to convert all config file properties.");
        }
    }
}
