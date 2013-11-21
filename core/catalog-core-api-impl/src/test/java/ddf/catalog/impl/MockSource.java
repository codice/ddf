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
package ddf.catalog;

import java.io.Serializable;
import java.net.URI;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.ContentTypeImpl;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.Source;
import ddf.catalog.source.SourceMonitor;

public class MockSource implements FederatedSource {

    private String description;

    private String organization;

    private String shortName;

    private String title;

    private String version;

    private Set<ContentType> contentTypes;

    private boolean isAvailable;

    private Date lastAvailability;

    /**
     * 
     * @param shortName
     * @param title
     * @param version
     * @param organization
     * @param catalogTypes
     * @param isAvailable
     * @param lastAvailability
     */
    public MockSource(String shortName, String title, String version, String organization,
            Set<ContentType> catalogTypes, boolean isAvailable, Date lastAvailability) {
        this.shortName = shortName;
        this.title = title;
        this.version = version;
        this.organization = organization;
        this.contentTypes = catalogTypes;
        this.isAvailable = isAvailable;
        this.lastAvailability = lastAvailability;
        contentTypes = new HashSet<ContentType>();
        contentTypes.add(new ContentTypeImpl("data", "version1"));
    }

    public Date getLastAvailabilityDate() {
        return lastAvailability;
    }

    @Override
    public boolean isAvailable() {
        return isAvailable;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getOrganization() {
        return organization;
    }

    @Override
    public String getId() {
        return shortName;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public SourceResponse query(QueryRequest query) {
        return null;
    }

    // @Override
    // public BlockingQueue<Response<Metacard>> read( Subject user, List<String> ids ) throws
    // CatalogException
    // {
    // return null;
    // }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else {
            if ((obj instanceof Source) && ((Source) obj).getId().equals(this.shortName)) {
                return true;
            }
        }
        // if nothing passed, return false
        return false;
    }

    @Override
    public int hashCode() {
        return this.shortName.hashCode();
    }

    @Override
    public ResourceResponse retrieveResource(URI uri, Map<String, Serializable> requestProperties)
        throws ResourceNotFoundException, ResourceNotSupportedException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<ContentType> getContentTypes() {
        return contentTypes;

    }

    @Override
    public boolean isAvailable(SourceMonitor callback) {
        return isAvailable();
    }

    @Override
    public Set<String> getSupportedSchemes() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<String> getOptions(Metacard metacard) {
        // TODO Auto-generated method stub
        return null;
    }

}
