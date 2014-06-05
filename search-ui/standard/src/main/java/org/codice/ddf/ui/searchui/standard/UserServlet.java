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
package org.codice.ddf.ui.searchui.standard;

import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import net.minidev.json.JSONObject;
import org.apache.shiro.subject.PrincipalCollection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Writer;
import java.security.Principal;

public class UserServlet extends HttpServlet {

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, java.io.IOException {

        Subject subject = null;

        Writer writer = null;

        if (req.getAttribute(SecurityConstants.SECURITY_SUBJECT) != null) {
            subject = (Subject) req.getAttribute(SecurityConstants.SECURITY_SUBJECT);
        }

        if (subject != null)
        {
            PrincipalCollection principalCollection = subject.getPrincipals();
    
            String user = "";
    
            for(Object principal : principalCollection.asList()) {
                if(principal instanceof SecurityAssertion) {
                    SecurityAssertion assertion = (SecurityAssertion) principal;
    
                    Principal jPrincipal = assertion.getPrincipal();
                    user = jPrincipal.getName();
                }
            }
    
            JSONObject obj = new JSONObject();
            obj.put("user", user);
    
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
    
            try {
                writer = resp.getWriter();
                writer.write(obj.toJSONString());
            } finally {
                writer.close();
            }
        }
    }

}
