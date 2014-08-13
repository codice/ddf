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
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
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
import org.codice.ddf.admin.application.rest.model.FeatureDto;
import org.codice.ddf.admin.application.service.Application;
import org.codice.ddf.admin.application.service.ApplicationNode;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.codice.ddf.admin.application.service.ApplicationServiceException;
import org.codice.ddf.admin.application.service.ApplicationStatus;
import org.codice.ddf.admin.application.service.ApplicationStatus.ApplicationState;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the ApplicationService. Uses the karaf features service and
 * bundle state service to determine current state of items in karaf.
 */
public class ApplicationServiceImpl implements ApplicationService {

    private Logger logger = LoggerFactory.getLogger(ApplicationServiceImpl.class);

    private FeaturesService featuresService = null;

    private BundleContext context = null;

    private List<BundleStateService> bundleStateServices = null;

    private Set<String> ignoredApplicationNames = null;

    private static final String POST_CONFIG_START = "admin-post-install-modules";

    private static final String POST_CONFIG_STOP = "admin-modules-installer";

    private static final String INSTALLATION_PROFILE_PREFIX = "profile-";
    
    private static final String INSTALLED = "Installed";
    
    private static final String UNINSTALLED = "Uninstalled";

    /**
     * Used to make sure that the config file is only checked on first run.
     */
    private static final String FIRST_RUN_MARKER = "first-run";

    /**
     * Creates a new instance of Application Service.
     * 
     * @param featureService
     *            The internal features service exposed by Karaf.
     * @param context
     *            BundleContext for this bundle.
     * @param bundleStateServices
     *            List of BundleStateServices that allow fine-grained
     *            information about bundle status for deployment services (like
     *            blueprint and spring).
     */
    public ApplicationServiceImpl(BundleContext context,
            List<BundleStateService> bundleStateServices) {
        ServiceReference<FeaturesService> featuresServiceRef = context
                .getServiceReference(FeaturesService.class);
        this.featuresService = context.getService(featuresServiceRef);
        this.context = context;
        this.bundleStateServices = bundleStateServices;
        ignoredApplicationNames = new HashSet<String>();

    }

    @Override
    public Set<Application> getApplications() {
        logger.trace("Getting all applications.");
        Repository[] repos = featuresService.listRepositories();
        logger.debug("Found {} applications from feature service.", repos.length);

        if (logger.isDebugEnabled()) {
            for (int ii = 0; ii < repos.length; ++ii) {
                logger.debug("Repo/App {}: {}", ii, repos[ii].getName());
            }
        }

        Set<Application> applications = new HashSet<Application>(repos.length);
        for (int i = 0; i < repos.length; i++) {
            Application newApp = new ApplicationImpl(repos[i]);
            try {
                if (!ignoredApplicationNames.contains(newApp.getName())
                        && newApp.getFeatures().size() > 0) {
                    applications.add(newApp);
                }
            } catch (ApplicationServiceException ase) {
                logger.warn("Exception while trying to find information for application named "
                        + newApp.getName() + ". It will be excluded from the application list.",
                        ase);
            }
        }
        return new TreeSet<Application>(applications);
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
        ApplicationStatus status = getApplicationStatus(application);
        return (status.getState().equals(ApplicationState.ACTIVE));
    }

    @Override
    public ApplicationStatus getApplicationStatus(Application application) {
        Set<Feature> uninstalledFeatures = new HashSet<Feature>();
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

            uninstalledFeatures = getNotInstalledFeatures(requiredFeatures);
            BundleStateSet bundleStates = getCurrentBundleStates(requiredFeatures);
            errorBundles.addAll(bundleStates.getFailedBundles());
            errorBundles.addAll(bundleStates.getInactiveBundles());

            if (bundleStates.getNumFailedBundles() > 0) {
                // Any failed bundles, regardless of feature state, indicate a
                // failed application state
                installState = ApplicationState.FAILED;
            } else if (!uninstalledFeatures.isEmpty() || bundleStates.getNumInactiveBundles() > 0) {
                installState = ApplicationState.INACTIVE;
            } else {
                installState = ApplicationState.ACTIVE;
            }
        } catch (Exception e) {
            logger.warn("Encountered an error while trying to determine status of application ("
                    + application.getName() + "). Setting status as UNKNOWN.", e);
            installState = ApplicationState.UNKNOWN;
        }

