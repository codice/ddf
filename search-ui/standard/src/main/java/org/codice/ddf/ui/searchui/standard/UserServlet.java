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

import net.minidev.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.Writer;

public class UserServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserServlet.class);

    private static final String USER_ATTRIBUTE = "org.codice.ddf.ui.searchui.standard.properties.user";

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, java.io.IOException {

        String user = "";

        Writer writer = null;

        if (req.getAttribute(USER_ATTRIBUTE) != null) {
            user = req.getAttribute(USER_ATTRIBUTE).toString();
        }

        LOGGER.debug("user: {}", user);

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
