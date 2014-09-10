/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package org.codice.ddf.spatial.ogc.wfs.catalog.mapper.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;


public class TestMetacardMapperImpl {

    private static final String FACTORY_PID = "org.codice.ddf.spatial.ogc.wfs.catalog.mapper.MetacardMapper";
    
    private static final String CONFIG_FILTER = "(" + ConfigurationAdmin.SERVICE_FACTORYPID + "=" + FACTORY_PID + ")";
    
    private static final String exampleFeatureType = "{http://www.example.com}featureType1";
    
    private static final String invalidExampleFeatureType = "featureType1";
    
    private static final String[] exampleMetacardAttrToFeaturePropMapping = new String[]{"metacardattr1=feature.prop1", "metacardattr2=feature.prop2", "metacardattr3=feature.prop3", "metacardattr4=feature.prop4"};
    
    private static final String[] exampleFeaturePropToMetacardAttrMapping = new String[]{"feature.prop1=metacardattr1", "feature.prop2=metacardattr2", "feature.prop3=metacardattr3", "feature.prop4=metacardattr4"};

    private static final String[] invalidExampleMetacardAttrToFeaturePropMapping = new String[]{"metacardattr1=feature.prop1", "metacardattr2->feature.prop2", "metacardattr3=feature.prop3", "metacardattr4=feature.prop4"};
    
    private static final String[] invalidExampleFeaturePropToMetacardAttrMapping = new String[]{"feature.prop1=metacardattr1", "feature.prop2->metacardattr2", "feature.prop3=metacardattr3", "feature.prop4=metacardattr4"};
    
    private static final String METACARD_ATTR_TO_FEATURE_PROP_MAPPING_REGEX = "([^=,]+)=([^=,]+)";
    
    private static final Pattern METACARD_ATTR_TO_FEATURE_PROP_MAPPING_PATTERN = Pattern.compile(METACARD_ATTR_TO_FEATURE_PROP_MAPPING_REGEX);
    
    private static final String EXPECTED_FEATURE_PROPERTY = "feature.prop3";
    
    /**
     * There is a bug in the felix admin console that prevents metatypes with lists (cardinality) from displaying and working correctly.
     * See https://issues.apache.org/jira/browse/KARAF-1701 for additional information.  When a configuration is persisted for the first
     * time (from a features file or a .cfg file), the comma separated list (metacardAttrToFeaturePropMap = metacardattr1=featureprop1,metacardattr2=featureprop2,metacardattr3=featureprop3)  
     * is displayed as one list item in the admin console (so metacardattr1=featureprop1,metacardattr2=featureprop2,metacardattr3=featureprop3 is on one line).
     * NOTE: When the setMetacardAttrToFeaturePropMap(String[]) is called, the array actually contains separate elements for each 'metacard attribute' 
     * to 'feature property mapping', but the elements are displayed on a single line in the admin console.  When mappings are added (using the +), 
     * the setMetacardAttrToFeaturePropMap(String[]) method receives an array with the initial mappings as a single element (like below) and all other mappings are individual 
     * elements.
     */
    private static final String[] exampleMetacardAttrToFeaturePropMappingWithAdminConsoleBug = new String[]{"metacardattr1=feature.prop1,metacardattr2=feature.prop2,metacardattr3=feature.prop3", "metacardattr4=feature.prop4"};
    
    private static final String[] exampleFeaturePropToMetacardAttrMappingWithAdminConsoleBug = new String[]{"feature.prop1=metacardattr1,feature.prop2=metacardattr2,feature.prop3=metacardattr3", "feature.prop4=metacardattr4"};
    
    @Test
    public void testGetFeaturePropertyForMetacardAttribute_MappingExists() {
        // setup
        MetacardMapperImpl metacardMapper = new MetacardMapperImpl();
        metacardMapper.setFeatureType(exampleFeatureType);
        metacardMapper.setMetacardAttrToFeaturePropMap(exampleMetacardAttrToFeaturePropMapping);
        
        // Test
        String metacardAttribute = "metacardattr3";
        String featureProperty = metacardMapper.getFeatureProperty(metacardAttribute);
        
        // Verify
        assertThat(featureProperty, is(EXPECTED_FEATURE_PROPERTY));
    }
    
