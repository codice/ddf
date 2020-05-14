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
package ddf.catalog.cache.solr.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CacheBulkProcessorTest {

  @Captor ArgumentCaptor<Collection<Metacard>> capturedMetacards;

  private CacheBulkProcessor cacheBulkProcessor;

  @Mock private SolrCache mockSolrCache;

  @Before
  public void setUp() throws Exception {
    cacheBulkProcessor =
        new CacheBulkProcessor(mockSolrCache, 1, TimeUnit.MILLISECONDS, CacheStrategy.ALL);
    cacheBulkProcessor.setBatchSize(10);
  }

  @After
  public void tearDown() throws Exception {
    cacheBulkProcessor.shutdown();
  }

  @Test
  public void bulkAdd() throws Exception {
    cacheBulkProcessor.setFlushInterval(TimeUnit.MINUTES.toMillis(1));
    List<Result> mockResults = getMockResults(10);

    cacheBulkProcessor.add(mockResults);
    waitForPendingMetacardsToCache();

    verify(mockSolrCache, times(1)).put(capturedMetacards.capture());
    assertThat(capturedMetacards.getValue()).containsAll(getMetacards(mockResults));
  }

  @Test
  public void partialFlush() throws Exception {
    cacheBulkProcessor.setFlushInterval(1);
    List<Result> mockResults = getMockResults(1);

    cacheBulkProcessor.add(mockResults);
    waitForPendingMetacardsToCache();

    verify(mockSolrCache, times(1)).put(capturedMetacards.capture());
    assertThat(capturedMetacards.getValue()).containsAll(getMetacards(mockResults));
  }

  @Test
  public void nullResult() throws Exception {
    cacheBulkProcessor.add(Collections.singletonList((Result) null));

    verify(mockSolrCache, never()).put(anyCollection());
  }

  @Test
  public void nullMetacard() throws Exception {
    Result mockResult = mock(Result.class);
    when(mockResult.getMetacard()).thenReturn(null);

    cacheBulkProcessor.add(Collections.singletonList(mockResult));

    verify(mockSolrCache, never()).put(anyCollection());
  }

  @Test
  public void exceedsBacklog() throws Exception {
    cacheBulkProcessor.setMaximumBacklogSize(0);
    cacheBulkProcessor.add(getMockResults(10));

    verify(mockSolrCache, never()).put(anyCollection());
  }

  @Test
  public void cacheThrowsExcpetion() throws Exception {
    doThrow(new RuntimeException()).doNothing().when(mockSolrCache).put(anyCollection());
    List<Result> mockResults = getMockResults(10);

    cacheBulkProcessor.add(mockResults);
    waitForPendingMetacardsToCache();

    verify(mockSolrCache, atLeast(2)).put(capturedMetacards.capture());
    for (Collection<Metacard> metacards : capturedMetacards.getAllValues()) {
      assertThat(metacards).containsAll(getMetacards(mockResults));
    }
  }

  @Test
  public void updateMetacards() throws Exception {
    cacheBulkProcessor.setFlushInterval(TimeUnit.MINUTES.toMillis(1));

    List<List<Result>> mockResultHalfs = Lists.partition(getMockResults(10), 5);
    List<Result> mockResults = new ArrayList<>(mockResultHalfs.get(0));

    // Duplicate the first half
    Collections.addAll(mockResults, mockResultHalfs.get(0).toArray(new Result[5]));
    // Add the second half
    Collections.addAll(mockResults, mockResultHalfs.get(1).toArray(new Result[5]));

    cacheBulkProcessor.add(mockResults);
    waitForPendingMetacardsToCache();

    verify(mockSolrCache).put(capturedMetacards.capture());
    assertThat(capturedMetacards.getValue()).containsAll(getMetacards(mockResults));
  }

  private void waitForPendingMetacardsToCache() throws InterruptedException {
    while (cacheBulkProcessor.pendingMetacards() > 0) {
      Thread.sleep(2);
    }
  }

  private Collection<Metacard> getMetacards(List<Result> results) {
    List<Metacard> metacards = new ArrayList<>(results.size());

    for (Result result : results) {
      metacards.add(result.getMetacard());
    }

    return metacards;
  }

  private List<Result> getMockResults(int size) {
    List<Result> results = new ArrayList<>(size);

    for (int i = 0; i < size; i++) {
      Metacard mockMetacard = mock(Metacard.class);
      when(mockMetacard.getId()).thenReturn(Integer.toString(i));

      Result mockResult = mock(Result.class);
      when(mockResult.getMetacard()).thenReturn(mockMetacard);

      results.add(mockResult);
    }

    return results;
  }
}
