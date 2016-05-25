/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.ui.admin.api;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.codice.ddf.ui.admin.api.plugin.ConfigurationAdminPlugin;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;

public class ConfigurationAdminExtTest {
    final BundleContext testBundleContext = mock(BundleContext.class);

    final MetaTypeService testMTS = mock(MetaTypeService.class);

    private ConfigurationAdmin testConfigAdmin;

    private ConfigurationAdminExt configurationAdminExt;

    private static final String LIST_CONFIG_STRING =
            "(|(service.factoryPid=TestPID)(service.factoryPid=TestPID_disabled))";

    private static final String TEST_PID = "TestPID";

    private static final String BAD_PID = ">TestPID<";

    private static final String TEST_FACT_FILTER = "TestFactoryFilter";

    private static final String TEST_FILTER = "TestFilter";

    private static final String TEST_BUNDLE_LOC = "TestBundleLocation";

    private static final String TEST_OCD = "TestOCD";

    private static final String TEST_BUNDLE_NAME = "TestBundle";

    private static final String TEST_FACTORY_PID = "TestFactPID";

    private static final String TEST_KEY = "TestKey";

    private static final String TEST_VALUE = "TestValue";

    private Configuration testConfig;

    private Bundle testBundle;

    private Dictionary<String, String> bundleHeaders;

    private MetaTypeInformation testMTI;

    private ObjectClassDefinition testOCD;

    private AttributeDefinition testAttDef;

    private ServiceReference testRef1;

    @Before
    public void setUpBasic() {
        testConfigAdmin = mock(ConfigurationAdmin.class);
        configurationAdminExt = new ConfigurationAdminExt(testConfigAdmin) {
            @Override
            BundleContext getBundleContext() {
                return testBundleContext;
            }

            @Override
            MetaTypeService getMetaTypeService() {
                return testMTS;
            }
        };
    }

    public void setUpTestConfig() {
        Dictionary<String, Object> testProp = new Hashtable<>();
        testProp.put(TEST_KEY, TEST_VALUE);
        testConfig = mock(Configuration.class);

        when(testConfig.getPid()).thenReturn(TEST_PID);
        when(testConfig.getFactoryPid()).thenReturn(TEST_FACTORY_PID);
        when(testConfig.getBundleLocation()).thenReturn(TEST_BUNDLE_LOC);
        when(testConfig.getProperties()).thenReturn(testProp);
    }

    public void setUpListServices() throws Exception {
        testBundle = mock(Bundle.class);
        bundleHeaders = mock(Dictionary.class);
        testMTI = mock(MetaTypeInformation.class);
        testOCD = mock(ObjectClassDefinition.class);
        testAttDef = mock(AttributeDefinition.class);
        testRef1 = mock(ServiceReference.class);
        ServiceReference[] testServRefs = {testRef1};

        when(testRef1.getProperty(Constants.SERVICE_PID)).thenReturn(TEST_PID);
        when(testRef1.getBundle()).thenReturn(testBundle);

        when(testBundle.getLocation()).thenReturn(TEST_BUNDLE_LOC);
        when(testBundle.getHeaders(anyString())).thenReturn(bundleHeaders);
        when(bundleHeaders.get(Constants.BUNDLE_NAME)).thenReturn(TEST_BUNDLE_NAME);

        when(testOCD.getName()).thenReturn(TEST_OCD);
        when(testOCD.getAttributeDefinitions(ObjectClassDefinition.ALL)).thenReturn(new AttributeDefinition[] {
                testAttDef});

        when(testMTI.getBundle()).thenReturn(testBundle);
        when(testMTI.getFactoryPids()).thenReturn(new String[] {TEST_FACTORY_PID});
        when(testMTI.getPids()).thenReturn(new String[] {TEST_PID});
        when(testMTI.getObjectClassDefinition(anyString(), anyString())).thenReturn(testOCD);

        when(testMTS.getMetaTypeInformation(testBundle)).thenReturn(testMTI);

        when(testBundleContext.getBundles()).thenReturn(new Bundle[] {testBundle});

        when(testConfigAdmin.listConfigurations(anyString())).thenReturn(new Configuration[] {
                testConfig});

        when(testBundleContext.getAllServiceReferences(ManagedService.class.getName(),
                TEST_FILTER)).thenReturn(testServRefs);
        when(testBundleContext.getAllServiceReferences(ManagedServiceFactory.class.getName(),
                TEST_FILTER)).thenReturn(testServRefs);
    }