        return new ApplicationStatusImpl(application, installState, uninstalledFeatures,
                errorBundles);
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

    /**
     * Sets the configuration file used for initial installation and starts the
     * installation.
     * 
     * @param configFileName
     *            Absolute path name of the file containing the application
     *            list.
     */
    public void setConfigFileName(String configFileName) {
        try {
            ServiceReference<ConfigurationAdmin> configAdminRef = context
                    .getServiceReference(ConfigurationAdmin.class);
            ConfigurationAdmin configAdmin = context.getService(configAdminRef);
            Configuration config = configAdmin.getConfiguration(ApplicationServiceImpl.class
                    .getName());
            Dictionary<String, Object> properties = config.getProperties();

            if (properties.get(FIRST_RUN_MARKER) == null) {
                logger.debug("Checking the configuration file on the first run.");
                ApplicationConfigInstaller configInstaller = new ApplicationConfigInstaller(
                        configFileName, this, featuresService, POST_CONFIG_START, POST_CONFIG_STOP);
                configInstaller.start();
                properties.put(FIRST_RUN_MARKER, Boolean.TRUE);
                config.update(properties);
            } else {
                logger.debug("Not the first run, ignoring the installer configuration file.");
            }

        } catch (Exception e) {
            logger.warn("Could not check for installer application configuration file.", e);
        }
    }

