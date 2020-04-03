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
package ddf.security.samlp.impl;

import static java.util.Objects.nonNull;

import com.google.common.collect.Maps;
import ddf.security.samlp.MetadataConfigurationParser;
import ddf.security.samlp.SamlProtocol.Binding;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Non-instantiable class that provides a utility function to parse service provider metadata */
public class SPMetadataParser {

  private static final Logger LOGGER = LoggerFactory.getLogger(SPMetadataParser.class);

  private SPMetadataParser() {}

  /**
   * @param spMetadata Metadata from the service provider either as the xml itself, a url to a
   *     service that returns the xml, or the path to a file with the xml starting with file:
   * @param bindingSet Set of supported bindings
   * @return Map of the service providers entity id and the entity information
   */
  public static Map<String, EntityInformation> parse(
      @Nullable List<String> spMetadata, Set<Binding> bindingSet) {
    if (spMetadata == null) {
      return Collections.emptyMap();
    }

    Map<String, EntityInformation> spMap = new HashMap<>();
    try {
      MetadataConfigurationParser metadataConfigurationParser =
          new MetadataConfigurationParser(
              spMetadata,
              ed -> {
                EntityInformation entityInfo =
                    new EntityInformation.Builder(ed, bindingSet).build();
                if (entityInfo != null) {
                  spMap.put(ed.getEntityID(), entityInfo);
                }
              });

      spMap.putAll(
          metadataConfigurationParser.getEntityDescriptors().entrySet().stream()
              .map(
                  e ->
                      Maps.immutableEntry(
                          e.getKey(),
                          new EntityInformation.Builder(e.getValue(), bindingSet).build()))
              .filter(e -> nonNull(e.getValue()))
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

    } catch (IOException e) {
      LOGGER.warn(
          "Unable to parse SP metadata configuration. Check the configuration for SP metadata.", e);
    }

    return spMap;
  }
}
