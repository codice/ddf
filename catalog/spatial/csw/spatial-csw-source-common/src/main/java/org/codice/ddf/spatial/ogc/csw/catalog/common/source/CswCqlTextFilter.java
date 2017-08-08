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
 **/
package org.codice.ddf.spatial.ogc.csw.catalog.common.source;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;

import org.codice.ddf.spatial.ogc.csw.catalog.common.ECQLMapperVisitor;
import org.geotools.filter.FilterFactoryImpl;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.filter.v1_1.OGCConfiguration;
import org.geotools.xml.Parser;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import ddf.catalog.source.UnsupportedQueryException;
import net.opengis.filter.v_1_1_0.FilterType;

/**
 * CswCqlTextFilter converts a {@link FilterType} to the equivalent CQL Text.
 */
public final class CswCqlTextFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CswCqlTextFilter.class);

    private static final JAXBContext JAXB_CONTEXT = initJaxbContext();

    private static CswCqlTextFilter instance;

    private CswCqlTextFilter() {
    }

    public static CswCqlTextFilter getInstance() {
        if (instance == null) {
            instance = new CswCqlTextFilter();
        }
        return instance;
    }

    private static JAXBContext initJaxbContext() {

        JAXBContext jaxbContext = null;

        try {
            jaxbContext = JAXBContext.newInstance(
                    "net.opengis.filter.v_1_1_0:net.opengis.gml.v_3_1_1:net.opengis.ows.v_1_0_0",
                    AbstractCswSource.class.getClassLoader());
        } catch (JAXBException e) {
            LOGGER.info("Failed to initialize JAXBContext", e);
        }

        return jaxbContext;
    }

    public String getCqlText(FilterType filterType) throws UnsupportedQueryException {
        Parser parser = new Parser(new OGCConfiguration());
        try {
            StringReader reader = new StringReader(marshalFilterType(filterType));
            Object parsedFilter = parser.parse(reader);
            if (parsedFilter instanceof Filter) {
                Filter filter = (Filter) parsedFilter;
                Filter filterToCql = (Filter) filter.accept(new ECQLMapperVisitor(),
                        new FilterFactoryImpl());
                LOGGER.debug("Filter to Convert to CQL => {}", filterToCql);
                String cql = ECQL.toCQL(filterToCql);
                LOGGER.debug("Generated CQL from Filter => {}", cql);
                return cql;
            } else {
                throw new UnsupportedQueryException("Query did not produce a valid filter.");
            }
        } catch (IOException | SAXException | ParserConfigurationException | JAXBException e) {
            throw new UnsupportedQueryException("Unable to create CQL Filter.", e);
        }
    }

    private String marshalFilterType(FilterType filterType) throws JAXBException {
        Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
        JAXBElement<FilterType> filterTypeJaxbElement = new JAXBElement<>(new QName(
                "http://www.opengis.net/ogc",
                "Filter"), FilterType.class, filterType);
        StringWriter writer = new StringWriter();
        marshaller.marshal(filterTypeJaxbElement, writer);
        LOGGER.debug("Filter as XML => {}", writer.toString());
        return writer.toString();
    }
}
