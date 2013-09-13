/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package com.lmco.ddf.webconsole.branding;

import java.io.IOException;
import java.util.Properties;

import org.apache.felix.webconsole.BrandingPlugin;

import org.apache.commons.io.IOUtils;

public class DDFBrandingPlugin implements BrandingPlugin {
    private static final String BRANDING_PROPERTIES = "/META-INF/webconsole.properties";

    private String brandName;

    private String productName;

    private String productURL;

    private String productImage;

    private String vendorName;

    private String vendorURL;

    private String vendorImage;

    private String favIcon;

    private String mainStyleSheet;

    public DDFBrandingPlugin() {

    }

    public void init() {
        Properties props;
        props = new Properties();
        java.io.InputStream ins = getClass().getResourceAsStream(BRANDING_PROPERTIES);
        if (ins != null) {
            try {
                props.load(ins);
            } catch (IOException ignore) {
                IOUtils.closeQuietly(ins);
            } finally {
                IOUtils.closeQuietly(ins);
            }
        }

        IOUtils.closeQuietly(ins);
        brandName = props.getProperty("webconsole.brand.name", "DDF Web Console");
        productName = props.getProperty("webconsole.product.name", "DDF");
        productURL = props.getProperty("webconsole.product.url", "http://felix.apache.org");
        productImage = props.getProperty("webconsole.product.image", "/res/ddf/ddf.jpg");
        vendorName = props.getProperty("webconsole.vendor.name", "Lockheed Martin");
        vendorURL = props.getProperty("webconsole.vendor.url", "http://www.lockheedmartin.com");
        vendorImage = props.getProperty("webconsole.vendor.image", "/res/ddf/logo.png");
        favIcon = props.getProperty("webconsole.favicon", "/res/ddf/favicon.ico");
        mainStyleSheet = props.getProperty("webconsole.stylesheet", "/res/ddf/webconsole.css");
    }

    public void destroy() {
        // don't need to do anything?
    }

    @Override
    public String getBrandName() {
        return brandName;
    }

    @Override
    public String getFavIcon() {
        return favIcon;
    }

    @Override
    public String getMainStyleSheet() {
        return mainStyleSheet;
    }

    @Override
    public String getProductImage() {
        return productImage;
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
    public String getVendorImage() {
        return vendorImage;
    }

    @Override
    public String getVendorName() {
        return vendorName;
    }

    @Override
    public String getVendorURL() {
        return vendorURL;
    }

}
