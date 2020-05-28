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
package org.codice.ddf.ui.searchui.simple.properties;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import org.codice.ddf.branding.BrandingRegistry;
import org.codice.ddf.configuration.DictionaryMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

@RunWith(MockitoJUnitRunner.class)
public class UiConfigurationPropertiesFactoryTest {

  @Mock private ConfigurationAdmin configurationAdmin;
  @Mock private BrandingRegistry branding;

  @Test
  public void testConfigurationDoesNotExist() throws IOException, InvalidSyntaxException {
    when(configurationAdmin.listConfigurations("(service.pid=ddf.platform.ui.config)"))
        .thenReturn(null);
    UiConfigurationPropertiesFactory.getInstance().setConfigurationAdmin(configurationAdmin);
    UiConfigurationProperties props =
        UiConfigurationPropertiesFactory.getInstance().getProperties();
    assertAllBlankProps(props);
  }

  @Test
  public void testExceptionIsHandled() throws IOException, InvalidSyntaxException {
    when(configurationAdmin.listConfigurations("(service.pid=ddf.platform.ui.config)"))
        .thenThrow(new IOException());
    UiConfigurationPropertiesFactory.getInstance().setConfigurationAdmin(configurationAdmin);
    UiConfigurationProperties props =
        UiConfigurationPropertiesFactory.getInstance().getProperties();
    assertAllBlankProps(props);
  }

  @Test
  public void testInvalidTypeProperties() throws IOException, InvalidSyntaxException {
    DictionaryMap<String, Object> configProps = new DictionaryMap<>();
    configProps.put("header", false);
    configProps.put("footer", false);
    configProps.put("color", false);
    configProps.put("background", false);
    configProps.put("systemUsageTitle", false);
    configProps.put("systemUsageMessage", false);
    configProps.put("systemUsageEnabled", "Unexpected text");
    configProps.put("systemUsageOncePerSession", "Unexpected text");

    Configuration configuration = mock(Configuration.class);
    Configuration[] configurations = {configuration};
    when(configurationAdmin.listConfigurations("(service.pid=ddf.platform.ui.config)"))
        .thenReturn(configurations);
    when(configurationAdmin.getConfiguration("ddf.platform.ui.config", null))
        .thenReturn(configuration);
    when(configuration.getProperties()).thenReturn(configProps);

    UiConfigurationPropertiesFactory.getInstance().setConfigurationAdmin(configurationAdmin);
    UiConfigurationProperties props =
        UiConfigurationPropertiesFactory.getInstance().getProperties();

    assertAllBlankProps(props);
  }

  @Test
  public void testProductNameExists() throws IOException, InvalidSyntaxException {
    DictionaryMap<String, Object> configProps = new DictionaryMap<>();

    Configuration configuration = mock(Configuration.class);
    Configuration[] configurations = {configuration};
    when(configurationAdmin.listConfigurations("(service.pid=ddf.platform.ui.config)"))
        .thenReturn(configurations);
    when(configurationAdmin.getConfiguration("ddf.platform.ui.config", null))
        .thenReturn(configuration);
    when(configuration.getProperties()).thenReturn(configProps);

    when(branding.getProductName()).thenReturn("Product Name");

    UiConfigurationPropertiesFactory.getInstance().setConfigurationAdmin(configurationAdmin);
    UiConfigurationPropertiesFactory.getInstance().setBranding(branding);
    UiConfigurationProperties props =
        UiConfigurationPropertiesFactory.getInstance().getProperties();

    assertThat(props.getProductName(), is("Product Name"));
  }

  @Test
  public void testProductNameDoesNotExist() throws IOException, InvalidSyntaxException {
    DictionaryMap<String, Object> configProps = new DictionaryMap<>();

    Configuration configuration = mock(Configuration.class);
    Configuration[] configurations = {configuration};
    when(configurationAdmin.listConfigurations("(service.pid=ddf.platform.ui.config)"))
        .thenReturn(configurations);
    when(configurationAdmin.getConfiguration("ddf.platform.ui.config", null))
        .thenReturn(configuration);
    when(configuration.getProperties()).thenReturn(configProps);

    UiConfigurationPropertiesFactory.getInstance().setConfigurationAdmin(configurationAdmin);
    UiConfigurationProperties props =
        UiConfigurationPropertiesFactory.getInstance().getProperties();

    assertThat(props.getProductName(), is(""));
  }

  @Test
  public void testPropsCreatedProperly() throws IOException, InvalidSyntaxException {
    DictionaryMap<String, Object> configProps = new DictionaryMap<>();
    configProps.put("header", "Test Header");
    configProps.put("footer", "Test Footer");
    configProps.put("color", "WHITE");
    configProps.put("background", "RED");
    configProps.put("systemUsageTitle", "Test Title");
    configProps.put("systemUsageMessage", "Test Message");
    configProps.put("systemUsageEnabled", true);
    configProps.put("systemUsageOncePerSession", false);

    Configuration configuration = mock(Configuration.class);
    Configuration[] configurations = {configuration};
    when(configurationAdmin.listConfigurations("(service.pid=ddf.platform.ui.config)"))
        .thenReturn(configurations);
    when(configurationAdmin.getConfiguration("ddf.platform.ui.config", null))
        .thenReturn(configuration);
    when(configuration.getProperties()).thenReturn(configProps);

    UiConfigurationPropertiesFactory.getInstance().setConfigurationAdmin(configurationAdmin);
    UiConfigurationProperties props =
        UiConfigurationPropertiesFactory.getInstance().getProperties();

    assertThat(props.getHeader(), is("Test Header"));
    assertThat(props.getFooter(), is("Test Footer"));
    assertThat(props.getColor(), is("WHITE"));
    assertThat(props.getBackground(), is("RED"));
    assertThat(props.getSystemUsageTitle(), is("Test Title"));
    assertThat(props.getSystemUsageMessage(), is("Test Message"));
    assertThat(props.getSystemUsageEnabled(), is(true));
    assertThat(props.getSystemUsageOncePerSession(), is(false));
  }

  private void assertAllBlankProps(UiConfigurationProperties props) {
    assertThat(props.getProductName(), is(""));
    assertThat(props.getHeader(), is(""));
    assertThat(props.getFooter(), is(""));
    assertThat(props.getColor(), is(""));
    assertThat(props.getBackground(), is(""));
    assertThat(props.getSystemUsageTitle(), is(""));
    assertThat(props.getSystemUsageMessage(), is(""));
    assertThat(props.getSystemUsageEnabled(), is(false));
    assertThat(props.getSystemUsageOncePerSession(), is(false));
  }
}
