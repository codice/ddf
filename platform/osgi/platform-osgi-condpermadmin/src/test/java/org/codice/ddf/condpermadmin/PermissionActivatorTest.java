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
package org.codice.ddf.condpermadmin;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.AllPermission;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.eclipse.osgi.internal.permadmin.EquinoxSecurityManager;
import org.eclipse.osgi.internal.permadmin.SecurityAdmin;
import org.eclipse.osgi.storage.PermissionData;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.osgi.framework.BundleContext;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;

public class PermissionActivatorTest {
  private static final String BUNDLE_1 = "bundle-1";
  private static final String BUNDLE_2 = "bundle-2";
  private static final String PERMISSION_1_TYPE = "org.osgi.framework.ServicePermission";
  private static final String PERMISSION_1_NAME = "*";
  private static final String PERMISSION_1_ACTIONS = "GET";
  private static final String PERMISSION_1 =
      PERMISSION_1_TYPE + " \"" + PERMISSION_1_NAME + "\", \"" + PERMISSION_1_ACTIONS + "\"";

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void setup() throws IOException {
    temporaryFolder.newFolder("security");
    System.setProperty("ddf.home", temporaryFolder.getRoot().getAbsolutePath());
  }

  @Test
  public void testStartGrant() throws Exception {
    final PermissionActivator permissionActivator = createPermissionActivator("/grant.policy");

    ConditionalPermissionAdmin conditionalPermissionAdmin =
        permissionActivator.getConditionalPermissionAdmin(null);
    List<ConditionalPermissionInfo> conditionalPermissionInfos =
        conditionalPermissionAdmin.newConditionalPermissionUpdate().getConditionalPermissionInfos();
    ConditionalPermissionInfo lastInfo =
        conditionalPermissionInfos.get(conditionalPermissionInfos.size() - 1);
    assertThat(lastInfo.getPermissionInfos().length, is(1));
    assertThat(lastInfo.getPermissionInfos()[0].getType(), is(AllPermission.class.getName()));
    assertThat(lastInfo.getAccessDecision(), is("allow"));

    ConditionalPermissionInfo beforeLastInfo =
        conditionalPermissionInfos.get(conditionalPermissionInfos.size() - 2);
    assertThat(beforeLastInfo.getAccessDecision(), is("deny"));

    ConditionalPermissionInfo beforeDeny =
        conditionalPermissionInfos.get(conditionalPermissionInfos.size() - 3);
    assertThat(beforeDeny.getAccessDecision(), is("allow"));
  }

  @Test
  public void testStartDeny() throws Exception {
    final PermissionActivator permissionActivator = createPermissionActivator("/deny.policy");

    ConditionalPermissionAdmin conditionalPermissionAdmin =
        permissionActivator.getConditionalPermissionAdmin(null);
    List<ConditionalPermissionInfo> conditionalPermissionInfos =
        conditionalPermissionAdmin.newConditionalPermissionUpdate().getConditionalPermissionInfos();
    ConditionalPermissionInfo lastInfo =
        conditionalPermissionInfos.get(conditionalPermissionInfos.size() - 1);
    assertThat(lastInfo.getPermissionInfos().length, is(1));
    assertThat(lastInfo.getPermissionInfos()[0].getType(), is(AllPermission.class.getName()));
    assertThat(lastInfo.getAccessDecision(), is("deny"));

    ConditionalPermissionInfo beforeLastInfo =
        conditionalPermissionInfos.get(conditionalPermissionInfos.size() - 2);
    assertThat(beforeLastInfo.getAccessDecision(), is("allow"));

    ConditionalPermissionInfo beforeDeny =
        conditionalPermissionInfos.get(conditionalPermissionInfos.size() - 3);
    assertThat(beforeDeny.getAccessDecision(), is("deny"));
  }

  @Test
  public void testStartNone() throws Exception {
    final PermissionActivator permissionActivator = createPermissionActivator("/none.policy");

    ConditionalPermissionAdmin conditionalPermissionAdmin =
        permissionActivator.getConditionalPermissionAdmin(null);
    List<ConditionalPermissionInfo> conditionalPermissionInfos =
        conditionalPermissionAdmin.newConditionalPermissionUpdate().getConditionalPermissionInfos();
    ConditionalPermissionInfo lastInfo =
        conditionalPermissionInfos.get(conditionalPermissionInfos.size() - 1);
    assertThat(lastInfo.getPermissionInfos().length, is(1));
    assertThat(lastInfo.getPermissionInfos()[0].getType(), is(AllPermission.class.getName()));
    assertThat(lastInfo.getAccessDecision(), is("allow"));

    ConditionalPermissionInfo beforeLastInfo =
        conditionalPermissionInfos.get(conditionalPermissionInfos.size() - 2);
    assertThat(beforeLastInfo.getAccessDecision(), is("deny"));

    ConditionalPermissionInfo beforeDeny =
        conditionalPermissionInfos.get(conditionalPermissionInfos.size() - 3);
    assertThat(beforeDeny.getAccessDecision(), is("allow"));
  }

