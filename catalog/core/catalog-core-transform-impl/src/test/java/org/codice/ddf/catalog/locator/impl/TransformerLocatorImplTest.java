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
package org.codice.ddf.catalog.locator.impl;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import ddf.catalog.Constants;
import ddf.catalog.transform.InputTransformer;
import ddf.catalog.transform.MetacardTransformer;
import ddf.catalog.transform.QueryResponseTransformer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import org.codice.ddf.catalog.transform.MultiInputTransformer;
import org.codice.ddf.catalog.transform.MultiMetacardTransformer;
import org.codice.ddf.catalog.transform.TransformerProperties;
import org.codice.ddf.catalog.transform.impl.InputTransformerAdapter;
import org.codice.ddf.catalog.transform.impl.MetacardTransformerAdapter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

@SuppressWarnings("unchecked")
public class TransformerLocatorImplTest {

  private BundleContext bundleContext;

  private TransformerLocatorImpl transformerLocator;

  /** Map of the mock service references registered for each class. */
  private Map<Class, List<ServiceReference>> serviceMap;

  @Before
  public void setup() throws InvalidSyntaxException {
    bundleContext = mock(BundleContext.class);

    transformerLocator = new TransformerLocatorTest(bundleContext);

    serviceMap = new HashMap<>();

    when(bundleContext.getServiceReferences(isA(Class.class), anyString()))
        .thenAnswer(
            (Answer<Collection<ServiceReference>>)
                invocationOnMock -> {
                  Class<?> clazz = (Class<?>) invocationOnMock.getArguments()[0];
                  String filter = (String) invocationOnMock.getArguments()[1];

                  if (filter != null) {
                    Filter f = FrameworkUtil.createFilter(filter);
                    return serviceMap
                        .get(clazz)
                        .stream()
                        .filter(f::match)
                        .collect(Collectors.toList());
                  }
                  if (serviceMap.containsKey(clazz)) {
                    return serviceMap.get(clazz);
                  }

                  return null;
                });
  }

  private MultiInputTransformer mockMultiInputTransformer(MimeType... mimeTypes) {
    return mockMultiTransformer(MultiInputTransformer.class, mimeTypes);
  }

  private <T extends TransformerProperties> T mockMultiTransformer(
      Class<T> clazz, MimeType... mimeTypes) {
    T transformer = mock(clazz);
    ServiceReference<T> serviceReference = mock(ServiceReference.class);

    when(bundleContext.getService(serviceReference)).thenReturn(transformer);
    when(transformer.getMimeTypes()).thenReturn(new HashSet<>(Arrays.asList(mimeTypes)));
    serviceMap.computeIfAbsent(clazz, aClass -> new ArrayList()).add(serviceReference);

    return transformer;
  }

  private MultiInputTransformer mockMultiInputTransformer(String id) {
    return mockMultiTransformer(MultiInputTransformer.class, id);
  }

  private <T extends TransformerProperties> T mockMultiTransformer(Class<T> clazz, String id) {
    T transformer = mock(clazz);
    ServiceReference<T> serviceReference = mock(ServiceReference.class);
    when(bundleContext.getService(serviceReference)).thenReturn(transformer);
    when(transformer.getId()).thenReturn(id);
    when(serviceReference.getProperty(eq(Constants.SERVICE_ID))).thenReturn(id);

    serviceMap.computeIfAbsent(clazz, aClass -> new ArrayList()).add(serviceReference);

    return transformer;
  }

  private MultiMetacardTransformer mockMultiMetacardTransformer(MimeType... mimeTypes) {
    return mockMultiTransformer(MultiMetacardTransformer.class, mimeTypes);
  }

  private MultiMetacardTransformer mockMultiMetacardTransformer(String id) {
    return mockMultiTransformer(MultiMetacardTransformer.class, id);
  }

  private InputTransformer mockInputTransformer(Map<String, Object> serviceProperties) {
    return mockTransformer(serviceProperties, InputTransformer.class);
  }

  private MetacardTransformer mockMetacardTransformer(Map<String, Object> serviceProperties) {
    return mockTransformer(serviceProperties, MetacardTransformer.class);
  }

