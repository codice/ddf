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
package ddf.catalog.util.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.data.ContentType;
import ddf.catalog.data.impl.ContentTypeImpl;
import ddf.catalog.source.Source;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class SourcePollerRunnerTest {

  private Source createDefaultFederatedSource(
      boolean avail, Set<ContentType> types, String src, String version) {
    Source source = mock(Source.class);

    when(source.getTitle()).thenReturn(src);
    when(source.getVersion()).thenReturn(version);
    when(source.isAvailable()).thenReturn(avail);
    when(source.getContentTypes()).thenReturn(types);

    return source;
  }

  private Set<ContentType> createContentTypes() {
    Set<ContentType> types = new HashSet<ContentType>();

    types.add(new ContentTypeImpl("Type1", "v1.1"));
    types.add(new ContentTypeImpl("Type2", "v1.2"));
    return types;
  }

  @Test
  public void testDoesntUpdateUnexpiredCachedValuesOnAvailableSource() {
    SourcePollerRunner runner = new SourcePollerRunner();
    Set<ContentType> types = createContentTypes();
    Source source = createDefaultFederatedSource(true, types, "src", "1");
    CachedSource cached = null;
    runner.bind(source);

    SourceStatus status = null;
    do {
      Thread.yield();
      cached = runner.getCachedSource(source);
      if (cached != null) {
        status = cached.getSourceStatus();
      }
    } while (status == null || status == SourceStatus.UNCHECKED);

    for (int i = 0; i < 10; i++) {
      cached.isAvailable();
      cached.getSourceStatus();
      cached.getContentTypes();
      cached.getVersion();
      cached.getTitle();
      cached.getOrganization();
      cached.getId();
      cached.getDescription();
    }
    assertEquals(SourceStatus.AVAILABLE, cached.getSourceStatus());
    assertEquals(true, cached.isAvailable());
    assertEquals(types, cached.getContentTypes());

    verify(source, times(1)).isAvailable();
    verify(source, times(1)).getContentTypes();
  }

  @Test
  public void testDoesntUpdateUnexpiredCachedValuesOnUnAvailableSource() {
    SourcePollerRunner runner = new SourcePollerRunner();
    Set<ContentType> types = createContentTypes();
    Source source = createDefaultFederatedSource(false, types, "src", "1");
    CachedSource cached;
    runner.bind(source);

    SourceStatus status = null;
    do {
      Thread.yield();
      cached = runner.getCachedSource(source);
      if (cached != null) {
        status = cached.getSourceStatus();
      }
    } while (status == null || status == SourceStatus.UNCHECKED);

    for (int i = 0; i < 10; i++) {
      cached.isAvailable();
      cached.getSourceStatus();
      cached.getContentTypes();
      cached.getVersion();
      cached.getTitle();
      cached.getOrganization();
      cached.getId();
      cached.getDescription();
    }
    assertEquals(SourceStatus.UNAVAILABLE, cached.getSourceStatus());
    assertEquals(false, cached.isAvailable());

    verify(source, times(1)).isAvailable();
    verify(source, never()).getContentTypes();
  }

  @Test
  public void testCorrectSourceGetsIdentified() {
    SourcePollerRunner runner = new SourcePollerRunner();
    Set<ContentType> types = createContentTypes();
    Source source = createDefaultFederatedSource(true, types, "src", "1");
    Source source2 = createDefaultFederatedSource(true, types, "src2", "2");
    CachedSource cached;
    CachedSource cached2;
    runner.bind(source);
    runner.bind(source2);

    SourceStatus status = null;
    SourceStatus status2 = null;
    do {
      Thread.yield();
      cached = runner.getCachedSource(source);
      cached2 = runner.getCachedSource(source2);
      if (cached != null) {
        status = cached.getSourceStatus();
      }
      if (cached2 != null) {
        status2 = cached2.getSourceStatus();
      }
    } while (status == null
        || status == SourceStatus.UNCHECKED
        || status2 == null
        || status2 == SourceStatus.UNCHECKED);

    for (int i = 0; i < 10; i++) {
      cached.isAvailable();
      cached.getContentTypes();
      cached2.isAvailable();
      cached2.getContentTypes();
    }
    assertEquals(SourceStatus.AVAILABLE, cached.getSourceStatus());
    assertEquals(true, cached.isAvailable());

    assertEquals(SourceStatus.AVAILABLE, cached2.getSourceStatus());
    assertEquals(true, cached2.isAvailable());

    assertEquals(source.getTitle(), cached.getTitle());
    assertEquals(source2.getTitle(), cached2.getTitle());

    verify(source, times(1)).isAvailable();
    verify(source, times(1)).getContentTypes();
    verify(source2, times(1)).isAvailable();
    verify(source2, times(1)).getContentTypes();
  }

  @Test
  public void testKnownSourceRepresentedByDifferentObjectDiscovered() {
    SourcePollerRunner runner = new SourcePollerRunner();
    Set<ContentType> types = createContentTypes();
    Source source = createDefaultFederatedSource(false, types, "src1", "1");
    Source source2 = createDefaultFederatedSource(false, types, "src2", "1");
    CachedSource cached;
    runner.bind(source);

    SourceStatus status = null;
    do {
      Thread.yield();
      cached = runner.getCachedSource(source);
      if (cached != null) {
        status = cached.getSourceStatus();
      }
    } while (status == null || status == SourceStatus.UNCHECKED);

    for (int i = 0; i < 10; i++) {
      cached.isAvailable();
      cached.getSourceStatus();
      cached.getContentTypes();
      cached.getVersion();
      cached.getTitle();
      cached.getOrganization();
      cached.getId();
      cached.getDescription();
    }
    assertEquals(SourceStatus.UNAVAILABLE, cached.getSourceStatus());
    assertEquals(false, cached.isAvailable());

    verify(source, times(1)).isAvailable();
    verify(source, never()).getContentTypes();
  }

  @Test
  public void testNonExistantWithUnknownTitleIsntFound() {
    SourcePollerRunner runner = new SourcePollerRunner();
    Set<ContentType> types = createContentTypes();
    Source source = createDefaultFederatedSource(true, types, "src", "1");
    Source source2 = createDefaultFederatedSource(true, types, "src2", "1");
    CachedSource cached = null;
    runner.bind(source);

    SourceStatus status = null;
    do {
      Thread.yield();
      cached = runner.getCachedSource(source);
      if (cached != null) {
        status = cached.getSourceStatus();
      }
    } while (status == null || status == SourceStatus.UNCHECKED);

    assertNull(runner.getCachedSource(source2));
  }

  @Test
  public void testNonExistantWithDuplicateTitleButWrongVersionIsntFound() {
    SourcePollerRunner runner = new SourcePollerRunner();
    Set<ContentType> types = createContentTypes();
    Source source = createDefaultFederatedSource(true, types, "src", "1");
    Source source2 = createDefaultFederatedSource(true, types, "src", "2");
    CachedSource cached = null;
    runner.bind(source);

    SourceStatus status = null;
    do {
      Thread.yield();
      cached = runner.getCachedSource(source);
      if (cached != null) {
        status = cached.getSourceStatus();
      }
    } while (status == null || status == SourceStatus.UNCHECKED);

    assertNull(runner.getCachedSource(source2));
  }
}
