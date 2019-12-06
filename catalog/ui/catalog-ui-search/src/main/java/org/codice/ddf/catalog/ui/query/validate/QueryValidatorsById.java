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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryValidatorsById {

  private static final Logger LOGGER = LoggerFactory.getLogger(QueryValidatorsById.class);

  private Map<String, QueryValidator> validators = Collections.emptyMap();

  public QueryValidator get(String id) {
    return validators.getOrDefault(id, null);
  }

  public void setQueryValidators(List<QueryValidator> validatorsList) {
    LOGGER.error("Setting {} query validator(s)", validatorsList.size());
    this.validators =
        validatorsList
            .stream()
            .collect(Collectors.toMap(QueryValidator::getValidatorId, validator -> validator));
  }
}
