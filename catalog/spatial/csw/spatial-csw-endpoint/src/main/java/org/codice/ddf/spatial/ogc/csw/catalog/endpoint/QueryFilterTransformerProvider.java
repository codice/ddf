/*
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */

package org.codice.ddf.spatial.ogc.csw.catalog.endpoint;

import ddf.catalog.transform.QueryFilterTransformer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.namespace.QName;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages a reference list of {@link QueryFilterTransformer}'s by mapping them to the {@link
 * QName}'s they apply to.
 */
public class QueryFilterTransformerProvider {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(QueryFilterTransformerProvider.class);

  private Map<QName, QueryFilterTransformer> queryFilterTransformerMap = new ConcurrentHashMap<>();

  public synchronized void bind(ServiceReference<QueryFilterTransformer> reference) {
    if (reference == null) {
      return;
    }

    List<QName> namespaces = getNamespaces(reference);
    QueryFilterTransformer transformer = getTransformer(reference);

    for (QName namespace : namespaces) {
      queryFilterTransformerMap.put(namespace, transformer);
    }
  }

  public synchronized void unbind(ServiceReference<QueryFilterTransformer> reference) {
    if (reference == null) {
      return;
    }

    try {
      List<QName> namespaces = getNamespaces(reference);

      for (QName namespace : namespaces) {
        queryFilterTransformerMap.remove(namespace);
      }
    } finally {
      getBundleContext().ungetService(reference);
    }
  }

  public synchronized Optional<QueryFilterTransformer> getTransformer(QName qName) {
    if (qName == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(queryFilterTransformerMap.get(qName));
  }

  private List<QName> getNamespaces(ServiceReference<QueryFilterTransformer> reference) {
    Object id = reference.getProperty("id");
    List<QName> result = new ArrayList<>();
    if (id instanceof List) {
      List<String> namespaces = (List<String>) id;
      for (String namespace : namespaces) {
        result.add(QName.valueOf(namespace));
      }
    } else if (id instanceof String) {
      result.add(QName.valueOf((String) id));
    } else {
      LOGGER.debug(
          "QueryFilterTransformer reference has a bad ID property. Must be of type String or List<String>");
      throw new IllegalArgumentException("id must be of type String or a list of Strings");
    }

    return result;
  }

  private QueryFilterTransformer getTransformer(
      ServiceReference<QueryFilterTransformer> reference) {
    BundleContext bundleContext = getBundleContext();

    QueryFilterTransformer transformer = bundleContext.getService(reference);

    if (transformer == null) {
      LOGGER.debug("Failed to find a QueryFilterTransformer with service reference {}", reference);
      throw new IllegalStateException(
          "Attempted to retrieve an unregistered service: " + reference);
    }

    return transformer;
  }

  BundleContext getBundleContext() {
    return FrameworkUtil.getBundle(this.getClass()).getBundleContext();
  }
}
