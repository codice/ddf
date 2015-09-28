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
package org.codice.ddf.endpoints.rest.action;

public class AbstractActionProviderTest {

    protected static final String SAMPLE_PATH = "/catalog/sources/";

    protected static final String SAMPLE_SERVICES_ROOT = "/services";

    protected static final String SAMPLE_PROTOCOL = "http://";

    protected static final String SAMPLE_SECURE_PROTOCOL = "https://";

    protected static final String SAMPLE_PORT = "8181";

    protected static final String SAMPLE_SECURE_PORT = "8993";

    protected static final String SAMPLE_IP = "192.168.1.1";

    protected static final String SAMPLE_ID = "abcdef1234567890abdcef1234567890";

    protected static final String SAMPLE_SOURCE_NAME = "sampleSource";

    protected static final String ACTION_PROVIDER_ID = "catalog.view.metacard";

    protected void configureActionProvider() {
        configureActionProvider(SAMPLE_PROTOCOL, SAMPLE_IP, SAMPLE_PORT, SAMPLE_SERVICES_ROOT,
                SAMPLE_SOURCE_NAME);
    }

    protected void configureSecureActionProvider() {

        configureActionProvider(SAMPLE_SECURE_PROTOCOL, SAMPLE_IP, SAMPLE_SECURE_PORT,
                SAMPLE_SERVICES_ROOT, SAMPLE_SOURCE_NAME);
    }

    protected void configureActionProvider(String protocol, String host, String port,
            String contextRoot, String siteName) {

        setProperty("org.codice.ddf.system.hostname", host);
        setProperty("org.codice.ddf.system.httpPort", port);
        setProperty("org.codice.ddf.system.httpsPort", port);
        setProperty("org.codice.ddf.system.protocol", protocol);
        setProperty("org.codice.ddf.system.rootContext", contextRoot);
        setProperty("org.codice.ddf.system.siteName", siteName);
    }

    private void setProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