    /**
     * Tests the {@link ConfigurationAdminExt#getConfiguration(String)} method
     *
     * @throws Exception
     */
    @Test
    public void testGetConfiguration() throws Exception {
        Configuration testConfig = mock(Configuration.class);
        Configuration[] configurations = new Configuration[] {testConfig};

        when(testConfigAdmin.listConfigurations(
                '(' + Constants.SERVICE_PID + '=' + TEST_PID + ')')).thenReturn(configurations);
        assertNotNull(configurationAdminExt.getConfiguration(TEST_PID));
    }

    /**
     * Tests the {@link ConfigurationAdminExt#listServices(String, String)} method for the case
     * where there is a filter returned by the bundleContext
     *
     * @throws Exception
     */
    @Test
    public void testListServicesExistingFilter() throws Exception {
        setUpTestConfig();
        setUpListServices();

        Filter testFilter = mock(Filter.class);
        List<ConfigurationAdminPlugin> testPluginList = new ArrayList<>();
        ConfigurationAdminPlugin testPlugin = mock(ConfigurationAdminPlugin.class);
        testPluginList.add(testPlugin);
        Map<String, Object> testConfigData = new HashMap<>();
        testConfigData.put(TEST_KEY, TEST_VALUE);
        when(testPlugin.getConfigurationData(anyString(),
                any(Map.class),
                any(BundleContext.class))).thenReturn(testConfigData);

        when(testFilter.match(any(Hashtable.class))).thenReturn(true);

        when(testBundleContext.createFilter(anyString())).thenReturn(testFilter);

        configurationAdminExt.setConfigurationAdminPluginList(testPluginList);

        List<Map<String, Object>> result = configurationAdminExt.listServices(TEST_FACT_FILTER,
                TEST_FILTER);

        assertThat("Should return the correct services.",
                (String) result.get(0)
                        .get("id"),
                is(TEST_PID));

        verify(testConfigAdmin, atLeastOnce()).listConfigurations(LIST_CONFIG_STRING);
    }

    /**
     * Tests the {@link ConfigurationAdminExt#listServices(String, String)} and connected methods
     * for the case where the Configuration's bundle location can not be found
     *
     * @throws Exception
     */
    @Test
    public void testListServicesNoBundleLocation() throws Exception {
        setUpTestConfig();
        setUpListServices();

        when(testConfig.getBundleLocation()).thenReturn(null);

        List<Map<String, Object>> result = configurationAdminExt.listServices(TEST_FACT_FILTER,
                TEST_FILTER);

        assertThat("Should return the correct services.",
                (String) result.get(0)
                        .get("id"),
                is(TEST_PID));

        verify(testConfigAdmin, atLeastOnce()).listConfigurations(LIST_CONFIG_STRING);
    }

    /**
     * Tests the {@link ConfigurationAdminExt#listServices(String, String)} and connected methods
     * for the case where the Configuration's bundle location and factoryPid both can not be found
     *
     * @throws Exception
     */
    @Test
    public void testListServicesNoBundleLocationOrFactoryPID() throws Exception {
        setUpTestConfig();
        setUpListServices();

        when(testConfig.getBundleLocation()).thenReturn(null);
        when(testConfig.getFactoryPid()).thenReturn(null);

        List<Map<String, Object>> result = configurationAdminExt.listServices(TEST_FACT_FILTER,
                TEST_FILTER);

        assertThat("Should return the correct services.",
                (String) result.get(0)
                        .get("id"),
                is(TEST_PID));

        verify(testConfigAdmin, atLeastOnce()).listConfigurations(LIST_CONFIG_STRING);
    }

