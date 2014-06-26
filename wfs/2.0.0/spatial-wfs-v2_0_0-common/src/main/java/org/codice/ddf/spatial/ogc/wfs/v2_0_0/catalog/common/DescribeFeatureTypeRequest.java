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

import javax.xml.namespace.QName;

/**
 * JAX-RS Parameter Bean Class for the DescribeFeatureType request. The member variables will be
 * automatically injected by the JAX-RS annotations.
 * 
 */
public class DescribeFeatureTypeRequest {
    private String request = WfsConstants.DESCRIBE_FEATURE_TYPE;

    private String version = WfsConstants.VERSION_2_0_0;

    private String service = WfsConstants.WFS;

    private String typeName;

    public DescribeFeatureTypeRequest() {
        // Needed for Injection
    }

    public DescribeFeatureTypeRequest(QName qname) {
        this.typeName = qname.getPrefix() == null ? qname.getLocalPart() : qname.getPrefix()
                + WfsConstants.NAMESPACE_DELIMITER + qname.getLocalPart();
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

}
