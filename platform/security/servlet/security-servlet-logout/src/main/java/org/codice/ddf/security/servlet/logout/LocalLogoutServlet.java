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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import ddf.security.SecurityConstants;
import ddf.security.common.util.SecurityTokenHolder;

public class LocalLogoutServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setHeader("Cache-Control", "no-cache, no-store");
        response.setHeader("Pragma", "no-cache");
        response.setContentType("text/html");

        HttpSession session = request.getSession(false);
        if (session != null) {
            SecurityTokenHolder savedToken = (SecurityTokenHolder) session
                    .getAttribute(SecurityConstants.SAML_ASSERTION);

            if (savedToken != null) {
                savedToken.removeAll();
            }
            session.invalidate();
            deleteJSessionId(response);
        }

        //This is just here to avoid a blank screen
        //A user would most likely never see this as they will be redirected to some other
        //login page by a filter or just returned back to the same screen they were already viewing
        response.getWriter()
                .print("You have successfully logged out. Please close your browser or tab.");

        response.getWriter().close();
    }

    private void deleteJSessionId(HttpServletResponse response) {
        Cookie cookie = new Cookie("JSESSIONID", "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setComment("EXPIRING COOKIE at " + System.currentTimeMillis());
        response.addCookie(cookie);
    }
}
