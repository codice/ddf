/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.security.pdp.api;

import oasis.names.tc.xacml._3_0.core.schema.wd_17.RequestType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ResponseType;

/**
 * Interface to the DDF Policy Decision Point (PDP).
 *
 */
public interface PolicyDecisionPoint
{
    /**
     * Evaluates a XACML request and returns a XACML response.
     * 
     * @param xacmlRequest The XACML request.
     * @return The XACML response.
     * @throws PdpException
     */
    ResponseType evaluate( RequestType xacmlRequest ) throws PdpException;
}
