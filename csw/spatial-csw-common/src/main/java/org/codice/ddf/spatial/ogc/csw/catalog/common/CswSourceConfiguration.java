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

import java.util.HashMap;
import java.util.Map;

import ddf.catalog.data.Metacard;

/**
 * Domain object to encapsulate the configuration of an instance of a {@link CswSource}. CSW
 * converters, readers, etc. will access this object to determine the latest configuration of the
 * {@link CswSource} they are working on.
 * 
 * @author rodgersh
 * 
 */
public class CswSourceConfiguration {

    private String cswUrl;

    private String id;

    private String username;

    private String password;

    private boolean disableSSLCertVerification = false;

    private String wcsUrl;

    private Map<String, String> metacardCswMappings = new HashMap<String, String>();

    private String resourceUriMapping;

    private String thumbnailMapping;

    private boolean isLonLatOrder;

    private Integer pollIntervalMinutes;

    private String productRetrievalMethod;

    private boolean isCqlForced;

    private String outputSchema;

    public String getCswUrl() {
        return cswUrl;
    }

    public void setCswUrl(String cswUrl) {
        this.cswUrl = cswUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEffectiveDateMapping() {
        return metacardCswMappings.get(Metacard.EFFECTIVE);
    }

    public void setEffectiveDateMapping(String effectiveDateMapping) {
        metacardCswMappings.put(Metacard.EFFECTIVE, effectiveDateMapping);
    }

    public String getCreatedDateMapping() {
        return metacardCswMappings.get(Metacard.CREATED);
    }

    public void setCreatedDateMapping(String createdDateMapping) {
        metacardCswMappings.put(Metacard.CREATED, createdDateMapping);
    }

    public String getModifiedDateMapping() {
        return metacardCswMappings.get(Metacard.MODIFIED);
    }

    public void setModifiedDateMapping(String modifiedDateMapping) {
        metacardCswMappings.put(Metacard.MODIFIED, modifiedDateMapping);
    }

    public String getResourceUriMapping() {
        return resourceUriMapping;
    }

    public String getContentTypeMapping() {
        return metacardCswMappings.get(Metacard.CONTENT_TYPE);
    }

    public void setResourceUriMapping(String resourceUriMapping) {
        this.resourceUriMapping = resourceUriMapping;
    }

    public String getThumbnailMapping() {
        return thumbnailMapping;
    }

    public void setThumbnailMapping(String thumbnailMapping) {
        this.thumbnailMapping = thumbnailMapping;
    }

    public boolean getDisableSSLCertVerification() {
        return disableSSLCertVerification;
    }

    public void setDisableSSLCertVerification(boolean disableSSLCertVerification) {
        this.disableSSLCertVerification = disableSSLCertVerification;

    }

    public Map<String, String> getMetacardCswMappings() {
        return metacardCswMappings;
    }

    public void setMetacardCswMappings(Map<String, String> metacardCswMappings) {
        this.metacardCswMappings = metacardCswMappings;
    }

    public void setIsLonLatOrder(boolean isLonLatOrder) {
        this.isLonLatOrder = isLonLatOrder;
    }

    public boolean isLonLatOrder() {
        return this.isLonLatOrder;
    }

    public void setContentTypeMapping(String contentTypeMapping) {
        metacardCswMappings.put(Metacard.CONTENT_TYPE, contentTypeMapping);
    }

    public String getProductRetrievalMethod() {
        return productRetrievalMethod;
    }

    public void setProductRetrievalMethod(String productRetrievalMethod) {
        this.productRetrievalMethod = productRetrievalMethod;
    }

    public Integer getPollIntervalMinutes() {
        return pollIntervalMinutes;
    }

    public void setPollIntervalMinutes(Integer pollIntervalMinutes) {
        this.pollIntervalMinutes = pollIntervalMinutes;
    }

    public String getWcsUrl() {
        return wcsUrl;
    }

    public void setWcsUrl(String wcsUrl) {
        this.wcsUrl = wcsUrl;
    }

    public void setIsCqlForced(boolean isForceCql) {
        this.isCqlForced = isForceCql;
    }

    public boolean isCqlForced() {
        return this.isCqlForced;
    }

    public String getOutputSchema() {
        return outputSchema;
    }

    public void setOutputSchema(String outputSchema) {
        this.outputSchema = outputSchema;
    }
}
