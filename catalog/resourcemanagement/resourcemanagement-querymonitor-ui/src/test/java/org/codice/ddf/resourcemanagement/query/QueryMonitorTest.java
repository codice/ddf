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
package org.codice.ddf.resourcemanagement.query;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import ddf.catalog.source.Source;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.codice.ddf.resourcemanagement.query.plugin.ActiveSearch;
import org.codice.ddf.resourcemanagement.query.plugin.QueryMonitorPlugin;
import org.codice.ddf.resourcemanagement.query.service.QueryMonitor;
import org.junit.Before;
import org.junit.Test;

public class QueryMonitorTest {

  private QueryMonitor queryMonitor;

  private QueryMonitorPlugin queryMonitorPlugin;

  private Map<UUID, ActiveSearch> activeSearchMap;

  private static final String UUID_1 = "38400000-8cf0-11bd-b23e-10b96e4ef00d";

  private static final String UUID_2 = "38400000-8cf0-11bd-b23e-10b96e4ef00e";

  private static final String SOURCE = "Test Source";

  private static final String CQL = "anyText ILIKE 'bob'";

  private static final String USER_1 = "Juan";

  private static final String USER_2 = "Holmes";

  @Before
  public void setUp() {
    queryMonitorPlugin = mock(QueryMonitorPlugin.class);
    activeSearchMap = generateActiveSearchMap();

    doReturn(activeSearchMap).when(queryMonitorPlugin).getActiveSearches();

    doAnswer(
            invocationOnMock -> {
              UUID uuid = (UUID) invocationOnMock.getArguments()[0];
              return (activeSearchMap.remove(uuid) != null);
            })
        .when(queryMonitorPlugin)
        .removeActiveSearch(any(UUID.class));

    queryMonitor = new QueryMonitor(queryMonitorPlugin);
  }

  @Test
  public void testActiveSearchesEmptyAndNullMap() {
    Map<UUID, ActiveSearch> activeSearchMap = new HashMap<>();
    doReturn(activeSearchMap).when(queryMonitorPlugin).getActiveSearches();
    List<Map<String, String>> list = queryMonitor.activeSearches();
    assertThat(list, hasSize(0));

    doReturn(null).when(queryMonitorPlugin).getActiveSearches();
    list = queryMonitor.activeSearches();
    assertThat(list, hasSize(0));
  }

  @Test
  public void testActiveSearches() {
    List<Map<String, String>> list = queryMonitor.activeSearches();
    assertThat(list, hasSize(2));

    Map<String, String> userMap = list.get(0);
    assertThat(userMap.get(QueryMonitor.USER), is(USER_1));
    assertThat(userMap.get(QueryMonitor.SOURCE_ID), is(SOURCE));
    assertThat(userMap.get(QueryMonitor.QUERY), is(CQL));
    assertThat(userMap.get(QueryMonitor.UUID_PROPERTY), is(UUID_1));

    userMap = list.get(1);
    assertThat(userMap.get(QueryMonitor.USER), is(USER_2));
    assertThat(userMap.get(QueryMonitor.SOURCE_ID), is(SOURCE));
    assertThat(userMap.get(QueryMonitor.QUERY), is(CQL));
    assertThat(userMap.get(QueryMonitor.UUID_PROPERTY), is(UUID_2));
  }

  @Test
  public void testCancelActiveSearch() {
    queryMonitor.cancelActiveSearch(UUID_1);
    List<Map<String, String>> list = queryMonitor.activeSearches();
    assertThat(list, hasSize(1));

    queryMonitor.cancelActiveSearch(UUID_2);
    list = queryMonitor.activeSearches();
    assertThat(list, hasSize(0));
  }

  private Map<UUID, ActiveSearch> generateActiveSearchMap() {
    Map<UUID, ActiveSearch> activeSearchMap = new HashMap<>();
    activeSearchMap.put(UUID.fromString(UUID_1), generateActiveSearch(USER_1, UUID_1));
    activeSearchMap.put(UUID.fromString(UUID_2), generateActiveSearch(USER_2, UUID_2));
    return activeSearchMap;
  }

  private ActiveSearch generateActiveSearch(String name, String uuid) {
    Source source = mock(Source.class);
    doReturn(SOURCE).when(source).getId();
    Date date = new Date();
    ActiveSearch activeSearch = mock(ActiveSearch.class);
    doReturn(source).when(activeSearch).getSource();
    doReturn(name).when(activeSearch).getClientInfo();
    doReturn(CQL).when(activeSearch).getCQL();
    doReturn(date).when(activeSearch).getStartTime();
    doReturn(UUID.fromString(uuid)).when(activeSearch).getUniqueID();
    return activeSearch;
  }
}
