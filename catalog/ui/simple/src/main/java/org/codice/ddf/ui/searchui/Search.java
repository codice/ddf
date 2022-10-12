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
package org.codice.ddf.ui.searchui;

import java.io.IOException;
import java.time.Duration;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.codice.ddf.ui.searchui.simple.Catalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

public class Search extends HttpServlet {

  private static final Logger LOGGER = LoggerFactory.getLogger(Search.class);

  private Catalog catalog;

  private TemplateEngine templateEngine;

  public Search(Catalog catalog) {
    this.catalog = catalog;

    ClassLoaderTemplateResolver templateResolver =
        new ClassLoaderTemplateResolver(Search.class.getClassLoader());
    templateResolver.setTemplateMode(TemplateMode.HTML);
    templateResolver.setPrefix("/templates/");
    templateResolver.setSuffix(".html");
    templateResolver.setCacheTTLMs(Duration.ofHours(1).toMillis());

    templateEngine = new TemplateEngine();
    templateEngine.setTemplateResolver(templateResolver);
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    if (!"/".equals(req.getRequestURI()) && !"/index.html".equals(req.getRequestURI())) {
      resp.setStatus(404);
      return;
    }

    WebContext ctx =
        new WebContext(req, resp, getServletConfig().getServletContext(), req.getLocale());

    if (req.getParameterMap().containsKey("id")) {
      metacardPage(req, resp, ctx);
    } else {
      searchPage(req, resp, ctx);
    }
  }

  private void metacardPage(HttpServletRequest req, HttpServletResponse resp, WebContext ctx)
      throws IOException {
    String id = req.getParameter("id");
    ctx.setVariable("id", id);

    Catalog.MetacardDetails details = catalog.metacard(id);
    ctx.setVariable("metacard", details);

    templateEngine.process("metacard", ctx, resp.getWriter());
    resp.setContentType("text/html;charset=UTF-8");
  }

  private void searchPage(HttpServletRequest req, HttpServletResponse resp, WebContext ctx)
      throws IOException {
    ctx.setVariable("q", req.getParameter("q"));
    ctx.setVariable("sort", req.getParameter("sort"));
    ctx.setVariable("past", req.getParameter("past"));

    boolean hasResults = false;
    if (catalog.hasQuery(req)) {
      Catalog.QueryResult results = catalog.query(req);
      ctx.setVariable("results", results);
      hasResults = results.hasResults;
    }
    ctx.setVariable("hasResults", hasResults);

    templateEngine.process("search", ctx, resp.getWriter());
    resp.setContentType("text/html;charset=UTF-8");
  }
}
