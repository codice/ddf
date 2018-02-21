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

import com.google.common.collect.Sets;
import ddf.security.permission.KeyValueCollectionPermission;
import ddf.security.permission.KeyValuePermission;
import ddf.security.service.SecurityServiceException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.apache.shiro.SecurityUtils;
import org.boon.Boon;
import org.codice.ddf.admin.application.rest.model.FeatureDetails;
import org.codice.ddf.admin.application.service.Application;
import org.codice.ddf.admin.application.service.ApplicationNode;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.codice.ddf.admin.application.service.ApplicationServiceException;
import org.codice.ddf.admin.application.service.ApplicationStatus;
import org.codice.ddf.admin.application.service.ApplicationStatus.ApplicationState;
import org.codice.ddf.security.common.Security;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the ApplicationService. Uses the karaf features service and bundle state
 * service to determine current state of items in karaf.
 */
public class ApplicationServiceImpl implements ApplicationService {

  private static final String INSTALLATION_PROFILE_PREFIX = "profile-";

  private static final String INSTALLED = "Installed";

  private static final String UNINSTALLED = "Uninstalled";

  private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationServiceImpl.class);

  private FeaturesService featuresService = null;

  private static final Path APPLICATION_DEFINITIONS_FOLDER =
      Paths.get("etc", "application-definitions");

  public ApplicationServiceImpl() {
    BundleContext context = getContext();

    ServiceReference<FeaturesService> featuresServiceRef =
        context.getServiceReference(FeaturesService.class);
    this.featuresService = context.getService(featuresServiceRef);
  }

  @Override
  public Set<Application> getApplications() {
    File[] appDefinitions = APPLICATION_DEFINITIONS_FOLDER.toFile().listFiles();
    if (appDefinitions == null) {
      LOGGER.warn("No application-definitions configuration files found.");
      return Collections.emptySet();
    }

    List<Application> apps = new ArrayList<>();
    for (File appDef : appDefinitions) {
      try {
        Application app = Boon.fromJson(IOUtils.toString(appDef.toURI()), ApplicationImpl.class);
        apps.add(app);
      } catch (IOException e) {
        LOGGER.warn("Unable to parse application from file {} ", appDef.getPath());
      }
    }

    return new TreeSet<>(apps);
  }

  @Override
  public Application getApplication(String applicationName) {
    for (Application curApp : getApplications()) {
      if (curApp.getName().equalsIgnoreCase(applicationName)) {
        return curApp;
      }
    }
    return null;
  }

  @Override
  public boolean isApplicationStarted(Application application) {
    return false;
  }

  @Override
  public ApplicationStatus getApplicationStatus(Application application) {
    return new ApplicationStatusImpl(
        application, ApplicationState.UNKNOWN, Collections.emptySet(), Collections.emptySet());
  }

  protected BundleContext getContext() {
    Bundle cxfBundle = FrameworkUtil.getBundle(ApplicationServiceImpl.class);
    if (cxfBundle != null && cxfBundle.getBundleContext() != null) {
      return cxfBundle.getBundleContext();
    }
    throw new IllegalStateException("Could not get context; cxfBundle is null");
  }

  @Override
  public Set<ApplicationNode> getApplicationTree() {
    Set<ApplicationNode> nodes = new HashSet<>();
    for (Application app : getApplications()) {
      nodes.add(new ApplicationNodeImpl(app));
    }

    return new TreeSet<>(nodes);
  }

  @Override
  public Application findFeature(Feature feature) {
    return null;
  }

  @Override
  public List<Feature> getInstallationProfiles() {
    LOGGER.debug("Looking for installation profile features");
    List<Feature> profiles = new ArrayList<>();
    try {
      profiles =
          Arrays.asList(featuresService.listFeatures())
              .stream()
              .filter(f -> f.getName().contains(INSTALLATION_PROFILE_PREFIX))
              .sorted(Comparator.comparingInt(Feature::getStartLevel))
              .collect(Collectors.toList());
    } catch (Exception e) {
      LOGGER.warn(
          "Encountered an error while trying to obtain the installation profile features.", e);
    }

    return profiles;
  }

  @Override
  public synchronized void startApplication(Application application)
      throws ApplicationServiceException {}

  @Override
  public void startApplication(String application) throws ApplicationServiceException {}

  @Override
  public synchronized void stopApplication(Application application)
      throws ApplicationServiceException {}

  @Override
  public void stopApplication(String application) throws ApplicationServiceException {}

  @Override
  public void addApplication(URI applicationURL) throws ApplicationServiceException {}

  @Override
  public void removeApplication(URI applicationURL) throws ApplicationServiceException {}

  @Override
  public void removeApplication(Application application) throws ApplicationServiceException {}

  @Override
  public void removeApplication(String applicationName) throws ApplicationServiceException {}

  @Override
  public List<FeatureDetails> getAllFeatures() {
    List<FeatureDetails> features = new ArrayList<FeatureDetails>();
    try {
      for (Feature feature : featuresService.listFeatures()) {
        if (isPermittedToViewFeature(feature.getName())) {
          features.add(getFeatureView(feature));
        }
      }
    } catch (Exception ex) {
      LOGGER.warn("Could not obtain all features.", ex);
    }
    return features;
  }

  private Map<String, String> getFeatureToRepository() {
    Map<String, String> feature2repo = new HashMap<String, String>();
    try {
      for (Repository repository : featuresService.listRepositories()) {
        for (Feature feature : repository.getFeatures()) {
          feature2repo.put(feature.getId(), repository.getName());
        }
      }
    } catch (Exception ex) {
      LOGGER.warn("Could not map Features to their Repositories.", ex);
    }
    return feature2repo;
  }

  private FeatureDetails getFeatureView(Feature feature) {
    String status = featuresService.isInstalled(feature) ? INSTALLED : UNINSTALLED;
    String repository = getFeatureToRepository().get(feature.getId());
    return new FeatureDetails(feature, status, repository);
  }

  @Override
  public List<FeatureDetails> findApplicationFeatures(String applicationName) {
    return Collections.emptyList();
  }

  public boolean isPermittedToViewFeature(String featureName) {
    KeyValueCollectionPermission serviceToCheck =
        new KeyValueCollectionPermission(
            "view-feature.name",
            new KeyValuePermission("feature.name", Sets.newHashSet(featureName)));
    try {
      return Security.getInstance()
          .runWithSubjectOrElevate(() -> SecurityUtils.getSubject().isPermitted(serviceToCheck));
    } catch (SecurityServiceException | InvocationTargetException e) {
      LOGGER.warn("Failed to elevate subject", e);
      return false;
    }
  }
}
