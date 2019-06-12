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
package org.codice.ddf.admin.application.service.command;

import com.google.gson.reflect.TypeToken;
import ddf.security.common.audit.SecurityLogger;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.karaf.bundle.core.BundleService;
import org.apache.karaf.features.Dependency;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.codice.ddf.admin.application.service.ApplicationServiceException;
import org.codice.ddf.admin.application.service.migratable.JsonUtils;
import org.osgi.framework.BundleException;
import org.osgi.service.resolver.ResolutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "profile", name = "install", description = "Installs the profile")
@Service
public class ProfileInstallCommand extends AbstractProfileCommand {
  @Argument(
    index = 0,
    name = "profileName",
    description = "Name of install profile to use",
    required = true,
    multiValued = false
  )
  String profileName;

  private static final String ADVANCED_PROFILE_INSTALL_FEATURES = "install-features";
  private static final String ADVANCED_PROFILE_UNINSTALL_FEATURES = "uninstall-features";
  private static final String ADVANCED_PROFILE_STOP_BUNDLES = "stop-bundles";
  private static final String FEATURE_FAILURE_MESSAGE = "Feature: %s does not exist";
  private static final EnumSet NO_AUTO_REFRESH =
      EnumSet.of(FeaturesService.Option.NoAutoRefreshBundles);
  private static final String PROFILE_PREFIX = "profile-";
  private static final String RESTART_WARNING =
      "An unexpected error occurred during the installation process. The system is in unknown state. It is strongly recommended to restart the installation from the beginning.";
  private static final Logger LOGGER = LoggerFactory.getLogger(ProfileInstallCommand.class);

  @Override
  protected final void doExecute(
      ApplicationService applicationService,
      FeaturesService featuresService,
      BundleService bundleService)
      throws Exception {

    profileName = profileName.trim();

    if (profileName.startsWith(".")
        || profileName.startsWith("/")
        || profileName.matches("((?i)(?s)[A-Z]):.*")) {
      throw new IllegalArgumentException(
          "Profile Name must not start with '.', '/', or a windows drive letter");
    }

    try {
      AccessController.doPrivileged(
          (PrivilegedExceptionAction<Void>)
              () -> {
                installProfile(applicationService, featuresService, bundleService, profileName);
                return null;
              });
    } catch (PrivilegedActionException e) {
      throw e.getException();
    }
  }

  private void installProfile(
      ApplicationService applicationService,
      FeaturesService featuresService,
      BundleService bundleService,
      String profileName)
      throws Exception {
    Optional<Feature> optionalProfile = getProfile(applicationService, profileName);
    Optional<Map<String, List<String>>> optionalExtraProfiles = getProfile(profileName);

    try {
      if (optionalProfile.isPresent()) {
        List<String> profileApps =
            optionalProfile
                .get()
                .getDependencies()
                .stream()
                .map(Dependency::getName)
                .collect(Collectors.toList());
        installFeatures(featuresService, profileApps);
        uninstallInstallerModule(featuresService);
        printSuccess("Installation Complete");
      } else {
        if (optionalExtraProfiles.isPresent()) {
          Map<String, List<String>> profile = optionalExtraProfiles.get();
          installFeatures(featuresService, profile.get(ADVANCED_PROFILE_INSTALL_FEATURES));
          uninstallFeatures(featuresService, profile.get(ADVANCED_PROFILE_UNINSTALL_FEATURES));
          /* The stop-bundles operation is a workaround currently in place for dealing with
          the inter-dependencies of some features, this will likely be removed in the future. */
          stopBundles(bundleService, profile.get(ADVANCED_PROFILE_STOP_BUNDLES));
          uninstallInstallerModule(featuresService);
          printSuccess("Installation Complete");
          SecurityLogger.audit("Installed profile: {}", profile);
        } else {
          printError(String.format("Profile: %s not found", profileName));
        }
      }
    } catch (ApplicationServiceException
        | ResolutionException
        | BundleException
        | IllegalArgumentException e) {
      SecurityLogger.audit("Failed to install profile: {}", profileName);
      printError(RESTART_WARNING);
      throw e;
    }
  }

  private void uninstallInstallerModule(FeaturesService featuresService) throws Exception {
    try {
      if (!featuresService.isInstalled(featuresService.getFeature("admin-post-install-modules"))) {
        printSectionHeading("Finalizing Installation");
        installFeature(featuresService, "admin-post-install-modules");
      }
      if (featuresService.isInstalled(featuresService.getFeature("admin-modules-installer"))) {
        uninstallFeature(featuresService, featuresService.getFeature("admin-modules-installer"));
      }
    } catch (Exception e) {
      printError("An error occurred while trying to perform post-install operations");
      throw e;
    }
  }

