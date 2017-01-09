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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Arrays;
import java.util.Set;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.Repository;
import org.apache.karaf.features.internal.service.RepositoryImpl;
import org.codice.ddf.admin.application.service.Application;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests out the ApplicationImpl code to make sure it is following the interface
 * specification.
 */
public class ApplicationImplTest {

    private static final String FILE_NO_MAIN_FEATURES = "test-features-no-main-feature.xml";

    private static final String FILE_MAIN_FEATURE = "test-features-with-main-feature.xml";

    private static final String MAIN_FEATURE_NAME = "Main Feature Test";

    private static final String TEST_APP = "test-app";

    /**
     * number of non-duplicate bundles in the feature file
     */
    private static final int NUM_BUNDLES = 5;

    /**
     * Verify that the application is properly exposing the underlying
     * repository.
     *
     * @throws Exception
     */
    @Test
    public void testAppGetters() throws Exception {
        RepositoryImpl repo = new RepositoryImpl(getClass().getClassLoader()
                .getResource(FILE_NO_MAIN_FEATURES)
                .toURI());
        repo.load();
        Application testApp = new ApplicationImpl(repo);

        assertEquals(TEST_APP, testApp.getName());

        Set<Feature> appFeatures = testApp.getFeatures();
        assertNotNull(appFeatures);
        assertEquals(repo.getFeatures().length, appFeatures.size());
        assertTrue(appFeatures.containsAll(Arrays.asList(repo.getFeatures())));
        assertNull(testApp.getMainFeature());

        assertEquals(NUM_BUNDLES,
                testApp.getBundles()
                        .size());
    }

    // There is no known reason for creating an instance of ApplicationImpl if the repository
    // has no name. Getting a repositories name or its features triggers the repository to be
    // loaded from storage. If that fails, it throws an exception. An Application without features
    // or a name is not useful.
    @Ignore
    @Test
    public void testBadRepoNoName() throws Exception {
        Repository repo = mock(Repository.class);
        when(repo.getURI()).thenReturn(new URI(""));
        when(repo.getFeatures()).thenThrow(new RuntimeException("Testing Exceptions."));
        new ApplicationImpl(repo).getFeatures();
    }

    @Test
    public void testMissingVersionInName() throws Exception {
        String expectedName = "test-dependencies";
        Repository repo = mock(Repository.class);
        when(repo.getName()).thenReturn(expectedName);
        when(repo.getFeatures()).thenReturn(new Feature[0]);
        ApplicationImpl application = new ApplicationImpl(repo);
        assertThat("Application name is wrong", application.getName(), equalTo(expectedName));
        assertThat("Version number is wrong", application.getVersion(), equalTo("0.0.0"));
    }

    @Test
    public void testComplexApplicationName() throws Exception {
        String name = "test-dependencies-1.2.3-1a";
        Repository repo = mock(Repository.class);
        when(repo.getName()).thenReturn(name);
        when(repo.getFeatures()).thenReturn(new Feature[0]);
        ApplicationImpl application = new ApplicationImpl(repo);
        assertThat("Application name is wrong", application.getName(), equalTo("test-dependencies"));
        assertThat("Version number is wrong", application.getVersion(), equalTo("1.2.3-1a"));
    }

    /**
     * Tests that if an application HAS a main feature that the properties in it
     * are properly parsed and set.
     *
     * @throws Exception
     */
    @Test
    public void testMainFeature() throws Exception {
        String mainFeatureName = "main-feature";
        String mainFeatureVersion = "1.0.1";
        String mainFeatureDescription = "Main Feature Test";
        String appToString = mainFeatureName + " - " + mainFeatureVersion;
        RepositoryImpl repo = new RepositoryImpl(getClass().getClassLoader()
                .getResource(FILE_MAIN_FEATURE)
                .toURI());
        repo.load();
        Application testApp = new ApplicationImpl(repo);

        assertEquals(mainFeatureName, testApp.getName());
        assertEquals(mainFeatureVersion, testApp.getVersion());
        assertEquals(mainFeatureDescription, testApp.getDescription());
        assertNotNull(testApp.toString());
        assertEquals(appToString, testApp.toString());

        assertNotNull(testApp.getMainFeature());
    }

    /**
     * Verifies if an app does NOT have a main feature that operations can still
     * be performed.
     *
     * @throws Exception
     */
    @Test
    public void testNoMainFeature() throws Exception {
        String mainFeatureVersion = "1.0.0";
        String mainFeatureDescription = null;
        String appToString = TEST_APP + " - " + mainFeatureVersion;
        RepositoryImpl repo = new RepositoryImpl(getClass().getClassLoader()
                .getResource(FILE_NO_MAIN_FEATURES)
                .toURI());
        repo.load();
        Application testApp = new ApplicationImpl(repo);

        assertEquals(TEST_APP, testApp.getName());
        assertEquals(mainFeatureVersion, testApp.getVersion());
        assertEquals(mainFeatureDescription, testApp.getDescription());
        assertNotNull(testApp.toString());
        assertEquals(appToString, testApp.toString());

        assertNull(testApp.getMainFeature());
    }

    /**
     * Tests that applications can be compared to each other for equality.
     *
     * @throws Exception
     */
    @Test
    public void testAppEquality() throws Exception {
        RepositoryImpl repo1 = new RepositoryImpl(getClass().getClassLoader()
                .getResource(FILE_MAIN_FEATURE)
                .toURI());
        repo1.load();
        Application testApp1 = new ApplicationImpl(repo1);
        Application testApp1Duplicate = new ApplicationImpl(repo1);
        Application testAppNull = null;

        RepositoryImpl repo2 = new RepositoryImpl(getClass().getClassLoader()
                .getResource(FILE_NO_MAIN_FEATURES)
                .toURI());
        repo2.load();
        Application testApp2 = new ApplicationImpl(repo2);

        assertTrue(testApp1.equals(testApp1));
        assertTrue(testApp2.equals(testApp2));
        assertTrue(testApp1.equals(testApp1Duplicate));
        assertFalse(testApp1.equals(testApp2));
        assertFalse(testApp2.equals(testApp1));
        assertFalse(testApp1.equals(testAppNull));

    }

    /**
     * Tests the {@link ApplicationImpl#getURI()} method
     *
     * @throws Exception
     */
    @Test
    public void testGetURI() throws Exception {
        URI testURI = getClass().getClassLoader()
                .getResource(FILE_MAIN_FEATURE)
                .toURI();
        RepositoryImpl repo1 = new RepositoryImpl(testURI);
        repo1.load();
        Application testApp1 = new ApplicationImpl(repo1);
        assertEquals(testURI, testApp1.getURI());
    }

    /**
     * Tests the {@link ApplicationImpl#getDescription()} method
     *
     * @throws Exception
     */
    @Test
    public void testGetDescription() throws Exception {
        RepositoryImpl repo = new RepositoryImpl(getClass().getClassLoader()
                .getResource(FILE_MAIN_FEATURE)
                .toURI());
        repo.load();

        Application testApp = new ApplicationImpl(repo);
        assertEquals(MAIN_FEATURE_NAME, testApp.getDescription());
    }

}
