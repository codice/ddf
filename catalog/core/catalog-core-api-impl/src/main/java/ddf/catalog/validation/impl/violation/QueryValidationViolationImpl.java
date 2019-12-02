package ddf.catalog.validation.impl.violation;

import ddf.catalog.validation.violation.QueryValidationViolation;
import java.util.Map;

public class QueryValidationViolationImpl implements QueryValidationViolation {

  private Severity severity;

  private String message;

  private Map<String, Object> extraData;

  public QueryValidationViolationImpl(
      final Severity severity, final String message, final Map<String, Object> extraData) {
    this.severity = severity;
    this.message = message;
    this.extraData = extraData;
  }

  @Override
  public Severity getSeverity() {
    return severity;
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public Map<String, Object> getExtraData() {
    return extraData;
  }
}
