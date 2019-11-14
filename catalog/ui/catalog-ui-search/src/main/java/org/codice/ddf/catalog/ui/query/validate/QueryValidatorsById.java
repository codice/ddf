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