    @Test
    public void testGetFeaturePropertyForMetacardAttribute_MappingDoesNotExist() {
        // setup
        MetacardMapperImpl metacardMapper = new MetacardMapperImpl();
        metacardMapper.setFeatureType(exampleFeatureType);
        metacardMapper.setMetacardAttrToFeaturePropMap(exampleMetacardAttrToFeaturePropMapping);
        
        // Test
        String metacardAttribure = "metacardattr200";
        String featureProperty = metacardMapper.getFeatureProperty(metacardAttribure);
        
        // Verify
        assertThat(featureProperty, is(nullValue()));
    }
    
    /**
     * There is a bug in the felix admin console that prevents metatypes with lists (cardinality) from displaying and working correctly.
     * See https://issues.apache.org/jira/browse/KARAF-1701 for additional information.  This test verifies that metacard attribute to
     * feature type mappings are mapped correctly even with the bug.
     */
    @Test
    public void testGetFeaturePropertyForMetacardAttribute_AdminConsoleBug_MappingExists() {
        // setup
        MetacardMapperImpl metacardMapper = new MetacardMapperImpl();
        metacardMapper.setFeatureType(exampleFeatureType);
        metacardMapper.setMetacardAttrToFeaturePropMap(exampleMetacardAttrToFeaturePropMappingWithAdminConsoleBug);
        
        // Test
        String metacardAttribute = "metacardattr3";
        String featureProperty = metacardMapper.getFeatureProperty(metacardAttribute);
        
        // Verify
        assertThat(featureProperty, is(EXPECTED_FEATURE_PROPERTY));
    }
    
