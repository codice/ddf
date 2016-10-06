package ddf.catalog.validation.impl.validator;

import com.google.common.base.Preconditions;
import ddf.catalog.data.Attribute;
import ddf.catalog.validation.AttributeValidator;
import ddf.catalog.validation.impl.report.AttributeValidationReportImpl;
import ddf.catalog.validation.impl.violation.ValidationViolationImpl;
import ddf.catalog.validation.report.AttributeValidationReport;
import ddf.catalog.validation.violation.ValidationViolation.Severity;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.io.Serializable;
import java.util.*;

/**
 * Validates an attribute's value(s) against the ISO_3166-1 Alpha3 country codes.
 * <p>
 * Is capable of validating {@link String}s.
 */
public class CountryCodeValidator implements AttributeValidator {
    private Set<String> countryCodesMap;

    //TODO: Should there be an option to ignore case?
    public CountryCodeValidator() {
        setupISO3CountryCodes();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Validates only the values of {@code attribute} that are {@link String}s.
     */
    @Override
    public Optional<AttributeValidationReport> validate(Attribute attribute) {
        Preconditions.checkArgument(attribute != null, "The attribute cannot be null.");

        final String name = attribute.getName();
        for (final Serializable value : attribute.getValues()) {
            if (value instanceof String && !countryCodesMap.contains(value)) {
                final AttributeValidationReportImpl report = new AttributeValidationReportImpl();
                report.addViolation(new ValidationViolationImpl(Collections.singleton(name),
                        name + "is not a valid ISO_3166-1 Alpha3 country code.",
                        Severity.ERROR));

                //TODO: Should the country codes be included in the error message?
                countryCodesMap.forEach(report::addSuggestedValue);
                return Optional.of(report);
            }
        }

        return Optional.empty();
    }

    private void setupISO3CountryCodes() {
        countryCodesMap = new HashSet<String>();
        for (String iso2CountryCode: Locale.getISOCountries()) {
            String iso3CountryCode = new Locale("", iso2CountryCode).getISO3Country();
            countryCodesMap.add(iso3CountryCode);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CountryCodeValidator that = (CountryCodeValidator) o;

        return new EqualsBuilder().append(countryCodesMap, that.countryCodesMap)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(countryCodesMap)
                .toHashCode();
    }
}
