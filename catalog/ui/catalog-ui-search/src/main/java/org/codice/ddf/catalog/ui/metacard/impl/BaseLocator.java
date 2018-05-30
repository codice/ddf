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
package org.codice.ddf.catalog.ui.metacard.impl;

import ddf.catalog.Constants;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.activation.MimeType;
import org.apache.commons.lang3.StringUtils;
import org.codice.ddf.catalog.ui.metacard.internal.ServiceProperties;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides basic methods for locating services. It is intended that it will be extended
 * to create service specific locators.
 */
public class BaseLocator {

  private static final Logger LOGGER = LoggerFactory.getLogger(BaseLocator.class);

  /**
   * Determine if a service supports a given mime-type. The mime-type may include a property for
   * {@link Constants#SERVICE_ID} used to further restrict matches.
   *
   * @param service the service being tested
   * @param mimeType the mime-type being matched
   * @param <T>
   * @return {@code true} if the service handles the given mime-type (and optional service
   *     identifier), otherwise {@code false}
   */
  protected <T extends ServiceProperties> boolean filterByMimeType(T service, MimeType mimeType) {
    return filterByMimeType(service.getMimeTypes(), service.getProperties(), mimeType);
  }

  private boolean filterByMimeType(
      Set<MimeType> serviceMimeTypes,
      Map<String, Object> serviceProperties,
      MimeType targetMimeType) {

    Set<String> serviceBaseTypes =
        serviceMimeTypes.stream().map(MimeType::getBaseType).collect(Collectors.toSet());

    String targetServiceId = targetMimeType.getParameter(Constants.SERVICE_ID);

    String targetBaseType = targetMimeType.getBaseType();

    String serviceId = getServiceId(serviceProperties);

    return serviceBaseTypes.contains(targetBaseType)
        && (targetServiceId == null || StringUtils.equals(targetServiceId, serviceId));
  }

  private String getServiceId(Map<String, Object> properties) {
    Object idServiceProperty = properties.get(Constants.SERVICE_ID);

    if (idServiceProperty != null) {
      return idServiceProperty.toString();
    }

    return null;
  }

  private BundleContext getContext() {
    Bundle bundle = FrameworkUtil.getBundle(BaseLocator.class);
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
      throw new IllegalArgumentException(String.format("Invalid syntax supplied: %s", filter));
    }
  }

  protected <T extends ServiceProperties> List<T> findServices(
      Class<T> clazz, String filter, Predicate<T> servicePropertiesFilter) {
    BundleContext bundleContext = getContext();

    if (bundleContext == null) {
      LOGGER.debug("Unable to get the OSGi bundle context.");
      return Collections.emptyList();
    }

    Collection<ServiceReference<T>> refs = findServices(clazz, filter, bundleContext);

    if (refs != null) {
      return refs.stream()
          .sorted(Collections.reverseOrder())
          .map(bundleContext::getService)
          .filter(servicePropertiesFilter)
          .collect(Collectors.toList());
    }

    return Collections.emptyList();
  }
}