    @Override
    public Set<ApplicationNode> getApplicationTree() {
        Set<ApplicationNode> applicationTree = new TreeSet<ApplicationNode>();
        Set<Application> applicationSet = getApplications();
        Map<Application, ApplicationNodeImpl> appMap = new HashMap<Application, ApplicationNodeImpl>(
                applicationSet.size());
        // add all values into a map
        for (Application curApp : applicationSet) {
            appMap.put(curApp, new ApplicationNodeImpl(curApp, getApplicationStatus(curApp)));
        }

        // find dependencies in each app and add them into correct node
        for (Entry<Application, ApplicationNodeImpl> curAppNode : appMap.entrySet()) {
            try {
                // main feature will contain dependencies
                Feature mainFeature = curAppNode.getKey().getMainFeature();

                if (null == mainFeature) {
                    logger.debug("Application \"{}\" does not contain a main feature", curAppNode
                            .getKey().getName());
                    continue;
                }

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
                if (!depAppSet.isEmpty()) {
                    Application parentApp;
                    if (depAppSet.size() > 1) {
                        parentApp = findCommonParent(depAppSet, appMap);
                        if (parentApp == null) {
                            logger.warn(
                                    "Found more than 1 application dependency for application {}. Could not determine which one is the correct parent. Application will be sent back as root application.",
                                    curAppNode.getKey().getName());
                            continue;
                        }
                    } else {
                        parentApp = depAppSet.iterator().next();
                    }
                    // update the dependency app with a new child
                    ApplicationNode parentAppNode = appMap.get(parentApp);
                    parentAppNode.getChildren().add(curAppNode.getValue());
                    curAppNode.getValue().setParent(parentAppNode);
                } else {
                    logger.debug(
                            "No dependency applications found for {}. This will be sent back as a root application.",
                            curAppNode.getKey().getName());
                }

                // ApplicationServiceException from DDF and Exception from Karaf
                // (FeaturesService)
            } catch (Exception e) {
                logger.warn(
                        "Encountered error while determining dependencies for \"{}\". This may cause an incomplete application hierarchy to be created.",
                        curAppNode.getKey().getName(), e);
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

    /**
     * Finds a common parent that contains all other applications as parent
     * dependencies.
     * 
     * @param applicationSet
     *            Set of applications that should be found in a single parent.
     * @param appMap
     *            Application Map containing all the application nodes.
     * @return A single application that is the parent which contains all of the
     *         required applications as dependencies or null if no parent is
     *         found.
     */
    private Application findCommonParent(Set<Application> applicationSet,
            Map<Application, ApplicationNodeImpl> appMap) {

        // build dependency trees for each application in the set
        Map<Application, Set<Application>> applicationTreeSet = new HashMap<Application, Set<Application>>(
                applicationSet.size());
        for (Application curDependency : applicationSet) {
            Set<Application> curDepSet = new HashSet<Application>();
            curDepSet.add(curDependency);
            for (ApplicationNode curParent = appMap.get(curDependency).getParent(); curParent != null; curParent = curParent
                    .getParent()) {
                curDepSet.add(curParent.getApplication());
            }
            applicationTreeSet.put(curDependency, curDepSet);
        }

        // check through each set to see if any application contains everything
        // within its parents
        for (Entry<Application, Set<Application>> curAppEntry : applicationTreeSet.entrySet()) {
            if (!(new HashSet<Application>(applicationSet).retainAll(curAppEntry.getValue()))) {
                logger.debug("{} contains all needed dependencies.", curAppEntry.getKey().getName());
                return curAppEntry.getKey();
            } else {
                logger.trace("{} does not contain all needed dependencies.", curAppEntry.getKey()
                        .getName());
            }
        }

        return null;
    }

    @Override
    public Application findFeature(Feature feature) {
        return findFeature(feature, getApplications());
    }

    @Override
    public List<Feature> getInstallationProfiles() {
    	logger.debug("Looking for installation profile features");
        List<Feature> profiles = new ArrayList<Feature>();
        try{
            for (Feature feature : featuresService.listFeatures()) {
                if (feature.getName().contains(INSTALLATION_PROFILE_PREFIX)) {
                    profiles.add(feature);
                }
            }
        } catch(Exception e){
            logger.error(
                    "Encountered an error while trying to obtain the installation profile features.",
                    e);
        }

        return profiles;
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
        // get accurate feature reference from service - workaround for
        // KARAF-2896 'RepositoryImpl load method incorrectly populates
        // "features" list'
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
     * Evaluates the bundles contained in a set of {@link Feature}s and
     * determines if each bundle is currently in an active, inactive, or failed
     * state.
     * 
     * @param features
     * @return {@link BundleStateSet} containing information on the state of
     *         each bundle
     */
    private final BundleStateSet getCurrentBundleStates(Set<Feature> features) {
        BundleStateSet bundleStateSet = new BundleStateSet();

        for (Feature curFeature : features) {
            for (BundleInfo curBundleInfo : curFeature.getBundles()) {
                Bundle curBundle = context.getBundle(curBundleInfo.getLocation());
                if (curBundle != null && 
                    curBundle.adapt(BundleRevision.class).getTypes() != BundleRevision.TYPE_FRAGMENT) {

                    // check if bundle is inactive
                    int bundleState = curBundle.getState();
                    switch (bundleState) {
                    case Bundle.RESOLVED:
                    case Bundle.STARTING:
                    case Bundle.STOPPING:
                        bundleStateSet.addInactiveBundle(curBundle);
                        break;
                    case Bundle.INSTALLED:
                    case Bundle.UNINSTALLED:
                        bundleStateSet.addFailedBundle(curBundle);
                        break;
                    case Bundle.ACTIVE:
                        // check if any service frameworks (e.g. Blueprint
                        // and SpringDM) failed on start
                        for (BundleStateService curStateService : bundleStateServices) {
                            logger.trace("Checking {} for bundle state of {}.",
                                    curStateService.getName(), curBundle.getSymbolicName());
                            BundleState curState = curStateService.getState(curBundle);

                            switch (curState) {
                            case Resolved:
                            case Waiting:
                            case Starting:
                            case Stopping:
                                logger.trace("{} is in an inactive state. Current State: {}",
                                        curBundle.getSymbolicName(), curState.toString());

                                bundleStateSet.addInactiveBundle(curBundle);
                                break;

                            case Installed:
                            case GracePeriod:
                            case Failure:
                                logger.trace("{} is in a failed state. Current State: {}",
                                        curBundle.getSymbolicName(), curState.toString());

                                bundleStateSet.addFailedBundle(curBundle);
                                break;

                            case Unknown:
                            case Active:
                            default:
                                logger.trace("{} is in an active state. Current State: {}",
                                        curBundle.getSymbolicName(), curState.toString());
                                
                                bundleStateSet.addActiveBundle(curBundle);
                                break;
                            }
                        }
                        break; // end case Bundle.Active
                    default:
                        bundleStateSet.addActiveBundle(curBundle);
                        break;
                    }
                }
            }
        }

        return bundleStateSet;
    }

    /**
     * Given a {@code Set} of {@code Feature}s, returns the subset of
     * {@code Features}s that are not installed
     * 
     * @param features
     *            The {@code Set} of {@link Feature}s from which to construct
     *            the subset of {@code Feature}s that are not installed
     * 
     * @return A {@code Set} of {@code Feature}s that are not installed that is
     *         a sub-set of the <i>features</i> {@code Feature}s {@code Set}
     *         parameter
     */
    private Set<Feature> getNotInstalledFeatures(Set<Feature> features) {
        Set<Feature> notInstalledFeatures = new HashSet<Feature>();
        for (Feature curFeature : features) {
            if (!featuresService.isInstalled(curFeature)) {
                logger.debug("{} is not installed.", curFeature.getName());
                notInstalledFeatures.add(curFeature);
            }
        }
        return notInstalledFeatures;
    }

    @Override
    public synchronized void startApplication(Application application) throws ApplicationServiceException {
        try {
            if (application.getMainFeature() != null) {
                featuresService.installFeature(application.getMainFeature().getName());
            } else {
                logger.debug(
                        "Main feature not found when trying to start {}, going through and manually starting all features with install=auto",
                        application.getName());
                for (Feature curFeature : application.getFeatures()) {
                    if (curFeature.getInstall().equalsIgnoreCase(Feature.DEFAULT_INSTALL_MODE)) {
                        logger.debug("Installing feature {} for application {}",
                                curFeature.getName(), application.getName());
                        featuresService.installFeature(curFeature.getName());
                    }
                }
            }
        } catch (Exception e) {
            throw new ApplicationServiceException("Could not start application "
                    + application.getName() + " due to errors.", e);
        }
    }

    @Override
    public void startApplication(String application) throws ApplicationServiceException {

        for (Application curApp : getApplications()) {
            if (curApp.getName().equals(application)) {
                startApplication(curApp);
                return;
            }
        }

        throw new ApplicationServiceException("Could not find application named " + application
                + ". Start application failed.");
    }

    @Override
    public synchronized void stopApplication(Application application) throws ApplicationServiceException {
        try {
            if (application.getMainFeature() != null) {
                if (featuresService.isInstalled(application.getMainFeature())) {

                    // uninstall dependency features
                    Set<Feature> features = getAllDependencyFeatures(application.getMainFeature());
                    for (Feature curFeature : features) {
                        try {
                            if (application.getFeatures().contains(curFeature)
                                    && featuresService.isInstalled(curFeature)) {
                                featuresService.uninstallFeature(curFeature.getName(),
                                        curFeature.getVersion());
                            }
                        } catch (Exception e) {
                            logger.debug("Error while trying to uninstall " + curFeature.getName(),
                                    e);
                        }
                    }
                } else {
                    throw new ApplicationServiceException("Application " + application.getName()
                            + " is already stopped.");
                }
            } else {
                logger.debug(
                        "Main feature not found when trying to stop {}, going through and manually stop all features that are installed.",
                        application.getName());
                for (Feature curFeature : application.getFeatures()) {
                    if (featuresService.isInstalled(curFeature)) {
                        logger.debug("Uninstalling feature {} for application {}",
                                curFeature.getName(), application.getName());
                        featuresService.uninstallFeature(curFeature.getName());
                    }
                }
            }
        } catch (Exception e) {
            throw new ApplicationServiceException(e);
        }
    }

    @Override
    public void stopApplication(String application) throws ApplicationServiceException {
        for (Application curApp : getApplications()) {
            if (curApp.getName().equals(application)) {
                stopApplication(curApp);
                return;
            }
        }
        throw new ApplicationServiceException("Could not find application named " + application
                + ". Stop application failed.");
    }

    /**
     * Data structure for storing various {@link Bundle} states
     */
    @SuppressWarnings("unused")
    private class BundleStateSet {
        Set<Bundle> activeBundles = new HashSet<Bundle>();

        Set<Bundle> inactiveBundles = new HashSet<Bundle>();

        Set<Bundle> failedBundles = new HashSet<Bundle>();

        void addActiveBundle(Bundle bundle) {
            activeBundles.add(bundle);
        }

        void addInactiveBundle(Bundle bundle) {
            inactiveBundles.add(bundle);
        }

        void addFailedBundle(Bundle bundle) {
            failedBundles.add(bundle);
        }

        Set<Bundle> getActiveBundles() {
            return activeBundles;
        }

        int getNumActiveBundles() {
            return activeBundles.size();
        }

        Set<Bundle> getInactiveBundles() {
            return inactiveBundles;
        }

        int getNumInactiveBundles() {
            return inactiveBundles.size();
        }

        Set<Bundle> getFailedBundles() {
            return failedBundles;
        }

        int getNumFailedBundles() {
            return failedBundles.size();
        }
    }

    @Override
    public void addApplication(URI applicationURL) throws ApplicationServiceException {
        try {
            if (applicationURL.toString().startsWith("file:")) {
                applicationURL = ApplicationFileInstaller.install(new File(applicationURL));
                logger.info("Installing newly added feature repo: {}", applicationURL);
            }
            featuresService.addRepository(applicationURL, false);
        } catch (Exception e) {
            logger.warn("Could not add new application due to error.", e);
            throw new ApplicationServiceException(e);
        }
    }

    @Override
    public void removeApplication(URI applicationURL) throws ApplicationServiceException {
        try {
            featuresService.removeRepository(applicationURL, true);
        } catch (Exception e) {
            logger.warn("Could not remove application due to error.", e);
            throw new ApplicationServiceException(e);
        }
    }

    @Override
    public void removeApplication(Application application) throws ApplicationServiceException {
        removeApplication(application.getURI());
    }

    @Override
    public void removeApplication(String applicationName) throws ApplicationServiceException {
        removeApplication(getApplication(applicationName));
    }

    @Override
    public List<FeatureDto> getAllFeatures() {
        List<FeatureDto> features = new ArrayList<FeatureDto>();
        try {
            for (Feature feature : featuresService.listFeatures()) {
                features.add(getFeatureView(feature));
            }
        } catch (Exception ex) {
            logger.warn("getAllFeatures Exception: " + ex.getMessage());
        }
        return features;
    }

    private Map<String, String> getFeature2Repo() {
        Map<String, String> feature2repo = new HashMap<String, String>();
        try {
            for (Repository repository : featuresService.listRepositories()) {
                for (Feature feature : repository.getFeatures()) {
                    feature2repo.put(feature.getId(), repository.getName());
                }
            }
        } catch (Exception ex) {
            logger.warn("getFeature2Repo Exception: " + ex.getMessage());
        }
        return feature2repo;
    }
    
    private FeatureDto getFeatureView(Feature feature) {
        String status = featuresService.isInstalled(feature) ? INSTALLED
                : UNINSTALLED;
        String repository = getFeature2Repo().get(feature.getId());
        return new FeatureDto(feature, status, repository);
    }

    @Override
    public List<FeatureDto> findApplicationFeatures(String applicationName) {
        List<FeatureDto> features = new ArrayList<FeatureDto>();
        try {
            for (Feature feature : getRepositoryFeatures(applicationName)) {
                if (!isAppInFeatureList(feature, applicationName)) {
                    features.add(getFeatureView(feature));
                }
            }
        } catch (Exception ex) {
            logger.warn("getRepositoryFeatures Exception: " + ex.getMessage());
        }
        return features;
    }

    private boolean isAppInFeatureList(Feature feature, String applicationName) {
        String appKey = feature.getName() + "-" + feature.getVersion();
        return appKey.equalsIgnoreCase(applicationName);
    }

    private List<Feature> getRepositoryFeatures(String repositoryName) {
        List<Feature> repoFeatures = new ArrayList<Feature>();
        for (Repository repository : featuresService.listRepositories()) {
            if (repository.getName().equalsIgnoreCase(repositoryName)) {
                try {
                    repoFeatures = Arrays.asList(repository.getFeatures());
                } catch (Exception ex) {
                    logger.warn("getRepositoryFeatures Exception: "
                            + ex.getMessage());
                }
                break;
            }
        }
        return repoFeatures;
    }

}
