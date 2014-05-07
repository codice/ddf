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
package ddf.catalog.transformer.xml;

import javax.xml.bind.JAXBContext;

import net.opengis.gml.v_3_1_1.AbstractGeometryType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.transformer.xml.adapter.AdaptedMetacard;
import ddf.catalog.transformer.xml.binding.MetacardElement;

public abstract class AbstractXmlTransformer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractXmlTransformer.class);

    protected static final JAXBContext CONTEXT = initContext();

    public static final String TEXT_XML = "text/xml";

    protected AbstractXmlTransformer() {
        super();
    }

    private static JAXBContext initContext() {
        JAXBContext context = null;
        try {
            String contextPath = MetacardElement.class.getPackage().getName() + ":"
                    + AdaptedMetacard.class.getPackage().getName() + ":"
                    + AbstractGeometryType.class.getPackage().getName();
            context = JAXBContext.newInstance(contextPath,
                    AbstractXmlTransformer.class.getClassLoader());
        } catch (Exception e) {
            LOGGER.error("JAXB Context could not be created.", e);
        }
        return context;
    }

}