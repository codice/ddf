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

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
import ddf.action.Action;
import ddf.action.ActionProvider;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.transform.CatalogTransformerException;
import de.micromata.opengis.kml.v_2_2_0.Data;
import de.micromata.opengis.kml.v_2_2_0.LineString;
import de.micromata.opengis.kml.v_2_2_0.MultiGeometry;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Point;
import de.micromata.opengis.kml.v_2_2_0.Polygon;
import de.micromata.opengis.kml.v_2_2_0.TimeSpan;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.spatial.kml.util.KmlMarshaller;
import org.custommonkey.xmlunit.NamespaceContext;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.xml.sax.SAXException;

public class KMLTransformerImplTest {

  private static final String DEFAULT_STYLE_LOCATION = "/kml-styling/defaultStyling.kml";

  private static final String ID = "1234567890";

  private static final String TITLE = "myTitle";

  private static final String POINT_WKT = "POINT (-110.00540924072266 34.265270233154297)";

  private static final String LINESTRING_WKT = "LINESTRING (1 1,2 1)";

  private static final String POLYGON_WKT = "POLYGON ((1 1,2 1,2 2,1 2,1 1))";

  private static final String MULTIPOINT_WKT = "MULTIPOINT ((1 1), (0 0), (2 2))";

  private static final String MULTILINESTRING_WKT = "MULTILINESTRING ((1 1, 2 1), (1 2, 0 0))";

  private static final String MULTIPOLYGON_WKT =
      "MULTIPOLYGON (((1 1,2 1,2 2,1 2,1 1)), ((0 0,1 1,2 0,0 0)))";

  private static final String GEOMETRYCOLLECTION_WKT =
      "GEOMETRYCOLLECTION (" + POINT_WKT + ", " + LINESTRING_WKT + ", " + POLYGON_WKT + ")";

  private static final String ACTION_URL = "http://example.com/source/id?transform=resource";

  private static final ImmutableSet<String> METACARD_TAGS = ImmutableSet.of("item1", "item2");

  private static final String METACARD_DATE_STRING = "3900-12-21T00:00:00.000+0000";

  private static final String METACARD_ID = "1234567890";

  private static final String METACARD_METADATA = "<xml>Metadata</xml>";

  private static final String METACARD_VERSION = "myVersion";

  private static final String METACARD_TYPE = "myContentType";

  private static final String METACARD_SOURCE = "sourceID";

  private static final String METACARD_TITLE = "myTitle";

  private static Date metacardDate = new Date(2000, 11, 21);

  private static BundleContext mockContext = mock(BundleContext.class);

  private static Bundle mockBundle = mock(Bundle.class);

  private static KMLTransformerImpl kmlTransformer;

  private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

  @BeforeClass
  public static void setUp() throws IOException {
    metacardDate =
        new Date(
            metacardDate.getTime()
                + Calendar.getInstance().getTimeZone().getOffset(metacardDate.getTime()));

    when(mockContext.getBundle()).thenReturn(mockBundle);
    URL url = KMLTransformerImplTest.class.getResource(DEFAULT_STYLE_LOCATION);
    when(mockBundle.getResource(any(String.class))).thenReturn(url);

    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    ActionProvider mockActionProvider = mock(ActionProvider.class);
    Action mockAction = mock(Action.class);
    when(mockActionProvider.getAction(any(Metacard.class))).thenReturn(mockAction);
    when(mockAction.getUrl()).thenReturn(new URL(ACTION_URL));
    kmlTransformer =
        new KMLTransformerImpl(
            mockContext,
            DEFAULT_STYLE_LOCATION,
            new KmlStyleMap(),
            mockActionProvider,
            new KmlMarshaller());
  }

  @Before
  public void setupXpath() {
    NamespaceContext ctx =
        new SimpleNamespaceContext(singletonMap("m", "http://www.opengis.net/kml/2.2"));
    XMLUnit.setXpathNamespaceContext(ctx);
  }

  @Test(expected = CatalogTransformerException.class)
  public void testPerformDefaultTransformationNoLocation() throws CatalogTransformerException {
    Metacard metacard = createMockMetacard();
    kmlTransformer.performDefaultTransformation(metacard);
  }

