/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.transformer.xml.streaming.impl;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.codice.ddf.transformer.xml.streaming.SaxEventHandler;
import org.codice.ddf.transformer.xml.streaming.SaxEventHandlerFactory;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;

/**
 * {@inheritDoc}
 */
public class XmlSaxEventHandlerFactoryImpl implements SaxEventHandlerFactory {

    private static final String VERSION = "1.0";

    private static final String ID = "xml-handler";

    private static final String TITLE = "XMLSaxEventHandler Factory";

    private static final String DESCRIPTION =
            "Factory that returns a SaxEventHandler to help parse XML Metacards";

    private static final String ORGANIZATION = "Codice";

    private static Set<AttributeDescriptor> attributeDescriptors = new HashSet<>();

    private Map<String, String> xmlToMetacardMapping;

    static {
        attributeDescriptors.add(new AttributeDescriptorImpl(Metacard.ID,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BasicTypes.STRING_TYPE));
        attributeDescriptors.add(new AttributeDescriptorImpl(Metacard.TITLE,
                true /* indexed */,
                true /* stored */,
                true /* tokenized */,
                false /* multivalued */,
                BasicTypes.STRING_TYPE));
        attributeDescriptors.add(new AttributeDescriptorImpl(Metacard.POINT_OF_CONTACT,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BasicTypes.STRING_TYPE));
        attributeDescriptors.add(new AttributeDescriptorImpl(Metacard.METADATA,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BasicTypes.XML_TYPE));
        attributeDescriptors.add(new AttributeDescriptorImpl(Metacard.DESCRIPTION,
                true /* indexed */,
                true /* stored */,
                false /* tokenized */,
                false /* multivalued */,
                BasicTypes.STRING_TYPE));
    }

    @Override
    public SaxEventHandler getNewSaxEventHandler() {
        if (xmlToMetacardMapping != null) {
            return new XmlSaxEventHandlerImpl(xmlToMetacardMapping);
        } else {
            return new XmlSaxEventHandlerImpl();
        }
    }

    @Override
    public Set<AttributeDescriptor> getSupportedAttributeDescriptors() {
        return attributeDescriptors;
    }

    @Override
    public String getVersion() {
        return VERSION;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public String getOrganization() {
        return ORGANIZATION;
    }

    public void setXmlToMetacardMapping(Map<String, String> mapping) {
        this.xmlToMetacardMapping = mapping;
    }

    public Map<String, String> getXmlToMetacardMapping() {
        return this.xmlToMetacardMapping;
    }
}
