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
package org.codice.ddf.admin.application.service.migratable;

import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.codice.ddf.migration.MigrationException;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;

public class BundleProcessorTest {

  private static final long ID = 14235L;
  private static final long ID2 = 24235L;
  private static final long ID3 = 34235L;
  private static final long ID4 = 44235L;
  private static final String NAME = "test.name";
  private static final String NAME2 = "test.name2";
  private static final String NAME3 = "test.name3";
  private static final String NAME4 = "test.name4";
  private static final Version VERSION = new Version(1, 2, 3, "what");
  private static final Version VERSION2 = new Version(2, 2, 3, "what");
  private static final Version VERSION3 = new Version(3, 2, 3, "what");
  private static final Version VERSION4 = new Version(4, 2, 3, "what");
  private static final int STATE = Bundle.STARTING;
  private static final int STATE2 = Bundle.UNINSTALLED;
  private static final int STATE3 = Bundle.INSTALLED;
  private static final int STATE4 = Bundle.ACTIVE;
  private static final String LOCATION = "test.location";
  private static final String LOCATION2 = "test.location2";
  private static final String LOCATION3 = "test.location3";
  private static final String LOCATION4 = "test.location4";
  private static final String NAME_VERSION = NAME + '/' + VERSION;
  private static final String NAME_VERSION2 = NAME2 + '/' + VERSION2;
  private static final String NAME_VERSION3 = NAME3 + '/' + VERSION3;
  private static final String NAME_VERSION4 = NAME4 + '/' + VERSION4;

  private final Bundle bundle = Mockito.mock(Bundle.class);

  private final Bundle bundle2 = Mockito.mock(Bundle.class);

  private final Bundle bundle3 = Mockito.mock(Bundle.class);

  private final Bundle bundle4 = Mockito.mock(Bundle.class);

  private final BundleProcessor bundleProcessor =
      Mockito.mock(BundleProcessor.class, Mockito.CALLS_REAL_METHODS);

  private final TaskList tasks = Mockito.mock(TaskList.class);

  private final BundleContext context = Mockito.mock(BundleContext.class);

  private final ProfileMigrationReport report = Mockito.mock(ProfileMigrationReport.class);

  private final JsonProfile jprofile = Mockito.mock(JsonProfile.class);

  @Before
  public void setup() throws Exception {
    // execute the tasks added right away
    Mockito.doAnswer(
            AdditionalAnswers.<Boolean, Operation, String, Predicate<ProfileMigrationReport>>answer(
                (o, i, t) -> t.test(report)))
        .when(tasks)
        .add(Mockito.any(), Mockito.anyString(), Mockito.any());

    Mockito.when(bundle.getBundleId()).thenReturn(ID);
    Mockito.when(bundle.getSymbolicName()).thenReturn(NAME);
    Mockito.when(bundle.getVersion()).thenReturn(VERSION);
    Mockito.when(bundle.getState()).thenReturn(STATE);
    Mockito.when(bundle.getLocation()).thenReturn(LOCATION);

    Mockito.when(bundle2.getBundleId()).thenReturn(ID2);
    Mockito.when(bundle2.getSymbolicName()).thenReturn(NAME2);
    Mockito.when(bundle2.getVersion()).thenReturn(VERSION2);
    Mockito.when(bundle2.getState()).thenReturn(STATE2);
    Mockito.when(bundle2.getLocation()).thenReturn(LOCATION2);

    Mockito.when(bundle3.getBundleId()).thenReturn(ID3);
    Mockito.when(bundle3.getSymbolicName()).thenReturn(NAME3);
    Mockito.when(bundle3.getVersion()).thenReturn(VERSION3);
    Mockito.when(bundle3.getState()).thenReturn(STATE3);
    Mockito.when(bundle3.getLocation()).thenReturn(LOCATION3);

    Mockito.when(bundle4.getBundleId()).thenReturn(ID4);
    Mockito.when(bundle4.getSymbolicName()).thenReturn(NAME4);
    Mockito.when(bundle4.getVersion()).thenReturn(VERSION4);
    Mockito.when(bundle4.getState()).thenReturn(STATE4);
    Mockito.when(bundle4.getLocation()).thenReturn(LOCATION4);
  }

  @Test
  public void testListBundles() throws Exception {
    Mockito.when(context.getBundles()).thenReturn(new Bundle[] {bundle, bundle2, bundle3, bundle4});

    Assert.assertThat(
        bundleProcessor.listBundles(context),
        Matchers.arrayContaining(
            Matchers.sameInstance(bundle),
            Matchers.sameInstance(bundle2),
            Matchers.sameInstance(bundle3),
            Matchers.sameInstance(bundle4)));
  }

