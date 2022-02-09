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
package org.codice.ddf.spatial.ogc.csw.catalog.common.transformer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.api.TransformerManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Maintains a list of available Transformers and their properties. */
public class TransformerManagerImpl implements TransformerManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(TransformerManagerImpl.class);

  public static final String MIME_TYPE = "mime-type";

  public static final String SCHEMA = "schema";

  public static final String SCHEMA_LOCATION = "schema-location";

  public static final String ID = "id";

  private final List<ServiceReference> serviceRefs;

  public TransformerManagerImpl(List<ServiceReference> serviceReferences) {
    this.serviceRefs = serviceReferences;
  }

  @Override
  public List<String> getAvailableMimeTypes() {
    return getAvailableProperty(MIME_TYPE);
  }

  @Override
  public List<String> getAvailableSchemas() {
    return getAvailableProperty(SCHEMA);
  }

  @Override
  public List<String> getAvailableIds() {
    return getAvailableProperty(ID);
  }

  @Override
  public String getTransformerIdForSchema(String schema) {

    List<Map<String, Object>> properties = getRelatedTransformerProperties(SCHEMA, schema);
    if (!properties.isEmpty()) {
      return (String) properties.get(0).get(ID);
    }
    return "";
  }

  @Override
  public String getTransformerSchemaForId(String id) {
    List<Map<String, Object>> properties = getRelatedTransformerProperties(ID, id);
    if (!properties.isEmpty()) {
      return (String) properties.get(0).get(SCHEMA);
    }
    return "";
  }

  @Override
  public String getTransformerSchemaLocationForId(String id) {

    List<Map<String, Object>> properties = getRelatedTransformerProperties(ID, id);
    if (properties.size() > 0) {
      return (String) properties.get(0).get(SCHEMA_LOCATION);
    }
    return "";
  }

  @Override
  public List<String> getAvailableProperty(String propertyName) {
    List<String> properties = new ArrayList<String>();

    for (ServiceReference serviceRef : serviceRefs) {
      Object mimeObject = serviceRef.getProperty(propertyName);
      if (mimeObject != null && mimeObject instanceof String) {
        properties.add((String) mimeObject);
      }
    }
    return properties;
  }

  @Override
  public <T> T getTransformerBySchema(String schema) {
    return getTransformerByProperty(SCHEMA, schema);
  }

  @Override
  public <T> T getTransformerByMimeType(String mimeType) {
    return getTransformerByProperty(MIME_TYPE, mimeType);
  }

  @Override
  public <T> T getTransformerById(String id) {
    return getTransformerByProperty(ID, id);
  }

  @Override
  public <T> T getTransformerByProperty(String property, String value) {
    if (value == null) {
      return null;
    }
    LOGGER.trace("Looking up transformer for property: {} == value: {}", property, value);
    for (ServiceReference serviceRef : serviceRefs) {
      Object propertyObject = serviceRef.getProperty(property);
      if (propertyObject != null && propertyObject instanceof String) {
        if (value.equals((String) propertyObject)) {
          LOGGER.trace("Found transformer for property: {} == value: {}", property, value);
          T serviceObject = (T) getBundleContext().getService(serviceRef);
          LOGGER.trace("Transformer is {}", serviceObject);
          return serviceObject;
        }
      }
    }
    LOGGER.debug("Did not find transformer for property: {} == value: {}", property, value);
    return null;
  }

  @Override
  public List<Map<String, Object>> getRelatedTransformerProperties(String property, String value) {
    List<Map<String, Object>> properties = new ArrayList<>();
    for (ServiceReference serviceRef : serviceRefs) {
      Object propertyObject = serviceRef.getProperty(property);
      if (value.equals(propertyObject)) {
        Map<String, Object> map = new HashMap<>();
        for (String key : serviceRef.getPropertyKeys()) {
          map.put(key, serviceRef.getProperty(key));
        }
        properties.add(map);
      }
    }
    return properties;
  }

  protected BundleContext getBundleContext() {
    return FrameworkUtil.getBundle(this.getClass()).getBundleContext();
  }
}
