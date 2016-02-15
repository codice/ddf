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
package org.codice.ddf.admin.application.service.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Dependency;
import org.apache.karaf.features.Feature;
import org.codice.ddf.admin.application.plugin.ApplicationPlugin;
import org.codice.ddf.admin.application.rest.model.FeatureDetails;
import org.codice.ddf.admin.application.service.Application;
import org.codice.ddf.admin.application.service.ApplicationNode;
import org.codice.ddf.admin.application.service.ApplicationService;
import org.codice.ddf.admin.application.service.ApplicationServiceException;
import org.codice.ddf.admin.application.service.ApplicationStatus;
import org.codice.ddf.ui.admin.api.ConfigurationAdminExt;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;

public class ApplicationServiceBeanTest {
    private ApplicationService testAppService;

    private ConfigurationAdminExt testConfigAdminExt;

    private ApplicationNode testNode1;

    private ApplicationNode testNode2;

    private ApplicationNode testNode3;

    private Application testApp;

    private ApplicationStatus testStatus;

    private Set<ApplicationNode> nodeSet;

    private Set<ApplicationNode> childrenSet;

    private BundleContext bundleContext;

    private MBeanServer mBeanServer;

    private ObjectName objectName;

    private static final String TEST_FEATURE_DESCRIPTION = "Mock Feature for ApplicationServiceBean tests";

    private static final String TEST_FEATURE_DETAILS = "TestFeatureDetails";

    private static final String TEST_FEATURE_STATUS = "TestStatus";

    private static final String TEST_APP_NAME = "TestApp";

    private static final String TEST_VERSION = "0.0.0";

    private static final String TEST_APP_DESCRIP = "Test app for testGetApplicationTree";

    private static final String TEST_FEATURE_NAME = "TestFeature";

    private static final String TEST_URL = "TestMockURL";

    private static final String BAD_URL = ">BadURL<";

    private static final String TEST_LOCATION = "TestLocation";

    private static final String TEST_REPO_NAME = "TestRepo";

    private static final String ADD_APP_ASE = "Could not add application";

    private static final String REMOVE_APP_ASE = "Could not remove application";

    private static final String GET_SERV_ASE = "There was an error while trying to access the application";

    private Logger logger = LoggerFactory.getLogger(ApplicationServiceBeanMBean.class);

    @Before
    public void setUp() throws Exception {
        testAppService = mock(ApplicationServiceImpl.class);
        testConfigAdminExt = mock(ConfigurationAdminExt.class);
        testApp = mock(ApplicationImpl.class);

        when(testApp.getName()).thenReturn(TEST_APP_NAME);
        when(testApp.getVersion()).thenReturn(TEST_VERSION);
        when(testApp.getDescription()).thenReturn(TEST_APP_DESCRIP);
        when(testApp.getURI()).thenReturn(
                getClass().getClassLoader().getResource("test-features-with-main-feature.xml")
                        .toURI());
        bundleContext = mock(BundleContext.class);
        mBeanServer = mock(MBeanServer.class);
        objectName = new ObjectName(
                ApplicationService.class.getName() + ":service=application-service");
    }

    /**
     * Sets up an application tree for use in testing
     *
     * @throws Exception
     */
    public void setUpTree() throws Exception {
        testNode1 = mock(ApplicationNodeImpl.class);
        testNode2 = mock(ApplicationNodeImpl.class);
        testNode3 = mock(ApplicationNodeImpl.class);

        testStatus = mock(ApplicationStatus.class);

        when(testNode1.getApplication()).thenReturn(testApp);
        when(testNode2.getApplication()).thenReturn(testApp);
        when(testNode3.getApplication()).thenReturn(testApp);

        when(testNode1.getStatus()).thenReturn(testStatus);
        when(testNode2.getStatus()).thenReturn(testStatus);
        when(testNode3.getStatus()).thenReturn(testStatus);

        nodeSet = new TreeSet<>();
        nodeSet.add(testNode1);
        nodeSet.add(testNode2);
        childrenSet = new TreeSet<>();
        childrenSet.add(testNode3);

        when(testAppService.getApplicationTree()).thenReturn(nodeSet);

        when(testStatus.getState()).thenReturn(ApplicationStatus.ApplicationState.ACTIVE);

        when(testNode1.getChildren()).thenReturn(childrenSet);
        when(testNode2.getChildren()).thenReturn(childrenSet);
        when(testNode3.getChildren()).thenReturn((new TreeSet<ApplicationNode>()));
    }

