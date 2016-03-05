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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.apache.karaf.bundle.core.BundleState;
import org.apache.karaf.bundle.core.BundleStateService;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Dependency;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.FeaturesService.Option;
import org.apache.karaf.features.Repository;
import org.apache.karaf.features.internal.service.RepositoryImpl;
import org.codice.ddf.admin.application.rest.model.FeatureDetails;
import org.codice.ddf.admin.application.service.Application;
import org.codice.ddf.admin.application.service.ApplicationNode;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.codice.ddf.admin.application.service.ApplicationServiceException;
import org.codice.ddf.admin.application.service.ApplicationStatus;
import org.codice.ddf.admin.application.service.ApplicationStatus.ApplicationState;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;

public class ApplicationServiceImplTest {

    private static final String TEST_NO_MAIN_FEATURE_1_FILE_NAME =
            "test-features-no-main-feature.xml";

    private static final String TEST_MAIN_FEATURES_1_MAIN_FEATURE_NAME = "main-feature";

    private static final String TEST_MAIN_FEATURES_2_MAIN_FEATURE_NAME = "main-feature2";

    private static final String TEST_MAIN_FEATURES_1_FEATURE_1_NAME = "test-feature-1";

    private static final String TEST_APP = "test-app";

    private static final String TEST_APP2 = "test-app2";

    private static final String TEST_APP_VERSION = "1.0.0";

    // Must be a bundle that is defined in only one feature of the features file
    // under test.
    private static final String TEST_MAIN_FEATURES_1_FEATURE_1_UNIQUE_BUNDLE_LOCATION =
            "mvn:org.codice.test/codice.test.bundle2/2.0.0";

    private static final String TEST_MAIN_FEATURES_2_UNIQUE_BUNDLE_LOCATION =
            "mvn:org.codice.test/codice.test.bundle1/1.0.0";

    private static final String TEST_NO_MAIN_FEATURE_2_FILE_NAME =
            "test-features-no-main-feature2.xml";

    private static final String TEST_MAIN_FEATURE_1_FILE_NAME =
            "test-features-with-main-feature.xml";

    private static final String TEST_MAIN_FEATURE_2_FILE_NAME =
            "test-features-with-main-feature2.xml";

    private static final String TEST_INSTALL_PROFILE_FILE_NAME =
            "test-features-install-profiles.xml";

    private static final String TEST_PREREQ_MAIN_FEATURE_FILE_NAME =
            "test-features-prereq-main-feature.xml";

    private static final String TEST_FEATURE_1_NAME = "TestFeature";

    private static final String TEST_FEATURE_2_NAME = "TestFeature2";

    private static final String TEST_FEATURE_VERSION = "0.0.0";

    private static final String TEST_APP_NAME = "TestApp";

    private static final String TEST_REPO_URI = "mvn:group.id/artifactid/1.0.0/xml/features";

    private static final String NO_REPO_FEATURES = "Could not get Repository Features";

    private static final String NO_APP_FEATURES = "Could not obtain Application Features.";

    private static final String MAP_FAIL_STRING = "Could not map Features to their Repositories.";

    private static final String FEATURE_FAIL_STRING = "Could not obtain all features.";

    private static final String UNINSTALL_FAIL = "Could not uninstall feature";

    private static final String UNINSTALL_ASE = "Error obtaining feature list from application";

    private static final String STOP_APP_ERROR = "Error while trying to uninstall";

    private static final String FIND_FEAT_EX = "Skipping and checking other applications.";

    private static final String FIND_FEAT_EX2 =
            "Could not find feature null in any known application, returning null.";

    private static final String PROF_INST_EX =
            "Encountered an error while trying to obtain the installation profile features.";

    private static final String APP_STATUS_EX =
            "Encountered an error while trying to determine status of application";

    private static Repository noMainFeatureRepo1, noMainFeatureRepo2, mainFeatureRepo,
            mainFeatureRepo2;

    private static List<BundleStateService> bundleStateServices;

    private Logger logger = LoggerFactory.getLogger(ApplicationServiceImplTest.class);

    private BundleContext bundleContext;

    private ServiceReference<FeaturesService> mockFeatureRef;

    /**
     * Creates a {@code Repository} from a features.xml file
     *
     * @param featuresFile The features.xml file from which to create a
     *                     {@code Repository}
     * @return A {@link Repository} created from the received features.xml file
     * @throws Exception
     */
    private static Repository createRepo(String featuresFile) throws Exception {
        RepositoryImpl repo = new RepositoryImpl(ApplicationServiceImplTest.class.getClassLoader()
                .getResource(featuresFile)
                .toURI());
        repo.load();

        return repo;
    }

