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
package org.codice.ddf.spatial.kml.transformer;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import ddf.action.Action;
import ddf.action.ActionProvider;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.transform.CatalogTransformerException;
import de.micromata.opengis.kml.v_2_2_0.LineString;
import de.micromata.opengis.kml.v_2_2_0.MultiGeometry;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Point;
import de.micromata.opengis.kml.v_2_2_0.Polygon;
import de.micromata.opengis.kml.v_2_2_0.TimeSpan;

public class TestKMLTransformerImpl {

    private static BundleContext mockContext = mock(BundleContext.class);

    private static Bundle mockBundle = mock(Bundle.class);

    private static final String defaultStyleLocation = "/kml-styling/defaultStyling.kml";

    private static KMLTransformerImpl kmlTransformer;

    private static final String ID = "1234567890";

    private static final String TITLE = "myTitle";

    private static final String POINT_WKT = "POINT (-110.00540924072266 34.265270233154297)";

    private static final String LINESTRING_WKT = "LINESTRING (1 1,2 1)";

    private static final String POLYGON_WKT = "POLYGON ((1 1,2 1,2 2,1 2,1 1))";

    private static final String MULTIPOINT_WKT = "MULTIPOINT ((1 1), (0 0), (2 2))";

    private static final String MULTILINESTRING_WKT = "MULTILINESTRING ((1 1, 2 1), (1 2, 0 0))";

    private static final String MULTIPOLYGON_WKT = "MULTIPOLYGON (((1 1,2 1,2 2,1 2,1 1)), ((0 0,1 1,2 0,0 0)))";

    private static final String GEOMETRYCOLLECTION_WKT = "GEOMETRYCOLLECTION (" + POINT_WKT + ", "
            + LINESTRING_WKT + ", " + POLYGON_WKT + ")";

    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    private static final String ACTION_URL = "http://example.com/source/id?transform=resource";

    private static ActionProvider mockActionProvider;
    private static Action mockAction;

    @BeforeClass
    public static void setUp() throws IOException {
        when(mockContext.getBundle()).thenReturn(mockBundle);
        URL url = TestKMLTransformerImpl.class.getResource(defaultStyleLocation);
        when(mockBundle.getResource(any(String.class))).thenReturn(url);

        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        mockActionProvider = mock(ActionProvider.class);
        mockAction = mock(Action.class);
        when(mockActionProvider.getAction(any(Metacard.class))).thenReturn(mockAction);
        when(mockAction.getUrl()).thenReturn(new URL(ACTION_URL));
        kmlTransformer = new KMLTransformerImpl(mockContext, defaultStyleLocation,
                new KmlStyleMap(), mockActionProvider);
    }

    @Test(expected = CatalogTransformerException.class)
    public void testPerformDefaultTransformationNoLocation() throws CatalogTransformerException {
        Metacard metacard = createMockMetacard();
        kmlTransformer.performDefaultTransformation(metacard, null);
    }

    @Test
    public void testPerformDefaultTransformationPointLocation() throws CatalogTransformerException {
        MetacardImpl metacard = createMockMetacard();
        metacard.setLocation(POINT_WKT);
        Placemark placemark = kmlTransformer.performDefaultTransformation(metacard, null);
        assertThat(placemark.getId(), is("Placemark-" + ID));
        assertThat(placemark.getName(), is(TITLE));
        assertThat(placemark.getStyleSelector().isEmpty(), is(true));
        assertThat(placemark.getStyleUrl(), nullValue());
        assertThat(placemark.getTimePrimitive(), is(TimeSpan.class));
        TimeSpan timeSpan = (TimeSpan) placemark.getTimePrimitive();
        assertThat(timeSpan.getBegin(), is(dateFormat.format(metacard.getEffectiveDate())));
        assertThat(placemark.getGeometry(), is(Point.class));
    }

    @Test
    public void testPerformDefaultTransformationLineStringLocation()
        throws CatalogTransformerException {
        MetacardImpl metacard = createMockMetacard();
        metacard.setLocation(LINESTRING_WKT);
        Placemark placemark = kmlTransformer.performDefaultTransformation(metacard, null);
        assertThat(placemark.getId(), is("Placemark-" + ID));
        assertThat(placemark.getName(), is(TITLE));
        assertThat(placemark.getTimePrimitive(), is(TimeSpan.class));
        TimeSpan timeSpan = (TimeSpan) placemark.getTimePrimitive();
        assertThat(timeSpan.getBegin(), is(dateFormat.format(metacard.getEffectiveDate())));
        assertThat(placemark.getGeometry(), is(MultiGeometry.class));
        MultiGeometry multiGeo = (MultiGeometry) placemark.getGeometry();
        assertThat(multiGeo.getGeometry().size(), is(2));
        assertThat(multiGeo.getGeometry().get(0), is(Point.class));
        assertThat(multiGeo.getGeometry().get(1), is(LineString.class));
    }

