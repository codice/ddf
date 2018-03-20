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

import static org.hamcrest.core.Is.is;
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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.osgi.framework.BundleContext;
import org.osgi.service.condpermadmin.ConditionalPermissionAdmin;
import org.osgi.service.condpermadmin.ConditionalPermissionInfo;

public class PermissionActivatorTest {

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void setup() throws IOException {
    temporaryFolder.newFolder("security");
    System.setProperty("ddf.home", temporaryFolder.getRoot().getAbsolutePath());
  }

  @Test
  public void testStartGrant() throws Exception {
    File grantPolicy = temporaryFolder.newFile("security/grant.policy");
    FileOutputStream grantOutStream = new FileOutputStream(grantPolicy);
    InputStream grantStream = PermissionActivatorTest.class.getResourceAsStream("/grant.policy");
    IOUtils.copy(grantStream, grantOutStream);
    IOUtils.closeQuietly(grantOutStream);
    IOUtils.closeQuietly(grantStream);

    PermissionActivatorForTest permissionActivator = new PermissionActivatorForTest();
    permissionActivator.start(mock(BundleContext.class));
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
    File denyPolicy = temporaryFolder.newFile("security/deny.policy");
    FileOutputStream denyOutStream = new FileOutputStream(denyPolicy);
    InputStream denyStream = PermissionActivatorTest.class.getResourceAsStream("/deny.policy");
    IOUtils.copy(denyStream, denyOutStream);
    IOUtils.closeQuietly(denyOutStream);
    IOUtils.closeQuietly(denyStream);

    PermissionActivatorForTest permissionActivator = new PermissionActivatorForTest();
    permissionActivator.start(mock(BundleContext.class));
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
    File nonePolicy = temporaryFolder.newFile("security/none.policy");
    FileOutputStream noneOutStream = new FileOutputStream(nonePolicy);
    InputStream noneStream = PermissionActivatorTest.class.getResourceAsStream("/none.policy");
    IOUtils.copy(noneStream, noneOutStream);
    IOUtils.closeQuietly(noneOutStream);
    IOUtils.closeQuietly(noneStream);

    PermissionActivatorForTest permissionActivator = new PermissionActivatorForTest();
    permissionActivator.start(mock(BundleContext.class));
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
    File denyPolicy = temporaryFolder.newFile("security/deny.policy");
    FileOutputStream denyOutStream = new FileOutputStream(denyPolicy);
    InputStream denyStream = PermissionActivatorTest.class.getResourceAsStream("/deny.policy");
    IOUtils.copy(denyStream, denyOutStream);

    File grantPolicy = temporaryFolder.newFile("security/grant.policy");
    FileOutputStream grantOutStream = new FileOutputStream(grantPolicy);
    InputStream grantStream = PermissionActivatorTest.class.getResourceAsStream("/grant.policy");
    IOUtils.copy(grantStream, grantOutStream);

    File nonePolicy = temporaryFolder.newFile("security/none.policy");
    FileOutputStream noneOutStream = new FileOutputStream(nonePolicy);
    InputStream noneStream = PermissionActivatorTest.class.getResourceAsStream("/none.policy");
    IOUtils.copy(noneStream, noneOutStream);
    IOUtils.closeQuietly(noneOutStream);
    IOUtils.closeQuietly(noneStream);
    IOUtils.closeQuietly(denyOutStream);
    IOUtils.closeQuietly(denyStream);
    IOUtils.closeQuietly(grantOutStream);
    IOUtils.closeQuietly(grantStream);

    PermissionActivatorForTest permissionActivator = new PermissionActivatorForTest();
    permissionActivator.start(mock(BundleContext.class));
  }

  private class PermissionActivatorForTest extends PermissionActivator {
    SecurityAdmin securityAdmin;

    ConditionalPermissionAdmin getConditionalPermissionAdmin(BundleContext bundleContext) {
      if (securityAdmin == null) {
        EquinoxSecurityManager equinoxSecurityManager = mock(EquinoxSecurityManager.class);
        PermissionData permissionData = new PermissionData();
        securityAdmin = new SecurityAdmin(equinoxSecurityManager, permissionData);
      }
      return securityAdmin;
    }
  }
}
