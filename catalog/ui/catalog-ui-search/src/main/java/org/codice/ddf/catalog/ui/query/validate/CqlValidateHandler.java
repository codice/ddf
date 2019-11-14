package org.codice.ddf.catalog.ui.query.validate;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ddf.catalog.CatalogFramework;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.validation.QueryValidator;
import ddf.catalog.validation.violation.QueryValidationViolation;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.codice.ddf.catalog.ui.query.cql.CqlRequest;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import org.codice.gsonsupport.GsonTypeAdapters.DateLongFormatTypeAdapter;
import org.codice.gsonsupport.GsonTypeAdapters.LongDoubleTypeAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Route;

public class CqlValidateHandler implements Route {

  private static final Logger LOGGER = LoggerFactory.getLogger(CqlValidateHandler.class);

  private static final Gson GSON =
      new GsonBuilder()
          .disableHtmlEscaping()
          .serializeNulls()
          .registerTypeAdapterFactory(LongDoubleTypeAdapter.FACTORY)
          .registerTypeAdapter(Date.class, new DateLongFormatTypeAdapter())
          .create();

  private QueryValidatorsById queryValidatorsById;

  private CatalogFramework catalogFramework;

  private FilterBuilder filterBuilder;

  private EndpointUtil util;

  // TODO remove dependency on endpoint util?
  public CqlValidateHandler(
      QueryValidatorsById queryValidatorsById,
      CatalogFramework catalogFramework,
      FilterBuilder filterBuilder,
      EndpointUtil util) {
    this.queryValidatorsById = queryValidatorsById;
    this.catalogFramework = catalogFramework;
    this.filterBuilder = filterBuilder;
    this.util = util;
  }

  @Override
  public Object handle(Request request, Response response) throws Exception {
    CqlRequest cqlRequest = GSON.fromJson(util.safeGetBody(request), CqlRequest.class);
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

    Set<QueryValidationViolation> violations =
        validator.validate(cqlRequest.createQueryRequest(catalogFramework.getId(), filterBuilder));
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
