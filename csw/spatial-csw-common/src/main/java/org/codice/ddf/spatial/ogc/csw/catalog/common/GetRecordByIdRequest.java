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
 * JAX-RS Parameter Bean Class for the GetRecordById request. The member variables will be
 * automatically injected by the JAX-RS annotations.
 * 
 */
public class GetRecordByIdRequest extends CswRequest {

    private String id;

    private String elementSetName;

    private String outputFormat;

    private String outputSchema;

    public GetRecordByIdRequest() {
        super(CswConstants.GET_RECORD_BY_ID);
    }

    public GetRecordByIdRequest(String service) {
        this();
        setService(service);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getElementSetName() {
        return elementSetName;
    }

    public void setElementSetName(String elementSetName) {
        this.elementSetName = elementSetName;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public String getOutputSchema() {
        return outputSchema;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    public void setOutputSchema(String outputSchema) {
        this.outputSchema = outputSchema;
    }
}
