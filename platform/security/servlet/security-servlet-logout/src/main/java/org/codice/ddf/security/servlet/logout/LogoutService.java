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
package org.codice.ddf.security.servlet.logout;

import static org.boon.Boon.toJson;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.action.Action;
import ddf.action.ActionProvider;
import ddf.security.SecurityConstants;
import ddf.security.common.util.SecurityTokenHolder;
import ddf.security.http.impl.HttpSessionFactory;

@Path("/")
public class LogoutService {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogoutService.class);

    private List<ActionProvider> logoutActionProviders;

    private HttpSessionFactory httpSessionFactory;

    public void setLogoutActionProviders(List<ActionProvider> logoutActionProviders) {
        this.logoutActionProviders = logoutActionProviders;
    }

    @GET
    @Path("/actions")
    public Response getActionProviders() {

        //TODO Filter from greatest to least using ranks
        // ((SecurityTokenHolder) session.getAttribute(SecurityConstants.SAML_ASSERTION)) -> get realm to token list -> SecurityAssertionImpl -> getPrincipal -> SubjectUtils.getName
        Response response;
        List<Map<String, String>> realmsAndActions = new ArrayList<>();
        Map<String, SecurityToken> realmTokenMap = ((SecurityTokenHolder) httpSessionFactory
                .getOrCreateSession(null).getAttribute(SecurityConstants.SAML_ASSERTION))
                .getRealmTokenMap();

        for (String realm : realmTokenMap.keySet()) {
            Map<String, String> realmNameMap = new HashMap<>();
            realmNameMap.put(realm, realmTokenMap.get(realm).getPrincipal().getName());
            realmsAndActions.add(realmNameMap);
        }

        for (ActionProvider actionProvider : logoutActionProviders) {
            Action action = actionProvider.getAction(null);
            Map<String, String> actionProperties = new HashMap<String, String>();
            actionProperties.put("title", action.getTitle());
            actionProperties.put("id", action.getId());
            actionProperties.put("description", action.getDescription());
            actionProperties.put("url", action.getUrl().toString());

            realmsAndActions.add(actionProperties);
        }

        String configString = toJson(realmsAndActions);
        response = Response.ok(new ByteArrayInputStream(configString.getBytes())).build();

        return response;
    }

    public void setHttpSessionFactory(HttpSessionFactory httpSessionFactory) {
        this.httpSessionFactory = httpSessionFactory;
    }
}
