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

import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsFeatureCollection;
import org.codice.ddf.spatial.ogc.wfs.catalog.converter.impl.EnhancedStaxDriver;
import org.codice.ddf.spatial.ogc.wfs.catalog.converter.impl.GenericFeatureConverter;
import org.codice.ddf.spatial.ogc.wfs.catalog.converter.impl.GmlEnvelopeConverter;
import org.codice.ddf.spatial.ogc.wfs.catalog.converter.impl.GmlGeometryConverter;
import org.codice.ddf.spatial.ogc.wfs.v1_0_0.catalog.converter.impl.FeatureCollectionConverterWfs10;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.naming.NoNameCoder;

@Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
@Provider
public class FeatureCollectionMessageBodyWriter implements MessageBodyWriter<WfsFeatureCollection> {

    private XStream xstream;

    private FeatureCollectionConverterWfs10 featureCollectionConverter;

    public FeatureCollectionMessageBodyWriter(SystemBaseUrl sbu) {
        xstream = new XStream(new EnhancedStaxDriver(new NoNameCoder()));
        xstream.setClassLoader(xstream.getClass()
                .getClassLoader());

        featureCollectionConverter = new FeatureCollectionConverterWfs10();
        featureCollectionConverter.setContextRoot(sbu.getRootContext());
        xstream.registerConverter(featureCollectionConverter);
        xstream.registerConverter(new GenericFeatureConverter());

        xstream.registerConverter(new GmlGeometryConverter());
        xstream.registerConverter(new GmlEnvelopeConverter());
        xstream.alias("wfs:FeatureCollection", WfsFeatureCollection.class);
    }

    @Override
    public long getSize(WfsFeatureCollection featureCollection, Class<?> clazz, Type type,
            Annotation[] annotations, MediaType mediaType) {
        // Returning -1 means we cannot determine the length prior to writing
        // the object.
        return -1;
    }

    @Override
    public boolean isWriteable(Class<?> clazz, Type type, Annotation[] annotations,
            MediaType mediaType) {
        return WfsFeatureCollection.class.isAssignableFrom(clazz);
    }

    @Override
    public void writeTo(WfsFeatureCollection featureCollection, Class<?> clazz, Type type,
            Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> headers,
            OutputStream outStream) throws IOException, WebApplicationException {
        xstream.toXML(featureCollection, outStream);
    }

}
