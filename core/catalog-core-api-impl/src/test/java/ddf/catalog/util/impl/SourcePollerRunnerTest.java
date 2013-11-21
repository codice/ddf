/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.catalog.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import ddf.catalog.data.ContentType;
import ddf.catalog.data.impl.ContentTypeImpl;
import ddf.catalog.source.Source;
import ddf.catalog.util.impl.CachedSource;
import ddf.catalog.util.impl.SourcePollerRunner;
import ddf.catalog.util.impl.SourceStatus;

public class SourcePollerRunnerTest {

    private Source createDefaultFederatedSource(boolean avail,
            Set<ContentType> types) {
        Source source = mock(Source.class);

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
        Source source = createDefaultFederatedSource(true, types);
        CachedSource cached = null;
        runner.bind(source);

        SourceStatus status = null;
        do {
            Thread.yield();
            cached = runner.getCachedSource(source);
            if(cached != null) {
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
        verify(source, times(1)).getVersion();
        verify(source, times(1)).getTitle();
        verify(source, times(1)).getOrganization();
        verify(source, times(1)).getDescription();
    }

    @Test
    public void testDoesntUpdateUnexpiredCachedValuesOnUnAvailableSource() {
        SourcePollerRunner runner = new SourcePollerRunner();
        Set<ContentType> types = createContentTypes();
        Source source = createDefaultFederatedSource(false, types);
        CachedSource cached;
        runner.bind(source);

        SourceStatus status = null;
        do {
            Thread.yield();
            cached = runner.getCachedSource(source);
            if(cached != null) {
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
        verify(source, never()).getVersion();
        verify(source, never()).getTitle();
        verify(source, never()).getOrganization();
        verify(source, never()).getDescription();
    }

}
