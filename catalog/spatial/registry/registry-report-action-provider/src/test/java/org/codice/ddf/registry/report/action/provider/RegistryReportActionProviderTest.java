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
package org.codice.ddf.registry.report.action.provider;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.action.Action;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.source.Source;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class RegistryReportActionProviderTest {

  private static final String SAMPLE_PATH = "/internal/registries/";

  private static final String SAMPLE_SERVICES_ROOT = "/services";

  private static final String SAMPLE_PROTOCOL = "http://";

  private static final String SAMPLE_SECURE_PROTOCOL = "https://";

  private static final String SAMPLE_PORT = "8181";

  private static final String SAMPLE_SECURE_PORT = "8993";

  private static final String SAMPLE_IP = "192.168.1.1";

  private static final String SAMPLE_ID = "abcdef1234567890abdcef1234567890";

  private static final String SAMPLE_REGISTRY_ID = "sample_reg_id";

  private static final String ACTION_PROVIDER_ID = "catalog.view.metacard";

  private static final Set<String> SAMPLE_REGISTRY_TAGS =
      new HashSet<>(Collections.singletonList(RegistryConstants.REGISTRY_TAG));

  private static final String PATH_AND_FORMAT = "/report.html";

  private static final String SAMPLE_SOURCE_ID = "ddf.distribution";

  private static final String SOURCE_ID_QUERY_PARAM = "?sourceId=";

  private RegistryReportActionProvider actionProvider;

  private ConfigurationAdmin configurationAdmin;

  private Configuration configuration;

  private MetacardImpl metacard;

  private Source source;

  @Before
  public void setup() {
    metacard = new MetacardImpl();
    source = mock(Source.class);
    configurationAdmin = mock(ConfigurationAdmin.class);
    configuration = mock(Configuration.class);
    actionProvider = new RegistryReportActionProvider(ACTION_PROVIDER_ID);
    metacard.setId(SAMPLE_ID);
    metacard.setTags(SAMPLE_REGISTRY_TAGS);
    metacard.setSourceId(SAMPLE_SOURCE_ID);
    metacard.setAttribute(RegistryObjectMetacardType.REGISTRY_ID, SAMPLE_REGISTRY_ID);
  }

  @Test
  public void testMetacardNull() {
    assertThat(new RegistryReportActionProvider(ACTION_PROVIDER_ID).getActions(null), hasSize(0));
  }

  @Test
  public void testUriSyntaxException() {
    configureActionProvider(SAMPLE_PROTOCOL, "23^&*#", SAMPLE_PORT, SAMPLE_SERVICES_ROOT);

    assertThat(
        "A bad url should have been caught and an empty list returned.",
        actionProvider.getActions(metacard),
        hasSize(0));
  }

  @Test
  public void testMetacardIdUrlEncodedSpace() {
    metacard.setAttribute(RegistryObjectMetacardType.REGISTRY_ID, "abd ef");

    configureActionProvider();

    String url = actionProvider.getActions(metacard).get(0).getUrl().toString();

    assertThat(url, is(expectedDefaultAddressWith("abd+ef")));
  }

  @Test
  public void testMetacardIdUrlEncodedAmpersand() {
    ArrayList<String> tags = new ArrayList<>();
    tags.add(RegistryConstants.REGISTRY_TAG);
    metacard.setAttribute(Metacard.TAGS, tags);
    metacard.setAttribute(RegistryObjectMetacardType.REGISTRY_ID, "abd&ef");

    configureActionProvider();

    String url = actionProvider.getActions(metacard).get(0).getUrl().toString();

    assertThat(url, is(expectedDefaultAddressWith("abd%26ef")));
  }

  @Test
  public void testRegistryIdNull() {
    metacard.setAttribute(RegistryObjectMetacardType.REGISTRY_ID, null);

    configureActionProvider();

    assertThat(
        "An action should not have been created when no id is provided.",
        actionProvider.getActions(metacard),
        hasSize(0));
  }

  @Test
  public void testIpNull() {
    configureActionProvider(SAMPLE_PROTOCOL, null, SAMPLE_PORT, SAMPLE_SERVICES_ROOT);

    assertThat(
        actionProvider.getActions(metacard).get(0).getUrl().toString(),
        containsString("localhost"));
  }

  @Test
  public void testPortNull() {
    configureActionProvider(SAMPLE_PROTOCOL, SAMPLE_IP, null, SAMPLE_SERVICES_ROOT);

    assertThat(
        actionProvider.getActions(metacard).get(0).getUrl().toString(), containsString("8181"));
  }

  @Test
  public void testContextRootNull() {
    configureActionProvider(SAMPLE_PROTOCOL, SAMPLE_IP, SAMPLE_PORT, null);

    assertThat(
        actionProvider.getActions(metacard).get(0).getUrl().toString(),
        not(containsString("/services")));
  }

  @Test
  public void testNonMetacard() {
    configureActionProvider();

    assertThat(
        "An action when metacard was not provided.",
        actionProvider.getActions(new Date()),
        hasSize(0));
  }

  @Test
  public void testMetacard() {
    configureActionProvider();

    Action action = actionProvider.getActions(metacard).get(0);

    assertEquals(actionProvider.getTitle(), action.getTitle());
    assertEquals(actionProvider.getDescription(), action.getDescription());
    assertThat(
        action.getUrl().toString(),
        is(
            expectedDefaultAddressWith(
                metacard
                    .getAttribute(RegistryObjectMetacardType.REGISTRY_ID)
                    .getValue()
                    .toString())));
    assertEquals(ACTION_PROVIDER_ID, actionProvider.getId());
  }

  @Test
  public void testCanHandleFederatedMetacard() {
    metacard.setTags(new HashSet<>());

    String newSourceName = "newSource";
    metacard.setSourceId(newSourceName);

    configureActionProvider();

    assertThat(actionProvider.canHandle(metacard), is(false));
  }

  @Test
  public void testSecureMetacard() {
    this.configureSecureActionProvider();

    Action action = actionProvider.getActions(metacard).get(0);

    assertEquals(actionProvider.getTitle(), action.getTitle());
    assertEquals(actionProvider.getDescription(), action.getDescription());
    assertEquals(
        SAMPLE_SECURE_PROTOCOL
            + SAMPLE_IP
            + ":"
            + SAMPLE_SECURE_PORT
            + SAMPLE_SERVICES_ROOT
            + SAMPLE_PATH
            + metacard.getAttribute(RegistryObjectMetacardType.REGISTRY_ID).getValue().toString()
            + PATH_AND_FORMAT
            + SOURCE_ID_QUERY_PARAM
            + SAMPLE_SOURCE_ID,
        action.getUrl().toString());
    assertEquals(ACTION_PROVIDER_ID, actionProvider.getId());
  }

  @Test
  public void testNullProtocol() {
    configureActionProvider(null, SAMPLE_IP, SAMPLE_SECURE_PORT, SAMPLE_SERVICES_ROOT);

    Action action = actionProvider.getActions(metacard).get(0);

    assertThat(action.getUrl().toString(), containsString("https"));
  }

  @Test
  public void testSourceIdPresent() {
    configureActionProvider();

    Action action = actionProvider.getActions(metacard).get(0);

    assertEquals(actionProvider.getTitle(), action.getTitle());
    assertEquals(actionProvider.getDescription(), action.getDescription());
    assertThat(
        action.getUrl().toString(),
        is(
            expectedDefaultAddressWith(
                metacard
                    .getAttribute(RegistryObjectMetacardType.REGISTRY_ID)
                    .getValue()
                    .toString())));
    assertEquals(ACTION_PROVIDER_ID, actionProvider.getId());
  }

  @Test
  public void testBlankSourceId() {
    metacard.setSourceId("");
    configureActionProvider();

    Action action = actionProvider.getActions(metacard).get(0);

    assertThat(
        action.getUrl().toString(),
        is(
            SAMPLE_PROTOCOL
                + SAMPLE_IP
                + ":"
                + SAMPLE_PORT
                + SAMPLE_SERVICES_ROOT
                + SAMPLE_PATH
                + metacard
                    .getAttribute(RegistryObjectMetacardType.REGISTRY_ID)
                    .getValue()
                    .toString()
                + PATH_AND_FORMAT));
  }

  @Test
  public void testNullSourceId() {
    metacard.setSourceId(null);
    configureActionProvider();

    Action action = actionProvider.getActions(metacard).get(0);

    assertThat(
        action.getUrl().toString(),
        is(
            SAMPLE_PROTOCOL
                + SAMPLE_IP
                + ":"
                + SAMPLE_PORT
                + SAMPLE_SERVICES_ROOT
                + SAMPLE_PATH
                + metacard
                    .getAttribute(RegistryObjectMetacardType.REGISTRY_ID)
                    .getValue()
                    .toString()
                + PATH_AND_FORMAT));
  }

  @Test
  public void testUnsupportedType() {
    configureActionProvider();

    List<Action> actions = actionProvider.getActions(SAMPLE_PATH);

    assertThat(actions.size(), is(0));
  }

  @Test
  public void testBlankSource() {
    configureActionProvider();

    List<Action> actions = actionProvider.getActions(source);

    assertThat(actions.size(), is(0));
  }

  @Test
  public void testEmptyConfigurationsSource() throws IOException, InvalidSyntaxException {
    configureActionProvider();
    actionProvider.setConfigurationAdmin(configurationAdmin);
    Configuration[] configurations = new Configuration[0];

    when(source.getId()).thenReturn(SAMPLE_SOURCE_ID);
    when(configurationAdmin.listConfigurations(String.format("(id=%s)", SAMPLE_SOURCE_ID)))
        .thenReturn(configurations);

    List<Action> actions = actionProvider.getActions(source);

    assertThat(actions.size(), is(0));
  }

  @Test
  public void testNullRegistryIdConfiguration() {
    configureActionProvider();

    when(configuration.getProperties()).thenReturn(new Hashtable<>());

    List<Action> actions = actionProvider.getActions(configuration);

    assertThat(actions.size(), is(0));
  }

  @Test
  public void testValidConfiguration() {
    configureActionProvider();
    Dictionary<String, Object> testDictionary = new Hashtable<>();

    testDictionary.put(RegistryConstants.CONFIGURATION_REGISTRY_ID_PROPERTY, SAMPLE_REGISTRY_ID);

    when(configuration.getProperties()).thenReturn(testDictionary);

    List<Action> actions = actionProvider.getActions(configuration);

    assertThat(actions.size(), is(1));
    assertThat(
        actions.get(0).getUrl().toString(),
        is(
            SAMPLE_PROTOCOL
                + SAMPLE_IP
                + ":"
                + SAMPLE_PORT
                + SAMPLE_SERVICES_ROOT
                + SAMPLE_PATH
                + metacard
                    .getAttribute(RegistryObjectMetacardType.REGISTRY_ID)
                    .getValue()
                    .toString()
                + PATH_AND_FORMAT));
  }

  @Test
  public void testConfigurationBadUri() {
    configureActionProvider(SAMPLE_PROTOCOL, "23^&*#", SAMPLE_PORT, SAMPLE_SERVICES_ROOT);
    Dictionary<String, Object> testDictionary = new Hashtable<>();

    testDictionary.put(RegistryConstants.CONFIGURATION_REGISTRY_ID_PROPERTY, SAMPLE_REGISTRY_ID);

    when(configuration.getProperties()).thenReturn(testDictionary);

    List<Action> actions = actionProvider.getActions(configuration);

    assertThat(actions.size(), is(0));
  }

  private String expectedDefaultAddressWith(String id) {
    return SAMPLE_PROTOCOL
        + SAMPLE_IP
        + ":"
        + SAMPLE_PORT
        + SAMPLE_SERVICES_ROOT
        + SAMPLE_PATH
        + id
        + PATH_AND_FORMAT
        + SOURCE_ID_QUERY_PARAM
        + SAMPLE_SOURCE_ID;
  }

  private void configureActionProvider() {
    configureActionProvider(SAMPLE_PROTOCOL, SAMPLE_IP, SAMPLE_PORT, SAMPLE_SERVICES_ROOT);
  }

  private void configureSecureActionProvider() {
    configureActionProvider(
        SAMPLE_SECURE_PROTOCOL, SAMPLE_IP, SAMPLE_SECURE_PORT, SAMPLE_SERVICES_ROOT);
  }

  private void configureActionProvider(
      String protocol, String host, String port, String contextRoot) {

    setProperty(SystemBaseUrl.EXTERNAL_HOST, host);
    setProperty(SystemBaseUrl.EXTERNAL_PORT, port);
    setProperty(SystemBaseUrl.EXTERNAL_HTTPS_PORT, port);
    setProperty(SystemBaseUrl.EXTERNAL_PROTOCOL, protocol);
    setProperty(SystemBaseUrl.ROOT_CONTEXT, contextRoot);
  }

  private void setProperty(String key, String value) {
    if (value == null) {
      System.clearProperty(key);
    } else {
      System.setProperty(key, value);
    }
  }
}
