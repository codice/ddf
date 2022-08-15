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
package ddf.catalog.transformer.metacard.propertyjson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.annotation.Nullable;
import javax.xml.bind.DatatypeConverter;
import org.apache.commons.lang.StringUtils;
import org.codice.gsonsupport.GsonTypeAdapters.LongDoubleTypeAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements the {@link MetacardTransformer} interface to transform a single {@link Metacard}
 * instance to GeoJSON. This class places what is returned by {@link Metacard#getLocation()} in the
 * geometry JSON object in the GeoJSON output. The rest of the attributes of the Metacard are placed
 * in the properties object in the JSON. See geojson.org for the GeoJSON specification.
 *
 * @author Ashraf Barakat
 * @see MetacardTransformer
 * @see Metacard
 * @see Attribute
 */

/**
 * Transforms a metacard into a JSON with a format similiar to geojson, but without any special
 * geojson components. Simply put, a flat mapping of all attributes to K/V pairs stored inside a
 * root key of "property"
 */
public class PropertyJsonMetacardTransformer implements MetacardTransformer {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(PropertyJsonMetacardTransformer.class);

  private static final String SOURCE_ID_PROPERTY = "source-id";

  private static final String ISO_8601_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

  private static final Gson GSON =
      new GsonBuilder()
          .disableHtmlEscaping()
          .serializeNulls()
          .registerTypeAdapterFactory(LongDoubleTypeAdapter.FACTORY)
          .create();

  protected static final MimeType DEFAULT_MIME_TYPE = new MimeType();

  public static final String ID = "propertyjson";

  static {
    try {
      DEFAULT_MIME_TYPE.setPrimaryType("application");
      DEFAULT_MIME_TYPE.setSubType("json");
    } catch (MimeTypeParseException e) {
      LOGGER.info("Failure creating MIME type", e);
      throw new ExceptionInInitializerError(e);
    }
  }

  public static Map<String, Object> convertToJSON(Metacard metacard)
      throws CatalogTransformerException {
    return convertToJSON(metacard, Collections.emptyList());
  }

  public static Map<String, Object> convertToJSON(
      Metacard metacard, List<AttributeType.AttributeFormat> dontInclude)
      throws CatalogTransformerException {
    if (metacard == null) {
      throw new CatalogTransformerException("Cannot transform null metacard.");
    }

    Map<String, Object> rootObject = new HashMap<>();
    Map<String, Object> properties = new HashMap<>();

    for (AttributeDescriptor ad : metacard.getMetacardType().getAttributeDescriptors()) {

      Attribute attribute = metacard.getAttribute(ad.getName());
      if (attribute != null) {
        Object value = convertAttribute(attribute, ad, dontInclude);
        if (value != null) {
          properties.put(attribute.getName(), value);
        }
      }
    }

    properties.put(MetacardType.METACARD_TYPE, metacard.getMetacardType().getName());

    if (StringUtils.isNotBlank(metacard.getSourceId())) {
      properties.put(SOURCE_ID_PROPERTY, metacard.getSourceId());
    }

    rootObject.put("properties", properties);
    return rootObject;
  }

  @Override
  public BinaryContent transform(Metacard metacard, Map<String, Serializable> arguments)
      throws CatalogTransformerException {

    Map<String, Object> rootObject = convertToJSON(metacard);

    String jsonText = GSON.toJson(rootObject);

    return new BinaryContentImpl(
        new ByteArrayInputStream(jsonText.getBytes(StandardCharsets.UTF_8)), DEFAULT_MIME_TYPE);
  }

  @Override
  public String toString() {
    return String.format(
        "%s {Impl=%s, id=%s, MIME Type=%s}",
        MetacardTransformer.class.getName(), this.getClass().getName(), ID, DEFAULT_MIME_TYPE);
  }

  @Nullable
  private static Object convertAttribute(
      Attribute attribute,
      AttributeDescriptor descriptor,
      List<AttributeType.AttributeFormat> dontInclude)
      throws CatalogTransformerException {
    if (dontInclude.contains(descriptor.getType().getAttributeFormat())) {
      return null;
    }

    if (descriptor.isMultiValued()) {
      List<Object> values = new ArrayList<>();
      for (Serializable value : attribute.getValues()) {
        values.add(
            convertValue(attribute.getName(), value, descriptor.getType().getAttributeFormat()));
      }
      return values;
    } else {
      return convertValue(
          attribute.getName(), attribute.getValue(), descriptor.getType().getAttributeFormat());
    }
  }

  @Nullable
  private static Object convertValue(
      String name, Serializable value, AttributeType.AttributeFormat format)
      throws CatalogTransformerException {
    if (value == null) {
      return null;
    }

    switch (format) {
      case DATE:
        if (!(value instanceof Date)) {
          LOGGER.debug(
              "Dropping attribute date value {} for {} because it isn't a Date object.",
              value,
              name);
          return null;
        }
        // Creating date format instance each time is inefficient, however
        // it is not a threadsafe class so we are not able to put it in a static
        // class variable. If this proves to be a slowdown this class should be refactored
        // such that we don't need this method to be static.
        SimpleDateFormat dateFormat = new SimpleDateFormat(ISO_8601_DATE_FORMAT);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format((Date) value);
      case BINARY:
        byte[] bytes = (byte[]) value;
        return DatatypeConverter.printBase64Binary(bytes);
      case BOOLEAN:
      case DOUBLE:
      case LONG:
      case INTEGER:
      case SHORT:
        return value;
      case STRING:
      case XML:
      case FLOAT:
      case GEOMETRY:
        return value.toString();
      case OBJECT:
      default:
        return null;
    }
  }
}
