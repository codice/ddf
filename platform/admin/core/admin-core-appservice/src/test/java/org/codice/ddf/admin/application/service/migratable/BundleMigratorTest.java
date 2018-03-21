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

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

public class BundleMigratorTest {

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
  private static final int STATE4 = Bundle.STARTING;
  private static final String LOCATION = "test.location";
  private static final String LOCATION2 = "test.location2";
  private static final String LOCATION3 = "test.location3";
  private static final String LOCATION4 = "test.location4";

  private final Bundle bundle = Mockito.mock(Bundle.class);

  private final Bundle bundle2 = Mockito.mock(Bundle.class);

  private final Bundle bundle3 = Mockito.mock(Bundle.class);

  private final Bundle bundle4 = Mockito.mock(Bundle.class);

  private final JsonBundle jbundle = new JsonBundle(NAME, VERSION, ID, STATE, LOCATION);

  private final JsonBundle jbundle2 = new JsonBundle(NAME2, VERSION2, ID2, STATE2, LOCATION2);

  private final JsonBundle jbundle3 = new JsonBundle(NAME3, VERSION3, ID3, STATE3, LOCATION3);

  private final TaskList tasks = Mockito.mock(TaskList.class);

  private final BundleProcessor bundleProcessor = Mockito.mock(BundleProcessor.class);

  private final BundleMigrator bundleMigrator =
      Mockito.mock(
          BundleMigrator.class,
          Mockito.withSettings()
              .useConstructor(bundleProcessor)
              .defaultAnswer(Answers.CALLS_REAL_METHODS));

  private final BundleContext context = Mockito.mock(BundleContext.class);

  private final ProfileMigrationReport report = Mockito.mock(ProfileMigrationReport.class);

  private final JsonProfile jprofile = Mockito.mock(JsonProfile.class);

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setup() throws Exception {
    Mockito.doReturn(context).when(bundleMigrator).getBundleContext();

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

    Mockito.when(bundleMigrator.newTaskList(report)).thenReturn(tasks);
  }

  @Test
  public void testConstructorWithNullProcessor() throws Exception {
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage(Matchers.containsString("null bundle processor"));

    new BundleMigrator(null);
  }

  @Test
  public void testExportBundles() throws Exception {
    Mockito.when(bundleProcessor.listBundles(context))
        .thenReturn(new Bundle[] {bundle2, bundle3, bundle});

    Assert.assertThat(
        bundleMigrator.exportBundles(), Matchers.contains(jbundle2, jbundle3, jbundle));
  }

  @Test
  public void testImportBundlesWithTasks() throws Exception {
    Mockito.doNothing()
        .when(bundleProcessor)
        .processBundlesAndPopulateTaskList(context, jprofile, tasks);
    Mockito.doReturn(false, true).when(tasks).isEmpty();
    Mockito.doReturn(true).when(tasks).execute();

    Assert.assertThat(bundleMigrator.importBundles(report, jprofile), Matchers.equalTo(true));

    Mockito.verify(bundleProcessor, Mockito.times(2))
        .processBundlesAndPopulateTaskList(context, jprofile, tasks);
    Mockito.verify(tasks, Mockito.times(2)).isEmpty();
    Mockito.verify(tasks).execute();
  }

  @Test
  public void testImportBundlesWithNoTasks() throws Exception {
    Mockito.doNothing()
        .when(bundleProcessor)
        .processBundlesAndPopulateTaskList(context, jprofile, tasks);
    Mockito.doReturn(true).when(tasks).isEmpty();

    Assert.assertThat(bundleMigrator.importBundles(report, jprofile), Matchers.equalTo(true));

    Mockito.verify(bundleProcessor).processBundlesAndPopulateTaskList(context, jprofile, tasks);
    Mockito.verify(tasks).isEmpty();
    Mockito.verify(tasks, Mockito.never()).execute();
  }

  @Test
  public void testImportBundlesWhenFailedToExecuteTasks() throws Exception {
    Mockito.doNothing()
        .when(bundleProcessor)
        .processBundlesAndPopulateTaskList(context, jprofile, tasks);
    Mockito.doReturn(false).when(tasks).isEmpty();
    Mockito.doReturn(false).when(tasks).execute();

    Assert.assertThat(bundleMigrator.importBundles(report, jprofile), Matchers.equalTo(false));

    Mockito.verify(bundleProcessor).processBundlesAndPopulateTaskList(context, jprofile, tasks);
    Mockito.verify(tasks).isEmpty();
    Mockito.verify(tasks).execute();
  }
}
