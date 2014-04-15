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
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Set;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.Repository;
import org.apache.karaf.features.internal.RepositoryImpl;
import org.codice.ddf.admin.application.service.Application;
import org.codice.ddf.admin.application.service.impl.ApplicationImpl;
import org.junit.Test;

/**
 * Tests out the ApplicationImpl code to make sure it is following the interface
 * specification.
 * 
 */
public class ApplicationImplTest {

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
                .getResource("test-features-no-main-feature.xml").toURI());
        repo.load();
        Application testApp = new ApplicationImpl(repo);

        assertEquals(repo.getName(), testApp.getName());

        Set<Feature> appFeatures = testApp.getFeatures();
        assertNotNull(appFeatures);
        assertEquals(repo.getFeatures().length, appFeatures.size());
        assertTrue(appFeatures.containsAll(Arrays.asList(repo.getFeatures())));
        assertNull(testApp.getMainFeature());

        assertEquals(NUM_BUNDLES, testApp.getBundles().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadRepoNoName() throws Exception {
        Repository repo = mock(Repository.class);
        when(repo.getFeatures()).thenThrow(new RuntimeException("Testing Exceptions."));

        new ApplicationImpl(repo).getFeatures();
        fail("Should have thrown an exception.");

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
                .getResource("test-features-with-main-feature.xml").toURI());
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
        String mainFeatureName = "test-app-1.0.0";
        String mainFeatureVersion = "0.0.0";
        String mainFeatureDescription = null;
        String appToString = mainFeatureName + " - " + mainFeatureVersion;
        RepositoryImpl repo = new RepositoryImpl(getClass().getClassLoader()
                .getResource("test-features-no-main-feature.xml").toURI());
        repo.load();
        Application testApp = new ApplicationImpl(repo);

        assertEquals(mainFeatureName, testApp.getName());
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
                .getResource("test-features-with-main-feature.xml").toURI());
        repo1.load();
        Application testApp1 = new ApplicationImpl(repo1);
        Application testApp1Duplicate = new ApplicationImpl(repo1);
        Application testAppNull = null;

        RepositoryImpl repo2 = new RepositoryImpl(getClass().getClassLoader()
                .getResource("test-features-no-main-feature.xml").toURI());
        repo2.load();
        Application testApp2 = new ApplicationImpl(repo2);

        assertTrue(testApp1.equals(testApp1));
        assertTrue(testApp2.equals(testApp2));
        assertTrue(testApp1.equals(testApp1Duplicate));
        assertFalse(testApp1.equals(testApp2));
        assertFalse(testApp2.equals(testApp1));
        assertFalse(testApp1.equals(testAppNull));

    }

}
