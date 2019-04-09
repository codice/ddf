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
package ddf.platform.solr.security;

import java.util.Map;

public interface SolrPasswordUpdate {

  /**
   * This method will read and potentially mutate the input object. It does not persist the changes
   * to a file or copy any changes to the System's Properties object. That is the responsibility of
   * the caller.
   *
   * @param properties is typically an instance of org.apache.felix.utils.properties.Properties
   *     which extends AbstractMap
   */
  void update(Map<String, String> properties);
}
