package ddf.catalog.validation;

import ddf.catalog.operation.QueryRequest;
import ddf.catalog.validation.violation.QueryValidationViolation;
import java.util.Set;

/**
 * A {@code QueryRequest} may be structurally valid but be semantically incorrect. For example, a
 * {@code QueryRequest} might include a valid {@code Filter} but the specified source may not
 * support it. A {@code QueryValidator} inspects a {@link QueryRequest} for these types of
 * violations.
 *
 * <p><b> This code is experimental. While this interface is functional and tested, it may change or
 * be removed in a future version of the library. </b>
 */
public interface QueryValidator {

  /**
   * Returns the unique identifier corresponding to this validator instance. This is used as a
   * marker to determine which violations were created by each validator instance.
   *
   * @return the id of this validator
   */
  String getValidatorId();

  /**
   * Validates a {@link QueryRequest} for semantic correctness.
   *
   * @param request - the {@link QueryRequest} to validate
   * @return a {@code Set} of violations found in the {@code request}.
   */
  Set<QueryValidationViolation> validate(QueryRequest request);
}
