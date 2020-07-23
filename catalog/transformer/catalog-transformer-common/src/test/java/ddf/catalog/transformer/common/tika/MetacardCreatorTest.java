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
package ddf.catalog.transformer.common.tika;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import com.google.common.collect.ImmutableSet;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.data.types.Contact;
import ddf.catalog.data.types.Media;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import org.apache.commons.collections.CollectionUtils;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.Office;
import org.apache.tika.metadata.TIFF;
import org.apache.tika.metadata.TikaCoreProperties;
import org.junit.Test;

public class MetacardCreatorTest {
  @Test
  public void testNoMetadata() {
    final Metadata metadata = new Metadata();

    final Metacard metacard =
        MetacardCreator.createMetacard(metadata, null, null, MetacardImpl.BASIC_METACARD);

    assertThat(metacard, notNullValue());
    assertThat(metacard.getTitle(), nullValue());
    assertThat(metacard.getContentTypeName(), nullValue());
    assertThat(metacard.getCreatedDate(), nullValue());
    assertThat(metacard.getModifiedDate(), nullValue());
    assertThat(metacard.getLocation(), nullValue());
    assertThat(metacard.getId(), nullValue());
    assertThat(metacard.getMetadata(), nullValue());
  }

  @Test
  public void testBasicMetacard() {
    Metacard metacard = createTestMetacard(null);
    assertThat(metacard.getMetacardType(), is(MetacardImpl.BASIC_METACARD));
  }

  @Test
  public void testValidDoubleAttribute() {

    Set<AttributeDescriptor> extraAttributes = new HashSet<>();
    extraAttributes.add(
        new AttributeDescriptorImpl(
            Media.DURATION, false, false, false, false, BasicTypes.DOUBLE_TYPE));
    final Metadata metadata = new Metadata();
    String durationValue = "14.88";
    metadata.add(MetacardCreator.DURATION_METDATA_KEY, durationValue);
    MetacardTypeImpl extendedMetacardType =
        new MetacardTypeImpl(
            MetacardImpl.BASIC_METACARD.getName(), MetacardImpl.BASIC_METACARD, extraAttributes);
    final String id = "id";
    final String metadataXml = "<xml>test</xml>";

    Metacard metacard =
        MetacardCreator.createMetacard(metadata, id, metadataXml, extendedMetacardType);
    assertThat(metacard.getMetadata(), is(metadataXml));
    assertThat(metacard.getAttribute(Media.DURATION).getValue(), is(Double.valueOf(durationValue)));
  }

  @Test
  public void testNullIntAttribute() {

    Set<AttributeDescriptor> extraAttributes = new HashSet<>();
    extraAttributes.add(
        new AttributeDescriptorImpl(
            Media.DURATION, false, false, false, false, BasicTypes.INTEGER_TYPE));
    final Metadata metadata = new Metadata();
    metadata.add(MetacardCreator.COMPRESSION_TYPE_METADATA_KEY, null);
    MetacardTypeImpl extendedMetacardType =
        new MetacardTypeImpl(
            MetacardImpl.BASIC_METACARD.getName(), MetacardImpl.BASIC_METACARD, extraAttributes);
    final String id = "id";
    final String metadataXml = "<xml>test</xml>";

    Metacard metacard =
        MetacardCreator.createMetacard(metadata, id, metadataXml, extendedMetacardType);
    assertThat(metacard.getMetadata(), is(metadataXml));
    assertThat(metacard.getAttribute(Media.COMPRESSION), is(nullValue()));
  }

  @Test
  public void testDoubleAttributeWithNumberFormatException() {

    Set<AttributeDescriptor> extraAttributes = new HashSet<>();
    extraAttributes.add(
        new AttributeDescriptorImpl(
            Media.DURATION, false, false, false, false, BasicTypes.DOUBLE_TYPE));
    final Metadata metadata = new Metadata();
    String durationValue = "Not actually a double";
    metadata.add(MetacardCreator.DURATION_METDATA_KEY, durationValue);
    MetacardTypeImpl extendedMetacardType =
        new MetacardTypeImpl(
            MetacardImpl.BASIC_METACARD.getName(), MetacardImpl.BASIC_METACARD, extraAttributes);
    final String id = "id";
    final String metadataXml = "<xml>test</xml>";

    Metacard metacard =
        MetacardCreator.createMetacard(metadata, id, metadataXml, extendedMetacardType);
    assertThat(metacard.getMetadata(), is(metadataXml));
    assertThat(metacard.getAttribute(Media.DURATION), is(nullValue()));
  }

  @Test
  public void testIntAttribute() {

    Set<AttributeDescriptor> extraAttributes = new HashSet<>();
    extraAttributes.add(
        new AttributeDescriptorImpl(
            Media.DURATION, false, false, false, false, BasicTypes.INTEGER_TYPE));
    final Metadata metadata = new Metadata();
    String imageLength = "14";
    metadata.add(TIFF.IMAGE_LENGTH, imageLength);
    MetacardTypeImpl extendedMetacardType =
        new MetacardTypeImpl(
            MetacardImpl.BASIC_METACARD.getName(), MetacardImpl.BASIC_METACARD, extraAttributes);
    final String id = "id";
    final String metadataXml = "<xml>test</xml>";

    Metacard metacard =
        MetacardCreator.createMetacard(metadata, id, metadataXml, extendedMetacardType);
    assertThat(metacard.getMetadata(), is(metadataXml));
    assertThat(metacard.getAttribute(Media.HEIGHT).getValue(), is(Integer.valueOf(imageLength)));
  }

