/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.admin.application.service.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.IOUtils;
import org.apache.karaf.features.Feature;
import org.codice.ddf.admin.application.service.ApplicationServiceException;
import org.codice.ddf.platform.util.XMLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ApplicationFileInstaller {

  private static final String FEATURE_TAG_NAME = "feature";

  private static final String NAME_ATTRIBUTE_NAME = "name";

  private static final String VERSION_ATTRIBUTE_NAME = "version";

  private static final String INSTALL_ATTRIBUTE_NAME = "install";

  private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationFileInstaller.class);

  private static final String REPO_LOCATION = "system" + File.separator;

  private static final XMLUtils XML_UTILS = XMLUtils.getInstance();

  /**
   * Installs the given application file to the system repository.
   *
   * @param application Application file to install.
   * @return A string URI that points to the main feature file for the application that was just
   *     installed.
   * @throws ApplicationServiceException If any errors occur while trying to install the
   *     application.
   */
  public static URI install(File application) throws ApplicationServiceException {
    // Extract files to local repo
    ZipFile appZip = null;
    try {
      appZip = new ZipFile(application);
      if (isFileValid(appZip)) {
        LOGGER.debug("Installing {} to the system repository.", application.getAbsolutePath());
        String featureLocation = installToRepo(appZip);
        String uri =
            new File("").getAbsolutePath() + File.separator + REPO_LOCATION + featureLocation;

        // Lets standardize the file separators in this uri.
        // It fails on windows if we do not use.
        return Paths.get(uri).toUri();
      }

    } catch (ZipException ze) {
      LOGGER.warn("Got an error when trying to read the application as a zip file.", ze);
    } catch (IOException ioe) {
      LOGGER.warn("Got an error when trying to read the incoming application.", ioe);
    } finally {
      IOUtils.closeQuietly(appZip);
    }

    throw new ApplicationServiceException("Could not install application.");
  }

  /**
   * Detects and Builds an AppDetail based on the zip file provided.
   *
   * <p>To start the process, we find the features.xml file. Once we find it within the zip file, we
   * specifically get a stream to that file. Next we parse through the features.xml and extract the
   * version/appname.
   *
   * @param applicationFile the file to detect appname and version from.
   * @return {@link ZipFileApplicationDetails} containing appname and version.
   * @throws ApplicationServiceException any errors that happening during extracting the
   *     appname/version from the zipfile.
   */
  public static ZipFileApplicationDetails getAppDetails(File applicationFile)
      throws ApplicationServiceException {
    ZipFile appZip = null;
    try {
      appZip = new ZipFile(applicationFile);
      LOGGER.debug(
          "Extracting version and application name from zipfile {}.",
          applicationFile.getAbsolutePath());

      ZipEntry featureFileEntry = getFeatureFile(appZip);
      return getAppDetailsFromFeature(appZip, featureFileEntry);

    } catch (IOException | SAXException | ParserConfigurationException e) {
      throw new ApplicationServiceException(
          "Could not get application details of the provided zipfile.", e);
    } finally {
      IOUtils.closeQuietly(appZip);
    }
  }

  /**
   * Verifies that the file is a file Karaf ARchive is a valid file.
   *
   * @param appZip Zip file that should be checked.
   * @return true if the file is a valid kar, false if not.
   */
  private static boolean isFileValid(ZipFile appZip) {
    Enumeration<?> entries = appZip.entries();
    while (entries.hasMoreElements()) {
      ZipEntry curEntry = (ZipEntry) entries.nextElement();
      if (!curEntry.isDirectory()) {
        if (isFeatureFile(curEntry)) {
          LOGGER.debug(
              "Found a feature in the application: {} which verifies this is a Karaf ARchive.",
              curEntry.getName());
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Extracts all files/folders from the provided zip file to our location defined in REPO_LOCATION.
   *
   * @param appZip the ZipFile to extract.
   * @return the detected feature file location.
   */
  private static String installToRepo(ZipFile appZip) {
    String featureLocation = null;
    Enumeration<?> entries = appZip.entries();
    while (entries.hasMoreElements()) {
      ZipEntry curEntry = (ZipEntry) entries.nextElement();
      if (!curEntry.isDirectory() && !curEntry.getName().startsWith("META-INF")) {
        try {
          InputStream is = appZip.getInputStream(curEntry);
          String outputName = curEntry.getName().substring("repository/".length());
          LOGGER.debug("Writing out {}", curEntry.getName());
          org.apache.aries.util.io.IOUtils.writeOut(new File(REPO_LOCATION), outputName, is);
          if (isFeatureFile(curEntry)) {
            featureLocation = outputName;
          }
        } catch (IOException e) {
          LOGGER.warn("Could not write out file.", e);
        }
      }
    }
    return featureLocation;
  }

  /**
   * Loops through all of zipFile's entries to find the features.xml file.
   *
   * @param zipFile The ZipFile to search for the feature file.
   * @return The ZipEntry representing the features.xml file.
   */
  private static ZipEntry getFeatureFile(ZipFile zipFile) {
    Enumeration<?> entries = zipFile.entries();
    while (entries.hasMoreElements()) {
      ZipEntry curEntry = (ZipEntry) entries.nextElement();
      if (!curEntry.isDirectory() && !curEntry.getName().startsWith("META-INF")) {
        if (isFeatureFile(curEntry)) {
          return curEntry;
        }
      }
    }
    return null;
  }

  /**
   * Detects if the given zip file entry matches the features.xml naming scheme.
   *
   * @param entry The zip entry to check.
   * @return <code>true</code> if the zip entry matches the features.xml file naming scheme.
   */
  private static boolean isFeatureFile(ZipEntry entry) {
    return entry.getName().endsWith("-features.xml");
  }

  /**
   * Does the grunt work of parsing through the features.xml file provided using {@link
   * ZipFile#getInputStream(ZipEntry)}. Currently we only parse the main feature which is denoted by
   * install=auto.
   *
   * @param zipFile zip file get the {@link InputStream} from.
   * @param featureZipEntry the specific file pointing to the features.xml file.
   * @return The {@link ZipFileApplicationDetails} of the given features.xml file.
   * @throws ApplicationServiceException Any error that occurs within this method will be wrapped in
   *     this.
   * @throws ParserConfigurationException
   * @throws IOException
   * @throws SAXException
   */
  private static ZipFileApplicationDetails getAppDetailsFromFeature(
      ZipFile zipFile, ZipEntry featureZipEntry)
      throws ParserConfigurationException, SAXException, IOException {

    // double check if this zip entry is a features.xml named file.
    if (!isFeatureFile(featureZipEntry)) {
      return null;
    }

    DocumentBuilder dBuilder = XML_UTILS.getSecureDocumentBuilder(false);
    Document doc = dBuilder.parse(zipFile.getInputStream(featureZipEntry));

    NodeList featureNodes = doc.getElementsByTagName(FEATURE_TAG_NAME);

    for (int i = 0; i < featureNodes.getLength(); i++) {
      Node curNode = featureNodes.item(i);
      if (curNode.getAttributes() != null
          && curNode.getAttributes().getNamedItem(INSTALL_ATTRIBUTE_NAME) != null
          && Feature.DEFAULT_INSTALL_MODE.equals(
              curNode.getAttributes().getNamedItem(INSTALL_ATTRIBUTE_NAME).getTextContent())) {
        String name = curNode.getAttributes().getNamedItem(NAME_ATTRIBUTE_NAME).getTextContent();
        String version =
            curNode.getAttributes().getNamedItem(VERSION_ATTRIBUTE_NAME).getTextContent();

        return new ZipFileApplicationDetails(name, version);
      }
    }
    return null;
  }
}
