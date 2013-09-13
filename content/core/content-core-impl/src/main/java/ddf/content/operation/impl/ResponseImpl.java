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
package ddf.content.operation.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import ddf.content.operation.Request;
import ddf.content.operation.Response;

/**
 * Response properties are the properties added specifically to a {@link Response} that are intended
 * for distribution to external clients, e.g., by Endpoints.
 * 
 * Properties, associated with the parent class {@link Operation} are properties intended for use by
 * the Content Framework components, e.g., {@link StorageProvider}s and {@link ContentPlugin}s.
 * 
 * @author Hugh Rodgers
 * @author ddf.isgs@lmco.com
 * 
 */
public class ResponseImpl<T extends Request> extends OperationImpl implements Response<T> {
    /** The original request associated with this response. */
    protected T request;

    /** The {@link Map} of response properties associated with an {@link Operation} */
    protected Map<String, String> responseProperties;

    /**
     * Instantiates an ResponseImpl object with a {@link Map} of response properties.
     * 
     * @param request
     *            - the original request
     * @param properties
     *            the properties of the response
     */
    public ResponseImpl(T request, Map<String, String> responseProperties) {
        this(request, responseProperties, null);
    }

    public ResponseImpl(T request, Map<String, String> responseProperties,
            Map<String, Serializable> properties) {
        super(properties);

        this.request = request;
        this.responseProperties = responseProperties;
        if (this.responseProperties == null) {
            this.responseProperties = new HashMap<String, String>();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.operation.Response#getRequest()
     */
    @Override
    public T getRequest() {
        return request;
    }

    /**
     * Set the {@link Map} of response properties for an {@link Response}.
     * 
     * @param newProperties
     *            the response properties
     */
    public void setResponseProperties(Map<String, String> newProperties) {
        this.responseProperties = newProperties;
    }

    @Override
    public Set<String> getResponsePropertyNames() {
        return responseProperties.keySet();
    }

    @Override
    public String getResponsePropertyValue(String name) {
        return responseProperties.get(name);
    }

    @Override
    public boolean containsResponsePropertyName(String name) {
        return responseProperties.containsKey(name);
    }

    @Override
    public boolean hasResponseProperties() {
        return !responseProperties.isEmpty();
    }

    @Override
    public Map<String, String> getResponseProperties() {
        return responseProperties;
    }
}
