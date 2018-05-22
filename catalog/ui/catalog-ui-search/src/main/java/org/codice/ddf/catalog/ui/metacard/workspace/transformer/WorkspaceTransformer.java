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
package org.codice.ddf.catalog.ui.metacard.workspace.transformer;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardImpl;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkspaceTransformer {
  private static final Logger LOGGER = LoggerFactory.getLogger(WorkspaceTransformer.class);

  private final CatalogFramework catalogFramework;

  private final InputTransformer inputTransformer;

  private final EndpointUtil endpointUtil;

  private final List<WorkspaceTransformation> transformations;

  public WorkspaceTransformer(
      CatalogFramework catalogFramework,
      InputTransformer inputTransformer,
      EndpointUtil endpointUtil,
      List<WorkspaceTransformation> transformations) {
    this.catalogFramework = catalogFramework;
    this.inputTransformer = inputTransformer;
    this.endpointUtil = endpointUtil;
    this.transformations = transformations;
  }

  private Optional<Map.Entry<String, Object>> metacardEntryToJsonEntry(
      final Map.Entry<String, Object> entry,
      WorkspaceTransformation transformation,
      Metacard workspaceMetacard) {
    if (transformation.getMetacardValueType().isInstance(entry.getValue())) {
      final String newKey = transformation.getJsonKey();
      return transformation
          .metacardValueToJsonValue(
              this, transformation.getMetacardValueType().cast(entry.getValue()), workspaceMetacard)
          .map(newValue -> new AbstractMap.SimpleEntry<>(newKey, newValue));
    } else {
      LOGGER.warn(
          "A workspace transformation expected a value of type {} for metacard attribute \"{}\", but instead found a value of type {}.",
          transformation.getMetacardValueType().getName(),
          entry.getKey(),
          entry.getValue().getClass().getName());
      return Optional.empty();
    }
  }

  private Optional<Map.Entry<String, Object>> metacardEntryToJsonEntry(
      final Map.Entry<String, Object> entry, Metacard workspaceMetacard) {
    return transformations
        .stream()
        .filter(transformation -> entry.getKey().equals(transformation.getMetacardKey()))
        .findAny()
        .map(transformation -> metacardEntryToJsonEntry(entry, transformation, workspaceMetacard))
        .orElse(Optional.of(entry));
  }

  private Optional<Map.Entry<String, Object>> jsonEntryToMetacardEntry(
      Map.Entry<String, Object> entry, WorkspaceTransformation transformation) {
    if (transformation.getJsonValueType().isInstance(entry.getValue())) {
      final String newKey = transformation.getMetacardKey();
      return transformation
          .jsonValueToMetacardValue(this, transformation.getJsonValueType().cast(entry.getValue()))
          .map(newValue -> new AbstractMap.SimpleEntry<>(newKey, newValue));
    } else {
      LOGGER.debug(
          "A workspace transformation expected a value of type {} for JSON key \"{}\", but instead found a value of type {}.",
          transformation.getJsonValueType().getName(),
          entry.getKey(),
          entry.getValue().getClass().getName());
      return Optional.empty();
    }
  }

  private Optional<Map.Entry<String, Object>> jsonEntryToMetacardEntry(
      Map.Entry<String, Object> entry) {
    return transformations
        .stream()
        .filter(transformation -> entry.getKey().equals(transformation.getJsonKey()))
        .findAny()
        .map(transformation -> jsonEntryToMetacardEntry(entry, transformation))
        .orElse(Optional.of(entry));
  }

  private void addAttributeValue(Metacard metacard, Map.Entry<String, Object> entry) {
    final Object value = entry.getValue();

    LOGGER.trace(
        "The map key \"{}\" with value \"{}\" is being added to the metacard.",
        entry.getKey(),
        value);

    if (value instanceof Serializable) {
      metacard.setAttribute(new AttributeImpl(entry.getKey(), (Serializable) value));
    } else if (value instanceof List) {
      metacard.setAttribute(new AttributeImpl(entry.getKey(), (List<Serializable>) value));
    } else {
      LOGGER.debug("The value was expected to be Serializable or a list of Serializable items.");
    }
  }

  public void transformIntoMetacard(Map<String, Object> json, Metacard init) {
    json.entrySet()
        .stream()
        .map(this::jsonEntryToMetacardEntry)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(endpointUtil::convertDateEntries)
        .filter(Objects::nonNull)
        .forEach(entry -> addAttributeValue(init, entry));
  }

  public Metacard transform(Map<String, Object> json) {
    WorkspaceMetacardImpl workspaceMetacard = new WorkspaceMetacardImpl();
    transformIntoMetacard(json, workspaceMetacard);
    return workspaceMetacard;
  }

  @Nullable
  private Map.Entry<String, Object> getEntryFromDescriptor(
      Metacard metacard, AttributeDescriptor descriptor) {
    Attribute attribute = metacard.getAttribute(descriptor.getName());
    if (attribute == null) {
      return null;
    } else if (descriptor.isMultiValued()) {
      return new AbstractMap.SimpleEntry<>(attribute.getName(), attribute.getValues());
    } else {
      return new AbstractMap.SimpleEntry<>(attribute.getName(), attribute.getValue());
    }
  }

  public Map<String, Object> transform(Metacard workspaceMetacard, Metacard metacard) {
    return Optional.of(metacard)
        .map(Metacard::getMetacardType)
        .map(MetacardType::getAttributeDescriptors)
        .orElseGet(Collections::emptySet)
        .stream()
        .map(descriptor -> getEntryFromDescriptor(metacard, descriptor))
        .filter(Objects::nonNull)
        .map(entry -> this.metacardEntryToJsonEntry(entry, workspaceMetacard))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .filter(entry -> entry.getKey() != null)
        .filter(entry -> entry.getValue() != null)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left));
  }

  public Map<String, Object> transform(Metacard workspaceMetacard) {
    return transform(workspaceMetacard, workspaceMetacard);
  }

  public List<Map<String, Object>> transform(List<Metacard> metacards) {
    return metacards.stream().map(this::transform).collect(Collectors.toList());
  }

  public String metacardToXml(Metacard metacard) {
    try (InputStream stream = catalogFramework.transform(metacard, "xml", null).getInputStream()) {
      return IOUtils.toString(stream, Charset.defaultCharset());
    } catch (IOException | CatalogTransformerException e) {
      throw new WorkspaceTransformException(e);
    }
  }

  public Metacard xmlToMetacard(String xml) {
    try (InputStream is = IOUtils.toInputStream(xml, Charset.defaultCharset())) {
      return inputTransformer.transform(is);
    } catch (IOException | CatalogTransformerException ex) {
      throw new WorkspaceTransformException(ex);
    }
  }
}
