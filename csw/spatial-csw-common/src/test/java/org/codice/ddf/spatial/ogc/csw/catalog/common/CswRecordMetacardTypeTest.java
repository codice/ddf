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
package org.codice.ddf.spatial.ogc.csw.catalog.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

import net.opengis.cat.csw.v_2_0_2.ObjectFactory;

import org.apache.cxf.common.util.CollectionUtils;
import org.junit.Test;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;

public class CswRecordMetacardTypeTest {

    public ObjectFactory objFactory;

    public net.opengis.cat.csw.v_2_0_2.dc.elements.ObjectFactory dcElementsObjFactory;

    @Test
    public void testConstruction() {
        CswRecordMetacardType cswRecordMetacardType = new CswRecordMetacardType();

        assertThat(cswRecordMetacardType, not(nullValue()));

        Set<AttributeDescriptor> descriptors = cswRecordMetacardType.getAttributeDescriptors();
        assertThat(descriptors, not(nullValue()));
        assertThat(CollectionUtils.isEmpty(descriptors), is(false));

        assertThat(cswRecordMetacardType.getAttributeDescriptor(Metacard.ID).isMultiValued(),
                is(false));
    }

    /**
     * Verifies can successfully create a CSW Record and set only the required minimum number of
     * fields.
     */
    @Test
    public void testCswMetacardWithOnlyRequiredAttributesSet() {

        MetacardImpl metacard = new MetacardImpl(new CswRecordMetacardType());
        assertThat(metacard, not(nullValue()));

        metacard.setAttribute(Metacard.ID, "ddf_id");
        Date modifiedDate = new Date();
        metacard.setAttribute(Metacard.MODIFIED, modifiedDate);
        String metadata = "<xml>metadata goes here ...</xml>";
        metacard.setAttribute(Metacard.METADATA, metadata);
        metacard.setAttribute(Metacard.TITLE, "ddf_title");
        metacard.setAttribute(CswRecordMetacardType.CSW_IDENTIFIER, "identifier_1");
        metacard.setAttribute(CswRecordMetacardType.CSW_TITLE, "title_1");

        assertThat(metacard.getId(), equalTo("ddf_id"));
        assertThat(metacard.getTitle(), equalTo("ddf_title"));
        assertThat(metacard.getModifiedDate().toString(), equalTo(modifiedDate.toString()));
        assertThat(metacard.getMetadata(), equalTo(metadata));
        assertThat((String) metacard.getAttribute(CswRecordMetacardType.CSW_IDENTIFIER).getValue(),
                equalTo("identifier_1"));
        assertThat((String) metacard.getAttribute(CswRecordMetacardType.CSW_TITLE).getValue(),
                equalTo("title_1"));
    }

