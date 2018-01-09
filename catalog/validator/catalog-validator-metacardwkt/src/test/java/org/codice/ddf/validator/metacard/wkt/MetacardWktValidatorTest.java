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
package org.codice.ddf.validator.metacard.wkt;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.Validation;
import ddf.catalog.validation.ValidationException;
import ddf.catalog.validation.report.MetacardValidationReport;
import ddf.catalog.validation.violation.ValidationViolation;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.codice.ddf.validator.wkt.WktValidatorImpl;
import org.junit.Test;

public class MetacardWktValidatorTest {

  public static final String TEST_ATTRIBUTE = "test-attribute";

  private MetacardWktValidator metacardWktValidator =
      new MetacardWktValidator(new WktValidatorImpl(), TEST_ATTRIBUTE);

  @Test
  public void testMetacardWithValidWkt() throws Exception {
    Metacard metacard = getMetacard();
    String wktLoc = "POINT(50 50)";
    metacard.setAttribute(new AttributeImpl(TEST_ATTRIBUTE, wktLoc));
    metacardWktValidator.validate(metacard);
  }

  @Test(expected = ValidationException.class)
  public void testMetacardWithInvalidLocation() throws Exception {
    Metacard metacard = getMetacard();
    String wktLoc = "POINT(250  250)";
    metacard.setAttribute(new AttributeImpl(TEST_ATTRIBUTE, wktLoc));
    metacardWktValidator.validate(metacard);
  }

  @Test
  public void testNoLocation() throws Exception {
    Metacard metacard = getMetacard();
    metacardWktValidator.validate(metacard);
  }

  @Test
  public void testLocationValidationErrorNoLocation() throws Exception {
    String validationMessage = TEST_ATTRIBUTE + " is invalid: POINT(2000 2000)";
    Metacard metacard = getMetacard();
    metacard.setAttribute(new AttributeImpl(Validation.VALIDATION_ERRORS, validationMessage));
    Optional<MetacardValidationReport> validationReportOptional =
        metacardWktValidator.validateMetacard(metacard);
    assertThat(validationReportOptional.isPresent(), is(true));

    Set<ValidationViolation> validationViolationSet =
        validationReportOptional.get().getMetacardValidationViolations();
    assertThat(validationViolationSet, hasSize(1));
    assertThat(validationViolationSet.iterator().next().getMessage(), equalTo(validationMessage));
  }

  @Test
  public void testNoLocationNoErrors() throws Exception {
    Metacard metacard = getMetacard();
    Optional<MetacardValidationReport> validationReportOptional =
        metacardWktValidator.validateMetacard(metacard);
    assertThat(validationReportOptional.isPresent(), is(false));
  }

  @Test
  public void testNoLocationNonLocationError() throws Exception {
    Metacard metacard = getMetacard();
    metacard.setAttribute(
        new AttributeImpl(Validation.VALIDATION_ERRORS, "another attribute error"));
    Optional<MetacardValidationReport> validationReportOptional =
        metacardWktValidator.validateMetacard(metacard);
    assertThat(validationReportOptional.isPresent(), is(false));
  }

  private Metacard getMetacard() {
    Metacard metacard = new MetacardImpl();
    metacard.setAttribute(new AttributeImpl(Core.ID, UUID.randomUUID().toString()));
    return metacard;
  }
}
