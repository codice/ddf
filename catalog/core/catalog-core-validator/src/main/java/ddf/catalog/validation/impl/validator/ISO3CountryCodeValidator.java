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

import com.google.common.base.Preconditions;
import ddf.catalog.data.Attribute;
import ddf.catalog.validation.AttributeValidator;
import ddf.catalog.validation.impl.report.AttributeValidationReportImpl;
import ddf.catalog.validation.impl.violation.ValidationViolationImpl;
import ddf.catalog.validation.report.AttributeValidationReport;
import ddf.catalog.validation.violation.ValidationViolation;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.codice.countrycode.standard.StandardProvider;
import org.codice.countrycode.standard.StandardRegistryImpl;

/**
 * Validates an attribute's value(s) against the ISO_3166-1 Alpha3 country codes.
 *
 * <p>Is capable of validating {@link String}s.
 */
public class ISO3CountryCodeValidator implements AttributeValidator {
  private static final String ISO_3166 = "ISO3166";
  private static final String VERSION = "1";

  private final boolean ignoreCase;

  private final Set<String> countryCodes;

  public ISO3CountryCodeValidator(final boolean ignoreCase) {
    this.ignoreCase = ignoreCase;

    StandardProvider standardProvider =
        StandardRegistryImpl.getInstance().lookup(ISO_3166, VERSION);

    if (standardProvider == null) {
      throw new IllegalStateException(
          "StandardProvider lookup failed for [" + ISO_3166 + ", " + VERSION + "]");
    }

    countryCodes =
        standardProvider.getStandardEntries().stream()
            .map(cc -> cc.getAsFormat("alpha3"))
            .collect(Collectors.toSet());
  }

  /**
   * {@inheritDoc}
   *
   * <p>Validates only the values of {@code attribute} that are {@link String}s.
   */
  @Override
  public Optional<AttributeValidationReport> validate(Attribute attribute) {
    Preconditions.checkArgument(attribute != null, "The attribute cannot be null.");

    AttributeValidationReport report = buildReport(attribute);

    return report.getAttributeValidationViolations().isEmpty()
        ? Optional.empty()
        : Optional.of(report);
  }

  private AttributeValidationReport buildReport(Attribute attribute) {
    AttributeValidationReportImpl report = new AttributeValidationReportImpl();

    attribute.getValues().stream()
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .map(ignoreCase ? String::toUpperCase : String::toString)
        .filter(s -> !countryCodes.contains(s))
        .map(
            s ->
                new ValidationViolationImpl(
                    Collections.singleton(attribute.getName()),
                    s + " is not a valid ISO_3166-1 Alpha3 country code.",
                    ValidationViolation.Severity.ERROR))
        .forEach(report::addViolation);

    countryCodes.forEach(report::addSuggestedValue);

    return report;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ISO3CountryCodeValidator that = (ISO3CountryCodeValidator) o;

    return new EqualsBuilder()
        .append(countryCodes, that.countryCodes)
        .append(ignoreCase, that.ignoreCase)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(countryCodes).toHashCode();
  }
}