  private void installFeatures(FeaturesService featuresService, List<String> installFeatures)
      throws Exception {
    if (installFeatures != null) {
      printSectionHeading("Installing Features");
      Set<String> uniqueValues = new HashSet<>();
      for (String feature : installFeatures) {
        if (uniqueValues.add(feature)) {
          printItemStatusPending("Installing: ", feature);
          installFeature(featuresService, feature);
          printItemStatusSuccess("Installed: ", feature);
        }
      }
    }
  }

  private void uninstallFeatures(FeaturesService featuresService, List<String> uninstallFeatures)
      throws Exception {
    if (uninstallFeatures != null) {
      printSectionHeading("Uninstalling Features");
      Set<String> uniqueValues = new HashSet<>();
      for (String feature : uninstallFeatures) {
        if (uniqueValues.add(feature)) {
          printItemStatusPending("Uninstalling: ", feature);
          Feature featureObject = null;
          try {
            featureObject = featuresService.getFeature(feature);
          } catch (Exception e) {
            printError(String.format(FEATURE_FAILURE_MESSAGE, feature));
            throw e;
          }
          if (featureObject == null) {
            printItemStatusFailure("Uninstall Failed: ", feature);
            printError(String.format(FEATURE_FAILURE_MESSAGE, feature));
            throw new IllegalArgumentException(String.format(FEATURE_FAILURE_MESSAGE, feature));
          }
          uninstallFeature(featuresService, featureObject);
          printItemStatusSuccess("Uninstalled: ", feature);
        }
      }
    }
  }

  private void stopBundles(BundleService bundleService, List<String> stopBundleNames)
      throws BundleException, IllegalArgumentException {
    if (stopBundleNames != null) {
      printSectionHeading("Stopping Bundles");
      Set<String> uniqueValues = new HashSet<>();
      for (String bundle : stopBundleNames) {
        if (uniqueValues.add(bundle)) {
          printItemStatusPending("Stopping: ", bundle);
          stopBundle(bundleService, bundle);
          printItemStatusSuccess("Stopped: ", bundle);
        }
      }
    }
  }

  /**
   * Get the {@link Feature} that makes up a profile
   *
   * @param applicationService {@link ApplicationService}
   * @param profileName Name of profile to get
   * @return {@link Optional} if the profile doesn't exist the {@link Optional} will be empty
   */
  private Optional<Feature> getProfile(ApplicationService applicationService, String profileName) {
    return applicationService
        .getInstallationProfiles()
        .stream()
        .filter(i -> i.getName().replaceFirst(PROFILE_PREFIX, "").equals(profileName))
        .findFirst();
  }

  /**
   * Gets the content of an advanced profile
   *
   * @param profileName Name of profile to get
   * @return {@link Optional}, if the profile doesn't exist the {@link Optional} will be empty
   */
  private Optional<Map<String, List<String>>> getProfile(String profileName) {
    File profileFile = Paths.get(profilePath.toString(), profileName + PROFILE_EXTENSION).toFile();
    Map<String, List<String>> profileMap = null;

    try {
      profileMap =
          (Map<String, List<String>>)
              JsonUtils.fromJson(
                  FileUtils.readFileToString(profileFile, StandardCharsets.UTF_8),
                  new TypeToken<Map<String, List<String>>>() {}.getType());
      SecurityLogger.audit("Read profile {} from {}", profileName, profileFile.getAbsolutePath());
    } catch (FileNotFoundException e) {
      LOGGER.debug(
          "The file associated with profile: {} was not found under {}", profileName, profilePath);
    } catch (IOException e) {
      LOGGER.debug("An IOException occurred: ", e);
    }

    return Optional.ofNullable(profileMap);
  }

  private void installFeature(FeaturesService featuresService, String feature) throws Exception {
    try {
      featuresService.installFeature(feature, NO_AUTO_REFRESH);
    } catch (Exception e) {
      printItemStatusFailure("Install Failed: ", feature);
      throw e;
    }
  }

  private void uninstallFeature(FeaturesService featuresService, Feature feature) throws Exception {
    try {
      featuresService.uninstallFeature(feature.getName(), feature.getVersion(), NO_AUTO_REFRESH);
    } catch (Exception e) {
      printItemStatusFailure("Uninstall Failed: ", feature.getName());
      throw e;
    }
  }

  private void stopBundle(BundleService bundleService, String bundle) throws BundleException {
    try {
      bundleService.getBundle(bundle).stop();
    } catch (BundleException e) {
      printItemStatusFailure("Stop Failed: ", bundle);
      throw e;
    } catch (IllegalArgumentException ie) {
      printItemStatusFailure("Stop Failed: ", bundle);
      printError(String.format("Bundle: %s does not exist", bundle));
      throw ie;
    }
  }
}
