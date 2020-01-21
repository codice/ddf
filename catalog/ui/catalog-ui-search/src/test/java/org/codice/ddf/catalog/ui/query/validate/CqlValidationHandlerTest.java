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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.validation.QueryValidator;
import ddf.catalog.validation.impl.violation.QueryValidationViolationImpl;
import ddf.catalog.validation.violation.QueryValidationViolation;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.opengis.filter.Filter;
import spark.Request;
import spark.Response;

public class CqlValidationHandlerTest {

  private CqlValidationHandler cqlValidationHandler;

  private CqlRequestParser parser = mock(CqlRequestParser.class);

  private QueryValidators queryValidators = mock(QueryValidators.class);

  private FilterBuilder filterBuilder = new GeotoolsFilterBuilder();

  @Test
  public void testHandleUnknownValidatorId() throws Exception {
    when(queryValidators.get(any())).thenReturn(null);
    QueryRequest queryRequest =
        createQueryRequest(filterBuilder.attribute("attr").is().text("val"), "src");
    when(parser.parse(any())).thenReturn(queryRequest);
    Request request = mock(Request.class);
    when(request.params(":validatorId")).thenReturn("invalidId");
    Response response = mock(Response.class);

    cqlValidationHandler = new CqlValidationHandler(queryValidators, parser);
    Object objResponse = cqlValidationHandler.handle(request, response);

    verify(response).status(404);
    Map<String, Object> jsonResponse = (Map<String, Object>) objResponse;
    assertThat(jsonResponse.get("error"), is("No validator matching id invalidId"));
    assertThat((List<Object>) jsonResponse.get("validationViolations"), hasSize(0));
  }

  @Test
  public void testHandleOneValidatorOneViolation() throws Exception {
    QueryValidationViolation violation =
        new QueryValidationViolationImpl(
            QueryValidationViolation.Severity.ERROR,
            "there was an error",
            ImmutableMap.of("k", "v"));
    QueryValidator validator = mock(QueryValidator.class);
    when(validator.validate(any())).thenReturn(ImmutableSet.of(violation));
    when(validator.getValidatorId()).thenReturn("id");
    when(queryValidators.get("id")).thenReturn(validator);
    QueryRequest queryRequest = mock(QueryRequest.class);
    when(parser.parse(any())).thenReturn(queryRequest);
    Request request = mock(Request.class);
    when(request.params(":validatorId")).thenReturn("id");
    Response response = mock(Response.class);

    cqlValidationHandler = new CqlValidationHandler(queryValidators, parser);
    Object objResponse = cqlValidationHandler.handle(request, response);

    Map<String, Object> jsonResponse = (Map<String, Object>) objResponse;
    List<Object> violations = (List<Object>) jsonResponse.get("validationViolations");
    assertThat(violations, hasSize(1));
    assertViolation(
        (Map<String, Object>) violations.get(0),
        QueryValidationViolation.Severity.ERROR,
        "there was an error",
        ImmutableMap.of("k", "v"),
        "id");
  }

  @Test
  public void testHandleOneValidatorTwoViolations() throws Exception {
    QueryValidationViolation violation1 =
        new QueryValidationViolationImpl(
            QueryValidationViolation.Severity.ERROR, "ONE", ImmutableMap.of("k1", "v1"));
    QueryValidationViolation violation2 =
        new QueryValidationViolationImpl(
            QueryValidationViolation.Severity.WARNING, "TWO", ImmutableMap.of("k2", "v2"));
    QueryValidator validator = mock(QueryValidator.class);
    when(validator.validate(any())).thenReturn(ImmutableSet.of(violation1, violation2));
    when(validator.getValidatorId()).thenReturn("id");
    when(queryValidators.get("id")).thenReturn(validator);
    QueryRequest queryRequest = mock(QueryRequest.class);
    when(parser.parse(any())).thenReturn(queryRequest);
    Request request = mock(Request.class);
    when(request.params(":validatorId")).thenReturn("id");
    Response response = mock(Response.class);

    cqlValidationHandler = new CqlValidationHandler(queryValidators, parser);
    Object objResponse = cqlValidationHandler.handle(request, response);

    Map<String, Object> jsonResponse = (Map<String, Object>) objResponse;
    List<Object> violations = (List<Object>) jsonResponse.get("validationViolations");
    assertThat(violations, hasSize(2));
    assertViolation(
        (Map<String, Object>) violations.get(0),
        QueryValidationViolation.Severity.ERROR,
        "ONE",
        ImmutableMap.of("k1", "v1"),
        "id");
    assertViolation(
        (Map<String, Object>) violations.get(1),
        QueryValidationViolation.Severity.WARNING,
        "TWO",
        ImmutableMap.of("k2", "v2"),
        "id");
  }

  private void assertViolation(
      Map<String, Object> actualViolation,
      QueryValidationViolation.Severity severity,
      String message,
      Map<String, Object> extras,
      String validatorId) {
    assertThat(actualViolation.get("severity"), is(severity));
    assertThat(actualViolation.get("message"), is(message));
    assertThat(actualViolation.get("extraData"), is(extras));
    assertThat(actualViolation.get("type"), is(validatorId));
  }

  private QueryRequest createQueryRequest(Filter filter, String... sourceIds) {
    return new QueryRequestImpl(new QueryImpl(filter), Arrays.asList(sourceIds));
  }
}
