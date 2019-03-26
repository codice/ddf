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
package ddf.catalog.transformer.queryresponse.geojson;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import ddf.catalog.transformer.metacard.geojson.GeoJsonMetacardTransformer;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Tests the {@link GeoJsonQueryResponseTransformer} */
public class GeoJsonQueryResponseTransformerTest {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(GeoJsonQueryResponseTransformerTest.class);

  private static final JSONParser PARSER = new JSONParser();

  private static final String DEFAULT_TITLE = "myTitle";

  private static final String DEFAULT_VERSION = "myVersion";

  private static final String DEFAULT_XML = "<xml></xml>";

  private static final String DEFAULT_URI = "http://example.com";

  private static final String DEFAULT_TYPE = "myType";

  private static final String DEFAULT_LOCATION = "POINT (1 0)";

  private static final String DEFAULT_SOURCE_ID = "ddf";

  private static final double DEFAULT_RELEVANCE = 0.75;

  private static final Date NOW = new Date();

  private static GeoJsonQueryResponseTransformer geoJsonQueryResponseTransformer;

  @BeforeClass
  public static void setup() {
    geoJsonQueryResponseTransformer =
        new GeoJsonQueryResponseTransformer(new GeoJsonMetacardTransformer());
  }

  @Test(expected = CatalogTransformerException.class)
  public void testNullMetacardTransformer() throws CatalogTransformerException {
    GeoJsonQueryResponseTransformer geoJsonQueryResponseTransformer =
        new GeoJsonQueryResponseTransformer(null);
    SourceResponse sourceResponse = setupResponse(2, 2L);
    geoJsonQueryResponseTransformer.transform(sourceResponse, null);
  }

  @Test(expected = CatalogTransformerException.class)
  public void testNullResponse() throws CatalogTransformerException {
    geoJsonQueryResponseTransformer.transform(null, null);
  }

  @Test
  public void testNullResults() throws CatalogTransformerException, IOException, ParseException {
    SourceResponse sourceResponse = new SourceResponseImpl(null, null, 0L);
    JSONObject obj = transform(sourceResponse);
    verifyResponse(obj, 0, 0);
  }

  @Test(expected = CatalogTransformerException.class)
  public void testNullResult() throws CatalogTransformerException {

    List<Result> results = new LinkedList<Result>();
    results.add(null);
    results.add(null);

    SourceResponse sourceResponse = new SourceResponseImpl(null, results, 2L);
    geoJsonQueryResponseTransformer.transform(sourceResponse, null);
  }

  @Test(expected = CatalogTransformerException.class)
  public void testNullMetacard() throws CatalogTransformerException {

    List<Result> results = new LinkedList<Result>();
    Result result = new ResultImpl(null);

    results.add(result);

    SourceResponse sourceResponse = new SourceResponseImpl(null, results, 1L);
    geoJsonQueryResponseTransformer.transform(sourceResponse, null);
  }

  @Test
  public void testGoodResponse() throws CatalogTransformerException, IOException, ParseException {

    final int resultCount = 3;
    final int hitCount = 12;
    SourceResponse sourceResponse = setupResponse(resultCount, hitCount);
    JSONObject obj = transform(sourceResponse);

    verifyResponse(obj, resultCount, hitCount);
  }

  @Test
  public void testCustomTransformerWithJsonArray()
      throws ParseException, IOException, CatalogTransformerException {
    GeoJsonQueryResponseTransformer geoJsonQRT =
        new GeoJsonQueryResponseTransformer(
            createCustomMetacardTransformer("[{\"id\":\"0\"},{\"id\":\"1\"}]"));

    SourceResponse response = setupResponse(1, 1L);
    JSONObject json = transform(response, geoJsonQRT);

    JSONArray results = (JSONArray) json.get("results");
    JSONObject firstResult = (JSONObject) results.get(0);
    JSONArray metacard = (JSONArray) firstResult.get("metacard");
    assertThat(((JSONObject) metacard.get(0)).get("id"), is("0"));
    assertThat(((JSONObject) metacard.get(1)).get("id"), is("1"));
  }

  private MetacardTransformer createCustomMetacardTransformer(String binContent) {
    return (metacard, arguments) ->
        new BinaryContentImpl(IOUtils.toInputStream(binContent, StandardCharsets.UTF_8));
  }

  private JSONObject transform(SourceResponse sourceResponse)
      throws ParseException, IOException, CatalogTransformerException {
    return transform(sourceResponse, geoJsonQueryResponseTransformer);
  }

  private JSONObject transform(
      SourceResponse sourceResponse, GeoJsonQueryResponseTransformer geoJsonQRT)
      throws CatalogTransformerException, IOException, ParseException {

    BinaryContent content = geoJsonQRT.transform(sourceResponse, null);

    assertEquals(
        content.getMimeTypeValue(),
        GeoJsonQueryResponseTransformer.DEFAULT_MIME_TYPE.getBaseType());

    String jsonText = new String(content.getByteArray());
    Object object = PARSER.parse(jsonText);
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

    metacard.setCreatedDate(NOW);
    metacard.setModifiedDate(NOW);
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

  private void verifyResult(@SuppressWarnings("rawtypes") Map result) {
    assertThat(toString(result.get("relevance")), is(Double.toString(DEFAULT_RELEVANCE)));
    @SuppressWarnings("rawtypes")
    Map metacard = (Map) result.get("metacard");

    assertThat(metacard.get("properties"), notNullValue());
    @SuppressWarnings("rawtypes")
    Map properties = ((Map) metacard.get("properties"));
    assertThat(properties.size(), is(10));
    assertThat(toString(properties.get(Core.TITLE)), is(DEFAULT_TITLE));
    assertThat(toString(properties.get(Metacard.CONTENT_TYPE)), is(DEFAULT_TYPE));
    assertThat(toString(properties.get(Metacard.CONTENT_TYPE_VERSION)), is(DEFAULT_VERSION));
    SimpleDateFormat dateFormat =
        new SimpleDateFormat(GeoJsonMetacardTransformer.ISO_8601_DATE_FORMAT);
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    assertThat(toString(properties.get(Core.CREATED)), is(dateFormat.format(NOW)));
    assertThat(toString(properties.get(Core.EXPIRATION)), nullValue());
    assertThat(toString(properties.get(Metacard.EFFECTIVE)), nullValue());
    assertThat(toString(properties.get(Core.MODIFIED)), is(dateFormat.format(NOW)));
    assertThat(toString(properties.get(Core.THUMBNAIL)), is("CA=="));
    assertThat(toString(properties.get(Core.METADATA)), is(DEFAULT_XML));
    assertThat(toString(properties.get(Core.RESOURCE_URI)), is(DEFAULT_URI));
  }

  private String toString(Object object) {
    if (object != null) {
      return object.toString();
    }
    return null;
  }
}
