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
package org.codice.ddf.registry.source.configuration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.xml.XmlParser;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.federationadmin.service.FederationAdminService;
import org.codice.ddf.registry.schemabindings.helper.MetacardMarshaller;
import org.codice.ddf.registry.schemabindings.helper.RegistryPackageTypeHelper;
import org.codice.ddf.registry.schemabindings.helper.SlotTypeHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.event.Event;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;

public class SourceConfigurationHandlerTest {
    private Parser parser;

    private FederationAdminService adminService;

    private ExecutorService executorService;

    private ConfigurationAdmin configAdmin;

    private MetaTypeService metaTypeService;

    private BundleContext bundleContext;

    private MetaTypeInformation mti;

    private ObjectClassDefinition ocd;

    private SourceConfigurationHandler sch;

    private Configuration config;

    private MetacardImpl mcard;

    private Event createEvent;

    private Event updateEvent;

    private Event deleteEvent;

    @Before
    public void setUp() throws Exception {
        parser = new XmlParser();
        adminService = mock(FederationAdminService.class);
        configAdmin = mock(ConfigurationAdmin.class);
        metaTypeService = mock(MetaTypeService.class);
        bundleContext = mock(BundleContext.class);
        executorService = mock(ExecutorService.class);

        sch = new SourceConfigurationHandler(adminService, executorService) {
            @Override
            protected BundleContext getBundleContext() {
                return bundleContext;
            }
        };
        sch.setMetacardMarshaller(new MetacardMarshaller(parser));
        sch.setConfigurationAdmin(configAdmin);
        sch.setMetaTypeService(metaTypeService);
        sch.setSlotHelper(new SlotTypeHelper());
        sch.setRegistryTypeHelper(new RegistryPackageTypeHelper());
        sch.setActivateConfigurations(false);
        sch.setPreserveActiveConfigurations(true);
        sch.setUrlBindingName("urlBindingName");
        sch.setBindingTypeFactoryPid(Collections.singletonList("CSW_2.0.2=Csw_Federated_Source"));
        sch.setSourceActivationPriorityOrder(Collections.singletonList("CSW_2.0.2"));
        sch.setCleanUpOnDelete(false);

        Bundle bundle = mock(Bundle.class);
        mti = mock(MetaTypeInformation.class);
        ocd = mock(ObjectClassDefinition.class);
        config = mock(Configuration.class);

        AttributeDefinition adi = new AttributeDefinitionImpl("attId",
                "attName",
                "attDesc",
                "attValue");

        when(adminService.getRegistryMetacards()).thenReturn(new ArrayList());

        when(bundleContext.getBundles()).thenReturn(new Bundle[] {bundle});
        when(configAdmin.listConfigurations("(id=TestRegNode")).thenReturn(null);
        when(configAdmin.listConfigurations("(registry-id=urn:uuid:2014ca7f59ac46f495e32b4a67a51276")).thenReturn(
                null);
        when(metaTypeService.getMetaTypeInformation(any(Bundle.class))).thenReturn(mti);
        when(mti.getObjectClassDefinition(anyString(), anyString())).thenReturn(ocd);
        when(ocd.getAttributeDefinitions(anyInt())).thenReturn(new AttributeDefinition[] {adi});
        when(configAdmin.createFactoryConfiguration(anyString(), anyString())).thenReturn(config);

        mcard = new MetacardImpl(new RegistryObjectMetacardType());
        mcard.setTags(Collections.singleton(RegistryConstants.REGISTRY_TAG));
        mcard.setId("2014ca7f59ac46f495e32b4a67a51276");
        mcard.setAttribute(RegistryObjectMetacardType.REGISTRY_ID,
                "urn:uuid:2014ca7f59ac46f495e32b4a67a51276");
        mcard.setAttribute(RegistryObjectMetacardType.REGISTRY_IDENTITY_NODE, true);
        mcard.setMetadata(getMetadata("/csw-rim-node-csw-binding.xml"));
        mcard.setTitle("TestRegNode");

        Dictionary<String, Object> eventProperties = new Hashtable<>();
        eventProperties.put("ddf.catalog.event.metacard", mcard);
        createEvent = new Event("ddf/catalog/event/CREATED", eventProperties);
        updateEvent = new Event("ddf/catalog/event/UPDATED", eventProperties);
        deleteEvent = new Event("ddf/catalog/event/DELETED", eventProperties);

    }

