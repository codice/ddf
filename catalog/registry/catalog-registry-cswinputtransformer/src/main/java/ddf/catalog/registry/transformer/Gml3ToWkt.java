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
package ddf.catalog.registry.transformer;

import java.io.InputStream;
import java.util.Collections;

import javax.xml.bind.JAXBElement;

import org.apache.commons.io.IOUtils;
import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.ParserConfigurator;
import org.codice.ddf.parser.ParserException;
import org.jvnet.jaxb2_commons.locator.DefaultRootObjectLocator;
import org.jvnet.ogc.gml.v_3_1_1.jts.ConversionFailedException;
import org.jvnet.ogc.gml.v_3_1_1.jts.GML311ToJTSGeometryConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTWriter;

import net.opengis.gml.v_3_1_1.AbstractGeometryType;

public class Gml3ToWkt {

    private final Parser parser;

    private final ParserConfigurator configurator;

    private static final Logger LOGGER = LoggerFactory.getLogger(Gml3ToWkt.class);

    public Gml3ToWkt(Parser parser) {
        this.parser = parser;
        this.configurator = parser.configureParser(
                Collections.singletonList(AbstractGeometryType.class.getPackage().getName()),
                Gml3ToWkt.class.getClassLoader());
    }

    public String convert(String xml) {
        return convert(IOUtils.toInputStream(xml));
    }

    @SuppressWarnings("unchecked")
    public String convert(InputStream xml) {
        AbstractGeometryType geometry = null;
        try {
            JAXBElement<AbstractGeometryType> jaxbElement = parser
                    .unmarshal(configurator, JAXBElement.class, xml);
            geometry = jaxbElement.getValue();
            GML311ToJTSGeometryConverter geometryConverter = new GML311ToJTSGeometryConverter();
            Geometry jtsGeo = geometryConverter
                    .createGeometry(new DefaultRootObjectLocator(jaxbElement), geometry);
            WKTWriter wktWriter = new WKTWriter();
            return wktWriter.write(jtsGeo);
        } catch (ParserException e) {
            LOGGER.error("Cannot parse gml: {}", e.getMessage());
        } catch (ConversionFailedException e) {
            LOGGER.error("Cannot convert gml311 geo object {} to jts", geometry);
        }

        return "";
    }

}