  @Test
  public void testPerformDefaultTransformationPointLocation() throws CatalogTransformerException {
    MetacardImpl metacard = createMockMetacard();
    metacard.setLocation(POINT_WKT);
    Placemark placemark = kmlTransformer.performDefaultTransformation(metacard);
    assertThat(placemark.getId(), is("Placemark-" + ID));
    assertThat(placemark.getName(), is(TITLE));
    assertThat(placemark.getStyleSelector().isEmpty(), is(true));
    assertThat(placemark.getStyleUrl(), nullValue());
    assertThat(placemark.getTimePrimitive(), instanceOf(TimeSpan.class));
    TimeSpan timeSpan = (TimeSpan) placemark.getTimePrimitive();
    assertThat(timeSpan.getBegin(), is(dateFormat.format(metacard.getEffectiveDate())));
    assertThat(placemark.getGeometry(), instanceOf(Point.class));
  }

  @Test
  public void testPerformDefaultTransformationLineStringLocation()
      throws CatalogTransformerException {
    MetacardImpl metacard = createMockMetacard();
    metacard.setLocation(LINESTRING_WKT);
    Placemark placemark = kmlTransformer.performDefaultTransformation(metacard);
    assertThat(placemark.getId(), is("Placemark-" + ID));
    assertThat(placemark.getName(), is(TITLE));
    assertThat(placemark.getTimePrimitive(), instanceOf(TimeSpan.class));
    TimeSpan timeSpan = (TimeSpan) placemark.getTimePrimitive();
    assertThat(timeSpan.getBegin(), is(dateFormat.format(metacard.getEffectiveDate())));
    assertThat(placemark.getGeometry(), instanceOf(MultiGeometry.class));
    MultiGeometry multiGeo = (MultiGeometry) placemark.getGeometry();
    assertThat(multiGeo.getGeometry().size(), is(2));
    assertThat(multiGeo.getGeometry().get(0), instanceOf(Point.class));
    assertThat(multiGeo.getGeometry().get(1), instanceOf(LineString.class));
  }

  @Test
  public void testPerformDefaultTransformationPolygonLocation() throws CatalogTransformerException {
    MetacardImpl metacard = createMockMetacard();
    metacard.setLocation(POLYGON_WKT);
    Placemark placemark = kmlTransformer.performDefaultTransformation(metacard);
    assertThat(placemark.getId(), is("Placemark-" + ID));
    assertThat(placemark.getName(), is(TITLE));
    assertThat(placemark.getTimePrimitive(), instanceOf(TimeSpan.class));
    TimeSpan timeSpan = (TimeSpan) placemark.getTimePrimitive();
    assertThat(timeSpan.getBegin(), is(dateFormat.format(metacard.getEffectiveDate())));
    assertThat(placemark.getGeometry(), instanceOf(MultiGeometry.class));
    MultiGeometry multiGeo = (MultiGeometry) placemark.getGeometry();
    assertThat(multiGeo.getGeometry().size(), is(2));
    assertThat(multiGeo.getGeometry().get(0), instanceOf(Point.class));
    assertThat(multiGeo.getGeometry().get(1), instanceOf(Polygon.class));
  }

  @Test
  public void testPerformDefaultTransformationMultiPointLocation()
      throws CatalogTransformerException {
    MetacardImpl metacard = createMockMetacard();
    metacard.setLocation(MULTIPOINT_WKT);
    Placemark placemark = kmlTransformer.performDefaultTransformation(metacard);
    assertThat(placemark.getId(), is("Placemark-" + ID));
    assertThat(placemark.getName(), is(TITLE));
    assertThat(placemark.getTimePrimitive(), instanceOf(TimeSpan.class));
    TimeSpan timeSpan = (TimeSpan) placemark.getTimePrimitive();
    assertThat(timeSpan.getBegin(), is(dateFormat.format(metacard.getEffectiveDate())));
    assertThat(placemark.getGeometry(), instanceOf(MultiGeometry.class));
    MultiGeometry multiGeo = (MultiGeometry) placemark.getGeometry();
    assertThat(multiGeo.getGeometry().size(), is(2));
    assertThat(multiGeo.getGeometry().get(0), instanceOf(Point.class));
    assertThat(multiGeo.getGeometry().get(1), instanceOf(MultiGeometry.class));
    MultiGeometry multiPoint = (MultiGeometry) multiGeo.getGeometry().get(1);
    assertThat(multiPoint.getGeometry().size(), is(3));
    assertThat(multiPoint.getGeometry().get(0), instanceOf(Point.class));
    assertThat(multiPoint.getGeometry().get(1), instanceOf(Point.class));
    assertThat(multiPoint.getGeometry().get(2), instanceOf(Point.class));
  }

