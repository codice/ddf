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
package org.codice.ddf.admin.core.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.subject.Subject;
import org.codice.ddf.admin.core.api.Service;
import org.codice.ddf.ui.admin.api.module.AdminModule;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;

@RunWith(MockitoJUnitRunner.class)
public class AdminConsoleServiceTest {

  private static final String GUEST_CLAIMS_CONFIG_PID = "ddf.security.sts.guestclaims";

  private static final String TEST_PID = "TestPID";

  private static final String TEST_FACTORY_PID = "TestFactoryPID";

  private static final String TEST_FACT_PID_DISABLED = "TestFactoryPID_disabled";

  private static final String TEST_FILTER_1 = "TestFilter1";

  private static final String TEST_KEY = "TestKey";

  private static final String TEST_VALUE = "TestValue";

  private static final String TEST_LOCATION = "TestLocation";

  private static final String TEST_URI = "TestURI";

  private static final String TEST_MODULE_1 = "TestModule1";

  private static final String TEST_MODULE_2 = "TestModule2";

  private static final String LOC_NOT_BOUND = "Configuration is not yet bound to a bundle location";

  private static final String TEST_OCD = "TestOCD";

  private static final String TEST_BUNDLE_NAME = "TestBundle";

  private static final String UI_CONFIG_PID = "ddf.platform.ui.config";

  private static final String PROFILE_KEY = "profile";

  @Mock private GuestClaimsHandlerExt mockGuestClaimsHandlerExt;

  private static final org.osgi.service.cm.ConfigurationAdmin CONFIGURATION_ADMIN =
      mock(org.osgi.service.cm.ConfigurationAdmin.class);

  private static final int CARDINALITY_ARRAY = 100;

  private static final int CARDINALITY_VECTOR = -100;

  private static final int CARDINALITY_PRIMITIVE = 0;

  private static final int[] CARDINALITIES =
      new int[] {CARDINALITY_VECTOR, CARDINALITY_PRIMITIVE, CARDINALITY_ARRAY};

  private static final int TEST_INT = 42;

  private static Configuration testConfig;

  private static ConfigurationAdminImpl configurationAdminImpl;

  private AdminConsoleService configAdmin;

  private MBeanServer testServer;

  @Before
  public void setupMethod() throws NotCompliantMBeanException {
    testConfig = mock(Configuration.class);
    org.osgi.service.cm.ConfigurationAdmin testConfigAdmin =
        mock(org.osgi.service.cm.ConfigurationAdmin.class);

    configurationAdminImpl =
        new ConfigurationAdminImpl(testConfigAdmin, new ArrayList<>()) {
          @Override
          public boolean isPermittedToViewService(String servicePid) {
            return true;
          }

          @Override
          public boolean isPermittedToViewService(String servicePid, Subject subject) {
            return true;
          }
        };

    configAdmin =
        new AdminConsoleService(testConfigAdmin, configurationAdminImpl) {
          @Override
          public boolean isPermittedToViewService(String servicePid) {
            return true;
          }
        };

    testServer = mock(MBeanServer.class);

    configAdmin.setMBeanServer(testServer);
  }

  @After
  public void setupAfter() {
    testServer = mock(MBeanServer.class);

    configAdmin.setMBeanServer(testServer);
  }

  /**
   * Tests the {@link
   * AdminConsoleService#AdminConsoleService(org.osgi.service.cm.ConfigurationAdmin,
   * org.codice.ddf.admin.core.api.ConfigurationAdmin)} constructor and the {@link
   * AdminConsoleService#init()} method
   *
   * @throws Exception
   */
  @Test
  public void testConstructorInit() throws Exception {

    assertNotNull(configAdmin);

    configAdmin.init();

    verify(testServer).registerMBean(any(Object.class), any(ObjectName.class));
  }

  /**
   * Tests the {@link AdminConsoleService#init()} method for the case where it has already been
   * initialized
   *
   * @throws Exception
   */
  @Test
  public void testInitAlreadyExists() throws Exception {
    when(testServer.registerMBean(any(Object.class), any(ObjectName.class)))
        .thenThrow(new InstanceAlreadyExistsException())
        .thenReturn(null);

    configAdmin.init();

    verify(testServer, times(2)).registerMBean(any(Object.class), any(ObjectName.class));
    verify(testServer).unregisterMBean(any(ObjectName.class));
  }

  /**
   * Tests the {@link AdminConsoleService#init()} method for the case where an exception is thrown
   * by mBeanServer.registerMBean(...)
   *
   * @throws Exception
   */
  @Test(expected = RuntimeException.class)
  public void testInitException() throws Exception {
    when(testServer.registerMBean(any(Object.class), any(ObjectName.class)))
        .thenThrow(new NullPointerException());

    configAdmin.init();
  }

  /**
   * Tests the {@link AdminConsoleService#destroy()} method
   *
   * @throws Exception
   */
  @Test
  public void testDestroy() throws Exception {

    configAdmin.init();
    configAdmin.destroy();

    verify(testServer).unregisterMBean(any(ObjectName.class));
  }

  /**
   * Tests the {@link AdminConsoleService#destroy()} method for the case where an exception is
   * thrown by mBeanServer.unregisterMBean(..)
   *
   * @throws Exception
   */
  @Test(expected = RuntimeException.class)
  public void testDestroyException() throws Exception {

    doThrow(new NullPointerException()).when(testServer).unregisterMBean(any(ObjectName.class));

    configAdmin.init();
    configAdmin.destroy();
  }