    @Test
    public void testNullMetacard() {
        Dictionary<String, Object> eventProperties = new Hashtable<>();
        Event event = new Event("ddf/catalog/event/CREATED", eventProperties);
        sch.handleEvent(event);
        verify(executorService, never()).execute(any(Runnable.class));
    }

    @Test
    public void testNonRegistryMetacard() {
        MetacardImpl metacard = new MetacardImpl();
        metacard.setAttribute(Metacard.TAGS, Metacard.DEFAULT_TAG);
        Dictionary<String, Object> eventProperties = new Hashtable<>();
        eventProperties.put("ddf.catalog.event.metacard", metacard);
        Event event = new Event("ddf/catalog/event/CREATED", eventProperties);
        sch.handleEvent(event);
        verify(executorService, never()).execute(any(Runnable.class));
    }

    @Test
    public void testRegistryMetacardExecutorCall() {
        MetacardImpl mcard = new MetacardImpl();
        mcard.setAttribute(Metacard.TAGS, RegistryConstants.REGISTRY_TAG);
        Dictionary<String, Object> eventProperties = new Hashtable<>();
        eventProperties.put("ddf.catalog.event.metacard", mcard);
        Event event = new Event("ddf/catalog/event/CREATED", eventProperties);
        sch.handleEvent(event);
        event = new Event("ddf/catalog/event/UPDATED", eventProperties);
        sch.handleEvent(event);
        event = new Event("ddf/catalog/event/DELETED", eventProperties);
        sch.handleEvent(event);
        verify(executorService, times(3)).execute(any(Runnable.class));
    }

    @Test
    public void testDefaultConfigurationLocalNodeCreate() throws Exception {

        ArgumentCaptor<Dictionary> captor = ArgumentCaptor.forClass(Dictionary.class);
        setupSerialExecutor();
        sch.handleEvent(createEvent);

        verify(configAdmin, times(1)).createFactoryConfiguration("Csw_Federated_Source_disabled",
                null);
        verify(config).update(captor.capture());
        Dictionary passedValues = captor.getValue();
        assertThat(passedValues.get("attId"), equalTo("attValue"));
        assertCswProperties(passedValues);

    }

    @Test
    public void testDefaultConfigurationLocalNodeCreateNoTitle() throws Exception {

        ArgumentCaptor<Dictionary> captor = ArgumentCaptor.forClass(Dictionary.class);
        mcard.setAttribute(Metacard.TITLE, null);
        setupSerialExecutor();
        sch.handleEvent(createEvent);

        verify(configAdmin, times(1)).createFactoryConfiguration("Csw_Federated_Source_disabled",
                null);
        verify(config).update(captor.capture());
        Dictionary passedValues = captor.getValue();
        assertThat(passedValues.get("attId"), equalTo("attValue"));
        assertThat(passedValues.get("urlBindingName"), equalTo("cswUrl"));
        assertThat(passedValues.get("id"), equalTo("urn:uuid:2014ca7f59ac46f495e32b4a67a51276"));
        assertThat(passedValues.get("cswUrl"), equalTo("https://localhost:1234/mycsw/endpoint"));
        assertThat(passedValues.get("bindingType"), equalTo("CSW_2.0.2"));
        assertThat(passedValues.get("customSlot"), equalTo("customValue"));

    }

    @Test
    public void testDefaultConfigurationLocalNodeCreateNoBindingType() throws Exception {

        mcard.setMetadata(this.getMetadata("/csw-rim-node-missing-type-binding.xml"));
        setupSerialExecutor();
        sch.handleEvent(createEvent);

        verify(configAdmin, never()).createFactoryConfiguration(anyString(), anyString());
    }

    @Test
    public void testDefaultConfigurationRemoteNodeCreate() throws Exception {

        ArgumentCaptor<Dictionary> captor = ArgumentCaptor.forClass(Dictionary.class);

        mcard.setAttribute(RegistryObjectMetacardType.REGISTRY_IDENTITY_NODE, null);

        setupSerialExecutor();
        sch.handleEvent(createEvent);

        verify(configAdmin, times(1)).createFactoryConfiguration("Csw_Federated_Source_disabled",
                null);
        verify(config).update(captor.capture());
        Dictionary passedValues = captor.getValue();
        assertThat(passedValues.get("attId"), equalTo("attValue"));
        assertCswProperties(passedValues);

    }

