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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import javax.servlet.http.HttpSession;

import org.apache.tika.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import ddf.action.Action;
import ddf.security.SecurityConstants;
import ddf.security.common.util.SecurityTokenHolder;
import ddf.security.http.impl.HttpSessionFactory;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

/**
 * Created by tbatie on 12/1/15.
 */
public class TestLogoutService {

    private static HttpSessionFactory sessionFactory;

    @BeforeClass
    public static void initialize() {
        sessionFactory = mock(HttpSessionFactory.class);
        HttpSession httpSession = mock(HttpSession.class);
        SecurityTokenHolder securityTokenHolder = mock(SecurityTokenHolder.class);

        when(sessionFactory.getOrCreateSession(null)).thenReturn(httpSession);
        when(httpSession.getAttribute(SecurityConstants.SAML_ASSERTION)).thenReturn(securityTokenHolder);
        when(securityTokenHolder.getRealmTokenMap()).thenReturn(new HashMap<>());
    }

    @Test
    public void testLogout() throws IOException, ParseException {
        LocalLogoutAction localLogoutAction = new LocalLogoutAction();
        Action logoutAction = localLogoutAction.getAction(null);

        LogoutService logoutService = new LogoutService();
        logoutService.setHttpSessionFactory(sessionFactory);
        logoutService.setLogoutActionProviders(Arrays.asList(localLogoutAction));

        String responseMessage = IOUtils.toString((ByteArrayInputStream) logoutService.getActionProviders(null).getEntity());

        JSONArray actionProperties = (JSONArray) new JSONParser().parse(responseMessage);
        assertEquals(actionProperties.size(), 1);
        JSONObject actionProperty = ((JSONObject) actionProperties.get(0));

        assertEquals(actionProperty.get("description"), logoutAction.getDescription());
        assertEquals(actionProperty.get("realm"), logoutAction.getId().substring(logoutAction.getId().lastIndexOf(".") + 1));
        assertEquals(actionProperty.get("title"), logoutAction.getTitle());
        assertEquals(actionProperty.get("url"), logoutAction.getUrl().toString());
    }
}
