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
package ddf.catalog.transformer.queryresponse.geojson;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Result;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import ddf.catalog.transform.QueryResponseTransformer;
import ddf.catalog.transformer.metacard.geojson.GeoJsonMetacardTransformer;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

/**
 * Implements the {@link QueryResponseTransformer} interface to transform a {@link SourceResponse}
 * instance to GeoJSON. This class creates JSON objects for the list of {@link ddf.catalog.data.Metacard}s that are
 * the results from a query. This class leverages the {@link GeoJsonMetacardTransformer} to convert
 * metacards to JSON.
 *
 * @see GeoJsonMetacardTransformer
 * @see QueryResponseTransformer
 * @see ddf.catalog.data.Metacard
 * @see ddf.catalog.data.Attribute
 *
 */
public class GeoJsonQueryResponseTransformer implements QueryResponseTransformer {

    public static final String ID = "geojson";

    private static final Logger LOGGER = LoggerFactory
            .getLogger(GeoJsonQueryResponseTransformer.class);

    public static MimeType defaultMimeType = null;

    static {
        try {
            defaultMimeType = new MimeType("application/json");
        } catch (MimeTypeParseException e) {
            LOGGER.warn("Trying to set defaultMimeType", e);
        }
    }

    public static JSONObject convertToJSON(Result result) throws CatalogTransformerException {
        JSONObject rootObject = new JSONObject();

        addNonNullObject(rootObject, "distance", result.getDistanceInMeters());
        addNonNullObject(rootObject, "relevance", result.getRelevanceScore());
        addNonNullObject(rootObject, "metacard",
                GeoJsonMetacardTransformer.convertToJSON(result.getMetacard()));

        return rootObject;
    }

    private static void addNonNullObject(JSONObject obj, String name, Object value) {
        if (value != null) {
            obj.put(name, value);
        }
    }

    @Override
    public BinaryContent transform(SourceResponse upstreamResponse,
            Map<String, Serializable> arguments) throws CatalogTransformerException {
        if (upstreamResponse == null) {
            throw new CatalogTransformerException(
                    "Cannot transform null " + SourceResponse.class.getName());
        }

        JSONObject rootObject = new JSONObject();

        addNonNullObject(rootObject, "hits", upstreamResponse.getHits());

        JSONArray resultsList = new JSONArray();

        if (upstreamResponse.getResults() != null) {
            for (Result result : upstreamResponse.getResults()) {
                if (result == null) {
                    throw new CatalogTransformerException(
                            "Cannot transform null " + Result.class.getName());
                }
                JSONObject jsonObj = convertToJSON(result);
                if (jsonObj != null) {
                    resultsList.add(jsonObj);
                }
            }
        }
        addNonNullObject(rootObject, "results", resultsList);

        String jsonText = JSONValue.toJSONString(rootObject);

        return new ddf.catalog.data.BinaryContentImpl(
                new ByteArrayInputStream(jsonText.getBytes(StandardCharsets.UTF_8)),
                defaultMimeType);
    }

    @Override
    public String toString() {
        return MetacardTransformer.class.getName() + " {Impl=" + this.getClass().getName() + ", id="
                + ID + ", MIME Type=" + defaultMimeType + "}";
    }
}
