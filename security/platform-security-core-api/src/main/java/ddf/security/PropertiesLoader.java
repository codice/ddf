/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.security;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.support.PropertiesLoaderUtils;

/**
 * Utility class that attempts several different methods for loading in properties files from the
 * classpath or file system.
 * 
 */
public final class PropertiesLoader {
    private static Logger LOGGER = LoggerFactory.getLogger(PropertiesLoader.class);

    private PropertiesLoader() {
        // static only!
    }

    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> toMap(Properties properties) {
        if (properties != null) {
            Set<K> keySet = (Set<K>) properties.keySet();
            Map<K, V> map = new HashMap<K, V>(keySet.size() * 2);
            for (K obj : keySet) {
                map.put(obj, (V) properties.get(obj));
            }
            return map;
        }
        return new HashMap<K, V>();
    }

    /**
     * Load properties from a file with no classloader specified.
     * 
     * @param propertiesFile
     * @return Properties
     */
    public static Properties loadProperties(String propertiesFile) {
        return loadProperties(propertiesFile, null);
    }

    /**
     * Will attempt to load properties from a file using the given classloader. If that fails,
     * several other methods will be tried until the properties file is located.
     * 
     * @param propertiesFile
     * @param classLoader
     * @return Properties
     */
    public static Properties loadProperties(String propertiesFile, ClassLoader classLoader) {
        boolean error = false;
        Properties properties = new Properties();
        if (propertiesFile != null) {
            try {
                LOGGER.debug("Attempting to load properties from {} with Spring PropertiesLoaderUtils.", propertiesFile);
                properties = PropertiesLoaderUtils.loadAllProperties(propertiesFile);
            } catch (IOException e) {
                error = true;
                LOGGER.error("Unable to load properties using default Spring properties loader.", e);
            }
            if (error || properties.isEmpty()) {
                if (classLoader != null) {
                    try {
                        LOGGER.debug("Attempting to load properties from {} with Spring PropertiesLoaderUtils with class loader.", propertiesFile);
                        properties = PropertiesLoaderUtils.loadAllProperties(propertiesFile,
                                classLoader);
                        error = false;
                    } catch (IOException e) {
                        error = true;
                        LOGGER.error(
                                "Unable to load properties using default Spring properties loader.",
                                e);
                    }
                } else {
                    try {
                        LOGGER.debug("Attempting to load properties from {} with Spring PropertiesLoaderUtils with class loader.", propertiesFile);
                        properties = PropertiesLoaderUtils.loadAllProperties(propertiesFile,
                                PropertiesLoader.class.getClassLoader());
                        error = false;
                    } catch (IOException e) {
                        error = true;
                        LOGGER.error(
                                "Unable to load properties using default Spring properties loader.",
                                e);
                    }
                }
            }

            if (error || properties.isEmpty()) {
                LOGGER.debug("Attempting to load properties from file system: {}", propertiesFile);
                File propFile = new File(propertiesFile);
                // If properties file has fully-qualified absolute path (which
                // the blueprint file specifies) then can load it directly.
                if (propFile.isAbsolute()) {
                    LOGGER.debug("propertiesFile {} is absolute", propertiesFile);
                    propFile = new File(propertiesFile);
                } else {
                    String karafHome = System.getProperty("karaf.home");
                    if (karafHome != null && !karafHome.isEmpty()) {
                        propFile = new File(karafHome, propertiesFile);
                    } else {
                        karafHome = System.getProperty("ddf.home");
                        if (karafHome != null && !karafHome.isEmpty()) {
                            propFile = new File(karafHome, propertiesFile);
                        } else {
                            propFile = new File(propertiesFile);
                        }
                    }
                }
                properties = new Properties();
                FileReader reader = null;
                try {
                    reader = new FileReader(propFile);
                    properties.load(reader);
                } catch (FileNotFoundException e) {
                    error = true;
                    LOGGER.error("Could not find properties file: {}", propFile.getAbsolutePath(), e);
                } catch (IOException e) {
                    error = true;
                    LOGGER.error("Error reading properties file: {}", propFile.getAbsolutePath(), e);
                } finally {
                    IOUtils.closeQuietly(reader);
                }
            }
            if (error || properties.isEmpty()) {
                LOGGER.debug("Attempting to load properties as a resource: {}", propertiesFile);
                InputStream ins = PropertiesLoader.class.getResourceAsStream(propertiesFile);
                if (ins != null) {
                    try {
                        properties.load(ins);
                        ins.close();
                    } catch (IOException e) {
                        LOGGER.error("Unable to load properties: {}", propertiesFile, e);
                    } finally {
                        IOUtils.closeQuietly(ins);
                    }
                }
            }
        } else {
            LOGGER.debug("Properties file must not be null.");
        }

        return properties;
    }
}
