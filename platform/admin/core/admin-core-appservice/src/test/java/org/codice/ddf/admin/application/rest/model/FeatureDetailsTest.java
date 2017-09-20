/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.admin.application.rest.model;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.karaf.features.Feature;
import org.junit.Test;

public class FeatureDetailsTest {

  private static final String TEST_FEATURE_NAME = "TestFeature";

  private static final String TEST_FEATURE_ID = "001";

  private static final String TEST_FEATURE_VERSION = "0.0.0";

  private static final String TEST_FEATURE_INSTALL = "TestInstallString";

  private static final String TEST_FEATURE_DESCRIP = "Feature for testing FeatureDetails";

  private static final String TEST_FEATURE_DETAILS = "TestDetails";

  private static final String TEST_FEATURE_RESOLVER = "TestResolver";

  private static final String TEST_FEATURE_STATUS = "TestStatus";

  private static final String TEST_FEATURE_REPO = "TestRepo";

  /**
   * Tests the {@link FeatureDetails#FeatureDetails(Feature, String, String)} constructor, and all
   * associated getters
   */
  @Test
  public void testFeatureDetails() {
    Feature testFeature = mock(Feature.class);
    when(testFeature.getName()).thenReturn(TEST_FEATURE_NAME);
    when(testFeature.getId()).thenReturn(TEST_FEATURE_ID);
    when(testFeature.getVersion()).thenReturn(TEST_FEATURE_VERSION);
    when(testFeature.getInstall()).thenReturn(TEST_FEATURE_INSTALL);
    when(testFeature.getDescription()).thenReturn(TEST_FEATURE_DESCRIP);
    when(testFeature.getDetails()).thenReturn(TEST_FEATURE_DETAILS);
    when(testFeature.getResolver()).thenReturn(TEST_FEATURE_RESOLVER);

    FeatureDetails testDetails =
        new FeatureDetails(testFeature, TEST_FEATURE_STATUS, TEST_FEATURE_REPO);

    assertEquals(TEST_FEATURE_NAME, testDetails.getName());
    assertEquals(TEST_FEATURE_ID, testDetails.getId());
    assertEquals(TEST_FEATURE_VERSION, testDetails.getVersion());
    assertEquals(TEST_FEATURE_INSTALL, testDetails.getInstall());
    assertEquals(TEST_FEATURE_DESCRIP, testDetails.getDescription());
    assertEquals(TEST_FEATURE_DETAILS, testDetails.getDetails());
    assertEquals(TEST_FEATURE_RESOLVER, testDetails.getResolver());
    assertEquals(TEST_FEATURE_REPO, testDetails.getRepository());
    assertEquals(TEST_FEATURE_STATUS, testDetails.getStatus());
  }
}
