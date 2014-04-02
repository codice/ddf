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
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint.writer;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.GregorianCalendar;
import java.util.List;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import net.opengis.cat.csw.v_2_0_2.AcknowledgementType;
import net.opengis.cat.csw.v_2_0_2.EchoedRequestType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import net.opengis.cat.csw.v_2_0_2.ObjectFactory;
import net.opengis.cat.csw.v_2_0_2.ResultType;

import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordCollection;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.RecordConverterFactory;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.impl.GetRecordsResponseConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.io.naming.NoNameCoder;
import com.thoughtworks.xstream.io.xml.StaxDriver;

/**
 * 
 * CswRecordCollectionMessageBodyWriter generates an xml response for a {@link CswRecordCollection}
 *
 */

@Produces({ MediaType.TEXT_XML, MediaType.APPLICATION_XML })
@Provider
public class CswRecordCollectionMessageBodyWriter implements MessageBodyWriter<CswRecordCollection> {

    private XStream xstreamGetRecordsResponse;
    
    private XStream xstreamGetRecordByIdResponse;

    private static final Logger LOGGER = LoggerFactory.getLogger(CswRecordCollectionMessageBodyWriter.class);    

    public CswRecordCollectionMessageBodyWriter(List<RecordConverterFactory> factories) {
        xstreamGetRecordsResponse = initXstream(CswConstants.GET_RECORDS_RESPONSE, factories);
        xstreamGetRecordByIdResponse = initXstream(CswConstants.GET_RECORD_BY_ID_RESPONSE,
                factories);
    }
    
    @Override
    public long getSize(CswRecordCollection recordCollection, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations,
            MediaType mediaType) {
        return CswRecordCollection.class.isAssignableFrom(type);
    }

    @Override
    public void writeTo(CswRecordCollection recordCollection, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream outStream)
        throws IOException, WebApplicationException {
        if (recordCollection.isById()) {
            try {
                xstreamGetRecordByIdResponse.toXML(recordCollection, outStream);
            } catch (XStreamException e) {
                throw new WebApplicationException(e);
            }
        } else {
            if (ResultType.VALIDATE.equals(recordCollection.getRequest().getResultType())) {
                writeAcknowledgement(recordCollection.getRequest(), outStream);
            } else {
                try {
                    xstreamGetRecordsResponse.toXML(recordCollection, outStream);
                } catch (XStreamException e) {
                    throw new WebApplicationException(e);
                }
            }
        }
    }

    private void writeAcknowledgement(GetRecordsType request, OutputStream outStream)
        throws IOException {
        //xstream.toXML(new Acknowledgement(recordCollection.getRequest()), outStream);

        try {
            JAXBContext jaxBContext = JAXBContext.newInstance("net.opengis.cat.csw.v_2_0_2:" +  
                            "net.opengis.filter.v_1_1_0:net.opengis.gml.v_3_1_1:net.opengis.ows.v_1_0_0");
            Marshaller marshaller = jaxBContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            AcknowledgementType ack = new AcknowledgementType();
            EchoedRequestType echoedRequest = new EchoedRequestType();
            JAXBElement<GetRecordsType> jaxBRequest = new ObjectFactory().createGetRecords(request);
            echoedRequest.setAny(jaxBRequest);
            ack.setEchoedRequest(echoedRequest);
            try {
                ack.setTimeStamp(DatatypeFactory.newInstance().newXMLGregorianCalendar(new GregorianCalendar()));
            } catch (DatatypeConfigurationException e) {
                LOGGER.warn("Failed to set timestamp on Acknowledgement, Exception {}", e);
            }
            
            JAXBElement<AcknowledgementType> jaxBAck = new ObjectFactory().createAcknowledgement(ack);
            marshaller.marshal(jaxBAck, outStream);
        } catch (JAXBException e) {
            throw new IOException(e);
        }
    }
    
    private XStream initXstream(final String elementName, List<RecordConverterFactory> factories) {
        XStream xstream = new XStream(new StaxDriver(new NoNameCoder()));
        xstream.setClassLoader(xstream.getClass().getClassLoader());

        GetRecordsResponseConverter cswGetRecordsResponseConverter = new GetRecordsResponseConverter(
                factories);

        xstream.registerConverter(cswGetRecordsResponseConverter);

        xstream.alias(CswConstants.CSW_NAMESPACE_PREFIX + CswConstants.NAMESPACE_DELIMITER
                + elementName, CswRecordCollection.class);

        return xstream;
    }

}