    @Test
    public void testConfigurationCreateActivateConfig() throws Exception {

        ArgumentCaptor<Dictionary> captor = ArgumentCaptor.forClass(Dictionary.class);
        mcard.setAttribute(RegistryObjectMetacardType.REGISTRY_IDENTITY_NODE, null);
        sch.setActivateConfigurations(true);
        setupSerialExecutor();
        sch.handleEvent(createEvent);

        verify(configAdmin, times(1)).createFactoryConfiguration("Csw_Federated_Source", null);
        verify(config).update(captor.capture());
        Dictionary passedValues = captor.getValue();
        assertThat(passedValues.get("attId"), equalTo("attValue"));
        assertCswProperties(passedValues);

    }

    @Test
    public void testConfigurationCreateActivateConfigWithExistingDisabledConfig() throws Exception {

        ArgumentCaptor<Dictionary> captor = ArgumentCaptor.forClass(Dictionary.class);
        mcard.setAttribute(RegistryObjectMetacardType.REGISTRY_IDENTITY_NODE, null);
        when(configAdmin.listConfigurations(anyString())).thenReturn(new Configuration[] {config});
        Hashtable<String, Object> props = new Hashtable<>();
        props.put("id", "TestRegNode");
        props.put("origConfig", "origConfigValue");
        props.put(RegistryObjectMetacardType.REGISTRY_ID,
                "urn:uuid:2014ca7f59ac46f495e32b4a67a51276");
        when(config.getProperties()).thenReturn(props);
        when(config.getFactoryPid()).thenReturn("Csw_Federated_Source_disabled");
        sch.setActivateConfigurations(true);
        setupSerialExecutor();
        sch.handleEvent(createEvent);
        verify(config, times(1)).delete();
        verify(configAdmin, times(1)).createFactoryConfiguration("Csw_Federated_Source", null);
        verify(config, times(2)).update(captor.capture());
        Dictionary passedValues = captor.getAllValues()
                .get(1);
        assertCswProperties(passedValues);
        assertThat(passedValues.get("origConfig"), equalTo("origConfigValue"));
    }

    @Test
    public void testConfigurationCreateActivateConfigWithExistingMatchingActiveConfig()
            throws Exception {

        ArgumentCaptor<Dictionary> captor = ArgumentCaptor.forClass(Dictionary.class);
        mcard.setAttribute(RegistryObjectMetacardType.REGISTRY_IDENTITY_NODE, null);
        when(configAdmin.listConfigurations(anyString())).thenReturn(new Configuration[] {config});
        Hashtable<String, Object> props = new Hashtable<>();
        props.put("id", "TestRegNode");
        props.put("origConfig", "origConfigValue");
        props.put(RegistryObjectMetacardType.REGISTRY_ID,
                "urn:uuid:2014ca7f59ac46f495e32b4a67a51276");
        when(config.getProperties()).thenReturn(props);
        when(config.getFactoryPid()).thenReturn("Csw_Federated_Source");
        sch.setActivateConfigurations(true);
        setupSerialExecutor();
        sch.handleEvent(createEvent);
        verify(config, never()).delete();
        verify(configAdmin, never()).createFactoryConfiguration("Csw_Federated_Source", null);
        verify(config).update(captor.capture());
        Dictionary passedValues = captor.getValue();
        assertCswProperties(passedValues);
        assertThat(passedValues.get("origConfig"), equalTo("origConfigValue"));
    }

