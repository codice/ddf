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
package org.codice.ddf.catalog.plugin.gazetteer;

import static org.codice.ddf.spatial.geocoding.GeoCodingConstants.GAZETTEER_METACARD_TAG;

import ddf.catalog.data.Metacard;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PostIngestPlugin;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuildSuggesterIndexPlugin implements PostIngestPlugin {
  private static final Logger LOGGER = LoggerFactory.getLogger(BuildSuggesterIndexPlugin.class);

  private final ScheduledThreadPoolExecutor executor;

  private final BuildSuggesterIndex buildSuggesterIndex;

  private ScheduledFuture<?> future;

  public BuildSuggesterIndexPlugin(
      final ScheduledThreadPoolExecutor executor, final BuildSuggesterIndex buildSuggesterIndex) {
    this.executor = executor;
    this.buildSuggesterIndex = buildSuggesterIndex;
  }

  @Override
  public CreateResponse process(final CreateResponse input) throws PluginExecutionException {
    final List<Metacard> metacards = input.getCreatedMetacards();
    if (containsGazetteerMetacards(metacards)) {
      LOGGER.trace("Create response contains gazetteer metacards. Building suggester index.");
      scheduleSuggesterIndexBuild();
    }
    return input;
  }

  @Override
  public UpdateResponse process(final UpdateResponse input) throws PluginExecutionException {
    if (containsGazetteerMetacards(input)) {
      LOGGER.trace("Update response contains gazetteer metacards. Building suggester index.");
      scheduleSuggesterIndexBuild();
    }
    return input;
  }

  @Override
  public DeleteResponse process(final DeleteResponse input) throws PluginExecutionException {
    final List<Metacard> metacards = input.getDeletedMetacards();
    if (containsGazetteerMetacards(metacards)) {
      LOGGER.trace("Delete response contains gazetteer metacards. Building suggester index.");
      scheduleSuggesterIndexBuild();
    }
    return input;
  }

  private boolean containsGazetteerMetacards(final List<Metacard> metacards) {
    return metacards.stream().anyMatch(m -> m.getTags().contains(GAZETTEER_METACARD_TAG));
  }

  private boolean containsGazetteerMetacards(final UpdateResponse updateResponse) {
    return updateResponse.getUpdatedMetacards().stream().anyMatch(this::containsGazetteerMetacard);
  }

  private boolean containsGazetteerMetacard(final Update update) {
    return update.getOldMetacard().getTags().contains(GAZETTEER_METACARD_TAG)
        || update.getNewMetacard().getTags().contains(GAZETTEER_METACARD_TAG);
  }

  private synchronized void scheduleSuggesterIndexBuild() throws PluginExecutionException {
    if (future != null) {
      future.cancel(false);
    }

    try {
      future = executor.schedule(buildSuggesterIndex, 1, TimeUnit.MINUTES);
    } catch (RejectedExecutionException e) {
      LOGGER.warn(
          "The offline gazetteer's suggester index could not be built automatically. It may have "
              + "to be built manually with the 'gazetteer:build-suggester-index' command.");
      throw new PluginExecutionException(e);
    }
  }
}
