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

import org.apache.cxf.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WfsRequest {
    private static final Logger LOGGER = LoggerFactory.getLogger(WfsRequest.class);

    private String request;
    private String service;
    private String typeName;
    private String version;
    
    public WfsRequest(){
        
    }
    
    public WfsRequest(QName qname){
        if(qname != null){
            this.typeName = StringUtils.isEmpty(qname.getPrefix()) ? qname.getLocalPart() : qname.getPrefix()
                    + ":" + qname.getLocalPart();
        } else {
            LOGGER.debug("Incoming QName was null");
        }
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