  @Test
  public void testPerformDefaultTransformationMultiLineStringLocation()
      throws CatalogTransformerException {
    MetacardImpl metacard = createMockMetacard();
    metacard.setLocation(MULTILINESTRING_WKT);
    Placemark placemark = kmlTransformer.performDefaultTransformation(metacard);
    assertThat(placemark.getId(), is("Placemark-" + ID));
    assertThat(placemark.getName(), is(TITLE));
    assertThat(placemark.getTimePrimitive(), instanceOf(TimeSpan.class));
    TimeSpan timeSpan = (TimeSpan) placemark.getTimePrimitive();
    assertThat(timeSpan.getBegin(), is(dateFormat.format(metacard.getEffectiveDate())));
    assertThat(placemark.getGeometry(), instanceOf(MultiGeometry.class));
    MultiGeometry multiGeo = (MultiGeometry) placemark.getGeometry();
    assertThat(multiGeo.getGeometry().size(), is(2));
    assertThat(multiGeo.getGeometry().get(0), instanceOf(Point.class));
    assertThat(multiGeo.getGeometry().get(1), instanceOf(MultiGeometry.class));
    MultiGeometry multiLineString = (MultiGeometry) multiGeo.getGeometry().get(1);
    assertThat(multiLineString.getGeometry().size(), is(2));
    assertThat(multiLineString.getGeometry().get(0), instanceOf(LineString.class));
    assertThat(multiLineString.getGeometry().get(1), instanceOf(LineString.class));
  }

  @Test
  public void testPerformDefaultTransformationExtendedData()
      throws CatalogTransformerException, DateTimeParseException {
    MetacardImpl metacard = createMockMetacard();

    metacard.setLocation(POINT_WKT);
    metacard.setCreatedDate(metacardDate);
    metacard.setEffectiveDate(metacardDate);
    metacard.setExpirationDate(metacardDate);
    metacard.setModifiedDate(metacardDate);
    metacard.setTags(METACARD_TAGS);

    final Set<AttributeDescriptor> attributeDescriptors =
        metacard.getMetacardType().getAttributeDescriptors();

    Placemark placemark = kmlTransformer.performDefaultTransformation(metacard);

    final List<Data> dataList = placemark.getExtendedData().getData();

    int dataCount = 0;
    for (AttributeDescriptor attributeDescriptor : attributeDescriptors) {
      final String attributeName = attributeDescriptor.getName();
      final Attribute attribute = metacard.getAttribute(attributeName);
      if (attribute != null) {
        dataCount++;
      }
    }

    assertThat(dataList.size(), is(dataCount));

    for (Data data : dataList) {
      switch (data.getName()) {
        case Core.ID:
          assertThat(data.getValue(), is(METACARD_ID));
          break;
        case Core.TITLE:
          assertThat(data.getValue(), is(METACARD_TITLE));
          break;
        case Core.LOCATION:
          assertThat(data.getValue(), is(POINT_WKT));
          break;
        case Metacard.CONTENT_TYPE:
          assertThat(data.getValue(), is(METACARD_TYPE));
          break;
        case Metacard.CONTENT_TYPE_VERSION:
          assertThat(data.getValue(), is(METACARD_VERSION));
          break;
        case Core.METADATA:
          assertThat(data.getValue(), is(METACARD_METADATA));
          break;
        case Core.METACARD_TAGS:
          assertThat(
              data.getValue(),
              is(METACARD_TAGS.asList().get(0) + "," + METACARD_TAGS.asList().get(1)));
          break;
        case Core.MODIFIED:
        case Metacard.EFFECTIVE:
        case Core.EXPIRATION:
        case Core.CREATED:
          assertThat(data.getValue(), is(METACARD_DATE_STRING));
          break;
        default:
          throw new IllegalArgumentException(
              String.format("Data %s was not expected", data.getName()));
      }
    }
  }

  @Test
  public void testPerformDefaultTransformationMultiPolygonLocation()
      throws CatalogTransformerException {
    MetacardImpl metacard = createMockMetacard();
    metacard.setLocation(MULTIPOLYGON_WKT);
    Placemark placemark = kmlTransformer.performDefaultTransformation(metacard);
    assertThat(placemark.getId(), is("Placemark-" + ID));
    assertThat(placemark.getName(), is(TITLE));
    assertThat(placemark.getTimePrimitive(), instanceOf(TimeSpan.class));
    TimeSpan timeSpan = (TimeSpan) placemark.getTimePrimitive();
    assertThat(timeSpan.getBegin(), is(dateFormat.format(metacard.getEffectiveDate())));
    assertThat(placemark.getGeometry(), instanceOf(MultiGeometry.class));
    MultiGeometry multiGeo = (MultiGeometry) placemark.getGeometry();
    assertThat(multiGeo.getGeometry().size(), is(2));
    assertThat(multiGeo.getGeometry().get(0), instanceOf(Point.class));
    assertThat(multiGeo.getGeometry().get(1), instanceOf(MultiGeometry.class));
    MultiGeometry multiPolygon = (MultiGeometry) multiGeo.getGeometry().get(1);
    assertThat(multiPolygon.getGeometry().size(), is(2));
    assertThat(multiPolygon.getGeometry().get(0), instanceOf(Polygon.class));
    assertThat(multiPolygon.getGeometry().get(1), instanceOf(Polygon.class));
  }

