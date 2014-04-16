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
package org.codice.admin.modules.installer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.codice.ddf.admin.application.service.ApplicationServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationConfigInstaller {

    private ApplicationService appService;

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfigInstaller.class);

    private static final String CONFIG_LOCATION = "etc" + File.separator;

    public ApplicationConfigInstaller(ApplicationService appService) {
        this.appService = appService;
    }

    public void setInstallFromConfig(String fileName) {
        Installer configInstaller = new Installer(fileName);
        configInstaller.start();
    }

    class Installer extends Thread {

        String fileName;

        public Installer(String fileName) {
            this.fileName = fileName;
        }

        @Override
        public void run() {
            
                File configFile = new File(CONFIG_LOCATION + fileName);
                InputStream is = null;
                try {
                    is = new FileInputStream(configFile);
                    Properties props = new Properties();
                    props.load(is);
                    is.close();

                    for (Entry<Object, Object> curApp : props.entrySet()) {
                        LOGGER.debug("Starting app {} at location: {}", curApp.getKey(),
                                curApp.getValue());

                        try {
                            appService.startApplication(curApp.getKey().toString());
                        } catch (ApplicationServiceException ase) {
                            LOGGER.warn("Could not start " + curApp.getKey().toString(), ase);
                        }

                    }

                    LOGGER.debug("DONE INSTALLING FROM CONFIG.");
                } catch (FileNotFoundException fnfe) {
                    LOGGER.warn(
                            "Could not file the configuration file at "
                                    + configFile.getAbsolutePath(), fnfe);
                } catch (IOException ioe) {
                    LOGGER.warn("Could not load file as property list.", ioe);
                } finally {
                    IOUtils.closeQuietly(is);
                }
        }
    }

}
