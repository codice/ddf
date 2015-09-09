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

import static java.util.Collections.EMPTY_MAP;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.xml.XmlParser;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswJAXBElementProvider;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import ddf.catalog.transformer.api.MetacardMarshaller;
import ddf.catalog.transformer.api.PrintWriterProvider;
import ddf.catalog.transformer.xml.MetacardMarshallerImpl;
import ddf.catalog.transformer.xml.PrintWriterProviderImpl;
import ddf.catalog.transformer.xml.XmlMetacardTransformer;

import net.opengis.cat.csw.v_2_0_2.AcknowledgementType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import net.opengis.cat.csw.v_2_0_2.ResultType;

public class TestCswQueryResponseTransformer {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(TestCswQueryResponseTransformer.class);

    private CswQueryResponseTransformer transformer;

    private Filter filter = mock(Filter.class);

    private TransformerManager mockTransformerManager;

    private PrintWriterProvider mockPrintWriterProvider;

    @Before
    public void before() {
        mockTransformerManager = mock(TransformerManager.class);
        mockPrintWriterProvider = mock(PrintWriterProvider.class);
        transformer = new CswQueryResponseTransformer(mockTransformerManager,
                mockPrintWriterProvider);
    }

    @Test
    public void testMarshalNullSourceResponseResultList() throws CatalogTransformerException {
        SourceResponseImpl sourceResponse = new SourceResponseImpl(null, null);
        BinaryContent bc = transformer.transform(sourceResponse, EMPTY_MAP);
        assertNull(bc);
    }

    @Test
    public void testMarshalRecordCollectionNotNull()
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

        TransformerManager mockTransformerManager = mock(TransformerManager.class);
        Parser parser = new XmlParser();
        PrintWriterProvider printWriterProvider = new PrintWriterProviderImpl();
        MetacardMarshaller metacardMarshaller = new MetacardMarshallerImpl(parser,
                printWriterProvider);
        MetacardTransformer metacardTransformer = new XmlMetacardTransformer(metacardMarshaller);
        when(mockTransformerManager.getTransformerBySchema(anyString()))
                .thenReturn(metacardTransformer);

        CswQueryResponseTransformer transformer = new CswQueryResponseTransformer(
                mockTransformerManager, printWriterProvider);
        transformer.init();

        BinaryContent bc = transformer.transform(sourceResponse, args);

        assertNotNull("CswQueryResponseTransformer output null binary content.", bc);
        //String outputXml = new String(bc.getByteArray());
        //LOGGER.info(outputXml);

        transformer.destroy();
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
