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
package org.codice.ddf.test.common.options;

import java.util.Arrays;
import org.codice.ddf.test.common.features.Feature;
import org.codice.ddf.test.common.features.FeatureRepo;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.ops4j.pax.exam.options.DefaultCompositeOption;

/** Options for handling karaf features */
public class FeatureOptions {

  /**
   * Adds the specified features to the distribution repository
   *
   * @param features
   * @return
   */
  public static Option addFeatureRepo(FeatureRepo... features) {
    return new DefaultCompositeOption(
        Arrays.stream(features)
            .map(f -> KarafDistributionOption.features(f.getFeatureFileUrl()))
            .toArray(Option[]::new));
  }

  /**
   * Adds the feature to the list of boot features to start before the test begins. This does no
   * guarantee all bundles will be in an active state when the test begins.
   *
   * @param features
   * @return
   */
  public static Option addBootFeature(Feature... features) {
    return new DefaultCompositeOption(
        Arrays.stream(features)
            .map(f -> KarafDistributionOption.features(f.getFeatureFileUrl(), f.featureName()))
            .toArray(Option[]::new));
  }
}
