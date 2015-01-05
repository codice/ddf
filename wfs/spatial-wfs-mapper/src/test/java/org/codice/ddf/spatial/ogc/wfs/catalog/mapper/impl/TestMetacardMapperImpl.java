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

import org.junit.Test;

import ddf.catalog.data.Metacard;



public class TestMetacardMapperImpl {

    private static final String exampleFeatureType = "{http://www.example.com}featureType1";
    
    private static final String FEATURE_PROPERTY = "feature.prop1";
    
    private static final String NON_EXISTENT_FEATURE_PROPERTY = "feature.prop.nonexistent";
      
    @Test
    public void testGetFeaturePropertyMappingGivenMetacardAttribute_MappingExists() {
        // setup
        MetacardMapperImpl metacardMapper = new MetacardMapperImpl();
        metacardMapper.setFeatureType(exampleFeatureType);
        metacardMapper.setCreatedDateMapping(FEATURE_PROPERTY);
        
        // Test
        String featureProperty = metacardMapper.getFeatureProperty(Metacard.CREATED);
        
        // Verify
        assertThat(featureProperty, is(FEATURE_PROPERTY));
    }
    
    @Test
    public void testGetFeaturePropertyMappingGivenMetacardAttribute_MappingDoesNotExists() {
        // setup
        MetacardMapperImpl metacardMapper = new MetacardMapperImpl();
        metacardMapper.setFeatureType(exampleFeatureType);
        metacardMapper.setCreatedDateMapping(FEATURE_PROPERTY);
        
        // Test
        String featureProperty = metacardMapper.getFeatureProperty(Metacard.MODIFIED);
        
        // Verify
        assertThat(featureProperty, is(nullValue()));
    }
    
    @Test
    public void testGetMetacardAttributeMappingGivenFeatureProperty_MappingExists() {
        // setup
        MetacardMapperImpl metacardMapper = new MetacardMapperImpl();
        metacardMapper.setFeatureType(exampleFeatureType);
        metacardMapper.setResourceSizeMapping(FEATURE_PROPERTY);
        metacardMapper.setDataUnit("MB");
        
        // Test
        String metacardAttribute = metacardMapper.getMetacardAttribute(FEATURE_PROPERTY);
        String unit = metacardMapper.getDataUnit();
        
        // Verify
        assertThat(metacardAttribute, is(Metacard.RESOURCE_SIZE));
        assertThat(unit, is("MB"));
    }
    
    @Test
    public void testGetMetacardAttributeMappingGivenFeatureProperty_MappingDoesNotExists() {
        // setup
        MetacardMapperImpl metacardMapper = new MetacardMapperImpl();
        metacardMapper.setFeatureType(exampleFeatureType);
        metacardMapper.setExpirationDateMapping(FEATURE_PROPERTY);
        
        // Test
        String metacardAttribute = metacardMapper.getMetacardAttribute(NON_EXISTENT_FEATURE_PROPERTY);

        
        // Verify
        assertThat(metacardAttribute, is(nullValue()));
    }
}
