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

import ddf.catalog.data.Metacard;
import java.io.Serializable;

public interface ResourceRequest extends Request {

  public static final String GET_RESOURCE_BY_ID = Metacard.ID;

  public static final String GET_RESOURCE_BY_PRODUCT_URI = Metacard.RESOURCE_URI;

  public static final String OPTION_ARGUMENT = "RESOURCE_OPTION";

  public static final String SOURCE_ID = "SOURCE_ID";

  public static final String IS_ENTERPRISE = "IS_ENTERPRISE";

  public static final String QUALIFIER = "QUALIFIER";

  public String getAttributeName();

  public Serializable getAttributeValue();
}
