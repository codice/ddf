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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.apache.commons.io.IOUtils;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsFeatureCollection;
import org.codice.ddf.spatial.ogc.wfs.catalog.converter.FeatureConverter;
import org.codice.ddf.spatial.ogc.wfs.catalog.converter.impl.FeatureCollectionConverter;
import org.codice.ddf.spatial.ogc.wfs.catalog.converter.impl.GmlEnvelopeConverter;
import org.codice.ddf.spatial.ogc.wfs.catalog.converter.impl.GmlGeometryConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.io.xml.WstxDriver;

import ddf.catalog.data.Metacard;

@Consumes({ MediaType.TEXT_XML, MediaType.APPLICATION_XML })
@Provider
public class FeatureCollectionMessageBodyReader implements MessageBodyReader<WfsFeatureCollection> {

    protected XStream xstream;

    protected FeatureCollectionConverter featureCollectionConverter;

    protected Map<String, FeatureConverter> featureConverterMap = new HashMap<String, FeatureConverter>();

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureCollectionMessageBodyReader.class);

    public FeatureCollectionMessageBodyReader() {
        xstream = new XStream(new WstxDriver());
        xstream.setClassLoader(this.getClass().getClassLoader());
        xstream.registerConverter(new GmlGeometryConverter());
        xstream.registerConverter(new GmlEnvelopeConverter());
        xstream.alias("FeatureCollection", WfsFeatureCollection.class);
    }

    @Override
    public boolean isReadable(Class<?> clazz, Type type, Annotation[] annotations,
            MediaType mediaType) {
        return WfsFeatureCollection.class.isAssignableFrom(clazz);
    }

    @Override
    public WfsFeatureCollection readFrom(Class<WfsFeatureCollection> clazz, Type type,
            Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> headers,
            InputStream inStream) throws IOException, WebApplicationException {

        // Save original input stream for any exception message that might need to be
        // created
        String originalInputStream = IOUtils.toString(inStream, "UTF-8");
        
        // Re-create the input stream (since it has already been read for potential
        // exception message creation)
        inStream = new ByteArrayInputStream(originalInputStream.getBytes("UTF-8"));

        WfsFeatureCollection featureCollection = null;

        try {
            featureCollection = (WfsFeatureCollection) xstream.fromXML(inStream);
        } catch (XStreamException e) {
            // If a ServiceExceptionReport is sent from the remote WFS site it will be sent with an
            // JAX-RS "OK" status, hence the ErrorResponse exception mapper will not fire.
            // Instead the ServiceExceptionReport will come here and be treated like a GetFeature
            // response, resulting in an XStreamException since ExceptionReport cannot be
            // unmarshalled. So this catch clause is responsible for catching that XStream
            // exception and creating a JAX-RS response containing the original stream
            // (with the ExceptionReport) and rethrowing it as a WebApplicationException,
            // which CXF will wrap as a ClientException that the WfsSource catches, converts
            // to a WfsException, and logs.
            LOGGER.error("Exception unmarshalling {}", e);
            ByteArrayInputStream bis = new ByteArrayInputStream(originalInputStream.getBytes());
            ResponseBuilder responseBuilder = Response.ok(bis);
            responseBuilder.type("text/xml");
            Response response = responseBuilder.build();
            throw new WebApplicationException(e, response);
        } finally {
            IOUtils.closeQuietly(inStream);
        }

        return featureCollection;
    }

    public void registerConverter(FeatureConverter converter) {
        featureConverterMap.put(converter.getMetacardType().getName(), converter);
        xstream.registerConverter(converter);
        xstream.alias(converter.getMetacardType().getName(), Metacard.class);
    }

}
