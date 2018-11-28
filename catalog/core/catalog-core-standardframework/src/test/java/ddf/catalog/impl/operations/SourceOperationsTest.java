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
package ddf.catalog.impl.operations;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.action.Action;
import ddf.action.ActionRegistry;
import ddf.catalog.impl.FrameworkProperties;
import ddf.catalog.operation.SourceInfoRequest;
import ddf.catalog.operation.SourceInfoResponse;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.SourceCapabilityRegistry;
import ddf.catalog.source.SourceDescriptor;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.util.impl.CachedSource;
import ddf.catalog.util.impl.SourcePoller;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class SourceOperationsTest {

  private FrameworkProperties frameworkProperties;

  private CachedSource cachedSource;

  private SourceInfoRequest sourceInfoRequest;

  @Before
  public void setup() {
    cachedSource = mock(CachedSource.class);
    when(cachedSource.getId()).thenReturn("source-id");

    frameworkProperties = mock(FrameworkProperties.class);

    CatalogProvider catalogProvider = mock(CatalogProvider.class);

    when(frameworkProperties.getCatalogProviders())
        .thenReturn(Collections.singletonList(catalogProvider));

    SourcePoller sourcePoller = mock(SourcePoller.class);
    when(sourcePoller.getCachedSource(any())).thenReturn(cachedSource);

    when(frameworkProperties.getSourcePoller()).thenReturn(sourcePoller);

    sourceInfoRequest = mock(SourceInfoRequest.class);
  }

  @Test
  public void testGettingSourceActions() throws SourceUnavailableException {

    Action action = mock(Action.class);

    ActionRegistry actionRegistry = mock(ActionRegistry.class);
    when(actionRegistry.list(cachedSource)).thenReturn(Collections.singletonList(action));

    SourceCapabilityRegistry sourceCapabilityRegistry = mock(SourceCapabilityRegistry.class);
    when(sourceCapabilityRegistry.list(any())).thenReturn(Collections.emptyList());

    SourceOperations sourceOperations =
        new SourceOperations(frameworkProperties, actionRegistry, sourceCapabilityRegistry);
    sourceOperations.bind((CatalogProvider) null);
    SourceInfoResponse sourceInfoResponse = sourceOperations.getSourceInfo(sourceInfoRequest, true);

    assertThat(sourceInfoResponse.getSourceInfo(), hasSize(1));
    SourceDescriptor sourceDescriptor =
        sourceInfoResponse.getSourceInfo().toArray(new SourceDescriptor[0])[0];
    assertThat(sourceDescriptor.getActions(), is(Collections.singletonList(action)));
  }

  @Test
  public void testGettingSourceCapabilities() throws SourceUnavailableException {
    List<String> capabilities = Collections.singletonList("capability");

    ActionRegistry actionRegistry = mock(ActionRegistry.class);
    when(actionRegistry.list(any())).thenReturn(Collections.emptyList());

    SourceCapabilityRegistry sourceCapabilityRegistry = mock(SourceCapabilityRegistry.class);
    when(sourceCapabilityRegistry.list(cachedSource)).thenReturn(capabilities);

    SourceOperations sourceOperations =
        new SourceOperations(frameworkProperties, actionRegistry, sourceCapabilityRegistry);
    sourceOperations.bind((CatalogProvider) null);
    SourceInfoResponse sourceInfoResponse = sourceOperations.getSourceInfo(sourceInfoRequest, true);

    assertThat(sourceInfoResponse.getSourceInfo(), hasSize(1));
    SourceDescriptor sourceDescriptor =
        sourceInfoResponse.getSourceInfo().toArray(new SourceDescriptor[0])[0];
    assertThat(sourceDescriptor.getCapabilities(), is(capabilities));
  }
}
