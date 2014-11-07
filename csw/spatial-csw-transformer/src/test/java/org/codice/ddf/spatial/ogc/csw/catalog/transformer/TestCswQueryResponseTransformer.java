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
package org.codice.ddf.spatial.ogc.csw.catalog.transformer;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.transform.CatalogTransformerException;
import net.opengis.cat.csw.v_2_0_2.AcknowledgementType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import net.opengis.cat.csw.v_2_0_2.ResultType;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswJAXBElementProvider;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordCollection;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.CswTransformProvider;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.GetRecordsResponseConverter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opengis.filter.Filter;

import javax.ws.rs.WebApplicationException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestCswQueryResponseTransformer {

    private CswQueryResponseTransformer transformer;

    private GetRecordsResponseConverter mockConverter;

    private CswTransformProvider mockTransformProvider;

    private Filter filter = mock(Filter.class);

    @Before
    public void before() {
        mockConverter = mock(GetRecordsResponseConverter.class);
        mockTransformProvider = mock(CswTransformProvider.class);
        transformer = new CswQueryResponseTransformer(mockConverter);
        when(mockConverter.canConvert(any(Class.class))).thenReturn(true);
        when(mockTransformProvider.canConvert(any(Class.class))).thenReturn(true);
    }

    @Test
    public void testMarshalRecordCollection()
            throws WebApplicationException, IOException, JAXBException,
            CatalogTransformerException {

        GetRecordsType query = new GetRecordsType();
        query.setResultType(ResultType.RESULTS);
        query.setMaxRecords(BigInteger.valueOf(6));
        query.setStartPosition(BigInteger.valueOf(4));
        SourceResponse sourceResponse = createSourceResponse(query, 22);

        Map<String, Serializable> args = new HashMap<>();
        args.put(CswConstants.OUTPUT_SCHEMA_PARAMETER, CswConstants.CSW_OUTPUT_SCHEMA);
        args.put(CswConstants.RESULT_TYPE_PARAMETER, ResultType.RESULTS);

        ArgumentCaptor<CswRecordCollection> captor = ArgumentCaptor
                .forClass(CswRecordCollection.class);

        BinaryContent content = transformer.transform(sourceResponse, args);

        verify(mockConverter, times(1)).marshal(captor.capture(), any(
                HierarchicalStreamWriter.class), any(MarshallingContext.class));

        CswRecordCollection collection = captor.getValue();

        assertThat(collection.getResultType(), is(ResultType.RESULTS));
        assertThat(collection.getOutputSchema(), is(CswConstants.CSW_OUTPUT_SCHEMA));
        assertThat(collection.getStartPosition(), is(4));
        assertThat(collection.isById(), is(false));
        assertThat(collection.getNumberOfRecordsMatched(), is(22L));
        assertThat(collection.getNumberOfRecordsReturned(), is(6L));
        assertThat(collection.getCswRecords().isEmpty(), is(false));
    }

    @Test
    public void testMarshalAcknowledgement()
            throws WebApplicationException, IOException, JAXBException,
            CatalogTransformerException {

        GetRecordsType query = new GetRecordsType();
        query.setResultType(ResultType.VALIDATE);
        query.setMaxRecords(BigInteger.valueOf(6));
        query.setStartPosition(BigInteger.valueOf(4));
        SourceResponse sourceResponse = createSourceResponse(query, 22);

        Map<String, Serializable> args = new HashMap<String, Serializable>();
        args.put(CswConstants.RESULT_TYPE_PARAMETER, ResultType.VALIDATE);
        args.put(CswConstants.GET_RECORDS, query);

        BinaryContent content = transformer.transform(sourceResponse, args);

        String xml = new String(content.getByteArray());

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
            IOException, JAXBException, CatalogTransformerException {
        SourceResponse sourceResponse = createSourceResponse(null, 2);

        Map<String, Serializable> args = new HashMap<String, Serializable>();
        args.put(CswConstants.IS_BY_ID_QUERY, true);
        ArgumentCaptor<CswRecordCollection> captor = ArgumentCaptor
                .forClass(CswRecordCollection.class);

        BinaryContent content = transformer.transform(sourceResponse, args);

        verify(mockConverter, times(1)).marshal(captor.capture(), any(
                HierarchicalStreamWriter.class), any(MarshallingContext.class));

        CswRecordCollection collection = captor.getValue();

        assertThat(collection.isById(), is(true));
    }

    private SourceResponse createSourceResponse(GetRecordsType request, int resultCount) {
        int first = 1;
        int last = 2;
        int max = 0;
        if (request != null) {
            first = request.getStartPosition().intValue();
            max = request.getMaxRecords().intValue();
            int next = request.getMaxRecords().intValue() + first;
            last = next - 1;
            if (last >= resultCount) {
                last = resultCount;
                next = 0;
            }
        }
        int returned = last - first + 1;

        QueryImpl query = new QueryImpl(filter, first, max, null, true, 0);
        SourceResponseImpl sourceResponse = new SourceResponseImpl(new QueryRequestImpl(query),
                createResults(first, last));
        sourceResponse.setHits(resultCount);
        return sourceResponse;
    }

    private List<Result> createResults(int start, int finish) {
        List<Result> list = new LinkedList<Result>();

        for (int i = start; i <= finish; i++) {
            MetacardImpl metacard = new MetacardImpl();

            metacard.setId("id_" + i);
            metacard.setSourceId("source_" + i);
            metacard.setTitle("title " + i);

            list.add(new ResultImpl(metacard));
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
