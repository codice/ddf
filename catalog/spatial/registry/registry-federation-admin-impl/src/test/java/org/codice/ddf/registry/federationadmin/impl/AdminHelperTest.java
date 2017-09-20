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
package org.codice.ddf.registry.federationadmin.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.service.cm.Configuration;

@RunWith(MockitoJUnitRunner.class)
public class AdminHelperTest {

  @Mock org.codice.ddf.admin.core.api.ConfigurationAdmin configAdmin;

  @Mock org.osgi.service.cm.ConfigurationAdmin felixConfigAdmin;

  private AdminHelper helper;

  @Before
  public void setup() throws Exception {
    helper = new AdminHelper(configAdmin, felixConfigAdmin);
  }

  @Test
  public void testFilterProperties() throws Exception {
    String[] filters = new String[] {"regid"};
    Configuration config = mock(Configuration.class);
    Dictionary<String, Object> props = new Hashtable<>();
    props.put("whiteList", true);
    props.put("registryDisabled", true);
    props.put("registryEntryIds", filters);

    when(config.getProperties()).thenReturn(props);
    when(felixConfigAdmin.getConfiguration(anyString(), anyString())).thenReturn(config);
    Map<String, Object> properties = helper.getFilterProperties();
    assertThat(properties.size(), equalTo(3));
    assertThat(properties.get(FederationAdmin.FILTER_INVERTED), equalTo(true));
    assertThat(properties.get(FederationAdmin.CLIENT_MODE), equalTo(true));
    assertThat(properties.get(FederationAdmin.SUMMARY_FILTERED), equalTo(filters));
  }

  @Test
  public void testFilterPropertiesNullProperties() throws Exception {
    Configuration config = mock(Configuration.class);
    when(config.getProperties()).thenReturn(null);
    when(felixConfigAdmin.getConfiguration(anyString(), anyString())).thenReturn(config);
    Map<String, Object> properties = helper.getFilterProperties();
    assertThat(properties.get(FederationAdmin.FILTER_INVERTED), equalTo(false));
    assertThat(properties.get(FederationAdmin.CLIENT_MODE), equalTo(false));
    assertThat(properties.get(FederationAdmin.SUMMARY_FILTERED), equalTo(new String[0]));
  }

  @Test
  public void testSetFilterProperty() throws Exception {
    ArgumentCaptor<Dictionary> captor = ArgumentCaptor.forClass(Dictionary.class);
    Configuration config = mock(Configuration.class);
    when(config.getProperties()).thenReturn(null);
    when(felixConfigAdmin.getConfiguration(anyString(), anyString())).thenReturn(config);
    helper.setFilterInverted(true);
    verify(config).update(captor.capture());
    assertThat(captor.getValue().get("whiteList"), equalTo(true));
  }
}
