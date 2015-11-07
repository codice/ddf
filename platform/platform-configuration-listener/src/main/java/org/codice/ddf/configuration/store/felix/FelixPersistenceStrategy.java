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
package org.codice.ddf.configuration.store.felix;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Dictionary;

import org.apache.felix.cm.file.ConfigurationHandler;
import org.codice.ddf.configuration.store.PersistenceStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that persists configuration properties using the Felix' file format.
 */
public class FelixPersistenceStrategy implements PersistenceStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(FelixPersistenceStrategy.class);

    @Override
    @SuppressWarnings("unchecked")
    public Dictionary<String, Object> read(InputStream inputStream) throws IOException {
        final StringBuilder filteredOutput = new StringBuilder();

        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        PropertyConverter propertyConverter = createPropertyConverter(filteredOutput);
        reader.lines().forEach(propertyConverter);

        LOGGER.debug("Calling ConfigurationHandler with {}", filteredOutput.toString());

        return ConfigurationHandler
                .read(new ByteArrayInputStream(filteredOutput.toString().getBytes()));
    }

    @Override
    public void write(OutputStream outputStream, Dictionary<String, Object> properties)
            throws IOException {
        ConfigurationHandler.write(outputStream, properties);
    }

    PropertyConverter createPropertyConverter(StringBuilder filteredOutput) {
        return new PropertyConverter(filteredOutput);
    }
}
