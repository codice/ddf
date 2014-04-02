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
package org.codice.ddf.spatial.ogc.csw.catalog.common;

import java.lang.reflect.Type;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Extends and overrides the JAXBElementProvider so that it is contextually aware of the
 * package names used in the services.
 *
 * This can be mapped in blueprint
 * by creating the bean with this class and mapping it as a jaxrs:providers element to
 * the service jaxrs:server
 *
 * @param <T> generic type
 */
public class CswJAXBElementProvider<T> extends JAXBElementProvider<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CswJAXBElementProvider.class);

    private static final JAXBContext jaxbContext = initJaxbContext();

    @Override
    public JAXBContext getJAXBContext(Class<?> type, Type genericType) throws JAXBException {
        return jaxbContext;
    }

    private static JAXBContext initJaxbContext() {
        JAXBContext jaxbContext = null;

        // JAXB context path
        // "net.opengis.cat.csw.v_2_0_2:net.opengis.filter.v_1_1_0:net.opengis.gml.v_3_1_1:net.opengis.ows.v_1_0_0"
        String contextPath = StringUtils.join(new String[] {CswConstants.OGC_CSW_PACKAGE,
            CswConstants.OGC_FILTER_PACKAGE, CswConstants.OGC_GML_PACKAGE,
            CswConstants.OGC_OWS_PACKAGE}, ":");

        try {
            LOGGER.debug("Creating JAXB context with context path: {}.", contextPath);
            jaxbContext = JAXBContext.newInstance(contextPath,
                    CswJAXBElementProvider.class.getClassLoader());
        } catch (JAXBException e) {
            LOGGER.error("Unable to create JAXB context using contextPath: {}.", contextPath, e);
        }

        return jaxbContext;
    }
}
