/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package com.lmco.ddf.endpoints.rest.action;

import java.util.HashMap;
import java.util.Map;

import ddf.catalog.util.DdfConfigurationManager;

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

    protected AbstractMetacardActionProvider configureActionProvider(AbstractMetacardActionProvider actionProvider) {
    
    	actionProvider.ddfConfigurationUpdated(getDefaultSettings());
    
    	return actionProvider;
    }

    protected AbstractMetacardActionProvider configureSecureActionProvider() {
        AbstractMetacardActionProvider actionProvider = new ViewMetacardActionProvider(ACTION_PROVIDER_ID);
    
        actionProvider.ddfConfigurationUpdated(getDefaultSecureSettings());
    
        return actionProvider;
    }

    protected Map getDefaultSettings() {
    
    	return createMap(SAMPLE_PROTOCOL, SAMPLE_IP, SAMPLE_PORT, SAMPLE_SERVICES_ROOT, SAMPLE_SOURCE_NAME);
    }

    private Map getDefaultSecureSettings() {
    
        return createMap(SAMPLE_SECURE_PROTOCOL, SAMPLE_IP, SAMPLE_SECURE_PORT, SAMPLE_SERVICES_ROOT, SAMPLE_SOURCE_NAME);
    }

    protected Map createMap(String protocol, String ip, String port,
            String contextRoot, String sourceName) {
            
            	Map<String, String> settings = new HashMap<String, String>();
            
                settings.put(DdfConfigurationManager.PROTOCOL, protocol);
            	settings.put(DdfConfigurationManager.HOST, ip);
            	settings.put(DdfConfigurationManager.PORT, port);
            	settings.put(DdfConfigurationManager.SERVICES_CONTEXT_ROOT, contextRoot);
            	settings.put(DdfConfigurationManager.SITE_NAME, sourceName);
            
            	return settings;
            }

}