  @Test
  public void testInstallBundle() throws Exception {
    Mockito.when(context.installBundle(LOCATION)).thenReturn(bundle);

    Assert.assertThat(
        bundleProcessor.installBundle(context, report, bundle), Matchers.equalTo(true));

    Mockito.verify(context).installBundle(LOCATION);
    Mockito.verify(report, Mockito.never()).recordOnFinalAttempt(Mockito.any());
  }

  @Test
  public void testInstallBundleWhenFailWithIllegalStateException() throws Exception {
    final IllegalStateException e = new IllegalStateException();

    Mockito.doThrow(e).when(context).installBundle(LOCATION);

    Assert.assertThat(
        bundleProcessor.installBundle(context, report, bundle), Matchers.equalTo(false));

    final ArgumentCaptor<MigrationException> captor =
        ArgumentCaptor.forClass(MigrationException.class);

    Mockito.verify(context).installBundle(LOCATION);
    Mockito.verify(report).recordOnFinalAttempt(captor.capture());

    final MigrationException me = captor.getValue();

    Assert.assertThat(
        me.getMessage(),
        Matchers.containsString("failed to install bundle [" + NAME_VERSION + "]"));
    Assert.assertThat(me.getCause(), Matchers.sameInstance(e));
  }

  @Test
  public void testInstallBundleWhenFailWithBundleException() throws Exception {
    final BundleException e = new BundleException("testing");

    Mockito.doThrow(e).when(context).installBundle(LOCATION);

    Assert.assertThat(
        bundleProcessor.installBundle(context, report, bundle), Matchers.equalTo(false));

    final ArgumentCaptor<MigrationException> captor =
        ArgumentCaptor.forClass(MigrationException.class);

    Mockito.verify(context).installBundle(LOCATION);
    Mockito.verify(report).recordOnFinalAttempt(captor.capture());

    final MigrationException me = captor.getValue();

    Assert.assertThat(
        me.getMessage(),
        Matchers.containsString("failed to install bundle [" + NAME_VERSION + "]"));
    Assert.assertThat(me.getCause(), Matchers.sameInstance(e));
  }

  @Test
  public void testInstallBundleWhenFailWithSecurityException() throws Exception {
    final SecurityException e = new SecurityException("testing");

    Mockito.doThrow(e).when(context).installBundle(LOCATION);

    Assert.assertThat(
        bundleProcessor.installBundle(context, report, bundle), Matchers.equalTo(false));

    final ArgumentCaptor<MigrationException> captor =
        ArgumentCaptor.forClass(MigrationException.class);

    Mockito.verify(context).installBundle(LOCATION);
    Mockito.verify(report).recordOnFinalAttempt(captor.capture());

    final MigrationException me = captor.getValue();

    Assert.assertThat(
        me.getMessage(),
        Matchers.containsString("failed to install bundle [" + NAME_VERSION + "]"));
    Assert.assertThat(me.getCause(), Matchers.sameInstance(e));
  }

  @Test
  public void testUninstallBundle() throws Exception {
    Mockito.doNothing().when(bundle).uninstall();

    Assert.assertThat(bundleProcessor.uninstallBundle(report, bundle), Matchers.equalTo(true));

    Mockito.verify(bundle).uninstall();
    Mockito.verify(report, Mockito.never()).recordOnFinalAttempt(Mockito.any());
  }

  @Test
  public void testUninstallBundleWhenFailWithIllegalStateException() throws Exception {
    final IllegalStateException e = new IllegalStateException();

    Mockito.doThrow(e).when(bundle).uninstall();

    Assert.assertThat(bundleProcessor.uninstallBundle(report, bundle), Matchers.equalTo(false));

    final ArgumentCaptor<MigrationException> captor =
        ArgumentCaptor.forClass(MigrationException.class);

    Mockito.verify(bundle).uninstall();
    Mockito.verify(report).recordOnFinalAttempt(captor.capture());

    final MigrationException me = captor.getValue();

    Assert.assertThat(
        me.getMessage(),
        Matchers.containsString("failed to uninstall bundle [" + NAME_VERSION + "]"));
    Assert.assertThat(me.getCause(), Matchers.sameInstance(e));
  }

