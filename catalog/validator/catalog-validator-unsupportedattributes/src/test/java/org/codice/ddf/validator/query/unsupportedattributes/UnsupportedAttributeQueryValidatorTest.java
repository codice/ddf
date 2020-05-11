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
package org.codice.ddf.validator.query.unsupportedattributes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.source.Source;
import ddf.catalog.source.SourceAttributeRestriction;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.validation.impl.violation.QueryValidationViolationImpl;
import ddf.catalog.validation.violation.QueryValidationViolation;
import ddf.catalog.validation.violation.QueryValidationViolation.Severity;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class UnsupportedAttributeQueryValidatorTest {

  private UnsupportedAttributeQueryValidator validator;

  private AttributeExtractor attributeExtractor = Mockito.mock(AttributeExtractor.class);

  private Supplier<String> messageFormatSupplier = (Supplier<String>) Mockito.mock(Supplier.class);

  @Before
  public void setup() {
    Mockito.when(messageFormatSupplier.get())
        .thenReturn("The field \"{attribute}\" is not supported by the {sources} Source(s)");
  }

  @Test
  public void testGetValidatorId() {
    validator = new UnsupportedAttributeQueryValidator(attributeExtractor, messageFormatSupplier);
    assertThat(validator.getValidatorId(), is("unsupportedAttribute"));
  }

  @Test
  public void testValidateNoViolationsIfNoRestrictionsRegistered()
      throws UnsupportedQueryException {
    Mockito.when(attributeExtractor.extractAttributes(any())).thenReturn(ImmutableSet.of("attr-1"));
    validator = new UnsupportedAttributeQueryValidator(attributeExtractor, messageFormatSupplier);

    QueryRequest request = mockQueryRequest("src-1");
    Set<QueryValidationViolation> violations = validator.validate(request);

    assertThat(violations, hasSize(0));
  }

  @Test
  public void testValidateSingleValidAttribute() throws UnsupportedQueryException {
    Mockito.when(attributeExtractor.extractAttributes(any())).thenReturn(ImmutableSet.of("attr"));
    QueryRequest request = mockQueryRequest("src-1");
    validator = new UnsupportedAttributeQueryValidator(attributeExtractor, messageFormatSupplier);
    validator.bind(mockRestriction("src-1", ImmutableSet.of("attr")));

    Set<QueryValidationViolation> violations = validator.validate(request);

    assertThat(violations, hasSize(0));
  }

  @Test
  public void testValidateSingleInvalidAttribute() throws UnsupportedQueryException {
    Mockito.when(attributeExtractor.extractAttributes(any())).thenReturn(ImmutableSet.of("attr-2"));
    QueryRequest request = mockQueryRequest("src-1");
    validator = new UnsupportedAttributeQueryValidator(attributeExtractor, messageFormatSupplier);
    validator.bind(mockRestriction("src-1", ImmutableSet.of("attr-1")));

    Set<QueryValidationViolation> violations = validator.validate(request);

    QueryValidationViolation violation =
        makeViolation(
            "The field \"attr-2\" is not supported by the src-1 Source(s)", "attr-2", "src-1");
    assertThat(violations, hasSize(1));
    assertThat(violations, hasItem(violation));
  }

  @Test
  public void testValidateNoViolationsIfSourceDoesNotHaveRestriction()
      throws UnsupportedQueryException {
    Mockito.when(attributeExtractor.extractAttributes(any())).thenReturn(ImmutableSet.of("attr-2"));
    QueryRequest request = mockQueryRequest("src-1");
    validator = new UnsupportedAttributeQueryValidator(attributeExtractor, messageFormatSupplier);
    validator.bind(mockRestriction("src-2", ImmutableSet.of("attr-2")));

    Set<QueryValidationViolation> violations = validator.validate(request);

    assertThat(violations, hasSize(0));
  }

  @Test
  public void testValidateNoViolationsIfFailedToGetAttributesFromQuery()
      throws UnsupportedQueryException {
    Mockito.when(attributeExtractor.extractAttributes(any()))
        .thenThrow(UnsupportedQueryException.class);
    QueryRequest request = mockQueryRequest("src-1");
    validator = new UnsupportedAttributeQueryValidator(attributeExtractor, messageFormatSupplier);
    validator.bind(mockRestriction("src-1", ImmutableSet.of("attr-1")));

    Set<QueryValidationViolation> violations = validator.validate(request);

    assertThat(violations, hasSize(0));
  }

  @Test
  public void testValidateMultipleInvalidAttributes() throws UnsupportedQueryException {
    Mockito.when(attributeExtractor.extractAttributes(any()))
        .thenReturn(ImmutableSet.of("attr-2", "attr-3"));
    QueryRequest request = mockQueryRequest("src-1");
    validator = new UnsupportedAttributeQueryValidator(attributeExtractor, messageFormatSupplier);
    validator.bind(mockRestriction("src-1", ImmutableSet.of("attr-1")));

    Set<QueryValidationViolation> violations = validator.validate(request);

    QueryValidationViolation violation1 =
        makeViolation(
            "The field \"attr-2\" is not supported by the src-1 Source(s)", "attr-2", "src-1");
    QueryValidationViolation violation2 =
        makeViolation(
            "The field \"attr-3\" is not supported by the src-1 Source(s)", "attr-3", "src-1");
    assertThat(violations, hasSize(2));
    assertThat(violations, hasItems(violation1, violation2));
  }

  @Test
  public void testValidateTwoSourcesProduceCorrectMessage() throws UnsupportedQueryException {
    Mockito.when(attributeExtractor.extractAttributes(any())).thenReturn(ImmutableSet.of("attr-2"));
    QueryRequest request = mockQueryRequest("src-1", "src-2");
    validator = new UnsupportedAttributeQueryValidator(attributeExtractor, messageFormatSupplier);
    validator.bind(mockRestriction("src-1", ImmutableSet.of("attr-1")));
    validator.bind(mockRestriction("src-2", ImmutableSet.of("attr-1")));

    Set<QueryValidationViolation> violations = validator.validate(request);

    QueryValidationViolation violation =
        makeViolation(
            "The field \"attr-2\" is not supported by the src-1 and src-2 Source(s)",
            "attr-2",
            "src-1",
            "src-2");
    assertThat(violations, hasSize(1));
    assertThat(violations, hasItem(violation));
  }

  @Test
  public void testValidateThreeSourcesProduceCorrectMessage() throws UnsupportedQueryException {
    Mockito.when(attributeExtractor.extractAttributes(any())).thenReturn(ImmutableSet.of("attr-2"));
    QueryRequest request = mockQueryRequest("src-1", "src-2", "src-3");
    validator = new UnsupportedAttributeQueryValidator(attributeExtractor, messageFormatSupplier);
    validator.bind(mockRestriction("src-1", ImmutableSet.of("attr-1")));
    validator.bind(mockRestriction("src-2", ImmutableSet.of("attr-1")));
    validator.bind(mockRestriction("src-3", ImmutableSet.of("attr-1")));

    Set<QueryValidationViolation> violations = validator.validate(request);

    QueryValidationViolation violation =
        makeViolation(
            "The field \"attr-2\" is not supported by the src-1, src-2, and src-3 Source(s)",
            "attr-2",
            "src-1",
            "src-2",
            "src-3");
    assertThat(violations, hasSize(1));
    assertThat(violations, hasItem(violation));
  }

  @Test
  public void testBindNullSourceAttributeRestrictionDoesNotErrorOut() {
    validator = new UnsupportedAttributeQueryValidator(attributeExtractor, messageFormatSupplier);
    validator.bind(null);
  }

  @Test
  public void testUnbindNullSourceAttributeRestrictionDoesNotErrorOut() {
    validator = new UnsupportedAttributeQueryValidator(attributeExtractor, messageFormatSupplier);
    validator.unbind(null);
  }

  @Test
  public void testUnbind() throws UnsupportedQueryException {
    Mockito.when(attributeExtractor.extractAttributes(any())).thenReturn(ImmutableSet.of("attr-2"));
    QueryRequest request = mockQueryRequest("src-1");
    validator = new UnsupportedAttributeQueryValidator(attributeExtractor, messageFormatSupplier);

    // There is one violation because a SourceAttributeViolation was bound
    validator.bind(mockRestriction("src-1", ImmutableSet.of("attr-1")));
    Set<QueryValidationViolation> violations = validator.validate(request);
    assertThat(violations, hasSize(1));

    // There are no violations because the SourceAttributeViolation was unbound
    validator.unbind(mockRestriction("src-1", ImmutableSet.of("attr-1")));
    Set<QueryValidationViolation> violationsAfterUnbind = validator.validate(request);
    assertThat(violationsAfterUnbind, hasSize(0));
  }

  private QueryRequest mockQueryRequest(String... sourceIds) {
    QueryRequest request = Mockito.mock(QueryRequest.class);
    Mockito.when(request.getSourceIds()).thenReturn(ImmutableSet.copyOf(sourceIds));
    return request;
  }

  private SourceAttributeRestriction mockRestriction(
      String sourceId, Set<String> supportedAttributes) {
    Source source = Mockito.mock(Source.class);
    Mockito.when(source.getId()).thenReturn(sourceId);
    SourceAttributeRestriction restriction = Mockito.mock(SourceAttributeRestriction.class);
    Mockito.when(restriction.getSource()).thenReturn(source);
    Mockito.when(restriction.getSupportedAttributes()).thenReturn(supportedAttributes);
    return restriction;
  }

  private QueryValidationViolation makeViolation(
      String message, String attribute, String... sources) {
    Map<String, Object> extraData =
        ImmutableMap.of("attribute", attribute, "sources", ImmutableSet.copyOf(sources));
    return new QueryValidationViolationImpl(Severity.ERROR, message, extraData);
  }
}
