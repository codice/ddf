/**
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
 */
package ddf.catalog.transformer.input.geojson;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.junit.Test;
import org.osgi.framework.BundleContext;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardTypeRegistry;
import ddf.catalog.data.QualifiedMetacardType;
import ddf.catalog.data.metacardtype.MetacardTypeRegistryImpl;
import ddf.catalog.transform.CatalogTransformerException;

public class TestGeoJsonInputTransformer {
    public static final String DEFAULT_TITLE = "myTitle";

    public static final String DEFAULT_VERSION = "myVersion";

    public static final String DEFAULT_TYPE = "myType";

    public static final byte[] DEFAULT_BYTES = {8};

    private static final String SAMPLE_ID = "myId";

    private static final String DEFAULT_URI = "http://example.com";

    private static final BundleContext CONTEXT = mock(BundleContext.class);

    private static final MetacardTypeRegistry MTR = MetacardTypeRegistryImpl.getInstance();

    private static List<QualifiedMetacardType> qmtList = new ArrayList<QualifiedMetacardType>();

    // @formatter:off
    private static final String noTypeJsonText() {
        return "{" +
                "    \"properties\":{" +
                "        \"title\":{" +
                "            \"value\":\"myTitle\"" +
                "        }," +
                "    }," +
                "    \"geometry\":{" +
                "        \"type\":\"GeometryCollection\"," +
                "        \"coordinates\":[" +
                "            {" +
                "                \"type\":\"Point\"," +
                "                \"coordinates\":[" +
                "                    4.0," +
                "                    6.0" +
                "                ]" +
                "            }," +
                "            {" +
                "                \"type\":\"LineString\"," +
                "                \"coordinates\":[" +
                "                    [" +
                "                        4.0," +
                "                        6.0" +
                "                    ]," +
                "                    [" +
                "                        7.0," +
                "                        10.0" +
                "                    ]" +
                "                ]" +
                "            }" +
                "        ]" +
                "    }" +
                "}";
    }

    private static final String sampleFeatureCollectionJsonText() {
        return "{" +
                "    \"type\":\"FeatureCollection\"," +
                "    \"geometry\":{" +
                "        \"type\":\"Polygon\"," +
                "        \"coordinates\":[" +
                "            [" +
                "                [" +
                "                    30.0," +
                "                    10.0" +
                "                ]," +
                "                [" +
                "                    10.0," +
                "                    20.0" +
                "                ]," +
                "                [" +
                "                    20.0," +
                "                    40.0" +
                "                ]," +
                "                [" +
                "                    40.0," +
                "                    40.0" +
                "                ]," +
                "                [" +
                "                    30.0," +
                "                    10.0" +
                "                ]" +
                "            ]" +
                "        ]" +
                "    }" +
                "}";
    }

    private static final String samplePointJsonText() {
        return "{" +
                "    \"properties\":{" +
                "        \"title\":\"myTitle\"," +
                "        \"thumbnail\":\"CA==\"," +
                "        \"resource-uri\":\"http:\\/\\/example.com\"," +
                "        \"created\":\"2012-09-01T00:09:19.368+0000\"," +
                "        \"metadata-content-type-version\":\"myVersion\"," +
                "        \"metadata-content-type\":\"myType\"," +
                "        \"metadata\":\"<xml><\\/xml>\"," +
                "        \"modified\":\"2012-09-01T00:09:19.368+0000\"" +
                "    }," +
                "    \"type\":\"Feature\"," +
                "    \"geometry\":{" +
                "        \"type\":\"Point\"," +
                "        \"coordinates\":[" +
                "                30.0," +
                "                10.0" +
                "        ]" +
                "    }" +
                "}";
    }

    private static final String sampleLineStringJsonText() {
        return "{" +
                "    \"properties\":{" +
                "        \"title\":\"myTitle\"," +
                "        \"thumbnail\":\"CA==\"," +
                "        \"resource-uri\":\"http:\\/\\/example.com\"," +
                "        \"created\":\"2012-09-01T00:09:19.368+0000\"," +
                "        \"metadata-content-type-version\":\"myVersion\"," +
                "        \"metadata-content-type\":\"myType\"," +
                "        \"metadata\":\"<xml><\\/xml>\"," +
                "        \"modified\":\"2012-09-01T00:09:19.368+0000\"" +
                "    }," +
                "    \"type\":\"Feature\"," +
                "    \"geometry\":{" +
                "        \"type\":\"LineString\"," +
                "        \"coordinates\":[" +
                "            [" +
                "                30.0," +
                "                10.0" +
                "            ]," +
                "            [" +
                "                10.0," +
                "                30.0" +
                "            ]," +
                "            [" +
                "                40.0," +
                "                40.0" +
                "            ]" +
                "        ]" +
                "    }" +
                "}";
    }

