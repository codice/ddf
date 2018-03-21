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
package org.codice.ddf.ui;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;
import ddf.security.service.impl.cas.CasAuthenticationToken;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.platform.util.XMLUtils;
import org.jasig.cas.client.authentication.AttributePrincipal;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This a a very simple example of a servlet protected by CAS that can be used to query for
 * metacards using metacard ids.
 *
 * <p>The query page that displays a metacard in xml format.
 */
public class Query extends HttpServlet {
  private static final long serialVersionUID = 1L;

  private static final Logger LOGGER = LoggerFactory.getLogger(Query.class);

  private static final String QUERY_FORM_SERVLET = "/ddf/query/QueryForm";

  private static final String QUERY_REQUEST_PARAM = "query";

  private static final String PROXY_TICKET_REQUEST_PARAM = "proxyticket";

  private static final String STS_SERVICE_URL =
      "https://localhost:8993/services/SecurityTokenService";

  private static final XMLUtils XML_UTILS = XMLUtils.getInstance();

  @SuppressWarnings("squid:S2226" /* Lifecycle managed by blueprint */)
  private transient CatalogFramework catalogFramework;

  @SuppressWarnings("squid:S2226" /* Lifecycle managed by blueprint */)
  private transient SecurityManager securityManager;

  @SuppressWarnings("squid:S2226" /* Lifecycle managed by blueprint */)
  private transient FilterBuilder filterBuilder;

  public void setCatalogFramework(CatalogFramework catalogFramework) {
    this.catalogFramework = catalogFramework;
  }

  public void setSecurityManager(SecurityManager securityManager) {
    LOGGER.debug("Got a security manager");
    this.securityManager = securityManager;
  }

  public void setFilterBuilder(FilterBuilder filterBuilder) {
    this.filterBuilder = filterBuilder;
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      doPost(request, response);
    } catch (ServletException | IOException e) {
      LOGGER.warn("Could not post response due to: ", e);
    }
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    response.setContentType("text/html");

