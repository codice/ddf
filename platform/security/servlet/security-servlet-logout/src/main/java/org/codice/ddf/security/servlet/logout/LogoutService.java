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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.cxf.ws.security.tokenstore.SecurityToken;

import ddf.action.Action;
import ddf.action.ActionProvider;
import ddf.security.SecurityConstants;
import ddf.security.assertion.impl.SecurityAssertionImpl;
import ddf.security.common.util.SecurityTokenHolder;
import ddf.security.encryption.EncryptionService;
import ddf.security.http.SessionFactory;

@Path("/")
public class LogoutService {

    private List<ActionProvider> logoutActionProviders;

    private SessionFactory httpSessionFactory;

    private EncryptionService encryptionService;

    @GET
    @Path("/actions")
    public Response getActionProviders(@Context HttpServletRequest request) {

        //TODO: Update docs for idp realm changes
        //TODO: Point admin logout to logout page
        // ((SecurityTokenHolder) session.getAttribute(SecurityConstants.SAML_ASSERTION)) -> get realm to token list -> SecurityAssertionImpl -> getPrincipal -> SubjectUtils.getName
        //pass securityToken to SecurityAssertionImpl -> getPrincipcal instead ->
        HttpSession session = httpSessionFactory.getOrCreateSession(request);
        Map<String, SecurityToken> realmTokenMap =
                ((SecurityTokenHolder) session.getAttribute(SecurityConstants.SAML_ASSERTION)).getRealmTokenMap();
        Map<String, String> realmToAuth = new HashMap<>();

        //create maps for realm -> auths
        for (String realm : realmTokenMap.keySet()) {
            realmToAuth.put(realm,
                    new SecurityAssertionImpl(realmTokenMap.get(realm)).getPrincipal()
                            .getName());
        }

        List<Map<String, String>> realmToPropMaps = new ArrayList<>();

        for (ActionProvider actionProvider : logoutActionProviders) {
            Action action = actionProvider.getAction(request);
            String realm = action.getId()
                    .substring(action.getId()
                            .lastIndexOf(".") + 1);

            if (realmToAuth.get(realm) != null && !realmToAuth.get(realm)
                    .contains("Anonymous")) {
                Map<String, String> actionProperties = new HashMap<String, String>();
                actionProperties.put("title", action.getTitle());
                actionProperties.put("realm", realm);
                actionProperties.put("auth", realmToAuth.get(realm));
                actionProperties.put("description", action.getDescription());
                actionProperties.put("url",
                        action.getUrl()
                                .toString());
                realmToPropMaps.add(actionProperties);
            }
        }

        return Response.ok(new ByteArrayInputStream(toJson(realmToPropMaps).getBytes()))
                .build();

    }

    public void setHttpSessionFactory(SessionFactory httpSessionFactory) {
        this.httpSessionFactory = httpSessionFactory;
    }

    public void setEncryptionService(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    public void setLogoutActionProviders(List<ActionProvider> logoutActionProviders) {
        this.logoutActionProviders = logoutActionProviders;
    }

}