    @Test
    public void testConfigurationCreateActivateConfigWithExistingDifferentActiveConfig()
            throws Exception {

        ArgumentCaptor<Dictionary> captor = ArgumentCaptor.forClass(Dictionary.class);
        mcard.setAttribute(RegistryObjectMetacardType.REGISTRY_IDENTITY_NODE, null);
        when(configAdmin.listConfigurations(anyString())).thenReturn(new Configuration[] {config});
        Hashtable<String, Object> props = new Hashtable<>();
        props.put("id", "TestRegNode");
        props.put("origConfig", "origConfigValue");
        props.put(RegistryObjectMetacardType.REGISTRY_ID,
                "urn:uuid:2014ca7f59ac46f495e32b4a67a51276");
        Configuration newConfig = mock(Configuration.class);
        when(newConfig.getFactoryPid()).thenReturn("Some_Other_Source_disabled");
        when(configAdmin.createFactoryConfiguration("Some_Other_Source_disabled", null)).thenReturn(
                newConfig);
        when(config.getProperties()).thenReturn(props);
        when(config.getFactoryPid()).thenReturn("Some_Other_Source");
        sch.setActivateConfigurations(true);
        setupSerialExecutor();
        sch.handleEvent(createEvent);
        verify(config, times(1)).delete();
        verify(configAdmin, times(1)).createFactoryConfiguration("Csw_Federated_Source", null);
        verify(config).update(captor.capture());
        List<Dictionary> values = captor.getAllValues();
        assertThat(values.size(), equalTo(1));
        Dictionary passedValues = values.get(0);
        assertCswProperties(passedValues);
    }

    @Test
    public void testConfigurationCreateNoBindingType() throws Exception {

        mcard.setAttribute(RegistryObjectMetacardType.REGISTRY_IDENTITY_NODE, null);
        mcard.setMetadata(getMetadata("/csw-rim-node-no-binding.xml"));
        sch.setActivateConfigurations(true);
        setupSerialExecutor();
        sch.handleEvent(createEvent);
        verify(config, never()).delete();
        verify(configAdmin, never()).createFactoryConfiguration(anyString(), anyString());
    }

    @Test
    public void testConfigurationUpdateActivateConfigWithExistingDifferentActiveConfig()
            throws Exception {

        ArgumentCaptor<Dictionary> captor = ArgumentCaptor.forClass(Dictionary.class);
        mcard.setAttribute(RegistryObjectMetacardType.REGISTRY_IDENTITY_NODE, null);
        when(configAdmin.listConfigurations(anyString())).thenReturn(new Configuration[] {config});
        Hashtable<String, Object> props = new Hashtable<>();
        props.put("id", "TestRegNode");
        props.put("origConfig", "origConfigValue");
        props.put(RegistryObjectMetacardType.REGISTRY_ID,
                "urn:uuid:2014ca7f59ac46f495e32b4a67a51276");
        when(config.getProperties()).thenReturn(props);
        when(config.getFactoryPid()).thenReturn("Some_Other_Source");
        sch.setActivateConfigurations(true);
        setupSerialExecutor();
        sch.handleEvent(updateEvent);
        verify(configAdmin, times(1)).createFactoryConfiguration("Csw_Federated_Source_disabled",
                null);
        verify(config).update(captor.capture());
        List<Dictionary> values = captor.getAllValues();
        assertThat(values.size(), equalTo(1));
        Dictionary passedValues = values.get(0);
        assertCswProperties(passedValues);
    }

    @Test
    public void testConfigurationUpdateActivateConfigWithExistingDifferentActiveConfigPreserveActiveFalse()
            throws Exception {

        ArgumentCaptor<Dictionary> captor = ArgumentCaptor.forClass(Dictionary.class);
        mcard.setAttribute(RegistryObjectMetacardType.REGISTRY_IDENTITY_NODE, null);
        when(configAdmin.listConfigurations(anyString())).thenReturn(new Configuration[] {config});
        Hashtable<String, Object> props = new Hashtable<>();
        props.put("id", "TestRegNode");
        props.put("origConfig", "origConfigValue");
        props.put(RegistryObjectMetacardType.REGISTRY_ID,
                "urn:uuid:2014ca7f59ac46f495e32b4a67a51276");
        Configuration newConfig = mock(Configuration.class);
        when(newConfig.getFactoryPid()).thenReturn("Some_Other_Source_disabled");
        when(configAdmin.createFactoryConfiguration("Some_Other_Source_disabled", null)).thenReturn(
                newConfig);
        when(config.getProperties()).thenReturn(props);
        when(config.getFactoryPid()).thenReturn("Some_Other_Source");
        sch.setPreserveActiveConfigurations(false);
        sch.setActivateConfigurations(true);
        setupSerialExecutor();
        sch.handleEvent(updateEvent);
        verify(config, times(1)).delete();
        verify(configAdmin, times(1)).createFactoryConfiguration("Csw_Federated_Source", null);
        verify(config).update(captor.capture());
        List<Dictionary> values = captor.getAllValues();
        assertThat(values.size(), equalTo(1));
        Dictionary passedValues = values.get(0);
        assertCswProperties(passedValues);
    }

