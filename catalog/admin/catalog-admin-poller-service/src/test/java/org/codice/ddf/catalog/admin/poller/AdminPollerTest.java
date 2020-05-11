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
package org.codice.ddf.catalog.admin.poller;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import ddf.action.Action;
import ddf.action.MultiActionProvider;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.service.ConfiguredService;
import ddf.catalog.source.Source;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import org.apache.shiro.util.CollectionUtils;
import org.codice.ddf.admin.core.api.ConfigurationStatus;
import org.codice.ddf.admin.core.api.Metatype;
import org.codice.ddf.admin.core.api.Service;
import org.codice.ddf.admin.core.impl.ServiceImpl;
import org.codice.ddf.opensearch.source.OpenSearchSource;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class AdminPollerTest {

  public static final String CONFIG_PID = "properPid";

  public static final String EXCEPTION_PID = "throwsAnException";

  public static final String FPID = "OpenSearchSource";

  public static MockedAdminPoller poller;

  private List<Result> results;

  private Metacard metacard1;

  private static final String MAP_ENTRY_ACTION_ID = "id";

  private static final String MAP_ENTRY_ACTION_TITLE = "title";

  private static final String MAP_ENTRY_ACTION_DESCRIPTION = "description";

  private static final String MAP_ENTRY_ACTION_URL = "url";

  @Before
  public void setup() {

    metacard1 = new MetacardImpl();
    results = new ArrayList<>();
    results.add(new ResultImpl(metacard1));

    poller = new AdminPollerTest().new MockedAdminPoller(null, null);
    poller.setOperationActionProviders(new ArrayList<>());
    poller.setReportActionProviders(new ArrayList<>());
  }

  @Test
  public void testAllSourceInfo() {
    Action operatorActionOne =
        getTestAction(
            "operationIdOne",
            "operationTitle1",
            "operationDescription1",
            "https://localhost:8993/provider/someAction");
    Action operatorActionTwo =
        getTestAction(
            "operationIdTwo",
            "operationTitleTwo",
            "operationDescriptionTwo",
            "https://localhost:8993/provider/someAction");
    ImmutableList<MultiActionProvider> operationActions =
        ImmutableList.of(
            getHandleableTestActionProvider(operatorActionOne),
            getNotHandleableTestActionProvider(),
            getHandleableTestActionProvider(operatorActionTwo));
    poller.setOperationActionProviders(operationActions);

    Action reportActionOne =
        getTestAction(
            "reportId",
            "reportTitle",
            "reportDescription",
            "https://localhost:8993/provider/someAction");
    ImmutableList<MultiActionProvider> reportActions =
        ImmutableList.of(
            getHandleableTestActionProvider(reportActionOne),
            getNotHandleableTestActionProvider(),
            getNotHandleableTestActionProvider());
    poller.setReportActionProviders(reportActions);

    List<Service> sources = poller.allSourceInfo();
    assertThat(sources, notNullValue());
    assertThat(sources.size(), is(2));

    assertThat(sources.get(0), not(hasKey("configurations")));
    assertThat(sources.get(1), hasKey("configurations"));
    List<Map<String, Object>> configurations = (List) sources.get(1).get("configurations");

    assertThat(configurations, hasSize(2));
    Map<String, Object> configurationMap = configurations.get(0);
    assertThat(configurationMap, hasKey("operation_actions"));
    assertThat(configurationMap, hasKey("report_actions"));

    List<Map<String, Object>> operationActionsList =
        (List) configurationMap.get("operation_actions");
    Map<String, String> operatorActionOneMap = getMapFromAction(operatorActionOne);
    Map<String, String> operatorActionTwoMap = getMapFromAction(operatorActionTwo);
    assertThat(operationActionsList, contains(operatorActionOneMap, operatorActionTwoMap));

    List<Map<String, Object>> reportActionsList = (List) configurationMap.get("report_actions");
    Map<String, String> reportActionMap = getMapFromAction(reportActionOne);
    assertThat(reportActionsList, contains(reportActionMap));
  }

  @Test
  public void testSourceStatus() {
    assertThat(poller.sourceStatus(CONFIG_PID), is(true));
    assertThat(poller.sourceStatus(EXCEPTION_PID), is(false));
    assertThat(poller.sourceStatus("FAKE SOURCE"), is(false));
  }

  private class MockedAdminPoller extends AdminPollerServiceBean {
    public MockedAdminPoller(
        org.codice.ddf.admin.core.api.ConfigurationAdmin configurationAdmin,
        ConfigurationAdmin configAdmin) {
      super(configurationAdmin, configAdmin);
    }

    @Override
    protected AdminSourceHelper getHelper() {
      AdminSourceHelper helper = mock(AdminSourceHelper.class);
      try {
        // Mock out the configuration
        Configuration config = mock(Configuration.class);
        when(config.getPid()).thenReturn(CONFIG_PID);
        when(config.getFactoryPid()).thenReturn(FPID);
        Dictionary<String, Object> dict = new Hashtable<>();
        dict.put("service.pid", CONFIG_PID);
        dict.put("service.factoryPid", FPID);
        when(config.getProperties()).thenReturn(dict);
        Configuration config2 = mock(Configuration.class);
        when(config2.getPid()).thenReturn(CONFIG_PID + ConfigurationStatus.DISABLED_EXTENSION);
        when(config2.getFactoryPid()).thenReturn(FPID + ConfigurationStatus.DISABLED_EXTENSION);
        Dictionary<String, Object> dict2 = new Hashtable<>();
        dict2.put("service.pid", CONFIG_PID + ConfigurationStatus.DISABLED_EXTENSION);
        dict2.put("service.factoryPid", FPID + ConfigurationStatus.DISABLED_EXTENSION);
        when(config2.getProperties()).thenReturn(dict);
        when(helper.getConfigurations(any(Metatype.class)))
            .thenReturn(CollectionUtils.asList(config, config2), null);

        // Mock out the sources
        OpenSearchSource source = mock(OpenSearchSource.class);
        when(source.isAvailable()).thenReturn(true);

        OpenSearchSource badSource = mock(OpenSearchSource.class);
        when(badSource.isAvailable()).thenThrow(new RuntimeException());

        // CONFIG_PID, EXCEPTION_PID, FAKE_SOURCE
        when(helper.getConfiguration(any(ConfiguredService.class)))
            .thenReturn(config, config, config);
        when(helper.getSources()).thenReturn(CollectionUtils.asList((Source) source, badSource));

        // Mock out the metatypes
        Service metatype = new ServiceImpl();
        metatype.put("id", "OpenSearchSource");
        metatype.put("OSGI-INF/blueprint/metatype", new ArrayList<Map<String, Object>>());

        Service noConfigMetaType = new ServiceImpl();
        noConfigMetaType.put("id", "No Configurations");
        noConfigMetaType.put("OSGI-INF/blueprint/metatype", new ArrayList<Map<String, Object>>());

        when(helper.getMetatypes()).thenReturn(CollectionUtils.asList(metatype, noConfigMetaType));
      } catch (Exception e) {

      }

      return helper;
    }
  }

  private MultiActionProvider getHandleableTestActionProvider(Action action) {
    MultiActionProvider actionProvider = mock(MultiActionProvider.class);
    when(actionProvider.canHandle(any(Configuration.class))).thenReturn(true);
    when(actionProvider.getActions(any(Class.class))).thenReturn(CollectionUtils.asList(action));

    return actionProvider;
  }

  private Action getTestAction(String id, String title, String description, String urlString) {
    Action action = mock(Action.class);
    when(action.getId()).thenReturn(id);
    when(action.getTitle()).thenReturn(title);
    when(action.getDescription()).thenReturn(description);
    try {
      URL url = new URL(urlString);
      when(action.getUrl()).thenReturn(url);
    } catch (MalformedURLException e) {
      // do nothing
    }

    return action;
  }

  private MultiActionProvider getNotHandleableTestActionProvider() {
    MultiActionProvider actionProvider = mock(MultiActionProvider.class);

    when(actionProvider.canHandle(any(Configuration.class))).thenReturn(false);

    return actionProvider;
  }

  private Map<String, String> getMapFromAction(Action action) {
    Map<String, String> map = new HashMap<>();
    if (action == null) {
      return map;
    }

    map.put(MAP_ENTRY_ACTION_ID, action.getId());
    map.put(MAP_ENTRY_ACTION_TITLE, action.getTitle());
    map.put(MAP_ENTRY_ACTION_DESCRIPTION, action.getDescription());
    map.put(MAP_ENTRY_ACTION_URL, action.getUrl().toString());

    return map;
  }
}