  @Test
  public void testUninstallBundleWhenFailWithBundleException() throws Exception {
    final BundleException e = new BundleException("testing");

    Mockito.doThrow(e).when(bundle).uninstall();

    Assert.assertThat(bundleProcessor.uninstallBundle(report, bundle), Matchers.equalTo(false));

    final ArgumentCaptor<MigrationException> captor =
        ArgumentCaptor.forClass(MigrationException.class);

    Mockito.verify(bundle).uninstall();
    Mockito.verify(report).recordOnFinalAttempt(captor.capture());

    final MigrationException me = captor.getValue();

    Assert.assertThat(
        me.getMessage(),
        Matchers.containsString("failed to uninstall bundle [" + NAME_VERSION + "]"));
    Assert.assertThat(me.getCause(), Matchers.sameInstance(e));
  }

  @Test
  public void testUninstallBundleWhenFailWithSecurityException() throws Exception {
    final SecurityException e = new SecurityException();

    Mockito.doThrow(e).when(bundle).uninstall();

    Assert.assertThat(bundleProcessor.uninstallBundle(report, bundle), Matchers.equalTo(false));

    final ArgumentCaptor<MigrationException> captor =
        ArgumentCaptor.forClass(MigrationException.class);

    Mockito.verify(bundle).uninstall();
    Mockito.verify(report).recordOnFinalAttempt(captor.capture());

    final MigrationException me = captor.getValue();

    Assert.assertThat(
        me.getMessage(),
        Matchers.containsString("failed to uninstall bundle [" + NAME_VERSION + "]"));
    Assert.assertThat(me.getCause(), Matchers.sameInstance(e));
  }

  @Test
  public void testStartBundle() throws Exception {
    Mockito.doNothing().when(bundle).start();

    Assert.assertThat(bundleProcessor.startBundle(report, bundle), Matchers.equalTo(true));

    Mockito.verify(bundle).start();
    Mockito.verify(report, Mockito.never()).recordOnFinalAttempt(Mockito.any());
  }

  @Test
  public void testStartBundleWhenFailWithIllegalStateException() throws Exception {
    final IllegalStateException e = new IllegalStateException();

    Mockito.doThrow(e).when(bundle).start();

    Assert.assertThat(bundleProcessor.startBundle(report, bundle), Matchers.equalTo(false));

    final ArgumentCaptor<MigrationException> captor =
        ArgumentCaptor.forClass(MigrationException.class);

    Mockito.verify(bundle).start();
    Mockito.verify(report).recordOnFinalAttempt(captor.capture());

    final MigrationException me = captor.getValue();

    Assert.assertThat(
        me.getMessage(), Matchers.containsString("failed to start bundle [" + NAME_VERSION + "]"));
    Assert.assertThat(me.getCause(), Matchers.sameInstance(e));
  }

  @Test
  public void testStartBundleWhenFailWithBundleException() throws Exception {
    final BundleException e = new BundleException("testing");

    Mockito.doThrow(e).when(bundle).start();

    Assert.assertThat(bundleProcessor.startBundle(report, bundle), Matchers.equalTo(false));

    final ArgumentCaptor<MigrationException> captor =
        ArgumentCaptor.forClass(MigrationException.class);

    Mockito.verify(bundle).start();
    Mockito.verify(report).recordOnFinalAttempt(captor.capture());

    final MigrationException me = captor.getValue();

    Assert.assertThat(
        me.getMessage(), Matchers.containsString("failed to start bundle [" + NAME_VERSION + "]"));
    Assert.assertThat(me.getCause(), Matchers.sameInstance(e));
  }

  @Test
  public void testStartBundleWhenFailWithSecurityException() throws Exception {
    final SecurityException e = new SecurityException();

    Mockito.doThrow(e).when(bundle).start();

    Assert.assertThat(bundleProcessor.startBundle(report, bundle), Matchers.equalTo(false));

    final ArgumentCaptor<MigrationException> captor =
        ArgumentCaptor.forClass(MigrationException.class);

    Mockito.verify(bundle).start();
    Mockito.verify(report).recordOnFinalAttempt(captor.capture());

    final MigrationException me = captor.getValue();

    Assert.assertThat(
        me.getMessage(), Matchers.containsString("failed to start bundle [" + NAME_VERSION + "]"));
    Assert.assertThat(me.getCause(), Matchers.sameInstance(e));
  }

  @Test
  public void testStopBundle() throws Exception {
    Mockito.doNothing().when(bundle).stop();

    Assert.assertThat(bundleProcessor.stopBundle(report, bundle), Matchers.equalTo(true));

    Mockito.verify(bundle).stop();
    Mockito.verify(report, Mockito.never()).recordOnFinalAttempt(Mockito.any());
  }