    @Test
    public void testConfigurationUpdateActivateConfigNotFirstPriority() throws Exception {

        ArgumentCaptor<Dictionary> captor = ArgumentCaptor.forClass(Dictionary.class);
        mcard.setAttribute(RegistryObjectMetacardType.REGISTRY_IDENTITY_NODE, null);
        when(configAdmin.listConfigurations(anyString())).thenReturn(new Configuration[] {config});
        Hashtable<String, Object> props = new Hashtable<>();
        props.put("id", "TestRegNode");
        props.put("origConfig", "origConfigValue");
        props.put(RegistryObjectMetacardType.REGISTRY_ID,
                "urn:uuid:2014ca7f59ac46f495e32b4a67a51276");
        when(config.getProperties()).thenReturn(props);
        when(config.getFactoryPid()).thenReturn("Some_Other_Source");
        Configuration newConfig = mock(Configuration.class);
        when(newConfig.getFactoryPid()).thenReturn("Some_Other_Source_disabled");
        when(configAdmin.createFactoryConfiguration("Some_Other_Source_disabled", null)).thenReturn(
                newConfig);
        List<String> priority = new ArrayList();
        priority.add("Top_Priority_Source");
        priority.add("CSW_2.0.2");
        sch.setSourceActivationPriorityOrder(priority);
        sch.setPreserveActiveConfigurations(false);
        sch.setActivateConfigurations(true);
        setupSerialExecutor();
        sch.handleEvent(updateEvent);
        verify(config, times(1)).delete();
        verify(configAdmin, times(1)).createFactoryConfiguration("Csw_Federated_Source", null);
        verify(configAdmin, times(1)).createFactoryConfiguration("Some_Other_Source_disabled",
                null);
        verify(config).update(captor.capture());
        List<Dictionary> values = captor.getAllValues();
        assertThat(values.size(), equalTo(1));
        Dictionary passedValues = values.get(0);
        assertCswProperties(passedValues);
    }

    @Test
    public void testConfigurationUpdateActivateConfigActivePriorityNotAvailable() throws Exception {

        ArgumentCaptor<Dictionary> captor = ArgumentCaptor.forClass(Dictionary.class);
        mcard.setAttribute(RegistryObjectMetacardType.REGISTRY_IDENTITY_NODE, null);
        when(configAdmin.listConfigurations(anyString())).thenReturn(new Configuration[] {config});
        Hashtable<String, Object> props = new Hashtable<>();
        props.put("id", "TestRegNode");
        props.put("origConfig", "origConfigValue");
        props.put(RegistryObjectMetacardType.REGISTRY_ID,
                "urn:uuid:2014ca7f59ac46f495e32b4a67a51276");
        Configuration newConfig = mock(Configuration.class);
        when(newConfig.getFactoryPid()).thenReturn("Some_Other_Source_disabled");
        when(configAdmin.createFactoryConfiguration("Some_Other_Source_disabled", null)).thenReturn(
                newConfig);
        when(config.getProperties()).thenReturn(props);
        when(config.getFactoryPid()).thenReturn("Some_Other_Source");
        List<String> priority = new ArrayList<>();
        priority.add("Top_Priority_Source");
        priority.add("CSW_2.0.2");
        List<String> bindings = new ArrayList<>();
        bindings.add("Top_Priority_Source=Some_Other_Source");
        bindings.add("CSW_2.0.2=Csw_Federated_Source");
        sch.setBindingTypeFactoryPid(bindings);
        sch.setSourceActivationPriorityOrder(priority);
        sch.setPreserveActiveConfigurations(false);
        sch.setActivateConfigurations(true);
        setupSerialExecutor();
        sch.handleEvent(updateEvent);
        verify(config, times(1)).delete();
        verify(configAdmin, times(1)).createFactoryConfiguration("Csw_Federated_Source", null);
        verify(config).update(captor.capture());
        List<Dictionary> values = captor.getAllValues();
        assertThat(values.size(), equalTo(1));
        Dictionary passedValues = values.get(0);
        assertCswProperties(passedValues);
    }

