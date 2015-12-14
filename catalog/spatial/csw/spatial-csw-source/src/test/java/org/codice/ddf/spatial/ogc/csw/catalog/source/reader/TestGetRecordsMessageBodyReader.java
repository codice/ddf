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
package org.codice.ddf.spatial.ogc.csw.catalog.source.reader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codice.ddf.spatial.ogc.csw.catalog.common.CswAxisOrder;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordCollection;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordMetacardType;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswSourceConfiguration;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.CswRecordConverter;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.CswTransformProvider;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.DefaultCswRecordMap;
import org.codice.ddf.spatial.ogc.csw.catalog.transformer.TransformerManager;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

import ddf.catalog.data.Metacard;

public class TestGetRecordsMessageBodyReader {

    private CswTransformProvider mockProvider = mock(CswTransformProvider.class);

    private TransformerManager mockInputManager = mock(TransformerManager.class);

    @Before
    public void setUp() {
        when(mockProvider.canConvert(any(Class.class))).thenReturn(true);
    }

    @Test
    public void testConfigurationArguments() throws Exception {
        when(mockInputManager.getTransformerBySchema(anyString()))
                .thenReturn(new CswRecordConverter());

        CswSourceConfiguration config = new CswSourceConfiguration();
        config.setMetacardCswMappings(
                DefaultCswRecordMap.getDefaultCswRecordMap().getCswToMetacardAttributeNames());
        config.setOutputSchema(CswConstants.CSW_OUTPUT_SCHEMA);
        config.setCswAxisOrder(CswAxisOrder.LAT_LON);
        config.setThumbnailMapping(CswRecordMetacardType.CSW_REFERENCES);
        config.setResourceUriMapping(CswRecordMetacardType.CSW_SOURCE);

        GetRecordsMessageBodyReader reader = new GetRecordsMessageBodyReader(mockProvider, config);
        InputStream is = TestGetRecordsMessageBodyReader.class
                .getResourceAsStream("/getRecordsResponse.xml");

        ArgumentCaptor<UnmarshallingContext> captor = ArgumentCaptor
                .forClass(UnmarshallingContext.class);

        reader.readFrom(CswRecordCollection.class, null, null, null, null, is);

        // Verify the context arguments were set correctly
        verify(mockProvider, times(3))
                .unmarshal(any(HierarchicalStreamReader.class), captor.capture());

        UnmarshallingContext context = captor.getValue();
        // Assert the properties needed for CswRecordConverter
        assertThat(context.get(CswConstants.CSW_MAPPING), notNullValue());
        Object cswMapping = context.get(CswConstants.CSW_MAPPING);
        assertThat(cswMapping, is(Map.class));
        assertThat(context.get(Metacard.RESOURCE_URI), is(String.class));
        assertThat((String) context.get(Metacard.RESOURCE_URI),
                is(CswRecordMetacardType.CSW_SOURCE));
        assertThat(context.get(Metacard.THUMBNAIL), is(String.class));
        assertThat((String) context.get(Metacard.THUMBNAIL),
                is(CswRecordMetacardType.CSW_REFERENCES));
        assertThat(context.get(CswConstants.AXIS_ORDER_PROPERTY), is(CswAxisOrder.class));
        assertThat((CswAxisOrder) context.get(CswConstants.AXIS_ORDER_PROPERTY),
                is(CswAxisOrder.LAT_LON));

        // Assert the output Schema is set.
        assertThat(context.get(CswConstants.OUTPUT_SCHEMA_PARAMETER), is(String.class));
        assertThat((String) context.get(CswConstants.OUTPUT_SCHEMA_PARAMETER),
                is(CswConstants.CSW_OUTPUT_SCHEMA));
    }

