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
package ddf.catalog.validation.impl;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import com.google.common.collect.Sets;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.validation.impl.validator.RequiredAttributesMetacardValidator;
import ddf.catalog.validation.report.MetacardValidationReport;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;

public class RequiredAttributesMetacardValidatorTest {
  @Test
  public void testValidMetacard() {
    final Metacard metacard = new MetacardImpl();
    metacard.setAttribute(new AttributeImpl("title", "test"));
    metacard.setAttribute(new AttributeImpl("created", new Date()));
    metacard.setAttribute(new AttributeImpl("thumbnail", new byte[] {1, 2, 3, 4}));
    validateNoErrors(metacard, Sets.newHashSet("title", "created", "thumbnail"));
  }

  @Test
  public void testInvalidMetacard() {
    final Metacard metacard = new MetacardImpl();
    metacard.setAttribute(new AttributeImpl("title", "test"));
    metacard.setAttribute(new AttributeImpl("created", new Date()));
    metacard.setAttribute(new AttributeImpl("thumbnail", new byte[] {1, 2, 3, 4}));
    validateWithErrors(
        metacard, Sets.newHashSet("title", "created", "thumbnail", "effective", "metadata"), 2);
  }

  private void validateNoErrors(final Metacard metacard, final Set<String> requiredAttributes) {
    final Optional<MetacardValidationReport> reportOptional =
        getReportOptional(metacard, requiredAttributes);
    assertThat(reportOptional.isPresent(), is(false));
  }

  private void validateWithErrors(
      final Metacard metacard, final Set<String> requiredAttributes, final int expectedErrors) {
    final Optional<MetacardValidationReport> reportOptional =
        getReportOptional(metacard, requiredAttributes);
    assertThat(reportOptional.get().getMetacardValidationViolations(), hasSize(expectedErrors));
  }

  private Optional<MetacardValidationReport> getReportOptional(
      final Metacard metacard, final Set<String> requiredAttributes) {
    final RequiredAttributesMetacardValidator validator =
        new RequiredAttributesMetacardValidator(
            metacard.getMetacardType().getName(), requiredAttributes);
    return validator.validateMetacard(metacard);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullMetacardTypeName() {
    new RequiredAttributesMetacardValidator(null, Sets.newHashSet("title"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullRequiredAttributes() {
    new RequiredAttributesMetacardValidator("test", null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyRequiredAttributes() {
    new RequiredAttributesMetacardValidator("test", new HashSet<>());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullMetacard() {
    new RequiredAttributesMetacardValidator("test", Sets.newHashSet("title"))
        .validateMetacard(null);
  }
}
