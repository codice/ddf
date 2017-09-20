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
package org.codice.ddf.resourcemanagement.query.plugin;

import ddf.catalog.plugin.PostFederatedQueryPlugin;
import ddf.catalog.plugin.PreFederatedQueryPlugin;
import java.util.Map;
import java.util.UUID;

/**
 * A {@link QueryMonitorPlugin} monitors ActiveSearches by maintaining a {@link Map} with a key of
 * {@link UUID} and a value of {@link ActiveSearch}. {@link QueryMonitorPlugin} extends both {@link
 * PreFederatedQueryPlugin} and {@link PostFederatedQueryPlugin} in order to process each search for
 * every source. The {@link PreFederatedQueryPlugin} should be implemented to add the {@link
 * ActiveSearch} to the {@link Map} and the {@link PostFederatedQueryPlugin} should be implemented
 * to remove the {@link ActiveSearch} from the {@link Map}.
 */
public interface QueryMonitorPlugin extends PreFederatedQueryPlugin, PostFederatedQueryPlugin {

  /**
   * Returns a copy of the {@link ActiveSearch} {@link Map}
   *
   * @return {@link Map} <{@link UUID}, {@link ActiveSearch}> copy of the {@link ActiveSearch}
   *     {@link Map}
   */
  Map<UUID, ActiveSearch> getActiveSearches();

  /**
   * Sets a boolean which determines whether or not searches will be removed from the {@link
   * ActiveSearch} {@link Map} upon search completion. A value of true will remove searches from the
   * {@link Map} as the searches finish and a value of false will keep the searches in the {@link
   * Map} after they complete.
   *
   * @param b boolean value. A value of false will keep {@link ActiveSearch}'s in the {@link Map}
   *     even after they finish (are no longer active). True will remove the ActiveSearches from the
   *     {@link Map} as the searches complete (default behavior is true)
   */
  void setRemoveSearchAfterComplete(boolean b);

  /**
   * Removes an ActiveSearch from the {@link ActiveSearch} {@link Map} using it's UUID as a key
   *
   * @param id java.util.UUID that corresponds to the ActiveSearch to be removed from the {@link
   *     Map}
   * @return boolean indicating if removing the ActiveSearch from the {@link Map} was successful
   */
  boolean removeActiveSearch(UUID id);

  /**
   * Adds an ActiveSearch to the {@link ActiveSearch} {@link Map}
   *
   * @param as ActiveSearch that will be added to the {@link Map}
   * @return boolean indicating if adding the {@link ActiveSearch} was successful
   */
  boolean addActiveSearch(ActiveSearch as);
}
