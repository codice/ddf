package org.codice.ddf.features.profiles.test;

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
public class ITInstallProfilesFeatures {

  private static final String FEATURE_REPO_PATH = getTestResource("/features.xml");

  // DDF-3769
  private static final List<String> IGNORED_FEATURES = Arrays.asList("profile-development");

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

  public ITInstallProfilesFeatures(String featureName) {
    this.featureName = featureName;
  }

  @Test
  public void installAndUninstallFeature() throws Exception {
    FeatureUtilities.installAndUninstallFeature(featuresService, featureName);
  }
}
