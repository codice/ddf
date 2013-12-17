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
package org.codice.ddf.spatial.kml.transformer;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;

/**
 * Default Implentation of {@link KmlStyleMapEntry}.
 * 
 * @author Keith C Wire
 * 
 */
public class KmlStyleMapEntryImpl implements KmlStyleMapEntry {

    private String attributeName;

    private String attributeValue;

    private String styleUrl;

    private static final Logger LOGGER = LoggerFactory.getLogger(KmlStyleMapEntryImpl.class);

    public KmlStyleMapEntryImpl() {
    }

    // For Unit Testing
    public KmlStyleMapEntryImpl(String name, String value, String url) {
        this.attributeName = name;
        this.attributeValue = value;
        this.styleUrl = url;
    }

    public void init() {
        LOGGER.debug("Creating {} with {}, {}, {}", KmlStyleMapEntryImpl.class.getName(),
                attributeName, attributeValue, styleUrl);
    }

    @Override
    public boolean metacardMatch(Metacard metacard) {
        if (Metacard.SOURCE_ID.equals(attributeName)) {
            return metacard.getSourceId().equals(attributeValue);
        } else {
            Attribute attribute = metacard.getAttribute(attributeName);
            if (attribute != null) {
                if (attributeValueMatch(attribute, metacard.getMetacardType()
                        .getAttributeDescriptor(attributeName))) {
                    LOGGER.debug("Found match for Attribute: {} Value: {} URL: {}", attributeName,
                            attributeValue, styleUrl);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String getAttributeName() {
        return this.attributeName;
    }

    @Override
    public String getAttributeValue() {
        return this.attributeValue;
    }

    @Override
    public String getStyleUrl() {
        return this.styleUrl;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public void setAttributeValue(String attributeValue) {
        this.attributeValue = attributeValue;
    }

    public void setStyleUrl(String styleUrl) {
        this.styleUrl = styleUrl;
    }

    private boolean attributeValueMatch(Attribute attribute, AttributeDescriptor descriptor) {

        switch (descriptor.getType().getAttributeFormat()) {
        case STRING:
        case XML:
        case GEOMETRY:
            return attributeValue.equals(attribute.getValue());
        case BOOLEAN:
            return Boolean.valueOf(attributeValue).equals(attribute.getValue());
        case DATE:
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                String mappedDate = dateFormat.format(dateFormat.parse(attributeValue));
                String metacardDate = dateFormat.format((Date) attribute.getValue());
                return mappedDate.equals(metacardDate);
            } catch (ParseException e) {
                LOGGER.warn("Unable to parese date and perform comparison.", e);
                return false;
            }
        case SHORT:
            return Short.valueOf(attributeValue).equals(attribute.getValue());
        case INTEGER:
            return Integer.valueOf(attributeValue).equals(attribute.getValue());
        case LONG:
            return Long.valueOf(attributeValue).equals(attribute.getValue());
        case FLOAT:
            return Float.valueOf(attributeValue).equals(attribute.getValue());
        case DOUBLE:
            return Double.valueOf(attributeValue).equals(attribute.getValue());
        case BINARY:
        case OBJECT:
        default:
            LOGGER.warn("Unsupported Attribute Format was attempted for KML Style Mapping.");
            return false;
        }

    }

}
