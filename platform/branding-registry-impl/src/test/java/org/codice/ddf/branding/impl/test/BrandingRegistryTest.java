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
package org.codice.ddf.branding.impl.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import org.codice.ddf.branding.BrandingPlugin;
import org.codice.ddf.branding.BrandingRegistry;
import org.codice.ddf.branding.impl.BrandingRegistryImpl;
import org.junit.Test;

public class BrandingRegistryTest {
  @Test
  public void testEmptyGetProductName() {
    assertThat(getEmptyBrandingRegistry().getProductName(), is("DDF"));
  }

  @Test
  public void testEmptyGetVersion() {
    assertThat(getEmptyBrandingRegistry().getProductVersion(), is(""));
  }

  @Test
  public void testGetProductName() {
    assertThat(getBrandingRegistry().getProductName(), is("DDF1"));
  }

  @Test
  public void testGetProductVersion() {
    assertThat(getBrandingRegistry().getProductVersion(), is("v1.0.0"));
  }

  @Test
  public void testGetAttributeFromBranding() throws IOException {
    assertThat(
        getBrandingRegistry().getAttributeFromBranding(BrandingPlugin::getProductName),
        is("DDF1 v1.0.0"));
  }

  @Test
  public void testGetProductNameMultiplePlugins() {
    assertThat(
        getBrandingRegistryMultiplePlugins("DDF1 v1.0.0", "DDF2 v2.0.0").getProductName(),
        is("DDF1"));
  }

  @Test
  public void testGetProductNameMultiplePluginsFirstNull() {
    assertThat(
        getBrandingRegistryMultiplePlugins(null, "DDF2 v2.0.0").getProductName(), is("DDF2"));
  }

  @Test
  public void testGetProductVersionMultiplePlugins() {
    assertThat(
        getBrandingRegistryMultiplePlugins("DDF1 v1.0.0", "DDF2 v2.0.0").getProductVersion(),
        is("v1.0.0"));
  }

  @Test
  public void testGetProductVersionMultiplePluginsFirstNull() {
    assertThat(
        getBrandingRegistryMultiplePlugins(null, "DDF2 v2.0.0").getProductVersion(), is("v2.0.0"));
  }

  @Test
  public void testGetAttributeFromBrandingMultiplePlugins() throws IOException {
    assertThat(
        getBrandingRegistryMultiplePlugins("DDF1 v1.0.0", "DDF2 v2.0.0")
            .getAttributeFromBranding(BrandingPlugin::getProductName),
        is("DDF1 v1.0.0"));
  }

  @Test
  public void testGetAttributeFromBrandingMultiplePluginsFirstNull() throws IOException {
    assertThat(
        getBrandingRegistryMultiplePlugins(null, "DDF2 v2.0.0")
            .getAttributeFromBranding(BrandingPlugin::getProductName),
        is("DDF2 v2.0.0"));
  }

  private BrandingRegistry getEmptyBrandingRegistry() {
    return new BrandingRegistryImpl();
  }

  private BrandingRegistry getBrandingRegistry() {
    BrandingPlugin plugin = mock(BrandingPlugin.class);
    when(plugin.getProductName()).thenReturn("DDF1 v1.0.0");
    BrandingRegistry registry = new BrandingRegistryImpl();
    registry.setBrandingPlugins(Collections.singletonList(plugin));
    return registry;
  }

  private BrandingRegistry getBrandingRegistryMultiplePlugins(
      String firstReturnValue, String secondReturnValue) {
    BrandingPlugin plugin = mock(BrandingPlugin.class);
    when(plugin.getProductName()).thenReturn(firstReturnValue);
    BrandingRegistry registry = new BrandingRegistryImpl();
    BrandingPlugin plugin2 = mock(BrandingPlugin.class);
    when(plugin2.getProductName()).thenReturn(secondReturnValue);
    registry.setBrandingPlugins(Arrays.asList(plugin, plugin2));
    return registry;
  }
}
