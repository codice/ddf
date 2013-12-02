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

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.Map;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

import org.apache.log4j.Logger;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import ddf.catalog.transform.QueryResponseTransformer;
import ddf.catalog.transformer.metacard.geojson.GeoJsonMetacardTransformer;

/**
 * Implements the {@link QueryResponseTransformer} interface to transform a {@link SourceResponse}
 * instance to GeoJSON. This class creates JSON objects for the list of {@link Metacard}s that are
 * the results from a query. This class leverages the {@link GeoJsonMetacardTransformer} to convert
 * metacards to JSON.
 * 
 * @see GeoJsonMetacardTransformer
 * @see QueryResponseTransformer
 * @see Metacard
 * @see Attribute
 * 
 */
public class GeoJsonQueryResponseTransformer implements QueryResponseTransformer {

    public static final String ID = "geojson";

    public static MimeType DEFAULT_MIME_TYPE = null;

    private static final Logger LOGGER = Logger.getLogger(GeoJsonQueryResponseTransformer.class);

    static {
        try {
            DEFAULT_MIME_TYPE = new MimeType("application/json");
        } catch (MimeTypeParseException e) {
            LOGGER.warn(e);
        }
    }

    @Override
    public BinaryContent transform(SourceResponse upstreamResponse,
            Map<String, Serializable> arguments) throws CatalogTransformerException {
        if (upstreamResponse == null) {
            throw new CatalogTransformerException("Cannot transform null "
                    + SourceResponse.class.getName());
        }

        JSONObject rootObject = new JSONObject();

        addNonNullObject(rootObject, "hits", upstreamResponse.getHits());

        JSONArray resultsList = new JSONArray();

        if (upstreamResponse.getResults() != null) {
            for (Result result : upstreamResponse.getResults()) {
                if (result == null) {
                    throw new CatalogTransformerException("Cannot transform null "
                            + Result.class.getName());
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
                new ByteArrayInputStream(jsonText.getBytes()), DEFAULT_MIME_TYPE);
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
    public String toString() {
        return MetacardTransformer.class.getName() + " {Impl=" + this.getClass().getName()
                + ", id=" + ID + ", MIME Type=" + DEFAULT_MIME_TYPE + "}";
    }
}
