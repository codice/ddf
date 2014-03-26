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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.apache.karaf.bundle.core.BundleState;
import org.apache.karaf.bundle.core.BundleStateService;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.codice.ddf.admin.application.service.Application;
import org.codice.ddf.admin.application.service.ApplicationNode;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.codice.ddf.admin.application.service.ApplicationStatus;
import org.codice.ddf.admin.application.service.ApplicationStatus.ApplicationState;
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
        logger.trace("Getting all applications.");
        Repository[] repos = featuresService.listRepositories();
        logger.debug("Found {} applications from feature service.", repos.length);
        Set<Application> applications = new HashSet<Application>(repos.length);
        for (int i = 0; i < repos.length; i++) {
            Application newApp = new ApplicationImpl(repos[i]);
            if (!ignoredApplicationNames.contains(newApp.getName())) {
                applications.add(newApp);
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
        logger.debug("Not ignoring applications with the following names: {}",
                ignoredApplicationNames);
    }

    public Set<ApplicationNode> getApplicationTree() {
        Set<ApplicationNode> applicationTree = new TreeSet<ApplicationNode>();
        Set<Application> applicationSet = getApplications();
        Map<Application, ApplicationNodeImpl> appMap = new HashMap<Application, ApplicationNodeImpl>(
                applicationSet.size());
        // add all values into a map
        for (Application curApp : applicationSet) {
            appMap.put(curApp, new ApplicationNodeImpl(curApp));
        }

        // find dependencies in each app and add them into correct node
        for (Entry<Application, ApplicationNodeImpl> curAppNode : appMap.entrySet()) {
            try {
                // main feature will contain dependencies
                Feature mainFeature = curAppNode.getKey().getMainFeature();
                // eliminate duplications with a set
                Set<Feature> dependencies = new HashSet<Feature>(mainFeature.getDependencies());
                // remove any features that are local to the application
                dependencies.removeAll(curAppNode.getKey().getFeatures());
                // loop through all of the features that are left to determine
                // where they are from
                Set<Application> depAppSet = new HashSet<Application>();
                for (Feature curDepFeature : dependencies) {
                    Application dependencyApp = findFeature(
                            featuresService.getFeature(curDepFeature.getName()), applicationSet);
                    if (dependencyApp != null) {
                        if (dependencyApp.equals(curAppNode.getKey())) {
                            logger.debug("Self-dependency");
                            continue;
                        } else {
                            logger.debug(
                                    "Application {} depends on the feature {} which is located in application {}.",
                                    curAppNode.getKey().getName(), curDepFeature.getName(),
                                    dependencyApp.getName());
                            depAppSet.add(dependencyApp);
                        }
                    }
                }
                // there should only be one main dependency application declared
                if (depAppSet.size() == 1) {
                    Application parentApp = depAppSet.iterator().next();
                    // update the dependency app with a new child
                    ApplicationNode parentAppNode = appMap.get(parentApp);
                    parentAppNode.getChildren().add(curAppNode.getValue());
                    curAppNode.getValue().setParent(parentAppNode);
                } else if (depAppSet.size() == 0) {
                    logger.debug(
                            "No dependency applications found for {}. This will be sent back as a root application.",
                            curAppNode.getKey().getName());
                } else {
                    logger.warn(
                            "Found more than 1 application dependency. Could not determine which one is the correct parent. Application {} may incorrectly show up as root application in hierarchy.",
                            curAppNode.getKey().getName());
                }

                // ApplicationServiceException from DDF and Exception from Karaf
                // (FeaturesService)
            } catch (Exception e) {
                logger.warn("Encountered error while determine dependencies for "
                        + curAppNode.getKey().getName()
                        + ". This may cause an incomplete application hierarchy to be created.", e);
            }
        }

        // determine the root applications (contain no parent) and return those
        for (Entry<Application, ApplicationNodeImpl> curAppNode : appMap.entrySet()) {
            if (curAppNode.getValue().getParent() == null) {
                logger.debug("Adding {} as a root application.", curAppNode.getKey().getName());
                applicationTree.add(curAppNode.getValue());
            }
        }

        return applicationTree;
    }

    @Override
    public Application findFeature(Feature feature) {
        return findFeature(feature, getApplications());
    }

    /**
     * Locates a given feature within the specified set of applications.
     * 
     * @param feature
     *            Feature to look for.
     * @param applications
     *            Set of applications to check for the feature.
     * @return The first application that contains the feature or null if no
     *         application contains the feature.
     */
    protected Application findFeature(Feature feature, Set<Application> applications) {
        logger.debug("Looking for feature {} - {}", feature.getName(), feature.getVersion());
        for (Application curApp : applications) {
            try {
                if (curApp.getFeatures().contains(feature)) {
                    return curApp;
                }
            } catch (Exception e) {
                logger.warn(
                        "Encountered and error when trying to check features in application named "
                                + curApp + ". Skipping and checking other applications.", e);
            }
        }
        logger.warn("Could not find feature {} in any known application, returning null.",
                feature.getName());
        return null;
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
