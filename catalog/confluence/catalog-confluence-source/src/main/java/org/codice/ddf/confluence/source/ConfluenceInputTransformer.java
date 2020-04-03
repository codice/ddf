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
package org.codice.ddf.confluence.source;

import static org.codice.gsonsupport.GsonTypeAdapters.MAP_STRING_TO_OBJECT_TYPE;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ddf.catalog.data.AttributeInjector;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.data.types.Associations;
import ddf.catalog.data.types.Contact;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.Media;
import ddf.catalog.data.types.Security;
import ddf.catalog.data.types.Topic;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.codice.ddf.confluence.common.Confluence;
import org.codice.gsonsupport.GsonTypeAdapters.LongDoubleTypeAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfluenceInputTransformer implements InputTransformer {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConfluenceInputTransformer.class);

  private static final String PRODUCT_XML =
      "<metadata><head><meta name=\"type\" content=\"%s\" /></head><body><p>%s</p></body></metadata>";

  private static final String UNKNOWN = "UNKNOWN";

  private static final String CONFLUENCE_TYPE_LINK =
      "https://developer.atlassian.com/confdev/confluence-server-rest-api/advanced-searching-using-cql/cql-field-reference#CQLFieldReference-titleTitleType";

  private static final int DESCRIPTION_SIZE = 256;

  private static final Gson GSON =
      new GsonBuilder()
          .disableHtmlEscaping()
          .registerTypeAdapterFactory(LongDoubleTypeAdapter.FACTORY)
          .create();

  public static final String METADATA = "metadata";
  public static final String USERNAME = "username";
  public static final String RESULTS = "results";

  private MetacardType metacardType;

  private List<AttributeInjector> injectors;

  public ConfluenceInputTransformer(MetacardType type, List<AttributeInjector> injectors) {
    this.metacardType = type;
    this.injectors = injectors;
  }

  @Override
  public Metacard transform(InputStream input) throws CatalogTransformerException {
    return transform(input, null);
  }

  @Override
  public Metacard transform(InputStream input, String id) throws CatalogTransformerException {

    Map<String, Object> json = getJsonObject(input);
    return transformConfluenceResult(json, null, id, null);
  }

  public List<Metacard> transformConfluenceResponse(InputStream input)
      throws CatalogTransformerException {
    return transformConfluenceResponse(input, null);
  }

  public List<Metacard> transformConfluenceResponse(InputStream input, String bodyExpansion)
      throws CatalogTransformerException {

    Map<String, Object> json = getJsonObject(input);
    List<Metacard> metacards = new ArrayList<>();

    String baseUrl = getString(json, "_links", "base");

    getJsonArray(json, RESULTS).stream()
        .forEach(
            e -> {
              try {
                metacards.add(transformConfluenceResult(e, baseUrl, null, bodyExpansion));
              } catch (CatalogTransformerException ex) {
                LOGGER.error("Exception transforming confluence result.", ex);
              }
            });
    return metacards;
  }

  private Metacard transformConfluenceResult(
      Object json, String baseUrl, String id, String bodyExpansion)
      throws CatalogTransformerException {
    MetacardType type =
        new MetacardTypeImpl(metacardType.getName(), metacardType.getAttributeDescriptors());
    for (AttributeInjector injector : injectors) {
      type = injector.injectAttributes(type);
    }
    MetacardImpl metacard = new MetacardImpl(type);
    parseBasicInfo(metacard, json, id);

    parseBody(
        metacard, json, bodyExpansion, metacard.getAttribute(Topic.CATEGORY).getValue().toString());

    parseLabels(metacard, json);

    parseHistory(metacard, json);

    parseLinks(metacard, json, baseUrl);

    parseRestrictions(metacard, json);

    Set<String> tags = new HashSet<>();
    tags.add(Metacard.DEFAULT_TAG);
    tags.add(metacardType.getName());
    metacard.setTags(tags);
    return metacard;
  }

  private void parseBasicInfo(MetacardImpl metacard, Object json, String id)
      throws CatalogTransformerException {
    if (id != null) {
      metacard.setId(id);
    } else {
      metacard.setId(getString(json, "id"));
    }
    String confluenceType = getString(json, "type");
    metacard.setContentTypeName(confluenceType);
    metacard.setAttribute(Topic.CATEGORY, confluenceType);
    metacard.setAttribute(Topic.VOCABULARY, CONFLUENCE_TYPE_LINK);

    metacard.setTitle(getString(json, "title"));

    metacard.setAttribute(Confluence.JSON_RESPONSE, json.toString());
  }

  private void parseLabels(MetacardImpl metacard, Object json) throws CatalogTransformerException {
    Object labelsElement = getJsonElement(json, METADATA, "labels");
    if (labelsElement != null) {
      ArrayList<String> labels = new ArrayList<>();
      getJsonArray(labelsElement, RESULTS).stream()
          .forEach(e -> labels.add(getStringOrDefault(e, UNKNOWN, "name")));
      metacard.setAttribute(Topic.KEYWORD, labels);
    }
  }

  private void parseHistory(MetacardImpl metacard, Object json) throws CatalogTransformerException {
    Object history = getRequiredJsonElement(json, "history");

    Date modified = getDate(getRequiredJsonElement(history, "lastUpdated", "when"));
    metacard.setModifiedDate(modified);
    metacard.setAttribute(Core.METACARD_MODIFIED, modified);

    Date created = getDate(getRequiredJsonElement(history, "createdDate"));
    metacard.setCreatedDate(created);
    metacard.setAttribute(Core.METACARD_CREATED, created);

    Object creator = getJsonElement(history, "createdBy", USERNAME);
    if (creator != null && StringUtils.isNotEmpty(creator.toString())) {
      metacard.setAttribute(Contact.CREATOR_NAME, creator.toString());
    } else {
      metacard.setAttribute(Contact.CREATOR_NAME, "Unknown");
    }

    ArrayList<String> contributors = new ArrayList<>();
    getJsonArray(history, "contributors", "publishers", "users").stream()
        .forEach(e -> addContributor(contributors, e));
    metacard.setAttribute(Contact.CONTRIBUTOR_NAME, contributors);
  }

  private void parseLinks(MetacardImpl metacard, Object json, String baseUrl)
      throws CatalogTransformerException {
    if (baseUrl == null) {
      baseUrl = getStringOrDefault(json, "", "baseUrl");
    }

    Object links = getRequiredJsonElement(json, "_links");

    String downloadUrl = getStringOrDefault(links, null, "download");
    if (downloadUrl != null) {
      metacard.setAttribute(
          Metacard.RESOURCE_DOWNLOAD_URL, String.format("%s%s", baseUrl, downloadUrl));
      metacard.setAttribute(Metacard.RESOURCE_URI, String.format("%s%s", baseUrl, downloadUrl));
      String fileSize = getStringOrDefault(json, null, "extensions", "fileSize");
      if (fileSize != null) {
        metacard.setAttribute(Metacard.RESOURCE_SIZE, fileSize);
      }
    }
    ArrayList<String> associations = new ArrayList<>();
    if (StringUtils.isNotEmpty(baseUrl)) {
      associations.add(String.format("%s%s", baseUrl, getString(links, "webui")));
    }

    if (!associations.isEmpty()) {
      metacard.setAttribute(Associations.EXTERNAL, associations);
    }
  }

  private void parseBody(
      MetacardImpl metacard, Object json, String bodyExpansion, String confluenceType) {
    String[] expansion;
    if (bodyExpansion != null) {
      expansion = bodyExpansion.split("\\.");
    } else {
      expansion = new String[] {"body", "view", "value"};
    }
    Object body = getJsonElement(json, expansion);
    if (body != null) {
      String cleanedText = body.toString().replaceAll("<.*?>", " ");
      String description = cleanedText;
      if (description.length() > DESCRIPTION_SIZE) {
        description = description.substring(0, DESCRIPTION_SIZE) + "...";
      }
      if (StringUtils.isNotEmpty(description)) {
        String xmlSafeString = StringEscapeUtils.escapeXml10(cleanedText);
        metacard.setAttribute(Metacard.DESCRIPTION, description);
        metacard.setAttribute(
            Metacard.METADATA, String.format(PRODUCT_XML, confluenceType, xmlSafeString));
        metacard.setAttribute(Confluence.BODY_TEXT, xmlSafeString);
      }
    }
    if (confluenceType.equals("attachment")) {
      Object comment = getJsonElement(json, METADATA, "comment");
      if (comment != null) {
        metacard.setAttribute(Metacard.DESCRIPTION, comment.toString());
      }
      Object mediaType = getJsonElement(json, METADATA, "mediaType");
      if (mediaType != null) {
        metacard.setAttribute(Media.TYPE, mediaType.toString());
      }
    } else {
      metacard.setAttribute(Media.TYPE, "text/html");
    }
  }

  private void parseRestrictions(MetacardImpl metacard, Object json)
      throws CatalogTransformerException {
    Object restrictions = getJsonElement(json, "restrictions", "read", "restrictions");
    if (restrictions != null) {
      ArrayList<String> userRestrictions = new ArrayList<>();
      ArrayList<String> groupRestrictions = new ArrayList<>();

      getJsonArray(restrictions, "user", RESULTS).stream()
          .forEach(e -> userRestrictions.add(getStringOrDefault(e, UNKNOWN, USERNAME)));
      getJsonArray(restrictions, "group", RESULTS).stream()
          .forEach(e -> groupRestrictions.add(getStringOrDefault(e, UNKNOWN, "name")));
      metacard.setAttribute(Security.ACCESS_INDIVIDUALS, userRestrictions);
      metacard.setAttribute(Security.ACCESS_GROUPS, groupRestrictions);
    }
  }

  private Map<String, Object> getJsonObject(InputStream stream) throws CatalogTransformerException {
    String jsonString = null;
    try {
      jsonString = IOUtils.toString(stream, StandardCharsets.UTF_8);
      Map<String, Object> rootObject = GSON.fromJson(jsonString, MAP_STRING_TO_OBJECT_TYPE);

      LOGGER.debug(jsonString);
      return rootObject;
    } catch (IOException | RuntimeException re) {
      throw new CatalogTransformerException("Invalid json. Could not parse: " + jsonString, re);
    }
  }

  private List getJsonArray(Object object, String... keys) throws CatalogTransformerException {
    Object jsonArray = getRequiredJsonElement(object, keys);
    if (!(jsonArray instanceof List)) {
      throw new CatalogTransformerException(
          String.format("Given element is not an array: %s", String.join("/", keys)));
    }
    return (List) jsonArray;
  }

  private Object getRequiredJsonElement(Object object, String... keys)
      throws CatalogTransformerException {
    Object json = getJsonElement(object, keys);
    if (json == null) {
      throw new CatalogTransformerException(
          String.format("Could not retrieve required value from json: %s", String.join("/", keys)));
    }
    return json;
  }

  private Object getJsonElement(Object object, String... keys) {

    if (keys == null || !(object instanceof Map)) {
      return null;
    }
    Object current = object;
    for (String key : keys) {

      if (((Map) current).get(key) == null) {
        return null;
      }
      current = ((Map) current).get(key);
    }
    return current;
  }

  private String getStringOrDefault(Object object, String defaultValue, String... keys) {
    Object json = getJsonElement(object, keys);
    if (json == null) {
      return defaultValue;
    }
    return json.toString();
  }

  private String getString(Object object, String... keys) throws CatalogTransformerException {
    return getRequiredJsonElement(object, keys).toString();
  }

  private Date getDate(Object dateTime) {
    if (dateTime instanceof Date) {
      return (Date) dateTime;
    }
    return Date.from(Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse((String) dateTime)));
  }

  private void addContributor(List<String> contributors, Object user) {
    if (getStringOrDefault(user, "", "type").equals("anonymous")) {
      contributors.add("anonymous");
    } else {
      contributors.add(getStringOrDefault(user, null, USERNAME));
    }
  }
}