    @Test
    public void testDefaultConfigurationUpdateMatchingConfig() throws Exception {

        ArgumentCaptor<Dictionary> captor = ArgumentCaptor.forClass(Dictionary.class);

        when(configAdmin.listConfigurations(anyString())).thenReturn(new Configuration[] {config});
        Hashtable<String, Object> props = new Hashtable<>();
        props.put("id", "TestRegNode");
        props.put(RegistryObjectMetacardType.REGISTRY_ID,
                "urn:uuid:2014ca7f59ac46f495e32b4a67a51276");
        when(config.getProperties()).thenReturn(props);
        when(config.getFactoryPid()).thenReturn("Csw_Federated_Source_disabled");

        setupSerialExecutor();
        sch.handleEvent(updateEvent);

        verify(config).update(captor.capture());
        Dictionary passedValues = captor.getValue();
        assertCswProperties(passedValues);
    }

    @Test
    public void testConfigurationUpdateMatchingConfigAndActiveAndPrioritySwitchWithMultipleBindings()
            throws Exception {

        mcard.setAttribute(RegistryObjectMetacardType.REGISTRY_IDENTITY_NODE, null);
        mcard.setMetadata(getMetadata("/csw-rim-node-multi-binding.xml"));
        when(configAdmin.listConfigurations(anyString())).thenReturn(new Configuration[] {config});
        Hashtable<String, Object> props = new Hashtable<>();
        props.put("id", "TestRegNode");
        props.put(RegistryObjectMetacardType.REGISTRY_ID,
                "urn:uuid:2014ca7f59ac46f495e32b4a67a51276");
        when(config.getProperties()).thenReturn(props);
        when(config.getFactoryPid()).thenReturn("Csw_Federated_Source2");

        List<String> priority = new ArrayList<>();
        priority.add("CSW_2.0.2");
        priority.add("CSW2_2.0.2");
        List<String> bindings = new ArrayList<>();
        bindings.add("CSW2_2.0.2=Csw_Federated_Source2");
        bindings.add("CSW_2.0.2=Csw_Federated_Source");
        sch.setBindingTypeFactoryPid(bindings);
        sch.setSourceActivationPriorityOrder(priority);

        sch.setPreserveActiveConfigurations(false);
        sch.setActivateConfigurations(true);
        setupSerialExecutor();
        sch.handleEvent(updateEvent);

        verify(config, times(1)).delete();
        verify(configAdmin, times(1)).createFactoryConfiguration("Csw_Federated_Source", null);
        verify(configAdmin, times(1)).createFactoryConfiguration("Csw_Federated_Source2_disabled",
                null);

    }

    @Test
    public void testDefaultConfigurationUpdateNoMatchingConfig() throws Exception {

        ArgumentCaptor<Dictionary> captor = ArgumentCaptor.forClass(Dictionary.class);

        when(configAdmin.listConfigurations(anyString())).thenReturn(new Configuration[] {config}, null);
        Hashtable<String, Object> props = new Hashtable<>();
        props.put("id", "TestRegNode");
        when(config.getProperties()).thenReturn(props);
        when(config.getFactoryPid()).thenReturn("Csw_Federated_Source");
        Configuration newConfig = mock(Configuration.class);
        when(configAdmin.createFactoryConfiguration("Csw_Federated_Source_disabled",
                null)).thenReturn(newConfig);
        setupSerialExecutor();
        sch.handleEvent(updateEvent);

        verify(configAdmin, times(1)).createFactoryConfiguration("Csw_Federated_Source_disabled",
                null);
        verify(newConfig).update(captor.capture());
        Dictionary passedValues = captor.getValue();
        assertThat(passedValues.get(RegistryObjectMetacardType.REGISTRY_ID),
                equalTo("urn:uuid:2014ca7f59ac46f495e32b4a67a51276"));
        assertThat(passedValues.get("attId"), equalTo("attValue"));
        assertThat(passedValues.get("urlBindingName"), equalTo("cswUrl"));
        assertThat(passedValues.get("id"),
                equalTo("TestRegNode - urn:uuid:2014ca7f59ac46f495e32b4a67a51276"));
        assertThat(passedValues.get("cswUrl"), equalTo("https://localhost:1234/mycsw/endpoint"));
        assertThat(passedValues.get("bindingType"), equalTo("CSW_2.0.2"));
        assertThat(passedValues.get("customSlot"), equalTo("customValue"));

    }

