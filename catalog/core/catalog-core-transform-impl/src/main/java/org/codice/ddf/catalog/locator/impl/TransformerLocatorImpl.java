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

import ddf.catalog.Constants;
import ddf.catalog.transform.InputTransformer;
import ddf.catalog.transform.MetacardTransformer;
import ddf.catalog.transform.QueryResponseTransformer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.activation.MimeType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.codice.ddf.catalog.locator.TransformerLocator;
import org.codice.ddf.catalog.transform.MultiInputTransformer;
import org.codice.ddf.catalog.transform.MultiMetacardTransformer;
import org.codice.ddf.catalog.transform.TransformerProperties;
import org.codice.ddf.catalog.transform.impl.AbstractTransformerAdapter;
import org.codice.ddf.catalog.transform.impl.InputTransformerAdapter;
import org.codice.ddf.catalog.transform.impl.MetacardTransformerAdapter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * For the input transformers, first search for MultiInputTransformers, and then search for
 * InputTransformers. That way the MultiInputTransformers will take precedence. The
 * InputTransformers will be wrapped in a InputTransformerAdapter.
 *
 * <p>For the metacard transformers, first search for MultiMetacardTransformers, and then search for
 * MetacardTransformers. That way the MultiMetacardTransformers will take precedence. The
 * MetacardTransformers will be wrapped in a MetacardTransformerAdapter.
 */
public class TransformerLocatorImpl implements TransformerLocator {

  @Override
  public List<MultiInputTransformer> findMultiInputTransformers() {
    return findMultiTransformers(
        MultiInputTransformer.class, InputTransformer.class, InputTransformerAdapter::new, null);
  }

  @Override
  public List<MultiInputTransformer> findMultiInputTransformers(MimeType mimeType) {
    return findMultiTransformers(
        MultiInputTransformer.class,
        InputTransformer.class,
        InputTransformerAdapter::new,
        null,
        multiInputTransformer -> filterByMimeType(multiInputTransformer, mimeType),
        serviceProperties -> filterByMimeType(serviceProperties, mimeType));
  }

  @Override
  public List<MultiInputTransformer> findMultiInputTransformers(String transformerId) {
    return findMultiTransformers(
        MultiInputTransformer.class,
        InputTransformer.class,
        InputTransformerAdapter::new,
        generateOsgiFilter(transformerId));
  }

  @Override
  public List<MultiMetacardTransformer> findMultiMetacardTransformers(String transformerId) {
    return findMultiTransformers(
        MultiMetacardTransformer.class,
        MetacardTransformer.class,
        MetacardTransformerAdapter::new,
        generateOsgiFilter(transformerId));
  }

  @Override
  public List<MultiMetacardTransformer> findMultiMetacardTransformers(MimeType mimeType) {
    return findMultiTransformers(
        MultiMetacardTransformer.class,
        MetacardTransformer.class,
        MetacardTransformerAdapter::new,
        null,
        multiMetacardTransformer -> filterByMimeType(multiMetacardTransformer, mimeType),
        serviceProperties -> filterByMimeType(serviceProperties, mimeType));
  }

  @Override
  public List<QueryResponseTransformer> findQueryResponseTransformers(MimeType mimeType) {
    return findQueryResponseTransformers(
        serviceProperties -> filterByMimeType(serviceProperties, mimeType), null);
  }

  @Override
  public List<QueryResponseTransformer> findQueryResponseTransformers(String transformerId) {
    return findQueryResponseTransformers(
        serviceProperties -> true, generateOsgiFilter(transformerId));
  }

  @SuppressWarnings("deprecation")
  private String generateOsgiFilter(String transformerId) {
    return "(|"
        + "("
        + Constants.SERVICE_SHORTNAME
        + "="
        + transformerId
        + ")"
        + "("
        + Constants.SERVICE_ID
        + "="
        + transformerId
        + ")"
        + ")";
  }

  private List<QueryResponseTransformer> findQueryResponseTransformers(
      Predicate<Map<String, Object>> servicePropertiesFilter, String filter) {
    return findTransformers(
        QueryResponseTransformer.class,
        (transformer, properties) -> transformer,
        filter,
        servicePropertiesFilter);
  }

  private <T extends TransformerProperties> boolean filterByMimeType(
      T transformer, MimeType userMimeType) {
    return filterByMimeType(transformer.getMimeTypes(), transformer.getProperties(), userMimeType);
  }