  @Test
  public void testStopBundleWhenFailWithIllegalStateException() throws Exception {
    final IllegalStateException e = new IllegalStateException();

    Mockito.doThrow(e).when(bundle).stop();

    Assert.assertThat(bundleProcessor.stopBundle(report, bundle), Matchers.equalTo(false));

    final ArgumentCaptor<MigrationException> captor =
        ArgumentCaptor.forClass(MigrationException.class);

    Mockito.verify(bundle).stop();
    Mockito.verify(report).recordOnFinalAttempt(captor.capture());

    final MigrationException me = captor.getValue();

    Assert.assertThat(
        me.getMessage(), Matchers.containsString("failed to stop bundle [" + NAME_VERSION + "]"));
    Assert.assertThat(me.getCause(), Matchers.sameInstance(e));
  }

  @Test
  public void testStopBundleWhenFailWithBundleException() throws Exception {
    final BundleException e = new BundleException("testing");

    Mockito.doThrow(e).when(bundle).stop();

    Assert.assertThat(bundleProcessor.stopBundle(report, bundle), Matchers.equalTo(false));

    final ArgumentCaptor<MigrationException> captor =
        ArgumentCaptor.forClass(MigrationException.class);

    Mockito.verify(bundle).stop();
    Mockito.verify(report).recordOnFinalAttempt(captor.capture());

    final MigrationException me = captor.getValue();

    Assert.assertThat(
        me.getMessage(), Matchers.containsString("failed to stop bundle [" + NAME_VERSION + "]"));
    Assert.assertThat(me.getCause(), Matchers.sameInstance(e));
  }

  @Test
  public void testStopBundleWhenFailWithSecurityException() throws Exception {
    final SecurityException e = new SecurityException();

    Mockito.doThrow(e).when(bundle).stop();

    Assert.assertThat(bundleProcessor.stopBundle(report, bundle), Matchers.equalTo(false));

    final ArgumentCaptor<MigrationException> captor =
        ArgumentCaptor.forClass(MigrationException.class);

    Mockito.verify(bundle).stop();
    Mockito.verify(report).recordOnFinalAttempt(captor.capture());

    final MigrationException me = captor.getValue();

    Assert.assertThat(
        me.getMessage(), Matchers.containsString("failed to stop bundle [" + NAME_VERSION + "]"));
    Assert.assertThat(me.getCause(), Matchers.sameInstance(e));
  }

  @Test
  public void testProcessMissingBundleAndPopulateTaskList() throws Exception {
    Mockito.doReturn(true).when(bundleProcessor).installBundle(context, report, NAME, LOCATION);

    bundleProcessor.processMissingBundleAndPopulateTaskList(
        context, new JsonBundle(NAME, VERSION, ID, STATE, LOCATION), tasks);

    Mockito.verify(tasks)
        .add(Mockito.eq(Operation.INSTALL), Mockito.eq(NAME_VERSION), Mockito.notNull());
    Mockito.verify(bundleProcessor).installBundle(context, report, NAME_VERSION, LOCATION);
  }

  @Test
  public void testProcessInstalledBundleAndPopulateTaskListWhenUninstalled() throws Exception {
    Mockito.doReturn(true).when(bundleProcessor).installBundle(context, report, bundle);
    Mockito.doReturn(Bundle.UNINSTALLED).when(bundle).getState();

    bundleProcessor.processInstalledBundleAndPopulateTaskList(context, bundle, tasks);

    Mockito.verify(tasks)
        .add(Mockito.eq(Operation.INSTALL), Mockito.eq(NAME_VERSION), Mockito.notNull());
    Mockito.verify(bundleProcessor).installBundle(context, report, bundle);
  }

  @Test
  public void testProcessInstalledBundleAndPopulateTaskListWhenInstalled() throws Exception {
    Mockito.doReturn(Bundle.INSTALLED).when(bundle).getState();

    bundleProcessor.processInstalledBundleAndPopulateTaskList(context, bundle, tasks);

    Mockito.verify(tasks, Mockito.never())
        .add(Mockito.any(), Mockito.eq(NAME_VERSION), Mockito.notNull());
    Mockito.verify(bundleProcessor, Mockito.never()).installBundle(context, report, bundle);
    Mockito.verify(bundleProcessor, Mockito.never()).startBundle(report, bundle);
    Mockito.verify(bundleProcessor, Mockito.never()).stopBundle(report, bundle);
  }

