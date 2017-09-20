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
package ddf.catalog.endpoint.impl;

import ddf.catalog.endpoint.CatalogEndpoint;
import java.util.HashMap;
import java.util.Map;
import org.codice.ddf.configuration.PropertyResolver;

public class CatalogEndpointImpl implements CatalogEndpoint {
  protected Map<String, String> endpointProperties;

  public static final String URL_BINDING_NAME_KEY = "urlBindingName";

  /**
   * Constructor
   *
   * <p>Creates empty endpoint properties map
   */
  public CatalogEndpointImpl() {
    this(new HashMap<>());
  }

  /**
   * Constructor
   *
   * @param endpointProperties - a map of endpoint properties
   */
  public CatalogEndpointImpl(Map<String, String> endpointProperties) {
    if (endpointProperties == null) {
      setEndpointProperties(new HashMap<>());
    } else {
      setEndpointProperties(endpointProperties);
    }
  }

  /**
   * Puts bindingType onto the endpoint properties map. Removes the entry from the map if
   * bindingType is null.
   *
   * @param bindingType
   */
  public void setBindingType(String bindingType) {
    setProperty(BINDING_TYPE_KEY, bindingType);
  }

  /**
   * Puts description onto the endpoint properties map. Removes the entry from the map if
   * description is null.
   *
   * @param description
   */
  public void setDescription(String description) {
    setProperty(DESCRIPTION_KEY, description);
  }

  /**
   * Puts id onto the endpoint properties map. Removes the entry from the map if id is null.
   *
   * @param id
   */
  public void setId(String id) {
    setProperty(ID_KEY, id);
  }

  /**
   * Puts name onto the endpoint properties map. Removes the entry from the map if name is null.
   *
   * @param name
   */
  public void setName(String name) {
    setProperty(NAME_KEY, name);
  }

  /**
   * Puts url onto the endpoint properties map. Removes the entry from the map if url is null.
   *
   * @param url
   */
  public void setUrl(String url) {
    setProperty(URL_KEY, PropertyResolver.resolveProperties(url));
  }

  /**
   * Puts version onto the endpoint properties map. Removes the entry from the map if version is
   * null.
   *
   * @param version
   */
  public void setVersion(String version) {
    setProperty(VERSION_KEY, version);
  }

  /**
   * Puts urlBindingName onto the endpoint properties map. Removes the entry from the map if
   * urlBindingName is null.
   *
   * @param urlBindingName
   */
  public void setUrlBindingName(String urlBindingName) {
    setProperty(URL_BINDING_NAME_KEY, urlBindingName);
  }

  /**
   * Set the endpointProperties map.
   *
   * @param endpointProperties
   */
  public void setEndpointProperties(Map<String, String> endpointProperties) {
    this.endpointProperties = endpointProperties;
  }

  /**
   * Returns the endpointProperties map.
   *
   * @return endpointPropties
   */
  @Override
  public Map<String, String> getEndpointProperties() {
    return endpointProperties;
  }

  private void setProperty(String key, String value) {
    if (value == null) {
      getEndpointProperties().remove(key);
    } else {
      getEndpointProperties().put(key, value);
    }
  }
}
