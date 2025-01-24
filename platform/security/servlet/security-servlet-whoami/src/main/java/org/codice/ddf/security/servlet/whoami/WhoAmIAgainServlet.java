/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.security.servlet.whoami;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhoAmIAgainServlet extends HttpServlet {

  private static final Logger LOGGER = LoggerFactory.getLogger(WhoAmIAgainServlet.class);

  private static final long serialVersionUID = 5538001643612956658L;

  private static Gson gson =
      new GsonBuilder()
          .serializeNulls()
          .setPrettyPrinting()
          .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
          .serializeNulls()
          .create();

  public WhoAmIAgainServlet() {}

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
    resp.setHeader("Cache-Control", "no-cache, no-store");
    resp.setHeader("Pragma", "no-cache");

    Map<String, String> realmToWhoMap = new HashMap<>();

    realmToWhoMap.put("default", "hi");

    resp.setContentType("application/json");

    resp.setStatus(403);
    throw new RuntimeException();
    //      resp.getWriter().print(gson.toJson(realmToWhoMap));

  }
}
