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

import java.io.IOException;
import java.util.Base64;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.branding.BrandingPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DdfBrandingPlugin implements BrandingPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(DdfBrandingPlugin.class);

    private String brandingPropertiesFilePath;

    private String productName;

    private String productURL;

    private String productImage;

    private String vendorName;

    private String vendorURL;

    private String vendorImage;

    private String favIcon;

    public DdfBrandingPlugin(String brandingPropertiesFilePath) {
        this.brandingPropertiesFilePath = brandingPropertiesFilePath;
    }

    public void init() {
        try {
            PropertiesConfiguration propertiesConfiguration =
                    new PropertiesConfiguration(getClass().getResource(brandingPropertiesFilePath));
            productName = propertiesConfiguration.getString("branding.product.name", "DDF");
            productURL = propertiesConfiguration.getString("branding.product.url",
                    "http://codice.org/ddf");
            productImage = propertiesConfiguration.getString("branding.product.image",
                    "/ddf/ddf.jpg");
            vendorName = propertiesConfiguration.getString("branding.vendor.name", "Codice");
            vendorURL = propertiesConfiguration.getString("branding.vendor.url",
                    "http://codice.org");
            vendorImage = propertiesConfiguration.getString("branding.vendor.image",
                    "/ddf/logo.png");
            favIcon = propertiesConfiguration.getString("branding.favicon", "/ddf/favicon.png");
        } catch (ConfigurationException e) {
            LOGGER.error("Unable to read properties file {}",
                    brandingPropertiesFilePath,
                    e.getMessage());
            productName = "DDF";
            productURL = "http://codice.org/ddf";
            productImage = "/ddf/ddf.jpg";
            vendorName = "Codice";
            vendorURL = "http://codice.org";
            vendorImage = "/ddf/logo.png";
            favIcon = "/ddf/favicon.png";
        }
    }

    @Override
    public String getProductName() {
        return productName;
    }

    @Override
    public String getProductURL() {
        return productURL;
    }

    @Override
    public String getProductImage() {
        return productImage;
    }

    @Override
    public String getBase64ProductImage() throws IOException {
        byte[] productImageAsBytes =
                IOUtils.toByteArray(DdfBrandingPlugin.class.getResourceAsStream(getProductImage()));
        if (productImageAsBytes.length > 0) {
            return Base64.getEncoder()
                    .encodeToString(productImageAsBytes);
        }
        return "";
    }

    @Override
    public String getVendorName() {
        return vendorName;
    }

    @Override
    public String getVendorURL() {
        return vendorURL;
    }

    @Override
    public String getVendorImage() {
        return vendorImage;
    }

    @Override
    public String getBase64VendorImage() throws IOException {
        byte[] vendorImageAsBytes = IOUtils.toByteArray(DdfBrandingPlugin.class.getResourceAsStream(
                getVendorImage()));
        if (vendorImageAsBytes.length > 0) {
            return Base64.getEncoder()
                    .encodeToString(vendorImageAsBytes);
        }
        return "";
    }

    @Override
    public String getFavIcon() {
        return favIcon;
    }

    @Override
    public String getBase64FavIcon() throws IOException {
        byte[] favIconAsBytes = IOUtils.toByteArray(DdfBrandingPlugin.class.getResourceAsStream(
                getFavIcon()));
        if (favIconAsBytes.length > 0) {
            return Base64.getEncoder()
                    .encodeToString(favIconAsBytes);
        }
        return "";
    }

}
