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

package org.codice.ddf.spatial.ogc.csw.catalog.endpoint.api;

import org.xml.sax.helpers.NamespaceSupport;

/** Maps property names from a CSW query to catalog taxonomy */
public interface CswRecordMap {

  /**
   * Gets the catalog taxonomy attribute name for the property name, or returns the property name if
   * not mapped. {@link NamespaceSupport} will be used to resolve namespaces. If {@link
   * NamespaceSupport} is null, then {@link CswRecordMap#getProperty(String)} should be used.
   *
   * @param propertyName CSW query property name. Can be prefixed.
   * @param context Namespace context from the CSW query
   * @return Catalog taxonomy attribute name.
   */
  String getProperty(String propertyName, NamespaceSupport context);

  /**
   * Gets the catalog taxonomy attribute name for the property name, or returns the property name if
   * not mapped. If the property name is prefixed, then the prefix will be removed and first match
   * will be returned.
   *
   * @param propertyName CSW query property name. Can be prefixed.
   * @return Catalog taxonomy attribute name
   */
  String getProperty(String propertyName);

  /**
   * Returns true if the property name has a corresponding catalog taxonomy attribute. {@link
   * NamespaceSupport} will be used to resolve namespaces. If {@link NamespaceSupport} is null, then
   * {@link CswRecordMap#hasProperty(String)} should be used.
   *
   * @param propertyName CSW query property name. Can be prefixed.
   * @param context Namespace context from the CSW query
   * @return True if the property name has a corresponding catalog taxonomy attribute.
   */
  boolean hasProperty(String propertyName, NamespaceSupport context);

  /**
   * Returns true if the property name has a corresponding catalog taxonomy attribute. If the
   * property name is prefixed, then the prefix will be removed before performing the check.
   *
   * @param propertyName CSW query property name. Can be prefixed.
   * @return True if the property name has a corresponding catalog taxonomy attribute.
   */
  boolean hasProperty(String propertyName);
}
