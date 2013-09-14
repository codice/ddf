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
import org.apache.log4j.Logger;

import ddf.action.Action;
import ddf.action.ActionProvider;
import ddf.catalog.data.Metacard;
import ddf.catalog.util.DdfConfigurationManager;
import ddf.catalog.util.DdfConfigurationWatcher;

public abstract class AbstractMetacardActionProvider implements ActionProvider,
        DdfConfigurationWatcher {

    protected String protocol;

    protected String host;

    protected String port;

    protected String contextRoot;

    protected String actionProviderId;

    protected String currentSourceName;

    static final Logger LOGGER = Logger.getLogger(AbstractMetacardActionProvider.class);

    static final String UNKNOWN_TARGET = "0.0.0.0";

    static final String PATH = "/catalog/sources";

    protected abstract Action getAction(String metacardId, String metacardSource);

    @Override
    public void ddfConfigurationUpdated(Map configuration) {

        if (configuration != null) {
            Object protocolMapValue = configuration.get(DdfConfigurationManager.PROTOCOL);
            Object hostMapValue = configuration.get(DdfConfigurationManager.HOST);
            Object portMapValue = configuration.get(DdfConfigurationManager.PORT);
            Object serviceContextMapValue = configuration
                    .get(DdfConfigurationManager.SERVICES_CONTEXT_ROOT);
            Object sourceNameValue = configuration.get(DdfConfigurationManager.SITE_NAME);

            if (hostMapValue != null) {
                this.host = hostMapValue.toString();
            }

            if (portMapValue != null) {
                this.port = portMapValue.toString();
            }

            if (serviceContextMapValue != null) {
                this.contextRoot = serviceContextMapValue.toString();
            }

            if (protocolMapValue != null) {
                this.protocol = protocolMapValue.toString();
            }

            if (sourceNameValue != null) {
                this.currentSourceName = sourceNameValue.toString();
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
                LOGGER.info(e);
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
