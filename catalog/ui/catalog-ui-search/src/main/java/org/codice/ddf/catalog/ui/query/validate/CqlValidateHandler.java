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

import com.google.common.collect.ImmutableMap;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.validation.QueryValidator;
import ddf.catalog.validation.violation.QueryValidationViolation;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;

public class CqlValidateHandler implements Route {

  private static final Logger LOGGER = LoggerFactory.getLogger(CqlValidateHandler.class);

  private QueryValidatorsById queryValidatorsById;

  private CqlRequestParser parser;

  public CqlValidateHandler(QueryValidatorsById queryValidatorsById, CqlRequestParser parser) {
    this.queryValidatorsById = queryValidatorsById;
    this.parser = parser;
  }

  @Override
  public Object handle(Request request, Response response) throws Exception {
    String validatorId = request.params(":validatorId");

    QueryValidator validator = queryValidatorsById.get(validatorId);
    if (validator == null) {
      LOGGER.debug(
          "No query validator could be found with id \"{}\". Skipping validation.", validatorId);
      response.status(404);
      return ImmutableMap.of(
          "error",
          "No validator matching id " + validatorId,
          "validationViolations",
          Collections.emptyList());
    }

    QueryRequest queryRequest = parser.parse(request);
    Set<QueryValidationViolation> violations = validator.validate(queryRequest);

    List<Map<String, Object>> violationResponses =
        violations
            .stream()
            .map(v -> constructViolationResponse(v, validatorId))
            .collect(Collectors.toList());
    return ImmutableMap.of("validationViolations", violationResponses);
  }

  private Map<String, Object> constructViolationResponse(
      QueryValidationViolation violation, String type) {
    return ImmutableMap.of(
        "type",
        type,
        "severity",
        violation.getSeverity(),
        "message",
        violation.getMessage(),
        "extraData",
        violation.getExtraData());
  }
}
