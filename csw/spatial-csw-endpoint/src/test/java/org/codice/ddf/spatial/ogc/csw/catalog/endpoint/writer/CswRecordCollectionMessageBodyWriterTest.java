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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import net.opengis.cat.csw.v_2_0_2.AcknowledgementType;
import net.opengis.cat.csw.v_2_0_2.GetRecordByIdResponseType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsResponseType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import net.opengis.cat.csw.v_2_0_2.ResultType;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswJAXBElementProvider;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordCollection;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.RecordConverterFactory;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.impl.CswRecordConverterFactory;
import org.junit.Test;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;

public class CswRecordCollectionMessageBodyWriterTest {

    @Test
    public void testMarshalRecordCollection() throws WebApplicationException, IOException, JAXBException {
        RecordConverterFactory factory = new CswRecordConverterFactory(null);
        CswRecordCollectionMessageBodyWriter writer = new CswRecordCollectionMessageBodyWriter(
                Arrays.asList(factory));

        GetRecordsType query = new GetRecordsType();
        query.setResultType(ResultType.RESULTS);
        query.setMaxRecords(BigInteger.valueOf(6));
        query.setStartPosition(BigInteger.valueOf(4));
        CswRecordCollection collection = createCswRecordCollection(query, 22);
        
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        writer.writeTo(collection, null, null, null, null, null, stream); 

        String xml = new String(stream.toByteArray());

        JAXBElement<?> jaxb = (JAXBElement<?>) getJaxBContext().createUnmarshaller().unmarshal(
                new ByteArrayInputStream(xml.getBytes("UTF-8")));
        
        assertThat(jaxb.getValue(), is(instanceOf(GetRecordsResponseType.class)));
        GetRecordsResponseType response = (GetRecordsResponseType) jaxb.getValue();
        assertThat(response.getSearchResults().getNumberOfRecordsReturned().intValue(), is(6));
        assertThat(response.getSearchResults().getNumberOfRecordsMatched().intValue(), is(22));
        assertThat(response.getSearchResults().getNextRecord().intValue(), is(10));
        assertThat(response.getSearchResults().getAbstractRecord().size(), is(6));
    }

    @Test
    public void testMarshalAcknowledgement() throws WebApplicationException, IOException, JAXBException {
        RecordConverterFactory factory = new CswRecordConverterFactory(null);
        CswRecordCollectionMessageBodyWriter writer = new CswRecordCollectionMessageBodyWriter(
                Arrays.asList(factory));

        GetRecordsType query = new GetRecordsType();
        query.setResultType(ResultType.VALIDATE);
        query.setMaxRecords(BigInteger.valueOf(6));
        query.setStartPosition(BigInteger.valueOf(4));
        CswRecordCollection collection = createCswRecordCollection(query, 22);
        
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        writer.writeTo(collection, null, null, null, null, null, stream); 

        String xml = new String(stream.toByteArray());

        JAXBElement<?> jaxb = (JAXBElement<?>) getJaxBContext().createUnmarshaller().unmarshal(
                new ByteArrayInputStream(xml.getBytes("UTF-8")));
        
        assertThat(jaxb.getValue(), is(instanceOf(AcknowledgementType.class)));
        AcknowledgementType response = (AcknowledgementType) jaxb.getValue();
        assertThat(response.getEchoedRequest().getAny(), is(instanceOf(JAXBElement.class)));
        JAXBElement<?> jaxB = (JAXBElement<?>) response.getEchoedRequest().getAny();
        assertThat(jaxB.getValue(), is(instanceOf(GetRecordsType.class)));
    }

    @Test
    public void testMarshalRecordCollectionByIdRequest() throws WebApplicationException,
        IOException, JAXBException {
        RecordConverterFactory factory = new CswRecordConverterFactory(null);
        CswRecordCollectionMessageBodyWriter writer = new CswRecordCollectionMessageBodyWriter(
                Arrays.asList(factory));
        CswRecordCollection collection = createCswRecordCollection(null, 2);
        collection.setById(true);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        writer.writeTo(collection, null, null, null, null, null, stream);

        String xml = new String(stream.toByteArray());

        JAXBElement<?> jaxb = (JAXBElement<?>) getJaxBContext().createUnmarshaller().unmarshal(
                new ByteArrayInputStream(xml.getBytes("UTF-8")));

        assertThat(jaxb.getValue(), is(instanceOf(GetRecordByIdResponseType.class)));
        GetRecordByIdResponseType response = (GetRecordByIdResponseType) jaxb.getValue();
        assertThat(response.getAbstractRecord().size(), is(2));
    }

    private CswRecordCollection createCswRecordCollection(GetRecordsType request, int resultCount) {
        CswRecordCollection collection = new CswRecordCollection();
        int first = 1;
        int last = 2;
        
        if (request != null) {
            first = request.getStartPosition().intValue();
            int next = request.getMaxRecords().intValue() + first;
            last = next - 1;
            if (last >= resultCount) {
                last = resultCount;
                next = 0;
            }
        }
        int returned = last - first + 1;

        collection.setCswRecords(createMetacardList(first, last));
        collection.setNumberOfRecordsMatched(resultCount);
        collection.setNumberOfRecordsReturned(returned);
        collection.setRequest(request);
        return collection;
    }
    
    private List<Metacard> createMetacardList(int start, int finish) {
        List<Metacard> list = new LinkedList<Metacard>();
        
        for (int i = start; i <= finish; i++) {
            MetacardImpl metacard = new MetacardImpl();
            
            metacard.setId("id_" + i);
            metacard.setSourceId("source_" + i);
            metacard.setTitle("title " + i);
                
            list.add(metacard);
        }
        
        return list;
    }

    private JAXBContext getJaxBContext() throws JAXBException {
        JAXBContext context = null;
        String contextPath = StringUtils.join(new String[] {
            CswConstants.OGC_CSW_PACKAGE, CswConstants.OGC_FILTER_PACKAGE, 
            CswConstants.OGC_GML_PACKAGE, CswConstants.OGC_OWS_PACKAGE}, ":");

        context = JAXBContext.newInstance(contextPath,
                CswJAXBElementProvider.class.getClassLoader());
        
        return context;
    }
}
