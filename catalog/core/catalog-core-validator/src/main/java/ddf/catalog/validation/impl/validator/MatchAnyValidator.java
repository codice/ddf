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
package ddf.catalog.validation.impl.validator;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.validation.AttributeValidator;
import ddf.catalog.validation.impl.report.AttributeValidationReportImpl;
import ddf.catalog.validation.report.AttributeValidationReport;
import ddf.catalog.validation.violation.ValidationViolation;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.collections.CollectionUtils;

public class MatchAnyValidator implements AttributeValidator {
  private final List<AttributeValidator> validators;

  public MatchAnyValidator(List<AttributeValidator> validators) {
    this.validators = validators;
  }

  @Override
  public Optional<AttributeValidationReport> validate(Attribute attribute) {
    if (attribute == null || CollectionUtils.isEmpty(validators)) {
      return Optional.empty();
    }

    List<Serializable> attributeValues = attribute.getValues();
    List<AttributeValidationReport> resultValidationReportList = new ArrayList<>();

    for (Serializable serializable : attributeValues) {

      List<AttributeValidationReport> validationReportList =
          validateAttributeSerializable(attribute.getName(), serializable);

      if (validationReportList.size() == validators.size()) {
        resultValidationReportList.addAll(validationReportList);
      }
    }

    return generateValidationReports(resultValidationReportList);
  }

  private List<AttributeValidationReport> validateAttributeSerializable(
      String attributeName, Serializable serializable) {
    Attribute newAttribute = new AttributeImpl(attributeName, serializable);
    List<AttributeValidationReport> validationReportList = new ArrayList<>();

    for (AttributeValidator attributeValidator : validators) {
      Optional<AttributeValidationReport> attributeValidationReport =
          attributeValidator.validate(newAttribute);
      if (attributeValidationReport.isPresent()) {
        validationReportList.add(attributeValidationReport.get());
      }
    }

    return validationReportList;
  }

  private Optional<AttributeValidationReport> generateValidationReports(
      List<AttributeValidationReport> validationReportList) {

    if (CollectionUtils.isEmpty(validationReportList)) {
      return Optional.empty();
    }

    AttributeValidationReportImpl result = new AttributeValidationReportImpl();

    for (AttributeValidationReport attributeValidationReport : validationReportList) {

      Set<ValidationViolation> validationViolations =
          attributeValidationReport.getAttributeValidationViolations();
      Set<Map<String, String>> suggestedValues = attributeValidationReport.getSuggestedValues();

      result.addViolations(validationViolations);

      if (CollectionUtils.isNotEmpty(suggestedValues)) {
        result.addSuggestedValues(suggestedValues);
      }
    }
    return Optional.of(result);
  }
}