    /**
     * Tests the {@link ApplicationServiceBean#init()} method
     *
     * @throws Exception
     */
    @Test
    public void testInit() throws Exception {
        ApplicationServiceBean serviceBean = new ApplicationServiceBean(testAppService,
                testConfigAdminExt, mBeanServer);
        serviceBean.init();

        verify(mBeanServer).registerMBean(serviceBean, objectName);
    }

    /**
     * Tests the {@link ApplicationServiceBean#init()} method for the case
     * where the serviceBean has already been initialized
     *
     * @throws Exception
     */
    @Test
    public void testInitTwice() throws Exception {
        ApplicationServiceBean serviceBean = new ApplicationServiceBean(testAppService,
                testConfigAdminExt, mBeanServer);
        when(mBeanServer.registerMBean(any(Object.class), any(ObjectName.class)))
                .thenThrow(new InstanceAlreadyExistsException()).thenReturn(null);

        serviceBean.init();

        verify(mBeanServer, atMost(1)).unregisterMBean(objectName);
        verify(mBeanServer, times(2)).registerMBean(serviceBean, objectName);
    }

    /**
     * Tests the {@link ApplicationServiceBean#init()} method for the case
     * where an exception other than the InstanceAlreadyExistsException is thrown
     * by mBeanServer.registerMBean(....)
     *
     * @throws Exception
     */
    @Test(expected = ApplicationServiceException.class)
    public void testInitWhenRegisterMBeanThrowsInstanceAlreadyExistsException() throws Exception {
        ApplicationServiceBean serviceBean = new ApplicationServiceBean(testAppService,
                testConfigAdminExt, mBeanServer);

        when(mBeanServer.registerMBean(any(Object.class), any(ObjectName.class)))
                .thenThrow(new NullPointerException());

        serviceBean.init();
    }

    /**
     * Tests the {@link ApplicationServiceBean#destroy()} method
     *
     * @throws Exception
     */
    @Test
    public void testDestroy() throws Exception {
        ApplicationServiceBean serviceBean = new ApplicationServiceBean(testAppService,
                testConfigAdminExt, mBeanServer);

        serviceBean.destroy();

        verify(mBeanServer).unregisterMBean(objectName);
    }

    /**
     * Tests the {@link ApplicationServiceBean#destroy()} method
     * for the case where an InstanceNotFoundException is thrown by mBeanServer.unregisterMBean(...)
     *
     * @throws Exception
     */
    @Test(expected = ApplicationServiceException.class)
    public void testDestroyWhenUnregisterMBeanThrowsInstanceNotFoundException() throws Exception {
        ApplicationServiceBean serviceBean = new ApplicationServiceBean(testAppService,
                testConfigAdminExt, mBeanServer);

        doThrow(new InstanceNotFoundException()).when(mBeanServer).unregisterMBean(objectName);

        serviceBean.destroy();
    }

    /**
     * Tests the {@link ApplicationServiceBean#destroy()} method
     * for the case where an MBeanRegistrationException is thrown
     * by mBeanServer.unregisterMBean(...)
     *
     * @throws Exception
     */
    @Test(expected = ApplicationServiceException.class)
    public void testDestroyWhenUnregisterMBeanThrowsMBeanRegistrationException() throws Exception {
        ApplicationServiceBean serviceBean = new ApplicationServiceBean(testAppService,
                testConfigAdminExt, mBeanServer);

        doThrow(new MBeanRegistrationException(new Exception())).when(mBeanServer)
                .unregisterMBean(any(ObjectName.class));

        serviceBean.destroy();
    }

