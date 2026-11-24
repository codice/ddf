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
package ddf.catalog.source.solr;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.plugin.PreIngestPlugin;
import ddf.catalog.plugin.StopProcessingException;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Rejects creates and updates of metacards marked with {@link
 * SolrMetacardClientImpl#PARTIAL_RESULT_ATTRIBUTE}. Such metacards came from a query that
 * restricted the returned fields, so storing them would silently drop every attribute that was not
 * returned (updates replace the whole metacard).
 *
 * <p>The attribute is also registered with the {@link AttributeRegistry} so that endpoints which
 * only keep registered attributes (e.g. JSON-RPC) pass it through on create/update, allowing this
 * plugin to detect it.
 */
public class PartialMetacardIngestPlugin implements PreIngestPlugin {

  private static final Logger LOGGER = LoggerFactory.getLogger(PartialMetacardIngestPlugin.class);

  private final AttributeRegistry attributeRegistry;

  public PartialMetacardIngestPlugin(AttributeRegistry attributeRegistry) {
    this.attributeRegistry = attributeRegistry;
  }

  public void init() {
    attributeRegistry.register(SolrMetacardClientImpl.PARTIAL_RESULT_DESCRIPTOR);
  }

  public void destroy() {
    attributeRegistry.deregister(SolrMetacardClientImpl.PARTIAL_RESULT_DESCRIPTOR);
  }

  @Override
  public CreateRequest process(CreateRequest input) throws StopProcessingException {
    rejectPartial(input.getMetacards(), "created");
    return input;
  }

  @Override
  public UpdateRequest process(UpdateRequest input) throws StopProcessingException {
    List<Map.Entry<Serializable, Metacard>> updates = input.getUpdates();
    if (updates != null) {
      rejectPartial(
          updates.stream().map(Map.Entry::getValue).collect(Collectors.toList()), "updated");
    }
    return input;
  }

  @Override
  public DeleteRequest process(DeleteRequest input) {
    return input;
  }

  private void rejectPartial(Collection<Metacard> metacards, String operation)
      throws StopProcessingException {
    if (metacards == null) {
      return;
    }
    List<String> partialIds =
        metacards.stream()
            .filter(Objects::nonNull)
            .filter(PartialMetacardIngestPlugin::isPartial)
            .map(metacard -> String.valueOf(metacard.getId()))
            .collect(Collectors.toList());
    if (partialIds.isEmpty()) {
      return;
    }
    LOGGER.debug("Rejecting partial metacards {}", partialIds);
    throw new StopProcessingException(
        "Partial metacards cannot be "
            + operation
            + " "
            + partialIds
            + ": they were retrieved with "
            + SolrMetacardClientImpl.INCLUDED_FIELDS
            + "/"
            + SolrMetacardClientImpl.EXCLUDED_FIELDS
            + " and may be missing attributes. Retrieve the full metacard before saving it.");
  }

  static boolean isPartial(Metacard metacard) {
    Attribute attribute = metacard.getAttribute(SolrMetacardClientImpl.PARTIAL_RESULT_ATTRIBUTE);
    if (attribute == null) {
      return false;
    }
    Serializable value = attribute.getValue();
    return Boolean.TRUE.equals(value)
        || (value instanceof String && Boolean.parseBoolean((String) value));
  }
}
