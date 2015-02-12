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
package org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.source.reader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.math.BigInteger;
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
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import net.opengis.wfs.v_2_0_0.FeatureCollectionType;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.Wfs20Constants;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.Wfs20FeatureCollection;
import org.codice.ddf.spatial.ogc.wfs.catalog.converter.FeatureConverter;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.converter.impl.FeatureCollectionConverterWfs20;
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
public class FeatureCollectionMessageBodyReaderWfs20 implements MessageBodyReader<Wfs20FeatureCollection> {

    protected XStream xstream;

    protected FeatureCollectionConverterWfs20 featureCollectionConverter;

    protected Map<String, FeatureConverter> featureConverterMap = new HashMap<String, FeatureConverter>();
    
    private static final JAXBContext JAXB_CONTEXT = initJaxbContext();

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureCollectionMessageBodyReaderWfs20.class);

    public FeatureCollectionMessageBodyReaderWfs20() {
        xstream = new XStream(new WstxDriver());
        xstream.setClassLoader(this.getClass().getClassLoader());
        xstream.registerConverter(new GmlGeometryConverter());
        xstream.registerConverter(new GmlEnvelopeConverter());
        featureCollectionConverter = new FeatureCollectionConverterWfs20();
        featureCollectionConverter.setFeatureConverterMap(featureConverterMap);
        xstream.registerConverter(featureCollectionConverter);
        xstream.alias("FeatureCollection", Wfs20FeatureCollection.class);
    }

    @Override
    public boolean isReadable(Class<?> clazz, Type type, Annotation[] annotations,
            MediaType mediaType) {
        if(!Wfs20FeatureCollection.class.isAssignableFrom(clazz)) {
            LOGGER.warn("{} class is not readable.", clazz);
        }
        return Wfs20FeatureCollection.class.isAssignableFrom(clazz);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Wfs20FeatureCollection readFrom(Class<Wfs20FeatureCollection> clazz, Type type,
            Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> headers,
            InputStream inStream) throws IOException, WebApplicationException {

        // Save original input stream for any exception message that might need to be
        // created and additional attributes
        String originalInputStream = IOUtils.toString(inStream, "UTF-8");
        LOGGER.debug("{}", originalInputStream);
        //Fetch FeatureCollection attributes
        Unmarshaller unmarshaller = null;
        JAXBElement<FeatureCollectionType> wfsFeatureCollectionType = null;
        try {
            unmarshaller = JAXB_CONTEXT.createUnmarshaller();
            Reader reader = new StringReader(originalInputStream);
            wfsFeatureCollectionType = (JAXBElement<FeatureCollectionType>) unmarshaller
                    .unmarshal(reader);
        } catch (ClassCastException e1) {
            LOGGER.warn("Exception unmarshalling {}, could be an OWS Exception Report from server.", e1.getMessage());
            
            // If an ExceptionReport is sent from the remote WFS site it will be sent with an
            // JAX-RS "OK" status, hence the ErrorResponse exception mapper will not fire.
            // Instead the ServiceExceptionReport will come here and be treated like a GetFeature
            // response, resulting in an XStreamException since ExceptionReport cannot be
            // unmarshalled. So this catch clause is responsible for catching that XStream
            // exception and creating a JAX-RS response containing the original stream
            // (with the ExceptionReport) and rethrowing it as a WebApplicationException,
            // which CXF will wrap as a ClientException that the WfsSource catches, converts
            // to a WfsException, and logs.

            ByteArrayInputStream bis = new ByteArrayInputStream(originalInputStream.getBytes());
            ResponseBuilder responseBuilder = Response.ok(bis);
            responseBuilder.type("text/xml");
            Response response = responseBuilder.build();
            throw new WebApplicationException(e1, response);
        }
        catch (JAXBException e1) {
            LOGGER.error("Error in retrieving feature collection.", e1);
        }

        Wfs20FeatureCollection featureCollection = null;

        if(null != wfsFeatureCollectionType && null != wfsFeatureCollectionType.getValue()) {
            BigInteger numberReturned = wfsFeatureCollectionType.getValue().getNumberReturned();
            String numberMatched = wfsFeatureCollectionType.getValue().getNumberMatched();

            // Re-create the input stream (since it has already been read for potential
            // exception message creation)
            inStream = new ByteArrayInputStream(originalInputStream.getBytes("UTF-8"));

            try {
                featureCollection = (Wfs20FeatureCollection) xstream.fromXML(inStream);
                featureCollection.setNumberMatched(numberMatched);
                featureCollection.setNumberReturned(numberReturned);

            } catch (XStreamException e) {
                LOGGER.error("Exception unmarshalling {}", e);
            } finally {
                IOUtils.closeQuietly(inStream);
            }
        }

        return featureCollection;
    }

    private static JAXBContext initJaxbContext() {
        JAXBContext jaxbContext = null;

        // JAXB context path
        String contextPath = StringUtils.join(new String[] {Wfs20Constants.OGC_WFS_PACKAGE,
                Wfs20Constants.OGC_FILTER_PACKAGE, Wfs20Constants.OGC_GML_PACKAGE,
                Wfs20Constants.OGC_OWS_PACKAGE}, ":");

        try {
            jaxbContext = JAXBContext.newInstance(contextPath,
                    FeatureCollectionMessageBodyReaderWfs20.class.getClassLoader());
        } catch (JAXBException e) {
            LOGGER.error("Unable to create JAXB context using contextPath: {}.", contextPath, e);
        }

        return jaxbContext;
    }
    
    public void registerConverter(FeatureConverter converter) {
        featureConverterMap.put(converter.getMetacardType().getName(), converter);
        xstream.registerConverter(converter);
        xstream.alias(converter.getMetacardType().getName(), Metacard.class);
    }
    
}
