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
import static org.junit.Assert.assertNotNull;
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
import org.codice.ddf.admin.application.service.ApplicationServiceException;
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
                .getResource("test-features.xml").toURI());
        repo.load();
        Application testApp = new ApplicationImpl(repo);

        assertEquals(repo.getName(), testApp.getName());

        Set<Feature> appFeatures = testApp.getFeatures();
        assertNotNull(appFeatures);
        assertEquals(repo.getFeatures().length, appFeatures.size());
        assertTrue(appFeatures.containsAll(Arrays.asList(repo.getFeatures())));

        assertEquals(NUM_BUNDLES, testApp.getBundles().size());
    }

    @Test(expected = ApplicationServiceException.class)
    public void testAppErrorHandling() throws Exception {
        Repository repo = mock(Repository.class);
        when(repo.getFeatures()).thenThrow(new RuntimeException("Testing Exceptions."));

        new ApplicationImpl(repo).getFeatures();
        fail("Should have thrown an exception.");

    }

}
