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
package org.codice.solr.factory.impl;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.codice.ddf.configuration.AbsolutePathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstraction layer for accessing files or directories on disk. Provides different implementations
 * depending on if the code is run within an OSGi container or not.
 */
public class ConfigurationFileProxy {

    public static final String DEFAULT_SOLR_CONFIG_PARENT_DIR = "etc";

    public static final String DEFAULT_SOLR_DATA_PARENT_DIR =
            new AbsolutePathResolver("data/solr").getPath();

    public static final String CATALOG_SOLR_COLLECTION_NAME = "metacard";

    protected static final List<String> SOLR_CONFIG_FILES =
            Collections.unmodifiableList(Arrays.asList("protwords.txt",
                    "schema.xml",
                    "solr.xml",
                    "solrconfig.xml",
                    "solrconfig-inmemory.xml",
                    "stopwords.txt",
                    "stopwords_en.txt",
                    "synonyms.txt"));

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationFileProxy.class);

    private File dataDirectory = null;

    /**
     * Constructor for the proxy
     *
     */
    public ConfigurationFileProxy(ConfigurationStore configurationStore) {
        LOGGER.debug("Creating new instance of {}", ConfigurationFileProxy.class.getSimpleName());
        String storedDataDirectoryPath = configurationStore.getDataDirectoryPath();

        if (isNotBlank(storedDataDirectoryPath)) {
            this.dataDirectory = new File(storedDataDirectoryPath);
            LOGGER.debug("1. dataDirectory set to [{}]", storedDataDirectoryPath);
        } else {
            this.dataDirectory = new File(DEFAULT_SOLR_DATA_PARENT_DIR);
            LOGGER.debug("2. dataDirectory set to [{}]", this.dataDirectory.getAbsolutePath());
        }
    }

    public void writeSolrConfiguration(String core) {
        writeSolrConfiguration(Paths.get(this.dataDirectory.getAbsolutePath(), core, "conf")
                .toFile());
    }

    /**
     * Writes the solr configuration files out of the classpath onto the disk.
     */
    public void writeSolrConfiguration(File configDir) {

        boolean directoriesMade = configDir.mkdirs();
        LOGGER.debug("Solr Config directories made?  {}", directoriesMade);

        for (String filename : SOLR_CONFIG_FILES) {
            try (InputStream inputStream = ConfigurationFileProxy.class.getClassLoader()
                    .getResourceAsStream("solr/conf/" + filename)) {
                File currentFile = new File(configDir, filename);

                if (!currentFile.exists()) {
                    try (FileOutputStream outputStream = new FileOutputStream(currentFile)) {
                        long byteCount = IOUtils.copyLarge(inputStream, outputStream);
                        LOGGER.debug("Wrote out {} bytes.", byteCount);
                    }
                }
            } catch (IOException e) {
                LOGGER.debug("IO exception copying out file", e);
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

    public URL getResource(String configFilename, String core) {
        File resourceFile = Paths.get(this.dataDirectory.getAbsolutePath(),
                core,
                "conf",
                configFilename)
                .toFile();
        if (resourceFile.exists()) {
            try {
                return resourceFile.toURI()
                        .toURL();
            } catch (MalformedURLException e) {
                LOGGER.info("Malformed URL exception getting SOLR configuration file", e);
            }
        }

        return this.getClass()
                .getClassLoader()
                .getResource("solr/conf/" + configFilename);
    }

    @Override
    public String toString() {
        return this.getClass()
                .getSimpleName() + "-->[" + getDataDirectory() + "]";
    }
}
