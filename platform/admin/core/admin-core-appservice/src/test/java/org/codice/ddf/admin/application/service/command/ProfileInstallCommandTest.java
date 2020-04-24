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
package org.codice.ddf.admin.application.service.command;

import static org.codice.mockito.PrivilegedVerificationMode.privileged;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.security.audit.SecurityLogger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.EnumSet;
import org.apache.karaf.bundle.core.BundleService;
import org.apache.karaf.bundle.core.internal.BundleServiceImpl;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.internal.service.FeaturesServiceImpl;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.codice.ddf.admin.application.service.impl.ApplicationServiceImpl;
import org.codice.ddf.security.Security;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.resolver.ResolutionException;

public class ProfileInstallCommandTest {

  private static final EnumSet NO_AUTO_REFRESH =
      EnumSet.of(FeaturesService.Option.NoAutoRefreshBundles);

  private ApplicationService applicationService;
  private FeaturesService featuresService;
  private BundleService bundleService;
  private Security security;
  private ProfileInstallCommand profileInstallCommand;

  @Rule public TemporaryFolder ddfHome = new TemporaryFolder();

  private Path profilePath;

  @Before
  public void setUp() throws Exception {
    ddfHome.newFolder("etc", "profiles");
    profilePath = Paths.get(ddfHome.getRoot().toString(), "etc", "profiles");
    Files.copy(
        this.getClass().getResourceAsStream("/profiles/devProfile.json"),
        Paths.get(profilePath.toAbsolutePath().toString(), "devProfile.json"));
    Files.copy(
        this.getClass().getResourceAsStream("/profiles/profileWithDuplicates.json"),
        Paths.get(profilePath.toAbsolutePath().toString(), "profileWithDuplicates.json"));
    Files.copy(
        this.getClass().getResourceAsStream("/profiles/invalidFeatureInstall.json"),
        Paths.get(profilePath.toAbsolutePath().toString(), "invalidFeatureInstall.json"));
    Files.copy(
        this.getClass().getResourceAsStream("/profiles/invalidFeatureUninstall.json"),
        Paths.get(profilePath.toAbsolutePath().toString(), "invalidFeatureUninstall.json"));
    Files.copy(
        this.getClass().getResourceAsStream("/profiles/invalidStopBundles.json"),
        Paths.get(profilePath.toAbsolutePath().toString(), "invalidStopBundles.json"));

    this.applicationService = mock(ApplicationServiceImpl.class);
    this.featuresService = mock(FeaturesServiceImpl.class);
    this.bundleService = mock(BundleServiceImpl.class);
    profileInstallCommand = getProfileInstallCommand(profilePath);
    profileInstallCommand.securityLogger = mock(SecurityLogger.class);

    Feature installerFeature = mock(Feature.class);
    when(installerFeature.getName()).thenReturn("admin-modules.installer/0.0.0");
    when(featuresService.isInstalled(installerFeature)).thenReturn(true, false);

    when(applicationService.getInstallationProfiles()).thenReturn(Collections.emptyList());
    when(featuresService.getFeature(anyString()))
        .thenAnswer(invocation -> createMockFeature(invocation.getArguments()[0].toString()));
    when(bundleService.getBundle(anyString())).thenReturn(mock(Bundle.class));
  }

  @Test
  public void testInstallValidExtraProfile() throws Exception {
    profileInstallCommand.profileName = "devProfile";
    profileInstallCommand.doExecute(applicationService, featuresService, bundleService);
    verifyExtraProfileMocks();
  }

  @Test
  public void testInstallValidExtraProfileWithDuplicates() throws Exception {
    profileInstallCommand.profileName = "profileWithDuplicates";
    profileInstallCommand.doExecute(applicationService, featuresService, bundleService);
    verifyExtraProfileMocks();
  }

  @Test(expected = ResolutionException.class)
  public void testExtraProfileInvalidFeatureInstall() throws Exception {
    profileInstallCommand.profileName = "invalidFeatureInstall";
    doThrow(new ResolutionException(""))
        .when(featuresService)
        .installFeature("badFeature", NO_AUTO_REFRESH);
    profileInstallCommand.doExecute(applicationService, featuresService, bundleService);
    verify(featuresService, times(2)).installFeature(anyString(), eq(NO_AUTO_REFRESH));
  }