    /**
     * Tests the {@link ConfigurationAdminExt#listServices(String, String)} and connected methods
     * for the case where the Configuration's factoryPid can not be found
     *
     * @throws Exception
     */
    @Test
    public void testListServicesNoFactoryPID() throws Exception {
        setUpTestConfig();
        setUpListServices();

        when(testConfig.getFactoryPid()).thenReturn(null);

        List<Map<String, Object>> result = configurationAdminExt.listServices(TEST_FACT_FILTER,
                TEST_FILTER);

        assertThat("Should return the correct services.",
                (String) result.get(0)
                        .get("id"),
                is(TEST_PID));

        verify(testConfigAdmin, atLeastOnce()).listConfigurations(LIST_CONFIG_STRING);
    }

    /**
     * Tests the {@link ConfigurationAdminExt#listServices(String, String)} and connected methods
     * for the case where the Configuration's Pid is null
     *
     * @throws Exception
     */
    @Test
    public void testListServicesNoPID() throws Exception {
        setUpTestConfig();
        setUpListServices();

        when(testConfig.getPid()).thenReturn(null);

        List<Map<String, Object>> result = configurationAdminExt.listServices(TEST_FACT_FILTER,
                TEST_FILTER);

        assertThat("Should return the correct services.",
                (String) result.get(0)
                        .get("id"),
                is(TEST_PID));

        verify(testConfigAdmin, atLeastOnce()).listConfigurations(LIST_CONFIG_STRING);
    }

    /**
     * Tests the {@link ConfigurationAdminExt#listServices(String, String)} method
     * and all associated methods for the case where there is no bundle name
     *
     * @throws Exception
     */
    @Test
    public void testListServicesNoName() throws Exception {
        setUpTestConfig();
        setUpListServices();

        when(bundleHeaders.get(Constants.BUNDLE_NAME)).thenReturn(null);

        List<Map<String, Object>> result = configurationAdminExt.listServices(TEST_FACT_FILTER,
                TEST_FILTER);

        assertThat("Should return the correct services.",
                (String) result.get(0)
                        .get("id"),
                is(TEST_PID));
    }

    /**
     * Tests the {@link ConfigurationAdminExt#listServices(String, String)} method and connected
     * methods for the case where the Configuration contains a password property and that its value
     * is properly changed to "password".
     *
     * @throws Exception
     */
    @Test
    public void testListServicesPasswordType() throws Exception {
        setUpTestConfig();
        setUpListServices();

        // Create a password property with a value of "secret".
        Dictionary<String, Object> testProp = new Hashtable<>();
        testProp.put("password", "secret");
        when(testAttDef.getID()).thenReturn("password");
        when(testAttDef.getType()).thenReturn(AttributeDefinition.PASSWORD);
        when(testConfig.getProperties()).thenReturn(testProp);

        List<Map<String, Object>> result = configurationAdminExt.listServices(TEST_FACT_FILTER,
                TEST_FILTER);

        // Assert that the password value was changed from "secret" to "password".
        String password = (String) ((Map<String, Object>) ((List<Map<String, Object>>) result.get(0)
                .get("configurations")).get(0)
                .get("properties")).get("password");

        assertThat(password, is(equalTo("password")));
    }

    /**
     * Tests the {@link ConfigurationAdminExt#listServices(String, String)} method
     * and all associated methods for the case where the isAllowedPid returns false
     *
     * @throws Exception
     */
    @Test
    public void testListServicesNotAllowedPid() throws Exception {
        setUpTestConfig();
        setUpListServices();

        when(testRef1.getProperty(Constants.SERVICE_PID)).thenReturn(BAD_PID);

        List<Map<String, Object>> result = configurationAdminExt.listServices(TEST_FACT_FILTER,
                TEST_FILTER);
        assertTrue("Should not return any services.", result.isEmpty());
    }

    /**
     * Tests the {@link ConfigurationAdminExt#listServices(String, String)} method
     * for the case where configurationAdmin.listConfigurations(....) throws an IOException
     *
     * @throws Exception
     */
    @Test
    public void testListServicesIOException() throws Exception {
        setUpListServices();
        setUpTestConfig();

        doThrow(new IOException()).when(testConfigAdmin)
                .listConfigurations(anyString());

        List<Map<String, Object>> result = configurationAdminExt.
                listServices(TEST_FACT_FILTER, TEST_FILTER);

        assertThat("Should recover gracefully but not add to the given data.",
                (String) result.get(0)
                        .get("name"),
                is(TEST_OCD));
        assertThat("Should only contain one map.", result.size(), is(1));
    }

