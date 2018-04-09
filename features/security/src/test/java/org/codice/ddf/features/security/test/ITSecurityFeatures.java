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
package org.codice.ddf.features.security.test;

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
public class ITSecurityFeatures {

  private static final String FEATURE_REPO_PATH = getTestResource("/features.xml");

  // TODO: tbatie - 4/9/18 - Make follow up ticket for cas bundles not starting
  //  // [caused by: Unable to resolve org.codice.thirdparty.cas-client-core/3.1.10.1: missing
  //  // requirement [org.codice.thirdparty.cas-client-core/3.1.10.1] osgi.wiring.package;
  //  // filter:="(osgi.wiring.package=org.opensaml)"]]]

  //  // missing requirement [security-cas-tokenvalidator/2.12.0.SNAPSHOT] osgi.wiring.package;
  //  // filter:="(&(osgi.wiring.package=javax.servlet)(version>=2.5.0)(!(version>=3.0.0)))"]]

  //  // [caused by: Unable to resolve org.codice.thirdparty.cas-client-core/3.1.10.1: missing
  //  // requirement [org.codice.thirdparty.cas-client-core/3.1.10.1] osgi.wiring.package;
  //  // filter:="(osgi.wiring.package=org.opensaml)"]]]

  private static final List<String> IGNORED_FEATURES =
      Arrays.asList(
          "security-cas-client", "security-cas-tokenvalidator", "security-cas-cxfservletfilter");

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
        addBootFeature(TestUtilitiesFeatures.testCommon()));
  }

  @Parameterized.Parameters
  public static List<Object[]> getParameters() {
    return FeatureUtilities.featureRepoToFeatureParameters(FEATURE_REPO_PATH, IGNORED_FEATURES);
  }

  @Inject private FeaturesService featuresService;

  private String featureName;

  public ITSecurityFeatures(String featureName) {
    this.featureName = featureName;
  }

  @Test
  public void installAndUninstallFeature() throws Exception {
    FeatureUtilities.installAndUninstallFeature(featuresService, featureName);
  }
}