  @Test(expected = ResolutionException.class)
  public void testExtraProfileInvalidFeatureUninstall() throws Exception {
    profileInstallCommand.profileName = "invalidFeatureUninstall";
    doThrow(new ResolutionException(""))
        .when(featuresService)
        .uninstallFeature("badFeature", "0.0.0", NO_AUTO_REFRESH);
    profileInstallCommand.doExecute(applicationService, featuresService, bundleService);
    verify(featuresService, times(2)).uninstallFeature(anyString(), eq(NO_AUTO_REFRESH));
  }

  @Test(expected = BundleException.class)
  public void testExtraProfileInvalidStopBundle() throws Exception {
    profileInstallCommand.profileName = "invalidStopBundles";
    Bundle badBundle = mock(Bundle.class);
    doThrow(new BundleException("")).when(badBundle).stop();
    when(bundleService.getBundle("badBundle")).thenReturn(badBundle);
    profileInstallCommand.doExecute(applicationService, featuresService, bundleService);
    verify(bundleService, times(2)).getBundle(anyString());
  }

  @Test
  public void testExtraProfilesWhenNotExists() throws Exception {
    profileInstallCommand.profileName = "nonExistent";
    profileInstallCommand.doExecute(applicationService, featuresService, bundleService);
    verifyMocksNoOp();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testProfileStartsFolderTraversal() throws Exception {
    profileInstallCommand.profileName = "../testProfile";
    profileInstallCommand.doExecute(applicationService, featuresService, bundleService);
    verifyMocksNoOp();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testProfileStartsSlash() throws Exception {
    profileInstallCommand.profileName = "/testProfile";
    profileInstallCommand.doExecute(applicationService, featuresService, bundleService);
    verifyMocksNoOp();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBundleNotExist() throws Exception {
    doThrow(IllegalArgumentException.class).when(bundleService).getBundle(any());
    profileInstallCommand.profileName = "invalidStopBundles";
    profileInstallCommand.doExecute(applicationService, featuresService, bundleService);
  }

  @Test(expected = Exception.class)
  public void testInstallPostInstallModuleFailure() throws Exception {
    Feature postInstallFeature = createMockFeature("admin-post-install-modules");
    when(featuresService.getFeature("admin-post-install-modules")).thenReturn(postInstallFeature);
    when(featuresService.isInstalled(postInstallFeature)).thenReturn(false);
    doThrow(Exception.class)
        .when(featuresService)
        .installFeature("admin-post-install-modules", NO_AUTO_REFRESH);
    profileInstallCommand.profileName = "devProfile";
    profileInstallCommand.doExecute(applicationService, featuresService, bundleService);
  }

  @Test
  public void testPostInstallWithInstallerInstalled() throws Exception {
    Feature installerFeature = createMockFeature("admin-modules-installer");
    this.featuresService = mock(FeaturesService.class);
    when(featuresService.getFeature(anyString())).thenReturn(installerFeature);
    when(featuresService.isInstalled(installerFeature)).thenReturn(true);
    profileInstallCommand.profileName = "invalidStopBundles";
    profileInstallCommand.doExecute(applicationService, featuresService, bundleService);
    verify(featuresService)
        .uninstallFeature(eq("admin-modules-installer"), eq("0.0.0"), eq(NO_AUTO_REFRESH));
  }

  @Test
  public void testPostInstallerAndInstaller() throws Exception {
    Feature postInstallFeature = createMockFeature("admin-post-install-modules");
    Feature installerFeature = createMockFeature("admin-modules-installer");
    this.featuresService = mock(FeaturesService.class);
    when(featuresService.getFeature("admin-post-install-modules")).thenReturn(postInstallFeature);
    when(featuresService.getFeature("admin-modules-installer")).thenReturn(installerFeature);
    when(featuresService.isInstalled(postInstallFeature)).thenReturn(false);
    when(featuresService.isInstalled(installerFeature)).thenReturn(true);
    profileInstallCommand.profileName = "invalidStopBundles";
    profileInstallCommand.doExecute(applicationService, featuresService, bundleService);
    verify(featuresService).installFeature(eq("admin-post-install-modules"), eq(NO_AUTO_REFRESH));
    verify(featuresService)
        .uninstallFeature(eq("admin-modules-installer"), eq("0.0.0"), eq(NO_AUTO_REFRESH));
  }

  @Test(expected = Exception.class)
  public void testUninstallInstallerFailure() throws Exception {
    Feature installerFeature = createMockFeature("admin-modules-installer");
    this.featuresService = mock(FeaturesService.class);
    when(featuresService.getFeature(anyString())).thenReturn(installerFeature);
    when(featuresService.isInstalled(installerFeature)).thenReturn(true);
    doThrow(Exception.class)
        .when(featuresService)
        .uninstallFeature("admin-modules-installer", "0.0.0", NO_AUTO_REFRESH);
    profileInstallCommand.profileName = "invalidStopBundles";
    profileInstallCommand.doExecute(applicationService, featuresService, bundleService);
  }

  @Test
  public void testExtraProfileFileError() throws Exception {
    ddfHome.newFolder("etc", "profiles", "foo.json");

    profileInstallCommand.profileName = "foo";
    profileInstallCommand.doExecute(applicationService, featuresService, bundleService);
    verifyMocksNoOp();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullFeature() throws Exception {
    FeaturesService badFeatureService = mock(FeaturesService.class);
    when(badFeatureService.getFeature(any())).thenReturn(null);
    profileInstallCommand.profileName = "invalidFeatureUninstall";
    profileInstallCommand.doExecute(applicationService, badFeatureService, bundleService);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testProfileStartingWithDrive() throws Exception {
    profileInstallCommand.profileName = "C:\\invalidStopBundles";
    profileInstallCommand.doExecute(applicationService, featuresService, bundleService);
  }

  private void verifyExtraProfileMocks() throws Exception {

    InOrder inOrder = Mockito.inOrder(applicationService, featuresService, bundleService);

    verify(featuresService, privileged(times(3)))
        .installFeature(anyString(), (EnumSet<FeaturesService.Option>) any());
    inOrder
        .verify(featuresService, privileged())
        .installFeature(eq("feature1"), eq(NO_AUTO_REFRESH));
    inOrder
        .verify(featuresService, privileged())
        .installFeature(eq("feature2"), eq(NO_AUTO_REFRESH));

    verify(featuresService, privileged(times(2))).uninstallFeature(anyString(), anyString(), any());
    inOrder
        .verify(featuresService, privileged())
        .uninstallFeature(eq("feature3"), anyString(), eq(NO_AUTO_REFRESH));
    inOrder
        .verify(featuresService, privileged())
        .uninstallFeature(eq("feature4"), anyString(), eq(NO_AUTO_REFRESH));

    verify(bundleService.getBundle(anyString()), times(2)).stop();
    inOrder.verify(bundleService).getBundle("bundle1");
    inOrder.verify(bundleService).getBundle("bundle2");
    inOrder
        .verify(featuresService, privileged())
        .installFeature(eq("admin-post-install-modules"), eq(NO_AUTO_REFRESH));
  }

  private void verifyMocksNoOp() throws Exception {
    verify(featuresService, never()).installFeature(anyString(), eq(NO_AUTO_REFRESH));
    verify(featuresService, never())
        .uninstallFeature(anyString(), anyString(), eq(NO_AUTO_REFRESH));
    verify(bundleService, never()).getBundle(anyString());
  }

  private Feature createMockFeature(String name) {
    Feature feature = mock(Feature.class);
    when(feature.getName()).thenReturn(name);
    when(feature.getVersion()).thenReturn("0.0.0");
    return feature;
  }

  private ProfileInstallCommand getProfileInstallCommand(Path profilePath) {
    ProfileInstallCommand command = new ProfileInstallCommand();
    command.setProfilePath(profilePath);
    //    command.setSecurity(createSecurityMock());
    return command;
  }
}
