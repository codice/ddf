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
package org.codice.ddf.catalog.ui.query.monitor.impl;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.security.Subject;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Supplier;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.ExecutionException;
import org.apache.shiro.subject.PrincipalCollection;
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
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;

public class WorkspaceQueryServiceTest {

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
    Scheduler scheduler = mock(Scheduler.class);
    when(scheduler.getContext()).thenReturn(mock(SchedulerContext.class));
    Supplier<Optional<Scheduler>> schedulerSupplier = () -> Optional.of(scheduler);
    SecurityService securityService =
        new SecurityService() {
          @Override
          public Subject getSystemSubject() {
            return mock(Subject.class);
          }

          @Override
          public Map<String, Serializable> addSystemSubject(Map<String, Serializable> properties) {
            return properties;
          }
        };
    FilterService filterService = mock(FilterService.class);
    when(filterService.getModifiedDateFilter(any())).thenReturn(mock(Filter.class));

    when(filterBuilder.anyOf(Mockito.any(Filter.class))).thenReturn(mock(Or.class));
    when(filterBuilder.allOf(Mockito.<Filter>anyVararg())).thenReturn(mock(And.class));

    WorkspaceQueryServiceImpl workspaceQueryServiceImpl =
        new WorkspaceQueryServiceImpl(
            queryUpdateSubscriber,
            workspaceService,
            catalogFramework,
            filterBuilder,
            schedulerSupplier,
            securityService,
            filterService);

    workspaceQueryServiceImpl.setQueryTimeInterval(60);
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
        Collections.singletonMap(
            id2.getValue().toString(),
            new ImmutablePair<>(
                workspaceMetacard,
                Arrays.asList(queryMetacardWithSource, queryMetacardWithoutSource)));

    when(workspaceService.getQueryMetacards()).thenReturn(queryMetacards);

    long hitCount1 = 10;
    long hitCount2 = 20;

    QueryResponse queryResponse = mock(QueryResponse.class);
    when(queryResponse.getHits()).thenReturn(hitCount1).thenReturn(hitCount2);

    when(catalogFramework.query(any())).thenReturn(queryResponse);

    workspaceQueryServiceImpl.setSubject(
        new Subject() {
          @Override
          public String getName() {
            return "";
          }

          @Override
          public boolean isGuest() {
            return false;
          }

          @Override
          public Object getPrincipal() {
            return null;
          }

          @Override
          public PrincipalCollection getPrincipals() {
            return null;
          }

          @Override
          public boolean isPermitted(String s) {
            return false;
          }

          @Override
          public boolean isPermitted(Permission permission) {
            return false;
          }

          @Override
          public boolean[] isPermitted(String... strings) {
            return new boolean[0];
          }

          @Override
          public boolean[] isPermitted(List<Permission> list) {
            return new boolean[0];
          }

          @Override
          public boolean isPermittedAll(String... strings) {
            return false;
          }

          @Override
          public boolean isPermittedAll(Collection<Permission> collection) {
            return false;
          }

          @Override
          public void checkPermission(String s) throws AuthorizationException {}

          @Override
          public void checkPermission(Permission permission) throws AuthorizationException {}

          @Override
          public void checkPermissions(String... strings) throws AuthorizationException {}

          @Override
          public void checkPermissions(Collection<Permission> collection)
              throws AuthorizationException {}

          @Override
          public boolean hasRole(String s) {
            return false;
          }

          @Override
          public boolean[] hasRoles(List<String> list) {
            return new boolean[0];
          }

          @Override
          public boolean hasAllRoles(Collection<String> collection) {
            return false;
          }

          @Override
          public void checkRole(String s) throws AuthorizationException {}

          @Override
          public void checkRoles(Collection<String> collection) throws AuthorizationException {}

          @Override
          public void checkRoles(String... strings) throws AuthorizationException {}

          @Override
          public void login(AuthenticationToken authenticationToken)
              throws AuthenticationException {}

          @Override
          public boolean isAuthenticated() {
            return false;
          }

          @Override
          public boolean isRemembered() {
            return false;
          }

          @Override
          public Session getSession() {
            return null;
          }

          @Override
          public Session getSession(boolean b) {
            return null;
          }

          @Override
          public void logout() {}

          @Override
          public <V> V execute(Callable<V> callable) throws ExecutionException {
            try {
              return callable.call();
            } catch (Exception e) {
              throw new ExecutionException(e);
            }
          }

          @Override
          public void execute(Runnable runnable) {}

          @Override
          public <V> Callable<V> associateWith(Callable<V> callable) {
            return null;
          }

          @Override
          public Runnable associateWith(Runnable runnable) {
            return null;
          }

          @Override
          public void runAs(PrincipalCollection principalCollection)
              throws NullPointerException, IllegalStateException {}

          @Override
          public boolean isRunAs() {
            return false;
          }

          @Override
          public PrincipalCollection getPreviousPrincipals() {
            return null;
          }

          @Override
          public PrincipalCollection releaseRunAs() {
            return null;
          }
        });

    workspaceQueryServiceImpl.setCronString("0 0 0 * * ?");
    workspaceQueryServiceImpl.setQueryTimeoutMinutes(5L);
    workspaceQueryServiceImpl.run();

    ArgumentCaptor<Map> argumentCaptor = ArgumentCaptor.forClass(Map.class);
    verify(queryUpdateSubscriber).notify(argumentCaptor.capture());

    Map queryUpdateSubscriberArgumentRaw = argumentCaptor.getValue();

    Map<String, Pair<WorkspaceMetacardImpl, Long>> queryUpdateSubscriberArgument =
        (Map<String, Pair<WorkspaceMetacardImpl, Long>>) queryUpdateSubscriberArgumentRaw;

    assertThat(
        queryUpdateSubscriberArgument.get(workspaceId).getRight(), is(hitCount1 + hitCount2));
  }
}
