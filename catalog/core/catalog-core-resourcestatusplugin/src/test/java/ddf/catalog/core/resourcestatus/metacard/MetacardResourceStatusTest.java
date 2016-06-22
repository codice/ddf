/**
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
package ddf.catalog.core.resourcestatus.metacard;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ddf.catalog.cache.ResourceCacheInterface;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.resource.data.ReliableResource;

public class MetacardResourceStatusTest {

    @Mock
    private ResourceCacheInterface cache;
    @Mock
    private ReliableResource cachedResource;
    @Mock
    private QueryResponse queryResponse;
    @Mock
    private MetacardImpl basicMetacard;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testMetacardDisplaysCachedProduct() throws Exception {

        setupCachedMock(getBasicMetacard());

        MetacardResourceStatus plugin = new MetacardResourceStatus(cache);

        Attribute resourceStatusAttribute = getReourceStatusAttribute(plugin.process(queryResponse));

        assertThat(resourceStatusAttribute.getValue(), is(true));
    }

    @Test
    public void testMetacardDisplaysNonCachedProduct() throws Exception {
        setupNonCachedMock(getBasicMetacard());

        MetacardResourceStatus plugin = new MetacardResourceStatus(cache);
        QueryResponse queryResponse = plugin.process(this.queryResponse);

        Attribute resourceStatusAttribute = getReourceStatusAttribute(plugin.process(queryResponse));

        assertThat(resourceStatusAttribute.getValue(), is(false));
    }

    private MetacardImpl getBasicMetacard() {
        MetacardImpl metacard = new MetacardImpl();
        metacard.setId("abc123");
        metacard.setSourceId("ddf-1");
        metacard.setResourceSize("N/A");

        return metacard;
    }

    private void setupCachedMock(MetacardImpl metacard) {
        when(cachedResource.getSize()).thenReturn(999L);
        when(cachedResource.hasProduct()).thenReturn(true);
        when(cache.getValid(anyString(), anyObject())).thenReturn(cachedResource);

        setupSingleResultResponseMock(metacard);
    }

    private void setupNonCachedMock(MetacardImpl metacard) {

        when(cachedResource.getSize()).thenReturn(0L);
        when(cachedResource.hasProduct()).thenReturn(false);
        when(cache.getValid(anyString(), anyObject())).thenReturn(null);

        setupSingleResultResponseMock(metacard);
    }

    private void setupSingleResultResponseMock(MetacardImpl metacard) {
        Result result = new ResultImpl(metacard);
        List<Result> results = new ArrayList<>();
        results.add(result);

        when(queryResponse.getResults()).thenReturn(results);
    }

    private Attribute getReourceStatusAttribute(QueryResponse queryResponse) {
        Metacard resultMetacard = queryResponse.getResults()
                .get(0)
                .getMetacard();
        return resultMetacard.getAttribute(Metacard.RESOURCE_CACHE_STATUS);
    }
}
