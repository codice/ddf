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
package org.codice.ddf.spatial.ogc.wfs.catalog.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;

import org.codice.ddf.spatial.ogc.wfs.catalog.message.api.WfsMetadata;
import org.junit.Before;
import org.junit.Test;

public class WfsMetadataImplTest {

  public static final String TEST_ID = "TEST_ID";

  public static final String COORDINATE_ORDER = "LAT/LON";

  private static final String FEATURE_MEMBER_ELEMENT = "featureMember";

  private WfsMetadata<FeatureMetacardType> testWfsMetadata;

  @Before
  public void setup() {
    WfsMetadataImpl<FeatureMetacardType> testWfsMetadataImpl =
        new WfsMetadataImpl<FeatureMetacardType>(
            () -> TEST_ID,
            () -> COORDINATE_ORDER,
            FEATURE_MEMBER_ELEMENT,
            FeatureMetacardType.class);
    FeatureMetacardType mockFeatureMetacardType = mock(FeatureMetacardType.class);
    testWfsMetadataImpl.addEntry(mockFeatureMetacardType);
    this.testWfsMetadata = testWfsMetadataImpl;
  }

  @Test
  public void testWfsMetadataImpl() {
    assertThat(testWfsMetadata.getCoordinateOrder(), is(COORDINATE_ORDER));
    assertThat(testWfsMetadata.getId(), is(TEST_ID));
    assertThat(testWfsMetadata.getDescriptors().size(), is(1));
  }
}
