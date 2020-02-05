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
package ddf.catalog.transformer.metacard.geojson;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import ddf.geo.formatter.CompositeGeometry;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.xml.bind.DatatypeConverter;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
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
public class GeoJsonMetacardTransformer implements MetacardTransformer {

  public static final String ISO_8601_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

  public static final String ID = "geojson";

  protected static final String METACARD_TYPE_PROPERTY_KEY = "metacard-type";

  private static final Logger LOGGER = LoggerFactory.getLogger(GeoJsonMetacardTransformer.class);

  private static final String SOURCE_ID_PROPERTY = "source-id";

  protected static final MimeType DEFAULT_MIME_TYPE = new MimeType();

  static {
    try {
      DEFAULT_MIME_TYPE.setPrimaryType("application");
      DEFAULT_MIME_TYPE.setSubType("json");
    } catch (MimeTypeParseException e) {
      LOGGER.info("Failure creating MIME type", e);
      throw new ExceptionInInitializerError(e);
    }
  }

  public static JSONObject convertToJSON(Metacard metacard) throws CatalogTransformerException {
    if (metacard == null) {
      throw new CatalogTransformerException("Cannot transform null metacard.");
    }

    // must be LinkedHashMap to maintain order
    JSONObject rootObject = new JSONObject();
    rootObject.put("type", "Feature");
    JSONObject properties = new JSONObject();

    for (AttributeDescriptor ad : metacard.getMetacardType().getAttributeDescriptors()) {

      Attribute attribute = metacard.getAttribute(ad.getName());
      if (attribute != null) {
        Object value = convertAttribute(attribute, ad);
        if (value != null) {
          if (Metacard.GEOGRAPHY.equals(attribute.getName())) {
            rootObject.put(CompositeGeometry.GEOMETRY_KEY, value);
          } else {
            properties.put(attribute.getName(), value);
          }
        }
      }
    }

    if (rootObject.get(CompositeGeometry.GEOMETRY_KEY) == null) {
      rootObject.put(CompositeGeometry.GEOMETRY_KEY, null);
    }

    properties.put(METACARD_TYPE_PROPERTY_KEY, metacard.getMetacardType().getName());

    if (metacard.getSourceId() != null && !"".equals(metacard.getSourceId())) {
      properties.put(SOURCE_ID_PROPERTY, metacard.getSourceId());
    }

    rootObject.put(CompositeGeometry.PROPERTIES_KEY, properties);
    return rootObject;
  }

  @Override
  public BinaryContent transform(Metacard metacard, Map<String, Serializable> arguments)
      throws CatalogTransformerException {

    JSONObject rootObject = convertToJSON(metacard);

    String jsonText = JSONValue.toJSONString(rootObject);

    return new BinaryContentImpl(
        new ByteArrayInputStream(jsonText.getBytes(StandardCharsets.UTF_8)), DEFAULT_MIME_TYPE);
  }

  @Override
  public String toString() {
    return MetacardTransformer.class.getName()
        + " {Impl="
        + this.getClass().getName()
        + ", id="
        + ID
        + ", MIME Type="
        + DEFAULT_MIME_TYPE
        + "}";
  }

  private static Object convertAttribute(Attribute attribute, AttributeDescriptor descriptor)
      throws CatalogTransformerException {
    if (descriptor.isMultiValued()) {
      List<Object> values = new ArrayList<>();
      for (Serializable value : attribute.getValues()) {
        values.add(convertValue(value, descriptor.getType().getAttributeFormat()));
      }
      return values;
    } else {
      return convertValue(attribute.getValue(), descriptor.getType().getAttributeFormat());
    }
  }

  private static Object convertValue(Serializable value, AttributeType.AttributeFormat format)
      throws CatalogTransformerException {
    if (value == null) {
      return null;
    }

    switch (format) {
      case BOOLEAN:
        return value;
      case DATE:
        SimpleDateFormat dateFormat = new SimpleDateFormat(ISO_8601_DATE_FORMAT);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        return dateFormat.format((Date) value);
      case BINARY:
        byte[] bytes = (byte[]) value;
        String base64 = DatatypeConverter.printBase64Binary(bytes);

        return base64;
      case DOUBLE:
      case LONG:
      case FLOAT:
      case INTEGER:
      case SHORT:
      case STRING:
      case XML:
        return value.toString();
      case GEOMETRY:
        WKTReader reader = new WKTReader();
        try {
          Geometry geometry = reader.read(value.toString());
          CompositeGeometry geoJsonGeometry = CompositeGeometry.getCompositeGeometry(geometry);
          if (geoJsonGeometry == null) {
            throw new CatalogTransformerException(
                "Could not perform transform: unsupported geometry [" + value + "]");
          }
          return geoJsonGeometry.toJsonMap();
        } catch (ParseException e) {
          throw new CatalogTransformerException(
              "Could not perform transform: could not parse geometry [" + value + "]", e);
        }
      case OBJECT:
      default:
        return null;
    }
  }
}