    @Test
    public void testDeleteConfiguration() throws Exception {
        when(configAdmin.listConfigurations(anyString())).thenReturn(new Configuration[] {config});
        sch.setCleanUpOnDelete(true);
        setupSerialExecutor();
        sch.handleEvent(deleteEvent);
        verify(config).delete();
    }

    @Test
    public void testDeleteConfigurationNoCleanup() throws Exception {
        when(configAdmin.listConfigurations(anyString())).thenReturn(new Configuration[] {config});
        setupSerialExecutor();
        sch.handleEvent(deleteEvent);
        verify(config, never()).delete();
    }

    @Test
    public void testMultipleAttributes() throws Exception {
        ArrayList<AttributeDefinition> attributeDefinitions = new ArrayList<>();
        attributeDefinitions.add(new AttributeDefinitionImpl("att1",
                "att1",
                "",
                AttributeDefinition.BOOLEAN,
                new String[] {"true"},
                0,
                new String[] {"att1Value"},
                null));
        attributeDefinitions.add(new AttributeDefinitionImpl("att2",
                "att2",
                "",
                AttributeDefinition.BYTE,
                new String[] {"0"},
                0,
                new String[] {"att2Value"},
                null));
        attributeDefinitions.add(new AttributeDefinitionImpl("att3",
                "att3",
                "",
                AttributeDefinition.DOUBLE,
                new String[] {"1.234"},
                0,
                new String[] {"att3Value"},
                null));
        attributeDefinitions.add(new AttributeDefinitionImpl("att4",
                "att4",
                "",
                AttributeDefinition.FLOAT,
                new String[] {"1.234"},
                0,
                new String[] {"att4Value"},
                null));
        attributeDefinitions.add(new AttributeDefinitionImpl("att5",
                "att5",
                "",
                AttributeDefinition.INTEGER,
                new String[] {"1"},
                0,
                new String[] {"att5Value"},
                null));
        attributeDefinitions.add(new AttributeDefinitionImpl("att6",
                "att6",
                "",
                AttributeDefinition.LONG,
                new String[] {"1234"},
                0,
                new String[] {"att6Value"},
                null));
        attributeDefinitions.add(new AttributeDefinitionImpl("att7",
                "att7",
                "",
                AttributeDefinition.SHORT,
                new String[] {"123"},
                0,
                new String[] {"att7Value"},
                null));
        attributeDefinitions.add(new AttributeDefinitionImpl("att8",
                "att8",
                "",
                AttributeDefinition.CHARACTER,
                null,
                0,
                new String[] {"a"},
                null));

        when(ocd.getAttributeDefinitions(-1)).thenReturn(attributeDefinitions.toArray(new AttributeDefinition[attributeDefinitions.size()]));
        mcard.setAttribute(RegistryObjectMetacardType.REGISTRY_IDENTITY_NODE, null);
        setupSerialExecutor();
        sch.handleEvent(createEvent);
        verify(configAdmin, times(1)).createFactoryConfiguration("Csw_Federated_Source_disabled",
                null);
    }

    @Test
    public void testActivationSetter() throws Exception {
        sch.setActivateConfigurations(false);
        verify(adminService, never()).getRegistryMetacards();
        sch.setActivateConfigurations(true);
        verify(adminService, never()).getRegistryMetacards();
        sch.setActivateConfigurations(false);
        sch.setPreserveActiveConfigurations(false);
        sch.setActivateConfigurations(true);
        verify(adminService, times(1)).getRegistryMetacards();
    }

    @Test
    public void testPreserveActivationSetter() throws Exception {
        sch.setPreserveActiveConfigurations(true);
        verify(adminService, never()).getRegistryMetacards();
        sch.setPreserveActiveConfigurations(false);
        verify(adminService, never()).getRegistryMetacards();
        sch.setPreserveActiveConfigurations(true);
        sch.setActivateConfigurations(true);
        sch.setPreserveActiveConfigurations(false);
        verify(adminService, times(1)).getRegistryMetacards();
    }

