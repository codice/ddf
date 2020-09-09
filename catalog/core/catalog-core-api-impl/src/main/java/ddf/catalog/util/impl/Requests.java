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
package ddf.catalog.util.impl;

import ddf.catalog.Constants;
import ddf.catalog.operation.Request;
import java.io.Serializable;
import java.util.Map;

/** Class for static request helper method */
public class Requests {

  private Requests() {}

  /**
   * Returns true if this request will be run on a remote catalog. This does not mean that this same
   * request will not be run on the local catalog. Implementation requires the catalog framework to
   * set the needed properties for this method to work.
   *
   * @param req The request to check
   * @return Returns true if the request contains remote store ids otherwise returns false
   */
  public static boolean isEnterprise(Request req) {
    return req != null
        && req.hasProperties()
        && req.getPropertyValue(Constants.REMOTE_DESTINATION_KEY) != null
        && (boolean) req.getPropertyValue(Constants.REMOTE_DESTINATION_KEY);
  }

  /**
   * Returns true if this request will be run on the local catalog. This does not mean that this
   * same request will not be run on a remote catalog. Implementation requires the catalog framework
   * to set the needed properties for this method to work.
   *
   * @param req The request to check
   * @return Returns true if the request contains the local catalog id in its store ids otherwise
   *     returns false
   */
  public static boolean isLocal(Request req) {
    return req == null || !req.hasProperties() || isLocal(req.getProperties());
  }

  /**
   * Returns true if this request will be run on the local catalog. This does not mean that this
   * same request will not be run on a remote catalog. Implementation requires the catalog framework
   * to set the needed properties for this method to work.
   *
   * @param props Property map of a request
   * @return Returns true if the properties contains the local catalog id in its store ids otherwise
   *     returns false
   */
  public static boolean isLocal(Map<String, Serializable> props) {
    return props == null
        || props.get(Constants.LOCAL_DESTINATION_KEY) == null
        || (boolean) props.get(Constants.LOCAL_DESTINATION_KEY);
  }
}
