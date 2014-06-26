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

/**
 * JAX-RS Parameter Bean Class for the GetCapabilities request. The member variables will be
 * automatically injected by the JAX-RS annotations.
 * 
 */
public class GetPropertyValueRequest {
	private String request = WfsConstants.GET_PROPERTY_VALUE;

    private String service = WfsConstants.WFS;

    private String version = WfsConstants.VERSION_2_0_0;
    
    private String adhocQuery = "";
    
    private String valueReference = "";
    
    private String resolvePath = "";

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

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getAdhocQuery() {
		return adhocQuery;
	}

	public void setAdhocQuery(String adhocQuery) {
		this.adhocQuery = adhocQuery;
	}

	public String getValueReference() {
		return valueReference;
	}

	public void setValueReference(String valueReference) {
		this.valueReference = valueReference;
	}

	public String getResolvePath() {
		return resolvePath;
	}

	public void setResolvePath(String resolvePath) {
		this.resolvePath = resolvePath;
	}
	
}
