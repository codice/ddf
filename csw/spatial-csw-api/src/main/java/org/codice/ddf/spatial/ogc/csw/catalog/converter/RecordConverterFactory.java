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
package org.codice.ddf.spatial.ogc.csw.catalog.converter;

import java.util.Map;

/**
 * Factory Interface to provide a method for generating unique {@link RecordConverter} instances
 * 
 */
public interface RecordConverterFactory {

    /**
     * Returns an instance of a {@link RecordConverter}
     * 
     * @param metacardAttributeMap Maps metacard attributes to output schema attributes
     * @param productRetrievalMethod Product Retrieval Method (Source URI or WCS)
     * @param resourceUriMapping Output schema field to map to a metacard's resource uri field
     * @param thumbnailMapping Output schema field to map to a metacard's thumbnail URI field
     * @param isLonLatOrder Forces longitude, latitude coordinate ordering
     * 
     * @return {@link RecordConverter}
     */
    public RecordConverter createConverter(Map<String, String> metacardAttributeMap,
            String productRetrievalMethod, String resourceUriMapping, String thumbnailMapping,
            boolean isLonLatOrder);

    /**
     * Returns an instance of a {@link RecordConverter}
     * 
     * @param metacardAttributeMap Maps metacard attributes to output schema attributes
     * @param prefixToUriMapping Maps prefixes to URIs
     * @param productRetrievalMethod Product Retrieval Method (Source URI or WCS)
     * @param resourceUriMapping Output schema field to map to a metacard's resource uri field
     * @param thumbnailMapping  Output schema field to map to a metacard's thumbnail URI field
     * @param isLonLatOrder Forces longitude, latitude coordinate ordering
     * 
     * @return {@link RecordConverter}
     */
    public RecordConverter createConverter(Map<String, String> metacardAttributeMap,
            Map<String, String> prefixToUriMapping, String productRetrievalMethod,
            String resourceUriMapping, String thumbnailMapping, boolean isLonLatOrder);
    
    /**
     * Returns the output schema for this {@link RecordConverterFactory}
     * 
     * @return outputSchema
     */
    public String getOutputSchema();
    
    /**
     * Sets the output schema for this {@link RecordConverterFactory}
     * 
     * @param outputSchema
     */
    public void setOutputSchema(String outputSchema);
}
