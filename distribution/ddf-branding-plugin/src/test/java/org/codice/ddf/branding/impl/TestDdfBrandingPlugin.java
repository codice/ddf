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
 **/

package org.codice.ddf.branding.impl;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Base64;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

public class TestDdfBrandingPlugin {

    private DdfBrandingPlugin ddfBrandingPlugin;

    private static String propertiesFilePath = "/META-INF/branding.properties";

    private static String invalidPropertiesFile = "/path/to/file/invalid.properties";

    private static String productName = "DDF";

    private static String productURL = "http://codice.org/ddf";

    private static String productImage = "/ddf/ddf.jpg";

    private static String vendorName = "Codice";

    private static String vendorURL = "http://codice.org";

    private static String vendorImage = "/ddf/logo.png";

    private static String favIcon = "/ddf/favicon.png";

    @Before
    public void setupDdfBrandingPlugin() {
        ddfBrandingPlugin = new DdfBrandingPlugin(propertiesFilePath);
    }

    @Test
    public void testInit() {
        ddfBrandingPlugin.init();
        assertThat(ddfBrandingPlugin.getProductName(),
                is(equalTo(productName + " " + System.getProperty("projectVersion"))));
        assertThat(ddfBrandingPlugin.getProductURL(), is(equalTo(productURL)));
        assertThat(ddfBrandingPlugin.getProductImage(), is(equalTo(productImage)));
        assertThat(ddfBrandingPlugin.getVendorName(), is(equalTo(vendorName)));
        assertThat(ddfBrandingPlugin.getVendorURL(), is(equalTo(vendorURL)));
        assertThat(ddfBrandingPlugin.getVendorImage(), is(equalTo(vendorImage)));
        assertThat(ddfBrandingPlugin.getFavIcon(), is(equalTo(favIcon)));
    }

    @Test
    public void testInitException() {
        DdfBrandingPlugin ddfBrandingPlugin = new DdfBrandingPlugin(invalidPropertiesFile);
        ddfBrandingPlugin.init();
        assertThat(ddfBrandingPlugin.getProductName(), is(equalTo(productName)));
        assertThat(ddfBrandingPlugin.getProductURL(), is(equalTo(productURL)));
        assertThat(ddfBrandingPlugin.getProductImage(), is(equalTo(productImage)));
        assertThat(ddfBrandingPlugin.getVendorName(), is(equalTo(vendorName)));
        assertThat(ddfBrandingPlugin.getVendorURL(), is(equalTo(vendorURL)));
        assertThat(ddfBrandingPlugin.getVendorImage(), is(equalTo(vendorImage)));
        assertThat(ddfBrandingPlugin.getFavIcon(), is(equalTo(favIcon)));
    }

    @Test
    public void testProductImage() throws IOException {
        ddfBrandingPlugin.init();
        assertThat(ddfBrandingPlugin.getBase64ProductImage(),
                is(equalTo(Base64.getEncoder()
                        .encodeToString(IOUtils.toByteArray(TestDdfBrandingPlugin.class.getResourceAsStream(
                                productImage))))));
    }

    @Test
    public void testVendorImage() throws IOException {
        ddfBrandingPlugin.init();
        assertThat(ddfBrandingPlugin.getBase64VendorImage(),
                is(equalTo(Base64.getEncoder()
                        .encodeToString(IOUtils.toByteArray(TestDdfBrandingPlugin.class.getResourceAsStream(
                                vendorImage))))));
    }

    @Test
    public void testFavIcon() throws IOException {
        ddfBrandingPlugin.init();
        assertThat(ddfBrandingPlugin.getBase64FavIcon(),
                is(equalTo(Base64.getEncoder()
                        .encodeToString(IOUtils.toByteArray(TestDdfBrandingPlugin.class.getResourceAsStream(
                                favIcon))))));
    }
}
