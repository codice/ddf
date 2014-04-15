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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.karaf.features.Feature;
import org.codice.ddf.admin.application.service.Application;
import org.codice.ddf.admin.application.service.ApplicationStatus;
import org.codice.ddf.admin.application.service.ApplicationStatus.ApplicationState;
import org.codice.ddf.admin.application.service.impl.ApplicationStatusImpl;
import org.junit.Test;
import org.osgi.framework.Bundle;

/**
 * Tests the ApplicationStatusImpl class which retuns the stats of an
 * application.
 *
 */
public class ApplicationStatusImplTest {

    /**
     * Tests that the app status is created properly and returns the correct
     * values.
     */
    @Test
    public void testAppStatusCreation() {
        Application testApp = mock(Application.class);
        ApplicationState testState = ApplicationState.ACTIVE;
        Set<Feature> testFeatures = new HashSet<Feature>();
        Set<Bundle> testBundles = new HashSet<Bundle>();

        Feature testFeature1 = mock(Feature.class);
        Feature testFeature2 = mock(Feature.class);

        List<Feature> testFeatureList = new ArrayList<Feature>(Arrays.asList(testFeature1,
                testFeature2));
        testFeatures.addAll(testFeatureList);

        Bundle testBundle1 = mock(Bundle.class);
        Bundle testBundle2 = mock(Bundle.class);

        List<Bundle> testBundleList = new ArrayList<Bundle>(Arrays.asList(testBundle1, testBundle2));
        testBundles.addAll(testBundleList);

        ApplicationStatus testStatus = new ApplicationStatusImpl(testApp, testState, testFeatures,
                testBundles);

        assertEquals(testApp, testStatus.getApplication());
        assertEquals(testState, testStatus.getState());
        assertTrue(testStatus.getErrorFeatures().containsAll(testFeatureList));
        assertTrue(testStatus.getErrorBundles().containsAll(testBundleList));
    }

}
