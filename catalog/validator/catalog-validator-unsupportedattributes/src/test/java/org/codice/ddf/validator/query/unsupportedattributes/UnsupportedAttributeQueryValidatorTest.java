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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.source.Source;
import ddf.catalog.source.SourceAttributeRestriction;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.validation.violation.QueryValidationViolation;
import ddf.catalog.validation.violation.QueryValidationViolation.Severity;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.mockito.Mockito;

public class UnsupportedAttributeQueryValidatorTest {

  private UnsupportedAttributeQueryValidator validator;

  private AttributeExtractor attributeExtractor = Mockito.mock(AttributeExtractor.class);

  @Test
  public void testGetValidatorId() {
    validator = new UnsupportedAttributeQueryValidator(attributeExtractor);
    assertThat(validator.getValidatorId(), is("unsupportedAttribute"));
  }

  @Test
  public void testValidateNoViolationsIfRestrictionsListIsEmpty() throws UnsupportedQueryException {
    List<SourceAttributeRestriction> noRestrictions = Collections.emptyList();
    Mockito.when(attributeExtractor.extractAttributes(any())).thenReturn(ImmutableSet.of("attr-1"));
    validator = new UnsupportedAttributeQueryValidator(attributeExtractor);
    validator.setSourceAttributeRestrictions(noRestrictions);

    QueryRequest request = mockQueryRequest("src-1");
    Set<QueryValidationViolation> violations = validator.validate(request);

    assertThat(violations, hasSize(0));
  }

  @Test
  public void testValidateSingleValidAttribute() throws UnsupportedQueryException {
    List<SourceAttributeRestriction> restrictions =
        ImmutableList.of(mockRestriction("src-1", ImmutableSet.of("attr")));
    Mockito.when(attributeExtractor.extractAttributes(any())).thenReturn(ImmutableSet.of("attr"));
    QueryRequest request = mockQueryRequest("src-1");
    validator = new UnsupportedAttributeQueryValidator(attributeExtractor);
    validator.setSourceAttributeRestrictions(restrictions);

    Set<QueryValidationViolation> violations = validator.validate(request);

    assertThat(violations, hasSize(0));
  }

  @Test
  public void testValidateSingleInvalidAttribute() throws UnsupportedQueryException {
    List<SourceAttributeRestriction> restrictions =
        ImmutableList.of(mockRestriction("src-1", ImmutableSet.of("attr-1")));
    Mockito.when(attributeExtractor.extractAttributes(any())).thenReturn(ImmutableSet.of("attr-2"));
    QueryRequest request = mockQueryRequest("src-1");
    validator = new UnsupportedAttributeQueryValidator(attributeExtractor);
    validator.setSourceAttributeRestrictions(restrictions);

    Set<QueryValidationViolation> violations = validator.validate(request);

    assertThat(violations, hasSize(1));
    for (QueryValidationViolation violation : violations) {
      assertThat(violation.getSeverity(), is(Severity.ERROR));
      assertThat(
          violation.getMessage(),
          is("The field \"attr-2\" is not supported by the src-1 Content Store(s)"));
      Map<String, Object> extraData = violation.getExtraData();
      assertThat(extraData.get("attribute"), is("attr-2"));
      assertThat((Set<String>) extraData.get("sources"), Matchers.hasItems("src-1"));
    }
  }

  @Test
  public void testValidateNoViolationsIfSourceDoesNotHaveRestriction()
      throws UnsupportedQueryException {
    List<SourceAttributeRestriction> restrictions =
        ImmutableList.of(mockRestriction("src-2", ImmutableSet.of("attr-2")));
    Mockito.when(attributeExtractor.extractAttributes(any())).thenReturn(ImmutableSet.of("attr-2"));
    QueryRequest request = mockQueryRequest("src-1");
    validator = new UnsupportedAttributeQueryValidator(attributeExtractor);
    validator.setSourceAttributeRestrictions(restrictions);

    Set<QueryValidationViolation> violations = validator.validate(request);

    assertThat(violations, hasSize(0));
  }

  @Test
  public void testValidateNoViolationsIfFailedToGetAttributesFromQuery()
      throws UnsupportedQueryException {
    List<SourceAttributeRestriction> restrictions =
        ImmutableList.of(mockRestriction("src-1", ImmutableSet.of("attr-1")));
    Mockito.when(attributeExtractor.extractAttributes(any()))
        .thenThrow(UnsupportedQueryException.class);
    QueryRequest request = mockQueryRequest("src-1");
    validator = new UnsupportedAttributeQueryValidator(attributeExtractor);
    validator.setSourceAttributeRestrictions(restrictions);

    Set<QueryValidationViolation> violations = validator.validate(request);

    assertThat(violations, hasSize(0));
  }

