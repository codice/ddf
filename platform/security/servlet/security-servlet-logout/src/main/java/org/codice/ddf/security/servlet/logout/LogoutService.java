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
import java.util.function.Function;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.Subject.Builder;

import ddf.action.Action;
import ddf.action.ActionProvider;
import ddf.security.SecurityConstants;
import ddf.security.SubjectUtils;
import ddf.security.assertion.impl.SecurityAssertionImpl;
import ddf.security.common.util.SecurityTokenHolder;
import ddf.security.http.SessionFactory;

@Path("/")
public class LogoutService {

    private List<ActionProvider> logoutActionProviders;

    private SessionFactory httpSessionFactory;

    @GET
    @Path("/actions")
    public Response getActionProviders(@Context HttpServletRequest request) {

        //TODO: Update docs for idp realm changes. Also add documentation about the rollback of other poliy manager shizz
        //TODO: look for incorrect DDF version
        //TODO: Make sure iframe has a scrollbar

        HttpSession session = httpSessionFactory.getOrCreateSession(request);
        Map<String, SecurityToken> realmTokenMap = ((SecurityTokenHolder) session.getAttribute(SecurityConstants.SAML_ASSERTION)).getRealmTokenMap();
        Map<String, Subject> realmSubjectMap = new HashMap<>();

        for (String realm : realmTokenMap.keySet()) {
            SecurityAssertionImpl securityAssertion = new SecurityAssertionImpl(realmTokenMap.get(realm));
            Subject subject = new Builder().principals(new SimplePrincipalCollection(securityAssertion, realm)).buildSubject();
            realmSubjectMap.put(realm, subject);
        }

        List<Map<String, String>> realmToPropMaps = new ArrayList<>();
        Function<String, String> getNameToEncrypt = (realm) -> SubjectUtils.getName(realmSubjectMap.get(realm));

        for (ActionProvider actionProvider : logoutActionProviders) {
            Action action = actionProvider.getAction(getNameToEncrypt);
            String realm = action.getId().substring(action.getId().lastIndexOf(".") + 1);

            Map<String, String> actionProperties = new HashMap<String, String>();
            actionProperties.put("title", action.getTitle());
            actionProperties.put("realm", realm);
            actionProperties.put("auth", SubjectUtils.getName(realmSubjectMap.get(realm), "", true));
            actionProperties.put("description", action.getDescription());
            actionProperties.put("url", action.getUrl().toString());
            realmToPropMaps.add(actionProperties);
        }

        return Response.ok(new ByteArrayInputStream(toJson(realmToPropMaps).getBytes())).build();
    }

    public void setHttpSessionFactory(SessionFactory httpSessionFactory) {
        this.httpSessionFactory = httpSessionFactory;
    }

    public void setLogoutActionProviders(List<ActionProvider> logoutActionProviders) {
        this.logoutActionProviders = logoutActionProviders;
    }

}