  @Test
  public void testProcessInstalledBundleAndPopulateTaskListWhenActive() throws Exception {
    Mockito.doReturn(true).when(bundleProcessor).stopBundle(report, bundle);
    Mockito.doReturn(Bundle.ACTIVE).when(bundle).getState();

    bundleProcessor.processInstalledBundleAndPopulateTaskList(context, bundle, tasks);

    Mockito.verify(tasks)
        .add(Mockito.eq(Operation.STOP), Mockito.eq(NAME_VERSION), Mockito.notNull());
    Mockito.verify(bundleProcessor).stopBundle(report, bundle);
  }

  @Test
  public void testProcessActiveBundleAndPopulateTaskListWhenUninstalled() throws Exception {
    Mockito.doReturn(true).when(bundleProcessor).installBundle(context, report, bundle);
    Mockito.doReturn(Bundle.UNINSTALLED).when(bundle).getState();

    bundleProcessor.processActiveBundleAndPopulateTaskList(context, bundle, tasks);

    Mockito.verify(tasks)
        .add(Mockito.eq(Operation.INSTALL), Mockito.eq(NAME_VERSION), Mockito.notNull());
    Mockito.verify(bundleProcessor).installBundle(context, report, bundle);
  }

  @Test
  public void testProcessActiveBundleAndPopulateTaskListWhenInstalled() throws Exception {
    Mockito.doReturn(true).when(bundleProcessor).startBundle(report, bundle);
    Mockito.doReturn(Bundle.INSTALLED).when(bundle).getState();

    bundleProcessor.processActiveBundleAndPopulateTaskList(context, bundle, tasks);

    Mockito.verify(tasks)
        .add(Mockito.eq(Operation.START), Mockito.eq(NAME_VERSION), Mockito.notNull());
    Mockito.verify(bundleProcessor).startBundle(report, bundle);
  }

  @Test
  public void testProcessActiveBundleAndPopulateTaskListWhenActive() throws Exception {
    Mockito.doReturn(Bundle.ACTIVE).when(bundle).getState();

    bundleProcessor.processActiveBundleAndPopulateTaskList(context, bundle, tasks);

    Mockito.verify(tasks, Mockito.never())
        .add(Mockito.any(), Mockito.eq(NAME_VERSION), Mockito.notNull());
    Mockito.verify(bundleProcessor, Mockito.never()).installBundle(context, report, bundle);
    Mockito.verify(bundleProcessor, Mockito.never()).startBundle(report, bundle);
    Mockito.verify(bundleProcessor, Mockito.never()).stopBundle(report, bundle);
  }

  @Test
  public void testProcessUninstalledBundleAndPopulateTaskListWhenUninstalled() throws Exception {
    Mockito.doReturn(Bundle.UNINSTALLED).when(bundle).getState();

    Assert.assertThat(
        bundleProcessor.processUninstalledBundleAndPopulateTaskList(bundle, tasks),
        Matchers.equalTo(false));

    Mockito.verify(tasks, Mockito.never())
        .add(Mockito.any(), Mockito.eq(NAME_VERSION), Mockito.notNull());
    Mockito.verify(bundleProcessor, Mockito.never()).installBundle(context, report, bundle);
    Mockito.verify(bundleProcessor, Mockito.never()).startBundle(report, bundle);
    Mockito.verify(bundleProcessor, Mockito.never()).stopBundle(report, bundle);
  }

  @Test
  public void testProcessUninstalledBundleAndPopulateTaskListWhenInstalled() throws Exception {
    Mockito.doReturn(true).when(bundleProcessor).uninstallBundle(report, bundle);
    Mockito.doReturn(Bundle.INSTALLED).when(bundle).getState();

    Assert.assertThat(
        bundleProcessor.processUninstalledBundleAndPopulateTaskList(bundle, tasks),
        Matchers.equalTo(true));

    Mockito.verify(tasks)
        .add(Mockito.eq(Operation.UNINSTALL), Mockito.eq(NAME_VERSION), Mockito.notNull());
    Mockito.verify(bundleProcessor).uninstallBundle(report, bundle);
  }