    /**
     * Tests the {@link ApplicationServiceBean#getInstallationProfiles()} method
     *
     * @throws Exception
     */
    @Test
    public void testGetInstallationProfiles() throws Exception {
        Feature testFeature1 = mock(Feature.class);
        Feature testFeature2 = mock(Feature.class);
        Dependency testDependency1 = mock(Dependency.class);
        Dependency testDependency2 = mock(Dependency.class);

        when(testFeature1.getName()).thenReturn(TEST_FEATURE_NAME);
        when(testFeature2.getName()).thenReturn(TEST_FEATURE_NAME);
        when(testDependency1.getName()).thenReturn(TEST_FEATURE_NAME);
        when(testDependency2.getName()).thenReturn(TEST_FEATURE_NAME);
        when(testFeature1.getDescription()).thenReturn(TEST_FEATURE_DESCRIPTION);
        when(testFeature2.getDescription()).thenReturn(TEST_FEATURE_DESCRIPTION);

        List<Dependency> dependencies1 = new ArrayList<>();
        dependencies1.add(testDependency1);
        List<Dependency> dependencies2 = new ArrayList<>();
        dependencies2.add(testDependency2);

        when(testFeature1.getDependencies()).thenReturn(dependencies1);
        when(testFeature2.getDependencies()).thenReturn(dependencies2);

        List<Feature> featureList = new ArrayList<>();
        featureList.add(testFeature1);
        featureList.add(testFeature2);
        when(testAppService.getInstallationProfiles()).thenReturn(featureList);

        ApplicationServiceBean serviceBean = new ApplicationServiceBean(testAppService,
                testConfigAdminExt, mBeanServer);

        List<Map<String, Object>> result = serviceBean.getInstallationProfiles();

        assertThat("Should contain the nodes set up previously.",
                (String) result.get(0).get("name"), is(TEST_FEATURE_NAME));
        assertThat("Should have two entries.", result.size(), is(2));
    }

    /**
     * Tests the {@link ApplicationServiceBean#getApplicationTree()} method
     *
     * @throws Exception
     */
    @Test
    public void testGetApplicationTree() throws Exception {
        setUpTree();
        ApplicationServiceBean serviceBean = new ApplicationServiceBean(testAppService,
                testConfigAdminExt, mBeanServer);
        serviceBean.init();
        List<Map<String, Object>> result = serviceBean.getApplicationTree();

        assertThat("Should return the application nodes set up previously.",
                (String) result.get(0).get("name"), is(TEST_APP_NAME));
        assertThat("Size of root should be one.", result.size(), is(1));

        verify(testApp, atLeastOnce()).getName();
        verify(testNode1).getChildren();
    }

    /**
     * Tests the {@link ApplicationServiceBean#getApplications()} method
     *
     * @throws Exception
     */
    @Test
    public void testGetApplications() throws Exception {
        setUpTree();
        ApplicationServiceBean serviceBean = new ApplicationServiceBean(testAppService,
                testConfigAdminExt, mBeanServer);
        serviceBean.init();

        List<Map<String, Object>> result = serviceBean.getApplications();

        assertThat("Should return the application nodes set up previously.",
                (String) result.get(0).get("name"), is(TEST_APP_NAME));
        assertThat("Size of root should be two.", result.size(), is(2));

        verify(testApp, atLeastOnce()).getName();
        verify(testNode1).getChildren();
        verify(testNode1, atLeastOnce()).getApplication();
    }

    /**
     * Tests the {@link ApplicationServiceBean#getApplications()} method for the case where
     * the applications have no children
     *
     * @throws Exception
     */
    @Test
    public void testGetApplicationsNoChildren() throws Exception {
        setUpTree();
        ApplicationServiceBean serviceBean = new ApplicationServiceBean(testAppService,
                testConfigAdminExt, mBeanServer);
        serviceBean.init();

        when(testNode1.getChildren()).thenReturn((new TreeSet<ApplicationNode>()));
        when(testNode2.getChildren()).thenReturn((new TreeSet<ApplicationNode>()));
        when(testNode3.getChildren()).thenReturn((new TreeSet<ApplicationNode>()));

        List<Map<String, Object>> result = serviceBean.getApplications();

        assertThat("Should return the application nodes set up previously.",
                (String) result.get(0).get("name"), is(TEST_APP_NAME));
        assertThat("Size of root should be one.", result.size(), is(1));
    }

