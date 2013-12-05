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

package ddf.catalog.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.catalog.data.ContentType;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.source.Source;
import ddf.catalog.source.SourceMonitor;
import ddf.catalog.source.UnsupportedQueryException;

/**
 * {@link CachedSource} wraps a {@link Source) and caches information about the source
 * @deprecated As of release 2.3.0, replaced by ddf.catalog.util.impl.CachedSource
 */
@Deprecated
public class CachedSource implements Source {

    private static XLogger logger = new XLogger(
            LoggerFactory.getLogger(CachedSource.class));

    private Source source;

    private Set<ContentType> cachedContentTypes;
    private Map<String, String> cachedAttributes;

    private SourceStatus sourceStatus;
    
    private static final String VERSION = "version";
    private static final String ID = "id";
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";
    private static final String ORGANIZATION = "organization";

    public CachedSource(Source source) {
        this.source = source;
        this.sourceStatus = SourceStatus.UNCHECKED;
        clearContentTypes();
        cachedAttributes = new HashMap<String, String>();
    }

    @Override
    public String getVersion() {
        return cachedAttributes.get(VERSION);
    }

    @Override
    public String getId() {
        return cachedAttributes.get(ID);
    }

    @Override
    public String getTitle() {
        return cachedAttributes.get(TITLE);
    }

    @Override
    public String getDescription() {
        return cachedAttributes.get(DESCRIPTION);
    }

    @Override
    public String getOrganization() {
        return cachedAttributes.get(ORGANIZATION);
    }

    @Override
    public boolean isAvailable() {
        return sourceStatus == SourceStatus.AVAILABLE;
    }

    @Override
    public boolean isAvailable(SourceMonitor callback) {
        return isAvailable();
    }

    @Override
    public SourceResponse query(QueryRequest request)
            throws UnsupportedQueryException {
        return source.query(request);
    }

    @Override
    public Set<ContentType> getContentTypes() {
        return cachedContentTypes;
    }

    /**
     * Returns the known SourceStatus of this Cached Source. The value
     * represents the cached status of the last check for the source. It can
     * either be available, unavailable, or never been tested (unchecked).
     * 
     * @return The SourceStatus for this Source
     */
    public SourceStatus getSourceStatus() {
        return sourceStatus;
    }

    /**
     * Updates the cached Source with the current status of the wrapped source.
     * The wrapped source is tested to be available, and if it is available, the
     * contentTypes will also be cached.
     */
    public void checkStatus() {
        try {
            logger.debug("Checking Source [{}] with id [{}] availability.",
                    source, source.getId());

            if (source.isAvailable()) {
                logger.debug("Source [{}] with id [{}] is available.  "
                        + "Updating cached values.", source, source.getId());
                setContentTypes(source.getContentTypes());
                setId(source.getId());
                setTitle(source.getTitle());
                setOrganization(source.getOrganization());
                setDescription(source.getDescription());
                setVersion(source.getVersion());
                setSourceStatus(SourceStatus.AVAILABLE);
            } else {
                logger.debug("Source [{}] with id [{}] is not available.  "
                        + "Clearing cached values", source, source.getId());
                setSourceStatus(SourceStatus.UNAVAILABLE);
                clearContentTypes();
            }
        } catch (Exception e) {
            logger.debug(
                    "Failed to check Source [{}] with id [{}]] availability.  "
                            + "Clearing cached values.", source, source.getId());
            setSourceStatus(SourceStatus.UNAVAILABLE);
            clearContentTypes();

        }
    }
    
    /**
     * Returns the value of an arbitrary attribute
     * 
     * @param attr The attribute value requested
     * @return The attribute Value
     */
    public String getAttributeValue(String attr) {
        return cachedAttributes.get(attr);
    }

    private void setContentTypes(Set<ContentType> contentTypes) {
        this.cachedContentTypes = contentTypes;
    }

    private void clearContentTypes() {
        this.cachedContentTypes = Collections.<ContentType> emptySet();
    }

    private void setSourceStatus(SourceStatus sourceStatus) {
        this.sourceStatus = sourceStatus;
    }

    private void setVersion(String version) {
        cachedAttributes.put(VERSION, version);
    }
    
    private void setId(String id) {
        cachedAttributes.put(ID, id);
    }

    private void setTitle(String title) {
        cachedAttributes.put(TITLE, title);
    }
   
    private void setDescription(String description) {
        cachedAttributes.put(DESCRIPTION, description);
    }
    
    private void setOrganization(String organization) {
        cachedAttributes.put(ORGANIZATION, organization);
    }
}
