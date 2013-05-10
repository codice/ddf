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
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jasig.cas.client.authentication.AttributePrincipal;


public class ExampleSSOWidget extends HttpServlet
{
    private static final long serialVersionUID = 1L;


    @Override
    protected void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException
    {
        response.setContentType( "text/html" );
        createPage( request, response.getWriter() );
    }


    @SuppressWarnings( "rawtypes" )
    protected void createPage( HttpServletRequest request, PrintWriter out ) throws IOException, ServletException
    {
        String endl = System.getProperty( "line.separator" );
        StringBuilder sb = new StringBuilder();

        sb.append( "<html>" );
        sb.append( "<head>" );
        sb.append( "<title>Example SSO Widget</title>" );
        sb.append( "</head>" );

        sb.append( "<body>" ).append( endl );
        sb.append( "<h2 align=\"center\">Example SSO Widget" ).append( " Protected Page on " ).append( request.getServerName() ).append( "</h2>" ).append( endl );

        sb.append( new java.util.Date() ).append( "<br/><br/>" ).append( endl );

        sb.append( "request.getRemoteUser() = " );

        if ( request.getRemoteUser() != null )
        {
            sb.append( request.getRemoteUser() ).append( "<br/>" ).append( endl );
        }
        else
        {
            sb.append( "None" ).append( "<br/>" ).append( endl );
        }

        sb.append( "request.getUserPrincipal() = " );

        Principal p = request.getUserPrincipal();

        if ( p != null )
        {
            sb.append( p.getName() ).append( "<br/><br/>" ).append( endl );
        }
        else
        {
            sb.append( "None" ).append( "<br/><br/>" ).append( endl );
        }

        if ( request.getContextPath() != null && !"".equals(request.getContextPath()) )
        {
            sb.append( "</p><p>" ).append( endl );
            sb.append( "The context root name of this application is " ).append( request.getContextPath() ).append( endl );
            sb.append( "</p>" ).append( endl );
        }
        
        sb.append( "<h3>Released Attributes:</h3>" ).append( endl );

        Map attributes = null;

        if ( p != null )
        {
            AttributePrincipal principal = (AttributePrincipal) p;

            attributes = principal.getAttributes();

            if ( attributes != null && attributes.size() > 0 )
            {
                Iterator iterator = attributes.keySet().iterator();
                while ( iterator.hasNext() )
                {
                    String key = (String) iterator.next();
                    Object value = attributes.get( key );
                    if ( value instanceof String )
                    {
                        sb.append( key ).append( ": " ).append( value ).append( "<br/>" ).append( endl );
                    }
                    else if ( value instanceof List )
                    {
                        sb.append( key ).append( " is a List:<br/>" ).append( endl );
                        for( Object o : ( (List) value ) )
                        {
                            sb.append( "&nbsp;&nbsp;&nbsp;" ).append( o.toString() ).append( "<br/>" ).append( endl );
                        }
                    }
                }
            }

        }
        else
        {
            sb.append( "None" ).append( endl );
        }

        sb.append( "<h3>Cookies:</h3>" ).append( endl );
        Cookie[] cookies = request.getCookies();
        if ( cookies != null && cookies.length > 0 )
        {
            sb.append( "getCookies() = <br/>" ).append( endl );
            for( Cookie o : cookies )
            {
                sb.append( "&nbsp;&nbsp;&nbsp;" ).append( o.getName() ).append( ": " ).append( o.getValue() ).append( "<br/>" ).append( endl );
            }
        }
        else
        {
            sb.append( "getCookies() = null<br/>" ).append( endl );
        }

        sb.append( "<h3>Headers:</h3>" ).append( endl );
        Enumeration<String> hdrEnum = request.getHeaderNames();

        if ( hdrEnum != null )
        {
            sb.append( "getHeaders() = <br/>" ).append( endl );
            while ( hdrEnum.hasMoreElements() )
            {
                String name = (String) hdrEnum.nextElement();
                sb.append( "&nbsp;&nbsp;&nbsp;" ).append( name ).append( ": " ).append( request.getHeader( name ) ).append( "<br/>" ).append( endl );
            }
        }
        else
        {
            sb.append( "getHeaderNames() = null<br/>" ).append( endl );
        }

        // <input type="hidden" name="LogoutType" value="AppicationLogout">

//        sb.append( "<br/><br/>" ).append( endl );
//        sb.append( "<form name=\"Logout\" action=\"LogoutServlet\" method=\"get\">" );
//        sb.append( "<input type=\"hidden\" name=\"LogoutType\" value=\"ApplicationLogout\"/>" ).append( endl );
//        sb.append( "<input type=\"submit\" value=\"Logout\"/></form><br/>" ).append( endl );
//        sb.append( "<form name=\"SSOLogout\" action=\"LogoutServlet\" method=\"get\">" );
//        sb.append( "<input type=\"hidden\" name=\"LogoutType\" value=\"SingleSignOut\"/>" ).append( endl );
//        sb.append( "<input type=\"submit\" value=\"Single Sign-Out\"/></form><br/>" ).append( endl );

        sb.append( "</body></html>" ).append( endl );
        out.println( sb.toString() );
    }
}