    /**
     * CSW Summary Record Sets every attribute in a CSW Summary Record with a single unique value
     * and then asserts that every attribute can be retrieved.
     */
    @Test
    public void testCswMetacardAllSummaryRecordAttributesSetWithSingleValues() {

        MetacardImpl metacard = new MetacardImpl(new CswRecordMetacardType());
        assertThat(metacard, not(nullValue()));

        metacard.setAttribute(Metacard.ID, "ddf_id");
        Date modifiedDate = new Date();
        metacard.setAttribute(Metacard.MODIFIED, modifiedDate);
        String metadata = "<xml>metadata goes here ...</xml>";
        metacard.setAttribute(Metacard.METADATA, metadata);
        metacard.setAttribute(Metacard.TITLE, "ddf_title");
        metacard.setAttribute(CswRecordMetacardType.CSW_IDENTIFIER, "identifier_1");
        metacard.setAttribute(CswRecordMetacardType.CSW_TITLE, "title_1");
        metacard.setAttribute(CswRecordMetacardType.CSW_TYPE, "type_1");
        metacard.setAttribute(CswRecordMetacardType.CSW_FORMAT, "format_1");
        metacard.setAttribute(CswRecordMetacardType.CSW_SUBJECT, "subject_1");
        metacard.setAttribute(CswRecordMetacardType.CSW_RELATION, "relation_1");
        Calendar cal = Calendar.getInstance();
        cal.set(2013, 12, 25);
        Date cswModifiedDate = cal.getTime();
        metacard.setAttribute(CswRecordMetacardType.CSW_MODIFIED, cswModifiedDate);
        metacard.setAttribute(CswRecordMetacardType.CSW_DESCRIPTION, "abstract_1");
        String cswSpatial = "<dc:spatial><dcmiBox:Box name=\"Geographic\" projection=\"EPSG:4326\"><dcmiBox:northlimit units=\"decimal degrees\">42.01</dcmiBox:northlimit><dcmiBox:eastlimit units=\"decimal degrees\">-109.21</dcmiBox:eastlimit><dcmiBox:southlimit units=\"decimal degrees\">36.98</dcmiBox:southlimit><dcmiBox:westlimit units=\"decimal degrees\">-114.1</dcmiBox:westlimit></dcmiBox:Box></dc:spatial>";
        metacard.setAttribute(CswRecordMetacardType.CSW_SPATIAL, cswSpatial);
        String cswBBox = "<ows:BoundingBox crs=\"urn:ogc:def:crs:EPSG:6.6:4326\">\r\n"
                + "      <ows:LowerCorner>97.3805 6.74583</ows:LowerCorner>\r\n"
                + "      <ows:UpperCorner>68.1442 35.5056</ows:UpperCorner>\r\n"
                + "    </ows:BoundingBox>";
        metacard.setAttribute(CswRecordMetacardType.OWS_BOUNDING_BOX, cswBBox);

        assertThat(metacard.getId(), equalTo("ddf_id"));
        assertThat(metacard.getTitle(), equalTo("ddf_title"));
        assertThat(metacard.getModifiedDate().toString(), equalTo(modifiedDate.toString()));
        assertThat(metacard.getMetadata(), equalTo(metadata));
        assertThat((String) metacard.getAttribute(CswRecordMetacardType.CSW_IDENTIFIER).getValue(),
                equalTo("identifier_1"));
        assertThat((String) metacard.getAttribute(CswRecordMetacardType.CSW_TITLE).getValue(),
                equalTo("title_1"));
        assertThat((String) metacard.getAttribute(CswRecordMetacardType.CSW_TYPE).getValue(),
                equalTo("type_1"));
        assertThat((String) metacard.getAttribute(CswRecordMetacardType.CSW_FORMAT).getValue(),
                equalTo("format_1"));
        assertThat((String) metacard.getAttribute(CswRecordMetacardType.CSW_SUBJECT).getValue(),
                equalTo("subject_1"));
        assertThat((String) metacard.getAttribute(CswRecordMetacardType.CSW_RELATION).getValue(),
                equalTo("relation_1"));
        assertThat((Date) metacard.getAttribute(CswRecordMetacardType.CSW_MODIFIED).getValue(),
                equalTo(cswModifiedDate));
        assertThat(
                (String) metacard.getAttribute(CswRecordMetacardType.CSW_DESCRIPTION).getValue(),
                equalTo("abstract_1"));
        assertThat((String) metacard.getAttribute(CswRecordMetacardType.CSW_SPATIAL).getValue(),
                equalTo(cswSpatial));
        assertThat((String) metacard.getAttribute(CswRecordMetacardType.OWS_BOUNDING_BOX)
                .getValue(), equalTo(cswBBox));
    }

    /**
     * Sets attributes in a CSW Summary Record with multiple values and verifies the values can be
     * retrieved.
     */
    @Test
    public void testCswMetacardAttributeWithMultipleValues() {

        MetacardImpl metacard = new MetacardImpl(new CswRecordMetacardType());
        assertThat(metacard, not(nullValue()));

        metacard.setAttribute(Metacard.ID, "ddf_id");
        Date modifiedDate = new Date();
        metacard.setAttribute(Metacard.MODIFIED, modifiedDate);
        String metadata = "<xml>metadata goes here ...</xml>";
        metacard.setAttribute(Metacard.METADATA, metadata);
        metacard.setAttribute(Metacard.TITLE, "ddf_title");
        metacard.setAttribute(CswRecordMetacardType.CSW_IDENTIFIER, "identifier_1");
        metacard.setAttribute(CswRecordMetacardType.CSW_TITLE, "title_1");

        Calendar cal = Calendar.getInstance();
        cal.set(2013, 12, 25);
        Date cswModifiedDate = cal.getTime();
        metacard.setAttribute(CswRecordMetacardType.CSW_MODIFIED, cswModifiedDate);

        AttributeImpl cswSubjects = new AttributeImpl(CswRecordMetacardType.CSW_SUBJECT,
                "subject_1");
        cswSubjects.addValue("subject_2");
        metacard.setAttribute(cswSubjects);

        // verifies STRING_TYPE attribute storage and retrieval
        assertThat((String) metacard.getAttribute(CswRecordMetacardType.CSW_IDENTIFIER).getValue(),
                equalTo("identifier_1"));

        // verifies DATE_TYPE attribute storage and retrieval
        assertThat((Date) metacard.getAttribute(CswRecordMetacardType.CSW_MODIFIED).getValue(),
                equalTo(cswModifiedDate));

        // verifies multi-value attribute storage and retrieval
        List<?> subjects = (List<?>) metacard.getAttribute(CswRecordMetacardType.CSW_SUBJECT)
                .getValues();
        assertThat(subjects, not(nullValue()));
        assertThat(subjects.size(), equalTo(2));

        List<String> subjectsList = new ArrayList<String>();
        subjectsList.addAll((List<? extends String>) subjects);
        assertThat(subjectsList.get(0), equalTo("subject_1"));
        assertThat(subjectsList.get(1), equalTo("subject_2"));
    }

