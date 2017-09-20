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

import ddf.security.Subject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.EnumSet;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.Callable;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.karaf.features.FeaturesService;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.codice.ddf.admin.application.service.ApplicationServiceException;
import org.codice.ddf.security.common.Security;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Installs applications based on a configuration file. */
public class ApplicationConfigInstaller extends Thread {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfigInstaller.class);

  private static final Security SECURITY = Security.getInstance();

  private ApplicationService appService;

  private FeaturesService featuresService;

  private String postInstallFeatureStart;

  private String postInstallFeatureStop;

  private String fileName;

  /**
   * Constructor that creates the config installer.
   *
   * @param fileName Full pathname of the properties file.
   * @param appService Reference to the application server that should be called to start the
   *     applications.
   * @param featuresService Reference to the features service that should be used to stop and start
   *     and post-installation features.
   * @param postInstallFeatureStart Feature that should be started if installation occurs.
   * @param postInstallFeatureStop Feature that should be stopped if installation occurs.
   */
  public ApplicationConfigInstaller(
      String fileName,
      ApplicationService appService,
      FeaturesService featuresService,
      String postInstallFeatureStart,
      String postInstallFeatureStop) {
    this.fileName = fileName;
    this.appService = appService;
    this.featuresService = featuresService;
    this.postInstallFeatureStart = postInstallFeatureStart;
    this.postInstallFeatureStop = postInstallFeatureStop;
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
      if (!props.isEmpty()) {
        LOGGER.debug("Found applications to install from config.");
        for (Entry<Object, Object> curApp : props.entrySet()) {
          String appName = curApp.getKey().toString();
          String appLocation = curApp.getValue().toString();
          LOGGER.debug("Starting app {} at location: {}", appName, appLocation);

          try {
            if (StringUtils.isNotEmpty(appLocation)) {
              appService.addApplication(new URI(appLocation));
            }
            executeAsSystem(
                () -> {
                  appService.startApplication(appName);
                  return true;
                });
          } catch (ApplicationServiceException ase) {
            LOGGER.warn("Could not start " + appName, ase);
          } catch (URISyntaxException use) {
            LOGGER.warn(
                "Could not install application, location is not a valid URI " + appLocation, use);
          }
        }
        LOGGER.debug("Finished installing applications, uninstalling installer module...");

        try {
          if (!StringUtils.isBlank(postInstallFeatureStart)) {
            featuresService.installFeature(
                postInstallFeatureStart, EnumSet.of(FeaturesService.Option.NoAutoRefreshBundles));
          }
          if (!StringUtils.isBlank(postInstallFeatureStop)) {
            featuresService.uninstallFeature(postInstallFeatureStop);
          }
        } catch (Exception e) {
          LOGGER.debug("Error while trying to run the post-install start and stop operations.", e);
        }

      } else {
        LOGGER.debug("No applications were found in the configuration file.");
      }
      is.close();
    } catch (FileNotFoundException fnfe) {
      LOGGER.warn("Could not file the configuration file at " + configFile.getAbsolutePath(), fnfe);
    } catch (IOException ioe) {
      LOGGER.warn("Could not load file as property list.", ioe);
    } finally {
      IOUtils.closeQuietly(is);
    }
  }

  private <T> T executeAsSystem(Callable<T> func) {
    Subject systemSubject = getSystemSubject();
    if (systemSubject == null) {
      throw new RuntimeException("Could not get system user to auto install applications.");
    }
    return systemSubject.execute(func);
  }

  Subject getSystemSubject() {
    return SECURITY.runAsAdmin(SECURITY::getSystemSubject);
  }
}
