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
package org.codice.ddf.platform.status.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.karaf.bundle.core.BundleState;
import org.apache.karaf.bundle.core.BundleStateService;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.codice.ddf.platform.status.Application;
import org.codice.ddf.platform.status.ApplicationService;
import org.codice.ddf.platform.status.ApplicationStatus;
import org.codice.ddf.platform.status.ApplicationStatus.ApplicationState;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleRevision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the ApplicationService. Uses the karaf features service and
 * bundle state service to determine current state of items in karaf.
 *
 *
 */
public class ApplicationServiceImpl implements ApplicationService {

    private Logger logger = LoggerFactory.getLogger(ApplicationServiceImpl.class);

    FeaturesService featuresService = null;

    BundleContext context = null;

    List<BundleStateService> bundleStateServices = null;

    Set<String> ignoredApplicationNames = null;

    public ApplicationServiceImpl(FeaturesService featureService, BundleContext context,
            List<BundleStateService> bundleStateServices) {
        this.featuresService = featureService;
        this.context = context;
        this.bundleStateServices = bundleStateServices;
        ignoredApplicationNames = new HashSet<String>();
    }

    @Override
    public Set<Application> getApplications() {
        logger.debug("Getting all applications.");
        Repository[] repos = featuresService.listRepositories();
        logger.debug("Found {} applications from feature service.", repos.length);
        Set<Application> applications = new HashSet<Application>(repos.length);
        for (int i = 0; i < repos.length; i++) {
            if (!ignoredApplicationNames.contains(repos[i].getName())) {
                applications.add(new ApplicationImpl(repos[i]));
            }
        }
        return new TreeSet<Application>(applications);
    }

    @Override
    public boolean isApplicationStarted(Application application) {
        ApplicationStatus status = getApplicationStatus(application);
        return (status.getState().equals(ApplicationState.ACTIVE));
    }

    @Override
    public ApplicationStatus getApplicationStatus(Application application) {
        Set<Feature> errorFeatures = new HashSet<Feature>();
        Set<Feature> requiredFeatures = new HashSet<Feature>();
        Set<Bundle> errorBundles = new HashSet<Bundle>();
        ApplicationState installState = null;

        // check features
        try {
            Set<Feature> features = application.getFeatures();
            for (Feature curFeature : features) {
                if (curFeature.getInstall().equals(Feature.DEFAULT_INSTALL_MODE)) {
                    requiredFeatures.addAll(getAllDependencyFeatures(curFeature));
                }
            }

            logger.debug("{} has {} required features that must be started.",
                    application.getName(), requiredFeatures.size());

            errorFeatures = checkFeatureStatus(requiredFeatures);
            errorBundles = checkBundleStatus(requiredFeatures);

            if (errorFeatures.isEmpty() && errorBundles.isEmpty()) {
                installState = ApplicationState.ACTIVE;
            } else if (errorFeatures.size() >= requiredFeatures.size()) {
                installState = ApplicationState.INACTIVE;
            } else {
                installState = ApplicationState.FAILED;
            }

        } catch (Exception e) {
            logger.warn("Encountered an error while trying to determine status of application ("
                    + application.getName() + "). Setting status as UNKNOWN.", e);
            installState = ApplicationState.UNKNOWN;
        }
        return new ApplicationStatusImpl(application, installState, errorFeatures, errorBundles);
    }

    @Override
    public Set<String> getIgnoredApplicationNames() {
        return Collections.unmodifiableSet(ignoredApplicationNames);
    }

    /**
     * Sets the names of applications that this service should ignore when
     * checking status.
     *
     * @param applicationNames
     *            Comma delimited list of application names, these names must
     *            exactly match the name of the application to ignore.
     */
    public void setIgnoredApplications(String applicationNames) {
        String[] names = applicationNames.split(",");
        ignoredApplicationNames = new HashSet<String>(Arrays.asList(names));
    }

    /**
     * Retrieves all of the dependencies for a given feature.
     *
     * @param feature
     *            Feature to look for dependencies on.
     * @return A set of all features that are dependencies
     */
    private Set<Feature> getAllDependencyFeatures(Feature feature) throws Exception {
        Set<Feature> tmpList = new HashSet<Feature>();
        // get accurate feature reference from service
        Feature curFeature = featuresService.getFeature(feature.getName(), feature.getVersion());
        if (curFeature != null) {
            for (Feature dependencyFeature : curFeature.getDependencies()) {
                tmpList.addAll(getAllDependencyFeatures(dependencyFeature));
            }
            tmpList.add(curFeature);
        } else {
            // feature may not be installed
            tmpList.add(feature);
        }
        return tmpList;
    }

    /**
     * Goes through a set of features and returns a set of bundles from those
     * features that are not properly started.
     *
     * @param features
     * @return a set of bundles that are not started
     */
    private Set<Bundle> checkBundleStatus(Set<Feature> features) {
        Set<Bundle> badBundles = new HashSet<Bundle>();
        for (Feature curFeature : features) {
            for (BundleInfo curBundleInfo : curFeature.getBundles()) {
                Bundle curBundle = context.getBundle(curBundleInfo.getLocation());
                if (curBundle != null
                        && (curBundle.adapt(BundleRevision.class).getTypes() != BundleRevision.TYPE_FRAGMENT)) {
                    // check if bundle state is NOT active
                    if (curBundle.getState() != Bundle.ACTIVE) {
                        badBundles.add(curBundle);
                        continue;
                    }

                    // check if any service frameworks failed on start
                    for (BundleStateService curStateService : bundleStateServices) {
                        logger.debug("Checking {} for bundle state of {}.",
                                curStateService.getName(), curBundle.getSymbolicName());
                        BundleState curState = curStateService.getState(curBundle);
                        if (!curState.equals(BundleState.Active)
                                && !curState.equals(BundleState.Unknown)) {
                            logger.debug("{} is not in an active/unknown state. Current State: {}",
                                    curBundle.getSymbolicName(), curState.toString());
                            badBundles.add(curBundle);
                        }
                    }

                }
            }
        }
        return badBundles;
    }

    /**
     * Checks a list of features to see which ones are not started.
     *
     * @param features
     * @return a sub-set of the passed in set and it contains the features that
     *         are not started
     */
    private Set<Feature> checkFeatureStatus(Set<Feature> features) {
        Set<Feature> badFeatures = new HashSet<Feature>();
        for (Feature curFeature : features) {
            if (!featuresService.isInstalled(curFeature)) {
                logger.debug("{} is not started.", curFeature.getName());
                badFeatures.add(curFeature);
            }
        }
        return badFeatures;
    }

}
