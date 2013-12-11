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
package ddf.catalog.source.solr;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;

/**
 * Abstraction layer for accessing files or directories on disk. Provides different implementations
 * depending on if the code is run within an OSGi container or not.
 */
public class ConfigurationFileProxy {

    public static final String DEFAULT_SOLR_CONFIG_PARENT_DIR = "etc";

    public static final String SOLR_CONFIG_LOCATION_IN_BUNDLE = "solr/conf";

    private static final String DEFAULT_SOLR_DATA_PARENT_DIR = "data";

    private BundleContext bundleContext;

    private File dataDirectory = null;

    private static final Logger LOGGER = Logger.getLogger(ConfigurationFileProxy.class);

    /**
     * Constructor for the proxy
     * 
     * @param bundleContext
     *            This is mandatory for running in an OSGi container; the BundleContext is used to
     *            find the location of configuration files within this bundle
     */
    public ConfigurationFileProxy(BundleContext bundleContext, ConfigurationStore configurationStore) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Creating new instance of " + ConfigurationFileProxy.class.getSimpleName());
        }

        this.bundleContext = bundleContext;

        String storedDataDirectoryPath = configurationStore.getDataDirectoryPath();

        if (isNotBlank(storedDataDirectoryPath)) {
            this.dataDirectory = new File(storedDataDirectoryPath);
            LOGGER.info("dataDirectory set to [" + storedDataDirectoryPath + "]");
        } else {
            this.dataDirectory = new File(DEFAULT_SOLR_DATA_PARENT_DIR, "solr");
        }

    }

    /**
     * Writes the solr configuration files out of the bundle onto the disk. This method requires
     * that the dataDirectoryPath has been set. If the code is run in an OSGi container, it will
     * automatically have a default dataDirectory location set and will not require setting
     * dataDirectory ahead of time.
     */
    public void writeBundleFilesTo(File configDir) {
        if (bundleContext != null && configDir != null) {

            boolean directoriesMade = configDir.mkdirs();

            LOGGER.info("Solr Config directories made?  " + directoriesMade);

            @SuppressWarnings("rawtypes")
            Enumeration entries = bundleContext.getBundle().findEntries(
                    SOLR_CONFIG_LOCATION_IN_BUNDLE, "*.*", false);

            while (entries.hasMoreElements()) {
                URL resourceURL = (URL) (entries.nextElement());
                LOGGER.debug("Found " + resourceURL);

                InputStream inputStream = null;
                try {
                    inputStream = resourceURL.openStream();

                    String fileName = FilenameUtils.getName(resourceURL.getPath());

                    File currentFile = new File(configDir, fileName);

                    if (!currentFile.exists()) {
                        FileOutputStream outputStream = null;

                        try {
                            outputStream = new FileOutputStream(currentFile);

                            long byteCount = IOUtils.copyLarge(inputStream, outputStream);

                            LOGGER.debug("Wrote out " + byteCount + " bytes.");

                        } finally {
                            IOUtils.closeQuietly(outputStream);
                        }
                    }

                } catch (IOException e) {
                    LOGGER.warn(e);
                } finally {
                    IOUtils.closeQuietly(inputStream);
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

        if (bundleContext != null) {

            try {
                return new File(new File(new File(DEFAULT_SOLR_CONFIG_PARENT_DIR, "solr"), "conf"),
                        name).toURI().toURL();
            } catch (MalformedURLException e) {
                LOGGER.warn(e);
            }
        }

        return this.getClass().getClassLoader().getResource("solr/conf/" + name);

    }

    @Override
    public String toString() {

        return this.getClass().getSimpleName() + "-->[" + getDataDirectory() + "]";
    }
}