    /**
     * Tests the {@link ApplicationServiceBean#getApplications()} method for the case where
     * a child node has dependencies
     *
     * @throws Exception
     */
    @Test
    public void testGetApplicationsChildDependencies() throws Exception {
        setUpTree();
        ApplicationServiceBean serviceBean = new ApplicationServiceBean(testAppService,
                testConfigAdminExt, mBeanServer);
        serviceBean.init();

        ApplicationNode testNode4 = mock(ApplicationNodeImpl.class);
        when(testNode4.getApplication()).thenReturn(testApp);
        when(testNode4.getStatus()).thenReturn(testStatus);
        Set<ApplicationNode> testNode3ChildrenSet = new TreeSet<>();
        testNode3ChildrenSet.add(testNode4);
        when(testNode3.getChildren()).thenReturn(testNode3ChildrenSet);

        List<Map<String, Object>> result = serviceBean.getApplications();

        assertThat("Should return the applications set up previously.",
                (String) result.get(0).get("name"), is(TEST_APP_NAME));
        assertThat("Size of root should be three.", result.size(), is(3));
    }

    /**
     * Tests the {@link ApplicationServiceBean#getApplications()} method for the case where
     * more than one child node has dependencies
     *
     * @throws Exception
     */
    @Test
    public void testGetApplicationsMultiChildDependencies() throws Exception {
        setUpTree();
        ApplicationServiceBean serviceBean = new ApplicationServiceBean(testAppService,
                testConfigAdminExt, mBeanServer);
        serviceBean.init();

        ApplicationNode testNode4 = mock(ApplicationNodeImpl.class);
        when(testNode4.getApplication()).thenReturn(testApp);
        when(testNode4.getStatus()).thenReturn(testStatus);

        Set<ApplicationNode> testNode1ChildrenSet = new TreeSet<>();
        testNode1ChildrenSet.add(testNode2);
        testNode1ChildrenSet.add(testNode4);

        when(testNode1.getChildren()).thenReturn(testNode1ChildrenSet);

        List<Map<String, Object>> result = serviceBean.getApplications();

        assertThat("Should return the applications set up previously.",
                (String) result.get(0).get("name"), is(TEST_APP_NAME));
        assertThat("Size of root should be three.", result.size(), is(3));
    }

    /**
     * Tests the {@link ApplicationServiceBean#startApplication(String)} method
     *
     * @throws Exception
     */
    @Test
    public void testStartApplication() throws Exception {
        ApplicationServiceBean serviceBean = new ApplicationServiceBean(testAppService,
                testConfigAdminExt, mBeanServer);

        assertTrue(serviceBean.startApplication(TEST_APP_NAME));

        verify(testAppService).startApplication(TEST_APP_NAME);
    }

    /**
     * Tests the {@link ApplicationServiceBean#startApplication(String)} method for the case where
     * an exception is thrown
     *
     * @throws Exception
     */
    @Test
    public void testStartApplicationException() throws Exception {
        ApplicationServiceBean serviceBean = new ApplicationServiceBean(testAppService,
                testConfigAdminExt, mBeanServer);
        doThrow(new ApplicationServiceException()).when(testAppService)
                .startApplication(TEST_APP_NAME);

        assertFalse(serviceBean.startApplication(TEST_APP_NAME));

        verify(testAppService).startApplication(TEST_APP_NAME);
    }

    /**
     * Tests the {@link ApplicationServiceBean#stopApplication(String)}
     *
     * @throws Exception
     */
    @Test
    public void testStopApplication() throws Exception {
        ApplicationServiceBean serviceBean = new ApplicationServiceBean(testAppService,
                testConfigAdminExt, mBeanServer);

        assertTrue(serviceBean.stopApplication(TEST_APP_NAME));

        verify(testAppService).stopApplication(TEST_APP_NAME);
    }

    /**
     * Tests the {@link ApplicationServiceBean#stopApplication(String)} method for the case where
     * an exception is thrown
     *
     * @throws Exception
     */
    @Test
    public void testStopApplicationException() throws Exception {
        ApplicationServiceBean serviceBean = new ApplicationServiceBean(testAppService,
                testConfigAdminExt, mBeanServer);
        doThrow(new ApplicationServiceException()).when(testAppService)
                .stopApplication(TEST_APP_NAME);

        assertFalse(serviceBean.stopApplication(TEST_APP_NAME));
        verify(testAppService).stopApplication(TEST_APP_NAME);
    }