    @Test
    public void testPerformDefaultTransformationPolygonLocation()
        throws CatalogTransformerException {
        MetacardImpl metacard = createMockMetacard();
        metacard.setLocation(POLYGON_WKT);
        Placemark placemark = kmlTransformer.performDefaultTransformation(metacard, null);
        assertThat(placemark.getId(), is("Placemark-" + ID));
        assertThat(placemark.getName(), is(TITLE));
        assertThat(placemark.getTimePrimitive(), is(TimeSpan.class));
        TimeSpan timeSpan = (TimeSpan) placemark.getTimePrimitive();
        assertThat(timeSpan.getBegin(), is(dateFormat.format(metacard.getEffectiveDate())));
        assertThat(placemark.getGeometry(), is(MultiGeometry.class));
        MultiGeometry multiGeo = (MultiGeometry) placemark.getGeometry();
        assertThat(multiGeo.getGeometry().size(), is(2));
        assertThat(multiGeo.getGeometry().get(0), is(Point.class));
        assertThat(multiGeo.getGeometry().get(1), is(Polygon.class));
    }

    @Test
    public void testPerformDefaultTransformationMultiPointLocation()
        throws CatalogTransformerException {
        MetacardImpl metacard = createMockMetacard();
        metacard.setLocation(MULTIPOINT_WKT);
        Placemark placemark = kmlTransformer.performDefaultTransformation(metacard, null);
        assertThat(placemark.getId(), is("Placemark-" + ID));
        assertThat(placemark.getName(), is(TITLE));
        assertThat(placemark.getTimePrimitive(), is(TimeSpan.class));
        TimeSpan timeSpan = (TimeSpan) placemark.getTimePrimitive();
        assertThat(timeSpan.getBegin(), is(dateFormat.format(metacard.getEffectiveDate())));
        assertThat(placemark.getGeometry(), is(MultiGeometry.class));
        MultiGeometry multiGeo = (MultiGeometry) placemark.getGeometry();
        assertThat(multiGeo.getGeometry().size(), is(2));
        assertThat(multiGeo.getGeometry().get(0), is(Point.class));
        assertThat(multiGeo.getGeometry().get(1), is(MultiGeometry.class));
        MultiGeometry multiPoint = (MultiGeometry) multiGeo.getGeometry().get(1);
        assertThat(multiPoint.getGeometry().size(), is(3));
        assertThat(multiPoint.getGeometry().get(0), is(Point.class));
        assertThat(multiPoint.getGeometry().get(1), is(Point.class));
        assertThat(multiPoint.getGeometry().get(2), is(Point.class));
    }

    @Test
    public void testPerformDefaultTransformationMultiLineStringLocation()
        throws CatalogTransformerException {
        MetacardImpl metacard = createMockMetacard();
        metacard.setLocation(MULTILINESTRING_WKT);
        Placemark placemark = kmlTransformer.performDefaultTransformation(metacard, null);
        assertThat(placemark.getId(), is("Placemark-" + ID));
        assertThat(placemark.getName(), is(TITLE));
        assertThat(placemark.getTimePrimitive(), is(TimeSpan.class));
        TimeSpan timeSpan = (TimeSpan) placemark.getTimePrimitive();
        assertThat(timeSpan.getBegin(), is(dateFormat.format(metacard.getEffectiveDate())));
        assertThat(placemark.getGeometry(), is(MultiGeometry.class));
        MultiGeometry multiGeo = (MultiGeometry) placemark.getGeometry();
        assertThat(multiGeo.getGeometry().size(), is(2));
        assertThat(multiGeo.getGeometry().get(0), is(Point.class));
        assertThat(multiGeo.getGeometry().get(1), is(MultiGeometry.class));
        MultiGeometry multiLineString = (MultiGeometry) multiGeo.getGeometry().get(1);
        assertThat(multiLineString.getGeometry().size(), is(2));
        assertThat(multiLineString.getGeometry().get(0), is(LineString.class));
        assertThat(multiLineString.getGeometry().get(1), is(LineString.class));
    }