  @Test
  public void testPerformDefaultTransformationGeometryCollectionLocation()
      throws CatalogTransformerException {
    MetacardImpl metacard = createMockMetacard();
    metacard.setLocation(GEOMETRYCOLLECTION_WKT);
    Placemark placemark = kmlTransformer.performDefaultTransformation(metacard);
    assertThat(placemark.getId(), is("Placemark-" + ID));
    assertThat(placemark.getName(), is(TITLE));
    assertThat(placemark.getTimePrimitive(), instanceOf(TimeSpan.class));
    TimeSpan timeSpan = (TimeSpan) placemark.getTimePrimitive();
    assertThat(timeSpan.getBegin(), is(dateFormat.format(metacard.getEffectiveDate())));
    assertThat(placemark.getGeometry(), instanceOf(MultiGeometry.class));
    MultiGeometry multiGeo = (MultiGeometry) placemark.getGeometry();
    assertThat(multiGeo.getGeometry().size(), is(2));
    assertThat(multiGeo.getGeometry().get(0), instanceOf(Point.class));
    assertThat(multiGeo.getGeometry().get(1), instanceOf(MultiGeometry.class));
    MultiGeometry multiGeo2 = (MultiGeometry) multiGeo.getGeometry().get(1);
    assertThat(multiGeo2.getGeometry().size(), is(3));
    assertThat(multiGeo2.getGeometry().get(0), instanceOf(Point.class));
    assertThat(multiGeo2.getGeometry().get(1), instanceOf(LineString.class));
    assertThat(multiGeo2.getGeometry().get(2), instanceOf(Polygon.class));
  }

  @Test
  public void testTransformMetacardGetsDefaultStyle()
      throws CatalogTransformerException, IOException {
    MetacardImpl metacard = createMockMetacard();
    metacard.setLocation(POINT_WKT);
    BinaryContent content = kmlTransformer.transform(metacard, null);
    assertThat(content.getMimeTypeValue(), is(KMLTransformerImpl.KML_MIMETYPE.toString()));
    IOUtils.toString(content.getInputStream(), StandardCharsets.UTF_8);
  }

  @Test
  public void testTransformMetacardFromUpstreamResponse()
      throws CatalogTransformerException, IOException, XpathException, SAXException {

    MetacardImpl metacard = createMockMetacard();
    metacard.setLocation(POINT_WKT);

    Result result = new ResultImpl(metacard);

    SourceResponseImpl sourceResponse = new SourceResponseImpl(null, singletonList(result));
    BinaryContent content = kmlTransformer.transform(sourceResponse, emptyMap());
    assertThat(content.getMimeTypeValue(), is(KMLTransformerImpl.KML_MIMETYPE.toString()));
    final String kmlString = IOUtils.toString(content.getInputStream(), StandardCharsets.UTF_8);

    assertXpathEvaluatesTo("Results (1)", "/m:kml/m:Document/m:name", kmlString);
    assertXpathExists("//m:Placemark[@id='Placemark-1234567890']", kmlString);
    assertXpathEvaluatesTo("myTitle", "//m:Placemark/m:name", kmlString);
  }

  private MetacardImpl createMockMetacard() {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setContentTypeName(METACARD_TYPE);
    metacard.setContentTypeVersion(METACARD_VERSION);
    metacard.setCreatedDate(Calendar.getInstance().getTime());
    metacard.setEffectiveDate(Calendar.getInstance().getTime());
    metacard.setExpirationDate(Calendar.getInstance().getTime());
    metacard.setId(METACARD_ID);
    // metacard.setLocation(wkt);
    metacard.setMetadata(METACARD_METADATA);
    metacard.setModifiedDate(Calendar.getInstance().getTime());
    // metacard.setResourceSize("10MB");
    // metacard.setResourceURI(uri)
    metacard.setSourceId(METACARD_SOURCE);
    metacard.setTitle(METACARD_TITLE);
    return metacard;
  }
}
