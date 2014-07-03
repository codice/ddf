/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common;

import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsRequest;

/**
 * JAX-RS Parameter Bean Class for the GetCapabilitiesRequest request. The member variables will be
 * automatically injected by the JAX-RS annotations.
 * 
 */
public class GetCapabilitiesRequest extends WfsRequest {
    
    public GetCapabilitiesRequest(){
        super();
        setRequest(WfsConstants.GET_CAPABILITIES);
        setVersion(WfsConstants.VERSION_2_0_0);
        setService(WfsConstants.WFS);
    }
}
