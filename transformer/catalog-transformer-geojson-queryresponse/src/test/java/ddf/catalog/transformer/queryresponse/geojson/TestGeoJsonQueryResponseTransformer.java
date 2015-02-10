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
package ddf.catalog.transformer.queryresponse.geojson;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.*;

import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transformer.metacard.geojson.GeoJsonMetacardTransformer;

/**
 * Tests the {@link GeoJsonQueryResponseTransformer}
 * 
 */
public class TestGeoJsonQueryResponseTransformer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestGeoJsonQueryResponseTransformer.class);

    private static final JSONParser parser = new JSONParser();

    private static final String DEFAULT_TITLE = "myTitle";

    private static final String DEFAULT_VERSION = "myVersion";

    private static final String DEFAULT_XML = "<xml></xml>";

    private static final String DEFAULT_URI = "http://example.com";

    private static final String DEFAULT_TYPE = "myType";

    private static final String DEFAULT_LOCATION = "POINT (1 0)";

    private static final String DEFAULT_SOURCE_ID = "ddf";

    private static final double DEFAULT_RELEVANCE = 0.75;

    private static final Date now = new Date();

    @Test(expected = CatalogTransformerException.class)
    public void testNullResponse() throws CatalogTransformerException {
        new GeoJsonQueryResponseTransformer().transform(null, null);
    }

    @Test
    public void testNullResults() throws CatalogTransformerException, IOException, ParseException {
        SourceResponse sourceResponse = new SourceResponseImpl(null, null, 0l);
        JSONObject obj = transform(sourceResponse, 0, 0);
        verifyResponse(obj, 0, 0);
    }

    @Test(expected = CatalogTransformerException.class)
    public void testNullResult() throws CatalogTransformerException {

        List<Result> results = new LinkedList<Result>();
        results.add(null);
        results.add(null);

        SourceResponse sourceResponse = new SourceResponseImpl(null, results, 2l);
        new GeoJsonQueryResponseTransformer().transform(sourceResponse, null);
    }

    @Test(expected = CatalogTransformerException.class)
    public void testNullMetacard() throws CatalogTransformerException {

        List<Result> results = new LinkedList<Result>();
        Result result = new ResultImpl(null);

        results.add(result);

        SourceResponse sourceResponse = new SourceResponseImpl(null, results, 1l);
        new GeoJsonQueryResponseTransformer().transform(sourceResponse, null);
    }

    @Test
    public void testGoodResponse() throws CatalogTransformerException, IOException, ParseException {

        final int resultCount = 3;
        final int hitCount = 12;
        SourceResponse sourceResponse = setupResponse(resultCount, hitCount);
        JSONObject obj = transform(sourceResponse, resultCount, hitCount);

        verifyResponse(obj, resultCount, hitCount);
    }

    private JSONObject transform(SourceResponse sourceResponse, final int resultCount,
            final int hitCount) throws CatalogTransformerException, IOException, ParseException {
        BinaryContent content = new GeoJsonQueryResponseTransformer().transform(sourceResponse,
                null);

        assertEquals(content.getMimeTypeValue(),
                GeoJsonQueryResponseTransformer.DEFAULT_MIME_TYPE.getBaseType());

        String jsonText = new String(content.getByteArray());
        Object object = parser.parse(jsonText);
        JSONObject obj = (JSONObject) object;
        return obj;
    }

    private SourceResponse setupResponse(int count, long hitsTotal) {
        List<Result> results = new LinkedList<Result>();

        for (int i = 0; i < count; i++) {
            results.add(setupResult());
        }
        SourceResponse sourceResponse = new SourceResponseImpl(null, results, hitsTotal);
        return sourceResponse;
    }

    private Result setupResult() {
        MetacardImpl metacard = new MetacardImpl();

        metacard.setCreatedDate(now);
        metacard.setModifiedDate(now);
        metacard.setMetadata(DEFAULT_XML);
        metacard.setContentTypeName(DEFAULT_TYPE);
        metacard.setContentTypeVersion(DEFAULT_VERSION);
        metacard.setLocation(DEFAULT_LOCATION);
        byte[] buffer = {8};
        metacard.setThumbnail(buffer);
        metacard.setTitle(DEFAULT_TITLE);
        metacard.setSourceId(DEFAULT_SOURCE_ID);
        try {
            metacard.setResourceURI(new URI(DEFAULT_URI));
        } catch (URISyntaxException e) {
            LOGGER.warn("Exception during testing", e);
        }

        ResultImpl result = new ResultImpl(metacard);
        result.setRelevanceScore(DEFAULT_RELEVANCE);
        return result;
    }

    @SuppressWarnings("rawtypes")
    private void verifyResponse(JSONObject response, int count, long hits) {
        assertThat(toString(response.get("hits")), is(Long.toString(hits)));
        List results = (List) response.get("results");
        assertThat(results.size(), is(count));

        for (Object o : results) {
            verifyResult((Map) o);
        }
    }

    private void verifyResult(@SuppressWarnings("rawtypes")
    Map result) {
        assertThat(toString(result.get("relevance")), is(Double.toString(DEFAULT_RELEVANCE)));
        @SuppressWarnings("rawtypes")
        Map metacard = (Map) result.get("metacard");

        assertThat(metacard.get("properties"), notNullValue());
        @SuppressWarnings("rawtypes")
        Map properties = ((Map) metacard.get("properties"));
        assertThat(properties.size(), is(10));
        assertThat(toString(properties.get(Metacard.TITLE)), is(DEFAULT_TITLE));
        assertThat(toString(properties.get(Metacard.CONTENT_TYPE)), is(DEFAULT_TYPE));
        assertThat(toString(properties.get(Metacard.CONTENT_TYPE_VERSION)), is(DEFAULT_VERSION));
        SimpleDateFormat dateFormat = new SimpleDateFormat(GeoJsonMetacardTransformer.ISO_8601_DATE_FORMAT);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        assertThat(toString(properties.get(Metacard.CREATED)),
                is(dateFormat.format(now)));
        assertThat(toString(properties.get(Metacard.EXPIRATION)), nullValue());
        assertThat(toString(properties.get(Metacard.EFFECTIVE)), nullValue());
        assertThat(toString(properties.get(Metacard.MODIFIED)),
                is(dateFormat.format(now)));
        assertThat(toString(properties.get(Metacard.THUMBNAIL)), is("CA=="));
        assertThat(toString(properties.get(Metacard.METADATA)), is(DEFAULT_XML));
        assertThat(toString(properties.get(Metacard.RESOURCE_URI)), is(DEFAULT_URI));
    }

    private String toString(Object object) {
        if (object != null) {
            return object.toString();
        }
        return null;
    }

}
