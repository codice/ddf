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
package ddf.catalog.transformer.input.geojson;

import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.isNotEmpty;
import static org.codice.gsonsupport.GsonTypeAdapters.MAP_STRING_TO_OBJECT_TYPE;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.geo.formatter.CompositeGeometry;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;
import java.util.function.Function;
import javax.xml.bind.DatatypeConverter;
import org.apache.commons.lang3.StringUtils;
import org.codice.ddf.platform.util.SortedServiceList;
import org.codice.gsonsupport.GsonTypeAdapters.LongDoubleTypeAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts standard GeoJSON (geojson.org) into a Metacard. The limitation on the GeoJSON is that it
 * must conform to the {@link ddf.catalog.data.impl.MetacardImpl#BASIC_METACARD} {@link
 * MetacardType}.
 */
public class GeoJsonInputTransformer implements InputTransformer {

  static final String ISO_8601_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

  private static final Gson GSON =
      new GsonBuilder()
          .disableHtmlEscaping()
          .registerTypeAdapterFactory(LongDoubleTypeAdapter.FACTORY)
          .create();

  private static final String METACARD_TYPE_PROPERTY_KEY = "metacard-type";

  private static final String ID = "geojson";

  private static final String MIME_TYPE = "application/json";

  private static final String SOURCE_ID_PROPERTY = "source-id";

  private static final Logger LOGGER = LoggerFactory.getLogger(GeoJsonInputTransformer.class);

  private List<MetacardType> metacardTypes;

  private AttributeRegistry attributeRegistry;

  private SortedServiceList inputTransformers;

  /** Transforms GeoJson (http://www.geojson.org/) into a {@link Metacard} */
  @Override
  public Metacard transform(InputStream input) throws IOException, CatalogTransformerException {
    return transform(input, null);
  }

  @Override
  public Metacard transform(InputStream input, String id)
      throws IOException, CatalogTransformerException {

    validateInput(input);
    Map<String, Object> rootObject = getRootObject(input);
    validateTypeValue(rootObject);
    Map<String, Object> properties = getProperties(rootObject);

    final String propertyTypeName = (String) properties.get(METACARD_TYPE_PROPERTY_KEY);
    MetacardImpl metacard = getMetacard(propertyTypeName, properties);

    MetacardType metacardType = metacard.getMetacardType();
    LOGGER.debug("Metacard type name: {}", metacardType.getName());

    // retrieve geometry
    CompositeGeometry geoJsonGeometry = getCompositeGeometry(rootObject);

    if (geoJsonGeometry != null && StringUtils.isNotEmpty(geoJsonGeometry.toWkt())) {
      metacard.setLocation(geoJsonGeometry.toWkt());
    }

    Map<String, AttributeDescriptor> attributeDescriptorMap =
        metacardType
            .getAttributeDescriptors()
            .stream()
            .collect(toMap(AttributeDescriptor::getName, Function.identity()));

    properties
        .entrySet()
        .forEach(entry -> addAttributeToMetacard(metacard, attributeDescriptorMap, entry));

    if (isNotEmpty(metacard.getSourceId())) {
      properties.put(SOURCE_ID_PROPERTY, metacard.getSourceId());
    }

    if (id != null) {
      metacard.setId(id);
    }

    return metacard;
  }

  public void setMetacardTypes(List<MetacardType> metacardTypes) {
    this.metacardTypes = metacardTypes;
  }

  public void setAttributeRegistry(AttributeRegistry attributeRegistry) {
    this.attributeRegistry = attributeRegistry;
  }

  @Override
  public String toString() {
    return "InputTransformer {Impl="
        + this.getClass().getName()
        + ", id="
        + ID
        + ", mime-type="
        + MIME_TYPE
        + "}";
  }

  private void validateInput(InputStream input) throws CatalogTransformerException {
    if (input == null) {
      throw new CatalogTransformerException("Cannot transform null input.");
    }
  }

  private Map<String, Object> getRootObject(InputStream input) throws CatalogTransformerException {
    Map<String, Object> rootObject;
    try {
      rootObject =
          GSON.fromJson(
              new InputStreamReader(input, StandardCharsets.UTF_8), MAP_STRING_TO_OBJECT_TYPE);
    } catch (JsonParseException e) {
      throw new CatalogTransformerException("Invalid JSON input", e);
    }

    if (rootObject == null) {
      throw new CatalogTransformerException("Unable to parse JSON for metacard.");
    }
    return rootObject;
  }

  private void validateTypeValue(Map<String, Object> rootObject)
      throws CatalogTransformerException {
    Object typeValue = rootObject.get(CompositeGeometry.TYPE_KEY);
    if (!"Feature".equals(typeValue)) {
      throw new CatalogTransformerException(
          new UnsupportedOperationException(
              "Only supported type is Feature, not [" + typeValue + "]"));
    }
  }

  private Map<String, Object> getProperties(Map<String, Object> rootObject)
      throws CatalogTransformerException {
    Map<String, Object> properties =
        (Map<String, Object>) rootObject.get(CompositeGeometry.PROPERTIES_KEY);

    if (properties == null) {
      throw new CatalogTransformerException("Properties are required to create a Metacard.");
    }
    return properties;
  }