  private boolean filterByMimeType(Map<String, Object> serviceProperties, MimeType userMimeType) {
    return filterByMimeType(
        AbstractTransformerAdapter.getTransformerMimeTypes(serviceProperties),
        serviceProperties,
        userMimeType);
  }

  private boolean filterByMimeType(
      Set<MimeType> mimeTypes, Map<String, Object> serviceProperties, MimeType userMimeType) {

    Set<String> baseTypes =
        mimeTypes.stream().map(MimeType::getBaseType).collect(Collectors.toSet());

    String userIdValue = userMimeType.getParameter(Constants.SERVICE_ID);

    String userBaseType = userMimeType.getBaseType();

    String serviceId = getServiceId(serviceProperties);

    return CollectionUtils.isNotEmpty(mimeTypes)
        && baseTypes.contains(userBaseType)
        && (userIdValue == null || StringUtils.equals(userIdValue, serviceId));
  }

  private String getServiceId(Map<String, Object> properties) {
    Object idServiceProperty = properties.get(Constants.SERVICE_ID);

    if (idServiceProperty != null) {
      return idServiceProperty.toString();
    }

    return null;
  }

  private Map<String, Object> getServiceProperties(ServiceReference serviceReference) {
    return Arrays.stream(serviceReference.getPropertyKeys())
        .collect(Collectors.toMap(s -> s, serviceReference::getProperty));
  }

  protected BundleContext getContext() {
    Bundle bundle = FrameworkUtil.getBundle(TransformerLocatorImpl.class);
    if (bundle != null) {
      return bundle.getBundleContext();
    }
    return null;
  }

  private <T> Collection<ServiceReference<T>> findServices(
      Class<T> clazz, String filter, BundleContext bundleContext) {
    try {
      return bundleContext.getServiceReferences(clazz, filter);
    } catch (InvalidSyntaxException e) {
      throw new IllegalArgumentException("Invalid syntax supplied");
    }
  }

  private <T extends TransformerProperties> List<T> findTransformers(
      Class<T> clazz, String filter, Predicate<T> transformerPropertiesFilter) {
    BundleContext bundleContext = getContext();

    Collection<ServiceReference<T>> refs = findServices(clazz, filter, bundleContext);

    if (refs != null) {
      return refs.stream()
          .sorted(Collections.reverseOrder())
          .map(bundleContext::getService)
          .filter(transformerPropertiesFilter)
          .collect(Collectors.toList());
    }

    return Collections.emptyList();
  }

  private <T, R> List<R> findTransformers(
      Class<T> clazz,
      BiFunction<T, Map<String, Object>, R> converter,
      String filter,
      Predicate<Map<String, Object>> servicePropertiesFilter) {
    BundleContext bundleContext = getContext();

    Collection<ServiceReference<T>> refs = findServices(clazz, filter, bundleContext);

    if (refs != null) {
      return refs.stream()
          .sorted(Collections.reverseOrder())
          .map(ref -> new ImmutablePair<>(bundleContext.getService(ref), getServiceProperties(ref)))
          .filter(pair -> servicePropertiesFilter.test(pair.getRight()))
          .map(pair -> converter.apply(pair.getLeft(), pair.getRight()))
          .collect(Collectors.toList());
    }

    return Collections.emptyList();
  }

  private <M extends TransformerProperties, S> List<M> findMultiTransformers(
      Class<M> multiClazz,
      Class<S> singleClazz,
      BiFunction<S, Map<String, Object>, M> convertSingleToMulti,
      String filter,
      Predicate<M> transformerPropertiesFilter,
      Predicate<Map<String, Object>> servicePropertiesFilter) {

    List<M> multiMetacardTransformers = new LinkedList<>();

    multiMetacardTransformers.addAll(
        findTransformers(multiClazz, filter, transformerPropertiesFilter));

    multiMetacardTransformers.addAll(
        findTransformers(singleClazz, convertSingleToMulti, filter, servicePropertiesFilter));

    return multiMetacardTransformers;
  }

  private <M extends TransformerProperties, S> List<M> findMultiTransformers(
      Class<M> multiClazz,
      Class<S> singleClazz,
      BiFunction<S, Map<String, Object>, M> convertSingleToMulti,
      String filter) {
    return findMultiTransformers(
        multiClazz,
        singleClazz,
        convertSingleToMulti,
        filter,
        multiInputTransformer -> true,
        serviceProperties -> true);
  }
}
