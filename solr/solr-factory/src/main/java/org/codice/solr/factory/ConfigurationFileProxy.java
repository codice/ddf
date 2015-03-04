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
package org.codice.solr.factory;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;

import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * Abstraction layer for accessing files or directories on disk. Provides different implementations
 * depending on if the code is run within an OSGi container or not.
 */
public class ConfigurationFileProxy {

    public static final String DEFAULT_SOLR_CONFIG_PARENT_DIR = "etc";

    public static final String SOLR_CONFIG_LOCATION_IN_BUNDLE = "solr/conf";

    public static final String DEFAULT_SOLR_DATA_PARENT_DIR = "data/solr";
    
    public static final String CATALOG_SOLR_COLLECTION_NAME = "metacard";

    private File dataDirectory = null;

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationFileProxy.class);

    /**
     * Constructor for the proxy
     *
     */
    public ConfigurationFileProxy(ConfigurationStore configurationStore) {
        LOGGER.debug("Creating new instance of {}", ConfigurationFileProxy.class.getSimpleName());
        String storedDataDirectoryPath = configurationStore.getDataDirectoryPath();

        if (isNotBlank(storedDataDirectoryPath)) {
            this.dataDirectory = new File(storedDataDirectoryPath);
            LOGGER.info("1. dataDirectory set to [{}]", storedDataDirectoryPath);
        } else {
            this.dataDirectory = new File(DEFAULT_SOLR_DATA_PARENT_DIR);
            LOGGER.info("2. dataDirectory set to [{}]", this.dataDirectory.getAbsolutePath());
        }
    }

    protected BundleContext getContext() {
        Bundle cxfBundle = FrameworkUtil.getBundle(ConfigurationFileProxy.class);
        if (cxfBundle != null) {
            return cxfBundle.getBundleContext();
        }
        return null;
    }

    /**
     * Writes the solr configuration files out of the bundle onto the disk. This method requires
     * that the dataDirectoryPath has been set. If the code is run in an OSGi container, it will
     * automatically have a default dataDirectory location set and will not require setting
     * dataDirectory ahead of time.
     */
    public void writeBundleFilesTo(File configDir) {
        BundleContext bundleContext = getContext();
        if (bundleContext != null && configDir != null) {
            boolean directoriesMade = configDir.mkdirs();
            LOGGER.info("Solr Config directories made?  {}", directoriesMade);

            @SuppressWarnings("rawtypes")
            Enumeration entries = bundleContext.getBundle().findEntries(
                    SOLR_CONFIG_LOCATION_IN_BUNDLE, "*.*", false);

            while (entries.hasMoreElements()) {
                URL resourceURL = (URL) (entries.nextElement());
                LOGGER.debug("Found {}", resourceURL);

                try (InputStream inputStream = resourceURL.openStream()) {
                    String fileName = FilenameUtils.getName(resourceURL.getPath());
                    File currentFile = new File(configDir, fileName);

                    if (!currentFile.exists()) {
                        try (FileOutputStream outputStream = new FileOutputStream(currentFile)) {
                            long byteCount = IOUtils.copyLarge(inputStream, outputStream);
                            LOGGER.debug("Wrote out {} bytes.", byteCount);
                        }
                    }
                } catch (IOException e) {
                    LOGGER.warn("IO exception copying out file", e);
                }
            }
        }
    }

    /**
     * 
     * @return the File of the directory where data can be written, should never return {@code null}
     */
    public File getDataDirectory() {
        return this.dataDirectory;
    }

    public URL getResource(String name) {
        BundleContext bundleContext = getContext();
        if (bundleContext != null) {
            try {
                return new File(new File(DEFAULT_SOLR_DATA_PARENT_DIR + "/" + CATALOG_SOLR_COLLECTION_NAME + "/conf"),
                        name).toURI().toURL();
            } catch (MalformedURLException e) {
                LOGGER.warn("Malformed URL exception getting SOLR configuration file", e);
            }
        }

        return this.getClass().getClassLoader().getResource("solr/conf/" + name);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "-->[" + getDataDirectory() + "]";
    }
}