    try {
      PrintWriter writer = response.getWriter();
      LOGGER.debug(
          "serviceticket request parameter: {}", request.getParameter(PROXY_TICKET_REQUEST_PARAM));
      LOGGER.debug("query request parameter: {}", request.getParameter(QUERY_REQUEST_PARAM));
      String html = createPage(request);
      writer.println(html);
    } catch (IOException e) {
      LOGGER.warn("Could not get PrintWriter due to: ", e);
    }
  }

  /**
   * @param request The Http servlet request.
   * @return Returns the html representation of the query page which includes the xml representation
   *     of the metacard.
   */
  private String createPage(HttpServletRequest request) {
    StringBuilder sb = new StringBuilder();
    sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    sb.append(
        "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"DTD/xhtml1-strict.dtd\">");
    sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">");
    sb.append("<head>");
    sb.append("<title>");
    sb.append("Query Result");
    sb.append("</title>");
    sb.append("</head>");
    sb.append("<body>");
    sb.append("<h1>");
    sb.append("Query Request:");
    sb.append("</h1>");
    sb.append("<p>");
    String metacardId = request.getParameter(QUERY_REQUEST_PARAM);
    sb.append(metacardId);
    sb.append("</p>");
    sb.append("<h1>");
    sb.append("Query Result:");
    sb.append("</h1>");
    sb.append("<p>");
    sb.append("<textarea name=\"queryresult\" id=\"queryresult\" rows=\"40\" cols=\"80\">");
    sb.append(getMetacardForId(metacardId, getProxyTicket(request)));
    sb.append("</textarea>");
    sb.append("</p>");
    sb.append("<p>");
    sb.append("<form method=\"post\" action=\"" + QUERY_FORM_SERVLET + "\">");
    sb.append("<input type=\"submit\" value=\"New Query\" />");
    sb.append("</form>");
    sb.append("</p>");
    sb.append("</body>");
    sb.append("</html>");

    if (LOGGER.isDebugEnabled()) {
      StringBuilder message = new StringBuilder();
      message.append(
          "\n########################################################################\n");
      message.append(" Query result html:\n");
      message.append(sb.toString());
      message.append(
          "\n########################################################################\n");
      LOGGER.debug(message.toString());
    }

    return sb.toString();
  }

  /**
   * @param searchPhrase The search phrase used to query for the metacard.
   * @param proxyTicket The CAS proxy ticket that will be used by the STS to get a SAML assertion.
   * @return
   */
  private String getMetacardForId(String searchPhrase, String proxyTicket) {

    Filter filter = filterBuilder.attribute(Metacard.ANY_TEXT).is().like().text(searchPhrase);
    LOGGER.info("Query filter: {}", filter);
    String queryError = "Unable to perform query " + filter.toString() + ".";
    QueryRequest request = new QueryRequestImpl(new QueryImpl(filter), true);
    StringBuilder responseString = new StringBuilder();

    try {
      Subject subject = securityManager.getSubject(new CasAuthenticationToken(proxyTicket));
      LOGGER.info(
          "Adding {} property with value {} to request",
          SecurityConstants.SECURITY_SUBJECT,
          subject);
      request.getProperties().put(SecurityConstants.SECURITY_SUBJECT, subject);
    } catch (SecurityServiceException se) {
      LOGGER.error("Could not retrieve subject from securitymanager.", se);
      return queryError;
    }

    try {
      LOGGER.debug("About to query the catalog framework with query {}", filter);
      QueryResponse queryResponse = catalogFramework.query(request, null);
      LOGGER.debug("Got query response from catalog framework for query {}", filter);
      List<Result> results = queryResponse.getResults();
      if (results != null) {
        String message =
            "The query for " + filter.toString() + " returned " + results.size() + " results.";
        responseString.append(message);
        LOGGER.debug(message);
        for (Result curResult : results) {
          Metacard metacard = curResult.getMetacard();
          LOGGER.debug("Transforming the metacard with id [{}] to xml.", metacard.getId());
          BinaryContent content = catalogFramework.transform(metacard, "xml", null);
          StringWriter writer = new StringWriter();
          IOUtils.copy(content.getInputStream(), writer, "UTF8");
          LOGGER.debug("Formatting xml for metacard with id [{}].", metacard.getId());
          responseString.append(format(writer.toString()));
        }
      } else {
        String message = "The query for " + filter.toString() + " returned a null result.";
        responseString.append(message);
        LOGGER.warn(message);
      }
    } catch (SourceUnavailableException e) {
      LOGGER.error(queryError, e);
    } catch (UnsupportedQueryException e) {
      LOGGER.error(queryError, e);
    } catch (FederationException e) {
      LOGGER.error(queryError, e);
    } catch (CatalogTransformerException e) {
      LOGGER.error(queryError, e);
    } catch (IOException e) {
      LOGGER.error(queryError, e);
    }

    return responseString.toString();
  }

  /**
   * Gets the CAS proxy ticket that will be used by the STS to get a SAML assertion.
   *
   * @param request The Http servlet request.
   * @return Returns the CAS proxy ticket that will be used by the STS to get a SAML assertion.
   */
  private String getProxyTicket(HttpServletRequest request) {
    AttributePrincipal attributePrincipal = (AttributePrincipal) request.getUserPrincipal();
    String proxyTicket = null;

    if (attributePrincipal != null) {
      // proxyTicket = attributePrincipal.getProxyTicketFor(
      // "https://server:8993/ddf/query/sts" );
      LOGGER.debug("Getting proxy ticket for {}", STS_SERVICE_URL);
      proxyTicket = attributePrincipal.getProxyTicketFor(STS_SERVICE_URL);
      LOGGER.info("proxy ticket: {}", proxyTicket);
    } else {
      LOGGER.error("attribute principal is null!");
    }

    return proxyTicket;
  }

  /**
   * @param unformattedXml Unformatted xml.
   * @return Returns formatted xml.
   */
  private String format(String unformattedXml) {
    Source xmlInput = new StreamSource(new StringReader(unformattedXml));
    String formattedXml;

    formattedXml = XML_UTILS.prettyFormat(xmlInput);

    LOGGER.debug("Formatted xml:\n{}", formattedXml);

    if (StringUtils.isBlank(formattedXml)) {
      // Did not format so return unformatted xml
      formattedXml = unformattedXml;
    }
    return formattedXml;
  }
}
