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
package ddf.catalog.data.impl.types;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import ddf.catalog.data.types.Associations;
import ddf.catalog.data.types.Contact;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.DateTime;
import ddf.catalog.data.types.Location;
import ddf.catalog.data.types.Media;
import ddf.catalog.data.types.Topic;
import ddf.catalog.data.types.Validation;
import ddf.catalog.data.types.Version;
import org.junit.Test;

public class TypeAttributesTest {

  private static final AssociationsAttributes ASSOCIATIONS_ATTRIBUTES =
      new AssociationsAttributes();

  private static final ContactAttributes CONTACT_ATTRIBUTES = new ContactAttributes();

  private static final DateTimeAttributes DATE_TIME_ATTRIBUTES = new DateTimeAttributes();

  private static final VersionAttributes VERSION_ATTRIBUTES = new VersionAttributes();

  private static final LocationAttributes LOCATION_ATTRIBUTES = new LocationAttributes();

  private static final MediaAttributes MEDIA_ATTRIBUTES = new MediaAttributes();

  private static final TopicAttributes TOPIC_ATTRIBUTES = new TopicAttributes();

  private static final ValidationAttributes VALIDATION_ATTRIBUTES = new ValidationAttributes();

  private static final CoreAttributes CORE_ATTRIBUTES = new CoreAttributes();

  @Test
  public void testContactAttributes() {
    assertThat(CONTACT_ATTRIBUTES.getName(), is("contact"));
    assertThat(
        CONTACT_ATTRIBUTES.getAttributeDescriptor(Contact.CONTRIBUTOR_ADDRESS), notNullValue());
    assertThat(CONTACT_ATTRIBUTES.getAttributeDescriptor(Location.ALTITUDE), nullValue());
  }

  @Test
  public void testCoreAttributes() {
    assertThat(CORE_ATTRIBUTES.getName(), is("core"));
    assertThat(CORE_ATTRIBUTES.getAttributeDescriptor(Core.CHECKSUM), notNullValue());
    assertThat(CORE_ATTRIBUTES.getAttributeDescriptor(Location.ALTITUDE), nullValue());
  }

  @Test
  public void testDateTimeAttributes() {
    assertThat(DATE_TIME_ATTRIBUTES.getName(), is("datetime"));
    assertThat(DATE_TIME_ATTRIBUTES.getAttributeDescriptor(DateTime.NAME), notNullValue());
    assertThat(DATE_TIME_ATTRIBUTES.getAttributeDescriptor(Location.ALTITUDE), nullValue());
  }

  @Test
  public void testVersionAttributes() {
    assertThat(VERSION_ATTRIBUTES.getName(), is("history"));
    assertThat(VERSION_ATTRIBUTES.getAttributeDescriptor(Version.ACTION), notNullValue());
    assertThat(VERSION_ATTRIBUTES.getAttributeDescriptor(Location.ALTITUDE), nullValue());
  }

  @Test
  public void testMediaAttributes() {
    assertThat(MEDIA_ATTRIBUTES.getName(), is("media"));
    assertThat(MEDIA_ATTRIBUTES.getAttributeDescriptor(Media.BITS_PER_SAMPLE), notNullValue());
    assertThat(MEDIA_ATTRIBUTES.getAttributeDescriptor(Version.ACTION), nullValue());
  }

  @Test
  public void testTopicAttributes() {
    assertThat(TOPIC_ATTRIBUTES.getName(), is("topic"));
    assertThat(TOPIC_ATTRIBUTES.getAttributeDescriptor(Topic.CATEGORY), notNullValue());
    assertThat(TOPIC_ATTRIBUTES.getAttributeDescriptor(Version.ACTION), nullValue());
  }

  @Test
  public void testLocationAttributes() {
    assertThat(LOCATION_ATTRIBUTES.getName(), is("location"));
    assertThat(LOCATION_ATTRIBUTES.getAttributeDescriptor(Location.ALTITUDE), notNullValue());
    assertThat(LOCATION_ATTRIBUTES.getAttributeDescriptor(Version.ACTION), nullValue());
  }

  @Test
  public void testValidationAttributes() {
    assertThat(VALIDATION_ATTRIBUTES.getName(), is("validation"));
    assertThat(
        VALIDATION_ATTRIBUTES.getAttributeDescriptor(Validation.VALIDATION_ERRORS), notNullValue());
    assertThat(VALIDATION_ATTRIBUTES.getAttributeDescriptor(Version.ACTION), nullValue());
  }

  @Test
  public void testAssociationsAttributes() {
    assertThat(ASSOCIATIONS_ATTRIBUTES.getName(), is("associations"));
    assertThat(
        ASSOCIATIONS_ATTRIBUTES.getAttributeDescriptor(Associations.DERIVED), notNullValue());
    assertThat(ASSOCIATIONS_ATTRIBUTES.getAttributeDescriptor(Version.ACTION), nullValue());
  }
}