    /**
     * Tests the {@link ConfigurationAdminExt#listServices(String, String)} method
     * for the case where configurationAdmin.listConfigurations(....) throws an InvalidSyntaxException
     *
     * @throws Exception
     */
    @Test
    public void testListServicesInvalidSyntaxException() throws Exception {
        setUpListServices();
        setUpTestConfig();

        doThrow(new InvalidSyntaxException("", "")).when(testConfigAdmin)
                .listConfigurations(anyString());

        List<Map<String, Object>> result = configurationAdminExt.
                listServices(TEST_FACT_FILTER, TEST_FILTER);

        assertThat("Should recover gracefully but not add to the given data.",
                (String) result.get(0)
                        .get("name"),
                is(TEST_OCD));
        assertThat("Should only contain one map.", result.size(), is(1));
    }

    /**
     * Tests the {@link ConfigurationAdminExt#listServices(String, String)} method
     * for the case where bundle.getHeaders(..).get(Constants.BUNDLE_NAME) returns null,
     * bundle.getSymbolicName() returns null, and bundle.getLocation returns null.
     *
     * @throws Exception
     */
    @Test
    public void testListServicesNullLocationSymNameBundleName() throws Exception {
        setUpTestConfig();
        setUpListServices();

        when(testBundle.getLocation()).thenReturn(TEST_BUNDLE_LOC)
                .thenReturn(TEST_BUNDLE_LOC)
                .thenReturn(null)
                .thenReturn(TEST_BUNDLE_LOC);
        when(testBundle.getSymbolicName()).thenReturn(null);
        when(bundleHeaders.get(Constants.BUNDLE_NAME)).thenReturn(null);

        List<Map<String, Object>> result = configurationAdminExt.listServices(TEST_FACT_FILTER,
                TEST_FILTER);

        assertThat("Should return the correct services.",
                (String) result.get(0)
                        .get("id"),
                is(TEST_PID));
    }

    /**
     * Tests the {@link ConfigurationAdminExt#getConfiguration(String)} method
     * for the case where configurationAdmin.listConfigurations(..) throws an InvalidSyntaxException
     *
     * @throws Exception
     */
    @Test
    public void testGetConfigurationInvalidSyntaxException() throws Exception {
        doThrow(new InvalidSyntaxException("", "")).when(testConfigAdmin)
                .listConfigurations(anyString());

        Configuration result = configurationAdminExt.getConfiguration(TEST_PID);

        assertThat("Should handle the exception gracefully and return null.",
                result,
                is(nullValue()));
    }

    /**
     * Tests the {@link ConfigurationAdminExt#getConfiguration(String)} method
     * for the case where configurationAdmin.listConfigurations(..) throws an IOException
     *
     * @throws Exception
     */
    @Test
    public void testGetConfigurationIOException() throws Exception {
        doThrow(new IOException()).when(testConfigAdmin)
                .listConfigurations(anyString());

        Configuration result = configurationAdminExt.getConfiguration(TEST_PID);

        assertThat("Should handle the exception gracefully and return null.",
                result,
                is(nullValue()));
    }

    /**
     * Tests the {@link ConfigurationAdminExt#getBundle(BundleContext, String)} method
     * for the case where the string parameter is null
     *
     * @throws Exception
     */
    @Test
    public void testGetBundleNullStringParam() throws Exception {
        assertThat("Should return null.",
                configurationAdminExt.getBundle(testBundleContext, null),
                is(nullValue()));
    }

    /**
     * Tests the {@link ConfigurationAdminExt#getBundle(BundleContext, String)} method
     * for the case where the bundleContext has no bundles
     *
     * @throws Exception
     */
    @Test
    public void testGetBundleNoBundles() throws Exception {
        when(testBundleContext.getBundles()).thenReturn(new Bundle[] {});

        assertThat("Should return null.",
                configurationAdminExt.getBundle(testBundleContext, TEST_BUNDLE_LOC),
                is(nullValue()));
    }
}
