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
package org.codice.ddf.spatial.kml.transformer;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Core;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import org.junit.Test;

public class KmlStyleMapTest {

  private static final String DEFAULT_STYLE_URL = "http://example.com/style#myStyle";

  @Test
  public void testGetStyleForMetacardStringAttribute() {
    Metacard metacard = new MockMetacard();
    KmlStyleMap mapper = new KmlStyleMap();
    mapper.addMapEntry(
        new KmlStyleMapEntryImpl(
            Metacard.CONTENT_TYPE, MockMetacard.DEFAULT_TYPE, DEFAULT_STYLE_URL));
    assertThat(mapper.getStyleForMetacard(metacard), is(DEFAULT_STYLE_URL));
  }

  @Test
  public void testGetStyleForMetacardBooleanAttribute() {
    Metacard metacard = new MockMetacard(AttributeFormat.BOOLEAN.toString(), true);
    KmlStyleMap mapper = new KmlStyleMap();
    mapper.addMapEntry(
        new KmlStyleMapEntryImpl(
            AttributeFormat.BOOLEAN.toString(), String.valueOf(true), DEFAULT_STYLE_URL));
    assertThat(mapper.getStyleForMetacard(metacard), is(DEFAULT_STYLE_URL));
  }

  @Test
  public void testGetStyleForMetacardXmlAttribute() {
    Metacard metacard = new MockMetacard();
    KmlStyleMap mapper = new KmlStyleMap();
    mapper.addMapEntry(
        new KmlStyleMapEntryImpl(Core.METADATA, MockMetacard.DEFAULT_METADATA, DEFAULT_STYLE_URL));
    assertThat(mapper.getStyleForMetacard(metacard), is(DEFAULT_STYLE_URL));
  }

  @Test
  public void testGetStyleForMetacardGeoAttribute() {
    Metacard metacard = new MockMetacard();
    KmlStyleMap mapper = new KmlStyleMap();
    mapper.addMapEntry(
        new KmlStyleMapEntryImpl(Core.LOCATION, MockMetacard.DEFAULT_LOCATION, DEFAULT_STYLE_URL));
    assertThat(mapper.getStyleForMetacard(metacard), is(DEFAULT_STYLE_URL));
  }

  @Test
  public void testGetStyleForMetacardDateAttribute() {
    Metacard metacard = new MockMetacard();
    KmlStyleMap mapper = new KmlStyleMap();
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    String date = dateFormat.format(MockMetacard.DEFAULT_DATE);
    mapper.addMapEntry(new KmlStyleMapEntryImpl(Metacard.EFFECTIVE, date, DEFAULT_STYLE_URL));
    assertThat(mapper.getStyleForMetacard(metacard), is(DEFAULT_STYLE_URL));
  }

  @Test
  public void testGetStyleForMetacardShortAttribute() {
    Short testShort = Short.valueOf("2");
    Metacard metacard = new MockMetacard(AttributeFormat.SHORT.toString(), testShort);
    KmlStyleMap mapper = new KmlStyleMap();
    mapper.addMapEntry(
        new KmlStyleMapEntryImpl(
            AttributeFormat.SHORT.toString(), String.valueOf(testShort), DEFAULT_STYLE_URL));
    assertThat(mapper.getStyleForMetacard(metacard), is(DEFAULT_STYLE_URL));
  }

  @Test
  public void testGetStyleForMetacardIntegerAttribute() {
    Integer testInteger = Integer.valueOf("2");
    Metacard metacard = new MockMetacard(AttributeFormat.INTEGER.toString(), testInteger);
    KmlStyleMap mapper = new KmlStyleMap();
    mapper.addMapEntry(
        new KmlStyleMapEntryImpl(
            AttributeFormat.INTEGER.toString(), String.valueOf(testInteger), DEFAULT_STYLE_URL));
    assertThat(mapper.getStyleForMetacard(metacard), is(DEFAULT_STYLE_URL));
  }

  @Test
  public void testGetStyleForMetacardLongAttribute() {
    Long testLong = Long.valueOf("2000000");
    Metacard metacard = new MockMetacard(AttributeFormat.LONG.toString(), testLong);
    KmlStyleMap mapper = new KmlStyleMap();
    mapper.addMapEntry(
        new KmlStyleMapEntryImpl(
            AttributeFormat.LONG.toString(), String.valueOf(testLong), DEFAULT_STYLE_URL));
    assertThat(mapper.getStyleForMetacard(metacard), is(DEFAULT_STYLE_URL));
  }

  @Test
  public void testGetStyleForMetacardFloatAttribute() {
    Float testFloat = Float.valueOf("2.0");
    Metacard metacard = new MockMetacard(AttributeFormat.FLOAT.toString(), testFloat);
    KmlStyleMap mapper = new KmlStyleMap();
    mapper.addMapEntry(
        new KmlStyleMapEntryImpl(
            AttributeFormat.FLOAT.toString(), String.valueOf(testFloat), DEFAULT_STYLE_URL));
    assertThat(mapper.getStyleForMetacard(metacard), is(DEFAULT_STYLE_URL));
  }

  @Test
  public void testGetStyleForMetacardDoubleAttribute() {
    Double testDouble = Double.valueOf("2");
    Metacard metacard = new MockMetacard(AttributeFormat.DOUBLE.toString(), testDouble);
    KmlStyleMap mapper = new KmlStyleMap();
    mapper.addMapEntry(
        new KmlStyleMapEntryImpl(
            AttributeFormat.DOUBLE.toString(), String.valueOf(testDouble), DEFAULT_STYLE_URL));
    assertThat(mapper.getStyleForMetacard(metacard), is(DEFAULT_STYLE_URL));
  }

  @Test
  public void testGetStyleForMetacardNoAttributeMatch() {
    MetacardImpl metacard = new MetacardImpl();
    KmlStyleMap mapper = new KmlStyleMap();
    assertThat(mapper.getStyleForMetacard(metacard), is(""));
  }

  @Test
  public void testGetStyleForMetacardBinaryNoMatch() {
    Metacard metacard =
        new MockMetacard(AttributeFormat.BINARY.toString(), MockMetacard.DEFAULT_LOCATION);
    KmlStyleMap mapper = new KmlStyleMap();
    mapper.addMapEntry(
        new KmlStyleMapEntryImpl(
            AttributeFormat.BINARY.toString(), MockMetacard.DEFAULT_LOCATION, DEFAULT_STYLE_URL));
    assertThat(mapper.getStyleForMetacard(metacard), is(""));
  }

  @Test
  public void testGetStyleForMetacardObjectNoMatch() {
    Metacard metacard =
        new MockMetacard(AttributeFormat.OBJECT.toString(), MockMetacard.DEFAULT_LOCATION);
    KmlStyleMap mapper = new KmlStyleMap();
    mapper.addMapEntry(
        new KmlStyleMapEntryImpl(
            AttributeFormat.OBJECT.toString(), MockMetacard.DEFAULT_LOCATION, DEFAULT_STYLE_URL));
    assertThat(mapper.getStyleForMetacard(metacard), is(""));
  }

  @Test
  public void testGetStyleForMetacardBySourceId() {
    Metacard metacard = new MockMetacard(null, null);
    KmlStyleMap mapper = new KmlStyleMap();
    mapper.addMapEntry(
        new KmlStyleMapEntryImpl(
            Core.SOURCE_ID, MockMetacard.DEFAULT_SOURCE_ID, DEFAULT_STYLE_URL));
    assertThat(mapper.getStyleForMetacard(metacard), is(DEFAULT_STYLE_URL));
  }
}
