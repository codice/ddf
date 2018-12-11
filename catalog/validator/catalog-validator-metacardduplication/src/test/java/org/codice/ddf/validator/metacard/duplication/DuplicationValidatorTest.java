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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.data.types.Validation;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import java.io.Serializable;
import java.util.AbstractMap.SimpleEntry;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DuplicationValidatorTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private FilterBuilder mockFilterBuilder;

  @Mock private CatalogFramework mockFramework;

  private DuplicationValidator validator;

  private Metacard matchingMetacard;

  private static final String EXISTING_CHECKSUM = "1";

  private static final String TAG1 = "1";

  private static final String TAG2 = "2";

  private Set tags = new HashSet<>(Arrays.asList(TAG1, TAG2));

  @Captor ArgumentCaptor<Attribute> attributeCaptor;

  @Before
  public void setup()
      throws UnsupportedQueryException, SourceUnavailableException, FederationException {

    matchingMetacard = getMockMetacard(EXISTING_CHECKSUM);
    List<Result> results = Arrays.asList(new ResultImpl(matchingMetacard));

    QueryResponse response = mock(QueryResponse.class);
    when(response.getResults()).thenReturn(results);

    when(mockFramework.query(any(QueryRequest.class))).thenReturn(response);

    validator = new DuplicationValidator(mockFramework, mockFilterBuilder);

    // set default configuration
    validator.setErrorOnDuplicateAttributes(new String[] {Metacard.CHECKSUM});
  }

  @Test
  public void testMarkDuplicateChecksumError()
      throws StopProcessingException, PluginExecutionException {
    Metacard duplicateMetacard = getMockMetacard(EXISTING_CHECKSUM);
    Metacard nonDuplicateMetacard = getMockMetacard("Not Duplicating!");

    CreateRequest result =
        validator.process(
            getMockCreateRequest(ImmutableList.of(duplicateMetacard, nonDuplicateMetacard)));
    assertThat(result.getMetacards().size(), Matchers.is(2));
    verify(duplicateMetacard, atLeastOnce()).setAttribute(attributeCaptor.capture());
    assertThat(
        attributeCaptor
            .getAllValues()
            .stream()
            .anyMatch(attr -> attr.getName().equals(Validation.VALIDATION_ERRORS)),
        Matchers.is(true));
  }

  @Test
  public void testMarkDuplicateChecksumWarning()
      throws StopProcessingException, PluginExecutionException {
    validator.setWarnOnDuplicateAttributes(new String[] {Metacard.CHECKSUM});

    Metacard duplicateMetacard = getMockMetacard(EXISTING_CHECKSUM);
    Metacard nonDuplicateMetacard = getMockMetacard("Not Duplicating!");

    CreateRequest result =
        validator.process(
            getMockCreateRequest(ImmutableList.of(duplicateMetacard, nonDuplicateMetacard)));
    assertThat(result.getMetacards().size(), Matchers.is(2));
    verify(duplicateMetacard, atLeastOnce()).setAttribute(attributeCaptor.capture());
    assertThat(
        attributeCaptor
            .getAllValues()
            .stream()
            .anyMatch(attr -> attr.getName().equals(Validation.VALIDATION_WARNINGS)),
        Matchers.is(true));
  }

  @Test
  public void testRejectDuplicateChecksum()
      throws StopProcessingException, PluginExecutionException {
    validator.setRejectOnDuplicateAttributes(new String[] {Metacard.CHECKSUM});

    Metacard duplicateMetacard = getMockMetacard(EXISTING_CHECKSUM);
    Metacard nonDuplicateMetacard = getMockMetacard("Not Duplicating!");

    CreateRequest result =
        validator.process(
            getMockCreateRequest(ImmutableList.of(duplicateMetacard, nonDuplicateMetacard)));
    assertThat(result.getMetacards().size(), Matchers.is(1));
  }

  @Test
  public void testValidateMetacardNullConfiguration()
      throws StopProcessingException, PluginExecutionException {
    // remove default
    validator.setErrorOnDuplicateAttributes(new String[0]);

    validator.setWarnOnDuplicateAttributes(null);
    validator.setErrorOnDuplicateAttributes(null);
    validator.setRejectOnDuplicateAttributes(null);

    Metacard duplicateMetacard = getMockMetacard(EXISTING_CHECKSUM);
    Metacard nonDuplicateMetacard = getMockMetacard("Not Duplicating!");

    CreateRequest result =
        validator.process(
            getMockCreateRequest(ImmutableList.of(duplicateMetacard, nonDuplicateMetacard)));
    assertThat(result.getMetacards().size(), Matchers.is(2));
    verify(duplicateMetacard, times(0)).setAttribute(attributeCaptor.capture());
  }

  @Test
  public void testValidateMetacardBlankConfiguration()
      throws StopProcessingException, PluginExecutionException {

    validator.setWarnOnDuplicateAttributes(new String[0]);
    validator.setErrorOnDuplicateAttributes(new String[0]);
    validator.setRejectOnDuplicateAttributes(new String[0]);

    Metacard duplicateMetacard = getMockMetacard(EXISTING_CHECKSUM);
    Metacard nonDuplicateMetacard = getMockMetacard("Not Duplicating!");

    CreateRequest result =
        validator.process(
            getMockCreateRequest(ImmutableList.of(duplicateMetacard, nonDuplicateMetacard)));
    assertThat(result.getMetacards().size(), Matchers.is(2));
    verify(duplicateMetacard, times(0)).setAttribute(attributeCaptor.capture());
  }

  @Test
  public void testRejectUpdate() {
    validator.setRejectOnDuplicateAttributes(new String[] {Metacard.CHECKSUM});

    Metacard duplicateMetacard = getMockMetacard(EXISTING_CHECKSUM);
    Metacard nonDuplicateMetacard = getMockMetacard("Not Duplicating!");
    List<Entry<Serializable, Metacard>> entryList =
        ImmutableList.of(
            new SimpleEntry<>("attr", duplicateMetacard),
            new SimpleEntry<>("attr", nonDuplicateMetacard));

    UpdateRequest result = validator.process(getMockUpdateRequest("attr", entryList));
    assertThat(result.getUpdates().size(), Matchers.is(1));
  }

  @Test
  public void testMarkErrorUpdate() {

    Metacard duplicateMetacard = getMockMetacard(EXISTING_CHECKSUM);
    Metacard nonDuplicateMetacard = getMockMetacard("Not Duplicating!");
    List<Entry<Serializable, Metacard>> entryList =
        ImmutableList.of(
            new SimpleEntry<>("attr", duplicateMetacard),
            new SimpleEntry<>("attr", nonDuplicateMetacard));

    UpdateRequest result = validator.process(getMockUpdateRequest("attr", entryList));

    assertThat(result.getUpdates().size(), Matchers.is(2));
    verify(duplicateMetacard, atLeastOnce()).setAttribute(attributeCaptor.capture());
    assertThat(
        attributeCaptor
            .getAllValues()
            .stream()
            .anyMatch(attr -> attr.getName().equals(Validation.VALIDATION_ERRORS)),
        Matchers.is(true));
  }

  @Test
  public void testMarkWarningUpdate() {
    validator.setWarnOnDuplicateAttributes(new String[] {Metacard.CHECKSUM});

    Metacard duplicateMetacard = getMockMetacard(EXISTING_CHECKSUM);
    Metacard nonDuplicateMetacard = getMockMetacard("Not Duplicating!");
    List<Entry<Serializable, Metacard>> entryList =
        ImmutableList.of(
            new SimpleEntry<>("attr", duplicateMetacard),
            new SimpleEntry<>("attr", nonDuplicateMetacard));

    UpdateRequest result = validator.process(getMockUpdateRequest("attr", entryList));

    assertThat(result.getUpdates().size(), Matchers.is(2));
    verify(duplicateMetacard, atLeastOnce()).setAttribute(attributeCaptor.capture());
    assertThat(
        attributeCaptor
            .getAllValues()
            .stream()
            .anyMatch(attr -> attr.getName().equals(Validation.VALIDATION_WARNINGS)),
        Matchers.is(true));
  }

  private CreateRequest getMockCreateRequest(List<Metacard> metacards) {
    CreateRequest mockCreateRequest = mock(CreateRequest.class);
    doReturn(metacards).when(mockCreateRequest).getMetacards();
    return mockCreateRequest;
  }

  private UpdateRequest getMockUpdateRequest(
      String attribute, List<Entry<Serializable, Metacard>> entries) {
    return new UpdateRequestImpl(
        entries, attribute, ImmutableMap.of("foo", "bar"), ImmutableSet.of("foo"));
  }

  private Metacard getMockMetacard(String checksum) {
    Metacard mockMetacard = mock(Metacard.class);
    Attribute mockAttribute = mock(Attribute.class);
    Attribute mockWarningAttribute = mock(Attribute.class);

    doReturn("warning").when(mockWarningAttribute).getValue();
    doReturn(checksum).when(mockAttribute).getValue();
    doReturn(null).when(mockAttribute).getValues();
    doReturn(mockAttribute).when(mockMetacard).getAttribute(eq(Metacard.CHECKSUM));
    doReturn(mockAttribute).when(mockMetacard).getAttribute(eq(Validation.VALIDATION_WARNINGS));
    return mockMetacard;
  }
}
