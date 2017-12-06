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
import java.util.Optional;
import org.codice.ddf.branding.BrandingRegistry;
import org.codice.ddf.configuration.DictionaryMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

@RunWith(MockitoJUnitRunner.class)
public class TestUiConfigurationPropertiesFactory {

  @Mock private ConfigurationAdmin configurationAdmin;

  private Optional<BrandingRegistry> branding = Optional.empty();

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
  public void testPropsCreatedProperly() throws IOException, InvalidSyntaxException {
    DictionaryMap<String, Object> configProps = new DictionaryMap<>();
    configProps.put("header", "Test Header");
    configProps.put("footer", "Test Footer");
    configProps.put("systemUsageEnabled", true);

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
    assertThat(props.getSystemUsageEnabled(), is(true));
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
