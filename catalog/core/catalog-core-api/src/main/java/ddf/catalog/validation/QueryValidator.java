package ddf.catalog.validation;

import ddf.catalog.operation.QueryRequest;
import ddf.catalog.validation.violation.QueryValidationViolation;
import java.util.Set;

public interface QueryValidator {

  // TODO javadoc, this still needed? Move into services desc in blueprint?
  String getValidatorId();

  // TODO javadoc
  Set<QueryValidationViolation> validate(QueryRequest request);
}
