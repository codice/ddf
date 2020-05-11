/*
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

package org.codice.ddf.validator.metacard.duplication;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.validation.ValidationException;
import ddf.catalog.validation.report.MetacardValidationReport;
import ddf.catalog.validation.violation.ValidationViolation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengis.filter.Filter;

@RunWith(MockitoJUnitRunner.class)
public class DuplicationValidatorTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private FilterBuilder mockFilterBuilder;

  @Mock private CatalogFramework mockFramework;

  private DuplicationValidator validator;

  private MetacardImpl matchingMetacard;

  private MetacardImpl testMetacard;

  private static final String ID = "matching metacard id";

  private static final String TAG1 = "1";

  private static final String TAG2 = "2";

  private Set tags = new HashSet<>(Arrays.asList(TAG1, TAG2));

  @Before
  public void setup()
      throws UnsupportedQueryException, SourceUnavailableException, FederationException {

    QueryResponse response = mock(QueryResponse.class);

    when(mockFramework.query(any(QueryRequest.class))).thenReturn(response);

    testMetacard = new MetacardImpl();
    matchingMetacard = new MetacardImpl();
    matchingMetacard.setId(ID);
    testMetacard.setId("test metacard ID");
    matchingMetacard.setAttribute(new AttributeImpl(Metacard.CHECKSUM, "checksum-value"));
    testMetacard.setAttribute(new AttributeImpl(Metacard.CHECKSUM, "checksum-value"));
    matchingMetacard.setTags(tags);
    testMetacard.setTags(tags);

    List<Result> results = Arrays.asList(new ResultImpl(matchingMetacard));

    when(response.getResults()).thenReturn(results);
    validator = new DuplicationValidator(mockFramework, mockFilterBuilder);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateMetacardNullInput() {
    validator.validateMetacard(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidateNullInput() throws ValidationException {
    validator.validate(null);
  }

  @Test
  public void testValidateMetacardNullConfiguration() throws ValidationException {

    validator.setWarnOnDuplicateAttributes(null);
    validator.setErrorOnDuplicateAttributes(null);

    Optional<MetacardValidationReport> report = validator.validateMetacard(testMetacard);
    assertThat(report.isPresent(), is(false));
  }

  @Test
  public void testValidateNullConfiguration() throws ValidationException {

    validator.setWarnOnDuplicateAttributes(null);
    validator.setErrorOnDuplicateAttributes(null);

    // verify no exception thrown
    validator.validate(testMetacard);
  }

  @Test
  public void testValidateMetacardBlankConfiguration() throws ValidationException {

    validator.setWarnOnDuplicateAttributes(new String[0]);
    validator.setErrorOnDuplicateAttributes(new String[0]);

    Optional<MetacardValidationReport> report = validator.validateMetacard(testMetacard);
    assertThat(report.isPresent(), is(false));
  }

  @Test
  public void testValidateBlankConfiguration() throws ValidationException {

    validator.setWarnOnDuplicateAttributes(new String[0]);
    validator.setErrorOnDuplicateAttributes(new String[0]);

    // verify no exception thrown
    validator.validate(testMetacard);
  }

  @Test
  public void testValidateMetacardWithValidationErrorAndWarning() {

    String[] checksumAttribute = {Metacard.CHECKSUM};
    String[] idAttribute = {Metacard.ID};

    validator.setWarnOnDuplicateAttributes(checksumAttribute);
    validator.setErrorOnDuplicateAttributes(idAttribute);

    Optional<MetacardValidationReport> report = validator.validateMetacard(testMetacard);
    assertThat(report.isPresent(), is(true));

    assertThat(report.get().getMetacardValidationViolations(), hasSize(2));

    Map<ValidationViolation.Severity, ValidationViolation> violations =
        report
            .get()
            .getMetacardValidationViolations()
            .stream()
            .collect(Collectors.toMap(ValidationViolation::getSeverity, Function.identity()));

    ValidationViolation warnViolation = violations.get(ValidationViolation.Severity.WARNING);
    ValidationViolation errorViolation = violations.get(ValidationViolation.Severity.ERROR);

    assertThat(warnViolation.getAttributes(), is(new HashSet<>(Arrays.asList(checksumAttribute))));
    assertThat(warnViolation.getMessage(), containsString(Metacard.CHECKSUM));
    assertThat(errorViolation.getAttributes(), is(new HashSet<>(Arrays.asList(idAttribute))));
    assertThat(errorViolation.getMessage(), containsString(Metacard.ID));
  }

  @Test
  public void testValidateWithValidationErrorAndWarning() throws ValidationException {

    String[] checksumAttribute = {Metacard.CHECKSUM};
    String[] idAttribute = {Metacard.ID};
    ValidationException expectedException = null;

    validator.setWarnOnDuplicateAttributes(checksumAttribute);
    validator.setErrorOnDuplicateAttributes(idAttribute);

    try {
      validator.validate(testMetacard);
    } catch (ValidationException e) {
      expectedException = e;
    }

    assertThat(expectedException, is(not(nullValue())));
    assertThat(expectedException.getMessage(), is(not(nullValue())));
    assertThat(expectedException.getErrors().size(), is(1));
    assertThat(expectedException.getWarnings().size(), is(1));

    expectedException
        .getWarnings()
        .forEach(warning -> assertThat(warning, containsString(Metacard.CHECKSUM)));
    expectedException.getErrors().forEach(error -> assertThat(error, containsString(Metacard.ID)));
  }

  @Test
  public void testValidateMetacardWithMultiValuedAttribute()
      throws StopProcessingException, PluginExecutionException, FederationException,
          UnsupportedQueryException, SourceUnavailableException {
    String[] tagAttributes = {Metacard.TAGS};

    ArgumentCaptor<String> attributeValueCaptor = ArgumentCaptor.forClass(String.class);

    ArgumentCaptor<QueryRequest> queryRequestCaptor = ArgumentCaptor.forClass(QueryRequest.class);
    when(mockFilterBuilder.attribute(anyString()).equalTo().text(attributeValueCaptor.capture()))
        .thenReturn(mock(Filter.class));

    String[] attributes = {Metacard.TAGS};
    validator.setWarnOnDuplicateAttributes(attributes);

    Optional<MetacardValidationReport> report = validator.validateMetacard(testMetacard);
    assertThat(report.isPresent(), is(true));

    assertThat(report.get().getMetacardValidationViolations().size(), is(1));
    verify(mockFramework).query(queryRequestCaptor.capture());
    assertThat(attributeValueCaptor.getAllValues(), hasItems(TAG1, TAG2));
    report
        .get()
        .getMetacardValidationViolations()
        .forEach(
            violation -> {
              assertThat(
                  violation.getAttributes(), is(new HashSet<>(Arrays.asList(tagAttributes))));
              assertThat(violation.getMessage(), containsString(Metacard.TAGS));
            });
  }
}
