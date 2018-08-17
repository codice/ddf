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
package org.codice.ddf.features.test;

import static org.codice.ddf.test.common.options.DebugOptions.defaultDebuggingOptions;
import static org.codice.ddf.test.common.options.DistributionOptions.kernelDistributionOption;
import static org.codice.ddf.test.common.options.FeatureOptions.addBootFeature;
import static org.codice.ddf.test.common.options.FeatureOptions.addFeatureRepo;
import static org.codice.ddf.test.common.options.LoggingOptions.defaultLogging;
import static org.codice.ddf.test.common.options.PortOptions.defaultPortsOptions;
import static org.codice.ddf.test.common.options.TestResourcesOptions.getTestResource;
import static org.codice.ddf.test.common.options.TestResourcesOptions.includeTestResources;
import static org.codice.ddf.test.common.options.VmOptions.defaultVmOptions;
import static org.ops4j.pax.exam.CoreOptions.options;

import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import org.apache.karaf.features.FeaturesService;
import org.codice.ddf.test.common.features.FeatureUtilities;
import org.codice.ddf.test.common.features.TestUtilitiesFeatures;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExamParameterized;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExamParameterized.class)
@ExamReactorStrategy(PerClass.class)
public class ITTestUtilitiesFeatures {

  private static final String FEATURE_REPO_PATH = getTestResource("/features.xml");

  // This feature is required as a boot feature so various test configuration options can be
  // exported before the test begins so it should not
  // be uninstalled.
  private static final List<String> IGNORED_FEATURES =
      Arrays.asList(TestUtilitiesFeatures.testCommon().featureName());

  @Configuration
  public static Option[] examConfiguration() {
    return options(
        kernelDistributionOption(),
        defaultVmOptions(),
        defaultDebuggingOptions(),
        defaultPortsOptions(),
        defaultLogging(),
        includeTestResources(),
        addFeatureRepo(FeatureUtilities.toFeatureRepo(FEATURE_REPO_PATH)),
        addBootFeature(
            FeatureUtilities.toFeature(
                FEATURE_REPO_PATH, TestUtilitiesFeatures.TEST_COMMON_FEATURE)));
  }

  @Inject private FeaturesService featuresService;
  @Inject private FeatureUtilities featureUtilities;

  private String featureName;

  @Parameterized.Parameters
  public static List<Object[]> getParameters() {
    return FeatureUtilities.featureRepoToFeatureParameters(FEATURE_REPO_PATH, IGNORED_FEATURES);
  }

  public ITTestUtilitiesFeatures(String featureName) {
    this.featureName = featureName;
  }

  @Test
  public void installAndUninstallFeature() throws Exception {
    featureUtilities.installAndUninstallFeature(featuresService, featureName);
  }
}
