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
package org.codice.ddf.spatial.ogc.wfs.catalog.endpoint.writer;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.apache.ws.commons.schema.XmlSchema;

/**
 * Custom MessageBodyWriter for the {@link XmlSchema} class. By default CXF will try to marhsal
 * objects via JAXB when the {@link MediaType} contains "xml". This provider will write an
 * {@link XmlSchema} to the message payload of the response.
 * 
 */
@Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
@Provider
public class XmlSchemaMessageBodyWriter implements MessageBodyWriter<XmlSchema> {

    public XmlSchemaMessageBodyWriter() {
    }

    @Override
    public long getSize(XmlSchema schema, Class<?> className, Type type, Annotation[] annotations,
            MediaType mediaType) {
        // Returning -1 means we cannot determine the length prior to writing
        // the object.
        return -1;
    }

    @Override
    public boolean isWriteable(Class<?> className, Type type, Annotation[] annotations,
            MediaType mediaType) {
        return XmlSchema.class.isAssignableFrom(className);
    }

    @Override
    public void writeTo(XmlSchema schema, Class<?> className, Type type, Annotation[] annotations,
            MediaType mediaType, MultivaluedMap<String, Object> headers, OutputStream os)
        throws IOException, WebApplicationException {
        schema.write(os);
    }

}
