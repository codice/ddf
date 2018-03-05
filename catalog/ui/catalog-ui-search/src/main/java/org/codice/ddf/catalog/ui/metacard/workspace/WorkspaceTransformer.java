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
package org.codice.ddf.catalog.ui.metacard.workspace;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.ListMetacardImpl;
import ddf.catalog.data.impl.QueryMetacardImpl;
import ddf.catalog.data.impl.QueryMetacardTypeImpl;
import ddf.catalog.data.types.Associations;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.Security;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.catalog.ui.util.EndpointUtil;

public class WorkspaceTransformer {

  private final CatalogFramework catalogFramework;

  private final InputTransformer inputTransformer;

  private final EndpointUtil endpointUtil;

  private final Map<String, Function<Map.Entry<String, Object>, Map.Entry<String, Object>>>
      metacardToJsonEntryMapper = new HashMap<>();

  private final Map<String, Function<Map.Entry<String, Object>, Map.Entry<String, Object>>>
      jsonToMetacardEntryMapper = new HashMap<>();

  private Function<Map.Entry<String, Object>, Map.Entry<String, Object>> remapKey(String key) {
    return entry -> new AbstractMap.SimpleEntry<>(key, entry.getValue());
  }

  private Function<Map.Entry<String, Object>, Map.Entry<String, Object>> remapValue(
      Function<Object, Object> fn) {
    return entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), fn.apply(entry.getValue()));
  }

  // for use during mapping keys/value from a json map to a metacard (json -> metacard)
  private void setupMetacardMappers() {
    metacardToJsonEntryMapper.put(
        WorkspaceAttributes.WORKSPACE_METACARDS, remapKey(Associations.RELATED));
    metacardToJsonEntryMapper.put("src", remapKey(QueryMetacardTypeImpl.QUERY_SOURCES));
    metacardToJsonEntryMapper.put(
        WorkspaceAttributes.WORKSPACE_QUERIES,
        remapValue(
            value -> {
              List<Map<String, Object>> queries = (List) value;
              return queries
                  .stream()
                  .map(query -> transformIntoMetacard(new QueryMetacardImpl()).apply(query))
                  .map(this::toMetacardXml)
                  .collect(Collectors.toList());
            }));
    metacardToJsonEntryMapper.put(
        WorkspaceAttributes.WORKSPACE_LISTS,
        remapValue(
            value -> {
              List<Map<String, Object>> content = (List) value;
              return content
                  .stream()
                  .map(transformIntoMetacard(new ListMetacardImpl()))
                  .map(this::toMetacardXml)
                  .collect(Collectors.toList());
            }));
    metacardToJsonEntryMapper.put(
        Security.ACCESS_INDIVIDUALS,
        remapValue(
            value -> {
              if (!(value instanceof List)) {
                return value;
              }

              return ((List<String>) value)
                  .stream()
                  .filter(StringUtils::isNotBlank)
                  .collect(Collectors.toCollection(ArrayList::new));
            }));
  }

  // for use during mapping keys/value from a metacard to a json map (metacard -> json)
  private void setupJsonMappers() {
    jsonToMetacardEntryMapper.put(
        Associations.RELATED, remapKey(WorkspaceAttributes.WORKSPACE_METACARDS));
    jsonToMetacardEntryMapper.put(QueryMetacardTypeImpl.QUERY_SOURCES, remapKey("src"));

    jsonToMetacardEntryMapper.put(Core.METACARD_TAGS, remapValue(v -> null));

    jsonToMetacardEntryMapper.put(
        WorkspaceAttributes.WORKSPACE_QUERIES,
        remapValue(
            value -> {
              List<String> queries = (List) value;

              return queries
                  .stream()
                  .map(this::toMetacardFromXml)
                  .map(this::transform)
                  .collect(Collectors.toList());
            }));
    jsonToMetacardEntryMapper.put(
        WorkspaceAttributes.WORKSPACE_LISTS,
        remapValue(
            value -> {
              List<String> lists = (List) value;

              return lists
                  .stream()
                  .map(this::toMetacardFromXml)
                  .map(this::transform)
                  .collect(Collectors.toList());
            }));
  }

  public WorkspaceTransformer(
      CatalogFramework catalogFramework,
      InputTransformer inputTransformer,
      EndpointUtil endpointUtil) {
    this.catalogFramework = catalogFramework;
    this.inputTransformer = inputTransformer;
    this.endpointUtil = endpointUtil;
    setupMetacardMappers();
    setupJsonMappers();
  }

  private Map.Entry<String, Object> remapMetacardEntry(Map.Entry<String, Object> entry) {
    return Optional.of(metacardToJsonEntryMapper)
        .map(m -> m.get(entry.getKey()))
        .map(fn -> fn.apply(entry))
        .orElse(entry);
  }

  @SuppressWarnings("unchecked")
  private BiFunction<Metacard, Map.Entry<String, Object>, Metacard> metacardBiFunc() {
    return (metacard, entry) -> {
      Object value = entry.getValue();
      if (value instanceof Serializable) {
        metacard.setAttribute(new AttributeImpl(entry.getKey(), (Serializable) value));
      } else if (value instanceof List) {
        metacard.setAttribute(new AttributeImpl(entry.getKey(), (List<Serializable>) value));
      }
      return metacard;
    };
  }

  private Function<Map<String, Object>, Metacard> transformIntoMetacard(Metacard init) {
    return map ->
        map.entrySet()
            .stream()
            .map(this::remapMetacardEntry)
            .map(endpointUtil::convertDateEntries)
            .reduce(init, metacardBiFunc(), (m1, m2) -> m2);
  }

  public Metacard transform(Map<String, Object> map) {
    return transformIntoMetacard(new WorkspaceMetacardImpl()).apply(map);
  }

  private Function<AttributeDescriptor, Map.Entry<String, Object>> getEntryFromDescriptor(
      Metacard metacard) {
    return (attributeDescriptor) ->
        Optional.of(attributeDescriptor)
            .map(ad -> metacard.getAttribute(ad.getName()))
            .map(
                attr -> {
                  if (attributeDescriptor.isMultiValued()) {
                    return new AbstractMap.SimpleEntry<String, Object>(
                        attr.getName(), attr.getValues());
                  } else {
                    return new AbstractMap.SimpleEntry<String, Object>(
                        attr.getName(), attr.getValue());
                  }
                })
            .orElse(null);
  }

  private Map.Entry<String, Object> remapJsonEntry(Map.Entry<String, Object> entry) {
    return Optional.of(jsonToMetacardEntryMapper)
        .map(jm -> jm.get(entry.getKey()))
        .map(fn -> fn.apply(entry))
        .orElse(entry);
  }

  public Map<String, Object> transform(Metacard metacard) {
    return Optional.of(metacard)
        .map(Metacard::getMetacardType)
        .map(MetacardType::getAttributeDescriptors)
        .orElseGet(Collections::emptySet)
        .stream()
        .map(getEntryFromDescriptor(metacard))
        .filter(Objects::nonNull)
        .map(this::remapJsonEntry)
        .filter(e -> e.getKey() != null && e.getValue() != null)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left));
  }

  public List<Map<String, Object>> transform(List<Metacard> metacards) {
    return metacards.stream().map(this::transform).collect(Collectors.toList());
  }

  public String toMetacardXml(Metacard m) {
    try (InputStream stream = catalogFramework.transform(m, "xml", null).getInputStream()) {
      return IOUtils.toString(stream);

    } catch (IOException | CatalogTransformerException e) {
      throw new WorkspaceTransformException(e);
    }
  }

  public Metacard toMetacardFromXml(Serializable xml) {
    try {
      if (xml instanceof String) {
        try (InputStream is = IOUtils.toInputStream((String) xml)) {
          return inputTransformer.transform(is);
        }
      }
    } catch (Exception ex) {
      throw new WorkspaceTransformException(ex);
    }

    return null;
  }
}
