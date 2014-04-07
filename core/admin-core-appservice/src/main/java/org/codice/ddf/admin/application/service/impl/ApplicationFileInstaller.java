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
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationFileInstaller {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationFileInstaller.class);

    public static void install(File application) {
        // extract files to local repo
        ZipFile appZip;
        try {
            appZip = new ZipFile(application);
            Enumeration<?> entries = appZip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry curEntry = (ZipEntry) entries.nextElement();
                if(!curEntry.isDirectory()) {
                   if (curEntry.getName().endsWith("-features.xml")) {
                       logger.info("Found a feature in the application: {}", curEntry.getName());
                   }
                }
            }

        } catch (ZipException ze) {
            logger.warn("Got an error when trying to read the application as a zip file.", ze);
        } catch (IOException ioe) {
            logger.warn("Got an error when trying to read the incoming application.", ioe);
        }

        // add to features service repo list
    }

    public void update(File application) {
        // uninstall original application (if there is one)

        // install new application

    }

    public void uninstall(File application) {
        // find feature repo from artifact

        // remove from features service repo

    }

}
