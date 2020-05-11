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
package org.codice.ddf.configuration;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Base64;
import java.util.Collections;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.codice.ddf.branding.BrandingPlugin;
import org.codice.ddf.branding.impl.BrandingRegistryImpl;
import org.junit.Before;
import org.junit.Test;

public class PlatformUiConfigurationTest {

  private static final int DEFAULT_TIMEOUT_MINUTES = 15;

  private PlatformUiConfiguration configuration;

  @Before
  public void setup() {
    configuration = new PlatformUiConfiguration();
  }

  @Test
  public void testConfig() throws IOException {
    int timeout = 1234;
    String wsOutput = configuration.getConfigAsJsonString();
    Object obj = JSONValue.parse(wsOutput); // throws JSON Parse exception if not valid json.
    if (!(obj instanceof JSONObject)) {
      fail("PlatformUiConfiguration is not a JSON Object.");
    }
    BrandingPlugin branding = mock(BrandingPlugin.class);
    when(branding.getBase64FavIcon())
        .thenReturn(Base64.getEncoder().encodeToString("fav".getBytes()));
    when(branding.getBase64ProductImage())
        .thenReturn(Base64.getEncoder().encodeToString("image".getBytes()));
    when(branding.getBase64VendorImage())
        .thenReturn(Base64.getEncoder().encodeToString("vendorimage".getBytes()));
    BrandingRegistryImpl brandingPlugin = mock(BrandingRegistryImpl.class);
    when(brandingPlugin.getProductName()).thenReturn("product");
    when(brandingPlugin.getBrandingPlugins()).thenReturn(Collections.singletonList(branding));
    when(brandingPlugin.getAttributeFromBranding(any())).thenCallRealMethod();

    configuration.setBranding(brandingPlugin);

    configuration.setHeader("header");
    configuration.setFooter("footer");
    configuration.setBackground("background");
    configuration.setColor("color");
    configuration.setTimeout(timeout);

    wsOutput = configuration.getConfigAsJsonString();
    obj = JSONValue.parse(wsOutput); // throws JSON Parse exception if not valid json.
    if (!(obj instanceof JSONObject)) {
      fail("PlatformUiConfiguration is not a JSON Object.");
    }
    JSONObject jsonObject = (JSONObject) obj;
    assertEquals("header", jsonObject.get(PlatformUiConfiguration.HEADER_CONFIG_KEY));
    assertEquals("footer", jsonObject.get(PlatformUiConfiguration.FOOTER_CONFIG_KEY));
    assertEquals("background", jsonObject.get(PlatformUiConfiguration.BACKGROUND_CONFIG_KEY));
    assertEquals("color", jsonObject.get(PlatformUiConfiguration.COLOR_CONFIG_KEY));
    assertEquals("product", jsonObject.get(PlatformUiConfiguration.TITLE_CONFIG_KEY));
    assertEquals(
        "image",
        new String(
            Base64.getMimeDecoder()
                .decode(
                    (String) jsonObject.get(PlatformUiConfiguration.PRODUCT_IMAGE_CONFIG_KEY))));
    assertEquals(
        "vendorimage",
        new String(
            Base64.getMimeDecoder()
                .decode((String) jsonObject.get(PlatformUiConfiguration.VENDOR_IMAGE_CONFIG_KEY))));
    assertEquals(
        "fav",
        new String(
            Base64.getMimeDecoder()
                .decode((String) jsonObject.get(PlatformUiConfiguration.FAV_ICON_CONFIG_KEY))));
    assertEquals(timeout, jsonObject.get(PlatformUiConfiguration.TIMEOUT_CONFIG_KEY));
  }

  @Test
  public void testOutOfBoundsTimeoutConfig() {
    int outOfBoundsTimeout = 1;
    configuration.setTimeout(outOfBoundsTimeout);
    assertThat(configuration.getTimeout(), is(DEFAULT_TIMEOUT_MINUTES));
  }

  @Test
  public void testLowerBoundsTimeoutConfig() {
    int lowerBoundTimeout = 2;
    configuration.setTimeout(lowerBoundTimeout);
    assertThat(configuration.getTimeout(), is(lowerBoundTimeout));
  }
}