  /**
   * Tests the {@link AdminConsoleService#setModuleList(List)} and {@link
   * AdminConsoleService#getModuleList()} methods
   *
   * @throws Exception
   */
  @Test
  public void testSetGetModuleList() throws Exception {
    List<AdminModule> moduleList = new ArrayList<>();
    AdminModule testModule1 = mock(AdminModule.class);
    AdminModule testModule2 = mock(AdminModule.class);
    moduleList.add(testModule1);
    moduleList.add(testModule2);

    configAdmin.setModuleList(moduleList);
    assertEquals(moduleList, configAdmin.getModuleList());
  }

  /**
   * Tests the {@link AdminConsoleService#listServices()} method
   *
   * @throws Exception
   */
  @Test
  public void testListServices() throws Exception {
    org.osgi.service.cm.ConfigurationAdmin testConfigAdmin =
        mock(org.osgi.service.cm.ConfigurationAdmin.class);
    ConfigurationAdminImpl testConfigAdminExt = mock(ConfigurationAdminImpl.class);
    AdminConsoleService configAdmin =
        new AdminConsoleService(testConfigAdmin, testConfigAdminExt) {
          @Override
          public boolean isPermittedToViewService(String servicePid) {
            return true;
          }
        };

    List<Service> result = configAdmin.listServices();

    assertTrue("Should return an empty list.", result.isEmpty());
    verify(testConfigAdminExt).listServices(nullable(String.class), nullable(String.class));
  }

  /**
   * Tests the {@link AdminConsoleService#listModules()} method for the case where
   * module.getJSLocation() returns null
   *
   * @throws Exception
   */
  @Test
  public void testListModules() throws Exception {

    List<AdminModule> moduleList = new ArrayList<>();
    AdminModule testModule1 = mock(AdminModule.class);
    AdminModule testModule2 = mock(AdminModule.class);
    moduleList.add(testModule1);
    moduleList.add(testModule2);
    configAdmin.setModuleList(moduleList);

    when(testModule1.getName()).thenReturn(TEST_MODULE_1);
    when(testModule2.getName()).thenReturn(TEST_MODULE_2);

    List<Map<String, Object>> result = configAdmin.listModules();

    assertThat("Should return the provided modules.", result.get(0).get("name"), is(TEST_MODULE_1));
  }

  /**
   * Tests the {@link AdminConsoleService#listModules()} method for the case where
   * module.getJSLocation(), module.getCSSLocation, and module.getIframeLocation do not return null
   *
   * @throws Exception
   */
  @Test
  public void testListModulesJSCSSIFrameLocation() throws Exception {

    List<AdminModule> moduleList = new ArrayList<>();
    AdminModule testModule1 = mock(AdminModule.class);
    AdminModule testModule2 = mock(AdminModule.class);
    moduleList.add(testModule1);
    moduleList.add(testModule2);
    configAdmin.setModuleList(moduleList);

    when(testModule1.getName()).thenReturn(TEST_MODULE_1);
    when(testModule2.getName()).thenReturn(TEST_MODULE_2);

    when(testModule1.getJSLocation()).thenReturn(new URI(TEST_URI));
    when(testModule2.getJSLocation()).thenReturn(new URI(TEST_URI));
    when(testModule1.getCSSLocation()).thenReturn(new URI(TEST_URI));
    when(testModule2.getCSSLocation()).thenReturn(new URI(TEST_URI));
    when(testModule1.getIframeLocation()).thenReturn(new URI(TEST_URI));
    when(testModule2.getIframeLocation()).thenReturn(new URI(TEST_URI));

    List<Map<String, Object>> result = configAdmin.listModules();

    assertThat("Should return the provided modules.", result.get(0).get("name"), is(TEST_MODULE_1));
    assertThat(
        "jsLocation should be assigned some value.",
        result.get(0).get("jsLocation"),
        is(notNullValue()));
  }

  /**
   * Tests the {@link AdminConsoleService#getService(String)} method
   *
   * @throws Exception
   */
  @Test
  public void testGetService() throws Exception {
    org.osgi.service.cm.ConfigurationAdmin testConfigAdmin =
        mock(org.osgi.service.cm.ConfigurationAdmin.class);
    ConfigurationAdminImpl testConfigAdminExt = mock(ConfigurationAdminImpl.class);
    AdminConsoleService configAdmin =
        new AdminConsoleService(testConfigAdmin, testConfigAdminExt) {
          @Override
          public boolean isPermittedToViewService(String servicePid) {
            return true;
          }
        };

    List<Service> serviceList = new ArrayList<>();
    Service testService = new ServiceImpl();
    testService.put(TEST_KEY, TEST_VALUE);
    serviceList.add(testService);
    when(testConfigAdminExt.listServices(TEST_FILTER_1, TEST_FILTER_1)).thenReturn(serviceList);

    assertEquals(testService.get(TEST_KEY), configAdmin.getService(TEST_FILTER_1).get(TEST_KEY));
    verify(testConfigAdminExt).listServices(TEST_FILTER_1, TEST_FILTER_1);
  }

