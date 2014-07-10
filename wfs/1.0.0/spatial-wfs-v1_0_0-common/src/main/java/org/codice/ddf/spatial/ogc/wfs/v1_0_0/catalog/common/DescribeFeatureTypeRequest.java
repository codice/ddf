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
package org.codice.ddf.spatial.ogc.wfs.v1_0_0.catalog.common;

import javax.xml.namespace.QName;

import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsRequest;

/**
 * JAX-RS Parameter Bean Class for the DescribeFeatureType request. The member variables will be
 * automatically injected by the JAX-RS annotations.
 * 
 */
public class DescribeFeatureTypeRequest extends WfsRequest {

    public DescribeFeatureTypeRequest() {
        setRequest(Wfs10Constants.DESCRIBE_FEATURE_TYPE);
        setVersion(Wfs10Constants.VERSION_1_0_0);
        setService(Wfs10Constants.WFS);
    }

    public DescribeFeatureTypeRequest(QName qname) {
        super(qname);
        setRequest(Wfs10Constants.DESCRIBE_FEATURE_TYPE);
        setVersion(Wfs10Constants.VERSION_1_0_0);
        setService(Wfs10Constants.WFS);
    }
}
