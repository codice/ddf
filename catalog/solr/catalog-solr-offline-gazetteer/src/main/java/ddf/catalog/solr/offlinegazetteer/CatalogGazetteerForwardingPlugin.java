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
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.COUNTRY_CODE;
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.DESCRIPTION;
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.FEATURE_CODE;
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.GAZETTEER_METACARD_TAG;
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.GAZETTEER_REQUEST_HANDLER;
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.GAZETTEER_TO_CATALOG;
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.ID;
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.LOCATION;
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.POPULATION;
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.SORT_VALUE;
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.STANDALONE_GAZETTEER_CORE_NAME;
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.SUGGEST_BUILD_KEY;
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.SUGGEST_DICT;
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.SUGGEST_DICT_KEY;
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.SUGGEST_Q_KEY;
import static ddf.catalog.solr.offlinegazetteer.GazetteerConstants.TITLE;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
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
import javax.annotation.Nullable;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.codice.solr.client.solrj.SolrClient;
import org.codice.solr.factory.SolrClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CatalogGazetteerForwardingPlugin implements PostIngestPlugin, PreQueryPlugin {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(CatalogGazetteerForwardingPlugin.class);

  private final SolrClient solrClient;

  public CatalogGazetteerForwardingPlugin(SolrClientFactory clientFactory) {
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
              .map(CatalogGazetteerForwardingPlugin::convert)
              .collect(Collectors.toList()));
    } catch (SolrServerException | IOException e) {
      throw new PluginExecutionException("Error while processing gazetteer data", e);
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
              .map(CatalogGazetteerForwardingPlugin::convert)
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
        (attributeName) ->
            Optional.ofNullable(getStringAttribute(metacard, attributeName))
                .ifPresent(
                    attributeValue ->
                        solrDoc.addField(
                            GAZETTEER_TO_CATALOG.inverse().get(attributeName), attributeValue));

    getAttrAndAdd.accept(GAZETTEER_TO_CATALOG.get(DESCRIPTION));
    getAttrAndAdd.accept(GAZETTEER_TO_CATALOG.get(FEATURE_CODE));
    getAttrAndAdd.accept(GAZETTEER_TO_CATALOG.get(TITLE));
    getAttrAndAdd.accept(GAZETTEER_TO_CATALOG.get(ID));
    getAttrAndAdd.accept(GAZETTEER_TO_CATALOG.get(COUNTRY_CODE));

    Optional.of(metacard)
        .map(m -> getStringAttribute(m, GAZETTEER_TO_CATALOG.get(LOCATION)))
        .ifPresent(v -> solrDoc.addField(LOCATION, v));

    Optional.of(metacard)
        .map(m -> m.getAttribute(GAZETTEER_TO_CATALOG.get(POPULATION)))
        .map(Attribute::getValue)
        .filter(Long.class::isInstance)
        .map(Long.class::cast)
        .ifPresent(v -> solrDoc.addField(POPULATION, v));

    Optional.of(metacard)
        .map(m -> m.getAttribute(GAZETTEER_TO_CATALOG.get(SORT_VALUE)))
        .map(Attribute::getValue)
        .filter(Integer.class::isInstance)
        .map(Integer.class::cast)
        .ifPresent(v -> solrDoc.addField(SORT_VALUE, v));

    return solrDoc;
  }

  @Nullable
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
      query.setRequestHandler(GAZETTEER_REQUEST_HANDLER);
      query.setParam(SUGGEST_Q_KEY, "SGOSBuildSuggester");
      query.setParam(SUGGEST_BUILD_KEY, true);
      query.setParam(SUGGEST_DICT_KEY, SUGGEST_DICT);
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
