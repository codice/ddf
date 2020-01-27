/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.source.solr.provider;

import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.create;
import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.deleteAll;
import static ddf.catalog.source.solr.provider.SolrProviderTestUtil.getFilterBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.source.solr.BaseSolrCatalogProvider;
import ddf.catalog.source.solr.BaseSolrProviderTest;
import java.beans.XMLEncoder;
import java.io.BufferedOutputStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.swing.border.BevelBorder;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class SolrProviderExtensibleMetacards {

  private Set<AttributeDescriptor> descriptionDescriptors = new HashSet<>();
  private static final String DESCRIPTION_FIELD = "description";
  private static final String DESCRIPTION_VALUE = "myDescription";

  private Set<AttributeDescriptor> authorDescriptors = new HashSet<>();
  private static final String AUTHOR_FIELD = "author";
  private static final String AUTHOR_VALUE =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" + "<author>john doe</author>";

  private Set<AttributeDescriptor> typesDescriptors = new HashSet<>();
  private String doubleField = "hertz";
  private double doubleFieldValue = 16065.435;

  private String floatField = "inches";
  private float floatFieldValue = 4.435f;

  private String intField = "count";
  private int intFieldValue = 4;

  private String longField = "milliseconds";
  private long longFieldValue = 987654322111L;

  private String byteField = "bytes";
  private byte[] byteFieldValue = {86};

  private String booleanField = "expected";

  private String dateField = "lost";
  private Date dateFieldValue = new Date();

  private String geoField = "geo";
  private String geoFieldValue = Library.GULF_OF_GUINEA_POINT_WKT;

  private String shortField = "daysOfTheWeek";
  private short shortFieldValue = 1;

  private String objectField = "payload";
  private BevelBorder objectFieldValue = new BevelBorder(BevelBorder.RAISED);

  private static BaseSolrCatalogProvider provider;

  @BeforeClass
  public static void setUp() {
    provider = BaseSolrProviderTest.getProvider();
  }

  @Before
  public void setup() throws IngestException, UnsupportedQueryException {
    deleteAll(provider);

    create(createDescriptionMetacard(), provider);
    create(createAuthor(), provider);
    create(createTypesMetacard(), provider);
  }

  private Metacard createAuthor() {
    MetacardImpl customMetacard;
    authorDescriptors.add(
        new AttributeDescriptorImpl(Metacard.ID, true, true, true, false, BasicTypes.STRING_TYPE));
    authorDescriptors.add(
        new AttributeDescriptorImpl(AUTHOR_FIELD, true, true, true, false, BasicTypes.XML_TYPE));
    MetacardTypeImpl newType = new MetacardTypeImpl("34ga$^TGHfg:/", authorDescriptors);
    customMetacard = new MetacardImpl(newType);
    customMetacard.setAttribute(AUTHOR_FIELD, AUTHOR_VALUE);
    return customMetacard;
  }

  private Metacard createDescriptionMetacard() {
    descriptionDescriptors.add(
        new AttributeDescriptorImpl(Metacard.ID, true, true, true, false, BasicTypes.STRING_TYPE));
    descriptionDescriptors.add(
        new AttributeDescriptorImpl(
            DESCRIPTION_FIELD, true, true, true, false, BasicTypes.STRING_TYPE));
    MetacardTypeImpl mType = new MetacardTypeImpl("custom1", descriptionDescriptors);
    MetacardImpl customMetacard = new MetacardImpl(mType);
    customMetacard.setAttribute(DESCRIPTION_FIELD, DESCRIPTION_VALUE);
    return customMetacard;
  }

  private Metacard createTypesMetacard() {

    typesDescriptors.add(
        new AttributeDescriptorImpl(Metacard.ID, true, true, true, false, BasicTypes.STRING_TYPE));
    typesDescriptors.add(
        new AttributeDescriptorImpl(doubleField, true, true, false, false, BasicTypes.DOUBLE_TYPE));
    typesDescriptors.add(
        new AttributeDescriptorImpl(floatField, true, true, false, false, BasicTypes.FLOAT_TYPE));
    typesDescriptors.add(
        new AttributeDescriptorImpl(intField, true, true, false, false, BasicTypes.INTEGER_TYPE));
    typesDescriptors.add(
        new AttributeDescriptorImpl(longField, true, true, false, false, BasicTypes.LONG_TYPE));
    typesDescriptors.add(
        new AttributeDescriptorImpl(byteField, false, true, false, false, BasicTypes.BINARY_TYPE));
    typesDescriptors.add(
        new AttributeDescriptorImpl(
            booleanField, true, true, false, false, BasicTypes.BOOLEAN_TYPE));
    typesDescriptors.add(
        new AttributeDescriptorImpl(dateField, true, true, false, false, BasicTypes.DATE_TYPE));
    typesDescriptors.add(
        new AttributeDescriptorImpl(geoField, true, true, false, false, BasicTypes.GEO_TYPE));
    typesDescriptors.add(
        new AttributeDescriptorImpl(shortField, true, true, false, false, BasicTypes.SHORT_TYPE));
    typesDescriptors.add(
        new AttributeDescriptorImpl(
            objectField, false, true, false, false, BasicTypes.OBJECT_TYPE));

    MetacardTypeImpl mType = new MetacardTypeImpl("numbersMT", typesDescriptors);

    MetacardImpl customMetacard = new MetacardImpl(mType);
    customMetacard.setAttribute(doubleField, doubleFieldValue);
    customMetacard.setAttribute(floatField, floatFieldValue);
    customMetacard.setAttribute(intField, intFieldValue);
    customMetacard.setAttribute(longField, longFieldValue);
    customMetacard.setAttribute(byteField, byteFieldValue);
    customMetacard.setAttribute(booleanField, true);
    customMetacard.setAttribute(dateField, dateFieldValue);
    customMetacard.setAttribute(geoField, geoFieldValue);
    customMetacard.setAttribute(shortField, shortFieldValue);
    customMetacard.setAttribute(objectField, objectFieldValue);

    return customMetacard;
  }

  @Test
  public void queryById() throws UnsupportedQueryException {
    Query query = new QueryImpl(getFilterBuilder().attribute("id").like().text("*"));

    QueryRequest request = new QueryRequestImpl(query);

    SourceResponse response = provider.query(request);

    assertThat(response.getResults().size(), equalTo(3));

    assertThat(
        response.getResults().get(0).getMetacard().getMetacardType().getName(), equalTo("custom1"));

    assertThat(
        response.getResults().get(0).getMetacard().getMetacardType().getAttributeDescriptors(),
        equalTo(descriptionDescriptors));
  }

  @Test
  public void queryTitleWithWildcard() throws UnsupportedQueryException {
    Query query = new QueryImpl(getFilterBuilder().attribute(DESCRIPTION_FIELD).like().text("*"));

    QueryRequest request = new QueryRequestImpl(query);

    SourceResponse response = provider.query(request);

    assertThat(response.getResults().size(), equalTo(1));

    assertThat(
        response.getResults().get(0).getMetacard().getMetacardType().getName(), equalTo("custom1"));

    assertThat(
        response.getResults().get(0).getMetacard().getMetacardType().getAttributeDescriptors(),
        equalTo(descriptionDescriptors));
  }

  @Test
  public void queryTitleEquals() throws UnsupportedQueryException {
    Query query =
        new QueryImpl(
            getFilterBuilder().attribute(DESCRIPTION_FIELD).equalTo().text(DESCRIPTION_VALUE));

    QueryRequest request = new QueryRequestImpl(query);

    SourceResponse response = provider.query(request);

    assertEquals(1, response.getResults().size());
  }

  @Test
  public void queryForMissingTerm() throws UnsupportedQueryException {
    Query query = new QueryImpl(getFilterBuilder().attribute(DESCRIPTION_FIELD).like().text("no"));

    QueryRequest request = new QueryRequestImpl(query);

    SourceResponse response = provider.query(request);

    assertEquals(0, response.getResults().size());
  }

  @Test
  public void queryAuthorType() throws UnsupportedQueryException {
    Query query =
        new QueryImpl(getFilterBuilder().attribute(AUTHOR_FIELD).like().caseSensitiveText("doe"));

    QueryRequest request = new QueryRequestImpl(query);

    SourceResponse response = provider.query(request);

    assertThat(response.getResults().size(), equalTo(1));

    assertThat(
        response.getResults().get(0).getMetacard().getMetacardType().getName(),
        equalTo("34ga$^TGHfg:/"));

    assertThat(
        response.getResults().get(0).getMetacard().getMetacardType().getAttributeDescriptors(),
        equalTo(authorDescriptors));
  }

  @Test
  public void queryMissingAuthorTerm() throws UnsupportedQueryException {
    Query query = new QueryImpl(getFilterBuilder().attribute(AUTHOR_FIELD).like().text("author"));

    QueryRequest request = new QueryRequestImpl(query);

    SourceResponse response = provider.query(request);

    assertThat(response.getResults().size(), equalTo(0));
  }

  @Test
  public void queryDouble() throws UnsupportedQueryException {
    Query query =
        new QueryImpl(
            getFilterBuilder().attribute(doubleField).greaterThan().number(doubleFieldValue - 1.0));

    QueryRequest request = new QueryRequestImpl(query);

    SourceResponse response = provider.query(request);

    assertThat(response.getResults().size(), equalTo(1));
  }

  @Test
  public void queryInteger() throws UnsupportedQueryException {
    Query query =
        new QueryImpl(
            getFilterBuilder().attribute(intField).greaterThan().number(intFieldValue - 1));

    QueryRequest request = new QueryRequestImpl(query);

    SourceResponse response = provider.query(request);

    assertEquals(1, response.getResults().size());

    Metacard resultMetacard = response.getResults().get(0).getMetacard();

    assertThat(resultMetacard.getAttribute(Metacard.ID), notNullValue());
    assertThat(resultMetacard.getAttribute(doubleField).getValue(), equalTo(doubleFieldValue));
    assertThat(resultMetacard.getAttribute(intField).getValue(), equalTo(intFieldValue));
    assertThat(resultMetacard.getAttribute(floatField).getValue(), equalTo(floatFieldValue));
    assertThat(resultMetacard.getAttribute(longField).getValue(), equalTo(longFieldValue));
    assertThat(resultMetacard.getAttribute(byteField).getValue(), equalTo(byteFieldValue));
    assertThat(resultMetacard.getAttribute(booleanField).getValue(), equalTo(true));
    assertThat(resultMetacard.getAttribute(dateField).getValue(), equalTo(dateFieldValue));
    assertThat(resultMetacard.getAttribute(geoField).getValue(), equalTo(geoFieldValue));
    assertThat(resultMetacard.getAttribute(shortField).getValue(), equalTo(shortFieldValue));
    assertThat(resultMetacard.getAttribute(objectField).getValue(), instanceOf(BevelBorder.class));
    /*
     * Going to use the XMLEncoder. If it writes out the objects the same way in xml, then they
     * are the same.
     */
    ByteArrayOutputStream beveledBytesStreamFromSolr = new ByteArrayOutputStream();
    XMLEncoder solrXMLEncoder =
        new XMLEncoder(new BufferedOutputStream(beveledBytesStreamFromSolr));
    solrXMLEncoder.writeObject((resultMetacard.getAttribute(objectField).getValue()));
    solrXMLEncoder.close();

    ByteArrayOutputStream beveledBytesStreamOriginal = new ByteArrayOutputStream();
    XMLEncoder currendEncoder =
        new XMLEncoder(new BufferedOutputStream(beveledBytesStreamOriginal));
    currendEncoder.writeObject(objectFieldValue);
    currendEncoder.close();

    assertThat(
        beveledBytesStreamFromSolr.toByteArray(),
        equalTo(beveledBytesStreamOriginal.toByteArray()));
  }

  @Test
  public void queryShort() throws UnsupportedQueryException {
    Query query =
        new QueryImpl(
            getFilterBuilder()
                .attribute(shortField)
                .greaterThanOrEqualTo()
                .number(shortFieldValue));

    QueryRequest request = new QueryRequestImpl(query);

    SourceResponse response = provider.query(request);

    assertThat(response.getResults().size(), equalTo(1));
  }
}