    @Test
    public void testPerformDefaultTransformationMultiPolygonLocation()
        throws CatalogTransformerException {
        MetacardImpl metacard = createMockMetacard();
        metacard.setLocation(MULTIPOLYGON_WKT);
        Placemark placemark = kmlTransformer.performDefaultTransformation(metacard, null);
        assertThat(placemark.getId(), is("Placemark-" + ID));
        assertThat(placemark.getName(), is(TITLE));
        assertThat(placemark.getTimePrimitive(), is(TimeSpan.class));
        TimeSpan timeSpan = (TimeSpan) placemark.getTimePrimitive();
        assertThat(timeSpan.getBegin(), is(dateFormat.format(metacard.getEffectiveDate())));
        assertThat(placemark.getGeometry(), is(MultiGeometry.class));
        MultiGeometry multiGeo = (MultiGeometry) placemark.getGeometry();
        assertThat(multiGeo.getGeometry().size(), is(2));
        assertThat(multiGeo.getGeometry().get(0), is(Point.class));
        assertThat(multiGeo.getGeometry().get(1), is(MultiGeometry.class));
        MultiGeometry multiPolygon = (MultiGeometry) multiGeo.getGeometry().get(1);
        assertThat(multiPolygon.getGeometry().size(), is(2));
        assertThat(multiPolygon.getGeometry().get(0), is(Polygon.class));
        assertThat(multiPolygon.getGeometry().get(1), is(Polygon.class));
    }

    @Test
    public void testPerformDefaultTransformationGeometryCollectionLocation()
        throws CatalogTransformerException {
        MetacardImpl metacard = createMockMetacard();
        metacard.setLocation(GEOMETRYCOLLECTION_WKT);
        Placemark placemark = kmlTransformer.performDefaultTransformation(metacard, null);
        assertThat(placemark.getId(), is("Placemark-" + ID));
        assertThat(placemark.getName(), is(TITLE));
        assertThat(placemark.getTimePrimitive(), is(TimeSpan.class));
        TimeSpan timeSpan = (TimeSpan) placemark.getTimePrimitive();
        assertThat(timeSpan.getBegin(), is(dateFormat.format(metacard.getEffectiveDate())));
        assertThat(placemark.getGeometry(), is(MultiGeometry.class));
        MultiGeometry multiGeo = (MultiGeometry) placemark.getGeometry();
        assertThat(multiGeo.getGeometry().size(), is(2));
        assertThat(multiGeo.getGeometry().get(0), is(Point.class));
        assertThat(multiGeo.getGeometry().get(1), is(MultiGeometry.class));
        MultiGeometry multiGeo2 = (MultiGeometry) multiGeo.getGeometry().get(1);
        assertThat(multiGeo2.getGeometry().size(), is(3));
        assertThat(multiGeo2.getGeometry().get(0), is(Point.class));
        assertThat(multiGeo2.getGeometry().get(1), is(LineString.class));
        assertThat(multiGeo2.getGeometry().get(2), is(Polygon.class));
    }

    @Test
    public void testTransformMetacardGetsDefaultStyle() throws CatalogTransformerException,
        IOException {
        MetacardImpl metacard = createMockMetacard();
        metacard.setLocation(POINT_WKT);
        BinaryContent content = kmlTransformer.transform(metacard, null);
        assertThat(content.getMimeTypeValue(), is(KMLTransformerImpl.KML_MIMETYPE.toString()));
        String kml = IOUtils.toString(content.getInputStream());
        // TODO - validate the style is there
    }

    private MetacardImpl createMockMetacard() {
        MetacardImpl metacard = new MetacardImpl();
        metacard.setContentTypeName("myContentType");
        metacard.setContentTypeVersion("myVersion");
        metacard.setCreatedDate(Calendar.getInstance().getTime());
        metacard.setEffectiveDate(Calendar.getInstance().getTime());
        metacard.setExpirationDate(Calendar.getInstance().getTime());
        metacard.setId("1234567890");
        // metacard.setLocation(wkt);
        metacard.setMetadata("<xml>Metadata</xml>");
        metacard.setModifiedDate(Calendar.getInstance().getTime());
        // metacard.setResourceSize("10MB");
        // metacard.setResourceURI(uri)
        metacard.setSourceId("sourceID");
        metacard.setTitle("myTitle");
        return metacard;
    }

}
