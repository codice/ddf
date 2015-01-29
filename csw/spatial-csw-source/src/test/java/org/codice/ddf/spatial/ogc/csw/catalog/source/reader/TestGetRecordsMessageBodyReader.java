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
package org.codice.ddf.spatial.ogc.csw.catalog.source.reader;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import ddf.catalog.data.Metacard;
import ddf.catalog.transform.InputTransformer;
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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                .thenReturn(new CswRecordConverter(null));

        CswSourceConfiguration config = new CswSourceConfiguration();
        config.setMetacardCswMappings(
                DefaultCswRecordMap.getDefaultCswRecordMap().getCswToMetacardAttributeNames());
        config.setOutputSchema(CswConstants.CSW_OUTPUT_SCHEMA);
        config.setIsLonLatOrder(false);
        config.setThumbnailMapping(CswRecordMetacardType.CSW_REFERENCES);
        config.setResourceUriMapping(CswRecordMetacardType.CSW_SOURCE);

        GetRecordsMessageBodyReader reader = new GetRecordsMessageBodyReader(mockProvider, config);
        InputStream is = TestGetRecordsMessageBodyReader.class
                .getResourceAsStream("/getRecordsResponse.xml");

        ArgumentCaptor<UnmarshallingContext> captor = ArgumentCaptor
                .forClass(UnmarshallingContext.class);

        reader.readFrom(CswRecordCollection.class, null, null,
                null, null, is);

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
        assertThat(context.get(CswConstants.IS_LON_LAT_ORDER_PROPERTY), is(Boolean.class));
        assertThat((Boolean) context.get(CswConstants.IS_LON_LAT_ORDER_PROPERTY), is(false));

        // Assert the output Schema is set.
        assertThat(context.get(CswConstants.OUTPUT_SCHEMA_PARAMETER), is(String.class));
        assertThat((String) context.get(CswConstants.OUTPUT_SCHEMA_PARAMETER),
                is(CswConstants.CSW_OUTPUT_SCHEMA));
    }

    @Test
    public void testFullThread() throws Exception {
        CswTransformProvider provider = new CswTransformProvider(null, mockInputManager);

        when(mockInputManager.getTransformerBySchema(anyString()))
                .thenReturn(new CswRecordConverter(null));

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
        config.setIsLonLatOrder(false);
        config.setThumbnailMapping(CswRecordMetacardType.CSW_REFERENCES);
        config.setResourceUriMapping(CswRecordMetacardType.CSW_SOURCE);

        GetRecordsMessageBodyReader reader = new GetRecordsMessageBodyReader(provider, config);
        InputStream is = TestGetRecordsMessageBodyReader.class
                .getResourceAsStream("/getRecordsResponse.xml");

        CswRecordCollection cswRecords = reader.readFrom(CswRecordCollection.class, null, null,
                null, null, is);

        List<Metacard> metacards = cswRecords.getCswRecords();

        assertThat(metacards, not(nullValue()));
        assertThat(metacards.size(), equalTo(3));

        // verify first metacard's values
        Metacard mc = metacards.get(0);
        Map<String, Object> expectedValues = new HashMap<String, Object>();
        expectedValues.put(Metacard.ID, "{8C1F6297-EC96-4302-A01E-14988C9149FD}");
        expectedValues.put(CswRecordMetacardType.CSW_IDENTIFIER,
                new String[] {"{8C1F6297-EC96-4302-A01E-14988C9149FD}"});
        expectedValues.put(Metacard.TITLE, "title 1");
        expectedValues.put(CswRecordMetacardType.CSW_TITLE, new String[] {"title 1"});
        String expectedModifiedDateStr = "2008-12-15";
        DateTimeFormatter dateFormatter = ISODateTimeFormat.dateOptionalTimeParser();
        Date expectedModifiedDate = dateFormatter.parseDateTime(expectedModifiedDateStr).toDate();
        expectedValues.put(CswRecordMetacardType.CSW_MODIFIED,
                new String[] {expectedModifiedDateStr});
        expectedValues.put(Metacard.MODIFIED, expectedModifiedDate);
        expectedValues.put(CswRecordMetacardType.CSW_SUBJECT, new String[] {"subject 1",
                "second subject"});
        expectedValues.put(CswRecordMetacardType.CSW_ABSTRACT, new String[] {"abstract 1"});
        expectedValues.put(CswRecordMetacardType.CSW_RIGHTS, new String[] {"copyright 1",
                "copyright 2"});
        expectedValues.put(CswRecordMetacardType.CSW_LANGUAGE, new String[] {"english"});
        expectedValues.put(CswRecordMetacardType.CSW_TYPE, "dataset");
        expectedValues.put(CswRecordMetacardType.CSW_FORMAT, new String[] {"Shapefile"});
        expectedValues.put(Metacard.GEOGRAPHY,
                "POLYGON((52.139 5.121, 52.517 5.121, 52.517 4.468, 52.139 4.468, 52.139 5.121))");
        expectedValues
                .put(CswRecordMetacardType.OWS_BOUNDING_BOX,
                        new String[] {
                                "POLYGON((52.139 5.121, 52.517 5.121, 52.517 4.468, 52.139 4.468, 52.139 5.121))"});
        assertMetacard(mc, expectedValues);

        expectedValues.clear();

        // verify second metacard's values
        mc = metacards.get(1);
        expectedValues = new HashMap<String, Object>();
        expectedValues.put(Metacard.ID, "{23362852-F370-4369-B0B2-BE74B2859614}");
        expectedValues.put(CswRecordMetacardType.CSW_IDENTIFIER,
                new String[] {"{23362852-F370-4369-B0B2-BE74B2859614}"});
        expectedValues.put(Metacard.TITLE, "mc2 title");
        expectedValues.put(CswRecordMetacardType.CSW_TITLE, new String[] {"mc2 title"});
        expectedModifiedDateStr = "2010-12-15";
        dateFormatter = ISODateTimeFormat.dateOptionalTimeParser();
        expectedModifiedDate = dateFormatter.parseDateTime(expectedModifiedDateStr).toDate();
        expectedValues.put(CswRecordMetacardType.CSW_MODIFIED,
                new String[] {expectedModifiedDateStr});
        expectedValues.put(Metacard.MODIFIED, expectedModifiedDate);
        expectedValues.put(CswRecordMetacardType.CSW_SUBJECT, new String[] {"first subject",
                "subject 2"});
        expectedValues.put(CswRecordMetacardType.CSW_ABSTRACT, new String[] {"mc2 abstract"});
        expectedValues.put(CswRecordMetacardType.CSW_RIGHTS, new String[] {"first copyright",
                "second copyright"});
        expectedValues.put(CswRecordMetacardType.CSW_LANGUAGE, new String[] {"english"});
        expectedValues.put(CswRecordMetacardType.CSW_TYPE, "dataset 2");
        expectedValues.put(CswRecordMetacardType.CSW_FORMAT, new String[] {"Shapefile 2"});
        expectedValues.put(Metacard.GEOGRAPHY,
                "POLYGON((53.139 6.121, 53.517 6.121, 53.517 5.468, 53.139 5.468, 53.139 6.121))");
        expectedValues
                .put(CswRecordMetacardType.OWS_BOUNDING_BOX,
                        new String[] {
                                "POLYGON((53.139 6.121, 53.517 6.121, 53.517 5.468, 53.139 5.468, 53.139 6.121))"});
        assertMetacard(mc, expectedValues);

        expectedValues.clear();
    }

    // verifies UTF-8 encoding configured properly when XML includes foreign text with special characters
    @Test
    public void testGetMultipleMetacardsWithForeignText() throws Exception {
        CswSourceConfiguration config = new CswSourceConfiguration();
        config.setMetacardCswMappings(
                DefaultCswRecordMap.getDefaultCswRecordMap().getCswToMetacardAttributeNames());
        config.setOutputSchema(CswConstants.CSW_OUTPUT_SCHEMA);
        GetRecordsMessageBodyReader reader = new GetRecordsMessageBodyReader(
                mockProvider, config);

        InputStream is = TestGetRecordsMessageBodyReader.class
                .getResourceAsStream("/geomaticsGetRecordsResponse.xml");
        CswRecordCollection cswRecords = reader.readFrom(CswRecordCollection.class, null, null,
                null, null, is);
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
        assertListStringAttribute(mc, CswRecordMetacardType.CSW_ABSTRACT,
                (String[]) expectedValues.get(CswRecordMetacardType.CSW_ABSTRACT));
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
}
