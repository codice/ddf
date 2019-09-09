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
package org.codice.ddf.rest.service.impl;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.ContentType;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.operation.SourceInfoResponse;
import ddf.catalog.operation.impl.SourceInfoRequestEnterprise;
import ddf.catalog.source.SourceDescriptor;
import ddf.catalog.source.SourceUnavailableException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.codice.ddf.attachment.AttachmentParser;
import org.codice.ddf.rest.service.AbstractCatalogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CatalogServiceImpl extends AbstractCatalogService {

  private static final Logger LOGGER = LoggerFactory.getLogger(CatalogServiceImpl.class);

  public CatalogServiceImpl(
      CatalogFramework framework,
      AttachmentParser attachmentParser,
      AttributeRegistry attributeRegistry) {
    super(framework, attachmentParser, attributeRegistry);
  }

  @Override
  public BinaryContent getSourcesInfo() {
    JSONArray resultsList = new JSONArray();
    SourceInfoResponse sources;
    String sourcesString;

    try {
      SourceInfoRequestEnterprise sourceInfoRequestEnterprise =
          new SourceInfoRequestEnterprise(true);

      sources = catalogFramework.getSourceInfo(sourceInfoRequestEnterprise);
      for (SourceDescriptor source : sources.getSourceInfo()) {
        JSONObject sourceObj = new JSONObject();
        sourceObj.put("id", source.getSourceId());
        sourceObj.put("version", source.getVersion() != null ? source.getVersion() : "");
        sourceObj.put("available", Boolean.valueOf(source.isAvailable()));

        List<JSONObject> sourceActions =
            source.getActions().stream().map(this::sourceActionToJSON).collect(Collectors.toList());

        sourceObj.put("sourceActions", sourceActions);

        JSONArray contentTypesObj = new JSONArray();
        if (source.getContentTypes() != null) {
          for (ContentType contentType : source.getContentTypes()) {
            if (contentType != null && contentType.getName() != null) {
              JSONObject contentTypeObj = new JSONObject();
              contentTypeObj.put("name", contentType.getName());
              contentTypeObj.put(
                  "version", contentType.getVersion() != null ? contentType.getVersion() : "");
              contentTypesObj.add(contentTypeObj);
            }
          }
        }
        sourceObj.put("contentTypes", contentTypesObj);
        resultsList.add(sourceObj);
      }
    } catch (SourceUnavailableException e) {
      LOGGER.info("Unable to retrieve Sources. {}", e.getMessage());
      LOGGER.debug("Unable to retrieve Sources", e);
    }

    sourcesString = JSONValue.toJSONString(resultsList);
    return new BinaryContentImpl(
        new ByteArrayInputStream(sourcesString.getBytes(StandardCharsets.UTF_8)), jsonMimeType);
  }
}