    private static final String noGeoJsonText() {
        return "{" +
                "    \"properties\":{" +
                "        \"title\":\"myTitle\"," +
                "        \"thumbnail\":\"CA==\"," +
                "        \"resource-uri\":\"http:\\/\\/example.com\"," +
                "        \"created\":\"2012-09-01T00:09:19.368+0000\"," +
                "        \"metadata-content-type-version\":\"myVersion\"," +
                "        \"metadata-content-type\":\"myType\"," +
                "        \"metadata\":\"<xml><\\/xml>\"," +
                "        \"modified\":\"2012-09-01T00:09:19.368+0000\"" +
                "    }," +
                "    \"type\":\"Feature\"," +
                "    \"geometry\":null" +
                "}";
    }

    @Test(expected = CatalogTransformerException.class)
    public void testNullInput() throws IOException, CatalogTransformerException {
        new GeoJsonInputTransformer(MTR).transform(null);
    }

    @Test(expected = CatalogTransformerException.class)
    public void testBadInput() throws IOException, CatalogTransformerException {
        new GeoJsonInputTransformer(MTR).transform(new ByteArrayInputStream("{key=".getBytes()));
    }

    @Test(expected = CatalogTransformerException.class)
    public void testFeatureCollectionType() throws IOException, CatalogTransformerException {
        new GeoJsonInputTransformer(MTR)
                .transform(new ByteArrayInputStream(sampleFeatureCollectionJsonText().getBytes()));
    }

    @Test(expected = CatalogTransformerException.class)
    public void testNoType() throws IOException, CatalogTransformerException {
        new GeoJsonInputTransformer(MTR)
                .transform(new ByteArrayInputStream(noTypeJsonText().getBytes()));
    }

    @Test(expected = CatalogTransformerException.class)
    public void testNoProperties() throws IOException, CatalogTransformerException {
        new GeoJsonInputTransformer(MTR).transform(
                new ByteArrayInputStream("{ \"type\": \"FeatureCollection\"}".getBytes()));
    }

    @Test()
    public void testNoGeo() throws IOException, CatalogTransformerException {

        Metacard metacard = new GeoJsonInputTransformer(MTR)
                .transform(new ByteArrayInputStream(noGeoJsonText().getBytes()));

        verifyBasics(metacard);

    }

    @Test()
    public void testPointGeo() throws IOException, CatalogTransformerException, ParseException {

        Metacard metacard = new GeoJsonInputTransformer(MTR)
                .transform(new ByteArrayInputStream(samplePointJsonText().getBytes()));

        verifyBasics(metacard);

        WKTReader reader = new WKTReader();

        Geometry geometry = reader.read(metacard.getLocation());

        assertThat(geometry.getCoordinate().x, is(30.0));

        assertThat(geometry.getCoordinate().y, is(10.0));

    }

    @Test
    public void testLineStringGeo()
            throws IOException, CatalogTransformerException, ParseException {

        GeoJsonInputTransformer transformer = new GeoJsonInputTransformer(MTR);

        InputStream inputStream = new ByteArrayInputStream(sampleLineStringJsonText().getBytes());

        Metacard metacard = transformer.transform(inputStream);

        verifyBasics(metacard);

        WKTReader reader = new WKTReader();

        Geometry geometry = reader.read(metacard.getLocation());

        Coordinate[] coords = geometry.getCoordinates();

        assertThat(coords[0].x, is(30.0));
        assertThat(coords[0].y, is(10.0));

        assertThat(coords[1].x, is(10.0));
        assertThat(coords[1].y, is(30.0));

        assertThat(coords[2].x, is(40.0));
        assertThat(coords[2].y, is(40.0));
    }

    @Test
    public void testSetId() throws IOException, CatalogTransformerException {

        Metacard metacard = new GeoJsonInputTransformer(MTR)
                .transform(new ByteArrayInputStream(samplePointJsonText().getBytes()), SAMPLE_ID);

        verifyBasics(metacard);

        assertEquals(SAMPLE_ID, metacard.getId());

    }

    protected void verifyBasics(Metacard metacard) {
        assertEquals(DEFAULT_TITLE, metacard.getTitle());
        assertEquals(DEFAULT_URI, metacard.getResourceURI().toString());
        assertEquals(DEFAULT_TYPE, metacard.getContentTypeName());
        assertEquals(DEFAULT_VERSION, metacard.getContentTypeVersion());
        assertEquals("<xml></xml>", metacard.getMetadata());
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                GeoJsonInputTransformer.ISO_8601_DATE_FORMAT);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        assertEquals("2012-09-01T00:09:19.368+0000", dateFormat.format(metacard.getCreatedDate()));
        assertEquals("2012-09-01T00:09:19.368+0000", dateFormat.format(metacard.getModifiedDate()));
        assertArrayEquals(DEFAULT_BYTES, metacard.getThumbnail());
    }
}