  @Test
  public void testProcessUninstalledBundleAndPopulateTaskListWhenActive() throws Exception {
    Mockito.doReturn(true).when(bundleProcessor).uninstallBundle(report, bundle);
    Mockito.doReturn(Bundle.ACTIVE).when(bundle).getState();

    Assert.assertThat(
        bundleProcessor.processUninstalledBundleAndPopulateTaskList(bundle, tasks),
        Matchers.equalTo(true));

    Mockito.verify(tasks)
        .add(Mockito.eq(Operation.UNINSTALL), Mockito.eq(NAME_VERSION), Mockito.notNull());
    Mockito.verify(bundleProcessor).uninstallBundle(report, bundle);
  }

  @Test
  public void testProcessBundleAndPopulateTaskListWhenItIsMissing() throws Exception {
    final JsonBundle jbundle2 = new JsonBundle(NAME2, VERSION2, ID2, Bundle.INSTALLED, LOCATION2);

    Mockito.doReturn(true)
        .when(bundleProcessor)
        .installBundle(context, report, NAME_VERSION2, LOCATION2);

    bundleProcessor.processBundleAndPopulateTaskList(context, jbundle2, null, tasks);

    Mockito.verify(tasks)
        .add(Mockito.eq(Operation.INSTALL), Mockito.contains(NAME_VERSION2), Mockito.notNull());
    Mockito.verify(bundleProcessor).installBundle(context, report, NAME_VERSION2, LOCATION2);
  }

  @Test
  public void testProcessBundleAndPopulateTaskListWhenItWasInstalledAndItIsUninstalled()
      throws Exception {
    final JsonBundle jbundle2 = new JsonBundle(NAME2, VERSION2, ID2, Bundle.INSTALLED, LOCATION2);

    Mockito.doReturn(true).when(bundleProcessor).installBundle(context, report, bundle2);

    bundleProcessor.processBundleAndPopulateTaskList(context, jbundle2, bundle2, tasks);

    Mockito.verify(tasks)
        .add(Mockito.eq(Operation.INSTALL), Mockito.contains(NAME_VERSION2), Mockito.notNull());
    Mockito.verify(bundleProcessor).installBundle(context, report, bundle2);
  }

  @Test
  public void testProcessBundleAndPopulateTaskListWhenItWasInstalledAndItIsInstalled()
      throws Exception {
    final JsonBundle jbundle3 = new JsonBundle(NAME3, VERSION3, ID3, Bundle.INSTALLED, LOCATION3);

    bundleProcessor.processBundleAndPopulateTaskList(context, jbundle3, bundle3, tasks);

    Mockito.verify(tasks, Mockito.never())
        .add(Mockito.any(), Mockito.eq(NAME_VERSION3), Mockito.notNull());
    Mockito.verify(bundleProcessor, Mockito.never()).installBundle(context, report, bundle3);
    Mockito.verify(bundleProcessor, Mockito.never()).startBundle(report, bundle3);
    Mockito.verify(bundleProcessor, Mockito.never()).stopBundle(report, bundle3);
  }

  @Test
  public void testProcessBundleAndPopulateTaskListWhenItWasInstalledAndItIsActive()
      throws Exception {
    final JsonBundle jbundle4 = new JsonBundle(NAME4, VERSION4, ID4, Bundle.INSTALLED, LOCATION4);

    Mockito.doReturn(true).when(bundleProcessor).stopBundle(report, bundle4);

    bundleProcessor.processBundleAndPopulateTaskList(context, jbundle4, bundle4, tasks);

    Mockito.verify(tasks)
        .add(Mockito.eq(Operation.STOP), Mockito.contains(NAME_VERSION4), Mockito.notNull());
    Mockito.verify(bundleProcessor).stopBundle(report, bundle4);
  }

  @Test
  public void testProcessBundleAndPopulateTaskListWhenItWasActiveAndItIsUninstalled()
      throws Exception {
    final JsonBundle jbundle2 = new JsonBundle(NAME2, VERSION2, ID2, Bundle.ACTIVE, LOCATION2);

    Mockito.doReturn(true).when(bundleProcessor).installBundle(context, report, bundle2);

    bundleProcessor.processBundleAndPopulateTaskList(context, jbundle2, bundle2, tasks);

    Mockito.verify(tasks)
        .add(Mockito.eq(Operation.INSTALL), Mockito.contains(NAME_VERSION2), Mockito.notNull());
    Mockito.verify(bundleProcessor).installBundle(context, report, bundle2);
  }

