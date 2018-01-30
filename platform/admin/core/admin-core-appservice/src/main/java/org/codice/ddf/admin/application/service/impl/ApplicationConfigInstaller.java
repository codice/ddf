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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.EnumSet;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.Callable;
import org.apache.commons.lang.StringUtils;
import org.apache.karaf.features.FeaturesService;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.codice.ddf.admin.application.service.ApplicationServiceException;
import org.codice.ddf.platform.util.properties.PropertiesLoader;
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
    // use PropertiesLoader to load properties, returns empty properties if there is an issue
    Properties props = PropertiesLoader.getInstance().loadProperties(fileName, null);

    if (props.isEmpty()) {
      LOGGER.debug("No applications were found in the configuration file.");
      return;
    }

    LOGGER.trace("Found applications to install from config.");
    for (Entry<Object, Object> curApp : props.entrySet()) {
      String appName = (String) curApp.getKey();
      String appLocation = (String) curApp.getValue();
      LOGGER.debug("Starting app {} at location: {}", appName, appLocation);

      startApp(appName, appLocation);
    }
    LOGGER.trace("Finished installing applications, uninstalling installer module...");

    installAndUninstallFeatures();
  }

  private void startApp(String appName, String appLocation) {
    try {
      if (!appLocation.isEmpty()) {
        appService.addApplication(new URI(appLocation));
      }
      executeAsSystem(
          () -> {
            appService.startApplication(appName);
            return true;
          });
    } catch (ApplicationServiceException ase) {
      LOGGER.warn("Could not start {}", appName, ase);
    } catch (URISyntaxException use) {
      LOGGER.warn(
          "Could not install application, location is not a valid URI {}", appLocation, use);
    }
  }

  private void installAndUninstallFeatures() {
    try {
      if (StringUtils.isNotBlank(postInstallFeatureStart)) {
        featuresService.installFeature(
            postInstallFeatureStart, EnumSet.of(FeaturesService.Option.NoAutoRefreshBundles));
      }
      if (StringUtils.isNotBlank(postInstallFeatureStop)) {
        featuresService.uninstallFeature(postInstallFeatureStop);
      }
    } catch (Exception e) {
      LOGGER.debug("Error while trying to run the post-install start and stop operations.", e);
    }
  }

  private <T> T executeAsSystem(Callable<T> func) throws ApplicationServiceException {
    Subject systemSubject = getSystemSubject();
    if (systemSubject == null) {
      throw new ApplicationServiceException(
          "Could not get system user to auto install applications.");
    }
    return systemSubject.execute(func);
  }

  Subject getSystemSubject() {
    return SECURITY.runAsAdmin(SECURITY::getSystemSubject);
  }
}
