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
package org.codice.ddf.spatial.ogc.wfs.catalog.metacardtype.registry.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.base.VerifyException;
import ddf.catalog.data.MetacardType;
import java.util.Dictionary;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class WfsMetacardTypeRegistryTest {

  public static final String TEST_SOURCE_ID = "test-source";

  public static final String TEST_FEATURE_SIMPLE_NAME = "amazon-waters";

  private BundleContext mockBundleContext;

  private WfsMetacardTypeRegistryImpl wfsMetacardTypeRegistry;

  private ServiceRegistration mockServiceRegistration;

  private MetacardType mockMetacardType;

  @Before
  public void setup() {
    mockBundleContext = mock(BundleContext.class);
    mockServiceRegistration = mock(ServiceRegistration.class);
    ServiceReference mockServiceReference = mock(ServiceReference.class);
    mockMetacardType = mock(MetacardType.class);

    when(mockServiceRegistration.getReference()).thenReturn(mockServiceReference);
    when(mockServiceReference.getProperty(WfsMetacardTypeRegistryImpl.SOURCE_ID))
        .thenReturn(TEST_SOURCE_ID);
    when(mockServiceReference.getProperty(WfsMetacardTypeRegistryImpl.FEATURE_NAME))
        .thenReturn(TEST_FEATURE_SIMPLE_NAME);
    when(mockBundleContext.registerService(
            same(MetacardType.class), any(MetacardType.class), any(Dictionary.class)))
        .thenReturn(mockServiceRegistration);
    when(mockBundleContext.getService(any(ServiceReference.class))).thenReturn(mockMetacardType);
    wfsMetacardTypeRegistry = new WfsMetacardTypeRegistryImpl(mockBundleContext);
  }

  @Test
  public void testRegisterMetacardType() {
    wfsMetacardTypeRegistry.registerMetacardType(
        mockMetacardType, TEST_SOURCE_ID, TEST_FEATURE_SIMPLE_NAME);

    verify(mockBundleContext, times(1))
        .registerService(same(MetacardType.class), same(mockMetacardType), any(Dictionary.class));
  }

  @Test
  public void testLookupMetacardTypeBySimpleName() {
    wfsMetacardTypeRegistry.registerMetacardType(
        mockMetacardType, TEST_SOURCE_ID, TEST_FEATURE_SIMPLE_NAME);
    Optional<MetacardType> metarcardTypeOptional =
        wfsMetacardTypeRegistry.lookupMetacardTypeBySimpleName(
            TEST_SOURCE_ID, TEST_FEATURE_SIMPLE_NAME);
    assertThat(metarcardTypeOptional.isPresent(), is(true));

    metarcardTypeOptional =
        wfsMetacardTypeRegistry.lookupMetacardTypeBySimpleName(
            "different-source", TEST_FEATURE_SIMPLE_NAME);
    assertThat(metarcardTypeOptional.isPresent(), is(false));

    metarcardTypeOptional =
        wfsMetacardTypeRegistry.lookupMetacardTypeBySimpleName(TEST_SOURCE_ID, "different-name");
    assertThat(metarcardTypeOptional.isPresent(), is(false));
  }

  @Test
  public void testClear() {
    wfsMetacardTypeRegistry.registerMetacardType(
        mockMetacardType, TEST_SOURCE_ID, TEST_FEATURE_SIMPLE_NAME);
    wfsMetacardTypeRegistry.clear();
    verify(mockServiceRegistration, times(1)).unregister();
  }

  @Test(expected = VerifyException.class)
  public void testNullMetacardType() {
    wfsMetacardTypeRegistry.registerMetacardType(null, TEST_SOURCE_ID, TEST_FEATURE_SIMPLE_NAME);
  }

  @Test(expected = VerifyException.class)
  public void testNullSourceId() {
    wfsMetacardTypeRegistry.registerMetacardType(mockMetacardType, null, TEST_FEATURE_SIMPLE_NAME);
  }

  @Test(expected = VerifyException.class)
  public void testNullFeatureSimpleName() {
    wfsMetacardTypeRegistry.registerMetacardType(mockMetacardType, TEST_SOURCE_ID, null);
  }

  @Test
  public void testLookupNullValues() {
    Optional<MetacardType> metacardTypeOptional =
        wfsMetacardTypeRegistry.lookupMetacardTypeBySimpleName(null, TEST_FEATURE_SIMPLE_NAME);
    assertThat(metacardTypeOptional.isPresent(), is(false));

    metacardTypeOptional =
        wfsMetacardTypeRegistry.lookupMetacardTypeBySimpleName(TEST_SOURCE_ID, null);
    assertThat(metacardTypeOptional.isPresent(), is(false));
  }
}
