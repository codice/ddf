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
package org.codice.ddf.test.common.features;

import static org.ops4j.pax.exam.CoreOptions.maven;

import org.codice.ddf.test.common.DependencyVersionResolver;

public class TestUtilitiesFeatures {

  private TestUtilitiesFeatures() {}

  public static final String TEST_COMMON_FEATURE = "test-common";

  public static final String HAMCREST_ALL_FEATURE = "hamcrest-all";

  public static final String AWAITILITY = "awaitility";

  public static final String TINY_BUNDLES = "tinybundles";

  private static final FeatureRepo FEATURE_COORDINATES =
      new FeatureRepoImpl(
          maven()
              .groupId("ddf.features")
              .artifactId("test-utilities")
              .type("xml")
              .classifier("features")
              .version(DependencyVersionResolver.resolver()));

  public static FeatureRepo featureRepo() {
    return FEATURE_COORDINATES;
  }

  public static Feature testCommon() {
    return new FeatureImpl(FEATURE_COORDINATES.getFeatureFileUrl(), TEST_COMMON_FEATURE);
  }

  public static Feature hamcrestAll() {
    return new FeatureImpl(FEATURE_COORDINATES.getFeatureFileUrl(), HAMCREST_ALL_FEATURE);
  }

  public static Feature awaitility() {
    return new FeatureImpl(FEATURE_COORDINATES.getFeatureFileUrl(), AWAITILITY);
  }

  public static Feature tinyBundles() {
    return new FeatureImpl(FEATURE_COORDINATES.getFeatureFileUrl(), TINY_BUNDLES);
  }
}