    /**
     * Tests the {@link ApplicationServiceBean#addApplications(List)} method
     *
     * @throws Exception
     */
    @Test
    public void testAddApplications() throws Exception {
        ApplicationServiceBean serviceBean = new ApplicationServiceBean(testAppService,
                testConfigAdminExt, mBeanServer);
        List<Map<String, Object>> testURLList = new ArrayList<>();
        Map<String, Object> testURLMap1 = mock(HashMap.class);
        when(testURLMap1.get("value")).thenReturn(TEST_URL);
        Map<String, Object> testURLMap2 = mock(HashMap.class);
        when(testURLMap2.get("value")).thenReturn(TEST_URL);
        testURLList.add(testURLMap1);
        testURLList.add(testURLMap2);

        serviceBean.addApplications(testURLList);

        verify(testURLMap1).get("value");
        verify(testURLMap2).get("value");
    }

    /**
     * Tests the {@link ApplicationServiceBean#addApplications(List)} method
     * for the case where a URISyntaxException is caught
     *
     * @throws Exception
     */
    @Test
    public void testAddApplicationsUSE() throws Exception {
        ApplicationServiceBean serviceBean = new ApplicationServiceBean(testAppService,
                testConfigAdminExt, mBeanServer);
        List<Map<String, Object>> testURLList = new ArrayList<>();
        Map<String, Object> testURLMap1 = mock(HashMap.class);
        when(testURLMap1.get("value")).thenReturn(BAD_URL);
        testURLList.add(testURLMap1);

        serviceBean.addApplications(testURLList);
        verify(testURLMap1, Mockito.times(2)).get("value");
    }

    /**
     * Tests the {@link ApplicationServiceBean#addApplications(List)} method
     * for the case where an ApplicationServiceException is thrown
     *
     * @throws Exception
     */
    @Test
    public void testAddApplicationsASE() throws Exception {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory
                .getLogger(Logger.ROOT_LOGGER_NAME);
        final Appender mockAppender = mock(Appender.class);
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);
        root.setLevel(Level.ALL);

        ApplicationServiceBean serviceBean = new ApplicationServiceBean(testAppService,
                testConfigAdminExt, mBeanServer);
        List<Map<String, Object>> testURLList = new ArrayList<>();
        Map<String, Object> testURLMap1 = mock(HashMap.class);
        when(testURLMap1.get("value")).thenReturn(TEST_URL);
        Map<String, Object> testURLMap2 = mock(HashMap.class);
        when(testURLMap2.get("value")).thenReturn(TEST_URL);
        testURLList.add(testURLMap1);
        testURLList.add(testURLMap2);

        doThrow(new ApplicationServiceException()).when(testAppService)
                .addApplication(any(URI.class));

        serviceBean.addApplications(testURLList);

