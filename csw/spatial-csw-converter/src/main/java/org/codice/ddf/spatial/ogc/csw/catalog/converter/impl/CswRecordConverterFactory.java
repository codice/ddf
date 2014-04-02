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
package org.codice.ddf.spatial.ogc.csw.catalog.converter.impl;

import java.util.Map;

import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordMetacardType;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.RecordConverter;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.RecordConverterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CswRecordConverterFactory implements RecordConverterFactory {

    private String outputSchema = CswConstants.CSW_OUTPUT_SCHEMA;
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CswRecordConverterFactory.class);
    
    public CswRecordConverterFactory() {
    }
    
    @Override
    public RecordConverter createConverter(Map<String, String> metacardAttributeMap,
            String productRetrievalMethod, String resourceUriMapping, String thumbnailMapping,
            boolean isLonLatOrder) {
        return createConverter(metacardAttributeMap, null, productRetrievalMethod,
                resourceUriMapping, thumbnailMapping, isLonLatOrder);
    }

    @Override
    public RecordConverter createConverter(Map<String, String> metacardAttributeMap,
            Map<String, String> prefixToUriMapping, String productRetrievalMethod,
            String resourceUriMapping, String thumbnailMapping, boolean isLonLatOrder) {
        RecordConverter recordConverter = new CswRecordConverter(metacardAttributeMap,
                prefixToUriMapping, productRetrievalMethod, resourceUriMapping, thumbnailMapping,
                isLonLatOrder);
        recordConverter.setMetacardType(new CswRecordMetacardType());
        return recordConverter;
    }
    
    @Override
    public String getOutputSchema() {
        return this.outputSchema;
    }
    
    @Override
    public void setOutputSchema(String schema) {
        LOGGER.debug("Setting output schema to: {}.", schema);
        this.outputSchema = schema;
    }

}
