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
package org.codice.ddf.spatial.ogc.csw.catalog.common;

/**
 * JAX-RS Parameter Bean Class for the GetCapabilities request. The member variables will be
 * automatically injected by the JAX-RS annotations.
 * 
 */
public class GetCapabilitiesRequest extends CswRequest {

    // The following parameters are optional for the GetCapabilities Request
    private String acceptVersions;

    private String sections;

    private String updateSequence;

    private String acceptFormats;

    public GetCapabilitiesRequest() {
        super(CswConstants.GET_CAPABILITIES);
    }

    public GetCapabilitiesRequest(String service) {
        this();
        setService(service);
    }
    
    public String getAcceptVersions() {
        return acceptVersions;
    }

    public void setAcceptVersions(String acceptVersions) {
        this.acceptVersions = acceptVersions;
    }

    public String getSections() {
        return sections;
    }

    public void setSections(String sections) {
        this.sections = sections;
    }

    public String getUpdateSequence() {
        return updateSequence;
    }

    public void setUpdateSequence(String updateSequence) {
        this.updateSequence = updateSequence;
    }

    public String getAcceptFormats() {
        return acceptFormats;
    }

    public void setAcceptFormats(String acceptFormats) {
        this.acceptFormats = acceptFormats;
    }
}
