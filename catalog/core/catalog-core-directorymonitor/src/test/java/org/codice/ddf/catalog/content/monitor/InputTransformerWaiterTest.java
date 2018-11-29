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
package org.codice.ddf.catalog.content.monitor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
import ddf.catalog.transform.InputTransformer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.jodah.failsafe.FailsafeException;
import org.codice.junit.rules.RestoreSystemProperties;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

public class InputTransformerWaiterTest {

  private static final String TRANSFORMER_WAIT_TIMEOUT_PROPERTY =
      "org.codice.ddf.cdm.transformerWaitTimeoutSeconds";

  @Rule
  public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

  @Test
  public void testWaitForInputTransformers() {
    InputTransformerIds inputTransformerIds = mock(InputTransformerIds.class);
    when(inputTransformerIds.getIds()).thenReturn(ImmutableSet.of("id1", "id2", "id3"));

    List<ServiceReference<InputTransformer>> inputTransformerReferences =
        mockServiceReferences("id1", "id2", "id3");

    new InputTransformerWaiter(inputTransformerIds, inputTransformerReferences, 1, 5);
  }

  @Test
  public void testWaitForNoInputTransformers() {
    InputTransformerIds inputTransformerIds = mock(InputTransformerIds.class);
    when(inputTransformerIds.getIds()).thenReturn(Collections.emptySet());

    new InputTransformerWaiter(inputTransformerIds, Collections.emptyList(), 1, 5);
  }

  @Test(expected = FailsafeException.class)
  public void testWaitForInputTransformersTimesOut() {
    InputTransformerIds inputTransformerIds = mock(InputTransformerIds.class);
    when(inputTransformerIds.getIds()).thenReturn(ImmutableSet.of("id1", "id2", "id3"));

    List<ServiceReference<InputTransformer>> inputTransformerReferences =
        mockServiceReferences("id1", "id2");

    new InputTransformerWaiter(inputTransformerIds, inputTransformerReferences, 1, 5);
  }

  @Test
  public void testTimeoutSystemPropertyIsRespected() {
    System.setProperty(TRANSFORMER_WAIT_TIMEOUT_PROPERTY, "1");
    InputTransformerWaiter waiter =
        new InputTransformerWaiter(mock(InputTransformerIds.class), Collections.emptyList());
    assertThat(waiter.getTransformerWaitTimeoutMillis(), is(1000L));
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
