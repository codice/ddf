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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.codice.ddf.admin.application.service.migratable.JsonBundle.SimpleState;
import org.codice.ddf.migration.MigrationException;
import org.hamcrest.Matchers;
import org.hamcrest.junit.internal.ThrowableMessageMatcher;
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
    Mockito.when(context.getBundles()).thenReturn(new Bundle[] {bundle3, bundle2, bundle4, bundle});

    Assert.assertThat(
        bundleProcessor.listBundles(context),
        Matchers.arrayContaining(
            Matchers.sameInstance(bundle3),
            Matchers.sameInstance(bundle2),
            Matchers.sameInstance(bundle4),
            Matchers.sameInstance(bundle)));
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
        Matchers.containsString("failed to install bundle [" + NAME + '/' + VERSION + "]"));
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
        Matchers.containsString("failed to install bundle [" + NAME + '/' + VERSION + "]"));
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
        Matchers.containsString("failed to install bundle [" + NAME + '/' + VERSION + "]"));
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
        Matchers.containsString("failed to uninstall bundle [" + NAME + '/' + VERSION + "]"));
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
        Matchers.containsString("failed to uninstall bundle [" + NAME + '/' + VERSION + "]"));
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
        Matchers.containsString("failed to uninstall bundle [" + NAME + '/' + VERSION + "]"));
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
        me.getMessage(),
        Matchers.containsString("failed to start bundle [" + NAME + '/' + VERSION + "]"));
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
        me.getMessage(),
        Matchers.containsString("failed to start bundle [" + NAME + '/' + VERSION + "]"));
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
        me.getMessage(),
        Matchers.containsString("failed to start bundle [" + NAME + '/' + VERSION + "]"));
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
        me.getMessage(),
        Matchers.containsString("failed to stop bundle [" + NAME + '/' + VERSION + "]"));
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
        me.getMessage(),
        Matchers.containsString("failed to stop bundle [" + NAME + '/' + VERSION + "]"));
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
        me.getMessage(),
        Matchers.containsString("failed to stop bundle [" + NAME + '/' + VERSION + "]"));
    Assert.assertThat(me.getCause(), Matchers.sameInstance(e));
  }

  @Test
  public void testProcessInstalledBundleWhenUninstalled() throws Exception {
    Mockito.doReturn(true).when(bundleProcessor).installBundle(context, report, bundle);

    bundleProcessor.processInstalledBundle(context, bundle, NAME, SimpleState.UNINSTALLED, tasks);

    Mockito.verify(tasks).add(Mockito.eq(Operation.INSTALL), Mockito.eq(NAME), Mockito.notNull());
    Mockito.verify(bundleProcessor).installBundle(context, report, bundle);
  }

  @Test
  public void testProcessInstalledBundleWhenInstalled() throws Exception {
    bundleProcessor.processInstalledBundle(context, bundle, NAME, SimpleState.INSTALLED, tasks);

    Mockito.verify(tasks, Mockito.never()).add(Mockito.any(), Mockito.eq(NAME), Mockito.notNull());
    Mockito.verify(bundleProcessor, Mockito.never()).installBundle(context, report, bundle);
    Mockito.verify(bundleProcessor, Mockito.never()).startBundle(report, bundle);
    Mockito.verify(bundleProcessor, Mockito.never()).stopBundle(report, bundle);
  }

  @Test
  public void testProcessInstalledBundleWhenActive() throws Exception {
    Mockito.doReturn(true).when(bundleProcessor).stopBundle(report, bundle);

    bundleProcessor.processInstalledBundle(context, bundle, NAME, SimpleState.ACTIVE, tasks);

    Mockito.verify(tasks).add(Mockito.eq(Operation.STOP), Mockito.eq(NAME), Mockito.notNull());
    Mockito.verify(bundleProcessor).stopBundle(report, bundle);
  }

  @Test
  public void testProcessActiveBundleWhenUninstalled() throws Exception {
    Mockito.doReturn(true).when(bundleProcessor).installBundle(context, report, bundle);

    bundleProcessor.processActiveBundle(context, bundle, NAME, SimpleState.UNINSTALLED, tasks);

    Mockito.verify(tasks).add(Mockito.eq(Operation.INSTALL), Mockito.eq(NAME), Mockito.notNull());
    Mockito.verify(tasks).increaseAttemptsFor(Operation.START);
    Mockito.verify(bundleProcessor).installBundle(context, report, bundle);
  }

  @Test
  public void testProcessActiveBundleWhenInstalled() throws Exception {
    Mockito.doReturn(true).when(bundleProcessor).startBundle(report, bundle);

    bundleProcessor.processActiveBundle(context, bundle, NAME, SimpleState.INSTALLED, tasks);

    Mockito.verify(tasks).add(Mockito.eq(Operation.START), Mockito.eq(NAME), Mockito.notNull());
    Mockito.verify(bundleProcessor).startBundle(report, bundle);
  }

  @Test
  public void testProcessActiveBundleWhenActive() throws Exception {
    bundleProcessor.processActiveBundle(context, bundle, NAME, SimpleState.ACTIVE, tasks);

    Mockito.verify(tasks, Mockito.never()).add(Mockito.any(), Mockito.eq(NAME), Mockito.notNull());
    Mockito.verify(bundleProcessor, Mockito.never()).installBundle(context, report, bundle);
    Mockito.verify(bundleProcessor, Mockito.never()).startBundle(report, bundle);
    Mockito.verify(bundleProcessor, Mockito.never()).stopBundle(report, bundle);
  }

  @Test
  public void testProcessUninstalledBundleWhenUninstalled() throws Exception {
    Assert.assertThat(
        bundleProcessor.processUninstalledBundle(bundle, NAME, SimpleState.UNINSTALLED, tasks),
        Matchers.equalTo(false));

    Mockito.verify(tasks, Mockito.never()).add(Mockito.any(), Mockito.eq(NAME), Mockito.notNull());
    Mockito.verify(bundleProcessor, Mockito.never()).installBundle(context, report, bundle);
    Mockito.verify(bundleProcessor, Mockito.never()).startBundle(report, bundle);
    Mockito.verify(bundleProcessor, Mockito.never()).stopBundle(report, bundle);
  }

  @Test
  public void testProcessUninstalledBundleWhenInstalled() throws Exception {
    Mockito.doReturn(true).when(bundleProcessor).uninstallBundle(report, bundle);

    Assert.assertThat(
        bundleProcessor.processUninstalledBundle(bundle, NAME, SimpleState.INSTALLED, tasks),
        Matchers.equalTo(true));

    Mockito.verify(tasks).add(Mockito.eq(Operation.UNINSTALL), Mockito.eq(NAME), Mockito.notNull());
    Mockito.verify(bundleProcessor).uninstallBundle(report, bundle);
  }

  @Test
  public void testProcessUninstalledBundleWhenActive() throws Exception {
    Mockito.doReturn(true).when(bundleProcessor).uninstallBundle(report, bundle);

    Assert.assertThat(
        bundleProcessor.processUninstalledBundle(bundle, NAME, SimpleState.ACTIVE, tasks),
        Matchers.equalTo(true));

    Mockito.verify(tasks).add(Mockito.eq(Operation.UNINSTALL), Mockito.eq(NAME), Mockito.notNull());
    Mockito.verify(bundleProcessor).uninstallBundle(report, bundle);
  }

  @Test
  public void testProcessBundleWhenItWasInstalledAndItIsUninstalled() throws Exception {
    final JsonBundle jbundle2 = new JsonBundle(NAME2, VERSION2, ID2, Bundle.INSTALLED, LOCATION2);

    Mockito.doReturn(true).when(bundleProcessor).installBundle(context, report, bundle2);

    bundleProcessor.processBundle(context, jbundle2, bundle2, tasks);

    Mockito.verify(tasks)
        .add(Mockito.eq(Operation.INSTALL), Mockito.contains(NAME2), Mockito.notNull());
    Mockito.verify(bundleProcessor).installBundle(context, report, bundle2);
  }

  @Test
  public void testProcessBundleWhenItWasInstalledAndItIsInstalled() throws Exception {
    final JsonBundle jbundle3 = new JsonBundle(NAME3, VERSION3, ID3, Bundle.INSTALLED, LOCATION3);

    bundleProcessor.processBundle(context, jbundle3, bundle3, tasks);

    Mockito.verify(tasks, Mockito.never()).add(Mockito.any(), Mockito.eq(NAME3), Mockito.notNull());
    Mockito.verify(bundleProcessor, Mockito.never()).installBundle(context, report, bundle3);
    Mockito.verify(bundleProcessor, Mockito.never()).startBundle(report, bundle3);
    Mockito.verify(bundleProcessor, Mockito.never()).stopBundle(report, bundle3);
  }

  @Test
  public void testProcessBundleWhenItWasInstalledAndItIsActive() throws Exception {
    final JsonBundle jbundle4 = new JsonBundle(NAME4, VERSION4, ID4, Bundle.INSTALLED, LOCATION4);

    Mockito.doReturn(true).when(bundleProcessor).stopBundle(report, bundle4);

    bundleProcessor.processBundle(context, jbundle4, bundle4, tasks);

    Mockito.verify(tasks)
        .add(Mockito.eq(Operation.STOP), Mockito.contains(NAME4), Mockito.notNull());
    Mockito.verify(bundleProcessor).stopBundle(report, bundle4);
  }

  @Test
  public void testProcessBundleWhenItWasActiveAndItIsUninstalled() throws Exception {
    final JsonBundle jbundle2 = new JsonBundle(NAME2, VERSION2, ID2, Bundle.ACTIVE, LOCATION2);

    Mockito.doReturn(true).when(bundleProcessor).installBundle(context, report, bundle2);

    bundleProcessor.processBundle(context, jbundle2, bundle2, tasks);

    Mockito.verify(tasks)
        .add(Mockito.eq(Operation.INSTALL), Mockito.contains(NAME2), Mockito.notNull());
    Mockito.verify(tasks).increaseAttemptsFor(Operation.START);
    Mockito.verify(bundleProcessor).installBundle(context, report, bundle2);
  }

  @Test
  public void testProcessBundleWhenItWasActiveAndItIsInstalled() throws Exception {
    final JsonBundle jbundle3 = new JsonBundle(NAME3, VERSION3, ID3, Bundle.ACTIVE, LOCATION3);

    Mockito.doReturn(true).when(bundleProcessor).startBundle(report, bundle3);

    bundleProcessor.processBundle(context, jbundle3, bundle3, tasks);

    Mockito.verify(tasks)
        .add(Mockito.eq(Operation.START), Mockito.contains(NAME3), Mockito.notNull());
    Mockito.verify(bundleProcessor).startBundle(report, bundle3);
  }

  @Test
  public void testProcessBundleWhenItWasActiveAndItIsActive() throws Exception {
    final JsonBundle jbundle4 = new JsonBundle(NAME4, VERSION4, ID4, Bundle.ACTIVE, LOCATION4);

    bundleProcessor.processBundle(context, jbundle4, bundle4, tasks);

    Mockito.verify(tasks, Mockito.never())
        .add(Mockito.any(), Mockito.contains(NAME), Mockito.notNull());
    Mockito.verify(bundleProcessor, Mockito.never()).installBundle(context, report, bundle4);
    Mockito.verify(bundleProcessor, Mockito.never()).startBundle(report, bundle4);
    Mockito.verify(bundleProcessor, Mockito.never()).stopBundle(report, bundle4);
  }

  @Test
  public void testProcessBundleWhenItWasUninstalledAndItIsUninstalled() throws Exception {
    final JsonBundle jbundle2 = new JsonBundle(NAME2, VERSION2, ID2, Bundle.UNINSTALLED, LOCATION2);

    bundleProcessor.processBundle(context, jbundle2, bundle2, tasks);

    Mockito.verify(tasks, Mockito.never())
        .add(Mockito.any(), Mockito.contains(NAME2), Mockito.notNull());
    Mockito.verify(bundleProcessor, Mockito.never()).installBundle(context, report, bundle2);
    Mockito.verify(bundleProcessor, Mockito.never()).startBundle(report, bundle2);
    Mockito.verify(bundleProcessor, Mockito.never()).stopBundle(report, bundle2);
  }

  @Test
  public void testProcessBundleWhenItWasUninstalledAndItIsInstalled() throws Exception {
    final JsonBundle jbundle3 = new JsonBundle(NAME3, VERSION3, ID3, Bundle.UNINSTALLED, LOCATION3);

    Mockito.doReturn(true).when(bundleProcessor).uninstallBundle(report, bundle3);

    bundleProcessor.processBundle(context, jbundle3, bundle3, tasks);

    Mockito.verify(tasks)
        .add(Mockito.eq(Operation.UNINSTALL), Mockito.contains(NAME3), Mockito.notNull());
    Mockito.verify(bundleProcessor).uninstallBundle(report, bundle3);
  }

  @Test
  public void testProcessBundleItWasUninstalledAndItIsctive() throws Exception {
    final JsonBundle jbundle4 = new JsonBundle(NAME4, VERSION4, ID4, Bundle.UNINSTALLED, LOCATION4);

    Mockito.doReturn(true).when(bundleProcessor).uninstallBundle(report, bundle4);

    bundleProcessor.processBundle(context, jbundle4, bundle4, tasks);

    Mockito.verify(tasks)
        .add(Mockito.eq(Operation.UNINSTALL), Mockito.contains(NAME4), Mockito.notNull());
    Mockito.verify(bundleProcessor).uninstallBundle(report, bundle4);
  }

  @Test
  public void testProcessBundleWhenItWasNotInstalledAndItIsUninstalled() throws Exception {
    bundleProcessor.processBundle(context, null, bundle2, tasks);

    Mockito.verify(tasks, Mockito.never())
        .add(Mockito.any(), Mockito.contains(NAME2), Mockito.notNull());
    Mockito.verify(bundleProcessor, Mockito.never()).installBundle(context, report, bundle2);
    Mockito.verify(bundleProcessor, Mockito.never()).startBundle(report, bundle2);
    Mockito.verify(bundleProcessor, Mockito.never()).stopBundle(report, bundle2);
  }

  @Test
  public void testProcessBundleWhenItWasNotInstalledAndItIsInstalled() throws Exception {
    Mockito.doReturn(true).when(bundleProcessor).uninstallBundle(report, bundle3);

    bundleProcessor.processBundle(context, null, bundle3, tasks);

    Mockito.verify(tasks).add(Mockito.any(), Mockito.contains(NAME3), Mockito.notNull());
    Mockito.verify(bundleProcessor).uninstallBundle(report, bundle3);
  }

  @Test
  public void testProcessBundleItWasNotInstalledAndItIsActive() throws Exception {
    Mockito.doReturn(true).when(bundleProcessor).uninstallBundle(report, bundle4);

    bundleProcessor.processBundle(context, null, bundle4, tasks);

    Mockito.verify(tasks)
        .add(Mockito.eq(Operation.UNINSTALL), Mockito.contains(NAME4), Mockito.notNull());
    Mockito.verify(bundleProcessor).uninstallBundle(report, bundle4);
  }

  @Test
  public void testProcessLeftOverExportedBundlesWhenWasUninstalled() throws Exception {
    final JsonBundle jbundle2 = new JsonBundle(NAME2, VERSION2, ID2, Bundle.UNINSTALLED, LOCATION2);
    final Map<String, JsonBundle> jbundles = ImmutableMap.of(NAME, jbundle2);

    Mockito.doReturn(bundle2).when(context).getBundle(LOCATION2);
    Mockito.doNothing().when(bundleProcessor).processBundle(context, jbundle2, bundle2, tasks);

    Assert.assertThat(
        bundleProcessor.processLeftoverExportedBundles(context, report, jbundles, tasks),
        Matchers.equalTo(true));

    Mockito.verify(context, Mockito.never()).getBundle(LOCATION2);
    Mockito.verify(bundleProcessor, Mockito.never())
        .processBundle(context, jbundle2, bundle2, tasks);
  }

  @Test
  public void testProcessLeftOverExportedBundlesWhenWasInstalled() throws Exception {
    final JsonBundle jbundle3 = new JsonBundle(NAME3, VERSION3, ID3, Bundle.INSTALLED, LOCATION3);
    final Map<String, JsonBundle> jbundles = ImmutableMap.of(NAME, jbundle3);

    Mockito.doReturn(bundle3).when(context).getBundle(LOCATION3);
    Mockito.doNothing().when(bundleProcessor).processBundle(context, jbundle3, bundle3, tasks);

    Assert.assertThat(
        bundleProcessor.processLeftoverExportedBundles(context, report, jbundles, tasks),
        Matchers.equalTo(true));

    Mockito.verify(context).getBundle(LOCATION3);
    Mockito.verify(bundleProcessor).processBundle(context, jbundle3, bundle3, tasks);
  }

  @Test
  public void testProcessLeftOverExportedBundlesWhenWasActive() throws Exception {
    final JsonBundle jbundle4 = new JsonBundle(NAME4, VERSION4, ID4, Bundle.ACTIVE, LOCATION4);
    final Map<String, JsonBundle> jbundles = ImmutableMap.of(NAME, jbundle4);

    Mockito.doReturn(bundle4).when(context).getBundle(LOCATION4);
    Mockito.doNothing().when(bundleProcessor).processBundle(context, jbundle4, bundle4, tasks);

    Assert.assertThat(
        bundleProcessor.processLeftoverExportedBundles(context, report, jbundles, tasks),
        Matchers.equalTo(true));

    Mockito.verify(context).getBundle(LOCATION4);
    Mockito.verify(bundleProcessor).processBundle(context, jbundle4, bundle4, tasks);
  }

  @Test
  public void testProcessLeftOverExportedBundlesWhenNotFoundInMemory() throws Exception {
    final JsonBundle jbundle = new JsonBundle(NAME, VERSION, ID, Bundle.ACTIVE, LOCATION);
    final Map<String, JsonBundle> jbundles = ImmutableMap.of(NAME, jbundle);

    Mockito.doReturn(null).when(context).getBundle(LOCATION);
    Mockito.doNothing().when(bundleProcessor).processBundle(context, jbundle, bundle, tasks);

    Assert.assertThat(
        bundleProcessor.processLeftoverExportedBundles(context, report, jbundles, tasks),
        Matchers.equalTo(false));

    final ArgumentCaptor<MigrationException> captor =
        ArgumentCaptor.forClass(MigrationException.class);

    Mockito.verify(context).getBundle(LOCATION);
    Mockito.verify(bundleProcessor, Mockito.never())
        .processBundle(
            Mockito.same(context), Mockito.same(jbundle), Mockito.any(), Mockito.same(tasks));
    Mockito.verify(report).record(captor.capture());

    Assert.assertThat(
        captor.getValue(),
        ThrowableMessageMatcher.hasMessage(
            Matchers.containsString("failed to retrieve bundle [" + NAME)));
  }

  @Test
  public void testProcessLeftOverExportedBundlesWhenNothingLeft() throws Exception {
    final Map<String, JsonBundle> jbundles = Collections.emptyMap();

    Assert.assertThat(
        bundleProcessor.processLeftoverExportedBundles(context, report, jbundles, tasks),
        Matchers.equalTo(true));

    Mockito.verify(context, Mockito.never()).getBundle(Mockito.anyString());
    Mockito.verify(bundleProcessor, Mockito.never())
        .processBundle(Mockito.same(context), Mockito.any(), Mockito.any(), Mockito.same(tasks));
  }

  @Test
  public void testProcessMemoryBundlesWhenFoundInExport() throws Exception {
    final JsonBundle jbundle = new JsonBundle(NAME, VERSION, ID, STATE, LOCATION);
    final Map<String, JsonBundle> jbundles = new HashMap<>();

    jbundles.put(jbundle.getFullName(), jbundle);

    Mockito.doReturn(new Bundle[] {bundle}).when(context).getBundles();
    Mockito.doNothing().when(bundleProcessor).processBundle(context, jbundle, bundle, tasks);

    bundleProcessor.processMemoryBundles(context, jbundles, tasks);

    Assert.assertThat(jbundles.isEmpty(), Matchers.equalTo(true));

    Mockito.verify(context).getBundles();
    Mockito.verify(bundleProcessor).processBundle(context, jbundle, bundle, tasks);
  }

  @Test
  public void testProcessMemoryBundlesWhenNotFoundInExport() throws Exception {
    final JsonBundle jbundle = new JsonBundle(NAME, VERSION, ID, STATE, LOCATION);
    final Map<String, JsonBundle> jbundles = new HashMap<>();

    jbundles.put(jbundle.getFullName(), jbundle);

    Mockito.doReturn(new Bundle[] {bundle2}).when(context).getBundles();
    Mockito.doNothing().when(bundleProcessor).processBundle(context, null, bundle2, tasks);

    bundleProcessor.processMemoryBundles(context, jbundles, tasks);

    Assert.assertThat(
        jbundles,
        Matchers.allOf(
            Matchers.aMapWithSize(1), Matchers.hasEntry(jbundle.getFullName(), jbundle)));

    Mockito.verify(context).getBundles();
    Mockito.verify(bundleProcessor).processBundle(context, null, bundle2, tasks);
  }

  @Test
  public void testProcessMemoryBundlesWhenNothingsFoundInMemory() throws Exception {
    final JsonBundle jbundle = new JsonBundle(NAME, VERSION, ID, STATE, LOCATION);
    final Map<String, JsonBundle> jbundles = new HashMap<>();

    jbundles.put(jbundle.getFullName(), jbundle);

    Mockito.doReturn(new Bundle[] {}).when(context).getBundles();
    Mockito.doNothing()
        .when(bundleProcessor)
        .processBundle(Mockito.same(context), Mockito.any(), Mockito.any(), Mockito.same(tasks));

    bundleProcessor.processMemoryBundles(context, jbundles, tasks);

    Assert.assertThat(
        jbundles,
        Matchers.allOf(
            Matchers.aMapWithSize(1), Matchers.hasEntry(jbundle.getFullName(), jbundle)));

    Mockito.verify(context).getBundles();
    Mockito.verify(bundleProcessor, Mockito.never())
        .processBundle(Mockito.same(context), Mockito.any(), Mockito.any(), Mockito.same(tasks));
  }

  @Test
  public void testProcessBundles() throws Exception {
    final JsonBundle jbundle = new JsonBundle(NAME, VERSION, ID, STATE, LOCATION);
    final JsonBundle jbundle2 = new JsonBundle(NAME2, VERSION2, ID2, STATE2, LOCATION2);
    final JsonBundle jbundle4 = new JsonBundle(NAME4, VERSION4, ID4, STATE4, LOCATION4);
    // because the processMemoryBundles has a side effect of modifying the provided map and
    // because Mockito ArgumentCapture only captures references to arguments and not a clone of
    // them we are forced to clone the jbundles received in the processMemoryBundles() in the
    // doAnswer() so we can verify that exact content at the end
    final AtomicReference<Map<String, JsonBundle>> jbundlesAtMemory = new AtomicReference<>();

    Mockito.doReturn(Stream.of(jbundle2, jbundle4, jbundle)).when(jprofile).bundles();
    Mockito.doAnswer(
            AdditionalAnswers.<BundleContext, Map<String, JsonBundle>, TaskList>answerVoid(
                (context, jbundlesMap, tasks) -> {
                  // capture a clone of the args for later verification
                  jbundlesAtMemory.set(new LinkedHashMap<>(jbundlesMap));
                  jbundlesMap.remove(
                      jbundle2.getFullName()); // simulate removing entries from the map
                }))
        .when(bundleProcessor)
        .processMemoryBundles(Mockito.same(context), Mockito.notNull(), Mockito.same(tasks));
    Mockito.doReturn(true)
        .when(bundleProcessor)
        .processLeftoverExportedBundles(
            Mockito.same(context), Mockito.same(report), Mockito.notNull(), Mockito.same(tasks));

    Assert.assertThat(
        bundleProcessor.processBundles(context, report, jprofile, tasks), Matchers.equalTo(true));

    final ArgumentCaptor<Map<String, JsonBundle>> jbundlesAtLeftover =
        ArgumentCaptor.forClass(Map.class);

    Mockito.verify(bundleProcessor)
        .processMemoryBundles(Mockito.same(context), Mockito.notNull(), Mockito.same(tasks));
    Mockito.verify(bundleProcessor)
        .processLeftoverExportedBundles(
            Mockito.same(context),
            Mockito.same(report),
            jbundlesAtLeftover.capture(),
            Mockito.same(tasks));
    Mockito.verify(jprofile).bundles();

    Assert.assertThat(
        jbundlesAtMemory.get(),
        Matchers.allOf(
            Matchers.aMapWithSize(3),
            Matchers.hasEntry(jbundle.getFullName(), jbundle),
            Matchers.hasEntry(jbundle2.getFullName(), jbundle2),
            Matchers.hasEntry(jbundle4.getFullName(), jbundle4)));
    Assert.assertThat(
        jbundlesAtLeftover.getValue(),
        Matchers.allOf(
            Matchers.aMapWithSize(2),
            Matchers.hasEntry(jbundle.getFullName(), jbundle),
            Matchers.hasEntry(jbundle4.getFullName(), jbundle4)));
  }

  @Test
  public void testProcessBundlesWhenUnableToFindInstalledBundles() throws Exception {
    final JsonBundle jbundle = new JsonBundle(NAME, VERSION, ID, STATE, LOCATION);
    final JsonBundle jbundle2 = new JsonBundle(NAME2, VERSION2, ID2, STATE2, LOCATION2);
    final JsonBundle jbundle4 = new JsonBundle(NAME4, VERSION4, ID4, STATE4, LOCATION4);
    // because the processMemoryBundles has a side effect of modifying the provided map and
    // because Mockito ArgumentCapture only captures references to arguments and not a clone of
    // them we are forced to clone the jbundles received in the processMemoryBundles() in the
    // doAnswer() so we can verify that exact content at the end
    final AtomicReference<Map<String, JsonBundle>> jbundlesAtMemory = new AtomicReference<>();

    Mockito.doReturn(Stream.of(jbundle4, jbundle2, jbundle)).when(jprofile).bundles();
    Mockito.doAnswer(
            AdditionalAnswers.<BundleContext, Map<String, JsonBundle>, TaskList>answerVoid(
                (context, jbundlesMap, tasks) -> {
                  // capture a clone of the args for later verification
                  jbundlesAtMemory.set(new LinkedHashMap<>(jbundlesMap));
                  jbundlesMap.remove(
                      jbundle2.getFullName()); // simulate removing entries from the map
                }))
        .when(bundleProcessor)
        .processMemoryBundles(Mockito.same(context), Mockito.notNull(), Mockito.same(tasks));
    Mockito.doReturn(false)
        .when(bundleProcessor)
        .processLeftoverExportedBundles(
            Mockito.same(context), Mockito.same(report), Mockito.notNull(), Mockito.same(tasks));

    Assert.assertThat(
        bundleProcessor.processBundles(context, report, jprofile, tasks), Matchers.equalTo(false));

    final ArgumentCaptor<Map<String, JsonBundle>> jbundlesAtLeftover =
        ArgumentCaptor.forClass(Map.class);

    Mockito.verify(bundleProcessor)
        .processMemoryBundles(Mockito.same(context), Mockito.notNull(), Mockito.same(tasks));
    Mockito.verify(bundleProcessor)
        .processLeftoverExportedBundles(
            Mockito.same(context),
            Mockito.same(report),
            jbundlesAtLeftover.capture(),
            Mockito.same(tasks));
    Mockito.verify(jprofile).bundles();

    Assert.assertThat(
        jbundlesAtMemory.get(),
        Matchers.allOf(
            Matchers.aMapWithSize(3),
            Matchers.hasEntry(jbundle.getFullName(), jbundle),
            Matchers.hasEntry(jbundle2.getFullName(), jbundle2),
            Matchers.hasEntry(jbundle4.getFullName(), jbundle4)));
    Assert.assertThat(
        jbundlesAtLeftover.getValue(),
        Matchers.allOf(
            Matchers.aMapWithSize(2),
            Matchers.hasEntry(jbundle.getFullName(), jbundle),
            Matchers.hasEntry(jbundle4.getFullName(), jbundle4)));
  }
}
