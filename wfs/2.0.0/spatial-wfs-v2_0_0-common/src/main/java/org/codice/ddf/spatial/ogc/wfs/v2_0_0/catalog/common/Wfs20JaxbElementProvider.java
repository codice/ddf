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
package org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common;

import java.lang.reflect.Type;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Wfs20JaxbElementProvider<T> extends JAXBElementProvider<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(Wfs20JaxbElementProvider.class);

    private static final JAXBContext jaxbContext = initJaxbContext();

    @Override
    public JAXBContext getJAXBContext(Class<?> type, Type genericType) throws JAXBException {
        return jaxbContext;
    }

    private static JAXBContext initJaxbContext() {
        JAXBContext jaxbContext = null;

        String contextPath = StringUtils.join(new String[] {Wfs20Constants.OGC_FILTER_PACKAGE,
            Wfs20Constants.OGC_GML_PACKAGE, Wfs20Constants.OGC_OWS_PACKAGE,
            Wfs20Constants.OGC_WFS_PACKAGE}, ":");

        try {
            LOGGER.debug("Creating JAXB context with context path: {}.", contextPath);
            jaxbContext = JAXBContext.newInstance(contextPath,
                    Wfs20JaxbElementProvider.class.getClassLoader());
        } catch (JAXBException e) {
            LOGGER.error("Unable to create JAXB context using contextPath: {}.", contextPath, e);
        }

        return jaxbContext;
    }
}
