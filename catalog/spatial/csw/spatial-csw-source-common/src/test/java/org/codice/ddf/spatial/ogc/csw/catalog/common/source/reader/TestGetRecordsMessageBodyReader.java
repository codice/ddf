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
package org.codice.ddf.spatial.ogc.csw.catalog.common.source.reader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.codice.ddf.spatial.ogc.csw.catalog.common.CswAxisOrder;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordCollection;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordMetacardType;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswSourceConfiguration;
import org.codice.ddf.spatial.ogc.csw.catalog.common.converter.DefaultCswRecordMap;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transformer.TransformerManager;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.resource.Resource;
import ddf.security.encryption.EncryptionService;

public class TestGetRecordsMessageBodyReader {

    private Converter mockProvider = mock(Converter.class);

    private EncryptionService encryptionService = mock(EncryptionService.class);

    @Before
    public void setUp() {
        when(mockProvider.canConvert(any(Class.class))).thenReturn(true);
    }

    @Test
    public void testConfigurationArguments() throws Exception {

        CswSourceConfiguration config = new CswSourceConfiguration(encryptionService);
        config.setMetacardCswMappings(DefaultCswRecordMap.getCswToMetacardAttributeNames());
        config.setOutputSchema(CswConstants.CSW_OUTPUT_SCHEMA);
        config.setCswAxisOrder(CswAxisOrder.LAT_LON);
        config.putMetacardCswMapping(Metacard.THUMBNAIL, CswRecordMetacardType.CSW_REFERENCES);
        config.putMetacardCswMapping(Metacard.RESOURCE_URI, CswRecordMetacardType.CSW_SOURCE);

        GetRecordsMessageBodyReader reader = new GetRecordsMessageBodyReader(mockProvider, config);
        InputStream is = TestGetRecordsMessageBodyReader.class.getResourceAsStream(
                "/getRecordsResponse.xml");
        MultivaluedMap<String, String> httpHeaders = new MultivaluedHashMap<>();

        ArgumentCaptor<UnmarshallingContext> captor =
                ArgumentCaptor.forClass(UnmarshallingContext.class);

        reader.readFrom(CswRecordCollection.class, null, null, null, httpHeaders, is);

        // Verify the context arguments were set correctly
        verify(mockProvider, times(1)).unmarshal(any(HierarchicalStreamReader.class),
                captor.capture());

        UnmarshallingContext context = captor.getValue();
        // Assert the properties needed for CswRecordConverter
        assertThat(context.get(CswConstants.CSW_MAPPING), notNullValue());
        Object cswMapping = context.get(CswConstants.CSW_MAPPING);
        assertThat(cswMapping, instanceOf(Map.class));
        assertThat(context.get(Metacard.RESOURCE_URI), instanceOf(String.class));
        assertThat(context.get(Metacard.RESOURCE_URI), is(CswRecordMetacardType.CSW_SOURCE));
        assertThat(context.get(Metacard.THUMBNAIL), instanceOf(String.class));
        assertThat(context.get(Metacard.THUMBNAIL), is(CswRecordMetacardType.CSW_REFERENCES));
        assertThat(context.get(CswConstants.AXIS_ORDER_PROPERTY), instanceOf(CswAxisOrder.class));
        assertThat(context.get(CswConstants.AXIS_ORDER_PROPERTY), is(CswAxisOrder.LAT_LON));

        // Assert the output Schema is set.
        assertThat(context.get(CswConstants.OUTPUT_SCHEMA_PARAMETER), instanceOf(String.class));
        assertThat(context.get(CswConstants.OUTPUT_SCHEMA_PARAMETER),
                is(CswConstants.CSW_OUTPUT_SCHEMA));

        assertThat(context.get(CswConstants.TRANSFORMER_LOOKUP_KEY), instanceOf(String.class));
        assertThat(context.get(CswConstants.TRANSFORMER_LOOKUP_KEY), is(TransformerManager.SCHEMA));
        assertThat(context.get(CswConstants.TRANSFORMER_LOOKUP_VALUE), instanceOf(String.class));
        assertThat(context.get(CswConstants.TRANSFORMER_LOOKUP_VALUE),
                is(CswConstants.CSW_OUTPUT_SCHEMA));
    }

    @Test
    public void testFullThread() throws Exception {
        List<Metacard> inputMetacards = new ArrayList<>();
        MetacardImpl metacard = new MetacardImpl(BasicTypes.BASIC_METACARD);
        metacard.setId("metacard1");
        metacard.setTitle("title1");
        inputMetacards.add(metacard);
        CswRecordCollection collection = new CswRecordCollection();
        collection.setCswRecords(inputMetacards);
        when(mockProvider.unmarshal(any(), any())).thenReturn(collection);

        CswSourceConfiguration config = new CswSourceConfiguration(encryptionService);
        Map<String, String> mappings = new HashMap<>();
        mappings.put(Metacard.CREATED, "dateSubmitted");
        mappings.put(Metacard.EFFECTIVE, "created");
        mappings.put(Metacard.MODIFIED, "modified");
        mappings.put(Metacard.CONTENT_TYPE, "type");
        config.setMetacardCswMappings(mappings);
        config.setOutputSchema(CswConstants.CSW_OUTPUT_SCHEMA);
        config.setCswAxisOrder(CswAxisOrder.LAT_LON);
        config.putMetacardCswMapping(Metacard.THUMBNAIL, CswRecordMetacardType.CSW_REFERENCES);
        config.putMetacardCswMapping(Metacard.RESOURCE_URI, CswRecordMetacardType.CSW_SOURCE);

        GetRecordsMessageBodyReader reader = new GetRecordsMessageBodyReader(mockProvider, config);
        InputStream is = TestGetRecordsMessageBodyReader.class.getResourceAsStream(
                "/getRecordsResponse.xml");
        MultivaluedMap<String, String> httpHeaders = new MultivaluedHashMap<>();

        CswRecordCollection cswRecords = reader.readFrom(CswRecordCollection.class,
                null,
                null,
                null,
                httpHeaders,
                is);

        List<Metacard> metacards = cswRecords.getCswRecords();

        assertThat(metacards, contains(metacard));
    }