  private QueryResponseTransformer mockQueryResponseTransformer(
      Map<String, Object> serviceProperties) {
    return mockTransformer(serviceProperties, QueryResponseTransformer.class);
  }

  private <T> T mockTransformer(Map<String, Object> serviceProperties, Class<T> clazz) {

    T metacardTransformer = mock(clazz);
    ServiceReference<T> serviceReference = mock(ServiceReference.class);
    when(serviceReference.getPropertyKeys())
        .thenReturn(serviceProperties.keySet().toArray(new String[0]));
    when(bundleContext.getService(serviceReference)).thenReturn(metacardTransformer);
    when(serviceReference.getProperty(eq(TransformerProperties.MIME_TYPE)))
        .thenReturn(
            serviceProperties
                .entrySet()
                .stream()
                .filter(
                    stringObjectEntry ->
                        stringObjectEntry.getKey().equals(TransformerProperties.MIME_TYPE))
                .map(Map.Entry::getValue)
                .filter(List.class::isInstance)
                .map(List.class::cast)
                .flatMap(Collection::stream)
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .collect(Collectors.toList()));
    when(serviceReference.getProperty(eq(Constants.SERVICE_ID)))
        .thenReturn(
            serviceProperties
                .entrySet()
                .stream()
                .filter(
                    stringObjectEntry -> stringObjectEntry.getKey().equals(Constants.SERVICE_ID))
                .map(Map.Entry::getValue)
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .findFirst()
                .orElse(null));

    serviceMap.computeIfAbsent(clazz, aClass -> new ArrayList()).add(serviceReference);

    return metacardTransformer;
  }

  @Test
  public void testFindMultiInputTransformers() {

    MultiInputTransformer multi = mockMultiInputTransformer();

    InputTransformer single = mockInputTransformer(Collections.emptyMap());

    List<MultiInputTransformer> multiInputTransformers =
        transformerLocator.findMultiInputTransformers();

    assertThat(multiInputTransformers, hasSize(2));
    assertThat(multiInputTransformers.get(0), is(multi));
    assertThat(
        ((InputTransformerAdapter) multiInputTransformers.get(1)).getInputTransformer(),
        is(single));
  }

  @Test
  public void testFindMultiInputTransformersByMimeType() throws MimeTypeParseException {

    MultiInputTransformer multi =
        mockMultiInputTransformer(new MimeType("text/plain"), new MimeType("text/xml"));

    InputTransformer single =
        mockInputTransformer(
            new ImmutableMap.Builder<String, Object>()
                .put(TransformerProperties.MIME_TYPE, Arrays.asList("text/plain", "text/xml"))
                .build());

    List<MultiInputTransformer> multiInputTransformers =
        transformerLocator.findMultiInputTransformers(new MimeType("text/plain"));

    assertThat(multiInputTransformers, hasSize(2));
    assertThat(multiInputTransformers.get(0), is(multi));
    assertThat(
        ((InputTransformerAdapter) multiInputTransformers.get(1)).getInputTransformer(),
        is(single));
  }

