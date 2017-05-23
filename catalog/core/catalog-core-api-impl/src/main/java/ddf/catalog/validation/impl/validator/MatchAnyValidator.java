/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.validation.impl.validator;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;

import ddf.catalog.data.Attribute;
import ddf.catalog.validation.AttributeValidator;
import ddf.catalog.validation.impl.report.AttributeValidationReportImpl;
import ddf.catalog.validation.report.AttributeValidationReport;
import ddf.catalog.validation.violation.ValidationViolation;

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

        List<Optional<AttributeValidationReport>> validationReportList = validators.stream()
                .map(validator -> validator.validate(attribute))
                .collect(Collectors.toList());

        return generateValidationReports(validationReportList);
    }

    private Optional<AttributeValidationReport> generateValidationReports(
            List<Optional<AttributeValidationReport>> validationReportList) {

        AttributeValidationReportImpl result = new AttributeValidationReportImpl();

        for (Optional<AttributeValidationReport> attributeValidationReportOptional : validationReportList) {

            if (attributeValidationReportOptional.isPresent()) {
                AttributeValidationReport attributeValidationReport =
                        attributeValidationReportOptional.get();

                Set<ValidationViolation> validationViolations =
                        attributeValidationReport.getAttributeValidationViolations();
                Set<String> suggestedValues = attributeValidationReport.getSuggestedValues();


                if (CollectionUtils.isEmpty(validationViolations)) {
                    return Optional.empty();
                }

                result.addViolations(validationViolations);

                if (CollectionUtils.isNotEmpty(suggestedValues)) {
                    result.addSuggestedValues(suggestedValues);
                }
            } else {
                return Optional.empty();
            }
        }
        return Optional.of(result);
    }
}
