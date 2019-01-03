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
package org.codice.felix.cm.file;

import static java.lang.String.format;
import static org.codice.felix.cm.file.ConfigurationContextImpl.FELIX_FILENAME;
import static org.codice.felix.cm.file.ConfigurationContextImpl.FELIX_NEW_CONFIG;
import static org.codice.felix.cm.file.ConfigurationContextImpl.PROPERTY_REVISION;
import static org.codice.felix.cm.file.ConfigurationContextImpl.SERVICE_FACTORY_PIDLIST;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.osgi.framework.Constants.SERVICE_PID;
import static org.osgi.service.cm.ConfigurationAdmin.SERVICE_FACTORYPID;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.UUID;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.service.cm.Configuration;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationContextImplTest {
  private static final String TEST_PID = "org.codice.test.ServiceFactory";

  private static final String TEST_PID_2 = "org.codice.test.separate.Service";

  private static final String TEST_MSF_PID =
      format("%s.%s", TEST_PID, UUID.randomUUID().toString());

  private static final String MALFORMED_URL = "htp:/www.google.com";

  private static final String URL_BUT_NOT_URI = "http:// ";

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Mock private Configuration mockConfig;

  private Dictionary<String, Object> testProps;

  private File temporaryFile;

  private ConfigurationContextImpl context;

  @Before
  public void before() throws Exception {
    testProps = new Hashtable<>();
    testProps.put("prop1", "value1");
    testProps.put("prop2", "value2");

    temporaryFile = temporaryFolder.newFile();
    when(mockConfig.getPid()).thenReturn(TEST_PID);
    when(mockConfig.getProperties()).thenReturn(testProps);
  }

  @Test(expected = NullPointerException.class)
  public void testCreateContextWhenConfigNull() {
    context = new ConfigurationContextImpl(null);
  }

  @Test(expected = NullPointerException.class)
  public void testCreateContextWhenPropMapNull() {
    when(mockConfig.getProperties()).thenReturn(null);
    context = new ConfigurationContextImpl(mockConfig);
  }

  @Test
  public void testVisibleForProcessing() {
    context = new ConfigurationContextImpl(TEST_PID, testProps);
    assertThat(context.shouldBeVisibleToPlugins(), is(true));
  }

  @Test
  public void testNotVisibleNullServicePid() {
    context = new ConfigurationContextImpl(null, testProps);
    assertThat(
        "Configurations with null pids, if any, should not be processed",
        context.shouldBeVisibleToPlugins(),
        is(false));
  }

  @Test
  public void testNotVisibleConfigIsNew() {
    testProps.put(FELIX_NEW_CONFIG, true);
    context = new ConfigurationContextImpl(TEST_PID, testProps);
    assertThat(
        "Configurations with the new config flag should not be processed",
        context.shouldBeVisibleToPlugins(),
        is(false));
  }

  @Test
  public void testNotVisiblePidlist() {
    testProps.put(SERVICE_FACTORY_PIDLIST, new ArrayList<>());
    context = new ConfigurationContextImpl(TEST_PID, testProps);
    assertThat(
        "Configurations that store linking data for factories should not be processed",
        context.shouldBeVisibleToPlugins(),
        is(false));
  }

  @Test
  public void testNotVisibleNoProperties() {
    context = new ConfigurationContextImpl(TEST_PID, new Hashtable<>());
    assertThat(
        "Configurations without any property fields should not be processed",
        context.shouldBeVisibleToPlugins(),
        is(false));
  }

  @Test
  public void testSetNormalProperty() {
    Dictionary<String, Object> innerProps = new Hashtable<>();
    context = new ConfigurationContextImpl(TEST_PID, innerProps);
    context.setProperty("key", "value");

    assertThat(
        "Inner properties should get updated with the new mapping so it gets persisted",
        innerProps.size(),
        is(1));
    assertThat(
        "Inner properties should have the mapping so Felix can persist it",
        innerProps.get("key"),
        is("value"));

    Dictionary<String, Object> sanitizedProps = context.getSanitizedProperties();
    assertThat(
        "Sanitized properties should get updated with the new mapping so it's visible",
        sanitizedProps.size(),
        is(1));
    assertThat(
        "Sanitized properties should have the mapping key=value so it's visible",
        sanitizedProps.get("key"),
        is("value"));
  }

  @Test
  public void testSetInternalProperty() {
    Dictionary<String, Object> innerProps = new Hashtable<>();
    context = new ConfigurationContextImpl(TEST_PID, innerProps);
    context.setProperty(FELIX_FILENAME, temporaryFile.getAbsolutePath());

    assertThat(
        "Inner properties should get updated with the new mapping so it gets persisted",
        innerProps.size(),
        is(1));
    assertThat(
        "Inner properties should have the mapping so Felix can persist it",
        innerProps.get(FELIX_FILENAME),
        is(temporaryFile.getAbsolutePath()));

    Dictionary<String, Object> sanitizedProps = context.getSanitizedProperties();
    assertThat(
        "Sanitized properties should ignore the new mapping because it's internal",
        sanitizedProps.size(),
        is(0));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetPropertyBadParams() {
    context = new ConfigurationContextImpl(TEST_PID, new Hashtable<>());
    context.setProperty(null, null);
  }

  @Test
  public void testEquals() {
    ConfigurationContextImpl context1 = new ConfigurationContextImpl(TEST_PID, new Hashtable<>());
    ConfigurationContextImpl context2 = new ConfigurationContextImpl(TEST_PID, new Hashtable<>());
    assertThat(context1.equals(context2), is(true));
  }

  @Test
  public void testNotEquals() {
    ConfigurationContextImpl context1 = new ConfigurationContextImpl(TEST_PID, new Hashtable<>());
    ConfigurationContextImpl context2 = new ConfigurationContextImpl(TEST_PID_2, new Hashtable<>());
    assertThat(context1.equals(context2), is(false));
  }

  @Test
  public void testNullEquals() {
    ConfigurationContextImpl context1 = new ConfigurationContextImpl(null, new Hashtable<>());
    ConfigurationContextImpl context2 = new ConfigurationContextImpl(null, new Hashtable<>());
    assertThat(context1.equals(context2), is(true));
  }

  @Test
  public void testPidIsSetCorrectly() {
    context = new ConfigurationContextImpl(TEST_PID, new Hashtable<>());
    assertThat(
        "PIDs for singleton services should be equal to the passed PID",
        context.getServicePid(),
        is(TEST_PID));
  }

  @Test
  public void testGetFactoryPid() {
    context = new ConfigurationContextImpl(TEST_PID, new Hashtable<>());
    assertThat(
        "Factory PIDs for singleton services should be null, since they are the same",
        context.getFactoryPid(),
        is(nullValue()));
  }

  @Test
  public void testGetFactoryPidSingletonService() {
    context = new ConfigurationContextImpl(TEST_MSF_PID, new Hashtable<>());
    assertThat(
        "Factory PIDs for factory services should be regular PIDs without a UUID",
        context.getFactoryPid(),
        is(TEST_PID));
  }

  @Test
  public void testGetSanitizedData() {
    testProps.put(SERVICE_PID, TEST_PID);
    testProps.put(SERVICE_FACTORYPID, TEST_PID);
    testProps.put(FELIX_FILENAME, temporaryFile.toURI());
    testProps.put(FELIX_NEW_CONFIG, true);
    testProps.put(SERVICE_FACTORY_PIDLIST, new ArrayList<>());
    testProps.put(PROPERTY_REVISION, TEST_PID);

    context = new ConfigurationContextImpl(TEST_PID, testProps);

    assertThat(context.getServicePid(), is(TEST_PID));
    assertThat(context.getSanitizedProperties().size(), is(2));
  }

  @Test
  public void testGetConfigWhenFelixPropNotFound() {
    context = new ConfigurationContextImpl(mockConfig);
    assertThat(context.getConfigFile(), is(nullValue()));
  }

  @Test
  public void testFelixFileFromURL() throws Exception {
    testProps.put(FELIX_FILENAME, temporaryFile.toURI().toURL());
    context = new ConfigurationContextImpl(mockConfig);
    assertThat(context.getConfigFile(), is(notNullValue()));
    assertThat(context.getConfigFile(), is(temporaryFile));
  }

  @Test
  public void testFelixFileFromURI() throws Exception {
    testProps.put(FELIX_FILENAME, temporaryFile.toURI());
    context = new ConfigurationContextImpl(mockConfig);
    assertThat(context.getConfigFile(), is(notNullValue()));
    assertThat(context.getConfigFile(), is(temporaryFile));
  }

  @Test
  public void testFelixFileFromString() throws Exception {
    testProps.put(FELIX_FILENAME, temporaryFile.toURI().toString());
    context = new ConfigurationContextImpl(mockConfig);
    assertThat(context.getConfigFile(), is(notNullValue()));
    assertThat(context.getConfigFile(), is(temporaryFile));
  }

  @Test
  public void testFelixFileFromURLwithBadURISyntax() throws Exception {
    testProps.put(FELIX_FILENAME, new URL(URL_BUT_NOT_URI));
    context = new ConfigurationContextImpl(mockConfig);
    assertThat(context.getConfigFile(), is(nullValue()));
  }

  @Test
  public void testFelixFileFromMalformedURLstring() throws Exception {
    testProps.put(FELIX_FILENAME, MALFORMED_URL);
    context = new ConfigurationContextImpl(mockConfig);
    assertThat(context.getConfigFile(), is(nullValue()));
  }

  @Test
  public void testFelixFileFromStringWithBadURISyntax() throws Exception {
    testProps.put(FELIX_FILENAME, URL_BUT_NOT_URI);
    context = new ConfigurationContextImpl(mockConfig);
    assertThat(context.getConfigFile(), is(nullValue()));
  }

  @Test
  public void testFelixFileFromUnexpectedType() throws Exception {
    testProps.put(FELIX_FILENAME, new Object());
    context = new ConfigurationContextImpl(mockConfig);
    assertThat(context.getConfigFile(), is(nullValue()));
  }
}
