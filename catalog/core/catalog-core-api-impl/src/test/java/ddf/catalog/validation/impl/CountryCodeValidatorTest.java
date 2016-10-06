package ddf.catalog.validation.impl;

import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.validation.impl.validator.CountryCodeValidator;
import ddf.catalog.validation.report.AttributeValidationReport;
import org.junit.Test;

import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class CountryCodeValidatorTest {

    @Test
    public void testValidCountryCode() {
        final CountryCodeValidator countryCodeValidator = new CountryCodeValidator();

        Optional<AttributeValidationReport> reportOptional = countryCodeValidator.validate(new AttributeImpl("location.country_code", "USA"));

        assertThat(reportOptional.isPresent(), is(false));
    }

    @Test
    public void testInvalidDataType() {
        final CountryCodeValidator countryCodeValidator = new CountryCodeValidator();

        Optional<AttributeValidationReport> reportOptional = countryCodeValidator.validate(new AttributeImpl("location.country_code", new byte[10]));

        assertThat(reportOptional.isPresent(), is(false));
    }

    @Test
    public void testInvalidCountryCode() {
        final CountryCodeValidator countryCodeValidator = new CountryCodeValidator();

        Optional<AttributeValidationReport> reportOptional = countryCodeValidator.validate(new AttributeImpl("location.country_code", "ZZZ"));

        assertThat(reportOptional.get().getAttributeValidationViolations(), hasSize(1));
    }

    @Test
    public void testEquals() {
        final CountryCodeValidator validator1 = new CountryCodeValidator();
        final CountryCodeValidator validator2 = new CountryCodeValidator();
        assertThat(validator1.equals(validator2), is(true));
        assertThat(validator2.equals(validator1), is(true));
    }

    @Test
    public void testEqualsSelf() {
        final CountryCodeValidator validator = new CountryCodeValidator();
        assertThat(validator.equals(validator), is(true));
    }

    @Test
    public void testEqualsNull() {
        final CountryCodeValidator validator = new CountryCodeValidator();
        assertThat(validator.equals(null), is(false));
    }

    @Test
    public void testHashCode() {
        final CountryCodeValidator validator1 = new CountryCodeValidator();
        final CountryCodeValidator validator2 = new CountryCodeValidator();
        assertThat(validator1.hashCode(), is(validator2.hashCode()));
    }
}
