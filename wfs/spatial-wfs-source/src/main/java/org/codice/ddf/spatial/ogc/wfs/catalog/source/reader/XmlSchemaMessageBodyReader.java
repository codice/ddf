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
package org.codice.ddf.spatial.ogc.wfs.catalog.source.reader;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import javax.xml.XMLConstants;

import org.apache.camel.builder.xml.XPathBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.commons.io.IOUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.codice.ddf.spatial.ogc.wfs.catalog.source.WfsUriResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

@Provider
public class XmlSchemaMessageBodyReader implements MessageBodyReader<XmlSchema> {

    private static final Logger LOGGER = LoggerFactory.getLogger(XmlSchemaMessageBodyReader.class);

    private static final String XSD_PREFIX = "xsd";

    private static final String COUNT_XPATH = "count(/" + XSD_PREFIX + ":schema)";

    private static final XPathBuilder countXPathBuilder = new XPathBuilder(COUNT_XPATH).namespace(
            XSD_PREFIX, XMLConstants.W3C_XML_SCHEMA_NS_URI).numberResult();

    private DefaultCamelContext camelContext = new DefaultCamelContext();

    private WfsUriResolver wfsUriResolver = new WfsUriResolver();

    @Override
    public boolean isReadable(Class<?> clazz, Type type, Annotation[] annotations,
            MediaType mediaType) {
        return XmlSchema.class.isAssignableFrom(clazz);
    }

    @Override
    public XmlSchema readFrom(Class<XmlSchema> clazz, Type type, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, String> headers, InputStream inStream)
        throws IOException, WebApplicationException {
        // Determine if this is an XMLSchema
        String input = IOUtils.toString(inStream);
        inStream.reset();
        String count = countXPathBuilder.evaluate(camelContext, input);
        // See if there exactly one instance of "xsd:schema" in this doc
        if (Integer.valueOf(count) == 1) {
            XmlSchema schema = null;
            XmlSchemaCollection schemaCollection = new XmlSchemaCollection();
            schemaCollection.init();
            schemaCollection.setSchemaResolver(wfsUriResolver);
            schema = schemaCollection.read(new InputSource(inStream));
            return schema;
        }
        LOGGER.warn("Did not receive valid XML Schema, instead got: \n{}", input);
        return null;
    }
}
