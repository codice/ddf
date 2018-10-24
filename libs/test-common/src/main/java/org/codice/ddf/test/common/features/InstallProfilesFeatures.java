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

public class InstallProfilesFeatures {

  private InstallProfilesFeatures() {}

  private static final FeatureRepo FEATURE_REPO_URL =
      new FeatureRepoImpl(
          maven()
              .groupId("ddf.features")
              .artifactId("install-profiles")
              .type("xml")
              .classifier("features")
              .version(DependencyVersionResolver.resolver()));

  public static final String PROFILE_STANDARD = "profile-standard";

  public static FeatureRepo featureRepo() {
    return FEATURE_REPO_URL;
  }

  public static Feature profileStandard() {
    return new FeatureImpl(FEATURE_REPO_URL.getFeatureFileUrl(), PROFILE_STANDARD);
  }
}
