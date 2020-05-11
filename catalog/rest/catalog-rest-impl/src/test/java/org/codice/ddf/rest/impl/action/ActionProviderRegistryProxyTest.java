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
package org.codice.ddf.rest.impl.action;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.action.ActionProvider;
import ddf.catalog.Constants;
import ddf.catalog.transformer.attribute.AttributeMetacardTransformer;
import java.util.Dictionary;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

@RunWith(MockitoJUnitRunner.class)
public class ActionProviderRegistryProxyTest {

  private static final String SAMPLE_TRANSFORMER_ID = "sampleTransformerId";

  @Mock private static BundleContext mockBundleContext;

  @Mock private ServiceReference mockServiceReference;

  @Mock private ServiceRegistration mockServiceRegistration;

  @Captor private ArgumentCaptor captor;

  private MetacardTransformerActionProviderFactory mtapf =
      new MetacardTransformerActionProviderFactory();

  private static class ActionProviderTestRegistryProxy extends ActionProviderRegistryProxy {
    public ActionProviderTestRegistryProxy(MetacardTransformerActionProviderFactory actionFactory) {
      super(actionFactory);
    }

    @Override
    protected BundleContext getBundleContext() {
      return mockBundleContext;
    }
  }

  @Before
  public void setUp() {
    when(mockBundleContext.registerService(
            isA(String.class), isA(Object.class), isA(Dictionary.class)))
        .thenReturn(mockServiceRegistration);

    when(mockServiceReference.getProperty(Constants.SERVICE_ID)).thenReturn(SAMPLE_TRANSFORMER_ID);
  }

  @Test
  public void testNoTransformerId() {
    // given
    ActionProviderRegistryProxy proxy = new ActionProviderTestRegistryProxy(mtapf);

    when(mockServiceReference.getProperty(Constants.SERVICE_ID)).thenReturn("");

    // when
    proxy.bind(mockServiceReference);

    // then
    verify(mockBundleContext, times(0))
        .registerService(isA(String.class), isA(Object.class), isA(Dictionary.class));
  }

  @Test
  public void testValidTransformerId() {
    // given
    ActionProviderRegistryProxy proxy = new ActionProviderTestRegistryProxy(mtapf);

    // when
    proxy.bind(mockServiceReference);

    // then
    verify(mockBundleContext, times(1))
        .registerService(isA(String.class), isA(Object.class), isA(Dictionary.class));
  }

  @Test
  public void testValidTransformerShortName() {
    // given
    ActionProviderRegistryProxy proxy = new ActionProviderTestRegistryProxy(mtapf);

    when(mockServiceReference.getProperty(Constants.SERVICE_ID)).thenReturn("");
    when(mockServiceReference.getProperty(Constants.SERVICE_SHORTNAME))
        .thenReturn(SAMPLE_TRANSFORMER_ID);

    // when
    proxy.bind(mockServiceReference);

    // then
    verify(mockBundleContext, times(1))
        .registerService(isA(String.class), isA(Object.class), isA(Dictionary.class));
  }

  @Test
  public void testAttributeTransformer() throws MimeTypeParseException {
    // given
    ActionProviderRegistryProxy proxy = new ActionProviderTestRegistryProxy(mtapf);

    when(mockBundleContext.getService(mockServiceReference))
        .thenReturn(
            new AttributeMetacardTransformer("metadata", "metadata", new MimeType("text", "xml")));

    // when
    proxy.bind(mockServiceReference);

    // then
    verify(mockBundleContext, times(1))
        .registerService(anyString(), captor.capture(), any(Dictionary.class));

    Object value = captor.getValue();
    assertThat(value, notNullValue());
    assertThat(value instanceof ActionProvider, is(true));
  }

  @Test
  public void testRegisterUnRegister() {

    // given
    ActionProviderRegistryProxy proxy = new ActionProviderTestRegistryProxy(mtapf);

    // when
    proxy.bind(mockServiceReference);
    proxy.unbind(mockServiceReference);

    // then
    verify(mockBundleContext, times(1))
        .registerService(isA(String.class), isA(Object.class), isA(Dictionary.class));

    verify(mockServiceRegistration, times(1)).unregister();
  }
}
