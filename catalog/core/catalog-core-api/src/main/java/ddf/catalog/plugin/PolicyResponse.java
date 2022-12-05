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
package ddf.catalog.plugin;

import java.util.Map;
import java.util.Set;

/**
 * Response from {@link PolicyPlugin} containing policy information for {@link
 * ddf.catalog.operation.Operation} and individual items, such as {@link ddf.catalog.data.Metacard}
 * or {@link ddf.catalog.data.Result}
 */
@Deprecated
public interface PolicyResponse {

  /**
   * Policy to be applied to the catalog operation being attempted.
   *
   * <p>The {@link PolicyResponse} object contains 1 or more policy objects of the type: Map<String,
   * Set<String>>. Where the key is some attribute that you wish to assert against a Subject and
   * Set<String> would be the values associated with that key.
   *
   * @return Map containing policy information pertaining to this operation.
   */
  Map<String, Set<String>> operationPolicy();

  /**
   * Policy to be applied to item being worked on by the catalog.
   *
   * <p>The {@link PolicyResponse} object contains 1 or more policy objects of the type: Map<String,
   * Set<String>>. Where the key is some attribute that you wish to assert against a Subject and
   * Set<String> would be the values associated with that key.
   *
   * @return Map containing policy information pertaining to this item.
   */
  Map<String, Set<String>> itemPolicy();
}
