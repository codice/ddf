/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.catalog.ui.query.monitor.impl;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.codice.ddf.catalog.ui.metacard.workspace.QueryMetacardImpl;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardImpl;
import org.codice.ddf.catalog.ui.query.monitor.api.FilterService;
import org.codice.ddf.catalog.ui.query.monitor.api.QueryUpdateSubscriber;
import org.codice.ddf.catalog.ui.query.monitor.api.SecurityService;
import org.codice.ddf.catalog.ui.query.monitor.api.WorkspaceService;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.opengis.filter.And;
import org.opengis.filter.Filter;
import org.opengis.filter.Or;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.security.Subject;

public class TestWorkspaceQueryService {

    @SuppressWarnings("unchecked")
    @Test
    public void testRun()
            throws SchedulerException, UnsupportedQueryException, SourceUnavailableException,
            FederationException {

        String workspaceId = "3";

        QueryUpdateSubscriber queryUpdateSubscriber = mock(QueryUpdateSubscriber.class);
        WorkspaceService workspaceService = mock(WorkspaceService.class);
        CatalogFramework catalogFramework = mock(CatalogFramework.class);
        FilterBuilder filterBuilder = mock(FilterBuilder.class);
        Supplier<Optional<Scheduler>> schedulerSupplier = Optional::empty;
        Supplier<Trigger> triggerSupplier = () -> null;
        SecurityService securityService = new SecurityService() {
            @Override
            public Subject getSystemSubject() {
                return mock(Subject.class);
            }

            @Override
            public Map<String, Serializable> addSystemSubject(
                    Map<String, Serializable> properties) {
                return properties;
            }
        };
        FilterService filterService = mock(FilterService.class);
        when(filterService.getModifiedDateFilter(any())).thenReturn(mock(Filter.class));

        when(filterBuilder.anyOf(Mockito.any(Filter.class))).thenReturn(mock(Or.class));
        when(filterBuilder.allOf(Mockito.<Filter>anyVararg())).thenReturn(mock(And.class));

        WorkspaceQueryService workspaceQueryService = new WorkspaceQueryService(
                queryUpdateSubscriber,
                workspaceService,
                catalogFramework,
                filterBuilder,
                schedulerSupplier,
                securityService,
                filterService);

        String ecql = "area( Polygon((10 10, 20 10, 20 20, 10 10)) ) BETWEEN 10000 AND 30000";

        WorkspaceMetacardImpl workspaceMetacard = mock(WorkspaceMetacardImpl.class);
        when(workspaceMetacard.getId()).thenReturn(workspaceId);

        QueryMetacardImpl queryMetacardWithSource = mock(QueryMetacardImpl.class);
        when(queryMetacardWithSource.getSources()).thenReturn(Collections.singletonList("SomeSource"));
        when(queryMetacardWithSource.getCql()).thenReturn(ecql);

        Attribute id1 = mock(Attribute.class);
        when(id1.getValue()).thenReturn("1");
        when(queryMetacardWithSource.getAttribute(Metacard.ID)).thenReturn(id1);

        QueryMetacardImpl queryMetacardWithoutSource = mock(QueryMetacardImpl.class);
        when(queryMetacardWithoutSource.getSources()).thenReturn(Collections.emptyList());
        when(queryMetacardWithoutSource.getCql()).thenReturn(ecql);

        Attribute id2 = mock(Attribute.class);
        when(id2.getValue()).thenReturn("2");
        when(queryMetacardWithoutSource.getAttribute(Metacard.ID)).thenReturn(id2);

        Map<String, Pair<WorkspaceMetacardImpl, List<QueryMetacardImpl>>> queryMetacards =
                Collections.singletonMap(id2.getValue()
                                .toString(),
                        new ImmutablePair<>(workspaceMetacard,
                                Arrays.asList(queryMetacardWithSource,
                                        queryMetacardWithoutSource)));

        when(workspaceService.getQueryMetacards()).thenReturn(queryMetacards);

        long hitCount1 = 10;
        long hitCount2 = 20;

        QueryResponse queryResponse = mock(QueryResponse.class);
        when(queryResponse.getHits()).thenReturn(hitCount1)
                .thenReturn(hitCount2);

        when(catalogFramework.query(any())).thenReturn(queryResponse);

        workspaceQueryService.run();

        ArgumentCaptor<Map> argumentCaptor = ArgumentCaptor.forClass(Map.class);
        verify(queryUpdateSubscriber).notify(argumentCaptor.capture());

        Map queryUpdateSubscriberArgumentRaw = argumentCaptor.getValue();

        Map<String, Pair<WorkspaceMetacardImpl, Long>> queryUpdateSubscriberArgument =
                (Map<String, Pair<WorkspaceMetacardImpl, Long>>) queryUpdateSubscriberArgumentRaw;

        assertThat(queryUpdateSubscriberArgument.get(workspaceId)
                .getRight(), is(hitCount1 + hitCount2));
    }

}