    @Test
    public void testFullThread() throws Exception {
        CswTransformProvider provider = new CswTransformProvider(null, mockInputManager);

        when(mockInputManager.getTransformerBySchema(anyString()))
                .thenReturn(new CswRecordConverter());

        CswSourceConfiguration config = new CswSourceConfiguration();
        Map<String, String> mappings = new HashMap<>();
        mappings.put(Metacard.CREATED, "dateSubmitted");
        mappings.put(Metacard.EFFECTIVE, "created");
        mappings.put(Metacard.MODIFIED, "modified");
        mappings.put(Metacard.CONTENT_TYPE, "type");
        config.setMetacardCswMappings(mappings);
        //        config.setMetacardCswMappings(
        //                DefaultCswRecordMap.getDefaultCswRecordMap().getCswToMetacardAttributeNames());
        config.setOutputSchema(CswConstants.CSW_OUTPUT_SCHEMA);
        config.setCswAxisOrder(CswAxisOrder.LAT_LON);
        config.setThumbnailMapping(CswRecordMetacardType.CSW_REFERENCES);
        config.setResourceUriMapping(CswRecordMetacardType.CSW_SOURCE);

        GetRecordsMessageBodyReader reader = new GetRecordsMessageBodyReader(provider, config);
        InputStream is = TestGetRecordsMessageBodyReader.class
                .getResourceAsStream("/getRecordsResponse.xml");

        CswRecordCollection cswRecords = reader
                .readFrom(CswRecordCollection.class, null, null, null, null, is);

        List<Metacard> metacards = cswRecords.getCswRecords();

        assertThat(metacards, not(nullValue()));
        assertThat(metacards.size(), equalTo(3));

        // verify first metacard's values
        Metacard mc = metacards.get(0);
        Map<String, Object> expectedValues = getExpectedMap(
                "{8C1F6297-EC96-4302-A01E-14988C9149FD}", "title 1", "2008-12-15",
                new String[] {"subject 1", "second subject"}, "abstract 1",
                new String[] {"copyright 1", "copyright 2"}, "dataset", "Shapefile",
                "POLYGON((52.139 5.121, 52.517 5.121, 52.517 4.468, 52.139 4.468, 52.139 5.121))");
        assertMetacard(mc, expectedValues);

        // verify second metacard's values
        mc = metacards.get(1);
        expectedValues = getExpectedMap("{23362852-F370-4369-B0B2-BE74B2859614}", "mc2 title",
                "2010-12-15", new String[] {"first subject", "subject 2"}, "mc2 abstract",
                new String[] {"first copyright", "second copyright"}, "dataset 2", "Shapefile 2",
                "POLYGON((53.139 6.121, 53.517 6.121, 53.517 5.468, 53.139 5.468, 53.139 6.121))");
        assertMetacard(mc, expectedValues);

        // verify third metacard's values
        mc = metacards.get(2);
        expectedValues = getExpectedMap("{23362852-F370-4369-B0B2-BE74B2859615}", "mc3 title", "2010-12-15",
                new String[] {"first subject", "subject 3"}, "mc3 abstract",
                new String[] {"first copyright", "second copyright"}, "dataset 3", "Shapefile 3",
                "POLYGON((53.139 6.121, 53.517 6.121, 53.517 5.468, 53.139 5.468, 53.139 6.121))");
        assertMetacard(mc, expectedValues);

    }

    // verifies UTF-8 encoding configured properly when XML includes foreign text with special characters
    @Test
    public void testGetMultipleMetacardsWithForeignText() throws Exception {
        CswSourceConfiguration config = new CswSourceConfiguration();
        config.setMetacardCswMappings(
                DefaultCswRecordMap.getDefaultCswRecordMap().getCswToMetacardAttributeNames());
        config.setOutputSchema(CswConstants.CSW_OUTPUT_SCHEMA);
        GetRecordsMessageBodyReader reader = new GetRecordsMessageBodyReader(mockProvider, config);

        InputStream is = TestGetRecordsMessageBodyReader.class
                .getResourceAsStream("/geomaticsGetRecordsResponse.xml");
        CswRecordCollection cswRecords = reader
                .readFrom(CswRecordCollection.class, null, null, null, null, is);
        List<Metacard> metacards = cswRecords.getCswRecords();
        assertThat(metacards, not(nullValue()));
        assertThat(metacards.size(), equalTo(10));
    }