    // verifies UTF-8 encoding configured properly when XML includes foreign text with special characters
    @Test
    public void testGetMultipleMetacardsWithForeignText() throws Exception {
        List<Metacard> inputMetacards = new ArrayList<>();
        MetacardImpl metacard = new MetacardImpl(BasicTypes.BASIC_METACARD);
        inputMetacards.add(metacard);
        CswRecordCollection collection = new CswRecordCollection();
        collection.setCswRecords(inputMetacards);
        when(mockProvider.unmarshal(any(), any())).thenReturn(collection);
        CswSourceConfiguration config = new CswSourceConfiguration(encryptionService);
        config.setMetacardCswMappings(DefaultCswRecordMap.getCswToMetacardAttributeNames());
        config.setOutputSchema(CswConstants.CSW_OUTPUT_SCHEMA);
        GetRecordsMessageBodyReader reader = new GetRecordsMessageBodyReader(mockProvider, config);

        InputStream is = TestGetRecordsMessageBodyReader.class.getResourceAsStream(
                "/geomaticsGetRecordsResponse.xml");
        MultivaluedMap<String, String> httpHeaders = new MultivaluedHashMap<>();
        CswRecordCollection cswRecords = reader.readFrom(CswRecordCollection.class,
                null,
                null,
                null,
                httpHeaders,
                is);
        List<Metacard> metacards = cswRecords.getCswRecords();
        assertThat(metacards, contains(metacard));
    }

    @Test
    public void testReadProductData() throws Exception {
        CswSourceConfiguration config = new CswSourceConfiguration(encryptionService);
        config.setMetacardCswMappings(DefaultCswRecordMap.getCswToMetacardAttributeNames());
        config.setOutputSchema(CswConstants.CSW_OUTPUT_SCHEMA);
        GetRecordsMessageBodyReader reader = new GetRecordsMessageBodyReader(mockProvider, config);

        byte[] data = "SampleData".getBytes();
        ByteArrayInputStream dataInputStream = new ByteArrayInputStream(data);
        MultivaluedMap<String, String> httpHeaders = new MultivaluedHashMap<>();
        httpHeaders.add(CswConstants.PRODUCT_RETRIEVAL_HTTP_HEADER, "TRUE");
        httpHeaders.add(HttpHeaders.CONTENT_DISPOSITION,
                String.format("inline; filename=ResourceName"));
        MediaType mediaType = new MediaType("text", "plain");

        CswRecordCollection cswRecords = reader.readFrom(CswRecordCollection.class,
                null,
                null,
                mediaType,
                httpHeaders,
                dataInputStream);

        Resource resource = cswRecords.getResource();
        assertThat(resource, not(nullValue()));
        assertThat(resource.getName(), equalTo("ResourceName"));
        assertThat(resource.getMimeType()
                .toString(), is(equalTo(MediaType.TEXT_PLAIN)));
        assertThat(resource.getByteArray(), is(equalTo(data)));
    }

    private void assertListStringAttribute(Metacard mc, String attrName, String[] expectedValues) {
        List<?> values = mc.getAttribute(attrName)
                .getValues();
        assertThat(values, not(nullValue()));
        assertThat(values.size(), equalTo(expectedValues.length));

        List<String> valuesList = new ArrayList<>();
        valuesList.addAll((List<? extends String>) values);
        assertThat(valuesList, hasItems(expectedValues));
    }

    private Map<String, Object> getExpectedMap(String id, String title, String dateString,
            String[] subject, String description, String[] rights, String dataset, String format,
            String poly) {
        Map<String, Object> expectedValues = new HashMap<>();
        expectedValues.put(Metacard.ID, id);
        expectedValues.put(CswRecordMetacardType.CSW_IDENTIFIER, new String[] {id});
        expectedValues.put(Metacard.TITLE, title);
        expectedValues.put(CswRecordMetacardType.CSW_TITLE, new String[] {title});
        DateTimeFormatter dateFormatter = ISODateTimeFormat.dateOptionalTimeParser();
        Date expectedModifiedDate = dateFormatter.parseDateTime(dateString)
                .toDate();
        expectedValues.put(CswRecordMetacardType.CSW_MODIFIED, new String[] {dateString});
        expectedValues.put(Metacard.MODIFIED, expectedModifiedDate);
        expectedValues.put(CswRecordMetacardType.CSW_SUBJECT, subject);
        expectedValues.put(Metacard.DESCRIPTION, new String[] {description});
        expectedValues.put(CswRecordMetacardType.CSW_RIGHTS, rights);
        expectedValues.put(CswRecordMetacardType.CSW_LANGUAGE, new String[] {"english"});
        expectedValues.put(CswRecordMetacardType.CSW_TYPE, dataset);
        expectedValues.put(CswRecordMetacardType.CSW_FORMAT, new String[] {format});
        expectedValues.put(Metacard.GEOGRAPHY, poly);
        expectedValues.put(CswRecordMetacardType.OWS_BOUNDING_BOX, new String[] {poly});
        return expectedValues;
    }
}