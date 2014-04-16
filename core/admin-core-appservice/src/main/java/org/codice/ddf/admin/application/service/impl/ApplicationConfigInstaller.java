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
package org.codice.ddf.admin.application.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.karaf.features.FeaturesService;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.codice.ddf.admin.application.service.ApplicationServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Installs applications based on a configuration file.
 */
public class ApplicationConfigInstaller extends Thread {

    private ApplicationService appService;

    private FeaturesService featuresService;

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfigInstaller.class);

    private String fileName;

    /**
     * Constructor of the Installer class.
     * 
     * @param fileName
     *            Name of the configuration file to install the applications
     *            from.
     */
    public ApplicationConfigInstaller(String fileName, ApplicationService appService,
            FeaturesService featuresService) {
        this.fileName = fileName;
        this.appService = appService;
        this.featuresService = featuresService;
    }

    @Override
    public void run() {

        File configFile = new File(fileName);
        if (!configFile.exists()) {
            LOGGER.debug("No config file located, cannot load from it.");
            return;
        }
        InputStream is = null;
        try {
            is = new FileInputStream(configFile);
            Properties props = new Properties();
            props.load(is);
            is.close();
            if (!props.isEmpty()) {
                LOGGER.debug("Found applications to install from config.");
                for (Entry<Object, Object> curApp : props.entrySet()) {
                    LOGGER.debug("Starting app {} at location: {}", curApp.getKey(),
                            curApp.getValue());

                    try {
                        appService.startApplication(curApp.getKey().toString());
                    } catch (ApplicationServiceException ase) {
                        LOGGER.warn("Could not start " + curApp.getKey().toString(), ase);
                    }

                }
                LOGGER.debug("Finished installing applications, uninstalling installer module...");

                try {
                    featuresService.installFeature("admin-post-install-modules");
                    featuresService.uninstallFeature("admin-modules-installer");
                } catch (Throwable e) {
                    LOGGER.debug(
                            "Error while trying to uninstall the installer admin module. Installer may still active.",
                            e);
                }

            } else {
                LOGGER.debug("No applications were found in the configuration file.");
            }

        } catch (FileNotFoundException fnfe) {
            LOGGER.warn("Could not file the configuration file at " + configFile.getAbsolutePath(),
                    fnfe);
        } catch (IOException ioe) {
            LOGGER.warn("Could not load file as property list.", ioe);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

}
