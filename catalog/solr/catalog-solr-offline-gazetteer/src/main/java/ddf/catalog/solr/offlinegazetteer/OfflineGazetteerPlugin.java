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
package ddf.catalog.solr.offlinegazetteer;

import static ddf.catalog.Constants.SUGGESTION_BUILD_KEY;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.Location;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PostIngestPlugin;
import ddf.catalog.plugin.PreQueryPlugin;
import ddf.catalog.plugin.StopProcessingException;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.codice.ddf.spatial.geocoding.GeoEntryAttributes;
import org.codice.solr.client.solrj.SolrClient;
import org.codice.solr.factory.SolrClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OfflineGazetteerPlugin implements PostIngestPlugin, PreQueryPlugin {
  private static final Logger LOGGER = LoggerFactory.getLogger(OfflineGazetteerPlugin.class);
  private static final String GAZETTEER_METACARD_TAG = "gazetteer";
  public static final String STANDALONE_GAZETTEER_CORE_NAME = "standalone-solr-gazetteer";

  private final SolrClientFactory clientFactory;
  private final SolrClient solrClient;

  public OfflineGazetteerPlugin(SolrClientFactory clientFactory) {
    this.clientFactory = clientFactory;
    this.solrClient = clientFactory.newClient(STANDALONE_GAZETTEER_CORE_NAME);
  }

  @Override
  public CreateResponse process(CreateResponse input) throws PluginExecutionException {
    List<Metacard> gazetteerMetacards =
        input
            .getCreatedMetacards()
            .stream()
            .filter(this::isGazetteerMetacard)
            .collect(Collectors.toList());

    if (gazetteerMetacards.isEmpty()) {
      return input;
    }

    try {
      solrClient.add(
          STANDALONE_GAZETTEER_CORE_NAME,
          gazetteerMetacards
              .stream()
              .map(OfflineGazetteerPlugin::convert)
              .collect(Collectors.toList()));
    } catch (SolrServerException | IOException e) {
      LOGGER.debug("Error while processing gazetteer data", e);
      throw new PluginExecutionException(e);
    }

    return input;
  }

  @Override
  public UpdateResponse process(UpdateResponse input) throws PluginExecutionException {
    List<Metacard> gazetteerMetacards =
        input
            .getUpdatedMetacards()
            .stream()
            .map(Update::getNewMetacard)
            .filter(this::isGazetteerMetacard)
            .collect(Collectors.toList());

    if (gazetteerMetacards.isEmpty()) {
      return input;
    }

    try {
      solrClient.add(
          STANDALONE_GAZETTEER_CORE_NAME,
          gazetteerMetacards
              .stream()
              .map(OfflineGazetteerPlugin::convert)
              .collect(Collectors.toList()));
    } catch (SolrServerException | IOException e) {
      LOGGER.debug("Error while processing gazetteer data", e);
      throw new PluginExecutionException(e);
    }
    return input;
  }

  @Override
  public DeleteResponse process(DeleteResponse input) throws PluginExecutionException {
    List<String> ids =
        input
            .getDeletedMetacards()
            .stream()
            .filter(this::isGazetteerMetacard)
            .map(Metacard::getId)
            .collect(Collectors.toList());
    if (ids.isEmpty()) {
      return input;
    }

    try {
      solrClient.deleteById(ids);
    } catch (SolrServerException | IOException e) {
      LOGGER.debug("Error while processing gazetteer data", e);
      throw new PluginExecutionException(e);
    }

    return input;
  }

  protected static SolrInputDocument convert(Metacard metacard) {
    SolrInputDocument solrDoc = new SolrInputDocument();
    Consumer<String> getAttrAndAdd =
        (attribute) ->
            Optional.ofNullable(getStringAttribute(metacard, attribute))
                .ifPresent(attr -> solrDoc.addField(attribute + "_txt", attr));

    getAttrAndAdd.accept(Metacard.DESCRIPTION);
    getAttrAndAdd.accept(GeoEntryAttributes.FEATURE_CODE_ATTRIBUTE_NAME);
    getAttrAndAdd.accept(Core.TITLE);
    getAttrAndAdd.accept(Core.ID);
    getAttrAndAdd.accept(Location.COUNTRY_CODE);

    Optional.of(metacard)
        .map(m -> getStringAttribute(m, Core.LOCATION))
        .ifPresent(v -> solrDoc.addField(Core.LOCATION + "_geo", v));

    Optional.of(metacard)
        .map(m -> m.getAttribute(GeoEntryAttributes.POPULATION_ATTRIBUTE_NAME))
        .map(Attribute::getValue)
        .filter(Long.class::isInstance)
        .map(Long.class::cast)
        .ifPresent(v -> solrDoc.addField(GeoEntryAttributes.POPULATION_ATTRIBUTE_NAME + "_lng", v));

    Optional.of(metacard)
        .map(m -> m.getAttribute(GeoEntryAttributes.GAZETTEER_SORT_VALUE))
        .map(Attribute::getValue)
        .filter(Integer.class::isInstance)
        .map(Integer.class::cast)
        .ifPresent(v -> solrDoc.addField(GeoEntryAttributes.GAZETTEER_SORT_VALUE + "_int", v));

    return solrDoc;
  }

  private static String getStringAttribute(Metacard metacard, String attributeName) {
    Attribute attribute = metacard.getAttribute(attributeName);
    if (attribute != null && attribute.getValue() instanceof String) {
      return (String) attribute.getValue();
    }
    return null;
  }

  private boolean isGazetteerMetacard(Metacard metacard) {
    return Optional.of(metacard)
        .map(Metacard::getTags)
        .map(tags -> tags.contains(GAZETTEER_METACARD_TAG))
        .orElse(false);
  }

  @Override
  public QueryRequest process(QueryRequest input)
      throws PluginExecutionException, StopProcessingException {
    Serializable build = input.getPropertyValue(SUGGESTION_BUILD_KEY);
    if (build instanceof Boolean && (Boolean) build) {
      SolrQuery query = new SolrQuery();
      query.setRequestHandler("/suggest");
      query.setParam("suggest.q", "SGOSBuildSuggester");
      query.setParam("suggest.build", true);
      query.setParam("suggest.dictionary", "suggestPlace");
      try {
        solrClient.query(query);
      } catch (SolrServerException | IOException e) {
        LOGGER.debug("Error while trying to build suggester");
        throw new PluginExecutionException(e);
      }
    }
    return input;
  }
}
