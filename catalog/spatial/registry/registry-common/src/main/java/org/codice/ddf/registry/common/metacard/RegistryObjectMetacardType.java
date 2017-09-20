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
package org.codice.ddf.registry.common.metacard;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardTypeImpl;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Base MetacardType for all registry MetacardTypes. */
public class RegistryObjectMetacardType extends MetacardTypeImpl {

  public static final String DATA_SOURCES = "registry.input-data-sources";

  public static final String LAST_PUBLISHED = "registry.local.last-published";

  public static final String LINKS = "registry.links";

  public static final String PUBLISHED_LOCATIONS = "registry.local.published-locations";

  public static final String REGION = "registry.region";

  public static final String REGISTRY_BASE_URL = "registry.registry-base-url";

  public static final String REGISTRY_ID = "registry.registry-id";

  public static final String REGISTRY_IDENTITY_NODE = "registry.local.registry-identity-node";

  public static final String REGISTRY_LOCAL_NODE = "registry.local.registry-local-node";

  public static final String REGISTRY_METACARD_TYPE_NAME = "registry";

  public static final String REMOTE_METACARD_ID = "registry.local.remote-metacard-id";

  public static final String REMOTE_REGISTRY_ID = "registry.local.remote-registry-id";

  public static final String SECURITY_LEVEL = "registry.security-level";

  // list of bindingType fields from all the service bindings
  public static final String SERVICE_BINDING_TYPES = "registry.service-binding-types";

  // list of all the service binding ids
  public static final String SERVICE_BINDINGS = "registry.service-bindings";

  public static final Set<String> TRANSIENT_ATTRIBUTES;

  static {
    final Set<String> transientAttributes = new HashSet<>();
    transientAttributes.add(REGISTRY_IDENTITY_NODE);
    transientAttributes.add(REGISTRY_LOCAL_NODE);
    transientAttributes.add(PUBLISHED_LOCATIONS);
    transientAttributes.add(LAST_PUBLISHED);
    transientAttributes.add(REMOTE_REGISTRY_ID);
    transientAttributes.add(REMOTE_METACARD_ID);
    TRANSIENT_ATTRIBUTES = Collections.unmodifiableSet(transientAttributes);
  }

  public RegistryObjectMetacardType() {
    this(REGISTRY_METACARD_TYPE_NAME, null);
  }

  public RegistryObjectMetacardType(String name, Set<AttributeDescriptor> descriptors) {
    super(name, descriptors);
    addRegistryAttributes();
  }

  private void addRegistryAttributes() {
    descriptors.add(BasicTypes.BASIC_METACARD.getAttributeDescriptor(Metacard.POINT_OF_CONTACT));
    addQueryableBoolean(REGISTRY_IDENTITY_NODE, false);
    addQueryableBoolean(REGISTRY_LOCAL_NODE, false);
    addQueryableDate(LAST_PUBLISHED);
    addQueryableString(DATA_SOURCES, true);
    addQueryableString(LINKS, true);
    addQueryableString(PUBLISHED_LOCATIONS, true);
    addQueryableString(REGION, false);
    addQueryableString(REGISTRY_BASE_URL, false);
    addQueryableString(REGISTRY_ID, false);
    addQueryableString(REMOTE_METACARD_ID, false);
    addQueryableString(REMOTE_REGISTRY_ID, false);
    addQueryableString(SECURITY_LEVEL, true);
    addQueryableString(SERVICE_BINDING_TYPES, true);
    addQueryableString(SERVICE_BINDINGS, true);
  }

  /**
   * Method to add a queryable string to the descriptors of this metacard type. Can be used to
   * dynamically add additional descriptors to the base set.
   *
   * @param name Name of the descriptor
   * @param multivalued Whether or not this descriptor represents several values (true) or one value
   *     (false)
   */
  public void addQueryableString(String name, boolean multivalued) {
    addDescriptor(name, true, multivalued, BasicTypes.STRING_TYPE);
  }

  /**
   * Method to add a queryable date to the descriptors of this metacard type. Can be used to
   * dynamically add additional descriptors to the base set.
   *
   * @param name Name of the descriptor
   */
  public void addQueryableDate(String name) {
    addDescriptor(name, true, false, BasicTypes.DATE_TYPE);
  }

  /**
   * Method to add a queryable boolean to the descriptors of this metacard type. Can be used to
   * dynamically add additional descriptors to the base set.
   *
   * @param name Name of the descriptor
   * @param multivalued Whether or not this descriptor represents several values (true) or one value
   *     (false)
   */
  public void addQueryableBoolean(String name, boolean multivalued) {
    addDescriptor(name, true, multivalued, BasicTypes.BOOLEAN_TYPE);
  }

  /**
   * Method to add an XML entry to the descriptors of the metacard type. Can be used to dynamically
   * add additional descriptors to the base set.
   *
   * @param name Name of the descriptor
   * @param queryable Whether or not this descriptor should be queryable.
   */
  public void addXml(String name, boolean queryable) {
    addDescriptor(name, queryable, false, BasicTypes.XML_TYPE);
  }

  protected void addQueryableGeo(String name, boolean multivalued) {
    addDescriptor(name, true, multivalued, BasicTypes.GEO_TYPE);
  }

  protected void addDescriptor(
      String name, boolean queryable, boolean multivalued, AttributeType<?> type) {
    descriptors.add(
        new AttributeDescriptorImpl(
            name,
            queryable /* indexed */,
            true /* stored */,
            false /* tokenized */,
            multivalued /* multivalued */,
            type));
  }
}