    // //////////////////////////////////////////////////////////////////////////////////////////////////////

    private void assertMetacard(Metacard mc, Map<String, Object> expectedValues) {
        assertThat(mc.getId(), equalTo((String) expectedValues.get(Metacard.ID)));
        assertListStringAttribute(mc, CswRecordMetacardType.CSW_IDENTIFIER,
                (String[]) expectedValues.get(CswRecordMetacardType.CSW_IDENTIFIER));
        assertThat(mc.getTitle(), equalTo((String) expectedValues.get(Metacard.TITLE)));
        assertListStringAttribute(mc, CswRecordMetacardType.CSW_TITLE,
                (String[]) expectedValues.get(CswRecordMetacardType.CSW_TITLE));
        assertThat(mc.getModifiedDate(), equalTo((Date) expectedValues.get(Metacard.MODIFIED)));
        assertListStringAttribute(mc, CswRecordMetacardType.CSW_MODIFIED,
                (String[]) expectedValues.get(CswRecordMetacardType.CSW_MODIFIED));
        assertListStringAttribute(mc, CswRecordMetacardType.CSW_SUBJECT,
                (String[]) expectedValues.get(CswRecordMetacardType.CSW_SUBJECT));
        assertListStringAttribute(mc, Metacard.DESCRIPTION,
                (String[]) expectedValues.get(Metacard.DESCRIPTION));
        assertListStringAttribute(mc, CswRecordMetacardType.CSW_RIGHTS,
                (String[]) expectedValues.get(CswRecordMetacardType.CSW_RIGHTS));
        assertListStringAttribute(mc, CswRecordMetacardType.CSW_LANGUAGE,
                (String[]) expectedValues.get(CswRecordMetacardType.CSW_LANGUAGE));
        assertThat((String) mc.getAttribute(CswRecordMetacardType.CSW_TYPE).getValue(),
                equalTo((String) expectedValues.get(CswRecordMetacardType.CSW_TYPE)));
        assertListStringAttribute(mc, CswRecordMetacardType.CSW_FORMAT,
                (String[]) expectedValues.get(CswRecordMetacardType.CSW_FORMAT));
        assertThat(mc.getLocation(), equalTo((String) expectedValues.get(Metacard.GEOGRAPHY)));
        assertListStringAttribute(mc, CswRecordMetacardType.OWS_BOUNDING_BOX,
                (String[]) expectedValues.get(CswRecordMetacardType.OWS_BOUNDING_BOX));
    }

    private void assertListStringAttribute(Metacard mc, String attrName, String[] expectedValues) {
        List<?> values = (List<?>) mc.getAttribute(attrName).getValues();
        assertThat(values, not(nullValue()));
        assertThat(values.size(), equalTo(expectedValues.length));

        List<String> valuesList = new ArrayList<String>();
        valuesList.addAll((List<? extends String>) values);
        assertThat(valuesList, hasItems(expectedValues));
    }

    private Map<String, Object> getExpectedMap(String id, String title, String dateString, String[] subject,
            String description, String[] rights, String dataset, String format, String poly) {
        Map<String, Object> expectedValues = new HashMap<String, Object>();
        expectedValues.put(Metacard.ID, id);
        expectedValues.put(CswRecordMetacardType.CSW_IDENTIFIER, new String[] {id});
        expectedValues.put(Metacard.TITLE, title);
        expectedValues.put(CswRecordMetacardType.CSW_TITLE, new String[] {title});
        DateTimeFormatter dateFormatter = ISODateTimeFormat.dateOptionalTimeParser();
        Date expectedModifiedDate = dateFormatter.parseDateTime(dateString).toDate();
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