        verify(mockAppender, times(2)).doAppend(argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
                return ((LoggingEvent) argument).getFormattedMessage().contains(ADD_APP_ASE);
            }
        }));
    }

    /**
     * Tests the {@link ApplicationServiceBean#removeApplication(String)} method for the case where
     * the string parameter is valid
     *
     * @throws Exception
     */
    @Test
    public void testRemoveApplication() throws Exception {
        ApplicationServiceBean serviceBean = new ApplicationServiceBean(testAppService,
                testConfigAdminExt, mBeanServer);

        serviceBean.removeApplication(TEST_APP_NAME);

        verify(testAppService).removeApplication(TEST_APP_NAME);
    }

    /**
     * Tests the {@link ApplicationServiceBean#removeApplication(String)} method for the case where
     * the string parameter is invalid
     *
     * @throws Exception
     */
    @Test
    public void testRemoveApplicationInvalidParam() throws Exception {
        ApplicationServiceBean serviceBean = new ApplicationServiceBean(testAppService,
                testConfigAdminExt, mBeanServer);

        serviceBean.removeApplication(StringUtils.EMPTY);

        verifyNoMoreInteractions(testAppService);
    }

    /**
     * Tests the {@link ApplicationServiceBean#removeApplication(String)} method
     * for the case where an ApplicationServiceException is thrown by the AppService
     *
     * @throws Exception
     */
    @Test
    public void testRemoveApplicationASE() throws Exception {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory
                .getLogger(Logger.ROOT_LOGGER_NAME);
        final Appender mockAppender = mock(Appender.class);
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);
        root.setLevel(Level.ALL);

        ApplicationServiceBean serviceBean = new ApplicationServiceBean(testAppService,
                testConfigAdminExt, mBeanServer);

        doThrow(new ApplicationServiceException()).when(testAppService)
                .removeApplication(any(String.class));

        serviceBean.removeApplication(TEST_APP_NAME);

        verify(mockAppender).doAppend(argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
                return ((LoggingEvent) argument).getFormattedMessage().contains(REMOVE_APP_ASE);
            }
        }));
    }

    /**
     * Tests the {@link ApplicationServiceBean#getServices(String)} method
     *
     * @throws Exception
     */
    @Test
    public void testGetServices() throws Exception {
        ApplicationServiceBean serviceBean = new ApplicationServiceBean(testAppService,
                testConfigAdminExt, mBeanServer) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };
        Bundle testBundle = mock(Bundle.class);
        Bundle[] bundles = {testBundle};
        when(bundleContext.getBundles()).thenReturn(bundles);

        List<Map<String, Object>> services = new ArrayList<>();
        Map<String, Object> testService1 = new HashMap<>();
        List<Map<String, Object>> testService1Configs = new ArrayList<>();
        Map<String, Object> testConfig1 = new HashMap<>();
        testConfig1.put("bundle_location", TEST_LOCATION);
        testService1Configs.add(testConfig1);
        services.add(testService1);
        testService1.put("configurations", testService1Configs);

        BundleInfo testBundle1 = mock(BundleInfo.class);
        Set<BundleInfo> testBundles = new HashSet<>();
        testBundles.add(testBundle1);

        when(testApp.getBundles()).thenReturn(testBundles);
        when(testBundle1.getLocation()).thenReturn(TEST_LOCATION);
        when(testAppService.getApplication(TEST_APP_NAME)).thenReturn(testApp);
        when(testConfigAdminExt.listServices(Mockito.any(String.class), Mockito.any(String.class)))
                .thenReturn(services);

        assertThat("Should find the given services.", serviceBean.getServices(TEST_APP_NAME).get(0),
                is(testService1));
    }

    /**
     * Tests the {@link ApplicationServiceBean#getServices(String)} method
     * for the case where the services do not have the "configurations" key
     *
     * @throws Exception
     */
    @Test
    public void testGetServicesNotContainsKey() throws Exception {
        ApplicationServiceBean serviceBean = new ApplicationServiceBean(testAppService,
                testConfigAdminExt, mBeanServer) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };
        Bundle testBundle = mock(Bundle.class);
        Bundle[] bundles = {testBundle};
        when(bundleContext.getBundles()).thenReturn(bundles);

        List<Map<String, Object>> services = new ArrayList<>();
        Map<String, Object> testService2 = mock(HashMap.class);
        Map<String, Object> testService1 = mock(HashMap.class);
        services.add(testService1);
        services.add(testService2);
        when(testService1.get("factory")).thenReturn(true);
        when(testService2.get("factory")).thenReturn(false);

        BundleInfo testBundle1 = mock(BundleInfo.class);
        Set<BundleInfo> testBundles = new HashSet<>();
        testBundles.add(testBundle1);

        when(testApp.getBundles()).thenReturn(testBundles);
        when(testBundle1.getLocation()).thenReturn(TEST_LOCATION);
        when(testAppService.getApplication(TEST_APP_NAME)).thenReturn(testApp);
        when(testConfigAdminExt.listServices(Mockito.any(String.class), Mockito.any(String.class)))
                .thenReturn(services);

        assertThat("Should not find any services.", serviceBean.getServices(TEST_APP_NAME),
                is(ListUtils.EMPTY_LIST));
    }

    /**
     * Tests the {@link ApplicationServiceBean#getServices(String)} method
     * for the case where the services do not have the "configurations" key
     * and there is MetatypeInformation present for each service.
     * <p>
     * This test mostly just checks that
     *
     * @throws Exception
     */
    @Test
    public void testGetServicesMetatypeInfo() throws Exception {
        ApplicationServiceBean serviceBean = new ApplicationServiceBean(testAppService,
                testConfigAdminExt, mBeanServer) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };

        ServiceTracker testServiceTracker = mock(ServiceTracker.class);
        serviceBean.setServiceTracker(testServiceTracker);
        MetaTypeService testMTS = mock(MetaTypeService.class);
        MetaTypeInformation testMTI = mock(MetaTypeInformation.class);
        when(testServiceTracker.getService()).thenReturn(testMTS);
        when(testMTS.getMetaTypeInformation(any(Bundle.class))).thenReturn(testMTI);
        when(testMTI.getPids()).thenReturn(new String[] {"001", "002"});
        when(testMTI.getFactoryPids()).thenReturn(new String[] {"001", "002"});

        Bundle testBundle = mock(Bundle.class);
        Bundle[] bundles = {testBundle};
        when(bundleContext.getBundles()).thenReturn(bundles);
        when(testBundle.getLocation()).thenReturn(TEST_LOCATION);

        List<Map<String, Object>> services = new ArrayList<>();
        Map<String, Object> testService2 = mock(HashMap.class);
        Map<String, Object> testService1 = mock(HashMap.class);
        services.add(testService1);
        services.add(testService2);

        List<Map<String, Object>> testService1Configs = new ArrayList<>();
        Map<String, Object> testConfig1 = new HashMap<>();
        testConfig1.put("bundle_location", TEST_LOCATION);
        testService1Configs.add(testConfig1);

        List<Map<String, Object>> testService2Configs = new ArrayList<>();
        Map<String, Object> testConfig2 = new HashMap<>();
        testConfig2.put("bundle_location", TEST_LOCATION);
        testService1Configs.add(testConfig2);

        when(testService1.get("factory")).thenReturn(true);
        when(testService2.get("factory")).thenReturn(false);
        when(testService1.get("configurations")).thenReturn(testService1Configs);
        when(testService2.get("configurations")).thenReturn(testService2Configs);
        when(testService1.get("id")).thenReturn("001");
        when(testService2.get("id")).thenReturn("002");

        BundleInfo testBundle1 = mock(BundleInfo.class);
        Set<BundleInfo> testBundles = new HashSet<>();
        testBundles.add(testBundle1);

        when(testApp.getBundles()).thenReturn(testBundles);
        when(testBundle1.getLocation()).thenReturn(TEST_LOCATION);
        when(testAppService.getApplication(TEST_APP_NAME)).thenReturn(testApp);
        when(testConfigAdminExt.listServices(Mockito.any(String.class), Mockito.any(String.class)))
                .thenReturn(services);

        assertThat("Should find the given services.", serviceBean.getServices(TEST_APP_NAME).get(0),
                is(testService1));
    }

    /**
     * Tests the {@link ApplicationServiceBean#getServices(String)} method
     * for the case where an ApplicationServiceException is thrown
     *
     * @throws Exception
     */
    @Test
    public void testGetServicesASE() throws Exception {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory
                .getLogger(Logger.ROOT_LOGGER_NAME);
        final Appender mockAppender = mock(Appender.class);
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);
        root.setLevel(Level.ALL);

        ApplicationServiceBean serviceBean = new ApplicationServiceBean(testAppService,
                testConfigAdminExt, mBeanServer) {
            @Override
            protected BundleContext getContext() {
                return bundleContext;
            }
        };
        Bundle testBundle = mock(Bundle.class);
        Bundle[] bundles = {testBundle};
        when(bundleContext.getBundles()).thenReturn(bundles);

        List<Map<String, Object>> services = new ArrayList<>();
        Map<String, Object> testService1 = new HashMap<>();
        services.add(testService1);

        doThrow(new ApplicationServiceException()).when(testApp).getBundles();
        when(testAppService.getApplication(TEST_APP_NAME)).thenReturn(testApp);
        when(testConfigAdminExt.listServices(Mockito.any(String.class), Mockito.any(String.class)))
                .thenReturn(services);

        serviceBean.getServices(TEST_APP_NAME);

        verify(mockAppender).doAppend(argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
                return ((LoggingEvent) argument).getFormattedMessage().contains(GET_SERV_ASE);
            }
        }));
    }

    /**
     * Tests the {@link ApplicationServiceBean#getApplicationPlugins()} method
     * and the {@link ApplicationServiceBean#setApplicationPlugins(List)} method
     *
     * @throws Exception
     */
    @Test
    public void testGetSetApplicationPlugins() throws Exception {
        ApplicationServiceBean serviceBean = new ApplicationServiceBean(testAppService,
                testConfigAdminExt, mBeanServer);
        ApplicationPlugin testPlugin1 = mock(ApplicationPlugin.class);
        ApplicationPlugin testPlugin2 = mock(ApplicationPlugin.class);
        List<ApplicationPlugin> pluginList = new ArrayList<>();
        pluginList.add(testPlugin1);
        pluginList.add(testPlugin2);

        serviceBean.setApplicationPlugins(pluginList);

        assertEquals(pluginList, serviceBean.getApplicationPlugins());
    }

    /**
     * Tests the {@link ApplicationServiceBean#getAllFeatures()} method
     *
     * @throws Exception
     */
    @Test
    public void testGetAllFeatures() throws Exception {
        ApplicationServiceBean serviceBean = new ApplicationServiceBean(testAppService,
                testConfigAdminExt, mBeanServer);
        List<FeatureDetails> testFeatureDetailsList = new ArrayList<>();
        FeatureDetails testFeatureDetails1 = mock(FeatureDetails.class);
        testFeatureDetailsList.add(testFeatureDetails1);
        when(testFeatureDetails1.getName()).thenReturn(TEST_FEATURE_DETAILS);
        when(testFeatureDetails1.getVersion()).thenReturn(TEST_VERSION);
        when(testFeatureDetails1.getStatus()).thenReturn(TEST_FEATURE_STATUS);
        when(testFeatureDetails1.getRepository()).thenReturn(TEST_REPO_NAME);

        when(testAppService.getAllFeatures()).thenReturn(testFeatureDetailsList);

        assertThat("Features returned should match testFeatureDetailsList features",
                (String) serviceBean.getAllFeatures().get(0).get("name"),
                is(testFeatureDetailsList.get(0).getName()));
        verify(testAppService).getAllFeatures();
    }

    /**
     * Tests the {@link ApplicationServiceBean#findApplicationFeatures(String)} method
     *
     * @throws Exception
     */
    @Test
    public void testFindApplicationFeatures() throws Exception {
        ApplicationServiceBean serviceBean = new ApplicationServiceBean(testAppService,
                testConfigAdminExt, mBeanServer);

        List<FeatureDetails> testFeatureDetailsList = new ArrayList<>();
        FeatureDetails testFeatureDetails1 = mock(FeatureDetails.class);
        testFeatureDetailsList.add(testFeatureDetails1);
        when(testFeatureDetails1.getName()).thenReturn(TEST_FEATURE_DETAILS);
        when(testFeatureDetails1.getVersion()).thenReturn(TEST_VERSION);
        when(testFeatureDetails1.getStatus()).thenReturn(TEST_FEATURE_STATUS);
        when(testFeatureDetails1.getRepository()).thenReturn(TEST_REPO_NAME);

        when(testAppService.findApplicationFeatures(TEST_APP_NAME))
                .thenReturn(testFeatureDetailsList);

        assertThat("Features returned should match testFeatureDetailsList features",
                (String) serviceBean.findApplicationFeatures(TEST_APP_NAME).get(0).get("name"),
                is(testFeatureDetailsList.get(0).getName()));
        verify(testAppService).findApplicationFeatures(TEST_APP_NAME);
    }

    /**
     * Tests the {@link ApplicationServiceBean#getPluginsForApplication(String)} method
     *
     * @throws Exception
     */
    @Test
    public void testGetPluginsForApplication() throws Exception {
        ApplicationServiceBean serviceBean = new ApplicationServiceBean(testAppService,
                testConfigAdminExt, mBeanServer);
        ApplicationPlugin testPlugin1 = mock(ApplicationPlugin.class);
        ApplicationPlugin testPlugin2 = mock(ApplicationPlugin.class);
        List<ApplicationPlugin> pluginList = new ArrayList<>();
        pluginList.add(testPlugin1);
        pluginList.add(testPlugin2);
        Map<String, Object> plugin1JSON = new HashMap<>();
        plugin1JSON.put("TestAppJSON", "Plugin1");
        Map<String, Object> plugin2JSON = new HashMap<>();
        plugin2JSON.put("TestAppJSON", "Plugin2");

        when(testPlugin1.matchesAssocationName(TEST_APP_NAME)).thenReturn(true);
        when(testPlugin2.matchesAssocationName(TEST_APP_NAME)).thenReturn(true);
        when(testPlugin1.toJSON()).thenReturn(plugin1JSON);
        when(testPlugin2.toJSON()).thenReturn(plugin2JSON);

        serviceBean.setApplicationPlugins(pluginList);

        assertThat("Should return the list of plugins given to it.",
                serviceBean.getPluginsForApplication(TEST_APP_NAME).get(0), is(plugin1JSON));
    }
}