  @Test
  public void testStartNoPolicy() throws Exception {
    PermissionActivatorForTest permissionActivator = new PermissionActivatorForTest();
    permissionActivator.start(mock(BundleContext.class));
    ConditionalPermissionAdmin conditionalPermissionAdmin =
        permissionActivator.getConditionalPermissionAdmin(null);
    List<ConditionalPermissionInfo> conditionalPermissionInfos =
        conditionalPermissionAdmin.newConditionalPermissionUpdate().getConditionalPermissionInfos();
    assertThat(conditionalPermissionInfos.size(), is(0));
  }

  @Test
  public void testStartAll() throws Exception {
    createPermissionActivator("/deny.policy", "/grant.policy", "/none.policy");
  }

  @Test
  public void policyEntryWithNoPermissionsIsValid() throws Exception {
    createPermissionActivator("/emptyEntry.policy");
  }

  @Test(expected = RuntimeException.class)
  public void malformedPolicyFile() throws Exception {
    createPermissionActivator("/badformat.policy");
  }

  @Test
  public void testGrantPermissionWithStartGrant() throws Exception {
    final PermissionActivator permissionActivator = createPermissionActivator("/grant.policy");
    final ConditionalPermissionAdmin conditionalPermissionAdmin =
        permissionActivator.getConditionalPermissionAdmin(null);
    final List<ConditionalPermissionInfo> initialConditionalPermissionInfos =
        conditionalPermissionAdmin.newConditionalPermissionUpdate().getConditionalPermissionInfos();

    permissionActivator.grantPermission(BUNDLE_1, PERMISSION_1);

    final List<ConditionalPermissionInfo> conditionalPermissionInfos =
        conditionalPermissionAdmin.newConditionalPermissionUpdate().getConditionalPermissionInfos();

    // the new permission should be at the top and the rest identical to the initial ones
    assertThat(conditionalPermissionInfos.size(), is(initialConditionalPermissionInfos.size() + 1));
    for (int i = 1; i < conditionalPermissionInfos.size(); i++) {
      assertThat(
          conditionalPermissionInfos.get(i), is(initialConditionalPermissionInfos.get(i - 1)));
    }
    final ConditionalPermissionInfo newInfo = conditionalPermissionInfos.get(0);

    assertThat(newInfo.getPermissionInfos().length, is(1));
    assertThat(newInfo.getPermissionInfos()[0].getType(), is(PERMISSION_1_TYPE));
    assertThat(newInfo.getPermissionInfos()[0].getName(), is(PERMISSION_1_NAME));
    assertThat(newInfo.getPermissionInfos()[0].getActions(), is(PERMISSION_1_ACTIONS));
    assertThat(newInfo.getConditionInfos().length, is(1));
    assertThat(newInfo.getConditionInfos()[0].getArgs(), Matchers.arrayContaining(BUNDLE_1));
    assertThat(newInfo.getAccessDecision(), is("allow"));
  }