  private MetacardImpl getMetacard(String propertyTypeName, Map<String, Object> properties)
      throws CatalogTransformerException {
    if (isEmpty(propertyTypeName) || metacardTypes == null) {
      LOGGER.debug(
          "MetacardType specified in input is null or empty.  Trying all transformers in order...");
      Optional<MetacardImpl> first =
          inputTransformers == null
              ? Optional.of(new MetacardImpl())
              : inputTransformers
                  .stream()
                  .map(service -> tryTransformers(properties, service))
                  .filter(Objects::nonNull)
                  .findFirst();
      return first.orElse(new MetacardImpl());
    } else {
      MetacardType metacardType =
          metacardTypes
              .stream()
              .filter(type -> type.getName().equals(propertyTypeName))
              .findFirst()
              .orElseThrow(
                  () ->
                      new CatalogTransformerException(
                          "MetacardType specified in input has not been registered with the system."
                              + " Cannot parse input. MetacardType name: "
                              + propertyTypeName));

      LOGGER.debug("Found registered MetacardType: {}", propertyTypeName);
      return new MetacardImpl(metacardType);
    }
  }

  private MetacardImpl tryTransformers(Map<String, Object> properties, Object service) {
    InputTransformer inputTransformer = null;
    try {
      inputTransformer = (InputTransformer) service;
      return new MetacardImpl(
          inputTransformer.transform(
              new ByteArrayInputStream(((String) properties.get("metadata")).getBytes())));
    } catch (Exception e) {
      LOGGER.debug("Error calling transformer: " + inputTransformer.toString(), e);
    }
    return null;
  }

  private CompositeGeometry getCompositeGeometry(Map<String, Object> rootObject) {
    Map<String, Object> geometryJson =
        (Map<String, Object>) rootObject.get(CompositeGeometry.GEOMETRY_KEY);
    CompositeGeometry geoJsonGeometry = null;
    if (geometryJson != null) {
      if (geometryJson.get(CompositeGeometry.TYPE_KEY) != null
          && (geometryJson.get(CompositeGeometry.COORDINATES_KEY) != null
              || geometryJson.get(CompositeGeometry.GEOMETRIES_KEY) != null)) {

        String geometryTypeJson = geometryJson.get(CompositeGeometry.TYPE_KEY).toString();

        geoJsonGeometry = CompositeGeometry.getCompositeGeometry(geometryTypeJson, geometryJson);

      } else {
        LOGGER.debug("Could not find geometry type.");
      }
    }
    return geoJsonGeometry;
  }

  private void addAttributeToMetacard(
      MetacardImpl metacard,
      Map<String, AttributeDescriptor> attributeDescriptorMap,
      Entry<String, Object> entry) {
    final String key = entry.getKey();
    final Object value = entry.getValue();
    try {
      if (attributeDescriptorMap.containsKey(key)) {
        AttributeDescriptor ad = attributeDescriptorMap.get(key);
        metacard.setAttribute(key, convertProperty(value, ad));
      } else {
        Optional<AttributeDescriptor> optional = attributeRegistry.lookup(key);
        if (optional.isPresent()) {
          metacard.setAttribute(key, convertProperty(value, optional.get()));
        }
      }
    } catch (NumberFormatException | ParseException e) {
      LOGGER.info(
          "GeoJSON input for attribute name '{}' does not match the expected AttributeType. "
              + "This attribute will not be added to the metacard.",
          key,
          e);
    }
  }

  private Serializable convertProperty(Object property, AttributeDescriptor descriptor)
      throws ParseException {
    AttributeFormat format = descriptor.getType().getAttributeFormat();
    if (descriptor.isMultiValued() && property instanceof List) {
      List<Serializable> values = new ArrayList<>();
      for (Object value : (List) property) {
        values.add(convertValue(value, format));
      }
      return (Serializable) values;
    } else {
      return convertValue(property, format);
    }
  }

  private Serializable convertValue(Object value, AttributeFormat format) throws ParseException {
    if (value == null) {
      return null;
    }

    switch (format) {
      case BINARY:
        return DatatypeConverter.parseBase64Binary(value.toString());
      case DATE:
        SimpleDateFormat dateFormat = new SimpleDateFormat(ISO_8601_DATE_FORMAT);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.parse(value.toString());
      case GEOMETRY:
      case STRING:
      case XML:
        return value.toString();
      case BOOLEAN:
        return Boolean.parseBoolean(value.toString());
      case SHORT:
        return Short.parseShort(value.toString());
      case INTEGER:
        return Integer.parseInt(value.toString());
      case LONG:
        return Long.parseLong(value.toString());
      case FLOAT:
        return Float.parseFloat(value.toString());
      case DOUBLE:
        return Double.parseDouble(value.toString());
      default:
        return null;
    }
  }

  public SortedServiceList getInputTransformers() {
    return inputTransformers;
  }

  public void setInputTransformers(SortedServiceList inputTransformers) {
    this.inputTransformers = inputTransformers;
  }
}
