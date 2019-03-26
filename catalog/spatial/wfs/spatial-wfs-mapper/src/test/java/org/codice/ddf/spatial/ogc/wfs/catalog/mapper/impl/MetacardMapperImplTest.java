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
package org.codice.ddf.spatial.ogc.wfs.catalog.mapper.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;

import ddf.catalog.data.types.Core;
import org.junit.Test;

public class MetacardMapperImplTest {

  private static final String EXAMPLE_FEATURE_TYPE = "{http://www.example.com}featureType1";

  private static final String FEATURE_PROPERTY = "feature.prop1";

  private static final String NON_EXISTENT_FEATURE_PROPERTY = "feature.prop.nonexistent";

  @Test
  public void testGetFeaturePropertyMappingGivenMetacardAttributeMappingExists() {
    // setup
    MetacardMapperImpl metacardMapper = new MetacardMapperImpl();
    metacardMapper.setFeatureType(EXAMPLE_FEATURE_TYPE);
    metacardMapper.setCreatedDateMapping(FEATURE_PROPERTY);

    // Test
    String featureProperty = metacardMapper.getFeatureProperty(Core.CREATED);

    // Verify
    assertThat(featureProperty, is(FEATURE_PROPERTY));
  }

  @Test
  public void testGetFeaturePropertyMappingGivenMetacardAttributeMappingDoesNotExists() {
    // setup
    MetacardMapperImpl metacardMapper = new MetacardMapperImpl();
    metacardMapper.setFeatureType(EXAMPLE_FEATURE_TYPE);
    metacardMapper.setCreatedDateMapping(FEATURE_PROPERTY);

    // Test
    String featureProperty = metacardMapper.getFeatureProperty(Core.MODIFIED);

    // Verify
    assertThat(featureProperty, is(nullValue()));
  }

  @Test
  public void testGetMetacardAttributeMappingGivenFeaturePropertyMappingExists() {
    // setup
    MetacardMapperImpl metacardMapper = new MetacardMapperImpl();
    metacardMapper.setFeatureType(EXAMPLE_FEATURE_TYPE);
    metacardMapper.setResourceSizeMapping(FEATURE_PROPERTY);
    metacardMapper.setDataUnit("MB");

    // Test
    String metacardAttribute = metacardMapper.getMetacardAttribute(FEATURE_PROPERTY);
    String unit = metacardMapper.getDataUnit();

    // Verify
    assertThat(metacardAttribute, is(Core.RESOURCE_SIZE));
    assertThat(unit, is("MB"));
  }

  @Test
  public void testGetMetacardAttributeMappingGivenFeaturePropertyMappingDoesNotExists() {
    // setup
    MetacardMapperImpl metacardMapper = new MetacardMapperImpl();
    metacardMapper.setFeatureType(EXAMPLE_FEATURE_TYPE);
    metacardMapper.setExpirationDateMapping(FEATURE_PROPERTY);

    // Test
    String metacardAttribute = metacardMapper.getMetacardAttribute(NON_EXISTENT_FEATURE_PROPERTY);

    // Verify
    assertThat(metacardAttribute, is(nullValue()));
  }
}
