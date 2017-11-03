/*
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package org.codice.ddf.spatial.kml.transformer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.transform.CatalogTransformerException;
import java.io.InputStream;
import java.util.HashSet;
import org.junit.Test;

public class TestKmzInputTransformer {

  private static final String EXPECTED_LONG = "-122.0822035425683";
  private static final String EXPECTED_LAT = "37.42228990140251";
  private static final String EXPECTED_TYPE = "POINT";

  @Test
  public void testKmzWithKmlInside() throws Exception {
    KmzInputTransformer kmzInputTransformer =
        new KmzInputTransformer(
            new KmlInputTransformer(new MetacardTypeImpl("kmlMetacardType", new HashSet<>())));
    InputStream stream = TestKmzInputTransformer.class.getResourceAsStream("/kml_inside.kmz");
    Metacard metacard = kmzInputTransformer.transform(stream);
    assertThat(metacard, notNullValue());

    String location = metacard.getLocation();
    assertThat(location, not(isEmptyOrNullString()));
    assertThat(location, containsString(EXPECTED_LAT));
    assertThat(location, containsString(EXPECTED_LONG));
    assertThat(location, containsString(EXPECTED_TYPE));
  }

  @Test
  public void testKmzWithMultipleKmlAndOtherStuffInside() throws Exception {
    KmzInputTransformer kmzInputTransformer =
        new KmzInputTransformer(
            new KmlInputTransformer(new MetacardTypeImpl("kmlMetacardType", new HashSet<>())));
    InputStream stream =
        TestKmzInputTransformer.class.getResourceAsStream("/kmzWithMultipleKml.kmz");
    Metacard metacard = kmzInputTransformer.transform(stream);
    assertThat(metacard, notNullValue());

    String location = metacard.getLocation();
    assertThat(location, not(isEmptyOrNullString()));
    assertThat(location, containsString(EXPECTED_LAT));
    assertThat(location, containsString(EXPECTED_LONG));
    assertThat(location, containsString(EXPECTED_TYPE));
  }

  @Test
  public void testSameKmlInSingleAndMultipleKmzGeneratesSameValues() throws Exception {
    KmzInputTransformer kmzInputTransformer =
        new KmzInputTransformer(
            new KmlInputTransformer(new MetacardTypeImpl("kmlMetacardType", new HashSet<>())));
    InputStream multipleKmlStream =
        TestKmzInputTransformer.class.getResourceAsStream("/kmzWithMultipleKml.kmz");
    InputStream singleKmlStream =
        TestKmzInputTransformer.class.getResourceAsStream("/kml_inside.kmz");
    Metacard singleKmlMetacard = kmzInputTransformer.transform(singleKmlStream);
    assertThat(singleKmlMetacard, notNullValue());

    Metacard multipleKmlMetacard = kmzInputTransformer.transform(multipleKmlStream);
    assertThat(multipleKmlMetacard, notNullValue());

    assertThat(
        ((MetacardImpl) multipleKmlMetacard).getDescription(),
        equalTo(((MetacardImpl) singleKmlMetacard).getDescription()));
    assertThat(multipleKmlMetacard.getMetadata(), equalTo(singleKmlMetacard.getMetadata()));
    assertThat(multipleKmlMetacard.getLocation(), equalTo(singleKmlMetacard.getLocation()));
  }

  @Test(expected = CatalogTransformerException.class)
  public void testKmzWithoutKmlInside() throws Exception {
    KmzInputTransformer kmzInputTransformer =
        new KmzInputTransformer(mock(KmlInputTransformer.class));
    InputStream stream = TestKmzInputTransformer.class.getResourceAsStream("/no_kml_inside.kmz");
    kmzInputTransformer.transform(stream);
  }
}
