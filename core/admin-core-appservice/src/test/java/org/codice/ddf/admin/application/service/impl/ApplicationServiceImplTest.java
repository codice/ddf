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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.karaf.bundle.core.BundleState;
import org.apache.karaf.bundle.core.BundleStateService;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.apache.karaf.features.internal.RepositoryImpl;
import org.codice.ddf.admin.application.service.Application;
import org.codice.ddf.admin.application.service.ApplicationNode;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.codice.ddf.admin.application.service.ApplicationStatus;
import org.codice.ddf.admin.application.service.ApplicationStatus.ApplicationState;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleRevision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationServiceImplTest {

    private Logger logger = LoggerFactory.getLogger(ApplicationServiceImplTest.class);

    private BundleContext bundleContext;

    private ServiceReference<FeaturesService> mockFeatureRef;

    private static final String TEST_NO_MAIN_FEATURE_1_FILE_NAME = "test-features-no-main-feature.xml";

    private static final String TEST_MAIN_FEATURES_1_MAIN_FEATURE_NAME = "main-feature";

    private static final String TEST_MAIN_FEATURES_1_FEATURE_1_NAME = "test-feature-1";

    // Must be a bundle that is defined in only one feature of the features file
    // under test.
    private static final String TEST_MAIN_FEATURES_1_FEATURE_1_UNIQUE_BUNDLE_LOCATION = "mvn:org.codice.test/codice.test.bundle2/2.0.0";

    private static final String TEST_NO_MAIN_FEATURE_2_FILE_NAME = "test-features-no-main-feature2.xml";

    private static final String TEST_MAIN_FEATURE_1_FILE_NAME = "test-features-with-main-feature.xml";

    private static final String TEST_MAIN_FEATURE_2_FILE_NAME = "test-features-with-main-feature2.xml";

    private static Repository noMainFeatureRepo1, noMainFeatureRepo2, mainFeatureRepo;

    private static List<BundleStateService> bundleStateServices;

    /**
     * Creates default {@link BundleContext}, {@code List} of
     * {@code BundleStateService}s, and {@link Repository} objects for use in
     * the tests.
     * 
     * NOTE: These must be in {@code setUp()} method rather than a
     * {@code beforeClass()} method because they are modified by individual
     * tests as part of the setup for individual test conditions. @see
     * {@link #createMockFeaturesService(Set, Set, Set)}
     * 
     * @throws java.lang.Exception
     */
    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        // Recreate the repos and BuncleContext prior to each test in order to
        // ensure modifications made in one test do not effect another test.
        noMainFeatureRepo1 = createRepo(TEST_NO_MAIN_FEATURE_1_FILE_NAME);
        noMainFeatureRepo2 = createRepo(TEST_NO_MAIN_FEATURE_2_FILE_NAME);
        mainFeatureRepo = createRepo(TEST_MAIN_FEATURE_1_FILE_NAME);
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
        ApplicationService appService = new ApplicationServiceImpl(

        bundleContext, bundleStateServices);

        Set<Application> applications = appService.getApplications();
        assertNotNull(applications);
        assertEquals(2, applications.size());
    }

    /**
     * Test method for
     * {@link ApplicationService#getApplicationStatus(Application)}
     * 
     * Verifies method returns an {@link ApplicationState#ACTIVE} state for an
     * Application under the following conditions:
     * 
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
        ApplicationService appService = new ApplicationServiceImpl(bundleContext,
                bundleStateServices);

        assertEquals(ApplicationService.class.getName()
                + " does not contain the expected number of Applications", 1, appService
                .getApplications().size());

        assertEquals(
                mainFeatureRepo.getName() + " returned unexpected state",
                ApplicationState.ACTIVE,
                appService.getApplicationStatus(
                        appService.getApplications().toArray(new Application[] {})[0]).getState());
    }

    /**
     * Test method for
     * {@link ApplicationServiceImpl#getApplicationStatus(Application)}
     * 
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
                mainFeatureRepo, BundleState.Unknown);
        assertNotNull("Repository \"" + mainFeatureRepo.getName()
                + "\" does not contain any bundles", appService);

        assertEquals(
                mainFeatureRepo.getName() + " returned unexpected state",
                ApplicationState.ACTIVE,
                appService.getApplicationStatus(
                        appService.getApplications().toArray(new Application[] {})[0]).getState());
    }

    /**
     * Test method for
     * {@link ApplicationServiceImpl#getApplicationStatus(Application)}
     * 
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
                notInstalledFeatures, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);

        ApplicationService appService = new ApplicationServiceImpl(

        bundleContext, bundleStateServices);

        assertEquals("More than one application was returned from mainFeatureRepo", 1, appService
                .getApplications().size());

        assertEquals(
                "mainFeatureRepo returned unexpected state",
                ApplicationState.INACTIVE,
                appService.getApplicationStatus(
                        appService.getApplications().toArray(new Application[] {})[0]).getState());

    }

    /**
     * Test method for
     * {@link ApplicationService#getApplicationStatus(Application)}
     * 
     * Verifies method returns an {@link ApplicationState#INACTIVE} state for an
     * {@code Application} under the following conditions:
     * 
     * <ul>
     * <li>Main feature is installed</li>
     * <li>One dependency feature is NOT installed</li>
     * <li>Dependency feature that is not installed contains a {@code Bundle}
     * with a state of {@link Bundle#ACTIVE} and extended state of
     * {@link BundleState#Active}</li>
     * </ul>
     * 
     * This effectively emulates the circumstance in which there is a
     * {@code Feature} in another {@code Application} that includes and starts
     * the same {@code Bundle} that is contained in a {@code Feature} of the
     * current {@code Application} that is not installed.
     * 
     * @throws Exception
     */
    @Test
    public void testGetApplicationStatusReturnsInactiveStatusForNotInstalledFeatureDependencyThatContainsActiveBundle()
            throws Exception {

        Set<String> notInstalledFeatureNames = new HashSet<String>();
        notInstalledFeatureNames.add(TEST_MAIN_FEATURES_1_FEATURE_1_NAME);

        FeaturesService featuresService = createMockFeaturesService(mainFeatureRepo,
                notInstalledFeatureNames, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);

        assertNotNull("Features repo is missing feature with the name of \""
                + TEST_MAIN_FEATURES_1_FEATURE_1_NAME + "\"",
                featuresService.getFeature(TEST_MAIN_FEATURES_1_FEATURE_1_NAME));

        ApplicationService appService = new ApplicationServiceImpl(bundleContext,
                bundleStateServices);

        assertEquals(ApplicationService.class.getName()
                + " does not contain the expected number of Applications", 1, appService
                .getApplications().size());

        assertEquals(
                mainFeatureRepo.getName() + " returned unexpected state",
                ApplicationState.INACTIVE,
                appService.getApplicationStatus(
                        appService.getApplications().toArray(new Application[] {})[0]).getState());
    }

    /**
     * Test method for
     * {@link ApplicationServiceImpl#getApplicationStatus(Application)}
     * 
     * Verifies that {@link ApplicationState#INACTIVE} is returned when
     * {@code Bundle} state is {@link Bundle#RESOLVED}
     * 
     * @throws Exception
     */
    @Test
    public void testGetApplicationStatusReturnsInactiveWhenBundleStateIsResolved() throws Exception {
        ApplicationService appService = getAppServiceWithBundleInGivenState(mainFeatureRepo,
                Bundle.RESOLVED);

        assertEquals(
                mainFeatureRepo.getName() + " returned unexpected state",
                ApplicationState.INACTIVE,
                appService.getApplicationStatus(
                        appService.getApplications().toArray(new Application[] {})[0]).getState());
    }

    /**
     * Test method for
     * {@link ApplicationServiceImpl#getApplicationStatus(Application)}
     * 
     * Verifies that {@link ApplicationState#INACTIVE} is returned when
     * {@code Bundle} state is {@link Bundle#STARTING}
     * 
     * @throws Exception
     */
    @Test
    public void testGetApplicationStatusReturnsInactiveWhenBundleStateIsStarting() throws Exception {

        ApplicationService appService = getAppServiceWithBundleInGivenState(mainFeatureRepo,
                Bundle.STARTING);

        assertEquals(
                mainFeatureRepo.getName() + " returned unexpected state",
                ApplicationState.INACTIVE,
                appService.getApplicationStatus(
                        appService.getApplications().toArray(new Application[] {})[0]).getState());
    }

    /**
     * Test method for
     * {@link ApplicationServiceImpl#getApplicationStatus(Application)}
     * 
     * Verifies that {@link ApplicationState#INACTIVE} is returned when
     * {@code Bundle} state is {@link Bundle#STOPPING}
     * 
     * @throws Exception
     */
    @Test
    public void testGetApplicationStatusReturnsInactiveWhenBundleStateIsStopping() throws Exception {
        ApplicationService appService = getAppServiceWithBundleInGivenState(mainFeatureRepo,
                Bundle.STOPPING);

        assertEquals(
                mainFeatureRepo.getName() + " returned unexpected state",
                ApplicationState.INACTIVE,
                appService.getApplicationStatus(
                        appService.getApplications().toArray(new Application[] {})[0]).getState());
    }

    /**
     * Test method for
     * {@link ApplicationServiceImpl#getApplicationStatus(Application)}
     * 
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
                mainFeatureRepo, BundleState.Resolved);
        assertNotNull("Repository \"" + mainFeatureRepo.getName()
                + "\" does not contain any bundles", appService);

        assertEquals(
                mainFeatureRepo.getName() + " returned unexpected state",
                ApplicationState.INACTIVE,
                appService.getApplicationStatus(
                        appService.getApplications().toArray(new Application[] {})[0]).getState());
    }

    /**
     * Test method for
     * {@link ApplicationServiceImpl#getApplicationStatus(Application)}
     * 
     * Verifies that {@link ApplicationState#INACTIVE} is returned when the
     * extended bundle state reported by an injection framework states that one
     * bundle is in an {@link BundleState#Waiting} state and the rest of the
     * bundles are in an {@link BundleState#Active} state.
     * 
     * @throws Exception
     */
    @Test
    public void testGetApplicationStatusReturnsInactiveWhenBundleStateServiceStateIsWaiting()
            throws Exception {

        ApplicationService appService = getAppServiceWithBundleStateServiceInGivenState(
                mainFeatureRepo, BundleState.Waiting);
        assertNotNull("Repository \"" + mainFeatureRepo.getName()
                + "\" does not contain any bundles", appService);

        assertEquals(
                mainFeatureRepo.getName() + " returned unexpected state",
                ApplicationState.INACTIVE,
                appService.getApplicationStatus(
                        appService.getApplications().toArray(new Application[] {})[0]).getState());
    }

    /**
     * Test method for
     * {@link ApplicationServiceImpl#getApplicationStatus(Application)}
     * 
     * Verifies that {@link ApplicationState#INACTIVE} is returned when the
     * extended bundle state reported by an injection framework states that one
     * bundle is in an {@link BundleState#Starting} state and the rest of the
     * bundles are in an {@link BundleState#Active} state.
     * 
     * @throws Exception
     */
    @Test
    public void testGetApplicationStatusReturnsInactiveWhenBundleStateServiceStateIsStarting()
            throws Exception {
        ApplicationService appService = getAppServiceWithBundleStateServiceInGivenState(
                mainFeatureRepo, BundleState.Starting);
        assertNotNull("Repository \"" + mainFeatureRepo.getName()
                + "\" does not contain any bundles", appService);

        assertEquals(
                mainFeatureRepo.getName() + " returned unexpected state",
                ApplicationState.INACTIVE,
                appService.getApplicationStatus(
                        appService.getApplications().toArray(new Application[] {})[0]).getState());
    }

    /**
     * Test method for
     * {@link ApplicationServiceImpl#getApplicationStatus(Application)}
     * 
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
                mainFeatureRepo, BundleState.Stopping);
        assertNotNull("Repository \"" + mainFeatureRepo.getName()
                + "\" does not contain any bundles", appService);

        assertEquals(
                mainFeatureRepo.getName() + " returned unexpected state",
                ApplicationState.INACTIVE,
                appService.getApplicationStatus(
                        appService.getApplications().toArray(new Application[] {})[0]).getState());
    }

    /**
     * Test method for
     * {@link ApplicationService#getApplicationStatus(Application)}
     * 
     * Verifies method returns an {@link ApplicationState#FAILED } state for an
     * {@code Application} under the following conditions:
     * 
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
        for (Feature feature : mainFeatureRepo.getFeatures()) {
            for (BundleInfo bundleInfo : feature.getBundles()) {
                if (bundleInfo.getLocation().equals(
                        TEST_MAIN_FEATURES_1_FEATURE_1_UNIQUE_BUNDLE_LOCATION)) {
                    ++bundleInclusionCount;
                }
            }
        }
        assertEquals("Bundle is not included in repository the expected number of times", 1,
                bundleInclusionCount);

        // Execute test
        Set<String> inactiveBundleNames = new HashSet<String>();
        inactiveBundleNames.add(TEST_MAIN_FEATURES_1_FEATURE_1_UNIQUE_BUNDLE_LOCATION);

        FeaturesService featuresService = createMockFeaturesService(mainFeatureRepo, null,
                inactiveBundleNames);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);

        ApplicationService appService = new ApplicationServiceImpl(bundleContext,
                bundleStateServices);

        assertEquals(ApplicationService.class.getName()
                + " does not contain the expected number of Applications", 1, appService
                .getApplications().size());

        assertEquals(
                mainFeatureRepo.getName() + " returned unexpected state",
                ApplicationState.FAILED,
                appService.getApplicationStatus(
                        appService.getApplications().toArray(new Application[] {})[0]).getState());
    }

    /**
     * Test method for
     * {@link ApplicationService#getApplicationStatus(Application)}
     * 
     * Verifies method returns an {@link ApplicationState#FAILED } state for an
     * {@code Application} under the following conditions:
     * 
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

        for (Feature feature : mainFeatureRepo.getFeatures()) {
            if (feature.getName().equals(TEST_MAIN_FEATURES_1_FEATURE_1_NAME)) {
                notInstalledFeatures.add(feature);
                inactiveBundles.addAll(feature.getBundles());
                break;
            }
        }

        assertEquals("Feature is not included in repository the expected number of times", 1,
                notInstalledFeatures.size());
        assertTrue("No bundles included in Feature", inactiveBundles.size() > 0);

        mainFeaturesRepoSet.add(mainFeatureRepo);

        FeaturesService featuresService = createMockFeaturesService(mainFeaturesRepoSet,
                notInstalledFeatures, inactiveBundles);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);

        ApplicationService appService = new ApplicationServiceImpl(bundleContext,
                bundleStateServices);

        assertEquals(ApplicationService.class.getName()
                + " does not contain the expected number of Applications", 1, appService
                .getApplications().size());

        assertEquals(
                mainFeatureRepo.getName() + " returned unexpected state",
                ApplicationState.FAILED,
                appService.getApplicationStatus(
                        appService.getApplications().toArray(new Application[] {})[0]).getState());
    }

    /**
     * Test method for
     * {@link ApplicationServiceImpl#getApplicationStatus(Application)}
     * 
     * Verifies that {@link ApplicationState#INACTIVE} is returned when
     * {@code Bundle} state is {@link Bundle#INSTALLED}
     * 
     * @throws Exception
     */
    @Test
    public void testGetApplicationStatusReturnsInactiveWhenBundleStateIsInstalled()
            throws Exception {
        ApplicationService appService = getAppServiceWithBundleInGivenState(mainFeatureRepo,
                Bundle.INSTALLED);

        assertEquals(
                mainFeatureRepo.getName() + " returned unexpected state",
                ApplicationState.FAILED,
                appService.getApplicationStatus(
                        appService.getApplications().toArray(new Application[] {})[0]).getState());
    }

    /**
     * Test method for
     * {@link ApplicationServiceImpl#getApplicationStatus(Application)}
     * 
     * Verifies that {@link ApplicationState#INACTIVE} is returned when
     * {@code Bundle} state is {@link Bundle#UNINSTALLED}
     * 
     * @throws Exception
     */
    @Test
    public void testGetApplicationStatusReturnsInactiveWhenBundleStateIsUninnstalled()
            throws Exception {
        ApplicationService appService = getAppServiceWithBundleInGivenState(mainFeatureRepo,
                Bundle.UNINSTALLED);

        assertEquals(
                mainFeatureRepo.getName() + " returned unexpected state",
                ApplicationState.FAILED,
                appService.getApplicationStatus(
                        appService.getApplications().toArray(new Application[] {})[0]).getState());
    }

    /**
     * Test method for
     * {@link ApplicationServiceImpl#getApplicationStatus(Application)}
     * 
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
                mainFeatureRepo, BundleState.Installed);
        assertNotNull("Repository \"" + mainFeatureRepo.getName()
                + "\" does not contain any bundles", appService);

        assertEquals(
                mainFeatureRepo.getName() + " returned unexpected state",
                ApplicationState.FAILED,
                appService.getApplicationStatus(
                        appService.getApplications().toArray(new Application[] {})[0]).getState());
    }

    /**
     * Test method for
     * {@link ApplicationServiceImpl#getApplicationStatus(Application)}
     * 
     * Verifies that {@link ApplicationState#FAILED} is returned when the
     * extended bundle state reported by an injection framework states that one
     * bundle is in an {@link BundleState#GracePeriod} state and the rest of the
     * bundles are in an {@link BundleState#Active} state.
     * 
     * @throws Exception
     */
    @Test
    public void testGetApplicationStatusReturnsFailedWhenBundleStateServiceStateIsGracePeriod()
            throws Exception {
        ApplicationService appService = getAppServiceWithBundleStateServiceInGivenState(
                mainFeatureRepo, BundleState.GracePeriod);
        assertNotNull("Repository \"" + mainFeatureRepo.getName()
                + "\" does not contain any bundles", appService);

        assertEquals(
                mainFeatureRepo.getName() + " returned unexpected state",
                ApplicationState.FAILED,
                appService.getApplicationStatus(
                        appService.getApplications().toArray(new Application[] {})[0]).getState());
    }

    /**
     * Test method for
     * {@link ApplicationServiceImpl#getApplicationStatus(Application)}
     * 
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
                mainFeatureRepo, BundleState.Failure);
        assertNotNull("Repository \"" + mainFeatureRepo.getName()
                + "\" does not contain any bundles", appService);

        assertEquals(
                mainFeatureRepo.getName() + " returned unexpected state",
                ApplicationState.FAILED,
                appService.getApplicationStatus(
                        appService.getApplications().toArray(new Application[] {})[0]).getState());
    }

    /**
     * Test method for
     * {@link ApplicationServiceImpl#getApplicationStatus(Application)}
     * 
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
        FeaturesService featuresService = createMockFeaturesService(mainFeatureRepo, null, null);
        Set<Bundle> bundleSet = getXBundlesFromFeaturesService(featuresService, 2);
        assertNotNull(mainFeatureRepo.getName() + " does not contain 2 bundles", bundleSet);
        Bundle[] bundles = bundleSet.toArray(new Bundle[] {});

        when(bundleStateServices.get(0).getState(bundles[0])).thenReturn(BundleState.Resolved);
        when(bundleStateServices.get(0).getState(bundles[1])).thenReturn(BundleState.Failure);

        ApplicationService appService = getAppServiceWithBundleStateServiceInGivenState(
                mainFeatureRepo, BundleState.Failure);
        assertNotNull("Repository \"" + mainFeatureRepo.getName()
                + "\" does not contain any bundles", appService);

        assertEquals(
                mainFeatureRepo.getName() + " returned unexpected state",
                ApplicationState.FAILED,
                appService.getApplicationStatus(
                        appService.getApplications().toArray(new Application[] {})[0]).getState());
    }

    /**
     * Test method for
     * {@link ApplicationServiceImpl#isApplicationStarted(Application)}
     * 
     * Verifies that method returns true when application state is
     * {@link ApplicationState#ACTIVE}
     * 
     * @throws Exception
     */
    @Test
    public void testIsApplicationStartedReturnsTrueForActiveApplicationState() throws Exception {

        FeaturesService featuresService = createMockFeaturesService(mainFeatureRepo, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);

        ApplicationService appService = new ApplicationServiceImpl(

        bundleContext, bundleStateServices);

        assertTrue(appService.isApplicationStarted(appService
                .getApplication(TEST_MAIN_FEATURES_1_MAIN_FEATURE_NAME)));
    }

    /**
     * Test method for
     * {@link ApplicationServiceImpl#isApplicationStarted(Application)}
     * 
     * Verifies that method returns false when application state is
     * {@link ApplicationState#INACTIVE}
     * 
     * @throws Exception
     */
    @Test
    public void testIsApplicationStartedReturnsFalseForInactiveApplicationState() throws Exception {

        Set<String> notInstalledFeatures = new HashSet<String>();
        notInstalledFeatures.add(TEST_MAIN_FEATURES_1_FEATURE_1_NAME);

        FeaturesService featuresService = createMockFeaturesService(mainFeatureRepo,
                notInstalledFeatures, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);

        ApplicationService appService = new ApplicationServiceImpl(

        bundleContext, bundleStateServices);

        assertFalse(appService.isApplicationStarted(appService
                .getApplication(TEST_MAIN_FEATURES_1_MAIN_FEATURE_NAME)));
    }

    /**
     * Test method for
     * {@link ApplicationServiceImpl#isApplicationStarted(Application)}
     * 
     * Verifies that method returns false when application state is
     * {@link ApplicationState#FAILED}
     * 
     * @throws Exception
     */
    @Test
    public void testIsApplicationStartedReturnsFalseForFailedApplicationState() throws Exception {

        Set<String> inactiveBundles = new HashSet<String>();
        inactiveBundles.add(TEST_MAIN_FEATURES_1_FEATURE_1_UNIQUE_BUNDLE_LOCATION);

        FeaturesService featuresService = createMockFeaturesService(mainFeatureRepo, null,
                inactiveBundles);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);

        ApplicationService appService = new ApplicationServiceImpl(

        bundleContext, bundleStateServices);

        assertFalse(appService.isApplicationStarted(appService
                .getApplication(TEST_MAIN_FEATURES_1_MAIN_FEATURE_NAME)));
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

        ApplicationService appService = new ApplicationServiceImpl(

        bundleContext, bundleStateServices);

        Set<Application> applications = appService.getApplications();
        assertEquals(1, applications.size());
        for (Application curApp : applications) {
            ApplicationStatus status = appService.getApplicationStatus(curApp);
            assertEquals(curApp, status.getApplication());
            assertEquals(ApplicationState.ACTIVE, status.getState());
            assertTrue(status.getErrorBundles().isEmpty());
            assertTrue(status.getErrorFeatures().isEmpty());
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

        ApplicationServiceImpl appService = new ApplicationServiceImpl(

        bundleContext, bundleStateServices);

        // just ignore the first application
        appService.setIgnoredApplications(noMainFeatureRepo1.getName());
        Set<Application> applications = appService.getApplications();
        assertNotNull(applications);
        assertEquals(1, applications.size());
        assertEquals(noMainFeatureRepo2.getName(), applications.iterator().next().getName());

        // now ignore both applications
        appService.setIgnoredApplications(noMainFeatureRepo1.getName() + ","
                + noMainFeatureRepo2.getName());
        applications = appService.getApplications();
        assertNotNull(applications);
        assertEquals(0, applications.size());

        // ignore none
        appService.setIgnoredApplications("");
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
        Repository mainFeatureRepo2 = createRepo(TEST_MAIN_FEATURE_2_FILE_NAME);

        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(mainFeatureRepo,
                mainFeatureRepo2));

        FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);

        ApplicationServiceImpl appService = new ApplicationServiceImpl(

        bundleContext, bundleStateServices);

        Set<ApplicationNode> rootApps = appService.getApplicationTree();

        assertNotNull(rootApps);
        assertEquals(1, rootApps.size());

        ApplicationNode mainAppNode = rootApps.iterator().next();
        assertEquals(1, mainAppNode.getChildren().size());
        assertNull(mainAppNode.getParent());
        assertEquals("main-feature2", mainAppNode.getChildren().iterator().next().getApplication()
                .getName());
        assertEquals(mainAppNode, mainAppNode.getChildren().iterator().next().getParent());

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
                mainFeaturesRepo2, noMainFeatureRepo1));

        FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);

        ApplicationServiceImpl appService = new ApplicationServiceImpl(

        bundleContext, bundleStateServices);

        Set<ApplicationNode> rootApps = appService.getApplicationTree();

        assertNotNull(rootApps);
        assertEquals(2, rootApps.size());
    }

    /**
     * Returns an {@code ApplicationService} that contains one bundle in the
     * received bundle state and the rest of the bundles in an {code
     * Bundle#ACTIVE} state.
     * 
     * @param repository
     *            The {@link Repository} from which to build the
     *            {@code ApplicationService}.
     * 
     * @param bundleState
     *            The state, as defined in the {@link Bundle} interface, to set
     *            for one of the {@code Bundle}s in the
     *            {@code ApplicationService}
     * 
     * @return An {@link ApplicationService} with one bundle set to the received
     *         bundle state and the rest of the {@code Bundle}s set to the
     *         {@link Bundle#ACTIVE} state
     * 
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
            appService = new ApplicationServiceImpl(bundleContext, bundleStateServices);
        }

        return appService;
    }

    /**
     * Returns an {@code ApplicationService} that contains one bundle for which
     * the extended bundle state reported by an injection framework is the
     * received {@code BundleState} and the extended bundle state for the rest
     * of the bundles in {@code BundleState#Active}.
     * 
     * @param repository
     *            The {@link Repository} from which to build the
     *            {@code ApplicationService}.
     * 
     * @param bundleState
     *            The {@link BundleState} to set for one of the {@code Bundle}s
     *            in the {@code ApplicationService}
     * 
     * @return An {@code ApplicationService} with one bundle's extended state
     *         set to the received bundle state and the rest of the
     *         {@code Bundle}s set to {@code Bundle#ACTIVE}
     * 
     * @throws Exception
     */
    private ApplicationService getAppServiceWithBundleStateServiceInGivenState(
            Repository repository, BundleState bundleState) throws Exception {
        ApplicationService appService = null;

        FeaturesService featuresService = createMockFeaturesService(repository, null, null);
        when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);
        Bundle bundle = getAnyBundleFromFeaturesService(featuresService);

        if (null != bundle) {
            when(bundleStateServices.get(0).getState(bundle)).thenReturn(bundleState);

            appService = new ApplicationServiceImpl(bundleContext, bundleStateServices);
        }

        return appService;
    }

    /**
     * Retrieves a {@code Bundle} from the received {@code FeaturesService}
     * 
     * @param featuresService
     *            The {@link FeaturesService} from which to obtain a
     *            {@code Bundle}
     * 
     * @return A {@link Bundle} from the received {@code FeaturesService} or
     *         <code>null</code> if the {@code FeaturesService} does not contain
     *         any {@code Bundle}s
     * 
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
     * @param featuresService
     *            The {@link FeaturesService} from which to obtain a
     *            {@code Bundle}
     * 
     * @param numBundles
     *            The number of bundles to be retrieved from the
     *            {@code FeaturesService}
     * 
     * @return A {@code Set} containing the requested number of {@link Bundle}s
     *         from the received {@code FeaturesService} or <code>null</code> if
     *         the {@code FeaturesService} does not contain the requested number
     *         of {@code Bundle}s
     * 
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
                bundle = bundleContext.getBundle(bundleInfos.get(ii).getLocation());

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
     * Creates a {@code Repository} from a features.xml file
     * 
     * @param featuresFile
     *            The features.xml file from which to create a
     *            {@code Repository}
     * 
     * @return A {@link Repository} created from the received features.xml file
     * 
     * @throws Exception
     */
    private static Repository createRepo(String featuresFile) throws Exception {
        RepositoryImpl repo = new RepositoryImpl(ApplicationServiceImplTest.class.getClassLoader()
                .getResource(featuresFile).toURI());
        repo.load();

        return repo;
    }

    /**
     * Creates a mock {@code FeaturesService} object consisting of all of the
     * features contained in a {@code Repository} object.
     * 
     * @see #createMockFeaturesService(Set, Set, Set) for additional details
     * 
     * @param repo
     * @param notInstalledFeatureNames
     * @param inactiveBundleLocations
     * @return
     * @throws Exception
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
     * 
     * Note that not all of the state and {@code Bundle} information is
     * contained in the {@code FeaturesService}. As such, this method stores
     * some of the required information in the class's {@code #bundleContext}
     * and {@code bundleStateServices}. As such, these objects must be
     * re-instantiated for each test (i.e., they must be instantiated in the
     * {@link #setUp()} method).
     * 
     * @param repos
     *            A {@code Set} of {@link Repository} objects from which to
     *            obtain the {@link Feature}s that are to be included in the
     *            mock {@code FeaturesService}
     * 
     * @param notInstalledFeatures
     *            A {@code Set} of {@code Feature}s that the
     *            {@code FeaturesService} should report as not installed
     * 
     * @param inactiveBundles
     *            A {@code Set} of {@link BundleInfo}s containing the locations
     *            of {@code Bundle}s that should be set to inactive and for
     *            which the {@link BundleStateService} contained in index 0 of
     *            {@link #bundleStateServices} should report a
     *            {@link BundleState#Installed} state.
     * 
     * @return A mock {@link FeaturesService} with {@link Feature}s and
     *         {@link Bundle}s in the requested states.
     * 
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

                    for (Feature depFeature : feature.getDependencies()) {
                        logger.trace("Dependency Feature: " + depFeature);
                        logger.trace("Dependency Feature name/version: " + depFeature.getName()
                                + "/" + depFeature.getVersion());
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
                when(featuresService.getFeature(curFeature.getName(), curFeature.getVersion()))
                        .thenReturn(curFeature);

                // TODO: File Karaf bug that necessitates this, then reference
                // it here.
                when(featuresService.getFeature(curFeature.getName(), "0.0.0")).thenReturn(
                        curFeature);

                when(featuresService.isInstalled(curFeature)).thenReturn(
                        !notInstalledFeatures.contains(curFeature));

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
                        when(bundleStateServices.get(0).getState(mockInstalledBundle)).thenReturn(
                                BundleState.Installed);
                    } else {
                        Bundle mockActiveBundle = mock(Bundle.class);
                        when(mockActiveBundle.getState()).thenReturn(Bundle.ACTIVE);
                        when(mockActiveBundle.adapt(BundleRevision.class)).thenReturn(
                                mockBundleRevision);

                        when(bundleContext.getBundle(bundleInfo.getLocation())).thenReturn(
                                mockActiveBundle);
                        when(bundleStateServices.get(0).getState(mockActiveBundle)).thenReturn(
                                BundleState.Active);
                    }
                }
            }
        }

        when(featuresService.listRepositories()).thenReturn(
                repos.toArray(new Repository[repos.size()]));
        when(featuresService.listFeatures()).thenReturn(featuresSet.toArray(new Feature[] {}));

        return featuresService;
    }
}