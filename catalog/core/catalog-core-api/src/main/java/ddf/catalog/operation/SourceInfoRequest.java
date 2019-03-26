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

import ddf.catalog.data.types.Core;
import ddf.catalog.federation.Federatable;

/** The SourceInfoRequest represents a request to obtain {@link Source} information. */
public interface SourceInfoRequest extends Request, Federatable {

  public static final String GET_RESOURCE_BY_ID = Core.ID;

  public static final String GET_RESOURCE_BY_PRODUCT_URI = Core.RESOURCE_URI;

  /**
   * Include content types.
   *
   * @return true, if successful
   */
  public boolean includeContentTypes();
}
