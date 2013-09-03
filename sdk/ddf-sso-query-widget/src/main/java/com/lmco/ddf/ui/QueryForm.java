/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package com.lmco.ddf.ui;


import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;


/**
 * This a a very simple example of a servlet protected by CAS that can be used
 * to query for metacards using metacard ids.
 * 
 * The main query form page that allows a user to enter a metacard id which will
 * be used to query for the associated metacard.
 * 
 */
public class QueryForm extends HttpServlet
{
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(QueryForm.class);

    private static final String QUERY_SERVLET = "/ddf/queryresult/Query";

    private static final String TICKET_PARAM = "ticket";

    @Override
    protected void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException,
        IOException
    {
        doPost(request, response);
    }

    @Override
    protected void doPost( HttpServletRequest request, HttpServletResponse response ) throws ServletException,
        IOException
    {
        response.setContentType("text/html");
        PrintWriter writer = response.getWriter();
        String html = createPage(request);
        writer.println(html);
    }

    /**
     * This method creates the main query form page which allows a user to enter
     * a metacard id which will be used to query for the associated metacard.
     * 
     * @param request The Http servlet request.
     * @return Returns the html representation of the query form page.
     */
    private String createPage( HttpServletRequest request )
    {
        StringBuilder sb = new StringBuilder();

        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"DTD/xhtml1-strict.dtd\">");
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">");
        sb.append("<head>");
        sb.append("<title>");
        sb.append("Example Query Widget");
        sb.append("</title>");
        sb.append("</head>");
        sb.append("<body>");
        sb.append("</h1>");
        sb.append("<p>");
        sb.append("Welcome ");
        sb.append("<b>");
        sb.append(getUserName(request));
        sb.append("</b>");
        sb.append("!");
        sb.append("</p>");
        sb.append("<h1>");
        sb.append("Example Query Widget");
        sb.append("<form method=\"post\" action=\"" + QUERY_SERVLET + "\">");
        sb.append("<p>");
        sb.append("<label for=\"query\">Search Query:</label><br />");
        sb.append("<textarea name=\"query\" id=\"query\" rows=\"1\" cols=\"80\"></textarea>");
        sb.append("</p>");
        sb.append("<input type=hidden name=proxyticket value=");
        sb.append(getProxyTicket(request));
        sb.append(" />");
        sb.append("<input type=\"submit\" />");
        sb.append("<input type=\"reset\" /> ");
        sb.append("</form>");
        sb.append("</body>");
        sb.append("</html>");

        StringBuilder message = new StringBuilder();
        message.append("\n########################################################################\n");
        message.append(" Query form html:\n");
        message.append(sb.toString());
        message.append("\n########################################################################\n");
        LOGGER.debug(message.toString());

        return sb.toString();
    }

    /**
     * Gets the user name of the user that is logged into the query widget.
     * 
     * @param request The Http servlet request
     * @return Return the user name of the user that is logged into the query
     *         widget.
     */
    private String getUserName( HttpServletRequest request )
    {
        Principal principal = request.getUserPrincipal();
        String userName = null;

        if (principal != null)
        {
            userName = principal.getName();
            LOGGER.info("user name: " + userName);
        }
        else
        {
            LOGGER.error("principal is null!");
            userName = "user";
        }

        return userName;
    }

    /**
     * Gets the CAS proxy ticket that will be used by the STS to get a SAML
     * assertion.
     * 
     * @param request The Http servlet request.
     * @return Returns the CAS proxy ticket that will be used by the STS to get
     *         a SAML assertion.
     */
    private String getProxyTicket( HttpServletRequest request )
    {
        return request.getParameter(TICKET_PARAM);
    }

    /**
     * Retrieves the url request of the service so that the ticket can be
     * validated later on.
     * 
     * @param request The Http servlet request.
     * @return Returns the service that the ticket was created for.
     */
    private String getService( HttpServletRequest request )
    {
        return request.getRequestURL().toString();
    }
}
