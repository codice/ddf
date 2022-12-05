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
package ddf.catalog.operation;

import java.io.Serializable;
import java.util.Set;

/**
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */

/**
 * TermFacetPropertiesImpl object using the supplied parameters to facet on the provided attributes,
 * returning results sorted by the provided key.
 */
@Deprecated
public interface TermFacetProperties extends Serializable {

  enum SortFacetsBy {
    INDEX,
    COUNT
  }

  Set<String> getFacetAttributes();

  SortFacetsBy getSortKey();

  int getFacetLimit();

  int getMinFacetCount();
}
