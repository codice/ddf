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
package org.codice.ddf.spatial.ogc.wfs.catalog.mapper;
/**
 * Maps Metacards to WFS Features. 
 *
 */
public interface MetacardMapper {
    /**
     * Gets the feature property for the give metacard attribute
     */
    String getFeatureProperty(String metacardAttribute);
    
    /**
     * Gets the metacard attribute for the given feature property
     */
    String getMetacardAttribute(String featureProperty);
    
    /**
     * Gets the feature type for this metacard mapper
     */
    String getFeatureType();
    
    /**
     * Gets the feature property used to sort temporally
     */
    String getSortByTemporalFeatureProperty();
    
    /**
     * Gets the feature property used to when sorting by relevance
     */
    String getSortByRelevanceFeatureProperty();
    
    /**
     * Gets the feature property used to when sorting by distance
     */
    String getSortByDistanceFeatureProperty();
}