  /**
   * Tests the {@link AdminConsoleService#createFactoryConfiguration(String)} and {@link
   * AdminConsoleService#createFactoryConfigurationForLocation(String, String)} methods
   *
   * @throws Exception
   */
  @Test
  public void testCreateFactoryConfiguration() throws Exception {
    org.osgi.service.cm.ConfigurationAdmin testConfigAdmin =
        mock(org.osgi.service.cm.ConfigurationAdmin.class);
    AdminConsoleService configAdmin =
        new AdminConsoleService(testConfigAdmin, configurationAdminImpl) {
          @Override
          public boolean isPermittedToViewService(String servicePid) {
            return true;
          }
        };

    Configuration testConfig = mock(Configuration.class);
    when(testConfig.getPid()).thenReturn(TEST_PID);

    when(testConfigAdmin.createFactoryConfiguration(TEST_PID)).thenReturn(testConfig);
    String config = configAdmin.createFactoryConfiguration(TEST_PID);

    assertNotNull(config);
    assertEquals(TEST_PID, config);
  }

  /**
   * Tests the {@link AdminConsoleService#createFactoryConfiguration(String)} and {@link
   * AdminConsoleService#createFactoryConfigurationForLocation(String, String)} methods for the case
   * where an IOException is thrown
   *
   * @throws Exception
   */
  @Test(expected = IOException.class)
  public void testCreateFactoryConfigurationIOException() throws Exception {

    configAdmin.createFactoryConfiguration(StringUtils.EMPTY);
  }

  /**
   * Tests the {@link AdminConsoleService#delete(String)} and {@link
   * AdminConsoleService#deleteForLocation(String, String)} methods
   *
   * @throws Exception
   */
  @Test
  public void testDelete() throws Exception {
    org.osgi.service.cm.ConfigurationAdmin testConfigAdmin =
        mock(org.osgi.service.cm.ConfigurationAdmin.class);
    AdminConsoleService configAdmin =
        new AdminConsoleService(testConfigAdmin, configurationAdminImpl) {
          @Override
          public boolean isPermittedToViewService(String servicePid) {
            return true;
          }
        };

    Configuration testConfig = mock(Configuration.class);

    when(testConfigAdmin.getConfiguration(TEST_PID, null)).thenReturn(testConfig);
    configAdmin.delete(TEST_PID);
    verify(testConfig).delete();
  }

  /**
   * Tests the {@link AdminConsoleService#delete(String)} and {@link
   * AdminConsoleService#deleteForLocation(String, String)} methods for the case where an
   * IOException is thrown
   *
   * @throws Exception
   */
  @Test(expected = Exception.class)
  public void testDeleteIOException() throws Exception {

    configAdmin.delete(null);
  }

  /**
   * Tests the {@link AdminConsoleService#deleteConfigurations(String)} method
   *
   * @throws Exception
   */
  @Test
  public void testDeleteConfigurations() throws Exception {
    org.osgi.service.cm.ConfigurationAdmin testConfigAdmin =
        mock(org.osgi.service.cm.ConfigurationAdmin.class);
    AdminConsoleService configAdmin =
        new AdminConsoleService(testConfigAdmin, configurationAdminImpl) {
          @Override
          public boolean isPermittedToViewService(String servicePid) {
            return true;
          }
        };

    Configuration testConfig = mock(Configuration.class);

    when(testConfigAdmin.listConfigurations(nullable(String.class)))
        .thenReturn(new Configuration[] {testConfig});

    configAdmin.deleteConfigurations(TEST_FILTER_1);

    verify(testConfigAdmin).listConfigurations(TEST_FILTER_1);
    verify(testConfig).delete();
  }

  /**
   * Tests the {@link AdminConsoleService#deleteConfigurations(String)} method for the case where an
   * IOException is thrown
   *
   * @throws Exception
   */
  @Test(expected = IOException.class)
  public void testDeleteConfigurationsIOException() throws Exception {

    configAdmin.deleteConfigurations(StringUtils.EMPTY);
  }

  /**
   * Tests the {@link AdminConsoleService#deleteConfigurations(String)} method for the case where
   * the filter is invalid but not null
   *
   * @throws Exception
   */
  @Test(expected = IOException.class)
  public void testDeleteConfigurationsInvalidFilter() throws Exception {
    org.osgi.service.cm.ConfigurationAdmin testConfigAdmin =
        mock(org.osgi.service.cm.ConfigurationAdmin.class);
    AdminConsoleService configAdmin =
        new AdminConsoleService(testConfigAdmin, configurationAdminImpl) {
          @Override
          public boolean isPermittedToViewService(String servicePid) {
            return true;
          }
        };

    doThrow(new InvalidSyntaxException("Invalid filter.", "filter"))
        .when(testConfigAdmin)
        .listConfigurations("><><");
    configAdmin.deleteConfigurations("><><");
  }

  /**
   * Tests the {@link AdminConsoleService#getBundleLocation(String)} method
   *
   * @throws Exception
   */
  @Test
  public void testGetBundleLocation() throws Exception {
    org.osgi.service.cm.ConfigurationAdmin testConfigAdmin =
        mock(org.osgi.service.cm.ConfigurationAdmin.class);
    AdminConsoleService configAdmin =
        new AdminConsoleService(testConfigAdmin, configurationAdminImpl);
    Configuration testConfig = mock(Configuration.class);

    when(testConfig.getBundleLocation()).thenReturn(TEST_LOCATION);

    when(testConfigAdmin.getConfiguration(TEST_PID, null)).thenReturn(testConfig);

    assertEquals(TEST_LOCATION, configAdmin.getBundleLocation(TEST_PID));

    verify(testConfig, atLeastOnce()).getBundleLocation();
  }