    /**
     *  Verify that if invalid syntax is used for feature type when configuring a MetacardMapper, the configuration is deleted.
     * 
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testSetFeatureType_InvalidFeatureType() throws Exception {
        // setup
        MetacardMapperImpl metacardMapper = new MetacardMapperImpl();
        BundleContext mockBundleContext = mock(BundleContext.class);
        ServiceReference mockConfigurationAdminReference = mock(ServiceReference.class);
        when(mockBundleContext.getServiceReference(ConfigurationAdmin.class.getName())).thenReturn(mockConfigurationAdminReference);
        ConfigurationAdmin mockConfigurationAdmin = mock(ConfigurationAdmin.class);
        when(mockBundleContext.getService(mockConfigurationAdminReference)).thenReturn(mockConfigurationAdmin);
        Configuration[] mockConfigurations = new Configuration[1];
        mockConfigurations[0] = mock(Configuration.class);
        Dictionary<String, Object> mockDictionary = (Dictionary<String, Object>) mock(Dictionary.class);
        when(mockDictionary.get("featureType")).thenReturn(invalidExampleFeatureType);
        when(mockConfigurations[0].getProperties()).thenReturn(mockDictionary);
        when(mockConfigurationAdmin.listConfigurations(CONFIG_FILTER)).thenReturn(mockConfigurations);
        metacardMapper.setContext(mockBundleContext);
        
        // Test
        metacardMapper.setFeatureType(invalidExampleFeatureType);
        metacardMapper.setMetacardAttrToFeaturePropMap(exampleMetacardAttrToFeaturePropMapping);
        metacardMapper.init();
        
        // Verify
        verify(mockConfigurations[0], times(1)).delete();
    }
    
    @Test
    public void testSetMetacardAttrToFeaturePropMap_InvalidMetacardAttributeToFeatureTypeMapping() {
        // setup
        MetacardMapperImpl metacardMapper = new MetacardMapperImpl();
        metacardMapper.setFeatureType(exampleFeatureType);
        metacardMapper.setMetacardAttrToFeaturePropMap(invalidExampleMetacardAttrToFeaturePropMapping);
        
        // Test
        metacardMapper.setFeatureType(exampleFeatureType);
        // 4 mappings, but one is invalid
        metacardMapper.setMetacardAttrToFeaturePropMap(invalidExampleMetacardAttrToFeaturePropMapping);
        metacardMapper.init();
        
        // Verify
        // Should only contain 3 mappings since one was invalid
        assertThat(metacardMapper.getMetacardAttributeToFeaturePropertyMap().size(), is(3));
        
        Map<String, String> validMappings = removeInvalidMappings(invalidExampleMetacardAttrToFeaturePropMapping);
        
        for(String key : metacardMapper.getMetacardAttributeToFeaturePropertyMap().keySet()) {
            assertThat(metacardMapper.getMetacardAttributeToFeaturePropertyMap().get(key), is(validMappings.get(key)));
        }
    }
    
    /**
     * Verify that if invalid syntax is used for feature type when configuring a MetacardMapper, the configuration is deleted.
     * 
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testSetFeatureType_InvalidFeatureTypeAndInvalidMetacardAttributeToFeatureTypeMapping() throws Exception {
        // setup
        MetacardMapperImpl metacardMapper = new MetacardMapperImpl();
        BundleContext mockBundleContext = mock(BundleContext.class);
        ServiceReference mockConfigurationAdminReference = mock(ServiceReference.class);
        when(mockBundleContext.getServiceReference(ConfigurationAdmin.class.getName())).thenReturn(mockConfigurationAdminReference);
        ConfigurationAdmin mockConfigurationAdmin = mock(ConfigurationAdmin.class);
        when(mockBundleContext.getService(mockConfigurationAdminReference)).thenReturn(mockConfigurationAdmin);
        Configuration[] mockConfigurations = new Configuration[1];
        mockConfigurations[0] = mock(Configuration.class);
        Dictionary<String, Object> mockDictionary = (Dictionary<String, Object>) mock(Dictionary.class);
        when(mockDictionary.get("featureType")).thenReturn(invalidExampleFeatureType);
        when(mockConfigurations[0].getProperties()).thenReturn(mockDictionary);
        when(mockConfigurationAdmin.listConfigurations(CONFIG_FILTER)).thenReturn(mockConfigurations);
        metacardMapper.setContext(mockBundleContext);
        
        // Test
        metacardMapper.setFeatureType(invalidExampleFeatureType);
        metacardMapper.setMetacardAttrToFeaturePropMap(invalidExampleMetacardAttrToFeaturePropMapping);
        metacardMapper.init();
        
        // Verify
        verify(mockConfigurations[0], times(1)).delete();
    }
    
    @Test
    public void testGetMetacardAttributeForFeatureProperty_MappingExists() {
        // setup
        MetacardMapperImpl metacardMapper = new MetacardMapperImpl();
        metacardMapper.setFeatureType(exampleFeatureType);
        metacardMapper.setFeaturePropToMetacardAttrMap(exampleFeaturePropToMetacardAttrMapping);
        
        // Test
        String metacardAttrib = metacardMapper.getMetacardAttribute(EXPECTED_FEATURE_PROPERTY);
        
        // Verify
        String expectedMetacardAttrib = "metacardattr3";
        assertThat(metacardAttrib, is(expectedMetacardAttrib));
    }
    
    @Test
    public void testGetMetacardAttributeForFeatureProperty_MappingDoesNotExist() {
        // setup
        MetacardMapperImpl metacardMapper = new MetacardMapperImpl();
        metacardMapper.setFeatureType(exampleFeatureType);
        metacardMapper.setFeaturePropToMetacardAttrMap(exampleFeaturePropToMetacardAttrMapping);
        
        // Test
        String featureProperty = "feature.prop200";
        String metacardAttrib = metacardMapper.getMetacardAttribute(featureProperty);
        
        // Verify
        assertThat(metacardAttrib, is(nullValue()));
    }
    
    /**
     * There is a bug in the felix admin console that prevents metatypes with lists (cardinality) from displaying and working correctly.
     * See https://issues.apache.org/jira/browse/KARAF-1701 for additional information.  This test verifies that metacard attribute to
     * feature type mappings are mapped correctly even with the bug.
     */
    @Test
    public void testGetMetacardAttributeForFeatureProperty_AdminConsoleBug_MappingExists() {
        // setup
        MetacardMapperImpl metacardMapper = new MetacardMapperImpl();
        metacardMapper.setFeatureType(exampleFeatureType);
        metacardMapper.setFeaturePropToMetacardAttrMap(exampleFeaturePropToMetacardAttrMappingWithAdminConsoleBug);
        
        // Test
        String metacardAttrib = metacardMapper.getMetacardAttribute(EXPECTED_FEATURE_PROPERTY);
        
        // Verify
        String expectedMetacardAttrib = "metacardattr3";
        assertThat(metacardAttrib, is(expectedMetacardAttrib));
    }
    
    @Test
    public void testSetFeaturePropToMetacardAttrMap_InvalidMetacardAttributeToFeatureTypeMapping() {
        // setup
        MetacardMapperImpl metacardMapper = new MetacardMapperImpl();
        metacardMapper.setFeatureType(exampleFeatureType);
        metacardMapper.setFeaturePropToMetacardAttrMap(invalidExampleFeaturePropToMetacardAttrMapping);
        
        // Test
        metacardMapper.setFeatureType(exampleFeatureType);
        // 4 mappings, but one is invalid
        metacardMapper.setFeaturePropToMetacardAttrMap(invalidExampleFeaturePropToMetacardAttrMapping);
        metacardMapper.init();
        
        // Verify
        // Should only contain 3 mappings since one was invalid
        assertThat(metacardMapper.getFeaturePropertyToMetacardAttributeMap().size(), is(3));
        
        Map<String, String> validMappings = removeInvalidMappings(invalidExampleFeaturePropToMetacardAttrMapping);
        
        for(String key : metacardMapper.getFeaturePropertyToMetacardAttributeMap().keySet()) {
            assertThat(metacardMapper.getFeaturePropertyToMetacardAttributeMap().get(key), is(validMappings.get(key)));
        }
    }
    
