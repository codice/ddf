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
package org.codice.ddf.admin.configuration;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Dictionary;
import java.util.Objects;
import java.util.UUID;

import javax.annotation.Nullable;

import org.osgi.service.cm.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for enabling functional constructs with {@link Configuration}s and exposes the
 * felix.fileinstall.filename property, if it exists, as a {@link File}.
 */
public class FelixConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(FelixConfig.class);

    private static final String FELIX_FILENAME_PROP = "felix.fileinstall.filename";

    private static final String SERVICE_FACTORY_PID = "service.factoryPid";

    private static final String SERVICE_PID = "service.pid";

    private final Configuration config;

    private File felixFile;

    FelixConfig(Configuration config) {
        if (config == null) {
            throw new IllegalArgumentException("Configuration can't be null");
        }
        this.config = config;
        this.felixFile = extractFelixFileInstallProp(config);
    }

    /**
     * @return a properties dictionary for the config that can be written to etc without interfering
     * with Felix's monitoring and processing. There are some values we should not serialize because
     * they are handled in a special way by Felix.
     */
    Dictionary<String, Object> getSanitizedProperties() {
        Dictionary<String, Object> props = config.getProperties();
        props.remove(FELIX_FILENAME_PROP);
        props.remove(SERVICE_FACTORY_PID);
        props.remove(SERVICE_PID);
        return props;
    }

    /**
     * @return the pid for this configuration.
     */
    String getPid() {
        return config.getPid();
    }

    /**
     * @return the File object pointing to the this configuration on disk.
     */
    @Nullable
    File getFelixFile() {
        return felixFile;
    }

    /**
     * Sets the felix file name to the provided {@link File} argument.
     *
     * @param file the file to use for the felix file name property.
     */
    void setFelixFile(File file) {
        Dictionary<String, Object> propsWithFelixFileName = config.getProperties();
        propsWithFelixFileName.put(FELIX_FILENAME_PROP,
                file.toURI()
                        .toString());
        try {
            config.update(propsWithFelixFileName);
            this.felixFile = file;
        } catch (IOException e) {
            LOGGER.error("Error writing configuration");
            LOGGER.debug("Could not set felix file name, error writing configuration: ", e);
            this.felixFile = null;
        }
    }

    /**
     * Sets the felix file name using the default technique from the config repo, using .config by
     * default in case complex data structures are present.
     *
     * @return <code>this</code> for chaining.
     */
    FelixConfig generateDefaultFelixFile() {
        setFelixFile(createFelixFileName(config));
        return this;
    }

    /**
     * Deletes the configuration from config admin.
     */
    void delete() {
        try {
            config.delete();
        } catch (IOException e) {
            LOGGER.error("Error deleting configuration");
            LOGGER.debug("Could not delete configuration: ", e);
        }
    }

    /**
     * Determine if the felix.fileinstall.filename property changed.
     *
     * @param original file location to compare to the current file location poitned to by this
     *                 config.
     * @return False if the file locations by absolute path are identical. True otherwise.
     */
    boolean filePropChanged(File original) {
        return !Objects.equals(felixFile, original);
    }

    private File extractFelixFileInstallProp(Configuration config) {
        if (config.getProperties() == null) {
            LOGGER.debug("Config properties are null, they may have just been created");
            return null;
        }
        Object felixConfigFileName = config.getProperties()
                .get(FELIX_FILENAME_PROP);
        if (felixConfigFileName == null) {
            LOGGER.debug("Felix filename prop not found for pid {}", config.getPid());
            return null;
        }
        return createFileFromFelixProp(felixConfigFileName);
    }

    /**
     * Code adopted from Karaf's Config Repository Impl:
     * https://github.com/apache/karaf/blob/master/config/src/main/java/org/apache/karaf/config/core/impl/
     * ConfigRepositoryImpl.java#L101-L114
     */
    private File createFileFromFelixProp(Object felixConfigFileName) {
        try {
            if (felixConfigFileName instanceof URL) {
                return new File(((URL) felixConfigFileName).toURI());
            }
            if (felixConfigFileName instanceof URI) {
                return new File((URI) felixConfigFileName);
            }
            if (felixConfigFileName instanceof String) {
                return new File(new URL((String) felixConfigFileName).toURI());
            }
        } catch (URISyntaxException | MalformedURLException e) {
            LOGGER.debug(
                    "Was expecting a correctly formatted URL or URI for felix file name [{}], but got: {}",
                    felixConfigFileName,
                    e);
            return null;
        }
        LOGGER.debug("Unexpected type for felix file name: {}", felixConfigFileName.getClass());
        return null;
    }

    /**
     * Code adopted from Karaf's Config Repository Impl
     */
    private File createFelixFileName(Configuration configuration) {
        final String fpid = configuration.getFactoryPid();
        final String bname;

        if (fpid != null) {
            LOGGER.debug("Generating felix filename for factory pid {}", fpid);
            final String alias = UUID.randomUUID()
                    .toString()
                    .replaceAll("-", "");
            bname = format("%s-%s", fpid, alias);
        } else {
            LOGGER.debug("Generating felix filename for pid {}", configuration.getPid());
            bname = configuration.getPid();
        }

        String ddfHome = System.getProperty("ddf.home");
        if (ddfHome == null) {
            throw new IllegalStateException("DDF_HOME not set");
        }

        return Paths.get(ddfHome, "etc", bname + ".config")
                .toFile();
    }
}