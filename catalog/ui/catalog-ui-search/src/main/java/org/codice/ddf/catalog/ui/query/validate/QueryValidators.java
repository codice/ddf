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
package org.codice.ddf.catalog.ui.query.validate;

import ddf.catalog.validation.QueryValidator;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A collection of {@code QueryValidator} instances that have been registered in the system. These
 * instances can be fetched by their unique id (see {@code QueryValidator#getValidatorId}).
 */
public class QueryValidators {

  private static final Logger LOGGER = LoggerFactory.getLogger(QueryValidators.class);

  private Map<String, QueryValidator> validators = new HashMap<>();

  /**
   * Gets a registered {@code QueryValidator} instance by it's id.
   *
   * @return a {@code QueryValidator} instance whose id matches {@code id}, otherwise null.
   */
  public QueryValidator get(String id) {
    return validators.getOrDefault(id, null);
  }

  /**
   * Adds a new {@code QueryValidator} to the {@code validators} map. Called by blueprint when a new
   * {@code QueryValidator} is registered as a service.
   *
   * @param queryValidator the new {@code QueryValidator} to be registered.
   */
  public void bind(QueryValidator queryValidator) {
    if (queryValidator != null) {
      LOGGER.trace("Adding query validator with id \"{}\"", queryValidator.getValidatorId());
      validators.put(queryValidator.getValidatorId(), queryValidator);
    }
  }

  /**
   * Removes an existing {@code QueryValidator} from the {@code validators} map. Called by blueprint
   * when an existing {@code QueryValidator} service is removed.
   *
   * @param queryValidator the {@code QueryValidator} to be removed from the collection.
   */
  public void unbind(QueryValidator queryValidator) {
    if (queryValidator != null) {
      LOGGER.trace("Removing query validator with id \"{}\"", queryValidator);
      validators.remove(queryValidator.getValidatorId());
    }
  }
}