    /**
     * Verifies can set multiple attributes that are substitution names for same base attribute,
     * e.g., date, modified, created, dateAccepted.
     */
    @Test
    public void testCswMetacardMultipleSubstitutionNameAttributesSet() {

        MetacardImpl metacard = new MetacardImpl(new CswRecordMetacardType());
        assertThat(metacard, not(nullValue()));

        metacard.setAttribute(Metacard.ID, "ddf_id");
        Date modifiedDate = new Date();
        metacard.setAttribute(Metacard.MODIFIED, modifiedDate);
        String metadata = "<xml>metadata goes here ...</xml>";
        metacard.setAttribute(Metacard.METADATA, metadata);
        metacard.setAttribute(Metacard.TITLE, "ddf_title");
        metacard.setAttribute(CswRecordMetacardType.CSW_IDENTIFIER, "identifier_1");
        metacard.setAttribute(CswRecordMetacardType.CSW_TITLE, "title_1");

        Calendar cal = Calendar.getInstance();
        cal.set(2013, 12, 25);
        Date cswDate = cal.getTime();
        metacard.setAttribute(CswRecordMetacardType.CSW_DATE, cswDate);
        cal.set(2013, 1, 1);
        Date cswModifiedDate = cal.getTime();
        metacard.setAttribute(CswRecordMetacardType.CSW_MODIFIED, cswModifiedDate);
        cal.set(2012, 2, 2);
        Date cswCreatedDate = cal.getTime();
        metacard.setAttribute(CswRecordMetacardType.CSW_CREATED, cswCreatedDate);
        cal.set(2013, 3, 3);
        Date cswDateAccepted = cal.getTime();
        metacard.setAttribute(CswRecordMetacardType.CSW_DATE_ACCEPTED, cswDateAccepted);

        assertThat((Date) metacard.getAttribute(CswRecordMetacardType.CSW_DATE).getValue(),
                equalTo(cswDate));
        assertThat((Date) metacard.getAttribute(CswRecordMetacardType.CSW_MODIFIED).getValue(),
                equalTo(cswModifiedDate));
        assertThat((Date) metacard.getAttribute(CswRecordMetacardType.CSW_CREATED).getValue(),
                equalTo(cswCreatedDate));
        assertThat(
                (Date) metacard.getAttribute(CswRecordMetacardType.CSW_DATE_ACCEPTED).getValue(),
                equalTo(cswDateAccepted));
    }

    @Test
    public void testCswMetacardHasBasicMetacardDescriptorsAsIsStoredFalse() {
        MetacardType cswMetacard = new CswRecordMetacardType();
        assertThat(cswMetacard.getAttributeDescriptor(Metacard.ID).isStored(), is(false));
        assertThat(cswMetacard.getAttributeDescriptor(Metacard.TITLE).isStored(), is(false));
        assertThat(cswMetacard.getAttributeDescriptor(Metacard.METADATA).isStored(), is(false));
        assertThat(cswMetacard.getAttributeDescriptor(Metacard.EFFECTIVE).isStored(), is(false));
        assertThat(cswMetacard.getAttributeDescriptor(Metacard.MODIFIED).isStored(), is(false));
        assertThat(cswMetacard.getAttributeDescriptor(Metacard.CREATED).isStored(), is(false));
        assertThat(cswMetacard.getAttributeDescriptor(Metacard.RESOURCE_URI).isStored(), is(false));
        assertThat(cswMetacard.getAttributeDescriptor(Metacard.CONTENT_TYPE).isStored(), is(false));
    }

}
