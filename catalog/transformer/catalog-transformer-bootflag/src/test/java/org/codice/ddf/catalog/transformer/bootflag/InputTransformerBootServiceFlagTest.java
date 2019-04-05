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
package org.codice.ddf.catalog.transformer.bootflag;

import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
import ddf.catalog.transform.InputTransformer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import net.jodah.failsafe.FailsafeException;
import org.codice.junit.rules.RestoreSystemProperties;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class InputTransformerBootServiceFlagTest {

  private Bundle bundle;

  private BundleContext bundleContext;

  @Rule
  public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

  @Before
  public void setup() {
    bundleContext = mock(BundleContext.class);

    bundle = mock(Bundle.class);
    when(bundle.getBundleContext()).thenReturn(bundleContext);
  }

  @Test
  public void testWaitForInputTransformers() {
    InputTransformerIds inputTransformerIds = mock(InputTransformerIds.class);
    when(inputTransformerIds.getIds()).thenReturn(ImmutableSet.of("id1", "id2", "id3"));

    List<ServiceReference<InputTransformer>> inputTransformerReferences =
        mockServiceReferences("id1", "id2", "id3");

    new InputTransformerBootServiceFlag(
        inputTransformerIds, inputTransformerReferences, bundle, 1, 5);

    verify(bundleContext, times(1))
        .registerService(isA(Class.class), isA(Object.class), isA(Dictionary.class));
  }

  @Test
  public void testWaitForNoInputTransformers() {
    InputTransformerIds inputTransformerIds = mock(InputTransformerIds.class);
    when(inputTransformerIds.getIds()).thenReturn(Collections.emptySet());

    new InputTransformerBootServiceFlag(inputTransformerIds, Collections.emptyList(), bundle, 1, 5);

    verify(bundleContext, times(1))
        .registerService(isA(Class.class), isA(Object.class), isA(Dictionary.class));
  }

  @Test(expected = FailsafeException.class)
  public void testWaitForInputTransformersTimesOut() {
    InputTransformerIds inputTransformerIds = mock(InputTransformerIds.class);
    when(inputTransformerIds.getIds()).thenReturn(ImmutableSet.of("id1", "id2", "id3"));

    List<ServiceReference<InputTransformer>> inputTransformerReferences =
        mockServiceReferences("id1", "id2");

    new InputTransformerBootServiceFlag(
        inputTransformerIds, inputTransformerReferences, bundle, 1, 5);

    verify(bundleContext, times(0))
        .registerService(isA(Class.class), isA(Object.class), isA(Dictionary.class));
  }

  private List<ServiceReference<InputTransformer>> mockServiceReferences(String... propertyValues) {
    List<ServiceReference<InputTransformer>> serviceReferences = new ArrayList<>();
    for (String propertyValue : propertyValues) {
      ServiceReference<InputTransformer> mockReference = mock(ServiceReference.class);
      when(mockReference.getProperty("id")).thenReturn(propertyValue);
      serviceReferences.add(mockReference);
    }
    return serviceReferences;
  }
}
