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
package org.codice.ddf.catalog.ui.query.cql;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.security.SourceWarningsFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public class SourceWarningsFilterManagerTest {

  @Test
  public void testNullProcessingDetailsProduceEmptyWarnings() {
    SourceWarningsFilter mockedFilter = mock(SourceWarningsFilter.class);

    SourceWarningsFilterManager filterManager =
        new SourceWarningsFilterManager(Collections.singletonList(mockedFilter));

    Set<String> filteredWarnings = filterManager.getFilteredWarningsFrom(null);

    assertThat(filteredWarnings, is(Collections.emptySet()));
  }

  @Test
  public void testProcessingDetailsWithNullSourceIdProduceEmptyWarnings() {
    ProcessingDetails mockedProcessingDetails = mock(ProcessingDetails.class);
    when(mockedProcessingDetails.getSourceId()).thenReturn(null);

    SourceWarningsFilter mockedFilter = mock(SourceWarningsFilter.class);

    SourceWarningsFilterManager filterManager =
        new SourceWarningsFilterManager(Collections.singletonList(mockedFilter));

    Set<String> filteredWarnings = filterManager.getFilteredWarningsFrom(mockedProcessingDetails);

    assertThat(filteredWarnings, is(Collections.emptySet()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullFiltersProduceIllegalArgumentException() {
    new SourceWarningsFilterManager(null);
  }

  @Test
  public void testEmptyFiltersProduceEmptyWarnings() {
    ProcessingDetails mockedProcessingDetails = mock(ProcessingDetails.class);

    SourceWarningsFilterManager filterManager =
        new SourceWarningsFilterManager(Collections.emptyList());

    Set<String> filteredWarnings = filterManager.getFilteredWarningsFrom(mockedProcessingDetails);

    assertThat(filteredWarnings, is(Collections.emptySet()));
  }

  @Test
  public void testManagerIgnoresNullMemberOfFilters() {
    ProcessingDetails mockedProcessingDetails = mock(ProcessingDetails.class);

    SourceWarningsFilterManager filterManager =
        new SourceWarningsFilterManager(Collections.singletonList(null));

    Set<String> filteredWarnings = filterManager.getFilteredWarningsFrom(mockedProcessingDetails);

    assertThat(filteredWarnings, is(Collections.emptySet()));
  }

  @Test
  public void testFilterWithNullIdProducesEmptyWarnings() {
    ProcessingDetails mockedProcessingDetails = mock(ProcessingDetails.class);

    SourceWarningsFilter mockedFilter = mock(SourceWarningsFilter.class);
    when(mockedFilter.getId()).thenReturn(null);

    SourceWarningsFilterManager filterManager =
        new SourceWarningsFilterManager(Collections.singletonList(mockedFilter));

    Set<String> filteredWarnings = filterManager.getFilteredWarningsFrom(mockedProcessingDetails);

    assertThat(filteredWarnings, is(Collections.emptySet()));
  }

  @Test
  public void testProcessingDetailsWithUnsupportedSourceIdProduceEmptyWarnings() {
    ProcessingDetails mockedProcessingDetails = mock(ProcessingDetails.class);
    when(mockedProcessingDetails.getSourceId()).thenReturn("unsupported source");

    SourceWarningsFilter mockedFilter = mock(SourceWarningsFilter.class);
    when(mockedFilter.getId()).thenReturn("supported source");

    SourceWarningsFilterManager filterManager =
        new SourceWarningsFilterManager(Collections.singletonList(mockedFilter));

    Set<String> filteredWarnings = filterManager.getFilteredWarningsFrom(mockedProcessingDetails);

    assertThat(filteredWarnings, is(Collections.emptySet()));
  }

  @Test
  public void testProcessingDetailsWithSupportedSourceIdGetFiltered() {
    String supportedSource = "supported source";

    ProcessingDetails mockedProcessingDetails = mock(ProcessingDetails.class);
    when(mockedProcessingDetails.getSourceId()).thenReturn(supportedSource);

    SourceWarningsFilter mockedFilter = mock(SourceWarningsFilter.class);
    when(mockedFilter.getId()).thenReturn(supportedSource);

    Set<String> warningsFilteredFromSupportedSource = new HashSet<>();
    warningsFilteredFromSupportedSource.add("Your query failed for this specific reason.");
    warningsFilteredFromSupportedSource.add(
        "Your query failed for a reason which we cannot specify.");
    when(mockedFilter.filter(mockedProcessingDetails))
        .thenReturn(warningsFilteredFromSupportedSource);

    SourceWarningsFilterManager filterManager =
        new SourceWarningsFilterManager(Collections.singletonList(mockedFilter));

    Set<String> filteredWarnings = filterManager.getFilteredWarningsFrom(mockedProcessingDetails);

    assertThat(filteredWarnings, is(warningsFilteredFromSupportedSource));
  }

  @Test
  public void testManagerAdheresToPriorityOfFilters() {
    String testSource = "test source";

    ProcessingDetails mockedProcessingDetailsFromTestSource = mock(ProcessingDetails.class);
    when(mockedProcessingDetailsFromTestSource.getSourceId()).thenReturn(testSource);

    SourceWarningsFilter mockedPrioritizedFilterForTestSource = mock(SourceWarningsFilter.class);
    when(mockedPrioritizedFilterForTestSource.getId()).thenReturn(testSource);

    Set<String> warningsWhichPrioritizedFilterLetsThrough = new HashSet<>();
    warningsWhichPrioritizedFilterLetsThrough.add("Your query failed for this specific reason.");
    warningsWhichPrioritizedFilterLetsThrough.add(
        "Your query failed for a reason which we cannot specify.");
    when(mockedPrioritizedFilterForTestSource.filter(mockedProcessingDetailsFromTestSource))
        .thenReturn(warningsWhichPrioritizedFilterLetsThrough);

    SourceWarningsFilter mockedNotPrioritizedFilterForTestSource = mock(SourceWarningsFilter.class);
    when(mockedNotPrioritizedFilterForTestSource.getId()).thenReturn(testSource);

    Set<String> warningWhichNotPrioritizedFilterLetsThrough = new HashSet<>();
    warningWhichNotPrioritizedFilterLetsThrough.add("Your query failed.");
    when(mockedNotPrioritizedFilterForTestSource.filter(mockedProcessingDetailsFromTestSource))
        .thenReturn(warningWhichNotPrioritizedFilterLetsThrough);

    ArrayList<SourceWarningsFilter> filters = new ArrayList<>();
    filters.add(mockedPrioritizedFilterForTestSource);
    filters.add(mockedNotPrioritizedFilterForTestSource);

    SourceWarningsFilterManager filterManager = new SourceWarningsFilterManager(filters);

    Set<String> filteredWarnings =
        filterManager.getFilteredWarningsFrom(mockedProcessingDetailsFromTestSource);

    assertThat(filteredWarnings, is(warningsWhichPrioritizedFilterLetsThrough));
  }

  @Test
  public void testManagerIteratesThroughFiltersToFindACompatibleOne() {
    String testSource = "test source";
    String notTestSource = "not test source";

    ProcessingDetails mockedProcessingDetailsFromTestSource = mock(ProcessingDetails.class);
    when(mockedProcessingDetailsFromTestSource.getSourceId()).thenReturn(testSource);

    List<SourceWarningsFilter> filters = new ArrayList<>();
    filters.add(null);

    SourceWarningsFilter mockedFilterWithNullId = mock(SourceWarningsFilter.class);
    when(mockedFilterWithNullId.getId()).thenReturn(null);
    filters.add(mockedFilterWithNullId);

    SourceWarningsFilter mockedFilterForNotTestSource = mock(SourceWarningsFilter.class);
    when(mockedFilterForNotTestSource.getId()).thenReturn(notTestSource);
    filters.add(mockedFilterForNotTestSource);

    SourceWarningsFilter mockedFilterForTestSource = mock(SourceWarningsFilter.class);
    when(mockedFilterForTestSource.getId()).thenReturn(testSource);

    Set<String> filteredWarningsFromTestSource = new HashSet<>();
    filteredWarningsFromTestSource.add("Your query failed for this specific reason.");
    filteredWarningsFromTestSource.add("Your query failed for a reason which we cannot specify.");
    when(mockedFilterForTestSource.filter(mockedProcessingDetailsFromTestSource))
        .thenReturn(filteredWarningsFromTestSource);
    filters.add(mockedFilterForTestSource);

    SourceWarningsFilterManager filterManager = new SourceWarningsFilterManager(filters);

    Set<String> filteredWarnings =
        filterManager.getFilteredWarningsFrom(mockedProcessingDetailsFromTestSource);

    verify(mockedFilterWithNullId, times(1)).getId();
    verify(mockedFilterForNotTestSource, times(1)).getId();
    verify(mockedFilterForTestSource, times(1)).getId();
    assertThat(filteredWarnings, is(filteredWarningsFromTestSource));
  }
}
