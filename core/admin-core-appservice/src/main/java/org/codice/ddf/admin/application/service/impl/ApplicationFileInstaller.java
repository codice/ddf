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
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.aries.util.io.IOUtils;
import org.codice.ddf.admin.application.service.ApplicationServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationFileInstaller {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationFileInstaller.class);

    private static final String REPO_LOCATION = "system" + File.separator;

    private static final String URI_PROTOCOL = "file:";

    /**
     * Installs the given application file to the system repository.
     * 
     * @param application
     *            Application file to install.
     * @return A string URI that points to the main feature file for the
     *         application that was just installed.
     * @throws ApplicationServiceException
     *             If any errors occur while trying to install the application
     */
    public static URI install(File application) throws ApplicationServiceException {
        // extract files to local repo
        ZipFile appZip = null;
        try {
            appZip = new ZipFile(application);
            if (isFileValid(appZip)) {
                logger.debug("Installing {} to the system repository.",
                        application.getAbsolutePath());
                String featureLocation = installToRepo(appZip);
                String uri = URI_PROTOCOL + new File("").getAbsolutePath() + File.separator
                        + REPO_LOCATION + featureLocation;

                // lets standardize the file separators in this uri.
                // It fails on windows if we do not use.
                uri = uri.replace("\\", "/");
                return new URI(uri);
            }

        } catch (ZipException ze) {
            logger.warn("Got an error when trying to read the application as a zip file.", ze);
        } catch (IOException ioe) {
            logger.warn("Got an error when trying to read the incoming application.", ioe);
        } catch (URISyntaxException e) {
            logger.warn(
                    "Installed application but could not obtain correct location to feature file.",
                    e);
        } finally {
            try {
                IOUtils.close(appZip);
            } catch (IOException e) {
                logger.warn("Unable to close zip stream.", e);
            }
        }

        throw new ApplicationServiceException("Could not install application.");
    }

    public void update(File application) {
        // uninstall original application (if there is one)

        // install new application

    }

    public void uninstall(File application) {
        // find feature repo from artifact

        // remove from features service repo

    }

    /**
     * Verifies that the file is a file Karaf ARchive
     * 
     * @param appZip
     *            Zip file that should be checked.
     * @return true if the file is a valid kar, false if not.
     */
    private static boolean isFileValid(ZipFile appZip) {
        Enumeration<?> entries = appZip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry curEntry = (ZipEntry) entries.nextElement();
            if (!curEntry.isDirectory()) {
                if (isFeatureFile(curEntry)) {
                    logger.info(
                            "Found a feature in the application: {} which verifies this is a Karaf ARchive.",
                            curEntry.getName());
                    return true;
                }
            }
        }
        return false;
    }

    private static String installToRepo(ZipFile appZip) {
        String featureLocation = null;
        Enumeration<?> entries = appZip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry curEntry = (ZipEntry) entries.nextElement();
            if (!curEntry.isDirectory() && !curEntry.getName().startsWith("META-INF")) {
                try {
                    InputStream is = appZip.getInputStream(curEntry);
                    String outputName = curEntry.getName().substring("repository/".length());
                    logger.info("Writing out {}", curEntry.getName());
                    IOUtils.writeOut(new File(REPO_LOCATION), outputName, is);
                    if (isFeatureFile(curEntry)) {
                        featureLocation = outputName;
                    }
                } catch (IOException e) {
                    logger.warn("Could not write out file.", e);
                }
            }
        }
        return featureLocation;
    }

    private static boolean isFeatureFile(ZipEntry entry) {
        return entry.getName().endsWith("-features.xml");
    }

}
