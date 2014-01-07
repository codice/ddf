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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.karaf.bundle.core.BundleStateService;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.apache.karaf.features.internal.RepositoryImpl;
import org.codice.ddf.platform.status.Application;
import org.codice.ddf.platform.status.ApplicationService;
import org.codice.ddf.platform.status.ApplicationStatus;
import org.codice.ddf.platform.status.ApplicationStatus.ApplicationState;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.BundleContext;

public class ApplicationServiceImplTest {

    private static Repository repo1, repo2;

    @BeforeClass
    public static void loadRepositories() throws Exception {
        repo1 = createRepo("test-features.xml");
        repo2 = createRepo("test-features2.xml");

    }

    /**
     * Tests that the get application method works and returns back the correct
     * applications.
     *
     * @throws Exception
     */
    @Test
    public void testGetApplications() throws Exception {

        List<BundleStateService> bundleStateServices = new ArrayList<BundleStateService>();
        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(repo1, repo2));
        Set<Repository> inactiveRepos = new HashSet<Repository>();
        ApplicationService appService = new ApplicationServiceImpl(createMockFeaturesService(
                activeRepos, inactiveRepos), mock(BundleContext.class), bundleStateServices);

        Set<Application> applications = appService.getApplications();
        assertNotNull(applications);
        assertEquals(2, applications.size());

    }

    /**
     * Tests the scenario where all of the repositories have their features
     * started.
     *
     * @throws Exception
     */
    @Test
    public void testApplicationAllStarted() throws Exception {
        List<BundleStateService> bundleStateServices = new ArrayList<BundleStateService>();
        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(repo1, repo2));
        Set<Repository> inactiveRepos = new HashSet<Repository>();
        ApplicationService appService = new ApplicationServiceImpl(createMockFeaturesService(
                activeRepos, inactiveRepos), mock(BundleContext.class), bundleStateServices);

        // both started
        Set<Application> applications = appService.getApplications();
        for (Application curApp : applications) {
            assertTrue(appService.isApplicationStarted(curApp));
        }
    }

    /**
     * Tests the scenario where only one (of two) repositories has features
     * started
     *
     * @throws Exception
     */
    @Test
    public void testApplicationOneStarted() throws Exception {
        List<BundleStateService> bundleStateServices = new ArrayList<BundleStateService>();
        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(repo1));
        Set<Repository> inactiveRepos = new HashSet<Repository>(Arrays.asList(repo2));
        ApplicationService appService = new ApplicationServiceImpl(createMockFeaturesService(
                activeRepos, inactiveRepos), mock(BundleContext.class), bundleStateServices);

        // only repo1 started
        Set<Application> applications = appService.getApplications();
        for (Application curApp : applications) {
            if (curApp.getName().equals(repo1.getName())) {
                assertTrue(appService.isApplicationStarted(curApp));
            } else {
                assertFalse(appService.isApplicationStarted(curApp));
            }
        }
    }

    /**
     * Tests the scenario where none of the repositories have features started.
     *
     * @throws Exception
     */
    @Test
    public void testApplicationNoneStarted() throws Exception {
        List<BundleStateService> bundleStateServices = new ArrayList<BundleStateService>();
        Set<Repository> activeRepos = new HashSet<Repository>();
        Set<Repository> inactiveRepos = new HashSet<Repository>(Arrays.asList(repo1, repo2));
        ApplicationService appService = new ApplicationServiceImpl(createMockFeaturesService(
                activeRepos, inactiveRepos), mock(BundleContext.class), bundleStateServices);

        // both NOT started
        Set<Application> applications = appService.getApplications();
        for (Application curApp : applications) {
            assertFalse(appService.isApplicationStarted(curApp));
        }
    }

    /**
     * Tests receiving application status for an application in the ACTIVE
     * state.
     */
    @Test
    public void testGetActiveApplicationStatus() throws Exception {
        List<BundleStateService> bundleStateServices = new ArrayList<BundleStateService>();
        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(repo1));
        Set<Repository> inactiveRepos = new HashSet<Repository>();
        ApplicationService appService = new ApplicationServiceImpl(createMockFeaturesService(
                activeRepos, inactiveRepos), mock(BundleContext.class), bundleStateServices);

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
     * Tests receiving application status for an application in the FAILURE
     * state.
     */
    @Test
    public void testGetInactiveApplicationStatus() throws Exception {
        List<BundleStateService> bundleStateServices = new ArrayList<BundleStateService>();
        Set<Repository> activeRepos = new HashSet<Repository>();
        Set<Repository> inactiveRepos = new HashSet<Repository>(Arrays.asList(repo1));
        ApplicationService appService = new ApplicationServiceImpl(createMockFeaturesService(
                activeRepos, inactiveRepos), mock(BundleContext.class), bundleStateServices);

        Set<Application> applications = appService.getApplications();
        assertEquals(1, applications.size());
        for (Application curApp : applications) {
            ApplicationStatus status = appService.getApplicationStatus(curApp);
            assertEquals(curApp, status.getApplication());
            assertEquals(ApplicationState.INACTIVE, status.getState());
            assertTrue(status.getErrorBundles().isEmpty());
            assertFalse(status.getErrorFeatures().isEmpty());
        }

    }

    /**
     * Tests that the service properly parses incoming 'ignored' application
     * names and returns them as a set.
     *
     * @throws Exception
     */
    @Test
    public void testGetIgnoreApplications() throws Exception {
        List<BundleStateService> bundleStateServices = new ArrayList<BundleStateService>();
        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(repo1, repo2));
        Set<Repository> inactiveRepos = new HashSet<Repository>();
        ApplicationServiceImpl appService = new ApplicationServiceImpl(createMockFeaturesService(
                activeRepos, inactiveRepos), mock(BundleContext.class), bundleStateServices);

        // test 1 name
        appService.setIgnoredApplications(repo1.getName());
        assertEquals(1, appService.getIgnoredApplicationNames().size());
        assertEquals(repo1.getName(), appService.getIgnoredApplicationNames().iterator().next());

        // test 2 names
        appService.setIgnoredApplications(repo1.getName() + "," + repo2.getName());
        assertEquals(2, appService.getIgnoredApplicationNames().size());

        // test 1 name duplicated
        appService.setIgnoredApplications(repo1.getName() + "," + repo1.getName());
        assertEquals(1, appService.getIgnoredApplicationNames().size());

    }

    /**
     * Tests that the service properly ignores applications when checking for
     * application status.
     *
     * @throws Exception
     */
    @Test
    public void testIgnoreApplications() throws Exception {
        List<BundleStateService> bundleStateServices = new ArrayList<BundleStateService>();
        Set<Repository> activeRepos = new HashSet<Repository>(Arrays.asList(repo1, repo2));
        Set<Repository> inactiveRepos = new HashSet<Repository>();
        ApplicationServiceImpl appService = new ApplicationServiceImpl(createMockFeaturesService(
                activeRepos, inactiveRepos), mock(BundleContext.class), bundleStateServices);

        // just ignore the first application
        appService.setIgnoredApplications(repo1.getName());
        Set<Application> applications = appService.getApplications();
        assertNotNull(applications);
        assertEquals(1, applications.size());
        assertEquals(repo2.getName(), applications.iterator().next().getName());
        // now ignore both applications
        appService.setIgnoredApplications(repo1.getName() + "," + repo2.getName());
        applications = appService.getApplications();
        assertNotNull(applications);
        assertEquals(0, applications.size());
        // ignore none
        appService.setIgnoredApplications("");
        applications = appService.getApplications();
        assertNotNull(applications);
        assertEquals(2, applications.size());
    }

    private static Repository createRepo(String featuresFile) throws Exception {
        RepositoryImpl repo = new RepositoryImpl(ApplicationServiceImplTest.class.getClassLoader()
                .getResource(featuresFile).toURI());
        repo.load();

        return repo;
    }

    /**
     * Mocks a features service. Configurable so that the mock service will
     * return different status based on the repositories that are passed in.
     *
     * @param activeRepos
     *            Repositories that should have all of their features started.
     * @param inactiveRepos
     *            Repositories that should have all of their features NOT
     *            started.
     * @return
     * @throws Exception
     */
    private FeaturesService createMockFeaturesService(Set<Repository> activeRepos,
            Set<Repository> inactiveRepos) throws Exception {
        List<Repository> returnRepos = new ArrayList<Repository>(activeRepos.size()
                + inactiveRepos.size());
        FeaturesService featuresService = mock(FeaturesService.class);

        // mock active repos
        for (Repository curRepo : activeRepos) {
            Feature[] features = curRepo.getFeatures();
            for (Feature curFeature : features) {
                when(featuresService.getFeature(curFeature.getName(), curFeature.getVersion()))
                        .thenReturn(curFeature);
                when(featuresService.isInstalled(curFeature)).thenReturn(Boolean.TRUE);
            }
        }
        returnRepos.addAll(activeRepos);

        // mock inactive repos
        for (Repository curRepo : inactiveRepos) {
            Feature[] features = curRepo.getFeatures();
            for (Feature curFeature : features) {
                when(featuresService.getFeature(curFeature.getName(), curFeature.getVersion()))
                        .thenReturn(curFeature);
                when(featuresService.isInstalled(curFeature)).thenReturn(Boolean.FALSE);
            }
        }
        returnRepos.addAll(inactiveRepos);

        when(featuresService.listRepositories()).thenReturn(
                returnRepos.toArray(new Repository[returnRepos.size()]));
        return featuresService;
    }

}