  @Test
  public void testGrantPermissionWithStartDeny() throws Exception {
    final PermissionActivator permissionActivator = createPermissionActivator("/deny.policy");
    final ConditionalPermissionAdmin conditionalPermissionAdmin =
        permissionActivator.getConditionalPermissionAdmin(null);
    final List<ConditionalPermissionInfo> initialConditionalPermissionInfos =
        conditionalPermissionAdmin.newConditionalPermissionUpdate().getConditionalPermissionInfos();

    permissionActivator.grantPermission(BUNDLE_1, PERMISSION_1);

    final List<ConditionalPermissionInfo> conditionalPermissionInfos =
        conditionalPermissionAdmin.newConditionalPermissionUpdate().getConditionalPermissionInfos();

    // the new permission should be before the last one and the beginning ones identical to the
    // initial one
    assertThat(conditionalPermissionInfos.size(), is(initialConditionalPermissionInfos.size() + 1));
    for (int i = 0; i < conditionalPermissionInfos.size() - 2; i++) {
      assertThat(conditionalPermissionInfos.get(i), is(initialConditionalPermissionInfos.get(i)));
    }
    assertThat(
        conditionalPermissionInfos.get(conditionalPermissionInfos.size() - 1),
        is(initialConditionalPermissionInfos.get(initialConditionalPermissionInfos.size() - 1)));
    final ConditionalPermissionInfo newInfo =
        conditionalPermissionInfos.get(conditionalPermissionInfos.size() - 2);

    assertThat(newInfo.getPermissionInfos().length, is(1));
    assertThat(newInfo.getPermissionInfos()[0].getType(), is(PERMISSION_1_TYPE));
    assertThat(newInfo.getPermissionInfos()[0].getName(), is(PERMISSION_1_NAME));
    assertThat(newInfo.getPermissionInfos()[0].getActions(), is(PERMISSION_1_ACTIONS));
    assertThat(newInfo.getConditionInfos().length, is(1));
    assertThat(newInfo.getConditionInfos()[0].getArgs(), Matchers.arrayContaining(BUNDLE_1));
    assertThat(newInfo.getAccessDecision(), is("allow"));
  }

  @Test
  public void testGrantPermissionWhenConditionalPermissionAlreadyExist() throws Exception {
    final PermissionActivator permissionActivator = createPermissionActivator("/grant.policy");
    final ConditionalPermissionAdmin conditionalPermissionAdmin =
        permissionActivator.getConditionalPermissionAdmin(null);
    final List<ConditionalPermissionInfo> initialConditionalPermissionInfos =
        conditionalPermissionAdmin.newConditionalPermissionUpdate().getConditionalPermissionInfos();

    permissionActivator.grantPermission(BUNDLE_1, PERMISSION_1);

    permissionActivator.grantPermission(BUNDLE_2, PERMISSION_1);

    final List<ConditionalPermissionInfo> conditionalPermissionInfos =
        conditionalPermissionAdmin.newConditionalPermissionUpdate().getConditionalPermissionInfos();

    // the new permission should be at the top and the rest identical to the initial ones
    assertThat(conditionalPermissionInfos.size(), is(initialConditionalPermissionInfos.size() + 1));
    for (int i = 1; i < conditionalPermissionInfos.size(); i++) {
      assertThat(
          conditionalPermissionInfos.get(i), is(initialConditionalPermissionInfos.get(i - 1)));
    }
    final ConditionalPermissionInfo newInfo = conditionalPermissionInfos.get(0);

    assertThat(newInfo.getPermissionInfos().length, is(1));
    assertThat(newInfo.getPermissionInfos()[0].getType(), is(PERMISSION_1_TYPE));
    assertThat(newInfo.getPermissionInfos()[0].getName(), is(PERMISSION_1_NAME));
    assertThat(newInfo.getPermissionInfos()[0].getActions(), is(PERMISSION_1_ACTIONS));
    assertThat(newInfo.getConditionInfos().length, is(1));
    assertThat(
        newInfo.getConditionInfos()[0].getArgs(), Matchers.arrayContaining(BUNDLE_1, BUNDLE_2));
    assertThat(newInfo.getAccessDecision(), is("allow"));
  }

  private PermissionActivator createPermissionActivator(String... names) throws Exception {
    for (final String name : names) {
      File grantPolicy = temporaryFolder.newFile("security" + name);
      FileOutputStream grantOutStream = new FileOutputStream(grantPolicy);
      InputStream grantStream = PermissionActivatorTest.class.getResourceAsStream(name);

      IOUtils.copy(grantStream, grantOutStream);
      IOUtils.closeQuietly(grantOutStream);
      IOUtils.closeQuietly(grantStream);
    }
    PermissionActivatorForTest permissionActivator = new PermissionActivatorForTest();
    permissionActivator.start(mock(BundleContext.class));
    return permissionActivator;
  }

  private class PermissionActivatorForTest extends PermissionActivator {
    SecurityAdmin securityAdmin;

    @Override
    ConditionalPermissionAdmin getConditionalPermissionAdmin(BundleContext bundleContext) {
      if (securityAdmin == null) {
        EquinoxSecurityManager equinoxSecurityManager = mock(EquinoxSecurityManager.class);
        PermissionData permissionData = new PermissionData();
        securityAdmin = new SecurityAdmin(equinoxSecurityManager, permissionData);
      }
      return securityAdmin;
    }

    @Override
    void systemExit(File file) {
      throw new RuntimeException("Expected System Exit");
    }
  }
}