  /**
   * Tests the {@link AdminConsoleService#getBundleLocation(String)} method for the case where the
   * configuration is not bound to a location
   *
   * @throws Exception
   */
  @Test
  public void testGetBundleLocationNotBound() throws Exception {
    org.osgi.service.cm.ConfigurationAdmin testConfigAdmin =
        mock(org.osgi.service.cm.ConfigurationAdmin.class);
    AdminConsoleService configAdmin =
        new AdminConsoleService(testConfigAdmin, configurationAdminImpl) {
          @Override
          public boolean isPermittedToViewService(String servicePid) {
            return true;
          }
        };
    Configuration testConfig = mock(Configuration.class);

    when(testConfig.getBundleLocation()).thenReturn(null);

    when(testConfigAdmin.getConfiguration(TEST_PID, null)).thenReturn(testConfig);

    assertEquals(LOC_NOT_BOUND, configAdmin.getBundleLocation(TEST_PID));
  }

  /**
   * Tests the {@link AdminConsoleService#getBundleLocation(String)} method for the case where an
   * IOException is thrown
   *
   * @throws Exception
   */
  @Test(expected = IOException.class)
  public void testGetBundleLocationIOException() throws Exception {

    configAdmin.getBundleLocation(null);
  }

  /**
   * Tests the {@link AdminConsoleService#getConfigurations(String)} method
   *
   * @throws Exception
   */
  @Test
  public void testGetConfigurations() throws Exception {
    org.osgi.service.cm.ConfigurationAdmin testConfigAdmin =
        mock(org.osgi.service.cm.ConfigurationAdmin.class);

    AdminConsoleService configAdmin =
        new AdminConsoleService(testConfigAdmin, configurationAdminImpl) {
          @Override
          public boolean isPermittedToViewService(String servicePid) {
            return true;
          }
        };
    Configuration testConfig = mock(Configuration.class);
    Configuration[] configs = {testConfig};

    when(testConfig.getPid()).thenReturn(TEST_PID);
    when(testConfig.getBundleLocation()).thenReturn(TEST_LOCATION);

    when(testConfigAdmin.listConfigurations(nullable(String.class))).thenReturn(configs);

    String[][] result = configAdmin.getConfigurations(TEST_FILTER_1);

    assertThat("Should return the given configurations.", result[0][0], is(TEST_PID));
    assertThat("Should return the given configurations.", result[0][1], is(TEST_LOCATION));
    verify(testConfig, times(2)).getPid();
    verify(testConfig).getBundleLocation();
  }

  /**
   * Tests the {@link AdminConsoleService#getConfigurations(String)} method for the case where the
   * argument is null
   *
   * @throws Exception
   */
  @Test(expected = IOException.class)
  public void testGetConfigurationsNullParam() throws Exception {

    configAdmin.getConfigurations(null);
  }

  /**
   * Tests the {@link AdminConsoleService#getConfigurations(String)} method for the case where the
   * filter is invalid
   *
   * @throws Exception
   */
  @Test(expected = IOException.class)
  public void testGetConfigurationsInvalidFilter() throws Exception {
    org.osgi.service.cm.ConfigurationAdmin testConfigAdmin =
        mock(org.osgi.service.cm.ConfigurationAdmin.class);
    AdminConsoleService configAdmin =
        new AdminConsoleService(testConfigAdmin, configurationAdminImpl) {
          @Override
          public boolean isPermittedToViewService(String servicePid) {
            return true;
          }
        };

    doThrow(new InvalidSyntaxException("", ""))
        .when(testConfigAdmin)
        .listConfigurations(nullable(String.class));
    configAdmin.getConfigurations(TEST_FILTER_1);
  }

  /**
   * Tests the {@link AdminConsoleService#getFactoryPid(String)} and {@link
   * AdminConsoleService#getFactoryPidForLocation(String, String)} methods
   *
   * @throws Exception
   */
  @Test
  public void testGetFactoryPid() throws Exception {
    org.osgi.service.cm.ConfigurationAdmin testConfigAdmin =
        mock(org.osgi.service.cm.ConfigurationAdmin.class);
    AdminConsoleService configAdmin =
        new AdminConsoleService(testConfigAdmin, configurationAdminImpl) {
          @Override
          public boolean isPermittedToViewService(String servicePid) {
            return true;
          }
        };

    Configuration testConfig = mock(Configuration.class);
    when(testConfig.getFactoryPid()).thenReturn(TEST_FACTORY_PID);

    when(testConfigAdmin.getConfiguration(TEST_PID, null)).thenReturn(testConfig);
    String pid = configAdmin.getFactoryPid(TEST_PID);

    assertEquals(TEST_FACTORY_PID, pid);
  }

  /**
   * Tests the {@link AdminConsoleService#getFactoryPid(String)} and {@link
   * AdminConsoleService#getFactoryPidForLocation(String, String)} methods for the case where the
   * argument pid is null
   */
  @Test(expected = Exception.class)
  public void testGetFactoryPidNullParam() throws Exception {

    configAdmin.getFactoryPid(null);
  }

