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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.apache.shiro.SecurityUtils;
import org.codice.ddf.admin.application.rest.model.FeatureDetails;
import org.codice.ddf.admin.application.service.Application;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.codice.ddf.admin.application.service.migratable.JsonUtils;
import org.codice.ddf.security.Security;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
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

  private static final Map<Long, String> BUNDLE_LOCATIONS = new ConcurrentHashMap<>();

  private final FeaturesService featuresService;

  private static final Path APPLICATION_DEFINITIONS_FOLDER =
      Paths.get("etc", "application-definitions");

  private final Security security;

  public ApplicationServiceImpl(FeaturesService featuresService, Security security) {
    this.featuresService = featuresService;
    this.security = security;
  }

  @Override
  public Set<Application> getApplications() {
    File[] appDefinitions = APPLICATION_DEFINITIONS_FOLDER.toFile().listFiles();
    if (appDefinitions == null) {
      LOGGER.warn("No application-definitions configuration files found.");
      return Collections.emptySet();
    }

    Map<String, Bundle> bundlesByLocation = getBundlesByLocation();
    Set<Application> apps = new HashSet<>();
    for (File appDef : appDefinitions) {
      try {
        ApplicationImpl app =
            JsonUtils.fromJson(IOUtils.toString(appDef.toURI()), ApplicationImpl.class);
        if (isPermittedToViewFeature(app.getName())) {
          app.loadBundles(bundlesByLocation);
          apps.add(app);
        }

      } catch (IOException e) {
        LOGGER.warn("Unable to parse application from file {} ", appDef.getPath());
      }
    }

    return apps;
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

  protected BundleContext getContext() {
    Bundle cxfBundle = FrameworkUtil.getBundle(ApplicationServiceImpl.class);
    if (cxfBundle != null && cxfBundle.getBundleContext() != null) {
      return cxfBundle.getBundleContext();
    }
    throw new IllegalStateException("Could not get context; cxfBundle is null");
  }

  @Override
  public List<Feature> getInstallationProfiles() {
    LOGGER.debug("Looking for installation profile features");
    List<Feature> profiles = new ArrayList<>();
    try {
      profiles =
          Arrays.stream(featuresService.listFeatures())
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
  public List<FeatureDetails> getAllFeatures() {
    List<FeatureDetails> features = new ArrayList<>();
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
    Map<String, String> feature2repo = new HashMap<>();
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

  public boolean isPermittedToViewFeature(String featureName) {
    KeyValueCollectionPermission serviceToCheck =
        new KeyValueCollectionPermission(
            "view-feature.name",
            new KeyValuePermission("feature.name", Sets.newHashSet(featureName)));
    try {
      return security.runWithSubjectOrElevate(
          () -> SecurityUtils.getSubject().isPermitted(serviceToCheck));
    } catch (SecurityServiceException | InvocationTargetException e) {
      LOGGER.warn("Failed to elevate subject", e);
      return false;
    }
  }

  private Map<String, Bundle> getBundlesByLocation() {
    return Arrays.stream(
            FrameworkUtil.getBundle(ApplicationServiceImpl.class).getBundleContext().getBundles())
        .collect(Collectors.toMap(ApplicationServiceImpl::computeLocation, Function.identity()));
  }

  private static String computeLocation(Bundle bundle) {
    return BUNDLE_LOCATIONS.computeIfAbsent(bundle.getBundleId(), id -> bundle.getLocation());
  }
}
