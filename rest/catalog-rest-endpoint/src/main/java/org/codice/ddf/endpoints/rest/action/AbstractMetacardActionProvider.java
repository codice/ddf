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
package org.codice.ddf.endpoints.rest.action;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.configuration.ConfigurationManager;
import org.codice.ddf.configuration.ConfigurationWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.action.Action;
import ddf.action.ActionProvider;
import ddf.catalog.data.Metacard;

public abstract class AbstractMetacardActionProvider implements ActionProvider,
        ConfigurationWatcher {

    protected String protocol;

    protected String host;

    protected String port;

    protected String contextRoot;

    protected String actionProviderId;

    protected String currentSourceName;

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMetacardActionProvider.class);

    static final String UNKNOWN_TARGET = "0.0.0.0";

    static final String PATH = "/catalog/sources";

    protected abstract Action getAction(String metacardId, String metacardSource);

    @Override
    public void configurationUpdateCallback(Map<String, String> configuration) {

        if (configuration != null) {
            String protocolMapValue = configuration.get(ConfigurationManager.PROTOCOL);
            String hostMapValue = configuration.get(ConfigurationManager.HOST);
            String portMapValue = configuration.get(ConfigurationManager.PORT);
            String serviceContextMapValue = configuration
                    .get(ConfigurationManager.SERVICES_CONTEXT_ROOT);
            String sourceNameValue = configuration.get(ConfigurationManager.SITE_NAME);

            if (StringUtils.isNotBlank(configuration.get(ConfigurationManager.PROTOCOL))) {
                this.host = hostMapValue;
            }

            if (StringUtils.isNotBlank(portMapValue)) {
                this.port = portMapValue;
            }

            if (StringUtils.isNotBlank(serviceContextMapValue)) {
                this.contextRoot = serviceContextMapValue;
            }

            if (StringUtils.isNotBlank(protocolMapValue)) {
                this.protocol = protocolMapValue;
            }

            if (StringUtils.isNotBlank(sourceNameValue)) {
                this.currentSourceName = sourceNameValue;
            }
        }
    }

    @Override
    public <T> Action getAction(T input) {

        if (input == null) {

            LOGGER.info("In order to receive url to Metacard, Metacard must not be null.");
            return null;
        }

        if (Metacard.class.isAssignableFrom(input.getClass())) {

            Metacard metacard = (Metacard) input;

            if (StringUtils.isBlank(metacard.getId())) {
                LOGGER.info("No id given. No action to provide.");
                return null;
            }

            if (isHostUnset(this.host)) {
                LOGGER.info("Host name/ip not set. Cannot create link for metacard.");
                return null;
            }

            String metacardId = null;
            String metacardSource = null;

            try {
                metacardId = URLEncoder.encode(metacard.getId(), CharEncoding.UTF_8);
                metacardSource = URLEncoder.encode(getSource(metacard), CharEncoding.UTF_8);
            } catch (UnsupportedEncodingException e) {
                LOGGER.info("Unsupported Encoding exception", e);
                return null;
            }

            return getAction(metacardId, metacardSource);

        }

        return null;
    }

    protected boolean isHostUnset(String host) {

        return (host == null || host.trim().equals(UNKNOWN_TARGET));
    }

    protected String getSource(Metacard metacard) {

        if (StringUtils.isNotBlank(metacard.getSourceId())) {
            return metacard.getSourceId();
        }

        return this.currentSourceName;
    }

    @Override
    public String getId() {
        return this.actionProviderId;
    }

}
