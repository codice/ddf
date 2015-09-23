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
package org.codice.ddf.spatial.ogc.csw.catalog.transformer;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.atLeastOnce;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswJAXBElementProvider;
import org.hamcrest.core.IsEqual;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import ddf.catalog.transformer.api.PrintWriter;
import ddf.catalog.transformer.api.PrintWriterProvider;

import net.opengis.cat.csw.v_2_0_2.AcknowledgementType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import net.opengis.cat.csw.v_2_0_2.ResultType;

//import static org.hamcrest.MatcherAssert.assertThat;
//import static org.mockito.Matchers.eq;

public class TestCswQueryResponseTransformer {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(TestCswQueryResponseTransformer.class);

    private CswQueryResponseTransformer transformer;

    private Filter filter = mock(Filter.class);

    private TransformerManager mockTransformerManager;

    private PrintWriterProvider mockPrintWriterProvider;

    private PrintWriter mockPrintWriter;

    private MetacardTransformer mockMetacardTransformer;

    private BinaryContent mockBinaryContent;

    private Query mockQuery;

    private SourceResponse mockSourceResponse;

    private QueryRequest mockQueryRequest;

    private Map<String, Serializable> mockArguments;

    private List<Result> mockResults;

    private Serializable mockSerializable;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void before() {
        mockTransformerManager = mock(TransformerManager.class);
        mockPrintWriterProvider = mock(PrintWriterProvider.class);
        mockPrintWriter = mock(PrintWriter.class);
        mockMetacardTransformer = mock(MetacardTransformer.class);
        mockQuery = mock(Query.class);
        mockSourceResponse = mock(SourceResponse.class);
        mockQueryRequest = mock(QueryRequest.class);
        mockArguments = mock(Map.class);
        mockResults = mock(List.class);
        mockSerializable = mock(Serializable.class);
        mockBinaryContent = mock(BinaryContent.class);

        transformer = new CswQueryResponseTransformer(mockTransformerManager,
                mockPrintWriterProvider);

    }

    @Test
    public void whenNullArgumentsThenThrowException() throws CatalogTransformerException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Null argument map.");

