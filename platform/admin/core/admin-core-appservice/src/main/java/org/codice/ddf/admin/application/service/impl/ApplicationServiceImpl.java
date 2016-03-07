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
package org.codice.ddf.admin.application.service.impl;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.karaf.bundle.core.BundleState;
import org.apache.karaf.bundle.core.BundleStateService;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Dependency;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.FeaturesService.Option;
import org.apache.karaf.features.Repository;
import org.codice.ddf.admin.application.rest.model.FeatureDetails;
import org.codice.ddf.admin.application.service.Application;
import org.codice.ddf.admin.application.service.ApplicationNode;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.codice.ddf.admin.application.service.ApplicationServiceException;
import org.codice.ddf.admin.application.service.ApplicationStatus;
import org.codice.ddf.admin.application.service.ApplicationStatus.ApplicationState;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
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
public class ApplicationServiceImpl implements ApplicationService, ServiceListener {

    private static final String POST_CONFIG_START = "admin-post-install-modules";

    private static final String POST_CONFIG_STOP = "admin-modules-installer";

    private static final String INSTALLATION_PROFILE_PREFIX = "profile-";

    private static final String INSTALLED = "Installed";

    private static final String UNINSTALLED = "Uninstalled";

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationServiceImpl.class);

    private FeaturesService featuresService = null;

    private List<BundleStateService> bundleStateServices = null;

    private Set<String> ignoredApplicationNames = null;

    private String configFileName;

    private static final int BUNDLE_WAIT_TIMEOUT = 2;

    /**
     * Creates a new instance of Application Service.
     *
     * @param bundleStateServices List of BundleStateServices that allow fine-grained
     *                            information about bundle status for deployment services (like
     *                            blueprint and spring).
     */
    public ApplicationServiceImpl(List<BundleStateService> bundleStateServices) {
        BundleContext context = getContext();
        ServiceReference<FeaturesService> featuresServiceRef = context.getServiceReference(
                FeaturesService.class);
        this.featuresService = context.getService(featuresServiceRef);
        this.bundleStateServices = bundleStateServices;
        ignoredApplicationNames = new HashSet<>();

        try {
            // If the service is not available at this time, it means this is the first
            // boot of the system and we need to listen for the completion of the
            // boot cycle in order to update configuration properties
            if (context.getServiceReference("org.apache.karaf.features.BootFinished") == null) {
                context.addServiceListener(this,
                        "(objectclass=org.apache.karaf.features.BootFinished)");
            }
        } catch (InvalidSyntaxException e) {
            LOGGER.error("Failed to create service listener filter", e);
        }
    }

    @Override
    public Set<Application> getApplications() {
        LOGGER.trace("Getting all applications.");
        Repository[] repos = {};
        try {
            repos = featuresService.listRepositories();

            LOGGER.debug("Found {} applications from feature service.", repos.length);

            if (LOGGER.isDebugEnabled()) {
                for (int ii = 0; ii < repos.length; ++ii) {
                    LOGGER.debug("Repo/App {}: {}", ii, repos[ii].getName());
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Unable to get list of Repositories.", e);
        }

        Set<Application> applications = new HashSet<Application>(repos.length);
        for (int i = 0; i < repos.length; i++) {
            Application newApp = new ApplicationImpl(repos[i]);
            try {
                if (!ignoredApplicationNames.contains(newApp.getName()) && newApp.getFeatures()
                        .size() > 0) {
                    applications.add(newApp);
                }
            } catch (ApplicationServiceException ase) {
                LOGGER.warn("Exception while trying to find information for application named {}. "
                        + "It will be excluded from the application list.", newApp.getName(), ase);
            }
        }
        return new TreeSet<>(applications);
    }

    private Set<String> getApplicationNames() {
        return getApplications().stream()
                .map(a -> a.getName())
                .collect(Collectors.toSet());
    }

    @Override
    public Application getApplication(String applicationName) {
        for (Application curApp : getApplications()) {
            if (curApp.getName()
                    .equalsIgnoreCase(applicationName)) {
                return curApp;
            }
        }
        return null;
    }

    @Override
    public boolean isApplicationStarted(Application application) {
        return application.getAutoInstallFeatures().stream().allMatch(featuresService::isInstalled);
    }

    @Override
    public ApplicationStatus getApplicationStatus(Application application) {
        Set<Feature> uninstalledFeatures = new HashSet<>();
        Set<Feature> requiredFeatures = new HashSet<>();
        Set<Bundle> errorBundles = new HashSet<>();
        ApplicationState installState = null;

        // check features
        try {
            // Check Main Feature
            Feature mainFeature = application.getMainFeature();
            boolean isMainFeatureUninstalled = true;
            if (mainFeature != null) {
                isMainFeatureUninstalled = !featuresService.isInstalled(mainFeature);
                requiredFeatures.add(mainFeature);
            } else {

                Set<Feature> features = application.getFeatures();
                if (features.size() == 1) {
                    requiredFeatures.addAll(getAllDependencyFeatures(features.iterator()
                            .next()));
                } else {
                    for (Feature curFeature : features) {
                        if (StringUtils.equalsIgnoreCase(Feature.DEFAULT_INSTALL_MODE,
                                curFeature.getInstall())) {
                            requiredFeatures.addAll(getAllDependencyFeatures(curFeature));
                        }
                    }
                }
            }

            LOGGER.debug("{} has {} required features that must be started.",
                    application.getName(),
                    requiredFeatures.size());

            BundleStateSet bundleStates = getCurrentBundleStates(requiredFeatures);
            errorBundles.addAll(bundleStates.getFailedBundles());
            errorBundles.addAll(bundleStates.getInactiveBundles());

            if (bundleStates.getNumFailedBundles() > 0) {
                // Any failed bundles, regardless of feature state, indicate a
                // failed application state
                installState = ApplicationState.FAILED;
            } else if ((mainFeature != null && isMainFeatureUninstalled)
                    || bundleStates.getNumInactiveBundles() > 0) {
                installState = ApplicationState.INACTIVE;
            } else if (bundleStates.getNumTransitionalBundles() > 0) {
                installState = ApplicationState.UNKNOWN;
            } else {
                installState = ApplicationState.ACTIVE;
            }
        } catch (Exception e) {
            LOGGER.warn("Encountered an error while trying to determine status of application {}. "
                    + "Setting status as UNKNOWN.", application.getName(), e);
            installState = ApplicationState.UNKNOWN;
        }

        return new ApplicationStatusImpl(application,
                installState,
                uninstalledFeatures,
                errorBundles);
    }

    /**
     * Sets the names of applications that this service should ignore when
     * checking status.
     *
     * @param applicationNames List of application names, these names must
     *                         exactly match the name of the application to ignore.
     */
    public void setIgnoredApplications(List<String> applicationNames) {
        if (applicationNames != null) {
            ignoredApplicationNames = new HashSet<String>(applicationNames);
            LOGGER.debug("Ignoring applications with the following names: {}",
                    ignoredApplicationNames);
        }
    }

    /**
     * Sets the configuration file used for initial installation and starts the
     * installation.
     *
     * @param configFileName Absolute path name of the file containing the application
     *                       list.
     */
    public void setConfigFileName(String configFileName) {
        this.configFileName = configFileName;
    }

    protected BundleContext getContext() {
        Bundle cxfBundle = FrameworkUtil.getBundle(ApplicationServiceImpl.class);
        if (cxfBundle != null) {
            return cxfBundle.getBundleContext();
        }
        return null;
    }

    @Override
    public Set<ApplicationNode> getApplicationTree() {
        Set<ApplicationNode> applicationTree = new TreeSet<ApplicationNode>();
        Set<Application> unfilteredApplications = getApplications();
        Set<Application> filteredApplications = new HashSet<Application>();

        for (Application application : unfilteredApplications) {
            if (!ignoredApplicationNames.contains(application.getName())) {
                filteredApplications.add(application);
            }
        }

        Map<Application, ApplicationNodeImpl> appMap =
                new HashMap<Application, ApplicationNodeImpl>(filteredApplications.size());
        // add all values into a map
        for (Application curApp : filteredApplications) {
            appMap.put(curApp, new ApplicationNodeImpl(curApp, getApplicationStatus(curApp)));
        }

        // The boolean is used because this function is used twice in a row.
        //   The proper output should be that of the second call rather than the first.
        traverseDependencies(appMap, filteredApplications, false);
        traverseDependencies(appMap, filteredApplications, true);

        // determine the root applications (contain no parent) and return those
        for (Entry<Application, ApplicationNodeImpl> curAppNode : appMap.entrySet()) {
            if (curAppNode.getValue()
                    .getParent() == null) {
                LOGGER.debug("Adding {} as a root application.",
                        curAppNode.getKey()
                                .getName());
                applicationTree.add(curAppNode.getValue());
            }
        }

        return applicationTree;
    }

    /**
     * Finds a parent and children dependencies for each app.  Needs to be run twice
     * in order to get full dependency correlations.
     *
     * @param appMap               Application Map containing all the application nodes.
     * @param filteredApplications Set containing all the application nodes minus those in the ignored list
     * @param reportDebug          Boolean that allows debug statements to be output or not.  Only reason
     *                             why this exists is because this function will be called twice and only
     *                             the second set of statements will be relevant
     */
    private void traverseDependencies(Map<Application, ApplicationNodeImpl> appMap,
            Set<Application> filteredApplications, boolean reportDebug) {
        // find dependencies in each app and add them into correct node
        for (Entry<Application, ApplicationNodeImpl> curAppNode : appMap.entrySet()) {
            try {
                // main feature will contain dependencies
                Feature mainFeature = curAppNode.getKey()
                        .getMainFeature();

                if (null == mainFeature) {
                    if (reportDebug) {
                        LOGGER.debug("Application \"{}\" does not contain a main feature",
                                curAppNode.getKey()
                                        .getName());
                    }
                    continue;
                }

                // eliminate duplications with a set
                Set<Dependency> dependencies = new HashSet<>(mainFeature.getDependencies());
                // remove any features that are local to the application
                dependencies.removeAll(curAppNode.getKey()
                        .getFeatures());
                // loop through all of the features that are left to determine
                // where they are from
                Set<Application> depAppSet = new HashSet<>();
                for (Dependency curDepFeature : dependencies) {
                    Application dependencyApp =
                            findFeature(featuresService.getFeature(curDepFeature.getName()),
                                    filteredApplications);
                    if (dependencyApp != null) {
                        if (dependencyApp.equals(curAppNode.getKey())) {
                            if (reportDebug) {
                                LOGGER.debug("Self-dependency");
                            }
                            continue;
                        } else {
                            if (reportDebug) {
                                LOGGER.debug(
                                        "Application {} depends on the feature {} which is located in application {}.",
                                        curAppNode.getKey()
                                                .getName(),
                                        curDepFeature.getName(),
                                        dependencyApp.getName());
                            }
                            depAppSet.add(dependencyApp);
                        }
                    }
                }
                if (!depAppSet.isEmpty()) {
                    Application parentApp;
                    if (depAppSet.size() > 1) {
                        parentApp = findCommonParent(depAppSet, appMap);
                        if (parentApp == null) {
                            if (reportDebug) {
                                LOGGER.warn(
                                        "Found more than 1 application dependency for application {}. Could not determine which one is the correct parent. Application will be sent back as root application.",
                                        curAppNode.getKey()
                                                .getName());
                            }
                            continue;
                        }
                    } else {
                        parentApp = depAppSet.iterator()
                                .next();
                    }
                    // update the dependency app with a new child
                    ApplicationNode parentAppNode = appMap.get(parentApp);
                    parentAppNode.getChildren()
                            .add(curAppNode.getValue());
                    curAppNode.getValue()
                            .setParent(parentAppNode);
                } else {
                    if (reportDebug) {
                        LOGGER.debug(
                                "No dependency applications found for {}. This will be sent back as a root application.",
                                curAppNode.getKey()
                                        .getName());
                    }
                }

                // ApplicationServiceException from DDF and Exception from Karaf
                // (FeaturesService)
            } catch (Exception e) {
                if (reportDebug) {
                    LOGGER.warn(
                            "Encountered error while determining dependencies for \"{}\". This may cause an incomplete application hierarchy to be created.",
                            curAppNode.getKey()
                                    .getName(),
                            e);
                }
            }
        }
    }

    /**
     * Finds a common parent that contains all other applications as parent
     * dependencies.
     *
     * @param applicationSet Set of applications that should be found in a single parent.
     * @param appMap         Application Map containing all the application nodes.
     * @return A single application that is the parent which contains all of the
     * required applications as dependencies or null if no parent is
     * found.
     */
    private Application findCommonParent(Set<Application> applicationSet,
            Map<Application, ApplicationNodeImpl> appMap) {

        // build dependency trees for each application in the set
        Map<Application, Set<Application>> applicationTreeSet =
                new HashMap<Application, Set<Application>>(applicationSet.size());
        for (Application curDependency : applicationSet) {
            Set<Application> curDepSet = new HashSet<Application>();
            curDepSet.add(curDependency);
            for (ApplicationNode curParent = appMap.get(curDependency)
                    .getParent(); curParent != null; curParent = curParent.getParent()) {
                curDepSet.add(curParent.getApplication());
            }
            applicationTreeSet.put(curDependency, curDepSet);
        }

        // check through each set to see if any application contains everything
        // within its parents
        for (Entry<Application, Set<Application>> curAppEntry : applicationTreeSet.entrySet()) {
            if (!(new HashSet<Application>(applicationSet).retainAll(curAppEntry.getValue()))) {
                LOGGER.debug("{} contains all needed dependencies.",
                        curAppEntry.getKey()
                                .getName());
                return curAppEntry.getKey();
            } else {
                LOGGER.trace("{} does not contain all needed dependencies.",
                        curAppEntry.getKey()
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
        LOGGER.debug("Looking for installation profile features");
        List<Feature> profiles = new ArrayList<>();
        try {
            profiles = Arrays.asList(featuresService.listFeatures())
                    .stream()
                    .filter(f -> f.getName()
                            .contains(INSTALLATION_PROFILE_PREFIX))
                    .sorted((f1, f2) -> Integer.compare(f1.getStartLevel(), f2.getStartLevel()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOGGER.error(
                    "Encountered an error while trying to obtain the installation profile features.",
                    e);
        }

        return profiles;
    }

    /**
     * Locates a given feature within the specified set of applications.
     *
     * @param feature      Feature to look for.
     * @param applications Set of applications to check for the feature.
     * @return The first application that contains the feature or null if no
     * application contains the feature.
     */
    protected Application findFeature(Feature feature, Set<Application> applications) {
        LOGGER.debug("Looking for feature {} - {}", feature.getName(), feature.getVersion());
        for (Application curApp : applications) {
            try {
                if (curApp.getFeatures()
                        .contains(feature)) {
                    return curApp;
                }
            } catch (Exception e) {
                LOGGER.warn(
                        "Encountered and error when trying to check features in application named {}. "
                                + "Skipping and checking other applications.",
                        curApp,
                        e);
            }
        }
        LOGGER.warn("Could not find feature {} in any known application, returning null.",
                feature.getName());

        return null;
    }

    /**
     * Retrieves all of the dependencies for a given feature.
     *
     * @param feature Feature to look for dependencies on.
     * @return A set of all features that are dependencies
     */
    private Set<Feature> getAllDependencyFeatures(Feature feature) throws Exception {
        Set<Feature> tmpList = new HashSet<>();
        // get accurate feature reference from service - workaround for
        // KARAF-2896 'RepositoryImpl load method incorrectly populates
        // "features" list'
        Feature curFeature = featuresService.getFeature(feature.getName(), feature.getVersion());

        if (curFeature != null) {
            for (Dependency dependencyFeature : curFeature.getDependencies()) {
                Feature feat = featuresService.getFeature(dependencyFeature.getName(),
                        dependencyFeature.getVersion());
                if (StringUtils.equals(curFeature.getRepositoryUrl(), feat.getRepositoryUrl())) {
                    tmpList.addAll(getAllDependencyFeatures(feat));
                }
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
     * each bundle
     */
    private final BundleStateSet getCurrentBundleStates(Set<Feature> features) {
        BundleStateSet bundleStateSet = new BundleStateSet();

        for (Feature curFeature : features) {
            for (BundleInfo curBundleInfo : curFeature.getBundles()) {
                Bundle curBundle = getContext().getBundle(curBundleInfo.getLocation());
                if (curBundle != null && curBundle.adapt(BundleRevision.class)
                        .getTypes() != BundleRevision.TYPE_FRAGMENT) {

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
                            LOGGER.trace("Checking {} for bundle state of {}.",
                                    curStateService.getName(),
                                    curBundle.getSymbolicName());
                            BundleState curState = curStateService.getState(curBundle);

                            switch (curState) {
                            case Resolved:

                            case Stopping:
                                LOGGER.trace("{} is in an inactive state. Current State: {}",
                                        curBundle.getSymbolicName(),
                                        curState.toString());

                                bundleStateSet.addInactiveBundle(curBundle);
                                break;

                            case Installed:
                            case Failure:
                                LOGGER.trace("{} is in a failed state. Current State: {}",
                                        curBundle.getSymbolicName(),
                                        curState.toString());

                                bundleStateSet.addFailedBundle(curBundle);
                                break;

                            case Waiting:
                            case Starting:
                            case GracePeriod:
                                LOGGER.trace("{} is in a transitional state. Current State: {}",
                                        curBundle.getSymbolicName(),
                                        curState.toString());

                                bundleStateSet.addTransitionalBundle(curBundle);
                                break;

                            case Active:
                                LOGGER.trace("{} is in an active state. Current State: {}",
                                        curBundle.getSymbolicName(),
                                        curState.toString());

                                bundleStateSet.addActiveBundle(curBundle);
                                break;

                            case Unknown:
                            default:
                                // Ignore - BundleStateService unaware of this bundle.
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
     * @param features The {@code Set} of {@link Feature}s from which to construct
     *                 the subset of {@code Feature}s that are not installed
     * @return A {@code Set} of {@code Feature}s that are not installed that is
     * a sub-set of the <i>features</i> {@code Feature}s {@code Set}
     * parameter
     */
    private Set<Feature> getNotInstalledFeatures(Set<Feature> features) {
        Set<Feature> notInstalledFeatures = new HashSet<Feature>();
        for (Feature curFeature : features) {
            if (!featuresService.isInstalled(curFeature)) {
                LOGGER.debug("{} is not installed.", curFeature.getName());
                notInstalledFeatures.add(curFeature);
            }
        }
        return notInstalledFeatures;
    }

    @Override
    public synchronized void startApplication(Application application)
            throws ApplicationServiceException {
        try {
            LOGGER.debug("Starting Application {} - {}",
                    application.getName(),
                    application.getVersion());
            Set<Feature> autoInstallFeatures = application.getAutoInstallFeatures();
            if (!autoInstallFeatures.isEmpty()) {
                Set<String> autoFeatureNames = autoInstallFeatures.stream()
                        .map(Feature::getName)
                        .collect(Collectors.toSet());
                for (Feature feature : autoInstallFeatures) {
                    if (featuresService.isInstalled(feature)) {
                        autoFeatureNames.remove(feature.getName());
                    } else {
                        for (Dependency dependency : feature.getDependencies()) {
                            if (!application.getName()
                                    .equals(dependency.getName()) && getApplicationNames().contains(
                                    dependency.getName())) {
                                if (!isApplicationStarted(getApplication(dependency.getName()))) {
                                    startApplication(dependency.getName());
                                }
                                autoFeatureNames.remove(dependency.getName());
                            }
                        }
                    }
                }
                if (!autoFeatureNames.isEmpty()) {
                    featuresService.installFeatures(autoFeatureNames,
                            EnumSet.of(Option.NoAutoRefreshBundles));
                    waitForApplication(application);
                }
            }

        } catch (Exception e) {
            throw new ApplicationServiceException(
                    "Could not start application " + application.getName() + " due to errors.", e);
        }
    }

    @Override
    public void startApplication(String application) throws ApplicationServiceException {

        for (Application curApp : getApplications()) {
            if (curApp.getName()
                    .equals(application)) {
                startApplication(curApp);
                return;
            }
        }

        throw new ApplicationServiceException(
                "Could not find application named " + application + ". Start application failed.");
    }

    @Override
    public synchronized void stopApplication(Application application)
            throws ApplicationServiceException {
        try {
            for (Feature feature : application.getFeatures()) {
                if (featuresService.isInstalled(feature)) {
                    featuresService.uninstallFeature(feature.getName(),
                            feature.getVersion(),
                            EnumSet.of(Option.NoAutoRefreshBundles));
                }
            }
            waitForApplication(application);
        } catch (Exception e) {
            throw new ApplicationServiceException(e);
        }
    }

    @Override
    public void stopApplication(String application) throws ApplicationServiceException {
        for (Application curApp : getApplications()) {
            if (curApp.getName()
                    .equals(application)) {
                stopApplication(curApp);
                return;
            }
        }
        throw new ApplicationServiceException(
                "Could not find application named " + application + ". Stop application failed.");
    }

    private void waitForApplication(Application application)
            throws ApplicationServiceException, InterruptedException {
        long timeoutLimit = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(
                BUNDLE_WAIT_TIMEOUT);
        boolean starting = true;
        while (starting) {
            BundleStateSet bundleStates = getCurrentBundleStates(application.getFeatures());
            if (!bundleStates.getTransitionalBundles()
                    .isEmpty()) {
                if (System.currentTimeMillis() > timeoutLimit) {
                    break;
                }
                LOGGER.trace("Waiting for the following bundles to become ACTIVE: {}",
                        bundleStates.getTransitionalBundles()
                                .stream()
                                .map(i -> i.toString())
                                .collect(Collectors.joining(", ")));
                this.wait(TimeUnit.SECONDS.toMillis(1));
            } else {
                starting = false;
            }
        }
    }

    @Override
    public void addApplication(URI applicationURL) throws ApplicationServiceException {
        try {
            if (applicationURL.toString()
                    .startsWith("file:")) {
                applicationURL = ApplicationFileInstaller.install(new File(applicationURL));
                LOGGER.info("Installing newly added feature repo: {}", applicationURL);
            }
            featuresService.addRepository(applicationURL, false);
        } catch (Exception e) {
            LOGGER.warn("Could not add new application due to error.", e);
            throw new ApplicationServiceException(e);
        }
    }

    @Override
    public void removeApplication(URI applicationURL) throws ApplicationServiceException {
        try {
            //This is a workaround for the Karaf FeaturesService
            //To remove the repository, it attempts to uninstall all features
            //whether they are uninstalled or not.
            uninstallAllFeatures(applicationURL);
            featuresService.removeRepository(applicationURL, false);
        } catch (Exception e) {
            LOGGER.warn("Could not remove application due to error.", e);
            throw new ApplicationServiceException(e);
        }
    }

    @Override
    public void removeApplication(Application application) throws ApplicationServiceException {
        try {
            if (application != null) {
                uninstallAllFeatures(application);
                featuresService.removeRepository(application.getURI(), false);
            }
        } catch (Exception e) {
            LOGGER.warn("Could not remove application due to error.", e);
            throw new ApplicationServiceException(e);
        }

    }

    @Override
    public void removeApplication(String applicationName) throws ApplicationServiceException {
        if (applicationName != null) {
            removeApplication(getApplication(applicationName));
        }
    }

    /**
     * This method takes in an Application's URI and finds the application
     * that needs to have all features uninstalled
     *
     * @param applicationURL - application to have all its features uninstalled
     */
    private void uninstallAllFeatures(URI applicationURL) {
        if (applicationURL != null) {
            Set<Application> applications = getApplications();

            //Loop through all the applications for a match
            for (Application application : applications) {
                URI applicationURI = application.getURI();
                if (applicationURI != null) {
                    if (StringUtils.equals(applicationURL.toString(), applicationURI.toString())) {
                        uninstallAllFeatures(application);
                        break;
                    }
                }
            }
        }
    }

    /**
     * @param application - application to have all its features uninstalled
     */
    private void uninstallAllFeatures(Application application) {
        try {
            Set<Feature> features = application.getFeatures();
            for (Feature feature : features) {
                if (featuresService.isInstalled(feature)) {
                    try {
                        featuresService.uninstallFeature(feature.getName(),
                                feature.getVersion(),
                                EnumSet.of(Option.NoAutoRefreshBundles));
                    } catch (Exception e) {
                        //if there is an issue uninstalling a feature try to keep uninstalling the other features
                        LOGGER.warn("Could not uninstall feature: {} version: {}",
                                feature.getName(),
                                feature.getVersion(),
                                e);
                    }
                }
            }
        } catch (ApplicationServiceException ase) {
            LOGGER.error("Error obtaining feature list from application", ase);
        }
    }

    @Override
    public List<FeatureDetails> getAllFeatures() {
        List<FeatureDetails> features = new ArrayList<FeatureDetails>();
        try {
            for (Feature feature : featuresService.listFeatures()) {
                features.add(getFeatureView(feature));
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
        List<FeatureDetails> features = getRepositoryFeatures(applicationName).stream()
                .filter(feature -> !isAppInFeatureList(feature, applicationName))
                .map(this::getFeatureView)
                .collect(Collectors.toList());
        return features;
    }

    private boolean isAppInFeatureList(Feature feature, String applicationName) {
        String appKey = feature.getName() + "-" + feature.getVersion();
        return appKey.equalsIgnoreCase(applicationName);
    }

    private List<Feature> getRepositoryFeatures(String repositoryName) {
        List<Feature> repoFeatures = new ArrayList<>();
        try {
            Feature feature = featuresService.getFeature(repositoryName);
            for (Repository repository : featuresService.listRepositories()) {
                if (StringUtils.equals(repository.getURI()
                        .toString(), feature.getRepositoryUrl())) {
                    repoFeatures = Arrays.asList(repository.getFeatures());
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Could not get Repository Features", e);
        }
        return repoFeatures;
    }

    @Override
    public void serviceChanged(ServiceEvent serviceEvent) {
        if (serviceEvent.getType() == ServiceEvent.REGISTERED) {
            BundleContext context = getContext();
            try {
                ServiceReference<ConfigurationAdmin> configAdminRef = context.getServiceReference(
                        ConfigurationAdmin.class);
                ConfigurationAdmin configAdmin = context.getService(configAdminRef);
                Configuration config =
                        configAdmin.getConfiguration(ApplicationServiceImpl.class.getName());
                Dictionary<String, Object> properties = config.getProperties();

                LOGGER.debug("Checking the configuration file on the first run.");
                ApplicationConfigInstaller configInstaller = new ApplicationConfigInstaller(
                        configFileName,
                        this,
                        featuresService,
                        POST_CONFIG_START,
                        POST_CONFIG_STOP);
                configInstaller.start();
                config.update(properties);

            } catch (Exception e) {
                LOGGER.warn("Could not check for installer application configuration file.", e);
            } finally {
                context.removeServiceListener(this);
            }
        }
    }

    /**
     * Data structure for storing various {@link Bundle} states
     */
    @SuppressWarnings("unused")
    private static class BundleStateSet {
        Set<Bundle> activeBundles = new HashSet<>();

        Set<Bundle> inactiveBundles = new HashSet<>();

        Set<Bundle> failedBundles = new HashSet<>();

        Set<Bundle> transitionalBundles = new HashSet<>();

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

        void addTransitionalBundle(Bundle bundle) {
            transitionalBundles.add(bundle);
        }

        Set<Bundle> getTransitionalBundles() {
            return transitionalBundles;
        }

        int getNumTransitionalBundles() {
            return transitionalBundles.size();
        }
    }

}