    /**
     * Verify that if invalid syntax is used for feature type when configuring a MetacardMapper, the configuration is deleted.
     * 
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    public void testSetFeatureType_InvalidFeatureTypeAndInvalidFeatureTypeToMetacardAttributeMapping() throws Exception {
        // setup
        MetacardMapperImpl metacardMapper = new MetacardMapperImpl();
        BundleContext mockBundleContext = mock(BundleContext.class);
        ServiceReference mockConfigurationAdminReference = mock(ServiceReference.class);
        when(mockBundleContext.getServiceReference(ConfigurationAdmin.class.getName())).thenReturn(mockConfigurationAdminReference);
        ConfigurationAdmin mockConfigurationAdmin = mock(ConfigurationAdmin.class);
        when(mockBundleContext.getService(mockConfigurationAdminReference)).thenReturn(mockConfigurationAdmin);
        Configuration[] mockConfigurations = new Configuration[1];
        mockConfigurations[0] = mock(Configuration.class);
        Dictionary<String, Object> mockDictionary = (Dictionary<String, Object>) mock(Dictionary.class);
        when(mockDictionary.get("featureType")).thenReturn(invalidExampleFeatureType);
        when(mockConfigurations[0].getProperties()).thenReturn(mockDictionary);
        when(mockConfigurationAdmin.listConfigurations(CONFIG_FILTER)).thenReturn(mockConfigurations);
        metacardMapper.setContext(mockBundleContext);
        
        // Test
        metacardMapper.setFeatureType(invalidExampleFeatureType);
        metacardMapper.setFeaturePropToMetacardAttrMap(invalidExampleFeaturePropToMetacardAttrMapping);
        metacardMapper.init();
        
        // Verify
        verify(mockConfigurations[0], times(1)).delete();
    }
    
    @Test
    public void testGetFeaturePropertyForMetacardAttributeAndGetMetacardAttributeForFeatureProperty_MappingExists() {
        // setup
        MetacardMapperImpl metacardMapper = new MetacardMapperImpl();
        metacardMapper.setFeatureType(exampleFeatureType);
        metacardMapper.setMetacardAttrToFeaturePropMap(exampleMetacardAttrToFeaturePropMapping);
        metacardMapper.setFeaturePropToMetacardAttrMap(exampleFeaturePropToMetacardAttrMapping);
        
        // Test
        String metacardAttribute1 = "metacardattr3";
        String featureProperty1 = metacardMapper.getFeatureProperty(metacardAttribute1);
        String metacardAttrib = metacardMapper.getMetacardAttribute(EXPECTED_FEATURE_PROPERTY);
        
        // Verify
        assertThat(featureProperty1, is(EXPECTED_FEATURE_PROPERTY));
        
        String expectedMetacardAttrib = "metacardattr3";
        assertThat(metacardAttrib, is(expectedMetacardAttrib));
    }
    
    private Map<String, String> removeInvalidMappings(String[] mappings) {
        Map<String, String> metacardAttributeToFeaturePropertyMap = new HashMap<String, String>();
        for(String mapping : mappings) {
            if(isValidMapping(mapping)) {
                metacardAttributeToFeaturePropertyMap.put(getMetacardAttribute(mapping), getFeatureProperty(mapping));
            }
        }
        
        return metacardAttributeToFeaturePropertyMap;
    }
    
    private boolean isValidMapping(String mapping) {
        return METACARD_ATTR_TO_FEATURE_PROP_MAPPING_PATTERN.matcher(mapping).matches();
    }
    
    private String getMetacardAttribute(String mapping) {
        String metacardAttribute = StringUtils.trim(StringUtils.split(mapping, "=")[0]);
        return metacardAttribute;
    }
    
    private String getFeatureProperty(String mapping) {
        String featureProperty = StringUtils.trim(StringUtils.split(mapping, "=")[1]);
        return featureProperty;
    }
}
