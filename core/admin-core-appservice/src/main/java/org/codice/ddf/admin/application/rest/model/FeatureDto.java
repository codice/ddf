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
package org.codice.ddf.admin.application.rest.model;

import org.apache.karaf.features.Feature;

/**
 * 
 * Feature DTO to support DDF Web Console with additional repository and status information.  
 * Wrapper class for Karaf Feature object.
 *
 */
public class FeatureDto {
    
    private String status;
    private String repository;
    private String name;
    private String version;
    private String id;
    private String install;
    private String description;
    private String details;
    private String resolver;
    
    public FeatureDto(Feature feature, String status, String repository){
        this.name = feature.getName();
        this.id = feature.getId();
        this.version = feature.getVersion();
        this.install = feature.getInstall();
        this.description = feature.getDescription();
        this.details = feature.getDetails();
        this.resolver = feature.getResolver();
        this.status = status;
        this.repository = repository;
    }

    public String getStatus() {
        return status;
    }

    public String getRepository() {
        return repository;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getId() {
        return id;
    }

    public String getInstall() {
        return install;
    }

    public String getDescription() {
        return description;
    }

    public String getDetails() {
        return details;
    }

    public String getResolver() {
        return resolver;
    }


}