  @Test
  public void testProcessBundleAndPopulateTaskListWhenItWasActiveAndItIsInstalled()
      throws Exception {
    final JsonBundle jbundle3 = new JsonBundle(NAME3, VERSION3, ID3, Bundle.ACTIVE, LOCATION3);

    Mockito.doReturn(true).when(bundleProcessor).startBundle(report, bundle3);

    bundleProcessor.processBundleAndPopulateTaskList(context, jbundle3, bundle3, tasks);

    Mockito.verify(tasks)
        .add(Mockito.eq(Operation.START), Mockito.contains(NAME_VERSION3), Mockito.notNull());
    Mockito.verify(bundleProcessor).startBundle(report, bundle3);
  }

  @Test
  public void testProcessBundleAndPopulateTaskListWhenItWasActiveAndItIsActive() throws Exception {
    final JsonBundle jbundle4 = new JsonBundle(NAME4, VERSION4, ID4, Bundle.ACTIVE, LOCATION4);

    bundleProcessor.processBundleAndPopulateTaskList(context, jbundle4, bundle4, tasks);

    Mockito.verify(tasks, Mockito.never())
        .add(Mockito.any(), Mockito.contains(NAME_VERSION), Mockito.notNull());
    Mockito.verify(bundleProcessor, Mockito.never()).installBundle(context, report, bundle4);
    Mockito.verify(bundleProcessor, Mockito.never()).startBundle(report, bundle4);
    Mockito.verify(bundleProcessor, Mockito.never()).stopBundle(report, bundle4);
  }

  @Test
  public void testProcessBundleAndPopulateTaskListWhenItWasUninstalledAndItIsUninstalled()
      throws Exception {
    final JsonBundle jbundle2 = new JsonBundle(NAME2, VERSION2, ID2, Bundle.UNINSTALLED, LOCATION2);

    bundleProcessor.processBundleAndPopulateTaskList(context, jbundle2, bundle2, tasks);

    Mockito.verify(tasks, Mockito.never())
        .add(Mockito.any(), Mockito.contains(NAME_VERSION2), Mockito.notNull());
    Mockito.verify(bundleProcessor, Mockito.never()).installBundle(context, report, bundle2);
    Mockito.verify(bundleProcessor, Mockito.never()).startBundle(report, bundle2);
    Mockito.verify(bundleProcessor, Mockito.never()).stopBundle(report, bundle2);
  }

  @Test
  public void testProcessBundleAndPopulateTaskListWhenItWasUninstalledAndItIsInstalled()
      throws Exception {
    final JsonBundle jbundle3 = new JsonBundle(NAME3, VERSION3, ID3, Bundle.UNINSTALLED, LOCATION3);

    Mockito.doReturn(true).when(bundleProcessor).uninstallBundle(report, bundle3);

    bundleProcessor.processBundleAndPopulateTaskList(context, jbundle3, bundle3, tasks);

    Mockito.verify(tasks)
        .add(Mockito.eq(Operation.UNINSTALL), Mockito.contains(NAME_VERSION3), Mockito.notNull());
    Mockito.verify(bundleProcessor).uninstallBundle(report, bundle3);
  }

  @Test
  public void testProcessBundleAndPopulateTaskListWhenItWasUninstalledAndItIsActive()
      throws Exception {
    final JsonBundle jbundle4 = new JsonBundle(NAME4, VERSION4, ID4, Bundle.UNINSTALLED, LOCATION4);

    Mockito.doReturn(true).when(bundleProcessor).uninstallBundle(report, bundle4);

    bundleProcessor.processBundleAndPopulateTaskList(context, jbundle4, bundle4, tasks);

    Mockito.verify(tasks)
        .add(Mockito.eq(Operation.UNINSTALL), Mockito.contains(NAME_VERSION4), Mockito.notNull());
    Mockito.verify(bundleProcessor).uninstallBundle(report, bundle4);
  }

  @Test
  public void testProcessLeftoverBundlesAndPopulateTaskList() throws Exception {
    final Map<String, Bundle> bundles = ImmutableMap.of(NAME, bundle, NAME2, bundle2);

    Mockito.doReturn(true)
        .when(bundleProcessor)
        .processUninstalledBundleAndPopulateTaskList(bundle, tasks);
    Mockito.doReturn(false)
        .when(bundleProcessor)
        .processUninstalledBundleAndPopulateTaskList(bundle2, tasks);

    bundleProcessor.processLeftoverBundlesAndPopulateTaskList(bundles, tasks);

    Mockito.verify(bundleProcessor).processUninstalledBundleAndPopulateTaskList(bundle, tasks);
    Mockito.verify(bundleProcessor).processUninstalledBundleAndPopulateTaskList(bundle2, tasks);
  }