  @Test
  public void testValidateMultipleInvalidAttributes() throws UnsupportedQueryException {
    List<SourceAttributeRestriction> restrictions =
        ImmutableList.of(mockRestriction("src-1", ImmutableSet.of("attr-1")));
    Mockito.when(attributeExtractor.extractAttributes(any()))
        .thenReturn(ImmutableSet.of("attr-2", "attr-3"));
    QueryRequest request = mockQueryRequest("src-1");
    validator = new UnsupportedAttributeQueryValidator(attributeExtractor);
    validator.setSourceAttributeRestrictions(restrictions);

    Set<QueryValidationViolation> violations = validator.validate(request);

    assertThat(violations, hasSize(2));
    for (QueryValidationViolation violation : violations) {
      assertThat(violation.getSeverity(), is(Severity.ERROR));
      Map<String, Object> extraData = violation.getExtraData();
      assertThat((Set<String>) extraData.get("sources"), Matchers.hasItems("src-1"));
      String attribute = (String) extraData.get("attribute");
      if (attribute.equals("attr-2")) {
        assertThat(
            violation.getMessage(),
            is("The field \"attr-2\" is not supported by the src-1 Content Store(s)"));
      } else if (attribute.equals("attr-3")) {
        assertThat(
            violation.getMessage(),
            is("The field \"attr-3\" is not supported by the src-1 Content Store(s)"));
      } else {
        fail("Unknown attribute found in violations");
      }
    }
  }

  @Test
  public void testValidateTwoSourcesProduceCorrectMessage() throws UnsupportedQueryException {
    List<SourceAttributeRestriction> restrictions =
        ImmutableList.of(
            mockRestriction("src-1", ImmutableSet.of("attr-1")),
            mockRestriction("src-2", ImmutableSet.of("attr-1")));
    Mockito.when(attributeExtractor.extractAttributes(any())).thenReturn(ImmutableSet.of("attr-2"));
    QueryRequest request = mockQueryRequest("src-1", "src-2");
    validator = new UnsupportedAttributeQueryValidator(attributeExtractor);
    validator.setSourceAttributeRestrictions(restrictions);

    Set<QueryValidationViolation> violations = validator.validate(request);

    assertThat(violations, hasSize(1));
    for (QueryValidationViolation violation : violations) {
      assertThat(violation.getSeverity(), is(Severity.ERROR));
      assertThat(
          violation.getMessage(),
          is("The field \"attr-2\" is not supported by the src-1 and src-2 Content Store(s)"));
      Map<String, Object> extraData = violation.getExtraData();
      assertThat(extraData.get("attribute"), is("attr-2"));
      assertThat((Set<String>) extraData.get("sources"), Matchers.hasItems("src-1", "src-2"));
    }
  }

  @Test
  public void testValidateThreeSourcesProduceCorrectMessage() throws UnsupportedQueryException {
    List<SourceAttributeRestriction> restrictions =
        ImmutableList.of(
            mockRestriction("src-1", ImmutableSet.of("attr-1")),
            mockRestriction("src-2", ImmutableSet.of("attr-1")),
            mockRestriction("src-3", ImmutableSet.of("attr-1")));
    Mockito.when(attributeExtractor.extractAttributes(any())).thenReturn(ImmutableSet.of("attr-2"));
    QueryRequest request = mockQueryRequest("src-1", "src-2", "src-3");
    validator = new UnsupportedAttributeQueryValidator(attributeExtractor);
    validator.setSourceAttributeRestrictions(restrictions);

    Set<QueryValidationViolation> violations = validator.validate(request);

    assertThat(violations, hasSize(1));
    for (QueryValidationViolation violation : violations) {
      assertThat(violation.getSeverity(), is(Severity.ERROR));
      assertThat(
          violation.getMessage(),
          is(
              "The field \"attr-2\" is not supported by the src-1, src-2, and src-3 Content Store(s)"));
      Map<String, Object> extraData = violation.getExtraData();
      assertThat(extraData.get("attribute"), is("attr-2"));
      assertThat(
          (Set<String>) extraData.get("sources"), Matchers.hasItems("src-1", "src-2", "src-3"));
    }
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
}