    @Test
    public void testSourceActivationPriorityOrderSetter() throws Exception {
        List<String> order1 = Arrays.asList(new String[] {"one", "two"});
        List<String> order2 = Arrays.asList(new String[] {"two", "one"});
        sch.setSourceActivationPriorityOrder(order1);
        verify(adminService, never()).getRegistryMetacards();
        sch.setActivateConfigurations(true);
        sch.setPreserveActiveConfigurations(false);
        sch.setSourceActivationPriorityOrder(order1);
        verify(adminService, times(1)).getRegistryMetacards();
        sch.setSourceActivationPriorityOrder(order2);
        verify(adminService, times(2)).getRegistryMetacards();
    }

    @Test
    public void testEmptyMetadata() throws Exception {
        mcard.setMetadata("");
        setupSerialExecutor();
        sch.handleEvent(createEvent);
        verify(configAdmin, never()).listConfigurations(anyString());
    }

    @Test
    public void testDestroy() throws Exception {
        when(executorService.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(true);
        sch.destroy();
        verify(executorService, times(1)).awaitTermination(anyLong(), any(TimeUnit.class));
        verify(executorService, times(0)).shutdownNow();
    }

    @Test
    public void testDestroyTerminateTasks() throws Exception {
        when(executorService.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(false);
        sch.destroy();
        verify(executorService, times(2)).awaitTermination(anyLong(), any(TimeUnit.class));
        verify(executorService, times(1)).shutdownNow();
    }

    @Test
    public void testDestroyInterupt() throws Exception {
        when(executorService.awaitTermination(anyLong(),
                any(TimeUnit.class))).thenThrow(new InterruptedException("interrupt"));
        sch.destroy();
        verify(executorService, times(1)).awaitTermination(anyLong(), any(TimeUnit.class));
        verify(executorService, times(1)).shutdownNow();
    }

    private String getMetadata(String path) throws Exception {
        InputStream inputStream = getClass().getResourceAsStream(path);
        BufferedReader buffer = new BufferedReader(new InputStreamReader(inputStream));
        return buffer.lines()
                .collect(Collectors.joining("\n"));
    }

    //taken from org.apache.felix.webconsole.internal.servlet.ConfigurationMetatypeSupport
    private static class AttributeDefinitionImpl implements AttributeDefinition {

        private final String id;

        private final String name;

        private final String description;

        private final int type;

        private final String[] defaultValues;

        private final int cardinality;

        private final String[] optionLabels;

        private final String[] optionValues;

        AttributeDefinitionImpl(final String id, final String name, final String description,
                final String defaultValue) {
            this(id, name, description, STRING, new String[] {defaultValue}, 0, null, null);
        }

        AttributeDefinitionImpl(final String id, final String name, final String description,
                final int type, final String[] defaultValues, final int cardinality,
                final String[] optionLabels, final String[] optionValues) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.type = type;
            this.defaultValues = defaultValues;
            this.cardinality = cardinality;
            this.optionLabels = optionLabels;
            this.optionValues = optionValues;
        }

        public int getCardinality() {
            return cardinality;
        }

        public String[] getDefaultValue() {
            return defaultValues;
        }

        public String getDescription() {
            return description;
        }

        public String getID() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String[] getOptionLabels() {
            return optionLabels;
        }

        public String[] getOptionValues() {
            return optionValues;
        }

        public int getType() {
            return type;
        }

        public String validate(String arg0) {
            return null;
        }
    }

    private void setupSerialExecutor() {
        doAnswer((args) -> {
            ((Runnable) args.getArguments()[0]).run();
            return null;
        }).when(executorService)
                .execute(any());
    }

    private void assertCswProperties(Dictionary passedValues) {
        assertThat(passedValues.get(RegistryObjectMetacardType.REGISTRY_ID),
                equalTo("urn:uuid:2014ca7f59ac46f495e32b4a67a51276"));
        assertThat(passedValues.get("urlBindingName"), equalTo("cswUrl"));
        assertThat(passedValues.get("id"), equalTo("TestRegNode"));
        assertThat(passedValues.get("cswUrl"), equalTo("https://localhost:1234/mycsw/endpoint"));
        assertThat(passedValues.get("bindingType"), equalTo("CSW_2.0.2"));
        assertThat(passedValues.get("customSlot"), equalTo("customValue"));
    }
}