  @Test
  public void testProcessExportedBundlesAndPopulateTaskListWhenFoundInMemory() throws Exception {
    final JsonBundle jbundle = new JsonBundle(NAME, VERSION, ID, STATE, LOCATION);
    final Map<String, Bundle> bundles = new HashMap<>();

    bundles.put(NAME_VERSION, bundle);

    Mockito.doReturn(Stream.of(jbundle)).when(jprofile).bundles();
    Mockito.doNothing()
        .when(bundleProcessor)
        .processBundleAndPopulateTaskList(context, jbundle, bundle, tasks);

    bundleProcessor.processExportedBundlesAndPopulateTaskList(context, jprofile, bundles, tasks);

    Assert.assertThat(bundles.isEmpty(), Matchers.equalTo(true));

    Mockito.verify(bundleProcessor)
        .processBundleAndPopulateTaskList(context, jbundle, bundle, tasks);
  }

  @Test
  public void testProcessExportedBundlesAndPopulateTaskListWhenNotFoundInMemory() throws Exception {
    final JsonBundle jbundle = new JsonBundle(NAME, VERSION, ID, STATE, LOCATION);
    final Map<String, Bundle> bundles = new HashMap<>();

    bundles.put(NAME_VERSION2, bundle2);

    Mockito.doReturn(Stream.of(jbundle)).when(jprofile).bundles();
    Mockito.doNothing()
        .when(bundleProcessor)
        .processBundleAndPopulateTaskList(context, jbundle, null, tasks);

    bundleProcessor.processExportedBundlesAndPopulateTaskList(context, jprofile, bundles, tasks);

    Assert.assertThat(bundles.size(), Matchers.equalTo(1));

    Mockito.verify(bundleProcessor).processBundleAndPopulateTaskList(context, jbundle, null, tasks);
  }

  @Test
  public void testProcessBundlesAndPopulateTaskList() throws Exception {
    // because the processExportedBundlesAndPopulateTaskList has a side effect of modifying the
    // provided
    // map and
    // because Mockito ArgumentCapture only captures references to arguments and not a clone of
    // them we are forced to clone the jbundles received in the
    // processExportedBundlesAndPopulateTaskList() in the
    // doAnswer() so we can verify that exact content at the end
    final AtomicReference<Map<String, Bundle>> bundlesAtExported = new AtomicReference<>();

    Mockito.doReturn(new Bundle[] {bundle, bundle2, bundle4}).when(context).getBundles();
    Mockito.doAnswer(
            AdditionalAnswers.<BundleContext, JsonProfile, Map<String, Bundle>, TaskList>answerVoid(
                (context, jprofile, bundlesMap, tasks) -> {
                  // capture a clone of the args for later verification
                  bundlesAtExported.set(new LinkedHashMap<>(bundlesMap));
                  // simulate removing entries from the map
                  bundlesMap.remove(NAME_VERSION);
                  bundlesMap.remove(NAME_VERSION2);
                }))
        .when(bundleProcessor)
        .processExportedBundlesAndPopulateTaskList(
            Mockito.same(context), Mockito.same(jprofile), Mockito.notNull(), Mockito.same(tasks));
    Mockito.doNothing()
        .when(bundleProcessor)
        .processLeftoverBundlesAndPopulateTaskList(Mockito.notNull(), Mockito.same(tasks));

    bundleProcessor.processBundlesAndPopulateTaskList(context, jprofile, tasks);

    final ArgumentCaptor<Map<String, Bundle>> bundlesAtLeftover =
        ArgumentCaptor.forClass(Map.class);

    Mockito.verify(bundleProcessor)
        .processExportedBundlesAndPopulateTaskList(
            Mockito.same(context), Mockito.same(jprofile), Mockito.notNull(), Mockito.same(tasks));
    Mockito.verify(bundleProcessor)
        .processLeftoverBundlesAndPopulateTaskList(
            bundlesAtLeftover.capture(), Mockito.same(tasks));

    Assert.assertThat(
        bundlesAtExported.get(),
        Matchers.allOf(
            Matchers.aMapWithSize(3),
            Matchers.hasEntry(NAME_VERSION, bundle),
            Matchers.hasEntry(NAME_VERSION2, bundle2),
            Matchers.hasEntry(NAME_VERSION4, bundle4)));
    Assert.assertThat(
        bundlesAtLeftover.getValue(),
        Matchers.allOf(Matchers.aMapWithSize(1), Matchers.hasEntry(NAME_VERSION4, bundle4)));
  }
}
