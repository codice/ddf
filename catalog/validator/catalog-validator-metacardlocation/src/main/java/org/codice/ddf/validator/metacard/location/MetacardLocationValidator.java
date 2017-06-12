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
package org.codice.ddf.validator.metacard.location;

import java.io.Serializable;
import java.text.ParseException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.context.jts.JtsSpatialContextFactory;
import org.locationtech.spatial4j.exception.InvalidShapeException;
import org.locationtech.spatial4j.io.WKTReader;

import com.google.common.collect.ImmutableSet;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.Validation;
import ddf.catalog.validation.MetacardValidator;
import ddf.catalog.validation.ReportingMetacardValidator;
import ddf.catalog.validation.ValidationException;
import ddf.catalog.validation.impl.ValidationExceptionImpl;
import ddf.catalog.validation.impl.report.MetacardValidationReportImpl;
import ddf.catalog.validation.impl.violation.ValidationViolationImpl;
import ddf.catalog.validation.report.MetacardValidationReport;
import ddf.catalog.validation.violation.ValidationViolation;

public class MetacardLocationValidator implements MetacardValidator, ReportingMetacardValidator {
    private static final JtsSpatialContextFactory JTS_SPATIAL_CONTEXT_FACTORY =
            new JtsSpatialContextFactory();

    private static final String ERROR_MSG = Core.LOCATION + " is invalid: ";

    private static final Set<String> VALIDATED_ATTRIBUTES = ImmutableSet.of(Core.LOCATION);

    static {
        JTS_SPATIAL_CONTEXT_FACTORY.allowMultiOverlap = true;
    }

    private static final SpatialContext SPATIAL_CONTEXT =
            JTS_SPATIAL_CONTEXT_FACTORY.newSpatialContext();

    @Override
    public Optional<MetacardValidationReport> validateMetacard(Metacard metacard) {
        MetacardValidationReportImpl report = null;

        if (StringUtils.isNotEmpty(metacard.getLocation())) {
            WKTReader wktReader = new WKTReader(SPATIAL_CONTEXT, JTS_SPATIAL_CONTEXT_FACTORY);
            try {
                wktReader.parse(metacard.getLocation());
            } catch (ParseException | InvalidShapeException e) {
                String message = ERROR_MSG + metacard.getLocation();
                ValidationViolation violation = new ValidationViolationImpl(VALIDATED_ATTRIBUTES,
                        message,
                        ValidationViolation.Severity.ERROR);
                report = new MetacardValidationReportImpl();
                report.addMetacardViolation(violation);
            }
        } else {
            Attribute validationErrorAttr = metacard.getAttribute(Validation.VALIDATION_ERRORS);
            if (validationErrorAttr != null) {
                List<Serializable> errors = validationErrorAttr.getValues();
                for (Serializable error : errors) {
                    String errorStr = String.valueOf(error);
                    if (errorStr.startsWith(ERROR_MSG)) {
                        ValidationViolation violation = new ValidationViolationImpl(VALIDATED_ATTRIBUTES,
                                errorStr,
                                ValidationViolation.Severity.ERROR);
                        report = new MetacardValidationReportImpl();
                        report.addMetacardViolation(violation);
                    }
                }
            }
        }

        return Optional.ofNullable(report);
    }

    @Override
    public void validate(Metacard metacard) throws ValidationException {
        Optional<MetacardValidationReport> validationReport = validateMetacard(metacard);
        if (validationReport.isPresent()) {
            MetacardValidationReport report = validationReport.get();
            Set<ValidationViolation> violations = report.getMetacardValidationViolations();
            if (!violations.isEmpty()) {
                metacard.setAttribute(new AttributeImpl(Core.LOCATION, (Serializable) null));
                final ValidationExceptionImpl exception = new ValidationExceptionImpl();
                exception.setErrors(violations.stream()
                        .map(ValidationViolation::getMessage)
                        .collect(Collectors.toList()));
                throw exception;
            }
        }

    }
}