  /**
   * Tests the {@link AdminConsoleService#getProperties(String)} and {@link
   * AdminConsoleService#getPropertiesForLocation(String, String)} methods
   *
   * @throws Exception
   */
  @Test
  public void testGetProperties() throws Exception {
    org.osgi.service.cm.ConfigurationAdmin testConfigAdmin =
        mock(org.osgi.service.cm.ConfigurationAdmin.class);
    AdminConsoleService configAdmin =
        new AdminConsoleService(testConfigAdmin, configurationAdminImpl) {
          @Override
          public boolean isPermittedToViewService(String servicePid) {
            return true;
          }
        };

    Configuration testConfig = mock(Configuration.class);
    Dictionary<String, Object> testProp = mock(Dictionary.class);
    Enumeration<String> testKeys = mock(Enumeration.class);

    when(testConfig.getProperties()).thenReturn(testProp);
    when(testProp.get(TEST_KEY)).thenReturn(TEST_VALUE);
    when(testProp.keys()).thenReturn(testKeys);
    when(testKeys.hasMoreElements()).thenReturn(true).thenReturn(false);
    when(testKeys.nextElement()).thenReturn(TEST_KEY);

    when(testConfigAdmin.getConfiguration(TEST_PID, null)).thenReturn(testConfig);
    Map<String, Object> result = configAdmin.getProperties(TEST_PID);

    assertThat("Should return the given properties.", result.get(TEST_KEY), is(TEST_VALUE));
  }

  /**
   * Tests the {@link AdminConsoleService#getPropertiesForLocation(String, String)} method for the
   * case where the argument is null
   *
   * @throws Exception
   */
  @Test(expected = Exception.class)
  public void testGetPropertiesNullParam() throws Exception {

    configAdmin.getPropertiesForLocation(null, null);
  }

  /**
   * Tests the {@link AdminConsoleService#setBundleLocation(String, String)} method
   *
   * @throws Exception
   */
  @Test
  public void testSetBundleLocation() throws Exception {
    org.osgi.service.cm.ConfigurationAdmin testConfigAdmin =
        mock(org.osgi.service.cm.ConfigurationAdmin.class);
    AdminConsoleService configAdmin =
        new AdminConsoleService(testConfigAdmin, configurationAdminImpl);

    Configuration testConfig = mock(Configuration.class);

    when(testConfigAdmin.getConfiguration(TEST_PID, null)).thenReturn(testConfig);
    configAdmin.setBundleLocation(TEST_PID, TEST_LOCATION);
    verify(testConfig).setBundleLocation(TEST_LOCATION);
  }

  /**
   * Tests the {@link AdminConsoleService#setBundleLocation(String, String)} for the case where the
   * pid argument is null
   *
   * @throws Exception
   */
  @Test(expected = IOException.class)
  public void testSetBundleLocationNullLocation() throws Exception {

    configAdmin.setBundleLocation(null, TEST_LOCATION);
  }

