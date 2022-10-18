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
package org.codice.ddf.ui.search;

import java.io.IOException;
import java.time.Duration;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

public class Search extends HttpServlet {

  private final Catalog catalog;

  private final TemplateEngine htmlTemplates;

  private final TemplateEngine cssTemplates;

  private String header = "UNCLASSIFIED";

  private String footer = "UNCLASSIFIED";

  private String color = "WHITE";

  private String background = "GREEN";

  public Search(Catalog catalog) {
    this.catalog = catalog;

    htmlTemplates = new TemplateEngine();
    htmlTemplates.setTemplateResolver(createTemplateResolver(TemplateMode.HTML));

    cssTemplates = new TemplateEngine();
    cssTemplates.setTemplateResolver(createTemplateResolver(TemplateMode.CSS));
  }

  private static ClassLoaderTemplateResolver createTemplateResolver(TemplateMode mode) {
    ClassLoaderTemplateResolver templateResolver =
        new ClassLoaderTemplateResolver(Search.class.getClassLoader());
    templateResolver.setTemplateMode(mode);
    if (mode.equals(TemplateMode.CSS)) {
      templateResolver.setSuffix(".css");
    } else {
      templateResolver.setSuffix(".html");
    }
    templateResolver.setPrefix("/templates/");
    templateResolver.setCharacterEncoding("UTF-8");
    templateResolver.setCacheTTLMs(Duration.ofHours(1).toMillis());
    return templateResolver;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    if (req.getServletPath().lastIndexOf("/") > 0
        || (!req.getServletPath().equals("/") && !req.getServletPath().endsWith(".css"))) {
      resp.setStatus(404);
      return;
    }

    WebContext ctx =
        new WebContext(req, resp, getServletConfig().getServletContext(), req.getLocale());

    addBanners(ctx);
    if (req.getServletPath().endsWith(".css")) {
      css(req, resp, ctx);
    } else if (req.getParameterMap().containsKey("id")) {
      metacardPage(req, resp, ctx);
    } else {
      searchPage(req, resp, ctx);
    }
  }

  private void css(HttpServletRequest req, HttpServletResponse resp, WebContext ctx)
      throws IOException {
    String css = req.getServletPath().replace("/", "").replace(".css", "");
    if (!css.isBlank()) {
      cssTemplates.process(css, ctx, resp.getWriter());
      resp.setContentType("text/css;charset=UTF-8");
    } else {
      resp.setStatus(400);
    }
  }

  private void metacardPage(HttpServletRequest req, HttpServletResponse resp, WebContext ctx)
      throws IOException {
    String id = req.getParameter("id");
    ctx.setVariable("id", id);

    Catalog.MetacardDetails details = catalog.metacard(id);
    ctx.setVariable("metacard", details);

    htmlTemplates.process("metacard", ctx, resp.getWriter());
    resp.setContentType("text/html;charset=UTF-8");
  }

  private void searchPage(HttpServletRequest req, HttpServletResponse resp, WebContext ctx)
      throws IOException {
    ctx.setVariable("q", req.getParameter("q"));
    ctx.setVariable("sort", req.getParameter("sort"));
    ctx.setVariable("past", req.getParameter("past"));

    if (catalog.hasQuery(req)) {
      Catalog.QueryResult results = catalog.query(req);
      ctx.setVariable("results", results);
    }

    htmlTemplates.process("search", ctx, resp.getWriter());
    resp.setContentType("text/html;charset=UTF-8");
  }

  private void addBanners(WebContext ctx) {
    ctx.setVariable("header", header);
    ctx.setVariable("footer", footer);
    ctx.setVariable("color", color);
    ctx.setVariable("background", background);
  }

  public void setHeader(String header) {
    this.header = header;
  }

  public void setFooter(String footer) {
    this.footer = footer;
  }

  public void setColor(String color) {
    this.color = color;
  }

  public void setBackground(String background) {
    this.background = background;
  }
}