  @Test
  public void testMetacardExtended() {
    Metacard metacard =
        createTestMetacard(ImmutableSet.of(createObjectAttr("attr1"), createObjectAttr("attr2")));
    assertThat(metacard.getMetacardType().getName(), is(MetacardImpl.BASIC_METACARD.getName()));

    ImmutableSet<String> attrNames = ImmutableSet.of("attr1", "attr2");
    int count =
        (int)
            metacard.getMetacardType().getAttributeDescriptors().stream()
                .map(AttributeDescriptor::getName)
                .filter(attrNames::contains)
                .count();
    assertThat(count, is(attrNames.size()));
  }

  @Test
  public void testMetacardTitle() {
    final Metadata metadata = new Metadata();

    metadata.add(TikaCoreProperties.TITLE, "metadata title");
    final Metacard metacard =
        MetacardCreator.createMetacard(metadata, null, null, MetacardImpl.BASIC_METACARD, false);

    assertThat(metacard.getTitle(), nullValue());
  }

  @Test
  public void testContributorAdded() {
    final Metadata metadata = new Metadata();

    metadata.add(Office.LAST_AUTHOR, "AnotherFirst AnotherLast");
    metadata.add(TikaCoreProperties.CREATOR, "First Last");
    final Metacard metacard =
        MetacardCreator.createMetacard(metadata, null, null, MetacardImpl.BASIC_METACARD, false);

    assertThat(
        metacard.getAttribute(Contact.CONTRIBUTOR_NAME).getValue().toString(),
        is("AnotherFirst AnotherLast"));
  }

  @Test
  public void testContributorNotAdded() {
    final Metadata metadata = new Metadata();

    metadata.add(Office.LAST_AUTHOR, "First Last");
    metadata.add(TikaCoreProperties.CREATOR, "First Last");
    final Metacard metacard =
        MetacardCreator.createMetacard(metadata, null, null, MetacardImpl.BASIC_METACARD, false);

    assertThat(metacard.getAttribute(Contact.CONTRIBUTOR_NAME), nullValue());
  }

  @Test
  public void testContributorNull() {
    final Metadata metadata = new Metadata();

    final Metacard metacard =
        MetacardCreator.createMetacard(metadata, null, null, MetacardImpl.BASIC_METACARD, false);

    assertThat(metacard.getAttribute(Contact.CONTRIBUTOR_NAME), nullValue());
  }

  private AttributeDescriptorImpl createObjectAttr(String name) {
    return new AttributeDescriptorImpl(name, false, false, false, false, BasicTypes.OBJECT_TYPE);
  }

  private Metacard createTestMetacard(Set<AttributeDescriptor> extraAttributes) {
    final Metadata metadata = new Metadata();

    final String title = "title";
    final String contentType = "content type";
    final String created = "2015-12-31T01:23:45Z";
    final String modified = "2016-01-01T02:34:56Z";
    final String latitude = "12.345";
    final String longitude = "67.891";

    metadata.add(TikaCoreProperties.TITLE, title);
    metadata.add(Metadata.CONTENT_TYPE, contentType);
    metadata.add(TikaCoreProperties.CREATED, created);
    metadata.add(TikaCoreProperties.MODIFIED, modified);
    metadata.add(Metadata.LATITUDE, latitude);
    metadata.add(Metadata.LONGITUDE, longitude);

    final String id = "id";
    final String metadataXml = "<xml>test</xml>";

    final Metacard metacard;
    if (CollectionUtils.isEmpty(extraAttributes)) {
      metacard =
          MetacardCreator.createMetacard(metadata, id, metadataXml, MetacardImpl.BASIC_METACARD);
    } else {
      MetacardTypeImpl extendedMetacardType =
          new MetacardTypeImpl(
              MetacardImpl.BASIC_METACARD.getName(), MetacardImpl.BASIC_METACARD, extraAttributes);
      metacard = MetacardCreator.createMetacard(metadata, id, metadataXml, extendedMetacardType);
    }

    assertThat(metacard, notNullValue());
    assertThat(metacard.getTitle(), is(title));
    assertThat(metacard.getContentTypeName(), is(contentType));
    assertThat(convertDate(metacard.getCreatedDate()), is(created));
    assertThat(convertDate(metacard.getModifiedDate()), is(modified));
    assertThat(metacard.getLocation(), is(String.format("POINT(%s %s)", longitude, latitude)));
    assertThat(metacard.getId(), is(id));
    assertThat(metacard.getMetadata(), is(metadataXml));

    return metacard;
  }

  private String convertDate(final Date date) {
    final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    df.setTimeZone(TimeZone.getTimeZone("UTC"));
    return df.format(date);
  }
}
