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
package org.codice.ddf.commands.spatial.gazetteer;

import static ddf.catalog.Constants.SUGGESTION_BUILD_KEY;
import static ddf.catalog.Constants.SUGGESTION_CONTEXT_KEY;
import static ddf.catalog.Constants.SUGGESTION_DICT_KEY;
import static ddf.catalog.Constants.SUGGESTION_QUERY_KEY;
import static ddf.catalog.data.types.Core.METACARD_TAGS;
import static org.codice.ddf.spatial.geocoding.GeoCodingConstants.GAZETTEER_METACARD_TAG;
import static org.codice.ddf.spatial.geocoding.GeoCodingConstants.SUGGEST_PLACE_KEY;

import ddf.catalog.CatalogFramework;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.codice.ddf.commands.catalog.SubjectCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Command(
    scope = "gazetteer",
    name = "build-suggester-index",
    description =
        "Builds the Solr suggest index used for placename autocompletion in Intrigue when using "
            + "the offline gazetteer. This index is built automatically whenever gazetteer "
            + "metacards are created, updated, or deleted, but if those builds fail then this "
            + "command can be used to attempt to build the index again.")
public class GazetteerBuildSuggesterIndexCommand extends SubjectCommands {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(GazetteerBuildSuggesterIndexCommand.class);

  @Reference private CatalogFramework catalogFramework;

  @Reference private FilterBuilder filterBuilder;

  public void setCatalogFramework(final CatalogFramework catalogFramework) {
    this.catalogFramework = catalogFramework;
  }

  public void setFilterBuilder(final FilterBuilder filterBuilder) {
    this.filterBuilder = filterBuilder;
  }

  @Override
  protected Object executeWithSubject() {
    console.println("Building the gazetteer suggester index.");
    console.println(
        "This will take a few seconds or longer depending on how many gazetteer entries there are.");

    buildSuggesterIndex();

    console.println("Done.");

    return null;
  }

  private void buildSuggesterIndex() {
    final QueryRequest buildSuggesterIndex = getQueryBuildSuggesterIndex();

    requestBuildSuggesterIndex(buildSuggesterIndex);
  }

  private void requestBuildSuggesterIndex(final QueryRequest buildSuggesterIndex) {
    try {
      catalogFramework.query(buildSuggesterIndex);
    } catch (SourceUnavailableException | FederationException | UnsupportedQueryException e) {
      printErrorMessage(
          "Could not build the Solr suggestion index. Try running this command again after resolving the following error.");
      printErrorMessage("Error: " + e.getMessage());
      console.println("The stacktrace can be viewed in the log.");
      LOGGER.info("The gazetteer:build-suggester-index command failed.", e);
    }
  }

  private QueryRequest getQueryBuildSuggesterIndex() {
    // The filter provided here isn't used since this query goes to the suggest handler.
    final Query suggestionQuery =
        new QueryImpl(filterBuilder.attribute(METACARD_TAGS).text(GAZETTEER_METACARD_TAG));

    return new QueryRequestImpl(suggestionQuery, getPropertiesBuildSuggesterIndex());
  }

  private Map<String, Serializable> getPropertiesBuildSuggesterIndex() {
    final Map<String, Serializable> suggestQueryProperties = new HashMap<>();
    // Query string must be included but we don't care about the result
    suggestQueryProperties.put(SUGGESTION_QUERY_KEY, "anything");
    suggestQueryProperties.put(SUGGESTION_CONTEXT_KEY, GAZETTEER_METACARD_TAG);
    suggestQueryProperties.put(SUGGESTION_DICT_KEY, SUGGEST_PLACE_KEY);
    suggestQueryProperties.put(SUGGESTION_BUILD_KEY, true);
    return suggestQueryProperties;
  }
}
