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
package org.codice.ddf.admin.application.service.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.karaf.bundle.core.BundleState;
import org.apache.karaf.bundle.core.BundleStateService;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Dependency;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.apache.karaf.features.internal.service.RepositoryImpl;
import org.codice.ddf.admin.application.rest.model.FeatureDetails;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleRevision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationServiceImplTest {

  private static final String TEST_NO_MAIN_FEATURE_1_FILE_NAME =
      "test-features-no-main-feature.xml";

  private static final String TEST_MAIN_FEATURES_1_MAIN_FEATURE_NAME = "main-feature";

  private static final String TEST_MAIN_FEATURES_2_MAIN_FEATURE_NAME = "main-feature2";

  private static final String TEST_MAIN_FEATURE_1_FILE_NAME = "test-features-with-main-feature.xml";

  private static final String TEST_INSTALL_PROFILE_FILE_NAME = "test-features-install-profiles.xml";

  private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationServiceImplTest.class);

  private static Repository noMainFeatureRepo1, mainFeatureRepo;

  private static List<BundleStateService> bundleStateServices;

  private BundleContext bundleContext;

  private ServiceReference<FeaturesService> mockFeatureRef;

  /**
   * Creates a {@code Repository} from a features.xml file
   *
   * @param featuresFile The features.xml file from which to create a {@code Repository}
   * @return A {@link Repository} created from the received features.xml file
   */
  private static Repository createRepo(String featuresFile) throws Exception {
    RepositoryImpl repo =
        new RepositoryImpl(
            ApplicationServiceImplTest.class.getClassLoader().getResource(featuresFile).toURI());

    return repo;
  }

  /**
   * Creates default {@link BundleContext}, {@code List} of {@code BundleStateService}s, and {@link
   * Repository} objects for use in the tests.
   *
   * <p>NOTE: These must be in {@code setUp()} method rather than a {@code beforeClass()} method
   * because they are modified by individual tests as part of the setup for individual test
   * conditions. @see {@link #createMockFeaturesService(Set, Set, Set)}
   */
  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws Exception {
    // Recreate the repos and BundleContext prior to each test in order to
    // ensure modifications made in one test do not effect another test.
    noMainFeatureRepo1 = createRepo(TEST_NO_MAIN_FEATURE_1_FILE_NAME);
    mainFeatureRepo = createRepo(TEST_MAIN_FEATURE_1_FILE_NAME);
    bundleContext = mock(BundleContext.class);

    mockFeatureRef = (ServiceReference<FeaturesService>) mock(ServiceReference.class);

    when(bundleContext.getServiceReference(FeaturesService.class)).thenReturn(mockFeatureRef);

    bundleStateServices = new ArrayList<>();

    // Create a BundleStateService for Blueprint
    BundleStateService mockBundleStateService = mock(BundleStateService.class);
    when(mockBundleStateService.getName()).thenReturn(BundleStateService.NAME_BLUEPRINT);
    bundleStateServices.add(mockBundleStateService);
  }

  /** Tests install profile and make sure they load correctly. */
  @Test
  public void testInstallProfileFeatures() throws Exception {
    Repository mainFeaturesRepo2 = createRepo(TEST_INSTALL_PROFILE_FILE_NAME);

    Set<Repository> activeRepos =
        new HashSet<>(Arrays.asList(mainFeatureRepo, mainFeaturesRepo2, noMainFeatureRepo1));

    FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
    when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);

    ApplicationService appService = createPermittedApplicationServiceImpl(featuresService);

    List<Feature> profiles = appService.getInstallationProfiles();

    assertNotNull(profiles);
    assertEquals(2, profiles.size());

    // Ensure order
    Feature profile1 = profiles.get(0);
    Feature profile2 = profiles.get(1);

    assertEquals("profile-b-test1", profile1.getName());
    assertEquals("Desc1", profile1.getDescription());
    List<String> featureNames = getFeatureNames(profile1.getDependencies());
    assertEquals(1, featureNames.size());
    assertTrue(featureNames.contains(TEST_MAIN_FEATURES_1_MAIN_FEATURE_NAME));

    assertEquals("profile-a-test2", profile2.getName());
    assertEquals("Desc2", profile2.getDescription());
    featureNames = getFeatureNames(profile2.getDependencies());
    assertEquals(2, featureNames.size());
    assertTrue(featureNames.contains(TEST_MAIN_FEATURES_1_MAIN_FEATURE_NAME));
    assertTrue(featureNames.contains(TEST_MAIN_FEATURES_2_MAIN_FEATURE_NAME));
  }

  /** Tests the {@link ApplicationServiceImpl#getAllFeatures()} method */
  @Test
  public void testGetAllFeatures() throws Exception {
    Set<Repository> activeRepos = new HashSet<>(Arrays.asList(mainFeatureRepo, noMainFeatureRepo1));
    FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
    when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);
    ApplicationService appService = createPermittedApplicationServiceImpl(featuresService);

    List<FeatureDetails> result = appService.getAllFeatures();

    assertThat(
        "Returned features should match features in mainFeatureRepo.",
        result.get(0).getName(),
        is(mainFeatureRepo.getFeatures()[0].getName()));
    assertThat(
        "Returned features should match features in mainFeatureRepo.",
        result.get(0).getId(),
        is(mainFeatureRepo.getFeatures()[0].getId()));
    assertThat("Should return seven features.", result.size(), is(4));
  }

  /**
   * Tests the {@link ApplicationServiceImpl#getAllFeatures()} method for the case where an
   * exception is thrown in getFeatureToRepository(..)
   */
  @Test
  public void testGetAllFeaturesFTRException() throws Exception {
    Set<Repository> activeRepos = new HashSet<>(Arrays.asList(mainFeatureRepo, noMainFeatureRepo1));
    FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
    when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);
    ApplicationService appService = createPermittedApplicationServiceImpl(featuresService);

    doThrow(new NullPointerException()).when(featuresService).listRepositories();

    List<FeatureDetails> details = appService.getAllFeatures();
    assertThat("List of feature details should have 7 entries", details, hasSize(4));
    details.forEach(
        d ->
            assertThat(
                d.getName() + " should not have a mapped repository",
                d.getRepository(),
                is(nullValue())));
  }

  /**
   * Tests the {@link ApplicationServiceImpl#getAllFeatures()} method for the case where an
   * exception is thrown by the featuresService
   */
  @Test
  public void testGetAllFeaturesException() throws Exception {
    Set<Repository> activeRepos = new HashSet<>(Arrays.asList(mainFeatureRepo, noMainFeatureRepo1));
    FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
    when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);
    ApplicationService appService = createPermittedApplicationServiceImpl(featuresService);

    doThrow(new NullPointerException()).when(featuresService).listFeatures();

    assertThat("No features should have been found", appService.getAllFeatures(), is(empty()));
  }

  /**
   * Tests the {@link ApplicationServiceImpl#getInstallationProfiles()} method for the case where
   * featuresService.listFeatures() throws an exception
   */
  @Test
  public void testGetInstallProfilesException() throws Exception {
    Set<Repository> activeRepos = new HashSet<>(Arrays.asList(mainFeatureRepo, noMainFeatureRepo1));
    FeaturesService featuresService = createMockFeaturesService(activeRepos, null, null);
    when(bundleContext.getService(mockFeatureRef)).thenReturn(featuresService);
    ApplicationService appService =
        new ApplicationServiceImpl(featuresService) {
          @Override
          protected BundleContext getContext() {
            return bundleContext;
          }
        };

    doThrow(new NullPointerException()).when(featuresService).listFeatures();

    assertThat(
        "No installation profiles should of been found, expected an empty collection",
        appService.getInstallationProfiles(),
        is(empty()));
  }

  /**
   * Builds a list containing the feature names of all features.
   *
   * @param dependencies dependencies to loop through.
   * @return list containing the feature names.
   */
  private List<String> getFeatureNames(List<Dependency> dependencies) {
    List<String> featureNames = new ArrayList<>();
    dependencies.forEach(dependency -> featureNames.add(dependency.getName()));
    return featureNames;
  }

  /**
   * Creates a mock {@code FeaturesService} object consisting of all of the features contained in a
   * {@code Set} of {@code Repository} objects. Each {@code Feature} will be in the <i>installed</i>
   * state unless it is contained in the received set of features that are not to be installed. Each
   * {@code Bundle} will be in the {@code Bundle#ACTIVE} state and the {@code BundleState#Active}
   * extended bundle state (as reported by a dependency injection framework) unless it is contained
   * in the received set of {@code Bundle}s that are not to be active, in which case the {@code
   * Bundle} will be in the {@code Bundle#INSTALLED} state and the {@code BundleState#Installed}
   * extended bundle state.
   *
   * <p>Note that not all of the state and {@code Bundle} information is contained in the {@code
   * FeaturesService}. As such, this method stores some of the required information in the class's
   * {@code #bundleContext} and {@code bundleStateServices}. As such, these objects must be
   * re-instantiated for each test (i.e., they must be instantiated in the {@link #setUp()} method).
   *
   * @param repos A {@code Set} of {@link Repository} objects from which to obtain the {@link
   *     Feature}s that are to be included in the mock {@code FeaturesService}
   * @param notInstalledFeatures A {@code Set} of {@code Feature}s that the {@code FeaturesService}
   *     should report as not installed
   * @param inactiveBundles A {@code Set} of {@link BundleInfo}s containing the locations of {@code
   *     Bundle}s that should be set to inactive and for which the {@link BundleStateService}
   *     contained in index 0 of {@link #bundleStateServices} should report a {@link
   *     BundleState#Installed} state.
   * @return A mock {@link FeaturesService} with {@link Feature}s and {@link Bundle}s in the
   *     requested states.
   * @throws Exception
   */
  private FeaturesService createMockFeaturesService(
      Set<Repository> repos, Set<Feature> notInstalledFeatures, Set<BundleInfo> inactiveBundles)
      throws Exception {

    if (LOGGER.isTraceEnabled()) {
      for (Repository repo : repos) {
        for (Feature feature : repo.getFeatures()) {
          LOGGER.trace("Repo Feature: {}", feature);
          LOGGER.trace("Repo Feature name/version: {}/{}", feature.getName(), feature.getVersion());

          LOGGER.trace("Dependencies: ");

          for (Dependency depFeature : feature.getDependencies()) {
            LOGGER.trace("Dependency Feature: {}", depFeature);
            LOGGER.trace(
                "Dependency Feature name/version: {}/{}",
                depFeature.getName(),
                depFeature.getVersion());
          }
        }
      }
    }

    if (null == notInstalledFeatures) {
      notInstalledFeatures = new HashSet<>();
    }

    if (null == inactiveBundles) {
      inactiveBundles = new HashSet<>();
    }

    Set<String> installedBundleLocations = new HashSet<>();
    for (BundleInfo bundleInfo : inactiveBundles) {
      installedBundleLocations.add(bundleInfo.getLocation());
    }

    FeaturesService featuresService = mock(FeaturesService.class);
    Set<Feature> featuresSet = new HashSet<>();

    BundleRevision mockBundleRevision = mock(BundleRevision.class);
    when(mockBundleRevision.getTypes()).thenReturn(0);

    for (Repository curRepo : repos) {
      for (Feature curFeature : curRepo.getFeatures()) {
        featuresSet.add(curFeature);
        when(featuresService.getFeature(curFeature.getName())).thenReturn(curFeature);
        when(featuresService.getFeature(curFeature.getName(), curFeature.getVersion()))
            .thenReturn(curFeature);

        // TODO: File Karaf bug that necessitates this, then reference
        // it here.
        when(featuresService.getFeature(curFeature.getName(), "0.0.0")).thenReturn(curFeature);

        when(featuresService.isInstalled(curFeature))
            .thenReturn(!notInstalledFeatures.contains(curFeature));

        // NOTE: The following logic creates a separate Bundle instance
        // for all Bundles in the repository, even if two features
        // refer to the same bundle. If future tests rely on
        // maintaining the same Bundle instance for each reference
        // of that bundle, this logic will need to be modified.
        for (BundleInfo bundleInfo : curFeature.getBundles()) {
          if (installedBundleLocations.contains(bundleInfo.getLocation())) {

            Bundle mockInstalledBundle = mock(Bundle.class);
            when(mockInstalledBundle.getState()).thenReturn(Bundle.INSTALLED);
            when(mockInstalledBundle.adapt(BundleRevision.class)).thenReturn(mockBundleRevision);

            when(bundleContext.getBundle(bundleInfo.getLocation())).thenReturn(mockInstalledBundle);
            when(bundleStateServices.get(0).getState(mockInstalledBundle))
                .thenReturn(BundleState.Installed);
          } else {
            Bundle mockActiveBundle = mock(Bundle.class);
            when(mockActiveBundle.getState()).thenReturn(Bundle.ACTIVE);
            when(mockActiveBundle.adapt(BundleRevision.class)).thenReturn(mockBundleRevision);

            when(bundleContext.getBundle(bundleInfo.getLocation())).thenReturn(mockActiveBundle);
            when(bundleStateServices.get(0).getState(mockActiveBundle))
                .thenReturn(BundleState.Active);
          }
        }
      }
    }

    when(featuresService.listRepositories())
        .thenReturn(repos.toArray(new Repository[repos.size()]));
    when(featuresService.listFeatures()).thenReturn(featuresSet.toArray(new Feature[] {}));

    return featuresService;
  }

  private ApplicationServiceImpl createPermittedApplicationServiceImpl(
      FeaturesService featuresService) {
    return new ApplicationServiceImpl(featuresService) {
      @Override
      protected BundleContext getContext() {
        return bundleContext;
      }

      @Override
      public boolean isPermittedToViewFeature(String featureName) {
        return true;
      }
    };
  }
}