  @Test
  public void testFindMultiInputTransformersByIdSearchForMulti() {
    MultiInputTransformer multi = mockMultiInputTransformer("id1");

    mockInputTransformer(
        new ImmutableMap.Builder<String, Object>().put(Constants.SERVICE_ID, "id2").build());

    List<MultiInputTransformer> multiInputTransformers =
        transformerLocator.findMultiInputTransformers("id1");

    assertThat(multiInputTransformers, hasSize(1));
    assertThat(multiInputTransformers.get(0), is(multi));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFindMultiInputTransformersByIdSearchForMultiWithInvalidId() {
    mockMultiInputTransformer("id1");

    mockInputTransformer(
        new ImmutableMap.Builder<String, Object>().put(Constants.SERVICE_ID, "id2").build());

    transformerLocator.findMultiInputTransformers("id1)))");
  }

  @Test
  public void testFindMultiInputTransformersByIdSearchForSingle() {
    mockMultiInputTransformer("id1");

    InputTransformer single =
        mockInputTransformer(
            new ImmutableMap.Builder<String, Object>().put(Constants.SERVICE_ID, "id2").build());

    List<MultiInputTransformer> multiInputTransformers =
        transformerLocator.findMultiInputTransformers("id2");

    assertThat(multiInputTransformers, hasSize(1));
    assertThat(
        ((InputTransformerAdapter) multiInputTransformers.get(0)).getInputTransformer(),
        is(single));
  }

  @Test
  public void testFindMultiMetacardTransformersByIdSearchForMulti() {
    MultiMetacardTransformer multi = mockMultiMetacardTransformer("id1");
    mockMetacardTransformer(
        new ImmutableMap.Builder<String, Object>().put(Constants.SERVICE_ID, "id2").build());
    List<MultiMetacardTransformer> transformers =
        transformerLocator.findMultiMetacardTransformers("id1");

    assertThat(transformers, hasSize(1));
    assertThat(transformers.get(0), is(multi));
  }

  @Test
  public void testFindMultiMetacardTransformersByIdSearchForSingle() {
    mockMultiMetacardTransformer("id1");
    MetacardTransformer single =
        mockMetacardTransformer(
            new ImmutableMap.Builder<String, Object>().put(Constants.SERVICE_ID, "id2").build());
    List<MultiMetacardTransformer> transformers =
        transformerLocator.findMultiMetacardTransformers("id2");

    assertThat(transformers, hasSize(1));
    assertThat(
        ((MetacardTransformerAdapter) transformers.get(0)).getMetacardTransformer(), is(single));
  }

  @Test
  public void testFindMultiMetacardTransformers() throws MimeTypeParseException {
    MultiMetacardTransformer multi =
        mockMultiMetacardTransformer(new MimeType("text/plain"), new MimeType("text/xml"));

    MetacardTransformer single =
        mockMetacardTransformer(
            new ImmutableMap.Builder<String, Object>()
                .put(TransformerProperties.MIME_TYPE, Arrays.asList("text/plain", "text/xml"))
                .build());

    List<MultiMetacardTransformer> multiInputTransformers =
        transformerLocator.findMultiMetacardTransformers(new MimeType("text/plain"));

    assertThat(multiInputTransformers, hasSize(2));
    assertThat(multiInputTransformers.get(0), is(multi));
    assertThat(
        ((MetacardTransformerAdapter) multiInputTransformers.get(1)).getMetacardTransformer(),
        is(single));
  }

  @Test
  public void testFindQueryResponseTransformersSearchByMime() throws MimeTypeParseException {
    QueryResponseTransformer transformer1 =
        mockQueryResponseTransformer(
            new ImmutableMap.Builder<String, Object>()
                .put(TransformerProperties.MIME_TYPE, Arrays.asList("text/plain", "text/xml"))
                .build());

    mockQueryResponseTransformer(
        new ImmutableMap.Builder<String, Object>()
            .put(TransformerProperties.MIME_TYPE, Collections.singletonList("application/other"))
            .build());

    List<QueryResponseTransformer> transformers =
        transformerLocator.findQueryResponseTransformers(new MimeType("text/xml"));

    assertThat(transformers, hasSize(1));
    assertThat(transformers.get(0), is(transformer1));
  }

  @Test
  public void testFindQueryResponseTransformersSearchById() {
    mockQueryResponseTransformer(
        new ImmutableMap.Builder<String, Object>().put(Constants.SERVICE_ID, "id1").build());

    QueryResponseTransformer transformer2 =
        mockQueryResponseTransformer(
            new ImmutableMap.Builder<String, Object>().put(Constants.SERVICE_ID, "id2").build());

    List<QueryResponseTransformer> transformers =
        transformerLocator.findQueryResponseTransformers("id2");

    assertThat(transformers, hasSize(1));
    assertThat(transformers.get(0), is(transformer2));
  }

  /** Override the bundle context getter method so that the context can be mocked. */
  private class TransformerLocatorTest extends TransformerLocatorImpl {

    private BundleContext bundleContext;

    TransformerLocatorTest(BundleContext bundleContext) {
      this.bundleContext = bundleContext;
    }

    @Override
    protected BundleContext getContext() {
      return bundleContext;
    }
  }
}
