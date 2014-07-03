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
package org.codice.ddf.spatial.ogc.wfs.catalog.common;

import javax.xml.namespace.QName;

/**
 * JAX-RS Parameter Bean Class for the DescribeFeatureType request. The member variables will be
 * automatically injected by the JAX-RS annotations.
 * 
 */
public class DescribeFeatureTypeRequest extends WfsRequest {

    public DescribeFeatureTypeRequest() {
        setRequest(WfsConstants.DESCRIBE_FEATURE_TYPE);
        setVersion(WfsConstants.VERSION_1_0_0);
        setService(WfsConstants.WFS);
    }

    public DescribeFeatureTypeRequest(QName qname) {
        super(qname);
        setRequest(WfsConstants.DESCRIBE_FEATURE_TYPE);
        setVersion(WfsConstants.VERSION_1_0_0);
        setService(WfsConstants.WFS);
    }
}
