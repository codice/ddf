/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.admin.application.service.command;

import static org.boon.Boon.fromJson;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
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
import org.codice.ddf.security.common.Security;
import org.osgi.framework.BundleException;
import org.osgi.service.resolver.ResolutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.Subject;
import ddf.security.SubjectUtils;

@Command(scope = "profile", name = "install", description = "Installs the profile")
@Service
public class ProfileInstallCommand extends AbstractProfileCommand {
    @Argument(index = 0, name = "profileName", description = "Name of install profile to use", required = true, multiValued = false)
    String profileName;

    private Security security = Security.getInstance();

    private static final String ADVANCED_PROFILE_INSTALL_FEATURES = "install-features";
    private static final String ADVANCED_PROFILE_UNINSTALL_FEATURES = "uninstall-features";
    private static final String ADVANCED_PROFILE_START_APPS = "start-apps";
    private static final String ADVANCED_PROFILE_STOP_BUNDLES = "stop-bundles";
    private static final EnumSet NO_AUTO_REFRESH = EnumSet.of(FeaturesService.Option.NoAutoRefreshBundles);
    private static final String PROFILE_PREFIX = "profile-";
    private static final String RESTART_WARNING = "An unexpected error occurred during the installation process. The system is in unknown state. It is strongly recommended to restart the installation from the beginning.";
    private static final String SECURITY_ERROR = "Could not get system user to install profile ";
    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileInstallCommand.class);

    // Added as a convenience for unit testing
    void setSecurity(Security security) {
        this.security = security;
    }

    @Override
    protected void doExecute(ApplicationService applicationService,
                             FeaturesService featuresService, BundleService bundleService) throws Exception {

        profileName = profileName.trim();

        if (profileName.startsWith(".") || profileName.startsWith("/") || profileName.matches("((?i)(?s)[A-Z]):.*")) {
            throw new IllegalArgumentException("Profile Name must not start with '.', '/', or a windows drive letter");
        }

        Optional<Feature> optionalProfile = getProfile(applicationService, profileName);
        Optional<Map<String, List<String>>> optionalExtraProfiles = getProfile(profileName);

        try {
            if (optionalProfile.isPresent()) {
                List<String> profileApps = optionalProfile.get().getDependencies().stream()
                        .map(Dependency::getName)
                        .collect(Collectors.toList());
                startApps(applicationService, profileApps);
                uninstallInstallerModule(featuresService);
                printSuccess("Installation Complete");
            } else {
                if (optionalExtraProfiles.isPresent()) {
                    Map<String, List<String>> profile = optionalExtraProfiles.get();
                    startApps(applicationService, profile.get(ADVANCED_PROFILE_START_APPS));
                    installFeatures(featuresService, profile.get(ADVANCED_PROFILE_INSTALL_FEATURES));
                    uninstallFeatures(featuresService, profile.get(ADVANCED_PROFILE_UNINSTALL_FEATURES));
                    /* The stop-bundles operation is a workaround currently in place for dealing with
                        the inter-dependencies of some features, this will likely be removed in the future. */
                    stopBundles(bundleService, profile.get(ADVANCED_PROFILE_STOP_BUNDLES));
                    uninstallInstallerModule(featuresService);
                    printSuccess("Installation Complete");
                } else {
                    printError(String.format("Profile: %s not found", profileName));
                }
            }
        } catch (ApplicationServiceException | ResolutionException | BundleException | IllegalArgumentException e) {
            printError(RESTART_WARNING);
            throw e;
        }
    }

    private void startApps(ApplicationService applicationService, List<String> startupApps)
            throws ApplicationServiceException {
        if (startupApps != null) {
            printSectionHeading("Starting Applications");
            startupApps.stream().distinct().forEach(application -> {
                printItemStatusPending("Starting: ", application);
                executeAsSystem(() -> {
                    try {
                        applicationService.startApplication(application);
                    } catch (ApplicationServiceException e) {
                        printItemStatusFailure("Start Failed: ", application);
                        throw e;
                    }
                    return true;
                });
                printItemStatusSuccess("Started: ", application);
            });
        }
    }

    private void uninstallInstallerModule(FeaturesService featuresService) throws Exception {
        try {
            if (!featuresService.isInstalled(featuresService.getFeature("admin-post-install-modules"))) {
                printSectionHeading("Finalizing Installation");
                featuresService.installFeature("admin-post-install-modules",
                        NO_AUTO_REFRESH);
            }
            if (featuresService.isInstalled(featuresService.getFeature("admin-modules-installer"))) {
                featuresService.uninstallFeature("admin-modules-installer",
                        NO_AUTO_REFRESH);
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
            installFeatures.stream().distinct().forEach(feature -> {
                printItemStatusPending("Installing: ", feature);
                executeAsSystem(() -> {
                    try {
                        featuresService.installFeature(feature,
                                NO_AUTO_REFRESH);
                    } catch (Exception e) {
                        printItemStatusFailure("Install Failed: ", feature);
                        throw e;
                    }
                    return true;
                });
                printItemStatusSuccess("Installed: ", feature);
            });
        }
    }

    private void uninstallFeatures(FeaturesService featuresService, List<String> uninstallFeatures) throws Exception {
        if (uninstallFeatures != null) {
            printSectionHeading("Uninstalling Features");
            uninstallFeatures.stream().distinct().forEach(feature -> {
                printItemStatusPending("Uninstalling: ", feature);
                executeAsSystem(() -> {
                    Feature featureObject = featuresService.getFeature(feature);
                    if (featureObject == null) {
                        printItemStatusFailure("Uninstall Failed: ", feature);
                        printError(String.format("Feature: %s does not exist", feature));
                        throw new IllegalArgumentException(String.format("Feature: %s does not exist", feature));
                    }
                    try {
                        featuresService.uninstallFeature(featureObject.getName(),
                                featureObject.getVersion(),
                                NO_AUTO_REFRESH);
                    } catch (Exception e) {
                        printItemStatusFailure("Uninstall Failed: ", feature);
                        throw e;
                    }
                    return true;
                });
                printItemStatusSuccess("Uninstalled: ", feature);
            });
        }
    }

    private void stopBundles(BundleService bundleService, List<String> stopBundleNames)
            throws BundleException, IllegalArgumentException{
        if (stopBundleNames != null) {
            printSectionHeading("Stopping Bundles");
            stopBundleNames.stream().distinct().forEach(bundle -> {
                printItemStatusPending("Stopping: ", bundle);
                executeAsSystem(() -> {
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
                    return true;
                });
                printItemStatusSuccess("Stopped: ", bundle);
            });
        }
    }

    /**
     * Get the {@link Feature} that makes up a profile
     * @param applicationService {@link ApplicationService}
     * @param profileName Name of profile to get
     * @return {@link Optional} if the profile doesn't exist the {@link Optional} will be empty
     */
    private Optional<Feature> getProfile(ApplicationService applicationService, String profileName) {
        return applicationService.getInstallationProfiles().stream()
                .filter(i -> i.getName().replaceFirst(PROFILE_PREFIX, "").equals(profileName)).findFirst();
    }

    /**
     * Gets the content of an advanced profile
     * @param profileName Name of profile to get
     * @return {@link Optional}, if the profile doesn't exist the {@link Optional} will be empty
     */
    private Optional<Map<String, List<String>>> getProfile(String profileName) {
        File profileFile = Paths.get(profilePath.toString(), profileName + PROFILE_EXTENSION).toFile();
        Map<String, List<String>> profileMap = null;

        try {
            profileMap = (Map<String, List<String>>) fromJson(FileUtils.readFileToString(profileFile, StandardCharsets.UTF_8));
        } catch (FileNotFoundException e) {
            LOGGER.debug("The file associated with profile: {} was not found under {}",
                    profileName, profilePath);
        } catch (IOException e) {
            LOGGER.debug("An IOException occurred: ", e);
        }

        return Optional.ofNullable(profileMap);
    }

    private <T> T executeAsSystem(Callable<T> func) {
        Subject systemSubject = security.getSystemSubject();
        LOGGER.debug("System Subject retrieved: " + SubjectUtils.getName(systemSubject));
        if (systemSubject == null) {
            printError(SECURITY_ERROR);
            throw new IllegalStateException(SECURITY_ERROR);
        }
        return systemSubject.execute(func);
    }
}
