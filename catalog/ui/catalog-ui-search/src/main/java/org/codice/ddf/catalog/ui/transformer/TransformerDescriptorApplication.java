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
package org.codice.ddf.catalog.ui.transformer;

import static spark.Spark.get;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.Locale;
import java.util.Map;
import spark.servlet.SparkApplication;

public class TransformerDescriptorApplication implements SparkApplication {

  private TransformerDescriptors descriptors;

  private static final Gson GSON =
      new GsonBuilder().disableHtmlEscaping().serializeNulls().create();

  private static final Map<String, String> TRANSFORMER_NOT_FOUND_RESPONSE =
      new ImmutableMap.Builder<String, String>().put("message", "Transformer not found").build();

  private static final Map<String, String> TRANSFORMER_TYPE_NOT_FOUND_RESPONSE =
      new ImmutableMap.Builder<String, String>()
          .put("message", "Transformer type not found")
          .build();

  private static final String QUERY_RESPONSE_TRANSFORMER = "query";

  private static final String METACARD_TRANSFORMER = "metacard";

  @Override
  public void init() {
    get(
        "/transformers/metacard",
        (req, res) -> {
          res.type("application/json");
          return descriptors.getMetacardTransformers();
        },
        GSON::toJson);

    get(
        "/transformers/query",
        (req, res) -> {
          res.type("application/json");
          return descriptors.getQueryResponseTransformers();
        },
        GSON::toJson);

    get(
        "/transformers/:type/:id",
        (req, res) -> {
          String id = req.params(":id");
          String type = req.params(":type").toLowerCase(Locale.US);

          Map<String, String> descriptor;

          res.type("application/json");

          if (METACARD_TRANSFORMER.equalsIgnoreCase(type)) {
            descriptor = descriptors.getMetacardTransformer(id);
          } else if (QUERY_RESPONSE_TRANSFORMER.equalsIgnoreCase(type)) {
            descriptor = descriptors.getQueryResponseTransformer(id);
          } else {
            res.status(404);
            return GSON.toJson(TRANSFORMER_TYPE_NOT_FOUND_RESPONSE);
          }

          if (descriptor == null) {
            res.status(404);
            return GSON.toJson(TRANSFORMER_NOT_FOUND_RESPONSE);
          }

          return GSON.toJson(descriptor);
        });
  }

  @SuppressWarnings("WeakerAccess" /* setter must be public for blueprint access */)
  public void setDescriptors(TransformerDescriptors descriptors) {
    this.descriptors = descriptors;
  }
}