    /**
     * Creates default {@link BundleContext}, {@code List} of
     * {@code BundleStateService}s, and {@link Repository} objects for use in
     * the tests.
     * <p>
     * NOTE: These must be in {@code setUp()} method rather than a
     * {@code beforeClass()} method because they are modified by individual
     * tests as part of the setup for individual test conditions. @see
     * {@link #createMockFeaturesService(Set, Set, Set)}
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        // Recreate the repos and BundleContext prior to each test in order to
        // ensure modifications made in one test do not effect another test.
        noMainFeatureRepo1 = createRepo(TEST_NO_MAIN_FEATURE_1_FILE_NAME);
        noMainFeatureRepo2 = createRepo(TEST_NO_MAIN_FEATURE_2_FILE_NAME);
        mainFeatureRepo = createRepo(TEST_MAIN_FEATURE_1_FILE_NAME);
        mainFeatureRepo2 = createRepo(TEST_MAIN_FEATURE_2_FILE_NAME);
        bundleContext = mock(BundleContext.class);

        mockFeatureRef = (ServiceReference<FeaturesService>) mock(ServiceReference.class);

        when(bundleContext.getServiceReference(FeaturesService.class)).thenReturn(mockFeatureRef);

        bundleStateServices = new ArrayList<BundleStateService>();

        // Create a BundleStateService for Blueprint
        BundleStateService mockBundleStateService = mock(BundleStateService.class);
        when(mockBundleStateService.getName()).thenReturn(BundleStateService.NAME_BLUEPRINT);
        bundleStateServices.add(mockBundleStateService);
    }

    /**
     * Tests that the {@link ApplicationServiceImpl#getApplications()} method
     * returns the correct number of applications.
     *
     * @throws Exception
     */
    @Test
    public void testGetApplications() throws Exception {
        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(noMainFeatureRepo1,
                noMainFeatureRepo2));
        FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);
        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        Set<Application> applications = appService.getApplications();
        assertNotNull(applications);
        assertEquals(2, applications.size());
    }

    /**
     * Test method for
     * {@link ApplicationService#getApplicationStatus(Application)}
     * <p>
     * Verifies method returns an {@link ApplicationState#ACTIVE} state for an
     * Application under the following conditions:
     * <p>
     * <ul>
     * <li>Main feature is installed</li>
     * <li>All dependency features are installed</li>
     * <li>The bundle state and extended bundle state of each bundle specified
     * in each dependency feature is {@link Bundle#ACTIVE} and
     * {@link BundleState#Active}, respectively</li>
     * </ul>
     *
     * @throws Exception
     */
    @Test
    public void testGetApplicationStatusReturnsActiveStatusWhenAllFeaturesInstalledAndAllBundlesActive()
            throws Exception {
        FeaturesService featuresService = createMockFeaturesService(mainFeatureRepo, null, null);

        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);
        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        assertEquals(ApplicationService.class.getName()
                        + " does not contain the expected number of Applications",
                1,
                appService.getApplications()
                        .size());

        assertEquals(mainFeatureRepo.getName() + " returned unexpected state",
                ApplicationState.ACTIVE,
                appService.getApplicationStatus(appService.getApplications()
                        .toArray(new Application[] {})[0])
                        .getState());
    }

    /**
     * Test method for
     * {@link ApplicationServiceImpl#getApplicationStatus(Application)}
     * <p>
     * Verifies that {@link ApplicationState#ACTIVE} is returned when the
     * extended bundle state reported by an injection framework states that one
     * bundle is in an {@link BundleState#Unknown} state and the rest of the
     * bundles are in an {@link BundleState#Active} state.
     *
     * @throws Exception
     */
    @Test
    public void testGetApplicationStatusReturnsActiveWhenBundleStateServiceStateIsUnknown()
            throws Exception {

        ApplicationService appService = getAppServiceWithBundleStateServiceInGivenState(
                mainFeatureRepo,
                BundleState.Unknown);
        assertNotNull(
                "Repository \"" + mainFeatureRepo.getName() + "\" does not contain any bundles",
                appService);

        assertEquals(mainFeatureRepo.getName() + " returned unexpected state",
                ApplicationState.ACTIVE,
                appService.getApplicationStatus(appService.getApplications()
                        .toArray(new Application[] {})[0])
                        .getState());
    }

    /**
     * Test method for
     * {@link ApplicationServiceImpl#getApplicationStatus(Application)}
     * <p>
     * Verifies that {@link ApplicationState#INACTIVE} is returned when an
     * {@code Application}'s main feature is not installed, but all of its
     * {@code Bundle}s states and extended states are {@code Bundle#ACTIVE} and
     * {@code BundleState#Active}, respectively.
     *
     * @throws Exception
     */
    @Test
    public void testGetApplicationStatusReturnsInactiveWhenMainFeatureNotInstalledAndAllBundlesActive()
            throws Exception {

        Set<String> notInstalledFeatures = new HashSet<String>();
        notInstalledFeatures.add(TEST_MAIN_FEATURES_1_MAIN_FEATURE_NAME);

        FeaturesService featuresService = createMockFeaturesService(mainFeatureRepo,
                notInstalledFeatures,
                null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);

        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        assertEquals("More than one application was returned from mainFeatureRepo",
                1,
                appService.getApplications()
                        .size());

        assertEquals("mainFeatureRepo returned unexpected state",
                ApplicationState.INACTIVE,
                appService.getApplicationStatus(appService.getApplications()
                        .toArray(new Application[] {})[0])
                        .getState());

    }

    /**
     * Test method for
     * {@link ApplicationService#getApplicationStatus(Application)}
     * <p>
     * Verifies method returns an {@link ApplicationState#ACTIVE} state for an
     * {@code Application} under the following conditions:
     * <p>
     * <ul>
     * <li>Main feature is installed</li>
     * <li>One dependency feature is NOT installed</li>
     * <li>Dependency feature that is not installed contains a {@code Bundle}
     * with a state of {@link Bundle#ACTIVE} and extended state of
     * {@link BundleState#Active}</li>
     * </ul>
     * <p>
     * This effectively emulates the circumstance in which there is a
     * {@code Feature} in another {@code Application} that includes and starts
     * the same {@code Bundle} that is contained in a {@code Feature} of the
     * current {@code Application} that is not installed.
     *
     * @throws Exception
     */
    @Test
    public void testGetApplicationStatusReturnsActiveStatusForNotInstalledFeatureDependencyThatContainsActiveBundle()
            throws Exception {

        Set<String> notInstalledFeatureNames = new HashSet<String>();
        notInstalledFeatureNames.add(TEST_MAIN_FEATURES_1_FEATURE_1_NAME);

        FeaturesService featuresService = createMockFeaturesService(mainFeatureRepo,
                notInstalledFeatureNames,
                null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);

        assertNotNull("Features repo is missing feature with the name of \""
                        + TEST_MAIN_FEATURES_1_FEATURE_1_NAME + "\"",
                featuresService.getFeature(TEST_MAIN_FEATURES_1_FEATURE_1_NAME));

        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        assertEquals(ApplicationService.class.getName()
                        + " does not contain the expected number of Applications",
                1,
                appService.getApplications()
                        .size());

        assertEquals(mainFeatureRepo.getName() + " returned unexpected state",
                ApplicationState.ACTIVE,
                appService.getApplicationStatus(appService.getApplications()
                        .toArray(new Application[] {})[0])
                        .getState());
    }

    /**
     * Test method for
     * {@link ApplicationServiceImpl#getApplicationStatus(Application)}
     * <p>
     * Verifies that {@link ApplicationState#INACTIVE} is returned when
     * {@code Bundle} state is {@link Bundle#RESOLVED}
     *
     * @throws Exception
     */
    @Test
    public void testGetApplicationStatusReturnsInactiveWhenBundleStateIsResolved()
            throws Exception {
        ApplicationService appService = getAppServiceWithBundleInGivenState(mainFeatureRepo2,
                Bundle.RESOLVED);

        assertEquals(mainFeatureRepo2.getName() + " returned unexpected state",
                ApplicationState.INACTIVE,
                appService.getApplicationStatus(appService.getApplications()
                        .toArray(new Application[] {})[0])
                        .getState());
    }

    /**
     * Test method for
     * {@link ApplicationServiceImpl#getApplicationStatus(Application)}
     * <p>
     * Verifies that {@link ApplicationState#INACTIVE} is returned when
     * {@code Bundle} state is {@link Bundle#STARTING}
     *
     * @throws Exception
     */
    @Test
    public void testGetApplicationStatusReturnsInactiveWhenBundleStateIsStarting()
            throws Exception {

        ApplicationService appService = getAppServiceWithBundleInGivenState(mainFeatureRepo2,
                Bundle.STARTING);

        assertEquals(mainFeatureRepo2.getName() + " returned unexpected state",
                ApplicationState.INACTIVE,
                appService.getApplicationStatus(appService.getApplications()
                        .toArray(new Application[] {})[0])
                        .getState());
    }

    /**
     * Test method for
     * {@link ApplicationServiceImpl#getApplicationStatus(Application)}
     * <p>
     * Verifies that {@link ApplicationState#INACTIVE} is returned when
     * {@code Bundle} state is {@link Bundle#STOPPING}
     *
     * @throws Exception
     */
    @Test
    public void testGetApplicationStatusReturnsInactiveWhenBundleStateIsStopping()
            throws Exception {
        ApplicationService appService = getAppServiceWithBundleInGivenState(mainFeatureRepo2,
                Bundle.STOPPING);

        assertEquals(mainFeatureRepo2.getName() + " returned unexpected state",
                ApplicationState.INACTIVE,
                appService.getApplicationStatus(appService.getApplications()
                        .toArray(new Application[] {})[0])
                        .getState());
    }

    /**
     * Test method for
     * {@link ApplicationServiceImpl#getApplicationStatus(Application)}
     * <p>
     * Verifies that {@link ApplicationState#INACTIVE} is returned when the
     * extended bundle state reported by an injection framework states that one
     * bundle is in a {@link BundleState#Resolved} state and the rest of the
     * bundles are in a {@link BundleState#Active} state.
     *
     * @throws Exception
     */
    @Test
    public void testGetApplicationStatusReturnsInactiveWhenBundleStateServiceStateIsResolved()
            throws Exception {
        ApplicationService appService = getAppServiceWithBundleStateServiceInGivenState(
                mainFeatureRepo2,
                BundleState.Resolved);
        assertNotNull(
                "Repository \"" + mainFeatureRepo.getName() + "\" does not contain any bundles",
                appService);

        assertEquals(mainFeatureRepo2.getName() + " returned unexpected state",
                ApplicationState.INACTIVE,
                appService.getApplicationStatus(appService.getApplications()
                        .toArray(new Application[] {})[0])
                        .getState());
    }

    /**
     * Test method for
     * {@link ApplicationServiceImpl#getApplicationStatus(Application)}
     * <p>
     * Verifies that {@link ApplicationState#INACTIVE} is returned when the
     * extended bundle state reported by an injection framework states that one
     * bundle is in an {@link BundleState#Waiting} state and the rest of the
     * bundles are in an {@link BundleState#Active} state.
     *
     * @throws Exception
     */
    @Test
    public void testGetApplicationStatusReturnsUnknownWhenBundleStateServiceStateIsWaiting()
            throws Exception {

        ApplicationService appService = getAppServiceWithBundleStateServiceInGivenState(
                mainFeatureRepo2,
                BundleState.Waiting);
        assertNotNull(
                "Repository \"" + mainFeatureRepo.getName() + "\" does not contain any bundles",
                appService);

        assertEquals(mainFeatureRepo2.getName() + " returned unexpected state",
                ApplicationState.UNKNOWN,
                appService.getApplicationStatus(appService.getApplications()
                        .toArray(new Application[] {})[0])
                        .getState());
    }

    /**
     * Test method for
     * {@link ApplicationServiceImpl#getApplicationStatus(Application)}
     * <p>
     * Verifies that {@link ApplicationState#INACTIVE} is returned when the
     * extended bundle state reported by an injection framework states that one
     * bundle is in an {@link BundleState#Starting} state and the rest of the
     * bundles are in an {@link BundleState#Active} state.
     *
     * @throws Exception
     */
    @Test
    public void testGetApplicationStatusReturnsUnkownWhenBundleStateServiceStateIsStarting()
            throws Exception {
        ApplicationService appService = getAppServiceWithBundleStateServiceInGivenState(
                mainFeatureRepo2,
                BundleState.Starting);
        assertNotNull(
                "Repository \"" + mainFeatureRepo.getName() + "\" does not contain any bundles",
                appService);

        assertEquals(mainFeatureRepo2.getName() + " returned unexpected state",
                ApplicationState.UNKNOWN,
                appService.getApplicationStatus(appService.getApplications()
                        .toArray(new Application[] {})[0])
                        .getState());
    }

    /**
     * Test method for
     * {@link ApplicationServiceImpl#getApplicationStatus(Application)}
     * <p>
     * Verifies that {@link ApplicationState#INACTIVE} is returned when the
     * extended bundle state reported by an injection framework states that one
     * bundle is in an {@link BundleState#Stopping} state and the rest of the
     * bundles are in an {@link BundleState#Active} state.
     *
     * @throws Exception
     */
    @Test
    public void testGetApplicationStatusReturnsInactiveWhenBundleStateServiceStateIsStopping()
            throws Exception {
        ApplicationService appService = getAppServiceWithBundleStateServiceInGivenState(
                mainFeatureRepo2,
                BundleState.Stopping);
        assertNotNull(
                "Repository \"" + mainFeatureRepo.getName() + "\" does not contain any bundles",
                appService);

        assertEquals(mainFeatureRepo2.getName() + " returned unexpected state",
                ApplicationState.INACTIVE,
                appService.getApplicationStatus(appService.getApplications()
                        .toArray(new Application[] {})[0])
                        .getState());
    }

    /**
     * Test method for
     * {@link ApplicationService#getApplicationStatus(Application)}
     * <p>
     * Verifies method returns an {@link ApplicationState#FAILED } state for an
     * {@code Application} under the following conditions:
     * <p>
     * <ul>
     * <li>Main feature is installed</li>
     * <li>All dependency features are installed</li>
     * <li>One dependency feature contains a Bundle that is in an inactive state
     * </li>
     * </ul>
     *
     * @throws Exception
     */
    @Test
    public void testGetApplicationStatusReturnsFailedStatusWhenFeatureDependencyContainsInactiveBundle()
            throws Exception {
        // Verify the pre-conditions
        int bundleInclusionCount = 0;
        for (Feature feature : mainFeatureRepo2.getFeatures()) {
            for (BundleInfo bundleInfo : feature.getBundles()) {
                if (bundleInfo.getLocation()
                        .equals(TEST_MAIN_FEATURES_2_UNIQUE_BUNDLE_LOCATION)) {
                    ++bundleInclusionCount;
                }
            }
        }
        assertEquals("Bundle is not included in repository the expected number of times",
                1,
                bundleInclusionCount);

        // Execute test
        Set<String> inactiveBundleNames = new HashSet<String>();
        inactiveBundleNames.add(TEST_MAIN_FEATURES_2_UNIQUE_BUNDLE_LOCATION);

        FeaturesService featuresService = createMockFeaturesService(mainFeatureRepo2,
                null,
                inactiveBundleNames);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);

        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        assertEquals(ApplicationService.class.getName()
                        + " does not contain the expected number of Applications",
                1,
                appService.getApplications()
                        .size());

        assertEquals(mainFeatureRepo2.getName() + " returned unexpected state",
                ApplicationState.FAILED,
                appService.getApplicationStatus(appService.getApplications()
                        .toArray(new Application[] {})[0])
                        .getState());
    }

    /**
     * Test method for
     * {@link ApplicationService#getApplicationStatus(Application)}
     * <p>
     * Verifies method returns an {@link ApplicationState#FAILED } state for an
     * {@code Application} under the following conditions:
     * <p>
     * <ul>
     * <li>Main feature is installed</li>
     * <li>One dependency feature is not installed</li>
     * <li>Dependency feature that is not installed contains Bundle(s) whose
     * states and extended states are inactive</li>
     * </ul>
     *
     * @throws Exception
     */
    @Test
    public void testGetApplicationStatusReturnsFailedStatusWhenOneFeatureConsistingOfInactiveBundlesIsNotInstalled()
            throws Exception {

        Set<Repository> mainFeaturesRepoSet = new HashSet<Repository>();
        Set<Feature> notInstalledFeatures = new HashSet<Feature>();
        Set<BundleInfo> inactiveBundles = new HashSet<BundleInfo>();

        for (Feature feature : mainFeatureRepo2.getFeatures()) {
            if (feature.getName()
                    .equals(TEST_MAIN_FEATURES_2_MAIN_FEATURE_NAME)) {
                notInstalledFeatures.add(feature);
                inactiveBundles.addAll(feature.getBundles());
                break;
            }
        }

        assertEquals("Feature is not included in repository the expected number of times",
                1,
                notInstalledFeatures.size());
        assertTrue("No bundles included in Feature", inactiveBundles.size() > 0);

        mainFeaturesRepoSet.add(mainFeatureRepo2);

        FeaturesService featuresService = createMockFeaturesService(mainFeaturesRepoSet,
                notInstalledFeatures,
                inactiveBundles);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);

        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        assertEquals(ApplicationService.class.getName()
                        + " does not contain the expected number of Applications",
                1,
                appService.getApplications()
                        .size());

        assertEquals(mainFeatureRepo2.getName() + " returned unexpected state",
                ApplicationState.FAILED,
                appService.getApplicationStatus(appService.getApplications()
                        .toArray(new Application[] {})[0])
                        .getState());
    }

    /**
     * Test method for
     * {@link ApplicationServiceImpl#getApplicationStatus(Application)}
     * <p>
     * Verifies that {@link ApplicationState#INACTIVE} is returned when
     * {@code Bundle} state is {@link Bundle#INSTALLED}
     *
     * @throws Exception
     */
    @Test
    public void testGetApplicationStatusReturnsInactiveWhenBundleStateIsInstalled()
            throws Exception {
        ApplicationService appService = getAppServiceWithBundleInGivenState(mainFeatureRepo2,
                Bundle.INSTALLED);

        assertEquals(mainFeatureRepo2.getName() + " returned unexpected state",
                ApplicationState.FAILED,
                appService.getApplicationStatus(appService.getApplications()
                        .toArray(new Application[] {})[0])
                        .getState());
    }

    /**
     * Test method for
     * {@link ApplicationServiceImpl#getApplicationStatus(Application)}
     * <p>
     * Verifies that {@link ApplicationState#INACTIVE} is returned when
     * {@code Bundle} state is {@link Bundle#UNINSTALLED}
     *
     * @throws Exception
     */
    @Test
    public void testGetApplicationStatusReturnsInactiveWhenBundleStateIsUninnstalled()
            throws Exception {
        ApplicationService appService = getAppServiceWithBundleInGivenState(mainFeatureRepo2,
                Bundle.UNINSTALLED);

        assertEquals(mainFeatureRepo2.getName() + " returned unexpected state",
                ApplicationState.FAILED,
                appService.getApplicationStatus(appService.getApplications()
                        .toArray(new Application[] {})[0])
                        .getState());
    }

    /**
     * Test method for
     * {@link ApplicationServiceImpl#getApplicationStatus(Application)}
     * <p>
     * Verifies that {@link ApplicationState#FAILED} is returned when the
     * extended bundle state reported by an injection framework states that one
     * bundle is in an {@link BundleState#Installed} state and the rest of the
     * bundles are in an {@link BundleState#Active} state.
     *
     * @throws Exception
     */
    @Test
    public void testGetApplicationStatusReturnsFailedWhenBundleStateServiceStateIsInstalled()
            throws Exception {
        ApplicationService appService = getAppServiceWithBundleStateServiceInGivenState(
                mainFeatureRepo2,
                BundleState.Installed);
        assertNotNull(
                "Repository \"" + mainFeatureRepo2.getName() + "\" does not contain any bundles",
                appService);

        assertEquals(mainFeatureRepo2.getName() + " returned unexpected state",
                ApplicationState.FAILED,
                appService.getApplicationStatus(appService.getApplications()
                        .toArray(new Application[] {})[0])
                        .getState());
    }

    /**
     * Test method for
     * {@link ApplicationServiceImpl#getApplicationStatus(Application)}
     * <p>
     * Verifies that {@link ApplicationState#FAILED} is returned when the
     * extended bundle state reported by an injection framework states that one
     * bundle is in an {@link BundleState#GracePeriod} state and the rest of the
     * bundles are in an {@link BundleState#Active} state.
     *
     * @throws Exception
     */
    @Test
    public void testGetApplicationStatusReturnsUnknownWhenBundleStateServiceStateIsGracePeriod()
            throws Exception {
        ApplicationService appService = getAppServiceWithBundleStateServiceInGivenState(
                mainFeatureRepo2,
                BundleState.GracePeriod);
        assertNotNull(
                "Repository \"" + mainFeatureRepo2.getName() + "\" does not contain any bundles",
                appService);

        assertEquals(mainFeatureRepo2.getName() + " returned unexpected state",
                ApplicationState.UNKNOWN,
                appService.getApplicationStatus(appService.getApplications()
                        .toArray(new Application[] {})[0])
                        .getState());
    }

    /**
     * Test method for
     * {@link ApplicationServiceImpl#getApplicationStatus(Application)}
     * <p>
     * Verifies that {@link ApplicationState#FAILED} is returned when the
     * extended bundle state reported by an injection framework states that one
     * bundle is in an {@link BundleState#Failure} state and the rest of the
     * bundles are in an {@link BundleState#Active} state.
     *
     * @throws Exception
     */
    @Test
    public void testGetApplicationStatusReturnsFailedWhenBundleStateServiceStateIsFailure()
            throws Exception {
        ApplicationService appService = getAppServiceWithBundleStateServiceInGivenState(
                mainFeatureRepo2,
                BundleState.Failure);
        assertNotNull(
                "Repository \"" + mainFeatureRepo2.getName() + "\" does not contain any bundles",
                appService);

        assertEquals(mainFeatureRepo2.getName() + " returned unexpected state",
                ApplicationState.FAILED,
                appService.getApplicationStatus(appService.getApplications()
                        .toArray(new Application[] {})[0])
                        .getState());
    }

    /**
     * Test method for
     * {@link ApplicationServiceImpl#getApplicationStatus(Application)}
     * <p>
     * Verifies that {@link ApplicationState#FAILED} is returned when the
     * extended bundle state reported by an injection framework states that one
     * bundle is in an {@link BundleState#Resolved} state, one bundle is in an
     * {@link BundleState#Installed} state and the rest of the bundles are in an
     * {@link BundleState#Active} state. This simulates a condition wherein the
     * {@code getApplicationStatus} method must discern between conditions that,
     * independent of each other, would produce
     * {@code ApplicationState#INACTIVE}, {@code ApplicationState#FAILED}, or
     * {@code ApplicationState#ACTIVE} states, respectively, and determine which
     * state to return for the overall Application.
     *
     * @throws Exception
     */
    @Test
    public void testGetApplicationStatusReturnsFailedWhenBundleStateServiceStatesIncludeActiveResolvedAndFailure()
            throws Exception {
        FeaturesService featuresService = createMockFeaturesService(noMainFeatureRepo1, null, null);
        Set<Bundle> bundleSet = getXBundlesFromFeaturesService(featuresService, 2);
        assertNotNull(noMainFeatureRepo1.getName() + " does not contain 2 bundles", bundleSet);
        Bundle[] bundles = bundleSet.toArray(new Bundle[] {});

        when(bundleStateServices.get(0)
                .getState(bundles[0])).thenReturn(BundleState.Resolved);
        when(bundleStateServices.get(0)
                .getState(bundles[1])).thenReturn(BundleState.Failure);

        ApplicationService appService = getAppServiceWithBundleStateServiceInGivenState(
                noMainFeatureRepo1,
                BundleState.Failure);
        assertNotNull(
                "Repository \"" + noMainFeatureRepo1.getName() + "\" does not contain any bundles",
                appService);

        assertEquals(noMainFeatureRepo1.getName() + " returned unexpected state",
                ApplicationState.FAILED,
                appService.getApplicationStatus(appService.getApplications()
                        .toArray(new Application[] {})[0])
                        .getState());
    }

    /**
     * Test method for
     * {@link ApplicationServiceImpl#isApplicationStarted(Application)}
     * <p>
     * Verifies that method returns true when application state is
     * {@link ApplicationState#ACTIVE}
     *
     * @throws Exception
     */
    @Test
    public void testIsApplicationStartedReturnsTrueForActiveApplicationState() throws Exception {

        FeaturesService featuresService = createMockFeaturesService(mainFeatureRepo, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);

        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        assertTrue(appService.isApplicationStarted(appService.getApplication(
                TEST_MAIN_FEATURES_1_MAIN_FEATURE_NAME)));
    }

    /**
     * Test method for
     * {@link ApplicationServiceImpl#isApplicationStarted(Application)}
     * <p>
     * Verifies that method returns false when application state is
     * {@link ApplicationState#INACTIVE}
     *
     * @throws Exception
     */
    @Test
    public void testIsApplicationStartedReturnsFalseForInactiveApplicationState() throws Exception {

        Set<String> notInstalledFeatures = new HashSet<String>();
        notInstalledFeatures.add(TEST_MAIN_FEATURES_1_MAIN_FEATURE_NAME);

        FeaturesService featuresService = createMockFeaturesService(mainFeatureRepo,
                notInstalledFeatures,
                null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);

        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        assertFalse(appService.isApplicationStarted(appService.getApplication(
                TEST_MAIN_FEATURES_1_MAIN_FEATURE_NAME)));
    }

    /**
     * Test method for
     * {@link ApplicationServiceImpl#isApplicationStarted(Application)}
     * <p>
     * Verifies that method returns false when application state is
     * {@link ApplicationState#FAILED}
     *
     * @throws Exception
     */
    @Test
    public void testIsApplicationStartedReturnsFalseForFailedApplicationState() throws Exception {

        Set<String> inactiveBundles = new HashSet<String>();
        inactiveBundles.add(TEST_MAIN_FEATURES_2_UNIQUE_BUNDLE_LOCATION);

        FeaturesService featuresService = createMockFeaturesService(mainFeatureRepo2,
                null,
                inactiveBundles);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);
        when(featuresService.isInstalled(mainFeatureRepo2.getFeatures()[0])).thenReturn(false);

        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        assertFalse(appService.isApplicationStarted(appService.getApplication(
                TEST_MAIN_FEATURES_2_MAIN_FEATURE_NAME)));
    }

    /**
     * Tests receiving application status for an application in the ACTIVE
     * state.
     */
    @Test
    public void testGetActiveApplicationStatus() throws Exception {
        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(noMainFeatureRepo1));

        FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);

        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        Set<Application> applications = appService.getApplications();
        assertEquals(1, applications.size());
        for (Application curApp : applications) {
            ApplicationStatus status = appService.getApplicationStatus(curApp);
            assertEquals(curApp, status.getApplication());
            assertEquals(ApplicationState.ACTIVE, status.getState());
            assertTrue(status.getErrorBundles()
                    .isEmpty());
            assertTrue(status.getErrorFeatures()
                    .isEmpty());
        }

    }

    @Test
    public void testGetApplicationStatusCoreFeature() throws Exception {
        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(createRepo(
                TEST_PREREQ_MAIN_FEATURE_FILE_NAME)));

        FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);
        when(featuresService.isInstalled(any())).thenReturn(false);

        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        Set<Application> applications = appService.getApplications();
        assertEquals(1, applications.size());
        for (Application curApp : applications) {
            ApplicationStatus status = appService.getApplicationStatus(curApp);
            assertEquals(curApp, status.getApplication());
            assertEquals(ApplicationState.INACTIVE, status.getState());
            assertTrue(status.getErrorBundles()
                    .isEmpty());
            assertTrue(status.getErrorFeatures()
                    .isEmpty());
        }

    }

    /**
     * Tests that the service properly ignores applications when checking for
     * application status.
     *
     * @throws Exception
     */
    @Test
    public void testIgnoreApplications() throws Exception {
        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(noMainFeatureRepo1,
                noMainFeatureRepo2));

        FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);

        ApplicationServiceImpl appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        // just ignore the first application
        List<String> ignoredApps1 = new ArrayList<>(1);
        ignoredApps1.add(TEST_APP);
        appService.setIgnoredApplications(ignoredApps1);
        Set<Application> applications = appService.getApplications();
        assertNotNull(applications);
        assertEquals(1, applications.size());
        assertEquals(TEST_APP2,
                applications.iterator()
                        .next()
                        .getName());

        // now ignore both applications
        List<String> ignoredApps2 = new ArrayList<>(2);
        ignoredApps2.add(TEST_APP);
        ignoredApps2.add(TEST_APP2);
        appService.setIgnoredApplications(ignoredApps2);
        applications = appService.getApplications();
        assertNotNull(applications);
        assertEquals(0, applications.size());

        // ignore none
        List<String> ignoredApps3 = new ArrayList<>(0);
        appService.setIgnoredApplications(ignoredApps3);
        applications = appService.getApplications();
        assertNotNull(applications);
        assertEquals(2, applications.size());
    }

    /**
     * Tests that an application tree is passed back correctly.
     *
     * @throws Exception
     */
    @Test
    public void testApplicationTree() throws Exception {
        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(mainFeatureRepo,
                mainFeatureRepo2));

        FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);

        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        Set<ApplicationNode> rootApps = appService.getApplicationTree();

        assertNotNull(rootApps);
        assertEquals(1, rootApps.size());

        ApplicationNode mainAppNode = rootApps.iterator()
                .next();
        assertEquals(1,
                mainAppNode.getChildren()
                        .size());
        assertNull(mainAppNode.getParent());
        assertEquals("main-feature2",
                mainAppNode.getChildren()
                        .iterator()
                        .next()
                        .getApplication()
                        .getName());
        assertEquals(mainAppNode,
                mainAppNode.getChildren()
                        .iterator()
                        .next()
                        .getParent());

        Application mainApp = mainAppNode.getApplication();
        assertEquals("main-feature", mainApp.getName());
    }

    /**
     * Test that an application tree can be created even if there is an app that
     * does not contain a main feature.
     *
     * @throws Exception
     */
    @Test
    public void testApplicationTreeWithNoMainFeature() throws Exception {
        Repository mainFeaturesRepo2 = createRepo(TEST_MAIN_FEATURE_2_FILE_NAME);

        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(mainFeatureRepo,
                mainFeaturesRepo2,
                noMainFeatureRepo1));

        FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);

        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        Set<ApplicationNode> rootApps = appService.getApplicationTree();

        assertNotNull(rootApps);
        assertEquals(2, rootApps.size());
    }

    /**
     * Tests install profile and make sure they load correctly.
     *
     * @throws Exception
     */
    @Test
    public void testInstallProfileFeatures() throws Exception {
        Repository mainFeaturesRepo2 = createRepo(TEST_INSTALL_PROFILE_FILE_NAME);

        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(mainFeatureRepo,
                mainFeaturesRepo2,
                noMainFeatureRepo1));

        FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);

        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        List<Feature> profiles = appService.getInstallationProfiles();

        assertNotNull(profiles);
        assertEquals(2, profiles.size());

        // Ensure order
        Feature profile1 = profiles.get(0);
        Feature profile2 = profiles.get(1);

        assertEquals("profile-b-test1", profile1.getName());
        assertEquals("Desc1", profile1.getDescription());
        List<String> featureNames = getFeatureNames(profile1.getDependencies());
        assertEquals(1, featureNames.size());
        assertTrue(featureNames.contains("main-feature"));

        assertEquals("profile-a-test2", profile2.getName());
        assertEquals("Desc2", profile2.getDescription());
        featureNames = getFeatureNames(profile2.getDependencies());
        assertEquals(2, featureNames.size());
        assertTrue(featureNames.contains("main-feature"));
        assertTrue(featureNames.contains("main-feature2"));
    }

    /**
     * Tests the {@link ApplicationServiceImpl#startApplication(Application)} method
     * for the case where there a main feature exists
     *
     * @throws Exception
     */
    @Test
    public void testStartApplicationMainFeature() throws Exception {
        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(mainFeatureRepo,
                noMainFeatureRepo1,
                noMainFeatureRepo2));
        FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);
        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        Application testApp = mock(ApplicationImpl.class);
        Feature testFeature = mock(Feature.class);
        when(testFeature.getName()).thenReturn(TEST_FEATURE_1_NAME);
        Set<Feature> features = new HashSet<>(Arrays.asList(testFeature));
        Set<String> featureNames = new HashSet<>(features.size());
        features.forEach(f -> featureNames.add(f.getName()));
        when(testApp.getAutoInstallFeatures()).thenReturn(features);

        appService.startApplication(testApp);

        verify(featuresService, atLeastOnce()).installFeatures(featureNames,
                EnumSet.of(Option.NoAutoRefreshBundles));
    }

    /**
     * Tests the {@link ApplicationServiceImpl#startApplication(Application)} method
     * for the case where an exception is thrown
     *
     * @throws Exception
     */
    @Test(expected = ApplicationServiceException.class)
    public void testStartApplicationASE() throws Exception {
        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(mainFeatureRepo,
                noMainFeatureRepo1,
                noMainFeatureRepo2));
        FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);
        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        Application testApp = mock(ApplicationImpl.class);
        Feature testFeature = mock(Feature.class);
        when(testFeature.getName()).thenReturn(TEST_FEATURE_1_NAME);
        when(testApp.getAutoInstallFeatures()).thenReturn(new HashSet<>(Arrays.asList(testFeature)));

        doThrow(new ApplicationServiceException()).when(featuresService)
                .installFeatures(anySet(), any(EnumSet.class));

        appService.startApplication(testApp);

    }

    /**
     * Tests the {@link ApplicationServiceImpl#startApplication(Application)} method
     * for the case where there is no main feature, but other features exist
     *
     * @throws Exception
     */
    @Test
    public void testStartApplicationNoMainFeature() throws Exception {
        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(mainFeatureRepo,
                noMainFeatureRepo1,
                noMainFeatureRepo2));
        FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);
        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        Application testApp = mock(ApplicationImpl.class);
        Feature testFeature1 = mock(Feature.class);
        Feature testFeature2 = mock(Feature.class);
        Set<Feature> featureSet = new HashSet<Feature>();
        featureSet.add(testFeature1);
        featureSet.add(testFeature2);

        when(testApp.getName()).thenReturn(TEST_APP_NAME);
        when(testApp.getAutoInstallFeatures()).thenReturn(featureSet);
        when(testFeature1.getName()).thenReturn(TEST_FEATURE_1_NAME);
        when(testFeature2.getName()).thenReturn(TEST_FEATURE_2_NAME);

        Set<String> featureNames = new HashSet<>(featureSet.size());
        featureSet.forEach(f -> featureNames.add(f.getName()));

        appService.startApplication(testApp);

        verify(featuresService).installFeatures(featureNames,
                EnumSet.of(Option.NoAutoRefreshBundles));
    }

    /**
     * Tests the {@link ApplicationServiceImpl#startApplication(String)} method
     *
     * @throws Exception
     */
    @Test
    public void testStartApplicationStringParam() throws Exception {
        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(mainFeatureRepo,
                noMainFeatureRepo1,
                noMainFeatureRepo2));
        FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);
        when(featuresService.isInstalled(any(Feature.class))).thenReturn(false);
        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        appService.startApplication(TEST_APP);

        Set<String> names = new HashSet<>();
        names.add(mainFeatureRepo.getFeatures()[0].getName());
        names.add(mainFeatureRepo.getFeatures()[1].getName());
        verify(featuresService).installFeatures(names, EnumSet.of(Option.NoAutoRefreshBundles));
    }

    /**
     * Tests the {@link ApplicationServiceImpl#startApplication(String)} method
     * for the case where the application cannot be found
     *
     * @throws Exception
     */
    @Test(expected = ApplicationServiceException.class)
    public void testStartApplicationStringParamASE() throws Exception {
        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(mainFeatureRepo,
                noMainFeatureRepo1,
                noMainFeatureRepo2));
        FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);
        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        appService.startApplication("");    //Shouldn't find this
    }

    /**
     * Tests the {@link ApplicationServiceImpl#stopApplication(Application)} method
     * for the case where a main feature exists
     *
     * @throws Exception
     */
    @Test
    public void testStopApplicationMainFeature() throws Exception {
        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(mainFeatureRepo,
                noMainFeatureRepo1,
                noMainFeatureRepo2));
        FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);
        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        Application testApp1 = mock(ApplicationImpl.class);
        Feature testFeature1 = mock(Feature.class);
        Dependency testDependency1 = mock(Dependency.class);
        List<Dependency> dependencyList1 = new ArrayList<>();
        Set<Feature> featureSet1 = new HashSet<>();
        dependencyList1.add(testDependency1);
        featureSet1.add(testFeature1);

        when(testFeature1.getName()).thenReturn(TEST_FEATURE_1_NAME);
        when(testApp1.getMainFeature()).thenReturn(testFeature1);
        when(testApp1.getFeatures()).thenReturn(featureSet1);
        when(featuresService.isInstalled(testFeature1)).thenReturn(true);
        when(testFeature1.getDependencies()).thenReturn(dependencyList1);
        when(testDependency1.getVersion()).thenReturn(TEST_FEATURE_VERSION);
        when(testFeature1.getVersion()).thenReturn(TEST_FEATURE_VERSION);

        appService.stopApplication(testApp1);

        verify(featuresService, atLeastOnce()).uninstallFeature(TEST_FEATURE_1_NAME,
                TEST_FEATURE_VERSION,
                EnumSet.of(Option.NoAutoRefreshBundles));
    }

    /**
     * Tests the {@link ApplicationServiceImpl#stopApplication(String)} method
     * for the case where the application cannot be found
     */
    @Test(expected = ApplicationServiceException.class)
    public void testStopApplicationStringParamASE() throws Exception {
        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(mainFeatureRepo,
                noMainFeatureRepo1,
                noMainFeatureRepo2));
        FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);
        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        appService.stopApplication("");
    }

    /**
     * Tests the {@link ApplicationServiceImpl#stopApplication(Application)} method
     * for the case where an exception is caught
     *
     * @throws Exception
     */
    @Test(expected = ApplicationServiceException.class)
    public void testStopApplicationGeneralASE() throws Exception {
        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(mainFeatureRepo,
                noMainFeatureRepo1,
                noMainFeatureRepo2));
        FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);
        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        Application testApp1 = mock(ApplicationImpl.class);
        Feature testFeature1 = mock(Feature.class);
        when(testApp1.getFeatures()).thenReturn(new HashSet<>(Arrays.asList(testFeature1)));
        when(featuresService.isInstalled(any())).thenReturn(true);

        doThrow(new Exception()).when(featuresService)
                .uninstallFeature(anyString(), anyString(), any());

        appService.stopApplication(testApp1);
    }

    /**
     * Tests the {@link ApplicationServiceImpl#stopApplication(String)} method
     * for the case where a main feature exists
     *
     * @throws Exception
     */
    @Test
    public void testStopApplicationMainFeatureStringParam() throws Exception {
        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(mainFeatureRepo,
                noMainFeatureRepo1,
                noMainFeatureRepo2));
        FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);
        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        Feature[] featureList = mainFeatureRepo.getFeatures();

        appService.stopApplication(TEST_APP);

        verify(featuresService).uninstallFeature(featureList[0].getName(),
                featureList[0].getVersion(),
                EnumSet.of(Option.NoAutoRefreshBundles));
    }

    /**
     * Tests the {@link ApplicationServiceImpl#stopApplication(Application)} method
     * for the case where there is no main feature, but other features exist
     *
     * @throws Exception
     */
    @Test
    public void testStopApplicationNoMainFeature() throws Exception {
        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(mainFeatureRepo,
                noMainFeatureRepo1,
                noMainFeatureRepo2));
        FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);
        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        Application testApp = mock(ApplicationImpl.class);
        Feature testFeature1 = mock(Feature.class);
        Feature testFeature2 = mock(Feature.class);
        Set<Feature> featureSet = new HashSet<Feature>();
        featureSet.add(testFeature1);
        featureSet.add(testFeature2);

        when(testApp.getName()).thenReturn(TEST_APP_NAME);
        when(testApp.getMainFeature()).thenReturn(null);
        when(testApp.getFeatures()).thenReturn(featureSet);
        when(testFeature1.getName()).thenReturn(TEST_FEATURE_1_NAME);
        when(testFeature2.getName()).thenReturn(TEST_FEATURE_2_NAME);
        when(testFeature1.getInstall()).thenReturn(Feature.DEFAULT_INSTALL_MODE);
        when(testFeature2.getInstall()).thenReturn(Feature.DEFAULT_INSTALL_MODE);
        when(featuresService.isInstalled(testFeature1)).thenReturn(true);
        when(featuresService.isInstalled(testFeature2)).thenReturn(true);

        appService.stopApplication(testApp);

        verify(featuresService, atLeastOnce()).uninstallFeature(TEST_FEATURE_1_NAME,
                null,
                EnumSet.of(Option.NoAutoRefreshBundles));
        verify(featuresService, atLeastOnce()).uninstallFeature(TEST_FEATURE_2_NAME,
                null,
                EnumSet.of(Option.NoAutoRefreshBundles));
    }

    /**
     * Tests the {@link ApplicationServiceImpl#stopApplication(String)} method
     * for the case where there is no main feature, but other features exist
     *
     * @throws Exception
     */
    @Test
    public void testStopApplicationNoMainFeatureStringParam() throws Exception {
        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(mainFeatureRepo,
                noMainFeatureRepo1,
                noMainFeatureRepo2));
        FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);
        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        Feature[] featureList = noMainFeatureRepo1.getFeatures();

        appService.stopApplication(TEST_APP);

        verify(featuresService).uninstallFeature(featureList[0].getName(),
                featureList[0].getVersion(),
                EnumSet.of(Option.NoAutoRefreshBundles));
        verify(featuresService).uninstallFeature(featureList[1].getName(),
                featureList[1].getVersion(),
                EnumSet.of(Option.NoAutoRefreshBundles));
    }

    /**
     * Tests the {@link ApplicationServiceImpl#stopApplication(Application)} method
     * for the case where an Exception is thrown
     *
     * @throws Exception
     */
    @Test(expected = ApplicationServiceException.class)
    public void testStopApplicationException() throws Exception {
        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(mainFeatureRepo,
                noMainFeatureRepo1,
                noMainFeatureRepo2));
        FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);
        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        Application testApp = mock(ApplicationImpl.class);
        when(testApp.getMainFeature()).thenReturn(mainFeatureRepo.getFeatures()[1]);
        doThrow(new NullPointerException()).when(testApp)
                .getFeatures();

        appService.stopApplication(testApp);
    }

    /**
     * Tests the {@link ApplicationServiceImpl#removeApplication(Application)} method
     *
     * @throws Exception
     */
    @Test
    public void testRemoveApplicationApplicationParam() throws Exception {
        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(mainFeatureRepo,
                noMainFeatureRepo1,
                noMainFeatureRepo2));
        FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);
        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        Application testApp = mock(ApplicationImpl.class);
        Feature testFeature1 = mock(Feature.class);
        Feature testFeature2 = mock(Feature.class);
        Set<Feature> featureSet = new HashSet<>();
        featureSet.add(testFeature1);
        featureSet.add(testFeature2);
        featuresService.installFeature(testFeature1, EnumSet.noneOf(Option.class));
        featuresService.installFeature(testFeature2, EnumSet.noneOf(Option.class));

        when(testApp.getFeatures()).thenReturn(featureSet);
        when(featuresService.isInstalled(testFeature1)).thenReturn(true);
        when(featuresService.isInstalled(testFeature2)).thenReturn(true);
        when(testFeature1.getName()).thenReturn(TEST_FEATURE_1_NAME);
        when(testFeature1.getVersion()).thenReturn(TEST_FEATURE_VERSION);
        when(testFeature2.getName()).thenReturn(TEST_FEATURE_1_NAME);
        when(testFeature2.getVersion()).thenReturn(TEST_FEATURE_VERSION);
        when(testApp.getURI()).thenReturn(null);

        appService.removeApplication(testApp);

        verify(testApp).getURI();
        verify(featuresService, Mockito.times(2)).uninstallFeature(TEST_FEATURE_1_NAME,
                TEST_FEATURE_VERSION,
                EnumSet.of(Option.NoAutoRefreshBundles));
        verify(featuresService).removeRepository(null, false);
    }

    /**
     * Tests the {@link ApplicationServiceImpl#removeApplication(Application)} method
     * for the case where an exception is thrown
     *
     * @throws Exception
     */
    @Test(expected = ApplicationServiceException.class)
    public void testRemoveApplicationApplicationParamASE() throws Exception {
        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(mainFeatureRepo,
                noMainFeatureRepo1,
                noMainFeatureRepo2));
        FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);
        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        Application testApp = mock(ApplicationImpl.class);

        doThrow(new Exception()).when(featuresService)
                .removeRepository(Mockito.any(URI.class), eq(false));

        appService.removeApplication(testApp);
    }

    /**
     * Tests the {@link ApplicationServiceImpl#getAllFeatures()} method
     *
     * @throws Exception
     */
    @Test
    public void testGetAllFeatures() throws Exception {
        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(mainFeatureRepo,
                noMainFeatureRepo1,
                noMainFeatureRepo2));
        FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);
        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        List<FeatureDetails> result = appService.getAllFeatures();

        assertThat("Returned features should match features in mainFeatureRepo.",
                result.get(0)
                        .getName(),
                is(mainFeatureRepo.getFeatures()[0].getName()));
        assertThat("Returned features should match features in mainFeatureRepo.",
                result.get(0)
                        .getId(),
                is(mainFeatureRepo.getFeatures()[0].getId()));
        assertThat("Should return seven features.", result.size(), is(7));
    }

    /**
     * Tests the {@link ApplicationServiceImpl#getAllFeatures()} method
     * for the case where an exception is thrown in getFeatureToRepository(..)
     *
     * @throws Exception
     */
    @Test
    public void testGetAllFeaturesFTRException() throws Exception {
        ch.qos.logback.classic.Logger root =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        final Appender mockAppender = mock(Appender.class);
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);

        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(mainFeatureRepo,
                noMainFeatureRepo1,
                noMainFeatureRepo2));
        FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);
        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        doThrow(new NullPointerException()).when(featuresService)
                .listRepositories();

        appService.getAllFeatures();

        verify(mockAppender, times(7)).doAppend(argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
                return ((LoggingEvent) argument).getFormattedMessage()
                        .contains(MAP_FAIL_STRING);
            }
        }));
    }

    /**
     * Tests the {@link ApplicationServiceImpl#getAllFeatures()} method
     * for the case where an exception is thrown by the featuresService
     *
     * @throws Exception
     */
    @Test
    public void testGetAllFeaturesException() throws Exception {
        ch.qos.logback.classic.Logger root =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        final Appender mockAppender = mock(Appender.class);
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);

        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(mainFeatureRepo,
                noMainFeatureRepo1,
                noMainFeatureRepo2));
        FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);
        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        doThrow(new NullPointerException()).when(featuresService)
                .listFeatures();

        appService.getAllFeatures();

        verify(mockAppender).doAppend(argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
                return ((LoggingEvent) argument).getFormattedMessage()
                        .contains(FEATURE_FAIL_STRING);
            }
        }));
    }

    /**
     * Tests the {@link ApplicationServiceImpl#removeApplication(Application)} method
     * for the case where an exception is thrown within uninstallAllFeatures(Application)
     *
     * @throws Exception
     */
    @Test
    public void testRemoveApplicationUninstallAllFeaturesException() throws Exception {
        ch.qos.logback.classic.Logger root =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        final Appender mockAppender = mock(Appender.class);
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);

        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(mainFeatureRepo,
                noMainFeatureRepo1,
                noMainFeatureRepo2));
        FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);
        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        Application testApp = mock(ApplicationImpl.class);
        Feature testFeature1 = mock(Feature.class);
        Feature testFeature2 = mock(Feature.class);
        Set<Feature> featureSet = new HashSet<>();
        featureSet.add(testFeature1);
        featureSet.add(testFeature2);
        when(featuresService.isInstalled(any(Feature.class))).thenReturn(true);
        when(testApp.getFeatures()).thenReturn(featureSet);

        doThrow(new Exception()).when(featuresService)
                .uninstallFeature(anyString(), anyString(), any(EnumSet.class));

        appService.removeApplication(testApp);

        verify(mockAppender, times(2)).doAppend(argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
                return ((LoggingEvent) argument).getFormattedMessage()
                        .contains(UNINSTALL_FAIL);
            }
        }));
    }

    /**
     * Tests the {@link ApplicationServiceImpl#removeApplication(Application)} method
     * for the case where an ApplicationServiceException is thrown within uninstallAllFeatures(..)
     *
     * @throws Exception
     */
    @Test
    public void testRemoveApplicationApplicationServiceException() throws Exception {
        ch.qos.logback.classic.Logger root =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        final Appender mockAppender = mock(Appender.class);
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);

        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(mainFeatureRepo,
                noMainFeatureRepo1,
                noMainFeatureRepo2));
        FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);
        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        Application testApp = mock(ApplicationImpl.class);

        doThrow(new ApplicationServiceException()).when(testApp)
                .getFeatures();

        appService.removeApplication(testApp);

        verify(mockAppender).doAppend(argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
                return ((LoggingEvent) argument).getFormattedMessage()
                        .contains(UNINSTALL_ASE);
            }
        }));
    }

    /**
     * Tests the {@link ApplicationServiceImpl#findApplicationFeatures(String)} method
     *
     * @throws Exception
     */
    @Test
    public void testFindApplicationFeatures() throws Exception {
        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(mainFeatureRepo,
                noMainFeatureRepo1,
                noMainFeatureRepo2));
        FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);
        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        Feature testFeature1 = mock(Feature.class);
        Feature[] featureList = {testFeature1};
        Repository testRepo = mock(Repository.class);
        when(testFeature1.getRepositoryUrl()).thenReturn(TEST_REPO_URI);
        when(testRepo.getURI()).thenReturn(new URI(TEST_REPO_URI));
        when(testRepo.getFeatures()).thenReturn(featureList);
        Repository[] repositoryList = {testRepo};
        when(featuresService.getFeature(TEST_APP_NAME)).thenReturn(testFeature1);
        when(featuresService.listRepositories()).thenReturn(repositoryList);
        when(featuresService.isInstalled(testFeature1)).thenReturn(true);

        List<FeatureDetails> result = appService.findApplicationFeatures(TEST_APP_NAME);

        assertThat("Should return one feature.", result.size(), is(1));
    }

    /**
     * Tests the {@link ApplicationServiceImpl#findApplicationFeatures(String)} method
     * for the case where an exception is thrown in getRepositoryFeatures
     *
     * @throws Exception
     */
    @Test
    public void testFindApplicationFeaturesGetRepoFeatException() throws Exception {
        ch.qos.logback.classic.Logger root =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        final Appender mockAppender = mock(Appender.class);
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);

        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(mainFeatureRepo,
                noMainFeatureRepo1,
                noMainFeatureRepo2));
        FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);
        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        Repository testRepo = mock(Repository.class);
        Repository[] repoList = {testRepo};
        when(featuresService.listRepositories()).thenReturn(repoList);
        when(testRepo.getName()).thenReturn(TEST_APP_NAME);
        doThrow(new Exception()).when(testRepo)
                .getFeatures();

        appService.findApplicationFeatures(TEST_APP_NAME);

        verify(mockAppender).doAppend(argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
                return ((LoggingEvent) argument).getFormattedMessage()
                        .contains(NO_REPO_FEATURES);
            }
        }));
    }

    /**
     * Tests the {@link ApplicationServiceImpl#findApplicationFeatures(String)} method
     * for the case where an exception is thrown by the featuresService
     *
     * @throws Exception
     */
    @Test
    public void testFindApplicationFeaturesException() throws Exception {
        ch.qos.logback.classic.Logger root =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        final Appender mockAppender = mock(Appender.class);
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);

        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(mainFeatureRepo,
                noMainFeatureRepo1,
                noMainFeatureRepo2));
        FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);
        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        doThrow(new NullPointerException()).when(featuresService)
                .listRepositories();

        appService.findApplicationFeatures(TEST_APP_NAME);

        verify(mockAppender).doAppend(argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
                return ((LoggingEvent) argument).getFormattedMessage()
                        .contains(NO_REPO_FEATURES);
            }
        }));
    }

    /**
     * Tests the {@link ApplicationServiceImpl#addApplication(URI)} method
     *
     * @throws Exception
     */
    @Test
    public void testAddApplicationURIParam() throws Exception {
        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(noMainFeatureRepo1,
                noMainFeatureRepo2));
        FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);
        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        URI testURI = ApplicationServiceImplTest.class.getClassLoader()
                .getResource("test-kar.zip")
                .toURI();

        appService.addApplication(testURI);
        verify(featuresService).addRepository(Mockito.any(URI.class), eq(false));
    }

    /**
     * Tests the {@link ApplicationServiceImpl#addApplication(URI)} method
     * for the case where an exception is thrown
     *
     * @throws Exception
     */
    @Test(expected = ApplicationServiceException.class)
    public void testAddApplicationASE() throws Exception {
        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(noMainFeatureRepo1,
                noMainFeatureRepo2));
        FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);
        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        URI testURI = ApplicationServiceImplTest.class.getClassLoader()
                .getResource("test-kar.zip")
                .toURI();

        doThrow(new Exception()).when(featuresService)
                .addRepository(Mockito.any(URI.class), eq(false));

        appService.addApplication(testURI);
    }

    /**
     * Tests the {@link ApplicationServiceImpl#removeApplication(URI)} method
     *
     * @throws Exception
     */
    @Test
    public void testRemoveApplicationURIParam() throws Exception {
        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(mainFeatureRepo,
                noMainFeatureRepo2));
        FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);
        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        Repository[] repoList = {mainFeatureRepo};
        URI testURL = mainFeatureRepo.getURI();
        Feature[] featureList = mainFeatureRepo.getFeatures();
        when(featuresService.listRepositories()).thenReturn(repoList);

        appService.removeApplication(testURL);

        verify(featuresService).removeRepository(testURL, false);
        verify(featuresService).uninstallFeature(featureList[0].getName(),
                featureList[0].getVersion(),
                EnumSet.of(Option.NoAutoRefreshBundles));
        verify(featuresService).uninstallFeature(featureList[1].getName(),
                featureList[1].getVersion(),
                EnumSet.of(Option.NoAutoRefreshBundles));
    }

    /**
     * Tests the {@link ApplicationServiceImpl#removeApplication(Application)} method
     * for the case where an exception is thrown
     *
     * @throws Exception
     */
    @Test(expected = ApplicationServiceException.class)
    public void testRemoveApplicationASE() throws Exception {
        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(mainFeatureRepo,
                noMainFeatureRepo2));
        FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);
        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        Repository[] repoList = {mainFeatureRepo};
        URI testURL = mainFeatureRepo.getURI();
        when(featuresService.listRepositories()).thenReturn(repoList);

        doThrow(new Exception()).when(featuresService)
                .removeRepository(Mockito.any(URI.class), eq(false));

        appService.removeApplication(testURL);
    }

    /**
     * Tests the {@link ApplicationServiceImpl#removeApplication(String)} method
     *
     * @throws Exception
     */
    @Test
    public void testRemoveApplicationStringParam() throws Exception {
        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(mainFeatureRepo,
                noMainFeatureRepo1));
        FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);
        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        Feature[] featureList = mainFeatureRepo.getFeatures();

        appService.removeApplication(TEST_APP);

        verify(featuresService).uninstallFeature(featureList[0].getName(),
                featureList[0].getVersion(),
                EnumSet.of(Option.NoAutoRefreshBundles));
        verify(featuresService).uninstallFeature(featureList[1].getName(),
                featureList[1].getVersion(),
                EnumSet.of(Option.NoAutoRefreshBundles));
    }

    /**
     * Tests the {@link ApplicationServiceImpl#findFeature(Feature)} method
     *
     * @throws Exception
     */
    @Test
    public void testFindFeature() throws Exception {
        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(mainFeatureRepo,
                noMainFeatureRepo1));
        FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);
        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        Feature testFeature = mainFeatureRepo.getFeatures()[0];

        Application result = appService.findFeature(testFeature);

        assertTrue("Check that the returned application is the correct one.",
                result.getFeatures()
                        .contains(testFeature));
    }

    /**
     * Tests the {@link ApplicationServiceImpl#findFeature(Feature)} method
     * for the case where exceptions are thrown inside findFeature(Feature, Set<Application>)
     *
     * @throws Exception
     */
    @Test
    public void testFindFeatureExceptions() throws Exception {
        ch.qos.logback.classic.Logger root =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        final Appender mockAppender = mock(Appender.class);
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);

        Application testApp = mock(ApplicationImpl.class);
        final Set<Application> applicationSet = new HashSet<>();
        applicationSet.add(testApp);
        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(mainFeatureRepo,
                noMainFeatureRepo1));
        FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);
        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }

            @Override
            public Set<Application> getApplications() {
                return applicationSet;
            }
        };

        Feature testFeature = mock(Feature.class);
        doThrow(new NullPointerException()).when(testApp)
                .getFeatures();

        appService.findFeature(testFeature);

        verify(mockAppender).doAppend(argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
                return ((LoggingEvent) argument).getFormattedMessage()
                        .contains(FIND_FEAT_EX);
            }
        }));

        verify(mockAppender).doAppend(argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
                return ((LoggingEvent) argument).getFormattedMessage()
                        .contains(FIND_FEAT_EX2);
            }
        }));
    }

    /**
     * Tests the {@link ApplicationServiceImpl#getInstallationProfiles()} method
     * for the case where featuresService.listFeatures() throws an exception
     *
     * @throws Exception
     */
    @Test
    public void testGetInstallProfilesException() throws Exception {
        ch.qos.logback.classic.Logger root =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        final Appender mockAppender = mock(Appender.class);
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);

        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(mainFeatureRepo,
                noMainFeatureRepo1));
        FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);
        ApplicationService appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        doThrow(new NullPointerException()).when(featuresService)
                .listFeatures();

        appService.getInstallationProfiles();

        verify(mockAppender).doAppend(argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
                return ((LoggingEvent) argument).getFormattedMessage()
                        .contains(PROF_INST_EX);
            }
        }));
    }

    /**
     * Tests  the {@link ApplicationServiceImpl#setConfigFileName(String)} method
     *
     * @throws Exception
     */
    @Test
    public void testServiceChanged() throws Exception {
        Set<Repository> activeRepos = new HashSet<>(Arrays.asList(mainFeatureRepo,
                noMainFeatureRepo1));
        FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);
        ApplicationServiceImpl appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };
        appService.setConfigFileName("foo");

        ServiceReference<ConfigurationAdmin> testConfigAdminRef = mock(ServiceReference.class);
        ConfigurationAdmin testConfigAdmin = mock(ConfigurationAdmin.class);
        Configuration testConfig = mock(Configuration.class);
        when(bundleContext.getServiceReference(ConfigurationAdmin.class)).thenReturn(
                testConfigAdminRef);
        when(bundleContext.getService(testConfigAdminRef)).thenReturn(testConfigAdmin);
        when(testConfigAdmin.getConfiguration(ApplicationServiceImpl.class.getName())).thenReturn(
                testConfig);

        Dictionary<String, Object> testProperties = new Hashtable<>();
        testProperties.put("test1", "foo");
        testProperties.put("test2", "bar");

        when(testConfig.getProperties()).thenReturn(testProperties);

        ServiceEvent serviceEvent = mock(ServiceEvent.class);
        when(serviceEvent.getType()).thenReturn(ServiceEvent.REGISTERED);

        appService.serviceChanged(serviceEvent);

        assertThat(testConfig.getProperties()
                .size(), is(testProperties.size()));
        assertThat(testConfig.getProperties()
                .get("test1"), is("foo"));
    }

    /**
     * Tests the {@link ApplicationServiceImpl#getApplicationStatus(Application)} method
     * for the case where an exception is thrown in the main block
     *
     * @throws Exception
     */
    @Test
    public void testGetApplicationStatusException() throws Exception {
        ch.qos.logback.classic.Logger root =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        final Appender mockAppender = mock(Appender.class);
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);

        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(mainFeatureRepo,
                noMainFeatureRepo1));
        FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);
        ApplicationServiceImpl appService = new ApplicationServiceImpl(bundleStateServices) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        Application testApp = mock(ApplicationImpl.class);
        doThrow(new NullPointerException()).when(testApp)
                .getFeatures();

        ApplicationStatus result = appService.getApplicationStatus(testApp);

        verify(mockAppender).doAppend(argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
                return ((LoggingEvent) argument).getFormattedMessage()
                        .contains(APP_STATUS_EX);
            }
        }));

        assertThat("State of resulting ApplicationStatus should be UNKNOWN.",
                result.getState(),
                is(ApplicationState.UNKNOWN));
    }

    /**
     * Builds a list containing the feature names of all features.
     *
     * @param dependencies dependencies to loop through.
     * @return list containing the feature names.
     */
    private List<String> getFeatureNames(List<Dependency> dependencies) {
        List<String> featureNames = new ArrayList<>();
        dependencies.forEach(dependency -> featureNames.add(dependency.getName()));
        return featureNames;
    }

    /**
     * Returns an {@code ApplicationService} that contains one bundle in the
     * received bundle state and the rest of the bundles in an {code
     * Bundle#ACTIVE} state.
     *
     * @param repository  The {@link Repository} from which to build the
     *                    {@code ApplicationService}.
     * @param bundleState The state, as defined in the {@link Bundle} interface, to set
     *                    for one of the {@code Bundle}s in the
     *                    {@code ApplicationService}
     * @return An {@link ApplicationService} with one bundle set to the received
     * bundle state and the rest of the {@code Bundle}s set to the
     * {@link Bundle#ACTIVE} state
     * @throws Exception
     */
    private ApplicationService getAppServiceWithBundleInGivenState(Repository repository,
            int bundleState) throws Exception {
        ApplicationService appService = null;

        FeaturesService featuresService = createMockFeaturesService(repository, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);
        Bundle bundle = getAnyBundleFromFeaturesService(featuresService);

        if (null != bundle) {
            when(bundle.getState()).thenReturn(bundleState);
            appService = new ApplicationServiceImpl(bundleStateServices) {
                @Override
                protected BundleContext getContext() {
                    return bundleContext;
                }
            };
        }

        return appService;
    }

    /**
     * Returns an {@code ApplicationService} that contains one bundle for which
     * the extended bundle state reported by an injection framework is the
     * received {@code BundleState} and the extended bundle state for the rest
     * of the bundles in {@code BundleState#Active}.
     *
     * @param repository  The {@link Repository} from which to build the
     *                    {@code ApplicationService}.
     * @param bundleState The {@link BundleState} to set for one of the {@code Bundle}s
     *                    in the {@code ApplicationService}
     * @return An {@code ApplicationService} with one bundle's extended state
     * set to the received bundle state and the rest of the
     * {@code Bundle}s set to {@code Bundle#ACTIVE}
     * @throws Exception
     */
    private ApplicationService getAppServiceWithBundleStateServiceInGivenState(
            Repository repository, BundleState bundleState) throws Exception {
        ApplicationService appService = null;

        FeaturesService featuresService = createMockFeaturesService(repository, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);
        Bundle bundle = getAnyBundleFromFeaturesService(featuresService);

        if (null != bundle) {
            when(bundleStateServices.get(0)
                    .getState(bundle)).thenReturn(bundleState);

            appService = new ApplicationServiceImpl(bundleStateServices) {
                @Override
                protected BundleContext getContext() {
                    return bundleContext;
                }
            };
        }

        return appService;
    }

    /**
     * Retrieves a {@code Bundle} from the received {@code FeaturesService}
     *
     * @param featuresService The {@link FeaturesService} from which to obtain a
     *                        {@code Bundle}
     * @return A {@link Bundle} from the received {@code FeaturesService} or
     * <code>null</code> if the {@code FeaturesService} does not contain
     * any {@code Bundle}s
     * @throws Exception
     */
    private Bundle getAnyBundleFromFeaturesService(FeaturesService featuresService)
            throws Exception {

        Bundle bundle = null;
        Set<Bundle> bundleSet = getXBundlesFromFeaturesService(featuresService, 1);

        if (null != bundleSet) {
            bundle = bundleSet.toArray(new Bundle[] {})[0];
        }

        return bundle;
    }

    /**
     * Retrieves a given number of {@code Bundle}s from the received
     * {@code FeaturesService}
     *
     * @param featuresService The {@link FeaturesService} from which to obtain a
     *                        {@code Bundle}
     * @param numBundles      The number of bundles to be retrieved from the
     *                        {@code FeaturesService}
     * @return A {@code Set} containing the requested number of {@link Bundle}s
     * from the received {@code FeaturesService} or <code>null</code> if
     * the {@code FeaturesService} does not contain the requested number
     * of {@code Bundle}s
     * @throws Exception
     */
    private Set<Bundle> getXBundlesFromFeaturesService(FeaturesService featuresService,
            int numBundles) throws Exception {
        Set<Bundle> bundles = new HashSet<Bundle>();
        // BundleInfo bundleInfo = null;
        Bundle bundle = null;

        Feature[] features = featuresService.listFeatures();
        List<BundleInfo> bundleInfos = null;

        int ii = 0;
        while (bundles.size() < numBundles && ii < features.length) {
            bundleInfos = features[ii].getBundles();
            int jj = 0;

            while (bundles.size() < numBundles && jj < bundleInfos.size()) {
                bundle = bundleContext.getBundle(bundleInfos.get(ii)
                        .getLocation());

                if (null != bundle) {
                    bundles.add(bundle);
                }

                ++jj;
            }

            ++ii;
        }

        if (bundles.size() < numBundles) {
            bundles = null;
        }

        return bundles;
    }

    /**
     * Creates a mock {@code FeaturesService} object consisting of all of the
     * features contained in a {@code Repository} object.
     *
     * @param repo
     * @param notInstalledFeatureNames
     * @param inactiveBundleLocations
     * @return
     * @throws Exception
     * @see #createMockFeaturesService(Set, Set, Set) for additional details
     */
    private FeaturesService createMockFeaturesService(Repository repo,
            Set<String> notInstalledFeatureNames, Set<String> inactiveBundleLocations)
            throws Exception {

        Set<Feature> notInstalledFeatures = new HashSet<Feature>();
        Set<BundleInfo> inactiveBundles = new HashSet<BundleInfo>();

        for (Feature feature : repo.getFeatures()) {
            if (null != notInstalledFeatureNames
                    && notInstalledFeatureNames.contains(feature.getName())) {
                notInstalledFeatures.add(feature);
            }

            if (null != inactiveBundleLocations) {
                for (BundleInfo bundleInfo : feature.getBundles()) {
                    if (inactiveBundleLocations.contains(bundleInfo.getLocation())) {
                        inactiveBundles.add(bundleInfo);
                    }
                }
            }
        }

        Set<Repository> repoSet = new HashSet<Repository>();
        repoSet.add(repo);

        return createMockFeaturesService(repoSet, notInstalledFeatures, inactiveBundles);

    }

    /**
     * Creates a mock {@code FeaturesService} object consisting of all of the
     * features contained in a {@code Set} of {@code Repository} objects. Each
     * {@code Feature} will be in the <i>installed</i> state unless it is
     * contained in the received set of features that are not to be installed.
     * Each {@code Bundle} will be in the {@code Bundle#ACTIVE} state and the
     * {@code BundleState#Active} extended bundle state (as reported by a
     * dependency injection framework) unless it is contained in the received
     * set of {@code Bundle}s that are not to be active, in which case the
     * {@code Bundle} will be in the {@code Bundle#INSTALLED} state and the
     * {@code BundleState#Installed} extended bundle state.
     * <p>
     * Note that not all of the state and {@code Bundle} information is
     * contained in the {@code FeaturesService}. As such, this method stores
     * some of the required information in the class's {@code #bundleContext}
     * and {@code bundleStateServices}. As such, these objects must be
     * re-instantiated for each test (i.e., they must be instantiated in the
     * {@link #setUp()} method).
     *
     * @param repos                A {@code Set} of {@link Repository} objects from which to
     *                             obtain the {@link Feature}s that are to be included in the
     *                             mock {@code FeaturesService}
     * @param notInstalledFeatures A {@code Set} of {@code Feature}s that the
     *                             {@code FeaturesService} should report as not installed
     * @param inactiveBundles      A {@code Set} of {@link BundleInfo}s containing the locations
     *                             of {@code Bundle}s that should be set to inactive and for
     *                             which the {@link BundleStateService} contained in index 0 of
     *                             {@link #bundleStateServices} should report a
     *                             {@link BundleState#Installed} state.
     * @return A mock {@link FeaturesService} with {@link Feature}s and
     * {@link Bundle}s in the requested states.
     * @throws Exception
     */
    private FeaturesService createMockFeaturesService(Set<Repository> repos,
            Set<Feature> notInstalledFeatures, Set<BundleInfo> inactiveBundles) throws Exception {

        if (logger.isTraceEnabled()) {
            for (Repository repo : repos) {
                for (Feature feature : repo.getFeatures()) {
                    logger.trace("Repo Feature: " + feature);
                    logger.trace("Repo Feature name/version: " + feature.getName() + "/"
                            + feature.getVersion());

                    logger.trace("Dependencies: ");

                    for (Dependency depFeature : feature.getDependencies()) {
                        logger.trace("Dependency Feature: " + depFeature);
                        logger.trace(
                                "Dependency Feature name/version: " + depFeature.getName() + "/"
                                        + depFeature.getVersion());
                    }
                }
            }
        }

        if (null == notInstalledFeatures) {
            notInstalledFeatures = new HashSet<Feature>();
        }

        if (null == inactiveBundles) {
            inactiveBundles = new HashSet<BundleInfo>();
        }

        Set<String> installedBundleLocations = new HashSet<String>();
        for (BundleInfo bundleInfo : inactiveBundles) {
            installedBundleLocations.add(bundleInfo.getLocation());
        }

        FeaturesService featuresService = mock(FeaturesService.class);
        Set<Feature> featuresSet = new HashSet<Feature>();

        BundleRevision mockBundleRevision = mock(BundleRevision.class);
        when(mockBundleRevision.getTypes()).thenReturn(0);

        for (Repository curRepo : repos) {
            for (Feature curFeature : curRepo.getFeatures()) {
                featuresSet.add(curFeature);
                when(featuresService.getFeature(curFeature.getName())).thenReturn(curFeature);
                when(featuresService.getFeature(curFeature.getName(),
                        curFeature.getVersion())).thenReturn(curFeature);

                // TODO: File Karaf bug that necessitates this, then reference
                // it here.
                when(featuresService.getFeature(curFeature.getName(), "0.0.0")).thenReturn(
                        curFeature);

                when(featuresService.isInstalled(curFeature)).thenReturn(!notInstalledFeatures.contains(
                        curFeature));

                // NOTE: The following logic creates a separate Bundle instance
                // for all Bundles in the repository, even if two features
                // refer to the same bundle. If future tests rely on
                // maintaining the same Bundle instance for each reference
                // of that bundle, this logic will need to be modified.
                for (BundleInfo bundleInfo : curFeature.getBundles()) {
                    if (installedBundleLocations.contains(bundleInfo.getLocation())) {

                        Bundle mockInstalledBundle = mock(Bundle.class);
                        when(mockInstalledBundle.getState()).thenReturn(Bundle.INSTALLED);
                        when(mockInstalledBundle.adapt(BundleRevision.class)).thenReturn(
                                mockBundleRevision);

                        when(bundleContext.getBundle(bundleInfo.getLocation())).thenReturn(
                                mockInstalledBundle);
                        when(bundleStateServices.get(0)
                                .getState(mockInstalledBundle)).thenReturn(BundleState.Installed);
                    } else {
                        Bundle mockActiveBundle = mock(Bundle.class);
                        when(mockActiveBundle.getState()).thenReturn(Bundle.ACTIVE);
                        when(mockActiveBundle.adapt(BundleRevision.class)).thenReturn(
                                mockBundleRevision);

                        when(bundleContext.getBundle(bundleInfo.getLocation())).thenReturn(
                                mockActiveBundle);
                        when(bundleStateServices.get(0)
                                .getState(mockActiveBundle)).thenReturn(BundleState.Active);
                    }
                }
            }
        }

        when(featuresService.listRepositories()).thenReturn(repos.toArray(new Repository[repos.size()]));
        when(featuresService.listFeatures()).thenReturn(featuresSet.toArray(new Feature[] {}));

        return featuresService;
    }
}