  /**
   * Tests the {@link AdminConsoleService#update(String, Map)} and {@link
   * AdminConsoleService#updateForLocation(String, String, Map)} methods
   *
   * @throws Exception
   */
  @Test
  public void testUpdate() throws Exception {
    AdminConsoleService configAdmin = getConfigAdmin();
    // test every typed cardinality<->cardinality mapping
    for (int i = 0; i < CARDINALITIES.length; i++) {
      int cardinality = CARDINALITIES[i];
      Map<String, Object> testConfigTable = new Hashtable<>();
      for (AdminConsoleService.TYPE type : AdminConsoleService.TYPE.values()) {
        for (int keyCardinality : CARDINALITIES) {
          testConfigTable.put(getKey(keyCardinality, type), getValue(cardinality, type));
        }
      }
      configAdmin.update(TEST_PID, testConfigTable);
      verify(testConfig, times(i + 1)).update(any(Dictionary.class));
    }
    Hashtable<String, Object> values = new Hashtable<>();
    String arrayString = getKey(CARDINALITY_ARRAY, AdminConsoleService.TYPE.STRING);
    ArgumentCaptor<Dictionary> captor = ArgumentCaptor.forClass(Dictionary.class);
    // test string jsonarray parsing
    values.put(arrayString, "[\"foo\",\"bar\",\"baz\"]");
    String primitiveBoolean = getKey(CARDINALITY_PRIMITIVE, AdminConsoleService.TYPE.BOOLEAN);
    // test string valueof parsing
    values.put(primitiveBoolean, "true");
    String primitiveInteger = getKey(CARDINALITY_PRIMITIVE, AdminConsoleService.TYPE.INTEGER);
    // test string valueof parsing for non-strings
    values.put(primitiveInteger, (long) TEST_INT);
    String arrayInteger = getKey(CARDINALITY_ARRAY, AdminConsoleService.TYPE.INTEGER);
    // test empty  array substitution
    values.put(arrayInteger, "");
    configAdmin.update(TEST_PID, values);
    verify(testConfig, times(4)).update(captor.capture());
    assertThat(((String[]) captor.getValue().get(arrayString)).length, equalTo(3));
    assertThat(captor.getValue().get(primitiveBoolean), equalTo(true));
    assertThat(captor.getValue().get(primitiveInteger), equalTo(TEST_INT));
    assertThat(((Integer[]) captor.getValue().get(arrayInteger)).length, equalTo(0));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSanitizeUIConfiguration() throws Exception {
    AdminConsoleService configAdmin = getConfigAdmin();

    // Initialize illegal values.
    Map<String, Object> currentProps = new Hashtable<>();
    currentProps.put("color", "yellow;");
    currentProps.put(
        "background",
        "black; float: left; text-align: center; width: 120px; border: 1px solid gray; margin: 4px; padding: 6px;");

    configAdmin.update(UI_CONFIG_PID, currentProps);
  }

  @Test
  public void testUpdateGuestClaimsProfile() throws Exception {
    Map<String, Object> guestClaims = new HashMap<>();
    Map<String, Object> systemClaims = new HashMap<>();
    List<Map<String, Object>> configs = new ArrayList<>();

    guestClaims.put("example", "example");

    Map<String, Object> configTable = new HashMap<>();
    AdminConsoleService configAdmin = spy(getConfigAdmin());

    when(mockGuestClaimsHandlerExt.getProfileConfigs()).thenReturn(configs);

    assertFalse(configAdmin.updateGuestClaimsProfile(UI_CONFIG_PID, configTable));
    verifyZeroInteractions(mockGuestClaimsHandlerExt);

    configTable.put(PROFILE_KEY, 32);
    assertFalse(configAdmin.updateGuestClaimsProfile(UI_CONFIG_PID, configTable));
    verifyZeroInteractions(mockGuestClaimsHandlerExt);

    //        when(configAdmin.getProperties(GUEST_CLAIMS_CONFIG_PID)).thenReturn(guestClaims);
    doReturn(guestClaims).when(configAdmin).getProperties(GUEST_CLAIMS_CONFIG_PID);

    configTable.put(PROFILE_KEY, "anyExampleProfileName");
    assertTrue(configAdmin.updateGuestClaimsProfile(UI_CONFIG_PID, configTable));
    verify(mockGuestClaimsHandlerExt).setSelectedClaimsProfileName("anyExampleProfileName");
    verify(configAdmin).update(eq(GUEST_CLAIMS_CONFIG_PID), eq(guestClaims));
  }

  /**
   * Tests the {@link AdminConsoleService#update(String, Map)} and {@link
   * AdminConsoleService#updateForLocation(String, String, Map)} methods and verifies when updating
   * a password with a value other than "password", it will update
   *
   * @throws Exception
   */
  @Test
  public void testUpdatePassword() throws Exception {
    AdminConsoleService configAdmin = getConfigAdmin();

    // Initialize password to "secret".
    Dictionary<String, Object> currentProps = new Hashtable<>();
    currentProps.put("TestKey_0_12", "secret");
    when(testConfig.getProperties()).thenReturn(currentProps);

    // Update the password with "newPassword".
    Hashtable<String, Object> values = new Hashtable<>();
    values.put("TestKey_0_12", "newPassword");

    ArgumentCaptor<Dictionary> captor = ArgumentCaptor.forClass(Dictionary.class);
    configAdmin.update(TEST_PID, values);
    verify(testConfig, times(1)).update(captor.capture());

    // Assert the password updated to "newPassword".
    assertThat(captor.getValue().get("TestKey_0_12"), equalTo("newPassword"));
  }

  /**
   * Tests the {@link AdminConsoleService#update(String, Map)} and {@link
   * AdminConsoleService#updateForLocation(String, String, Map)} methods and verifies when
   * attempting to update a password with "password", it will not update it.
   *
   * @throws Exception
   */
  @Test
  public void testUpdatePasswordWithPassword() throws Exception {
    AdminConsoleService configAdmin = getConfigAdmin();

    // Initialize password to "secret".
    Dictionary<String, Object> currentProps = new Hashtable<>();
    currentProps.put("TestKey_0_12", "secret");
    when(testConfig.getProperties()).thenReturn(currentProps);

    // Attempt updating the password with "password", the password should not actually update.
    Hashtable<String, Object> values = new Hashtable<>();
    values.put("TestKey_0_12", "password");

    ArgumentCaptor<Dictionary> captor = ArgumentCaptor.forClass(Dictionary.class);
    configAdmin.update(TEST_PID, values);
    verify(testConfig, times(1)).update(captor.capture());

    // Assert the password did not update to "password".
    assertThat(captor.getValue().get("TestKey_0_12"), equalTo("secret"));
  }

  private Object getValue(int cardinality, AdminConsoleService.TYPE type) {
    Object value = null;
    switch (type) {
      case PASSWORD:
      case STRING:
        value = TEST_VALUE;
        break;
      case BIGDECIMAL:
        value = BigDecimal.valueOf(TEST_INT);
        break;
      case BIGINTEGER:
        value = BigInteger.valueOf(TEST_INT);
        break;
      case BOOLEAN:
        value = true;
        break;
      case BYTE:
        value = (byte) TEST_INT;
        break;
      case CHARACTER:
        value = 'c';
        break;
      case DOUBLE:
        value = (double) TEST_INT;
        break;
      case FLOAT:
        value = (float) TEST_INT;
        break;
      case INTEGER:
        value = TEST_INT;
        break;
      case LONG:
        value = (long) TEST_INT;
        break;
      case SHORT:
        value = (short) TEST_INT;
        break;
    }
    switch (cardinality) {
      case CARDINALITY_VECTOR:
        Vector<Object> vector = new Vector<>();
        vector.add(value);
        return vector;
      case CARDINALITY_PRIMITIVE:
        return value;
      case CARDINALITY_ARRAY:
        return new Object[] {value};
    }

    return null;
  }

  /**
   * Tests the {@link AdminConsoleService#update(String, Map)} and {@link
   * AdminConsoleService#updateForLocation(String, String, Map)} methods for the case where the pid
   * argument is null
   */
  @Test(expected = IllegalArgumentException.class)
  public void testUpdateNullPid() throws Exception {

    Map<String, Object> testConfigTable = new Hashtable<>();

    configAdmin.update(null, testConfigTable);
  }

  /**
   * Tests the {@link AdminConsoleService#update(String, Map)} and {@link
   * AdminConsoleService#updateForLocation(String, String, Map)} methods for the case where the
   * configurationTable argument is null
   *
   * @throws Exception
   */
  @Test(expected = IllegalArgumentException.class)
  public void testUpdateNullConfigTable() throws Exception {

    configAdmin.update(TEST_PID, null);
  }

  /**
   * Tests the {@link AdminConsoleService#update(String, Map)} and {@link
   * AdminConsoleService#updateForLocation(String, String, Map)} methods for the case where the
   * value of an item in configurationTable is null/empty
   *
   * @throws Exception
   */
  @Test
  public void testUpdateNullValue() throws Exception {
    AdminConsoleService configAdmin = getConfigAdmin();
    Map<String, Object> testConfigTable = new HashMap<>();
    testConfigTable.put(getKey(CARDINALITY_PRIMITIVE, AdminConsoleService.TYPE.STRING), null);

    configAdmin.update(TEST_PID, testConfigTable);
    verify(testConfig).update(any(Dictionary.class));
  }

  /**
   * Tests the {@link AdminConsoleService#disableConfiguration(String)} method for the case where
   * configurationAdminImpl.getConfiguration(servicePid) returns null
   *
   * @throws Exception
   */
  @Test(expected = IOException.class)
  public void testDisableConfigurationsNoConfig() throws Exception {

    configAdmin.disableConfiguration(TEST_PID);
  }

  /**
   * Tests the {@link AdminConsoleService#disableConfiguration(String)} method for the case where
   * the pid argument is null
   *
   * @throws Exception
   */
  @Test(expected = IOException.class)
  public void testDisableConfigurationsNullParam() throws Exception {

    configAdmin.disableConfiguration(null);
  }

  /**
   * Tests the {@link AdminConsoleService#disableConfiguration(String)} method for the case where
   * the source is already disabled
   *
   * @throws Exception
   */
  @Test(expected = Exception.class)
  public void testDisableConfigurationsAlreadyDisabled() throws Exception {
    org.osgi.service.cm.ConfigurationAdmin testConfigAdmin =
        mock(org.osgi.service.cm.ConfigurationAdmin.class);
    AdminConsoleService configAdmin =
        new AdminConsoleService(testConfigAdmin, configurationAdminImpl);

    Configuration testConfig = mock(Configuration.class);
    Configuration testFactoryConfig = mock(Configuration.class);
    Dictionary<String, Object> testProperties = new Hashtable<>();

    testProperties.put(
        org.osgi.service.cm.ConfigurationAdmin.SERVICE_FACTORYPID, TEST_FACT_PID_DISABLED);

    configAdmin.disableConfiguration(TEST_PID);
  }

  /**
   * Tests the {@link AdminConsoleService#enableConfiguration}
   *
   * @throws Exception
   */
  @Test(expected = Exception.class)
  public void testEnableConfigurationsNullParam() throws Exception {

    configAdmin.enableConfiguration(null);
  }

  /**
   * Tests the {@link AdminConsoleService#enableConfiguration(String)} method for the case where the
   * configuration is already enabled
   *
   * @throws Exception
   */
  @Test(expected = IOException.class)
  public void testEnableConfigurationsAlreadyEnabled() throws Exception {
    org.osgi.service.cm.ConfigurationAdmin testConfigAdmin =
        mock(org.osgi.service.cm.ConfigurationAdmin.class);
    AdminConsoleService configAdmin =
        new AdminConsoleService(testConfigAdmin, configurationAdminImpl) {
          @Override
          public boolean isPermittedToViewService(String servicePid) {
            return true;
          }
        };

    Configuration testConfig = mock(Configuration.class);
    Configuration testFactoryConfig = mock(Configuration.class);
    Dictionary<String, Object> testProperties = new Hashtable<>();

    testProperties.put(org.osgi.service.cm.ConfigurationAdmin.SERVICE_FACTORYPID, TEST_FACTORY_PID);

    configAdmin.enableConfiguration(TEST_PID);
  }

  /**
   * Tests the {@link AdminConsoleService#enableConfiguration(String)} method for the case where
   * configurationAdminImpl.getConfiguration(..) can't find the configuration
   *
   * @throws Exception
   */
  @Test(expected = IOException.class)
  public void testEnableConfigurationsNullDisabledConfig() throws Exception {
    org.osgi.service.cm.ConfigurationAdmin testConfigAdmin =
        mock(org.osgi.service.cm.ConfigurationAdmin.class);
    AdminConsoleService configAdmin =
        new AdminConsoleService(testConfigAdmin, configurationAdminImpl) {
          @Override
          public boolean isPermittedToViewService(String servicePid) {
            return true;
          }
        };

    configAdmin.enableConfiguration(TEST_PID);
  }

  @Test
  public void testGetClaimsConfiguration() throws NotCompliantMBeanException {
    org.osgi.service.cm.ConfigurationAdmin testConfigAdmin =
        mock(org.osgi.service.cm.ConfigurationAdmin.class);
    ConfigurationAdminImpl testConfigAdminExt = mock(ConfigurationAdminImpl.class);
    AdminConsoleService configAdmin = new AdminConsoleService(testConfigAdmin, testConfigAdminExt);

    List<Service> serviceList = new ArrayList<>();
    Service testService = new ServiceImpl();
    testService.put(TEST_KEY, TEST_VALUE);
    serviceList.add(testService);
    when(testConfigAdminExt.listServices(TEST_FILTER_1, TEST_FILTER_1)).thenReturn(serviceList);

    // check call before setting handler
    assertNotNull(configAdmin.getClaimsConfiguration(TEST_FILTER_1));

    GuestClaimsHandlerExt handlerExt = mock(GuestClaimsHandlerExt.class);
    when(handlerExt.getClaims()).thenReturn(new HashMap<>());
    when(handlerExt.getClaimsProfiles()).thenReturn(new HashMap<>());

    configAdmin.setGuestClaimsHandlerExt(handlerExt);

    assertNotNull(configAdmin.getClaimsConfiguration(TEST_FILTER_1));

    // check with bad filter
    assertNull(configAdmin.getClaimsConfiguration("bad_filter"));
  }

  private AdminConsoleService getConfigAdmin()
      throws IOException, InvalidSyntaxException, NotCompliantMBeanException {
    final BundleContext testBundleContext = mock(BundleContext.class);
    final MetaTypeService testMTS = mock(MetaTypeService.class);

    ConfigurationAdminImpl configurationAdminImpl =
        new ConfigurationAdminImpl(CONFIGURATION_ADMIN, new ArrayList<>()) {
          @Override
          BundleContext getBundleContext() {
            return testBundleContext;
          }

          @Override
          MetaTypeService getMetaTypeService() {
            return testMTS;
          }

          @Override
          public boolean isPermittedToViewService(String servicePid) {
            return true;
          }

          @Override
          public boolean isPermittedToViewService(String servicePid, Subject subject) {
            return true;
          }
        };

    AdminConsoleService configurationAdmin =
        new AdminConsoleService(CONFIGURATION_ADMIN, configurationAdminImpl) {
          @Override
          public boolean isPermittedToViewService(String servicePid) {
            return true;
          }
        };

    configurationAdmin.setGuestClaimsHandlerExt(mockGuestClaimsHandlerExt);

    Dictionary<String, Object> testProp = new Hashtable<>();
    testProp.put(TEST_KEY, TEST_VALUE);

    when(testConfig.getPid()).thenReturn(TEST_PID);
    when(testConfig.getFactoryPid()).thenReturn(TEST_FACTORY_PID);
    when(testConfig.getBundleLocation()).thenReturn(TEST_LOCATION);
    when(testConfig.getProperties()).thenReturn(testProp);

    Bundle testBundle = mock(Bundle.class);
    Dictionary bundleHeaders = mock(Dictionary.class);
    MetaTypeInformation testMTI = mock(MetaTypeInformation.class);
    ObjectClassDefinition testOCD = mock(ObjectClassDefinition.class);
    ServiceReference testRef1 = mock(ServiceReference.class);
    ServiceReference[] testServRefs = {testRef1};

    ArrayList<AttributeDefinition> attDefs = new ArrayList<>();
    for (int cardinality : CARDINALITIES) {
      for (AdminConsoleService.TYPE type : AdminConsoleService.TYPE.values()) {
        AttributeDefinition testAttDef = mock(AttributeDefinition.class);
        when(testAttDef.getCardinality()).thenReturn(cardinality);
        when(testAttDef.getType()).thenReturn(type.getType());
        when(testAttDef.getID()).thenReturn(getKey(cardinality, type));
        attDefs.add(testAttDef);
      }
    }

    when(testRef1.getProperty(Constants.SERVICE_PID)).thenReturn(TEST_PID);
    when(testRef1.getBundle()).thenReturn(testBundle);

    when(testBundle.getLocation()).thenReturn(TEST_LOCATION);
    when(testBundle.getHeaders(nullable(String.class))).thenReturn(bundleHeaders);
    when(bundleHeaders.get(Constants.BUNDLE_NAME)).thenReturn(TEST_BUNDLE_NAME);

    when(testOCD.getName()).thenReturn(TEST_OCD);
    when(testOCD.getAttributeDefinitions(ObjectClassDefinition.ALL))
        .thenReturn(attDefs.toArray(new AttributeDefinition[attDefs.size()]));

    when(testMTI.getFactoryPids()).thenReturn(new String[] {TEST_FACTORY_PID});
    when(testMTI.getPids()).thenReturn(new String[] {TEST_PID});
    when(testMTI.getObjectClassDefinition(nullable(String.class), nullable(String.class)))
        .thenReturn(testOCD);

    when(testMTS.getMetaTypeInformation(testBundle)).thenReturn(testMTI);

    when(testBundleContext.getBundles()).thenReturn(new Bundle[] {testBundle});

    when(CONFIGURATION_ADMIN.listConfigurations(nullable(String.class)))
        .thenReturn(new Configuration[] {testConfig});
    when(CONFIGURATION_ADMIN.getConfiguration(nullable(String.class), nullable(String.class)))
        .thenReturn(testConfig);

    when(testBundleContext.getAllServiceReferences(nullable(String.class), nullable(String.class)))
        .thenReturn(testServRefs);
    when(testBundleContext.getAllServiceReferences(nullable(String.class), nullable(String.class)))
        .thenReturn(testServRefs);

    return configurationAdmin;
  }

  private String getKey(int cardinality, AdminConsoleService.TYPE type) {
    return TEST_KEY + "_" + cardinality + "_" + type.getType();
  }
}