        transformer.transform(mockSourceResponse, null);
    }

    @Test
    public void whenNullSourceResponseThenThrowException() throws CatalogTransformerException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Null source response.");

        transformer.transform(null, mockArguments);
    }

    @Test
    public void whenNullResultListThenThrowException() throws CatalogTransformerException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Null results list.");

        when(mockSourceResponse.getResults()).thenReturn(null);

        transformer.transform(mockSourceResponse, mockArguments);
    }

    @Test
    public void whenNullResultTypeArgumentThenThrowException() throws CatalogTransformerException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Null result type argument.");

        when(mockSourceResponse.getResults()).thenReturn(mockResults);
        when(mockArguments.get(anyString())).thenReturn(null);

        transformer.transform(mockSourceResponse, mockArguments);
    }

    @Test
    public void whenNullQueryRequestThenThrowException() throws CatalogTransformerException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Null source response query request.");

        when(mockSourceResponse.getResults()).thenReturn(mockResults);
        when(mockArguments.get(anyString())).thenReturn(ResultType.RESULTS);
        when(mockSourceResponse.getRequest()).thenReturn(null);

        transformer.transform(mockSourceResponse, mockArguments);
    }

    @Test
    public void whenNullQueryThenThrowException() throws CatalogTransformerException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Null source response query.");

        when(mockSourceResponse.getResults()).thenReturn(mockResults);
        when(mockArguments.get(anyString())).thenReturn(ResultType.RESULTS);
        when(mockSourceResponse.getRequest()).thenReturn(mockQueryRequest);
        when(mockQueryRequest.getQuery()).thenReturn(null);

        transformer.transform(mockSourceResponse, mockArguments);
    }

    @Test
    public void whenQueryByIdThenExpectOnlyOneXMLNode()
            throws CatalogTransformerException, IOException {
        // when
        when(mockPrintWriterProvider.build((Class<Metacard>) notNull()))
                .thenReturn(mockPrintWriter);
        when(mockPrintWriter.makeString()).thenReturn(new String());

        when(mockSourceResponse.getResults()).thenReturn(Collections.<Result>emptyList());
        when(mockSourceResponse.getRequest()).thenReturn(mockQueryRequest);
        when(mockQueryRequest.getQuery()).thenReturn(mockQuery);
        when(mockArguments.get(CswConstants.RESULT_TYPE_PARAMETER)).thenReturn(ResultType.RESULTS);
        when(mockArguments.get(CswConstants.IS_BY_ID_QUERY)).thenReturn(true);

        // given
        transformer.init();
        BinaryContent bc = transformer.transform(mockSourceResponse, mockArguments);
        transformer.destroy();

        // then
        ArgumentCaptor<String> strArgCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockPrintWriter, times(1)).startNode(strArgCaptor.capture());

        //LOGGER.info("[" + StringUtils.join(strArgCaptor.getAllValues(), ", ") + "]");
        List<String> values = strArgCaptor.getAllValues();
        assertThat("Missing XML node.", values.get(0),
                new IsEqual(CswQueryResponseTransformer.RECORDS_RESPONSE_QNAME));

    }

    @Test
    public void whenQueryByHitsThenTransformZeroMetacards()
            throws CatalogTransformerException, IOException {
        // when
        when(mockPrintWriterProvider.build((Class<Metacard>) notNull()))
                .thenReturn(mockPrintWriter);
        when(mockPrintWriter.makeString()).thenReturn(new String());

        when(mockSourceResponse.getResults()).thenReturn(createResults(1, 10));
        when(mockSourceResponse.getRequest()).thenReturn(mockQueryRequest);
        when(mockQueryRequest.getQuery()).thenReturn(mockQuery);
        when(mockArguments.get(CswConstants.RESULT_TYPE_PARAMETER)).thenReturn(ResultType.HITS);

        // given
        transformer.init();
        BinaryContent bc = transformer.transform(mockSourceResponse, mockArguments);
        transformer.destroy();

        // then
        ArgumentCaptor<String> tmCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockTransformerManager, never()).getTransformerBySchema(tmCaptor.capture());

        ArgumentCaptor<Map> mapCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Metacard> mcCaptor = ArgumentCaptor.forClass(Metacard.class);
        verify(mockMetacardTransformer, never()).transform(mcCaptor.capture(), mapCaptor.capture());

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> valCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockPrintWriter, atLeastOnce())
                .addAttribute(keyCaptor.capture(), valCaptor.capture());

        List<String> keys = keyCaptor.getAllValues();
        List<String> vals = valCaptor.getAllValues();
        int numAttrKeyIndex = keys
                .indexOf(CswQueryResponseTransformer.NUMBER_OF_RECORDS_RETURNED_ATTRIBUTE);
        assertThat("Missing XML attribute.", numAttrKeyIndex != -1, is(true));

        String numAttrValue = vals.get(numAttrKeyIndex);
        assertThat("Missing XML attribute value.", numAttrValue, new IsEqual("0"));
    }

    @Test
    public void whenEmptyResultListThenTransformZeroMetacards()
            throws CatalogTransformerException, IOException {
        // when
        when(mockPrintWriterProvider.build((Class<Metacard>) notNull()))
                .thenReturn(mockPrintWriter);
        when(mockPrintWriter.makeString()).thenReturn(new String());
        when(mockSourceResponse.getResults()).thenReturn(Collections.<Result>emptyList());
        when(mockSourceResponse.getRequest()).thenReturn(mockQueryRequest);
        when(mockQueryRequest.getQuery()).thenReturn(mockQuery);
        when(mockArguments.get(CswConstants.RESULT_TYPE_PARAMETER)).thenReturn(ResultType.RESULTS);

        // given
        transformer.init();
        BinaryContent bc = transformer.transform(mockSourceResponse, mockArguments);
        transformer.destroy();

        // then
        ArgumentCaptor<String> tmCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockTransformerManager, never()).getTransformerBySchema(tmCaptor.capture());

        ArgumentCaptor<Map> mapCaptor = ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Metacard> mcCaptor = ArgumentCaptor.forClass(Metacard.class);
        verify(mockMetacardTransformer, never()).transform(mcCaptor.capture(), mapCaptor.capture());

    }

    @Test
    public void whenEmptyResultListExpectOnlyThreeXMLNodes()
            throws CatalogTransformerException, IOException {
        // when
        when(mockPrintWriterProvider.build((Class<Metacard>) notNull()))
                .thenReturn(mockPrintWriter);
        when(mockPrintWriter.makeString()).thenReturn(new String());
        when(mockSourceResponse.getResults()).thenReturn(Collections.<Result>emptyList());
        when(mockSourceResponse.getRequest()).thenReturn(mockQueryRequest);
        when(mockQueryRequest.getQuery()).thenReturn(mockQuery);
        when(mockArguments.get(CswConstants.RESULT_TYPE_PARAMETER)).thenReturn(ResultType.RESULTS);

        // given
        transformer.init();
        BinaryContent bc = transformer.transform(mockSourceResponse, mockArguments);
        transformer.destroy();

        // then
        ArgumentCaptor<String> pwCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockPrintWriter, times(3)).startNode(pwCaptor.capture());

        //LOGGER.info("[" + StringUtils.join(strArgCaptor.getAllValues(), ", ") + "]");
        List<String> values = pwCaptor.getAllValues();
        assertThat("Missing XML node.", values.get(0),
                new IsEqual(CswQueryResponseTransformer.RECORDS_RESPONSE_QNAME));
        assertThat("Missing XML node.", values.get(1),
                new IsEqual(CswQueryResponseTransformer.SEARCH_STATUS_QNAME));
        assertThat("Missing XML node.", values.get(2),
                new IsEqual(CswQueryResponseTransformer.SEARCH_RESULTS_QNAME));

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

        JAXBElement<?> jaxb = (JAXBElement<?>) getJaxBContext().createUnmarshaller()
                .unmarshal(new ByteArrayInputStream(xml.getBytes("UTF-8")));

        assertThat(jaxb.getValue(), is(instanceOf(AcknowledgementType.class)));
        AcknowledgementType response = (AcknowledgementType) jaxb.getValue();
        assertThat(response.getEchoedRequest().getAny(), is(instanceOf(JAXBElement.class)));
        JAXBElement<?> jaxB = (JAXBElement<?>) response.getEchoedRequest().getAny();
        assertThat(jaxB.getValue(), is(instanceOf(GetRecordsType.class)));
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
        String contextPath = StringUtils
                .join(new String[] {CswConstants.OGC_CSW_PACKAGE, CswConstants.OGC_FILTER_PACKAGE,
                        CswConstants.OGC_GML_PACKAGE, CswConstants.OGC_OWS_PACKAGE}, ":");

        context = JAXBContext
                .newInstance(contextPath, CswJAXBElementProvider.class.getClassLoader());

        return context;
    }
}
