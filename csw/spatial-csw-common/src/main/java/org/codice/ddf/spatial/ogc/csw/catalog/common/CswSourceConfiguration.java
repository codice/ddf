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
import java.util.Map.Entry;

import ddf.catalog.data.Metacard;

/**
 * Domain object to encapsulate the configuration of an instance of a {@link CswSource}. CSW
 * converters, readers, etc. will access this object to determine the latest configuration of the
 * {@link CswSource} they are working on.
 * 
 */
public class CswSourceConfiguration {

    private String cswUrl;

    private String id;

    private String username;

    private String password;

    private boolean disableCnCheck = false;

    private Map<String, String> metacardCswMappings = new HashMap<String, String>();

    private String resourceUriMapping;

    private String thumbnailMapping;

    private boolean isLonLatOrder;
    
    private boolean usePosList;

    private Integer pollIntervalMinutes;

    private Integer connectionTimeout;

    private Integer receiveTimeout;

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

    public boolean getDisableCnCheck() {
        return disableCnCheck;
    }

    public void setDisableCnCheck(boolean disableCnCheck) {
        this.disableCnCheck = disableCnCheck;

    }

    public Map<String, String> getMetacardCswMappings() {
        Map<String, String> newMap = new HashMap<>();
        for (Entry<String, String> entry : metacardCswMappings.entrySet()) {
            newMap.put(entry.getValue(), entry.getKey());
        }
        return newMap;
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
    
    public boolean isSetUsePosList() {
        return usePosList;
    }
    
    public void setUsePosList(boolean usePosList) {
        this.usePosList = usePosList;
    }

    public void setContentTypeMapping(String contentTypeMapping) {
        metacardCswMappings.put(Metacard.CONTENT_TYPE, contentTypeMapping);
    }

    public Integer getPollIntervalMinutes() {
        return pollIntervalMinutes;
    }

    public void setPollIntervalMinutes(Integer pollIntervalMinutes) {
        this.pollIntervalMinutes = pollIntervalMinutes;
    }

    public Integer getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(Integer connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public Integer getReceiveTimeout() {
        return receiveTimeout;
    }

    public void setReceiveTimeout(Integer receiveTimeout) {
        this.receiveTimeout= receiveTimeout;
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
