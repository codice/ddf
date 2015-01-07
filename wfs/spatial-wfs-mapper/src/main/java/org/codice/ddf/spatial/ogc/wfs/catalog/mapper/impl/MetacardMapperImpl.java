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
package org.codice.ddf.spatial.ogc.wfs.catalog.mapper.impl;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.codice.ddf.spatial.ogc.wfs.catalog.mapper.MetacardMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;

/**
 *  Maps Metacard Attributes to WFS Feature Properties. 
 *
 */
public class MetacardMapperImpl implements MetacardMapper {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(MetacardMapperImpl.class);
    
    private String featureType;
    
    private BidiMap<String, String> bidiMap;
    
    private String dataUnit;
    
    private String sortByTemporalFeatureProperty;
    
    private String sortByRelevanceFeatureProperty;
    
    private String sortByDistanceFeatureProperty;
    
 
    public MetacardMapperImpl() {
        LOGGER.debug("Creating {}", MetacardMapperImpl.class.getName());
        bidiMap = new DualHashBidiMap<String, String>();
    }

    @Override
    public String getFeatureProperty(String metacardAttribute) {
        return bidiMap.get(metacardAttribute);
    }
    
    @Override
    public String getMetacardAttribute(String featureProperty) {
        return bidiMap.inverseBidiMap().get(featureProperty);
    }
    
    public void setFeatureType(String featureType) {
        LOGGER.debug("Setting feature type to: {}", featureType);
        this.featureType = featureType;
    }
    
    public String getFeatureType() {
        return this.featureType;
    }
    
    public void setSortByTemporalFeatureProperty(String temporalFeatureProperty) {
        LOGGER.debug("Setting sortByTemporalFeatureProperty to: {}", temporalFeatureProperty);
        this.sortByTemporalFeatureProperty = temporalFeatureProperty;
    }
    
    @Override
    public String getSortByTemporalFeatureProperty() {
        return this.sortByTemporalFeatureProperty;
    }
    
    public void setSortByRelevanceFeatureProperty(String relevanceFeatureProperty) {
        LOGGER.debug("Setting sortByRelevanceFeatureProperty to: {}", relevanceFeatureProperty);
        this.sortByRelevanceFeatureProperty = relevanceFeatureProperty;
    }
    
    @Override
    public String getSortByRelevanceFeatureProperty() {
        return this.sortByRelevanceFeatureProperty;
    }
    
    public void setSortByDistanceFeatureProperty(String distanceFeatureProperty) {
        LOGGER.debug("Setting sortByDistanceFeatureProperty to: {}", distanceFeatureProperty);
        this.sortByDistanceFeatureProperty = distanceFeatureProperty;
    }
    
    @Override
    public String getSortByDistanceFeatureProperty() {
        return this.sortByDistanceFeatureProperty;
    }
    
    public void setTitleMapping(String featureProperty) {
        LOGGER.debug("Setting title mapping to: {}", featureProperty);
        bidiMap.put(Metacard.TITLE, featureProperty);
    }
    
    public void setCreatedDateMapping(String featureProperty) {
        LOGGER.debug("Setting created date mapping to: {}", featureProperty);
        bidiMap.put(Metacard.CREATED, featureProperty);
    }
    
    public void setModifiedDateMapping(String featureProperty) {
        LOGGER.debug("Setting modified date mapping to: {}", featureProperty);
        bidiMap.put(Metacard.MODIFIED, featureProperty);
    }
    
    public void setEffectiveDateMapping(String featureProperty) {
        LOGGER.debug("Setting effective date mapping to: {}", featureProperty);
        bidiMap.put(Metacard.EFFECTIVE, featureProperty);
    }
    
    public void setExpirationDateMapping(String featureProperty) {
        LOGGER.debug("Setting expiration date mapping to: {}", featureProperty);
        bidiMap.put(Metacard.EXPIRATION, featureProperty);
    }
    
    public void setResourceUriMapping(String featureProperty) {
        LOGGER.debug("Setting resource uri mapping to: {}", featureProperty);
        bidiMap.put(Metacard.RESOURCE_URI, featureProperty);
    }
    
    public void setResourceSizeMapping(String featureProperty) {
        LOGGER.debug("Setting resource size mapping to: {}", featureProperty);
        bidiMap.put(Metacard.RESOURCE_SIZE, featureProperty);
    }

    public void setDataUnit(String unit) {
        LOGGER.debug("Setting data unit to: {}", unit);
        dataUnit = unit;
      }  
    
    public String getDataUnit() {
        return dataUnit;
    }
    
    public void setGeographyMapping(String featureProperty) {
        LOGGER.debug("Setting geography mapping to: {}", featureProperty);
        bidiMap.put(Metacard.GEOGRAPHY, featureProperty);
    }
    
    public void setThumbnailMapping(String featureProperty) {
        LOGGER.debug("Setting thumbnail mapping to: {}", featureProperty);
        bidiMap.put(Metacard.THUMBNAIL, featureProperty);
    }
}
