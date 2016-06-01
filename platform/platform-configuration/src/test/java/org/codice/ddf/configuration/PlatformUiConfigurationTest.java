/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Base64;

import org.codice.ddf.branding.BrandingPlugin;
import org.junit.Test;

import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

public class PlatformUiConfigurationTest {

    @Test
    public void testConfig() throws IOException {
        PlatformUiConfiguration configuration = new PlatformUiConfiguration();
        String wsOutput = configuration.getConfig();
        Object obj = JSONValue.parse(wsOutput);  // throws JSON Parse exception if not valid json.
        if (!(obj instanceof JSONObject)) {
            fail("PlatformUiConfiguration is not a JSON Object.");
        }

        BrandingPlugin brandingPlugin = mock(BrandingPlugin.class);
        when(brandingPlugin.getProductName()).thenReturn("product");
        when(brandingPlugin.getBase64ProductImage()).thenReturn(Base64.getEncoder()
                .encodeToString("image".getBytes()));
        when(brandingPlugin.getBase64FavIcon()).thenReturn(Base64.getEncoder()
                .encodeToString("fav".getBytes()));

        configuration.setProvider();

        configuration.setBranding(brandingPlugin);

        configuration.setHeader("header");
        configuration.setFooter("footer");
        configuration.setBackground("background");
        configuration.setColor("color");

        wsOutput = configuration.getConfig();
        obj = JSONValue.parse(wsOutput);  // throws JSON Parse exception if not valid json.
        if (!(obj instanceof JSONObject)) {
            fail("PlatformUiConfiguration is not a JSON Object.");
        }
        JSONObject jsonObject = (JSONObject) obj;
        assertEquals("header", jsonObject.get(PlatformUiConfiguration.HEADER));
        assertEquals("footer", jsonObject.get(PlatformUiConfiguration.FOOTER));
        assertEquals("background", jsonObject.get(PlatformUiConfiguration.BACKGROUND));
        assertEquals("color", jsonObject.get(PlatformUiConfiguration.COLOR));
        assertEquals("product", jsonObject.get(PlatformUiConfiguration.VERSION));
        assertEquals("image",
                new String(Base64.getMimeDecoder()
                        .decode((String) jsonObject.get(PlatformUiConfiguration.PRODUCT_IMAGE))));
        assertEquals("fav",
                new String(Base64.getMimeDecoder()
                        .decode((String) jsonObject.get(PlatformUiConfiguration.FAV_ICON))));
    }

}
