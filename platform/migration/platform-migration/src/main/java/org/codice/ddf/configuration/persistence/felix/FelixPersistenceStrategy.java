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
package org.codice.ddf.configuration.persistence.felix;

import static org.apache.commons.lang.Validate.notNull;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.cm.file.ConfigurationHandler;
import org.apache.felix.utils.properties.Properties;
import org.codice.ddf.configuration.persistence.PersistenceStrategy;
import org.codice.ddf.migration.UnexpectedMigrationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that persists configuration properties using the Felix file formats (cfg and config).
 * This persistence strategy assumes that the properties passed in contain a parsable felix.fileinstall.filename
 * with a valid format (cfg or config).
 */
public class FelixPersistenceStrategy implements PersistenceStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(FelixPersistenceStrategy.class);

    private static final String FELIX_FILEINSTALL_FILENAME = "felix.fileinstall.filename";

    private static final String CFG_FORMAT = "cfg";

    private static final String CONFIG_FORMAT = "config";

    @Override
    public void write(OutputStream outputStream, Dictionary<String, Object> properties)
            throws IOException {
        notNull(outputStream, "OutputStream cannot be null");
        notNull(properties, "Properties cannot be null");

        String format = getFormat(properties);

        if (isCfgFormat(format)) {
            writeInCfgFormat(outputStream, properties);
        } else {
            writeInConfigFormat(outputStream, properties);
        }
    }

    private boolean isCfgFormat(String fileExtension) {
        return StringUtils.equals(fileExtension, CFG_FORMAT);
    }

    private void writeInConfigFormat(OutputStream outputStream,
            Dictionary<String, Object> properties) throws IOException {
        ConfigurationHandler.write(outputStream, properties);
    }

    private void writeInCfgFormat(OutputStream outputStream, Dictionary<String, Object> properties)
            throws IOException {
        Properties props = new Properties();
        Enumeration<String> keys = properties.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            props.put(key, (String) properties.get(key));
        }
        props.save(outputStream);
    }

    private String getFormat(Dictionary<String, Object> properties) {
        try {
            Object felixFileIntallFileName =  properties.get(FELIX_FILEINSTALL_FILENAME);

            File file = null;

            if (felixFileIntallFileName != null) {
                if (felixFileIntallFileName instanceof URL) {
                    file = new File(((URL) felixFileIntallFileName).toURI());
                } else if (felixFileIntallFileName instanceof URI) {
                    file = new File((URI) felixFileIntallFileName);
                } else {
                    file = new File(new URL((String) felixFileIntallFileName).toURI());
                }
                return FilenameUtils.getExtension(file.getName());
            } else {
                return CONFIG_FORMAT;
            }
        } catch (MalformedURLException | URISyntaxException e) {
            String message = String.format(
                    "Unable to get format from %s configuration property. %s",
                    FELIX_FILEINSTALL_FILENAME,
                    e.getMessage());
            throw new UnexpectedMigrationException(message, e);
        }
    }

}
